package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final CommandCooldown plugin;

    // Globalny cooldown
    private final Map<UUID, Long> globalCooldowns = new ConcurrentHashMap<>();

    // Cooldowny per-komenda: UUID -> (komenda -> czas wygaśnięcia)
    private final Map<UUID, Map<String, Long>> commandCooldowns = new ConcurrentHashMap<>();

    public CooldownManager(CommandCooldown plugin) {
        this.plugin = plugin;
    }

    // =========== GLOBALNY COOLDOWN ===========

    public long getGlobalRemainingSeconds(Player player) {
        long now = System.currentTimeMillis();
        long allowedAt = globalCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now >= allowedAt) return 0;

        long remainingMs = allowedAt - now;
        return (remainingMs + 999) / 1000;
    }

    public boolean isOnGlobalCooldown(Player player) {
        return getGlobalRemainingSeconds(player) > 0;
    }

    public void applyGlobalCooldown(Player player) {
        int seconds = plugin.getConfigManager().getCooldownSeconds();
        if (seconds <= 0) return;
        globalCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    // =========== COOLDOWN PER-KOMENDA ===========

    public long getCommandRemainingSeconds(Player player, String command) {
        Map<String, Long> playerCooldowns = commandCooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return 0;

        Long allowedAt = playerCooldowns.get(command.toLowerCase());
        if (allowedAt == null) return 0;

        long now = System.currentTimeMillis();
        if (now >= allowedAt) return 0;

        long remainingMs = allowedAt - now;
        return (remainingMs + 999) / 1000;
    }

    public boolean isOnCommandCooldown(Player player, String command) {
        return getCommandRemainingSeconds(player, command) > 0;
    }

    public void applyCommandCooldown(Player player, String command, int seconds) {
        if (seconds <= 0) return;

        command = command.toLowerCase();
        long expiresAt = System.currentTimeMillis() + (seconds * 1000L);

        commandCooldowns
                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(command, expiresAt);
    }

    // =========== CZYSZCZENIE ===========

    public void clearAll(Player player) {
        globalCooldowns.remove(player.getUniqueId());
        commandCooldowns.remove(player.getUniqueId());
    }
}