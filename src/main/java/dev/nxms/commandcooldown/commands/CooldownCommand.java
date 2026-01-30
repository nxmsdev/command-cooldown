package dev.nxms.commandcooldown.commands;

import dev.nxms.commandcooldown.CommandCooldown;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CooldownCommand implements CommandExecutor, TabCompleter {

    private final CommandCooldown plugin;
    private final ConfigManager config;
    private final MessageManager messages;

    public CooldownCommand(CommandCooldown plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.messages = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!config.isEnabled()) {
            messages.send(sender, "plugin-disabled");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "pomoc", "help" -> sendHelp(sender);

            case "info" -> handleInfo(sender);

            case "ustaw", "set" -> handleSet(sender, args, label);

            case "usun", "remove" -> handleRemove(sender, args, label);

            case "lista", "list" -> handleList(sender);

            case "przeladuj", "reload" -> handleReload(sender);

            default -> messages.send(sender, "invalid-command");
        }

        return true;
    }

    private void handleInfo(CommandSender sender) {
        if (!sender.hasPermission("commandcooldown.info")) {
            messages.send(sender, "no-permission");
            return;
        }
        messages.send(sender, "cooldown-info", Map.of(
                "cooldown", String.valueOf(config.getCooldownSeconds())
        ));
    }

    private void handleSet(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("commandcooldown.set")) {
            messages.send(sender, "no-permission");
            return;
        }

        String cmdPrefix = getCommandPrefix(label);

        if (args.length < 2) {
            messages.send(sender, "invalid-arguments", Map.of(
                    "usage", cmdPrefix + " set <seconds> | " + cmdPrefix + " set <command> <seconds>"
            ));
            return;
        }

        // /cc set <seconds> - globalny cooldown
        if (args.length == 2) {
            int seconds;
            try {
                seconds = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                messages.send(sender, "invalid-arguments", Map.of(
                        "usage", cmdPrefix + " set <seconds>"
                ));
                return;
            }

            if (seconds < 0) seconds = 0;
            config.setCooldownSeconds(seconds);

            messages.send(sender, "cooldown-set", Map.of(
                    "cooldown", String.valueOf(seconds)
            ));
            return;
        }

        // /cc set <command> <seconds> - cooldown dla komendy
        String targetCmd = args[1].toLowerCase(Locale.ROOT);
        int seconds;
        try {
            seconds = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            messages.send(sender, "invalid-arguments", Map.of(
                    "usage", cmdPrefix + " set <command> <seconds>"
            ));
            return;
        }

        if (seconds < 0) seconds = 0;
        config.setCommandCooldown(targetCmd, seconds);

        messages.send(sender, "cooldown-set-command", Map.of(
                "command", targetCmd,
                "cooldown", String.valueOf(seconds)
        ));
    }

    private void handleRemove(CommandSender sender, String[] args, String label) {
        if (!sender.hasPermission("commandcooldown.remove")) {
            messages.send(sender, "no-permission");
            return;
        }

        String cmdPrefix = getCommandPrefix(label);

        if (args.length < 2) {
            messages.send(sender, "invalid-arguments", Map.of(
                    "usage", cmdPrefix + " remove <command>"
            ));
            return;
        }

        String targetCmd = args[1].toLowerCase(Locale.ROOT);

        if (!config.hasCommandCooldown(targetCmd)) {
            messages.send(sender, "cooldown-not-found", Map.of("command", targetCmd));
            return;
        }

        config.removeCommandCooldown(targetCmd);
        messages.send(sender, "cooldown-removed", Map.of("command", targetCmd));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("commandcooldown.list")) {
            messages.send(sender, "no-permission");
            return;
        }

        Map<String, Integer> cooldowns = config.getCommandCooldowns();

        if (cooldowns.isEmpty()) {
            messages.send(sender, "cooldown-list-empty");
            return;
        }

        messages.sendPlain(sender, "cooldown-list-header");

        List<String> sorted = new ArrayList<>(cooldowns.keySet());
        Collections.sort(sorted);

        for (String cmd : sorted) {
            int cd = cooldowns.get(cmd);
            messages.sendPlain(sender, messages.getRaw("cooldown-list-entry"), Map.of(
                    "command", cmd,
                    "cooldown", String.valueOf(cd)
            ));
        }

        messages.sendPlain(sender, "cooldown-list-footer");
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("commandcooldown.reload")) {
            messages.send(sender, "no-permission");
            return;
        }
        try {
            plugin.reloadAll();
            messages.send(sender, "reload-success");
        } catch (Exception e) {
            messages.send(sender, "reload-error");
            e.printStackTrace();
        }
    }

    private void sendHelp(CommandSender sender) {
        messages.sendPlain(sender, "help-header");

        for (String line : messages.getList("help-commands")) {
            messages.sendPlain(sender, line);
        }

        messages.sendPlain(sender, "help-footer");
    }

    // Zwraca prefix komendy w odpowiednim języku
    private String getCommandPrefix(String label) {
        String lang = config.getLanguage();
        label = label.toLowerCase(Locale.ROOT);

        // Jeśli użyto polskiego aliasu, zwróć polski prefix
        if (label.equals("ok") || label.equals("opoznieniekomend")) {
            return "/ok";
        }
        // Jeśli użyto angielskiego aliasu, zwróć angielski prefix
        if (label.equals("cc") || label.equals("commandcooldown")) {
            return "/cc";
        }

        // Domyślnie według języka w configu
        return lang.equals("en") ? "/cc" : "/ok";
    }

    private boolean isPolishAlias(String alias) {
        String lower = alias.toLowerCase(Locale.ROOT);
        return lower.equals("ok") || lower.equals("opoznieniekomend");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // baza: nie pokazuj nic osobom bez głównej permisji
        if (!sender.hasPermission("commandcooldown.command")) {
            return Collections.emptyList();
        }

        // język tabów zależny od aliasu (tak jak chciałeś wcześniej)
        boolean pl = isPolishAlias(alias);

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();

            // pomoc/help – zwykle bez osobnej permisji (jeśli chcesz, możesz też wymagać "commandcooldown.info" itp.)
            subs.add(pl ? "pomoc" : "help");

            if (sender.hasPermission("commandcooldown.info")) {
                subs.add("info"); // to samo PL/EN
            }
            if (sender.hasPermission("commandcooldown.set")) {
                subs.add(pl ? "ustaw" : "set");
            }
            if (sender.hasPermission("commandcooldown.remove")) {
                subs.add(pl ? "usun" : "remove");
            }
            if (sender.hasPermission("commandcooldown.list")) {
                subs.add(pl ? "lista" : "list");
            }
            if (sender.hasPermission("commandcooldown.reload")) {
                subs.add(pl ? "przeladuj" : "reload");
            }

            String token = args[0].toLowerCase(Locale.ROOT);
            return subs.stream()
                    .filter(s -> s.startsWith(token))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2 && (sub.equals("usun") || sub.equals("remove"))) {
            if (!sender.hasPermission("commandcooldown.remove")) {
                return Collections.emptyList();
            }

            String token = args[1].toLowerCase(Locale.ROOT);
            return config.getCommandCooldowns().keySet().stream()
                    .filter(s -> s.startsWith(token))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (sub.equals("ustaw") || sub.equals("set"))) {
            if (!sender.hasPermission("commandcooldown.set")) {
                return Collections.emptyList();
            }

            String token = args[1].toLowerCase(Locale.ROOT);

            List<String> suggestions = new ArrayList<>(Arrays.asList("0", "1", "3", "5", "10", "30", "60"));
            suggestions.addAll(config.getCommandCooldowns().keySet());

            return suggestions.stream()
                    .map(String::toLowerCase)
                    .filter(s -> s.startsWith(token))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && (sub.equals("ustaw") || sub.equals("set"))) {
            if (!sender.hasPermission("commandcooldown.set")) {
                return Collections.emptyList();
            }

            String token = args[2].toLowerCase(Locale.ROOT);

            List<String> suggestions = Arrays.asList("1", "3", "5", "10", "30", "60", "120");
            return suggestions.stream()
                    .filter(s -> s.startsWith(token))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}