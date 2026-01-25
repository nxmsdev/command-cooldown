package dev.nxms.commandcooldown;

import dev.nxms.commandcooldown.commands.CooldownCommand;
import dev.nxms.commandcooldown.listeners.CommandListener;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.CooldownManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CommandCooldown extends JavaPlugin {

    private static CommandCooldown instance;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.cooldownManager = new CooldownManager(this);

        CooldownCommand cmdExec = new CooldownCommand(this);
        PluginCommand cmd = Objects.requireNonNull(getCommand("opoznieniekomend"),
                "Brak komendy 'opoznieniekomend' w plugin.yml");
        cmd.setExecutor(cmdExec);
        cmd.setTabCompleter(cmdExec);

        getServer().getPluginManager().registerEvents(new CommandListener(this), this);
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

    public void reloadAll() {
        reloadConfig();
        configManager.reload();
        messageManager.reload();
    }
}