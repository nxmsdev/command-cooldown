package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import dev.nxms.commandcooldown.utils.TimeUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final CommandCooldown plugin;
    private FileConfiguration config;
    private File configFile;

    private boolean enabled;
    private boolean debug;
    private int defaultCooldown;
    private boolean useGlobalCooldown;
    private int globalCooldown;
    private boolean showRemainingTime;
    private boolean useActionBar;
    private boolean useTitle;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private boolean playSound;
    private String cooldownSound;
    private float soundVolume;
    private float soundPitch;
    private boolean persistentCooldowns;
    private int maxCooldown;
    private boolean checkAliases;
    private boolean separateArguments;
    private int argumentDepth;

    private Map<String, Integer> cooldowns;
    private List<String> excludedCommands;
    private List<String> excludedWorlds;
    private Map<String, CooldownGroup> cooldownGroups;

    public ConfigManager(CommandCooldown plugin) {
        this.plugin = plugin;
        this.cooldowns = new HashMap<>();
        this.excludedCommands = new ArrayList<>();
        this.excludedWorlds = new ArrayList<>();
        this.cooldownGroups = new HashMap<>();
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }

    private void loadValues() {
        enabled = config.getBoolean("enabled", true);
        debug = config.getBoolean("debug", false);
        defaultCooldown = config.getInt("default-cooldown", 0);
        useGlobalCooldown = config.getBoolean("use-global-cooldown", false);
        globalCooldown = config.getInt("global-cooldown", 3);
        showRemainingTime = config.getBoolean("show-remaining-time", true);
        useActionBar = config.getBoolean("use-actionbar", false);
        useTitle = config.getBoolean("use-title", false);
        titleFadeIn = config.getInt("title-fade-in", 10);
        titleStay = config.getInt("title-stay", 40);
        titleFadeOut = config.getInt("title-fade-out", 10);
        playSound = config.getBoolean("play-sound", true);
        cooldownSound = config.getString("cooldown-sound", "ENTITY_VILLAGER_NO");
        soundVolume = (float) config.getDouble("cooldown-sound-volume", 1.0);
        soundPitch = (float) config.getDouble("cooldown-sound-pitch", 1.0);
        persistentCooldowns = config.getBoolean("persistent-cooldowns", false);
        maxCooldown = config.getInt("max-cooldown", 86400);
        checkAliases = config.getBoolean("check-aliases", true);
        separateArguments = config.getBoolean("separate-arguments", false);
        argumentDepth = config.getInt("argument-depth", 1);

        // Wczytaj cooldowny
        cooldowns.clear();
        ConfigurationSection cooldownsSection = config.getConfigurationSection("cooldowns");
        if (cooldownsSection != null) {
            for (String key : cooldownsSection.getKeys(false)) {
                Object value = cooldownsSection.get(key);
                int seconds;

                if (value instanceof Integer) {
                    seconds = (Integer) value;
                } else if (value instanceof String) {
                    seconds = TimeUtils.parseTime((String) value);
                } else {
                    seconds = 0;
                }

                cooldowns.put(key.toLowerCase(), seconds);
            }
        }

        // Wczytaj wykluczone komendy
        excludedCommands = config.getStringList("excluded-commands");
        excludedCommands.replaceAll(String::toLowerCase);

        // Wczytaj wykluczone światy
        excludedWorlds = config.getStringList("excluded-worlds");

        // Wczytaj grupy cooldownów
        cooldownGroups.clear();
        ConfigurationSection groupsSection = config.getConfigurationSection("cooldown-groups");
        if (groupsSection != null) {
            for (String groupName : groupsSection.getKeys(false)) {
                ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupName);
                if (groupSection != null) {
                    double multiplier = groupSection.getDouble("multiplier", 1.0);
                    Map<String, Integer> groupCooldowns = new HashMap<>();

                    ConfigurationSection commandsSection = groupSection.getConfigurationSection("commands");
                    if (commandsSection != null) {
                        for (String cmd : commandsSection.getKeys(false)) {
                            groupCooldowns.put(cmd.toLowerCase(), commandsSection.getInt(cmd));
                        }
                    }

                    cooldownGroups.put(groupName.toLowerCase(), new CooldownGroup(multiplier, groupCooldowns));
                }
            }
        }
    }

    public void reload() {
        loadConfig();
    }

    public void save() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać config.yml: " + e.getMessage());
        }
    }

    public void setCooldown(String command, int seconds) {
        command = command.toLowerCase();
        cooldowns.put(command, seconds);
        config.set("cooldowns." + command, seconds);
        save();
    }

    public void removeCooldown(String command) {
        command = command.toLowerCase();
        cooldowns.remove(command);
        config.set("cooldowns." + command, null);
        save();
    }

    public int getCooldown(String command) {
        command = command.toLowerCase();

        // Sprawdź dokładne dopasowanie
        if (cooldowns.containsKey(command)) {
            return cooldowns.get(command);
        }

        // Sprawdź wildcard
        for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
            if (entry.getKey().endsWith("*")) {
                String prefix = entry.getKey().substring(0, entry.getKey().length() - 1);
                if (command.startsWith(prefix)) {
                    return entry.getValue();
                }
            }
        }

        return defaultCooldown;
    }

    // Gettery
    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getDefaultCooldown() {
        return defaultCooldown;
    }

    public boolean isUseGlobalCooldown() {
        return useGlobalCooldown;
    }

    public int getGlobalCooldown() {
        return globalCooldown;
    }

    public boolean isShowRemainingTime() {
        return showRemainingTime;
    }

    public boolean isUseActionBar() {
        return useActionBar;
    }

    public boolean isUseTitle() {
        return useTitle;
    }

    public int getTitleFadeIn() {
        return titleFadeIn;
    }

    public int getTitleStay() {
        return titleStay;
    }

    public int getTitleFadeOut() {
        return titleFadeOut;
    }

    public boolean isPlaySound() {
        return playSound;
    }

    public String getCooldownSound() {
        return cooldownSound;
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public float getSoundPitch() {
        return soundPitch;
    }

    public boolean isPersistentCooldowns() {
        return persistentCooldowns;
    }

    public int getMaxCooldown() {
        return maxCooldown;
    }

    public boolean isCheckAliases() {
        return checkAliases;
    }

    public boolean isSeparateArguments() {
        return separateArguments;
    }

    public int getArgumentDepth() {
        return argumentDepth;
    }

    public Map<String, Integer> getCooldowns() {
        return new HashMap<>(cooldowns);
    }

    public List<String> getExcludedCommands() {
        return excludedCommands;
    }

    public List<String> getExcludedWorlds() {
        return excludedWorlds;
    }

    public Map<String, CooldownGroup> getCooldownGroups() {
        return cooldownGroups;
    }

    // Klasa wewnętrzna dla grup cooldownów
    public static class CooldownGroup {
        private final double multiplier;
        private final Map<String, Integer> commands;

        public CooldownGroup(double multiplier, Map<String, Integer> commands) {
            this.multiplier = multiplier;
            this.commands = commands;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public Map<String, Integer> getCommands() {
            return commands;
        }
    }
}