package dev.nxms.commandcooldown.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final JavaPlugin plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    private FileConfiguration messages;
    private String prefix;

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        this.prefix = messages.getString("prefix", "&8[&6CommandCooldown&8] ");
    }

    public String getRaw(String key) {
        return messages.getString(key, "&cBrak wiadomości: " + key);
    }

    public List<String> getList(String key) {
        return messages.getStringList(key);
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = getRaw(key);
        msg = apply(msg, placeholders);
        sender.sendMessage(toComponent(prefix + msg));
    }

    public void sendPlain(CommandSender sender, String key) {
        sendPlain(sender, key, Map.of());
    }

    public void sendPlain(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = getRaw(key);
        msg = apply(msg, placeholders);
        sender.sendMessage(toComponent(msg));
    }

    public void sendPlainText(CommandSender sender, String text) {
        sender.sendMessage(toComponent(text));
    }

    public void sendPlainText(CommandSender sender, String text, Map<String, String> placeholders) {
        text = apply(text, placeholders);
        sender.sendMessage(toComponent(text));
    }

    private String apply(String text, Map<String, String> placeholders) {
        for (var e : placeholders.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }
        return text;
    }

    private Component toComponent(String legacyText) {
        return legacy.deserialize(legacyText);
    }
}