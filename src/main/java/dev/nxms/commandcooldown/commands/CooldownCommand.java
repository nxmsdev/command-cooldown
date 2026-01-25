package dev.nxms.commandcooldown.commands;

import dev.nxms.commandcooldown.CommandCooldown;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.CooldownManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import dev.nxms.commandcooldown.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CooldownCommand implements CommandExecutor, TabCompleter {

    private final CommandCooldown plugin;
    private final ConfigManager configManager;
    private final CooldownManager cooldownManager;
    private final MessageManager messageManager;

    public CooldownCommand(CommandCooldown plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.cooldownManager = plugin.getCooldownManager();
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help" -> sendHelp(sender);
            case "reload" -> handleReload(sender);
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            case "bypass" -> handleBypass(sender, args);
            case "clear" -> handleClear(sender, args);
            default -> messageManager.send(sender, "invalid-command");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        messageManager.sendRaw(sender, messageManager.getRaw("help-header"));

        for (String line : messageManager.getRawList("help-commands")) {
            messageManager.sendRaw(sender, line);
        }

        messageManager.sendRaw(sender, messageManager.getRaw("help-footer"));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("commandcooldown.reload")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        try {
            plugin.reload();
            messageManager.send(sender, "reload-success");
        } catch (Exception e) {
            messageManager.send(sender, "reload-error");
            plugin.getLogger().severe("Błąd podczas przeładowywania: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.set")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 3) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/cc set <komenda> <czas>");
            messageManager.send(sender, "invalid-arguments", placeholders);
            return;
        }

        String targetCommand = args[1].toLowerCase();
        String timeStr = args[2];

        int seconds;
        try {
            seconds = TimeUtils.parseTime(timeStr);
        } catch (Exception e) {
            messageManager.send(sender, "cooldown-set-error");
            return;
        }

        if (seconds <= 0) {
            messageManager.send(sender, "cooldown-set-error");
            return;
        }

        // Sprawdź maksymalny cooldown
        int maxCooldown = configManager.getMaxCooldown();
        if (maxCooldown > 0 && seconds > maxCooldown) {
            seconds = maxCooldown;
        }

        configManager.setCooldown(targetCommand, seconds);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("command", targetCommand);
        placeholders.put("cooldown", String.valueOf(seconds));
        messageManager.send(sender, "cooldown-set", placeholders);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.remove")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/cc remove <komenda>");
            messageManager.send(sender, "invalid-arguments", placeholders);
            return;
        }

        String targetCommand = args[1].toLowerCase();

        if (!configManager.getCooldowns().containsKey(targetCommand)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("command", targetCommand);
            messageManager.send(sender, "cooldown-not-found", placeholders);
            return;
        }

        configManager.removeCooldown(targetCommand);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("command", targetCommand);
        messageManager.send(sender, "cooldown-removed", placeholders);
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.list")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        Map<String, Integer> cooldowns = configManager.getCooldowns();

        if (cooldowns.isEmpty()) {
            messageManager.send(sender, "cooldown-list-empty");
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        int itemsPerPage = 10;
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(cooldowns.entrySet());
        entries.sort(Map.Entry.comparingByKey());

        int maxPages = (int) Math.ceil((double) entries.size() / itemsPerPage);
        page = Math.max(1, Math.min(page, maxPages));

        int start = (page - 1) * itemsPerPage;
        int end = Math.min(start + itemsPerPage, entries.size());

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-header"));

        for (int i = start; i < end; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("command", entry.getKey());
            placeholders.put("cooldown", messageManager.formatTime(entry.getValue()));
            messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-entry"), placeholders);
        }

        Map<String, String> pagePlaceholders = new HashMap<>();
        pagePlaceholders.put("page", String.valueOf(page));
        pagePlaceholders.put("max_pages", String.valueOf(maxPages));
        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-page"), pagePlaceholders);

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-footer"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.info")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/cc info <komenda>");
            messageManager.send(sender, "invalid-arguments", placeholders);
            return;
        }

        String targetCommand = args[1].toLowerCase();
        int cooldown = configManager.getCooldown(targetCommand);

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-header"));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("command", targetCommand);
        placeholders.put("cooldown", cooldown > 0 ? messageManager.formatTime(cooldown) : "Brak");

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-command"), placeholders);
        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-time"), placeholders);

        // Jeśli sender jest graczem, pokaż pozostały czas
        if (sender instanceof Player player) {
            long remaining = cooldownManager.getRemainingCooldown(player, targetCommand);

            if (remaining > 0) {
                placeholders.put("remaining", messageManager.formatTime(remaining));
                messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-remaining"), placeholders);
            } else {
                messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-no-cooldown"));
            }
        }
    }

    private void handleBypass(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/cc bypass <gracz> [komenda]");
            messageManager.send(sender, "invalid-arguments", placeholders);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[1]);
            messageManager.send(sender, "player-not-found", placeholders);
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());

        if (args.length >= 3) {
            // Bypass dla konkretnej komendy
            String targetCommand = args[2].toLowerCase();
            cooldownManager.toggleBypassCommand(target.getUniqueId(), targetCommand);

            placeholders.put("command", targetCommand);

            if (cooldownManager.isCommandBypassed(target.getUniqueId(), targetCommand)) {
                messageManager.send(sender, "bypass-command-enabled", placeholders);
            } else {
                messageManager.send(sender, "bypass-command-disabled", placeholders);
            }
        } else {
            // Bypass globalny
            cooldownManager.toggleBypass(target.getUniqueId());

            if (cooldownManager.isBypassed(target.getUniqueId())) {
                messageManager.send(sender, "bypass-enabled", placeholders);
            } else {
                messageManager.send(sender, "bypass-disabled", placeholders);
            }
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("usage", "/cc clear <gracz> [komenda]");
            messageManager.send(sender, "invalid-arguments", placeholders);
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", args[1]);
            messageManager.send(sender, "player-not-found", placeholders);
            return;
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());

        if (args.length >= 3) {
            String targetCommand = args[2].toLowerCase();
            cooldownManager.clearCooldown(target, targetCommand);
            placeholders.put("command", targetCommand);
            messageManager.send(sender, "cooldowns-cleared-command", placeholders);
        } else {
            cooldownManager.clearAllCooldowns(target);
            messageManager.send(sender, "cooldowns-cleared", placeholders);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("help", "reload", "set", "remove", "list", "info", "bypass", "clear");
            completions.addAll(subCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "remove", "info" -> completions.addAll(configManager.getCooldowns().keySet().stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()));
                case "bypass", "clear" -> completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList()));
                case "set" -> {
                    completions.addAll(Arrays.asList("spawn", "home", "tpa", "warp", "kit"));
                    completions = completions.stream()
                            .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                            .collect(Collectors.toList());
                }
                case "list" -> {
                    int maxPages = (int) Math.ceil((double) configManager.getCooldowns().size() / 10);
                    for (int i = 1; i <= maxPages; i++) {
                        completions.add(String.valueOf(i));
                    }
                }
            }
        } else if (args.length == 3) {
            String subCommand = args[0].toLowerCase();

            switch (subCommand) {
                case "set" -> completions.addAll(Arrays.asList("30", "60", "120", "300", "1m", "5m", "30m", "1h"));
                case "bypass", "clear" -> completions.addAll(configManager.getCooldowns().keySet().stream()
                        .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList()));
            }
        }

        return completions;
    }
}