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

import java.util.*;
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

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "pomoc", "help" -> sendHelp(sender);

            case "przeladuj", "reload" -> handleReload(sender);

            case "ustaw", "set" -> handleSet(sender, args);

            case "usun", "remove" -> handleRemove(sender, args);

            case "lista", "list" -> handleList(sender, args);

            case "info" -> handleInfo(sender, args);

            case "omin", "bypass" -> handleBypass(sender, args);

            case "wyczysc", "clear" -> handleClear(sender, args);

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
            messageManager.send(sender, "invalid-arguments", Map.of("usage", "/ok ustaw <komenda> <czas>"));
            return;
        }

        String targetCommand = args[1].toLowerCase();
        String timeStr = args[2];

        int seconds = TimeUtils.parseTime(timeStr);
        if (seconds <= 0) {
            messageManager.send(sender, "cooldown-set-error");
            return;
        }

        int maxCooldown = configManager.getMaxCooldown();
        if (maxCooldown > 0 && seconds > maxCooldown) {
            seconds = maxCooldown;
        }

        configManager.setCooldown(targetCommand, seconds);

        messageManager.send(sender, "cooldown-set", Map.of(
                "command", targetCommand,
                "cooldown", String.valueOf(seconds)
        ));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.remove")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messageManager.send(sender, "invalid-arguments", Map.of("usage", "/ok usun <komenda>"));
            return;
        }

        String targetCommand = args[1].toLowerCase();

        if (!configManager.getCooldowns().containsKey(targetCommand)) {
            messageManager.send(sender, "cooldown-not-found", Map.of("command", targetCommand));
            return;
        }

        configManager.removeCooldown(targetCommand);
        messageManager.send(sender, "cooldown-removed", Map.of("command", targetCommand));
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
            } catch (NumberFormatException ignored) {}
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
            messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-entry"), Map.of(
                    "command", entry.getKey(),
                    "cooldown", messageManager.formatTime(entry.getValue())
            ));
        }

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-page"), Map.of(
                "page", String.valueOf(page),
                "max_pages", String.valueOf(maxPages)
        ));

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-list-footer"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.info")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messageManager.send(sender, "invalid-arguments", Map.of("usage", "/ok info <komenda>"));
            return;
        }

        String targetCommand = args[1].toLowerCase();
        int cooldown = configManager.getCooldown(targetCommand);

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-header"));

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-command"), Map.of(
                "command", targetCommand
        ));

        messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-time"), Map.of(
                "cooldown", cooldown > 0 ? messageManager.formatTime(cooldown) : "Brak"
        ));

        if (sender instanceof Player player) {
            long remaining = cooldownManager.getRemainingCooldown(player, targetCommand);
            if (remaining > 0) {
                messageManager.sendRaw(sender, messageManager.getRaw("cooldown-info-remaining"), Map.of(
                        "remaining", messageManager.formatTime(remaining)
                ));
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
            messageManager.send(sender, "invalid-arguments", Map.of("usage", "/ok omin <gracz> [komenda]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }

        if (args.length >= 3) {
            String targetCmd = args[2].toLowerCase();
            cooldownManager.toggleBypassCommand(target.getUniqueId(), targetCmd);

            boolean enabled = cooldownManager.isCommandBypassed(target.getUniqueId(), targetCmd);
            messageManager.send(sender, enabled ? "bypass-command-enabled" : "bypass-command-disabled", Map.of(
                    "player", target.getName(),
                    "command", targetCmd
            ));
        } else {
            cooldownManager.toggleBypass(target.getUniqueId());
            boolean enabled = cooldownManager.isBypassed(target.getUniqueId());

            messageManager.send(sender, enabled ? "bypass-enabled" : "bypass-disabled", Map.of(
                    "player", target.getName()
            ));
        }
    }

    private void handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("commandcooldown.admin")) {
            messageManager.send(sender, "no-permission");
            return;
        }

        if (args.length < 2) {
            messageManager.send(sender, "invalid-arguments", Map.of("usage", "/ok wyczysc <gracz> [komenda]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageManager.send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }

        if (args.length >= 3) {
            String targetCmd = args[2].toLowerCase();
            cooldownManager.clearCooldown(target, targetCmd);
            messageManager.send(sender, "cooldowns-cleared-command", Map.of(
                    "player", target.getName(),
                    "command", targetCmd
            ));
        } else {
            cooldownManager.clearAllCooldowns(target);
            messageManager.send(sender, "cooldowns-cleared", Map.of(
                    "player", target.getName()
            ));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("pomoc", "przeladuj", "ustaw", "usun", "lista", "info", "omin", "wyczysc");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("usun") || sub.equals("remove") || sub.equals("info")) {
                return configManager.getCooldowns().keySet().stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (sub.equals("omin") || sub.equals("bypass") || sub.equals("wyczysc") || sub.equals("clear")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (sub.equals("lista") || sub.equals("list")) {
                int maxPages = (int) Math.ceil((double) configManager.getCooldowns().size() / 10);
                List<String> pages = new ArrayList<>();
                for (int i = 1; i <= Math.max(1, maxPages); i++) pages.add(String.valueOf(i));
                return pages;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();

            if (sub.equals("ustaw") || sub.equals("set")) {
                return Arrays.asList("3", "5", "10", "30", "60", "120", "300", "1m", "5m", "30m", "1h");
            }

            if (sub.equals("omin") || sub.equals("bypass") || sub.equals("wyczysc") || sub.equals("clear")) {
                return configManager.getCooldowns().keySet().stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}