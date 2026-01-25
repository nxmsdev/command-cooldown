package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final CommandCooldown plugin;
    private final Map<UUID, Map<String, Long>> playerCooldowns;
    private final Map<UUID, Long> globalCooldowns;
    private final Set<UUID> bypassedPlayers;
    private final Map<UUID, Set<String>> bypassedCommands;

    private File dataFile;
    private FileConfiguration dataConfig;

    public CooldownManager(CommandCooldown plugin) {
        this.plugin = plugin;
        this.playerCooldowns = new ConcurrentHashMap<>();
        this.globalCooldowns = new ConcurrentHashMap<>();
        this.bypassedPlayers = ConcurrentHashMap.newKeySet();
        this.bypassedCommands = new ConcurrentHashMap<>();

        if (plugin.getConfigManager().isPersistentCooldowns()) {
            loadData();
        }
    }

    public void reload() {
        if (plugin.getConfigManager().isPersistentCooldowns()) {
            loadData();
        }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć data.yml: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("cooldowns")) {
            for (String uuidStr : dataConfig.getConfigurationSection("cooldowns").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Long> cooldowns = new HashMap<>();

                for (String command : dataConfig.getConfigurationSection("cooldowns." + uuidStr).getKeys(false)) {
                    long expiry = dataConfig.getLong("cooldowns." + uuidStr + "." + command);
                    if (expiry > System.currentTimeMillis()) {
                        cooldowns.put(command, expiry);
                    }
                }

                if (!cooldowns.isEmpty()) {
                    playerCooldowns.put(uuid, cooldowns);
                }
            }
        }
    }

    public void saveData() {
        if (!plugin.getConfigManager().isPersistentCooldowns() || dataFile == null) {
            return;
        }

        dataConfig = new YamlConfiguration();

        for (Map.Entry<UUID, Map<String, Long>> entry : playerCooldowns.entrySet()) {
            String uuidStr = entry.getKey().toString();
            for (Map.Entry<String, Long> cooldown : entry.getValue().entrySet()) {
                if (cooldown.getValue() > System.currentTimeMillis()) {
                    dataConfig.set("cooldowns." + uuidStr + "." + cooldown.getKey(), cooldown.getValue());
                }
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać data.yml: " + e.getMessage());
        }
    }

    public boolean hasCooldown(Player player, String command) {
        command = command.toLowerCase();

        // Sprawdź bypass
        if (hasBypass(player, command)) {
            return false;
        }

        // Sprawdź globalny cooldown
        if (plugin.getConfigManager().isUseGlobalCooldown()) {
            Long globalExpiry = globalCooldowns.get(player.getUniqueId());
            if (globalExpiry != null && globalExpiry > System.currentTimeMillis()) {
                return true;
            }
        }

        // Sprawdź cooldown komendy
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (cooldowns == null) {
            return false;
        }

        Long expiry = cooldowns.get(command);
        if (expiry == null) {
            return false;
        }

        return expiry > System.currentTimeMillis();
    }

    public long getRemainingCooldown(Player player, String command) {
        command = command.toLowerCase();

        // Sprawdź globalny cooldown
        if (plugin.getConfigManager().isUseGlobalCooldown()) {
            Long globalExpiry = globalCooldowns.get(player.getUniqueId());
            if (globalExpiry != null && globalExpiry > System.currentTimeMillis()) {
                return (globalExpiry - System.currentTimeMillis()) / 1000;
            }
        }

        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (cooldowns == null) {
            return 0;
        }

        Long expiry = cooldowns.get(command);
        if (expiry == null || expiry <= System.currentTimeMillis()) {
            return 0;
        }

        return (expiry - System.currentTimeMillis()) / 1000;
    }

    public void setCooldown(Player player, String command) {
        command = command.toLowerCase();

        int cooldownSeconds = getCooldownTime(player, command);
        if (cooldownSeconds <= 0) {
            return;
        }

        long expiryTime = System.currentTimeMillis() + (cooldownSeconds * 1000L);

        playerCooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(command, expiryTime);

        // Ustaw globalny cooldown
        if (plugin.getConfigManager().isUseGlobalCooldown()) {
            long globalExpiry = System.currentTimeMillis() + (plugin.getConfigManager().getGlobalCooldown() * 1000L);
            globalCooldowns.put(player.getUniqueId(), globalExpiry);
        }

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("Ustawiono cooldown " + cooldownSeconds + "s dla " + player.getName() + " na komendę /" + command);
        }
    }

    public int getCooldownTime(Player player, String command) {
        command = command.toLowerCase();
        ConfigManager configManager = plugin.getConfigManager();

        // Sprawdź grupy cooldownów
        for (Map.Entry<String, ConfigManager.CooldownGroup> entry : configManager.getCooldownGroups().entrySet()) {
            if (player.hasPermission("commandcooldown.group." + entry.getKey())) {
                ConfigManager.CooldownGroup group = entry.getValue();

                // Sprawdź czy grupa ma specjalny cooldown dla tej komendy
                if (group.getCommands().containsKey(command)) {
                    return group.getCommands().get(command);
                }

                // Użyj mnożnika
                int baseCooldown = configManager.getCooldown(command);
                return (int) (baseCooldown * group.getMultiplier());
            }
        }

        return configManager.getCooldown(command);
    }

    public void clearCooldown(Player player, String command) {
        command = command.toLowerCase();
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUniqueId());
        if (cooldowns != null) {
            cooldowns.remove(command);
        }
    }

    public void clearAllCooldowns(Player player) {
        playerCooldowns.remove(player.getUniqueId());
        globalCooldowns.remove(player.getUniqueId());
    }

    public void clearAllCooldowns(UUID uuid) {
        playerCooldowns.remove(uuid);
        globalCooldowns.remove(uuid);
    }

    public boolean hasBypass(Player player, String command) {
        // Sprawdź permisję ogólną
        if (player.hasPermission("commandcooldown.bypass")) {
            return true;
        }

        // Sprawdź permisję dla konkretnej komendy
        if (player.hasPermission("commandcooldown.bypass.specific." + command.toLowerCase())) {
            return true;
        }

        // Sprawdź tymczasowy bypass
        if (bypassedPlayers.contains(player.getUniqueId())) {
            return true;
        }

        Set<String> commands = bypassedCommands.get(player.getUniqueId());
        return commands != null && commands.contains(command.toLowerCase());
    }

    public void toggleBypass(UUID uuid) {
        if (bypassedPlayers.contains(uuid)) {
            bypassedPlayers.remove(uuid);
        } else {
            bypassedPlayers.add(uuid);
        }
    }

    public boolean isBypassed(UUID uuid) {
        return bypassedPlayers.contains(uuid);
    }

    public void toggleBypassCommand(UUID uuid, String command) {
        command = command.toLowerCase();
        Set<String> commands = bypassedCommands.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet());

        if (commands.contains(command)) {
            commands.remove(command);
        } else {
            commands.add(command);
        }
    }

    public boolean isCommandBypassed(UUID uuid, String command) {
        Set<String> commands = bypassedCommands.get(uuid);
        return commands != null && commands.contains(command.toLowerCase());
    }

    public Map<String, Long> getPlayerCooldowns(UUID uuid) {
        Map<String, Long> cooldowns = playerCooldowns.get(uuid);
        if (cooldowns == null) {
            return new HashMap<>();
        }

        // Filtruj wygasłe
        Map<String, Long> active = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : cooldowns.entrySet()) {
            if (entry.getValue() > now) {
                active.put(entry.getKey(), entry.getValue());
            }
        }
        return active;
    }
}