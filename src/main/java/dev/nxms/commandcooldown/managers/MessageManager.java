package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final CommandCooldown plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix;

    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;

    public MessageManager(CommandCooldown plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        prefix = messages.getString("prefix", "&8[&6CommandCooldown&8] ");
    }

    public void reload() {
        loadMessages();
    }

    public String getRaw(String path) {
        return messages.getString(path, "&cBrak wiadomości: " + path);
    }

    public List<String> getRawList(String path) {
        return messages.getStringList(path);
    }

    public Component parse(String message) {
        if (message.contains("<") && message.contains(">")) {
            return miniMessage.deserialize(message);
        } else {
            return legacySerializer.deserialize(message);
        }
    }

    public String replacePlaceholders(String message, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, new HashMap<>());
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        String message = getRaw(path);
        message = replacePlaceholders(message, placeholders);

        Component component = parse(prefix + message);
        sender.sendMessage(component);
    }

    public void sendRaw(CommandSender sender, String message) {
        Component component = parse(message);
        sender.sendMessage(component);
    }

    public void sendRaw(CommandSender sender, String message, Map<String, String> placeholders) {
        message = replacePlaceholders(message, placeholders);
        Component component = parse(message);
        sender.sendMessage(component);
    }

    public void sendActionBar(Player player, String path, Map<String, String> placeholders) {
        String message = getRaw(path);
        message = replacePlaceholders(message, placeholders);

        Component component = parse(message);
        player.sendActionBar(component);
    }

    public void sendTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        ConfigManager configManager = plugin.getConfigManager();

        String titleText = getRaw(titlePath);
        String subtitleText = getRaw(subtitlePath);

        titleText = replacePlaceholders(titleText, placeholders);
        subtitleText = replacePlaceholders(subtitleText, placeholders);

        Component title = parse(titleText);
        Component subtitle = parse(subtitleText);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(configManager.getTitleFadeIn() * 50L),
                Duration.ofMillis(configManager.getTitleStay() * 50L),
                Duration.ofMillis(configManager.getTitleFadeOut() * 50L)
        );

        player.showTitle(Title.title(title, subtitle, times));
    }

    public void sendCooldownMessage(Player player, String command, long remainingSeconds) {
        ConfigManager configManager = plugin.getConfigManager();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("command", command);
        placeholders.put("remaining", formatTime(remainingSeconds));
        placeholders.put("player", player.getName());

        if (configManager.isUseActionBar()) {
            sendActionBar(player, "cooldown-actionbar", placeholders);
        } else if (configManager.isUseTitle()) {
            sendTitle(player, "cooldown-title", "cooldown-subtitle", placeholders);
        } else {
            send(player, "cooldown-active", placeholders);
        }
    }

    public String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();

        String daysFormat = getRaw("time-format.days");
        String hoursFormat = getRaw("time-format.hours");
        String minutesFormat = getRaw("time-format.minutes");
        String secondsFormat = getRaw("time-format.seconds");

        if (days > 0) sb.append(days).append(daysFormat);
        if (hours > 0) sb.append(hours).append(hoursFormat);
        if (minutes > 0) sb.append(minutes).append(minutesFormat);
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append(secondsFormat);

        return sb.toString();
    }

    public String getPrefix() {
        return prefix;
    }
}