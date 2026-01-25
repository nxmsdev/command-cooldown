package dev.nxms.commandcooldown.listeners;

import dev.nxms.commandcooldown.CommandCooldown;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.CooldownManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.Locale;

public class CommandListener implements Listener {

    private final ConfigManager config;
    private final CooldownManager cooldowns;
    private final MessageManager messages;

    public CommandListener(CommandCooldown plugin) {
        this.config = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
        this.messages = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        if (!config.isEnabled()) return;

        int cd = config.getCooldownSeconds();
        if (cd <= 0) return;

        Player player = event.getPlayer();
        if (player.hasPermission("commandcooldown.bypass")) return;

        String msg = event.getMessage();
        if (msg.length() <= 1 || msg.charAt(0) != '/') return;

        String full = msg.substring(1);
        String cmd = full.split(" ")[0].toLowerCase(Locale.ROOT);

        // zawsze nie blokuj komendy pluginu
        if (cmd.equals("opoznieniekomend") || cmd.equals("ok")) return;

        // wykluczenia z configu
        List<String> excluded = config.getExcludedCommands();
        if (excluded.contains(cmd)) return;

        if (cooldowns.isOnCooldown(player)) {
            event.setCancelled(true);
            long remaining = cooldowns.getRemainingSeconds(player);
            messages.send(player, "cooldown-active", java.util.Map.of(
                    "remaining", String.valueOf(remaining)
            ));
            return;
        }

        cooldowns.applyCooldown(player);
    }
}