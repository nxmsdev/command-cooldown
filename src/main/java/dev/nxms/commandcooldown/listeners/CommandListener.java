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
import java.util.Map;
import java.util.Set;

public class CommandListener implements Listener {

    private final ConfigManager config;
    private final CooldownManager cooldowns;
    private final MessageManager messages;

    // Komendy pluginu - zawsze wykluczone
    private static final Set<String> PLUGIN_COMMANDS = Set.of(
            "commandcooldown", "opoznieniekomend", "ok", "cc"
    );

    public CommandListener(CommandCooldown plugin) {
        this.config = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
        this.messages = plugin.getMessageManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        if (!config.isEnabled()) return;

        Player player = event.getPlayer();
        if (player.hasPermission("commandcooldown.bypass")) return;

        String msg = event.getMessage();
        if (msg.length() <= 1 || msg.charAt(0) != '/') return;

        String full = msg.substring(1);
        String cmd = full.split(" ")[0].toLowerCase(Locale.ROOT);

        // Zawsze nie blokuj komend pluginu
        if (PLUGIN_COMMANDS.contains(cmd)) return;

        // Wykluczenia z configu
        List<String> excluded = config.getExcludedCommands();
        if (excluded.contains(cmd)) return;

        // Sprawdź czy komenda ma indywidualny cooldown
        if (config.hasCommandCooldown(cmd)) {
            int cdSeconds = config.getCommandCooldown(cmd);

            if (cooldowns.isOnCommandCooldown(player, cmd)) {
                event.setCancelled(true);
                long remaining = cooldowns.getCommandRemainingSeconds(player, cmd);
                messages.send(player, "cooldown-active-command", Map.of(
                        "remaining", String.valueOf(remaining),
                        "command", cmd
                ));
                return;
            }

            cooldowns.applyCommandCooldown(player, cmd, cdSeconds);
            return;
        }

        // Globalny cooldown
        int globalCd = config.getCooldownSeconds();
        if (globalCd <= 0) return;

        if (cooldowns.isOnGlobalCooldown(player)) {
            event.setCancelled(true);
            long remaining = cooldowns.getGlobalRemainingSeconds(player);
            messages.send(player, "cooldown-active", Map.of(
                    "remaining", String.valueOf(remaining)
            ));
            return;
        }

        cooldowns.applyGlobalCooldown(player);
    }
}