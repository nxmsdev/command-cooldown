package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final CommandCooldown plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    private FileConfiguration messages;
    private String prefix;

    public MessageManager(CommandCooldown plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String lang = plugin.getConfigManager().getLanguage();

        // Zapisz oba pliki jeśli nie istnieją
        saveDefaultMessages("messages_pl.yml");
        saveDefaultMessages("messages_en.yml");

        // Wybierz odpowiedni plik
        String fileName = lang.equals("en") ? "messages_en.yml" : "messages_pl.yml";
        File file = new File(plugin.getDataFolder(), fileName);

        this.messages = YamlConfiguration.loadConfiguration(file);
        this.prefix = messages.getString("prefix", "&8[&6CommandCooldown&8] ");
    }

    private void saveDefaultMessages(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    public String getRaw(String key) {
        return messages.getString(key, "&cMissing message: " + key);
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