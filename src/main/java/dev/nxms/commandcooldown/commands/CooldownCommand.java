package dev.nxms.commandcooldown.commands;

import dev.nxms.commandcooldown.CommandCooldown;
import dev.nxms.commandcooldown.managers.ConfigManager;
import dev.nxms.commandcooldown.managers.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

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
            case "pomoc" -> sendHelp(sender);

            case "info" -> {
                if (!sender.hasPermission("commandcooldown.info")) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                messages.send(sender, "cooldown-info", Map.of(
                        "cooldown", String.valueOf(config.getCooldownSeconds())
                ));
            }

            case "ustaw" -> {
                if (!sender.hasPermission("commandcooldown.set")) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                if (args.length < 2) {
                    messages.send(sender, "invalid-arguments", Map.of("usage", "/ok ustaw <sekundy>"));
                    return true;
                }

                int seconds;
                try {
                    seconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    messages.send(sender, "invalid-arguments", Map.of("usage", "/ok ustaw <sekundy>"));
                    return true;
                }

                if (seconds < 0) seconds = 0;
                config.setCooldownSeconds(seconds);

                messages.send(sender, "cooldown-set", Map.of(
                        "cooldown", String.valueOf(seconds)
                ));
            }

            case "przeladuj" -> {
                if (!sender.hasPermission("commandcooldown.reload")) {
                    messages.send(sender, "no-permission");
                    return true;
                }
                try {
                    plugin.reloadAll();
                    messages.send(sender, "reload-success");
                } catch (Exception e) {
                    messages.send(sender, "reload-error");
                    e.printStackTrace();
                }
            }

            default -> messages.send(sender, "invalid-command");
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        messages.sendPlain(sender, "help-header");

        for (String line : messages.getList("help-commands")) {
            messages.sendPlainText(sender, line); // <- WAŻNE: to jest tekst, nie klucz
        }

        messages.sendPlain(sender, "help-footer");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("pomoc", "info", "ustaw", "przeladuj");
            return subs.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("ustaw")) {
            return Arrays.asList("0", "1", "2", "3", "5", "10");
        }
        return Collections.emptyList();
    }
}