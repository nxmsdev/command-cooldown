package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final CommandCooldown plugin;
    private final Map<UUID, Long> nextAllowedAt = new ConcurrentHashMap<>();

    public CooldownManager(CommandCooldown plugin) {
        this.plugin = plugin;
    }

    public long getRemainingSeconds(Player player) {
        long now = System.currentTimeMillis();
        long allowedAt = nextAllowedAt.getOrDefault(player.getUniqueId(), 0L);
        if (now >= allowedAt) return 0;

        long remainingMs = allowedAt - now;
        return (remainingMs + 999) / 1000; // sufit
    }

    public boolean isOnCooldown(Player player) {
        return getRemainingSeconds(player) > 0;
    }

    public void applyCooldown(Player player) {
        int seconds = plugin.getConfigManager().getCooldownSeconds();
        if (seconds <= 0) return;
        nextAllowedAt.put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }

    public void clear(Player player) {
        nextAllowedAt.remove(player.getUniqueId());
    }
}