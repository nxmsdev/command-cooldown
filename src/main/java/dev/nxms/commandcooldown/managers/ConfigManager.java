package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ConfigManager {

    private final CommandCooldown plugin;

    private boolean enabled;
    private int cooldownSeconds;
    private List<String> excludedCommands;

    public ConfigManager(CommandCooldown plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        this.enabled = cfg.getBoolean("enabled", true);
        this.cooldownSeconds = Math.max(0, cfg.getInt("cooldown-seconds", 3));

        List<String> list = cfg.getStringList("excluded-commands");
        List<String> normalized = new ArrayList<>();
        for (String s : list) {
            if (s != null && !s.isBlank()) {
                normalized.add(s.toLowerCase(Locale.ROOT));
            }
        }
        this.excludedCommands = normalized;
    }

    public void setCooldownSeconds(int seconds) {
        seconds = Math.max(0, seconds);
        plugin.getConfig().set("cooldown-seconds", seconds);
        plugin.saveConfig();
        this.cooldownSeconds = seconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<String> getExcludedCommands() {
        return excludedCommands;
    }
}