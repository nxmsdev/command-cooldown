package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ConfigManager {

    private final CommandCooldown plugin;

    private boolean enabled;
    private String language;
    private int cooldownSeconds;
    private List<String> excludedCommands;
    private Map<String, Integer> commandCooldowns;

    public ConfigManager(CommandCooldown plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();

        this.enabled = cfg.getBoolean("enabled", true);
        this.language = cfg.getString("language", "en").toLowerCase(Locale.ROOT);
        this.cooldownSeconds = Math.max(0, cfg.getInt("cooldown-seconds", 3));

        // Wykluczone komendy
        List<String> list = cfg.getStringList("excluded-commands");
        List<String> normalized = new ArrayList<>();
        for (String s : list) {
            if (s != null && !s.isBlank()) {
                normalized.add(s.toLowerCase(Locale.ROOT));
            }
        }
        this.excludedCommands = normalized;

        // Cooldowny per-komenda
        this.commandCooldowns = new HashMap<>();
        ConfigurationSection section = cfg.getConfigurationSection("command-cooldowns");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                int value = section.getInt(key, 0);
                if (value > 0) {
                    commandCooldowns.put(key.toLowerCase(Locale.ROOT), value);
                }
            }
        }

        plugin.getLogger().info("Config has been reloaded.");
    }

    public void setCooldownSeconds(int seconds) {
        seconds = Math.max(0, seconds);
        plugin.getConfig().set("cooldown-seconds", seconds);
        plugin.saveConfig();
        this.cooldownSeconds = seconds;
    }

    public void setCommandCooldown(String command, int seconds) {
        command = command.toLowerCase(Locale.ROOT);
        seconds = Math.max(0, seconds);

        plugin.getConfig().set("command-cooldowns." + command, seconds);
        plugin.saveConfig();

        if (seconds > 0) {
            commandCooldowns.put(command, seconds);
        } else {
            commandCooldowns.remove(command);
        }
    }

    public void removeCommandCooldown(String command) {
        command = command.toLowerCase(Locale.ROOT);

        plugin.getConfig().set("command-cooldowns." + command, null);
        plugin.saveConfig();

        commandCooldowns.remove(command);
    }

    public boolean hasCommandCooldown(String command) {
        return commandCooldowns.containsKey(command.toLowerCase(Locale.ROOT));
    }

    public int getCommandCooldown(String command) {
        return commandCooldowns.getOrDefault(command.toLowerCase(Locale.ROOT), 0);
    }

    public Map<String, Integer> getCommandCooldowns() {
        return new HashMap<>(commandCooldowns);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getLanguage() {
        return language;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<String> getExcludedCommands() {
        return excludedCommands;
    }
}