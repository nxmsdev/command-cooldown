package dev.nxms.commandcooldown.listeners;

import dev.nxms.commandcooldown.CommandCooldown;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.CooldownManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    private final CommandCooldown plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final MessageManager messageManager;

    public CommandListener(CommandCooldown plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.cooldownManager = plugin.getCooldownManager();
        this.messageManager = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        if (!configManager.isEnabled()) return;

        Player player = event.getPlayer();
        String message = event.getMessage();

        if (message.length() <= 1 || message.charAt(0) != '/') return;

        String fullCommand = message.substring(1);
        String command = fullCommand.split(" ")[0].toLowerCase();

        if (isExcluded(command)) return;

        if (configManager.getExcludedWorlds().contains(player.getWorld().getName())) return;

        String cooldownKey = getCooldownKey(fullCommand, command);

        int cooldownTime = cooldownManager.getCooldownTime(player, command);
        if (cooldownTime <= 0 && !configManager.isUseGlobalCooldown()) return;

        if (cooldownManager.hasCooldown(player, cooldownKey)) {
            event.setCancelled(true);

            long remaining = cooldownManager.getRemainingCooldown(player, cooldownKey);
            messageManager.sendCooldownMessage(player, command, remaining);

            if (configManager.isPlaySound()) {
                try {
                    Sound sound = Sound.valueOf(configManager.getCooldownSound());
                    player.playSound(player.getLocation(), sound,
                            configManager.getSoundVolume(), configManager.getSoundPitch());
                } catch (IllegalArgumentException ignored) {
                    if (configManager.isDebug()) {
                        plugin.getLogger().warning("Nieprawidłowy dźwięk: " + configManager.getCooldownSound());
                    }
                }
            }
            return;
        }

        cooldownManager.setCooldown(player, cooldownKey);
    }

    private boolean isExcluded(String command) {
        command = command.toLowerCase();

        for (String excluded : configManager.getExcludedCommands()) {
            if (excluded.equalsIgnoreCase(command)) return true;

            if (excluded.endsWith("*")) {
                String prefix = excluded.substring(0, excluded.length() - 1);
                if (command.startsWith(prefix)) return true;
            }
        }

        // Wyklucz komendy tego pluginu
        return command.equals("opoznieniekomend")
                || command.equals("ok")
                || command.equals("commandcooldown") // na wszelki wypadek
                || command.equals("cc");
    }

    private String getCooldownKey(String fullCommand, String command) {
        if (!configManager.isSeparateArguments()) return command;

        String[] parts = fullCommand.split(" ");
        int depth = Math.min(configManager.getArgumentDepth() + 1, parts.length);

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            if (i > 0) key.append(" ");
            key.append(parts[i].toLowerCase());
        }
        return key.toString();
    }
}