package dev.nxms.commandcooldown;

import dev.nxms.commandcooldown.commands.CooldownCommand;
import dev.nxms.commandcooldown.listeners.CommandListener;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.CooldownManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandCooldown extends JavaPlugin {

    private static CommandCooldown instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        // Inicjalizacja managerów
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.cooldownManager = new CooldownManager(this);

        // Rejestracja komend
        CooldownCommand cooldownCommand = new CooldownCommand(this);
        getCommand("commandcooldown").setExecutor(cooldownCommand);
        getCommand("commandcooldown").setTabCompleter(cooldownCommand);

        // Rejestracja listenerów
        getServer().getPluginManager().registerEvents(new CommandListener(this), this);

        getLogger().info("CommandCooldown został włączony!");
        getLogger().info("Załadowano " + configManager.getCooldowns().size() + " cooldownów.");
    }

    @Override
    public void onDisable() {
        if (cooldownManager != null) {
            cooldownManager.saveData();
        }
        getLogger().info("CommandCooldown został wyłączony!");
    }

    public static CommandCooldown getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public void reload() {
        configManager.reload();
        messageManager.reload();
        cooldownManager.reload();
    }
}