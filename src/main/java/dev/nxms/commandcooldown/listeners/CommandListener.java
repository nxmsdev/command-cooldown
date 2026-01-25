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
        if (event.isCancelled()) {
            return;
        }

        if (!configManager.isEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();

        // Usuń / z początku
        String fullCommand = message.substring(1);

        // Pobierz samą komendę (bez argumentów)
        String command = fullCommand.split(" ")[0].toLowerCase();

        // Sprawdź czy komenda jest wykluczona
        if (isExcluded(command)) {
            return;
        }

        // Sprawdź czy świat jest wykluczony
        if (configManager.getExcludedWorlds().contains(player.getWorld().getName())) {
            return;
        }

        // Określ klucz cooldownu
        String cooldownKey = getCooldownKey(fullCommand, command);

        // Sprawdź czy komenda ma zdefiniowany cooldown
        int cooldownTime = cooldownManager.getCooldownTime(player, command);
        if (cooldownTime <= 0 && !configManager.isUseGlobalCooldown()) {
            return;
        }

        // Sprawdź czy gracz ma aktywny cooldown
        if (cooldownManager.hasCooldown(player, cooldownKey)) {
            event.setCancelled(true);

            long remaining = cooldownManager.getRemainingCooldown(player, cooldownKey);
            messageManager.sendCooldownMessage(player, command, remaining);

            // Graj dźwięk
            if (configManager.isPlaySound()) {
                try {
                    Sound sound = Sound.valueOf(configManager.getCooldownSound());
                    player.playSound(player.getLocation(), sound,
                            configManager.getSoundVolume(), configManager.getSoundPitch());
                } catch (IllegalArgumentException e) {
                    if (configManager.isDebug()) {
                        plugin.getLogger().warning("Nieprawidłowy dźwięk: " + configManager.getCooldownSound());
                    }
                }
            }

            if (configManager.isDebug()) {
                plugin.getLogger().info("Zablokowano komendę /" + command + " dla " + player.getName() +
                        " (pozostało: " + remaining + "s)");
            }

            return;
        }

        // Ustaw cooldown
        cooldownManager.setCooldown(player, cooldownKey);
    }

    private boolean isExcluded(String command) {
        command = command.toLowerCase();

        for (String excluded : configManager.getExcludedCommands()) {
            if (excluded.equalsIgnoreCase(command)) {
                return true;
            }
            // Sprawdź wildcard
            if (excluded.endsWith("*")) {
                String prefix = excluded.substring(0, excluded.length() - 1);
                if (command.startsWith(prefix)) {
                    return true;
                }
            }
        }

        // Wyklucz komendy pluginu CommandCooldown
        if (command.equals("commandcooldown") || command.equals("cc") ||
                command.equals("cooldown") || command.equals("cmdcd")) {
            return true;
        }

        return false;
    }

    private String getCooldownKey(String fullCommand, String command) {
        if (!configManager.isSeparateArguments()) {
            return command;
        }

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