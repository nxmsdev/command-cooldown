package dev.nxms.commandcooldown.managers;

import dev.nxms.commandcooldown.CommandCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages plugin messages and translations.
 * Supports multiple languages, modular prefixes, and placeholder replacement.
 */
public class MessageManager {

    private final CommandCooldown plugin;
    private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

    private FileConfiguration messages;
    private String language;

    // Pattern to match {placeholder} format
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

    public MessageManager(CommandCooldown plugin) {
        this.plugin = plugin;
        reload();
    }

    /**
     * Loads/reloads messages from the appropriate language file.
     */
    public void reload() {
        // Save default message file if exists in JAR
        saveDefaultMessages("messages_en.yml");

        // Load configured language
        language = plugin.getConfigManager().getLanguage().toLowerCase();
        String fileName = "messages_" + language + ".yml";
        File file = new File(plugin.getDataFolder(), fileName);

        // Fallback to English if selected language doesn't exist
        if (!file.exists()) {
            plugin.getLogger().warning("Messages file " + fileName + " not found! Using messages_en.yml.");
            fileName = "messages_en.yml";
            file = new File(plugin.getDataFolder(), fileName);

            // Create English file if it doesn't exist
            if (!file.exists() && plugin.getResource("messages_en.yml") != null) {
                plugin.saveResource("messages_en.yml", false);
            }
        }

        messages = YamlConfiguration.loadConfiguration(file);

        // Load defaults from JAR
        InputStream defaultStream = plugin.getResource(fileName);
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
            );
            messages.setDefaults(defaultConfig);
        }

        plugin.getLogger().info("Messages file has been loaded (" + fileName + ").");
    }

    /**
     * Saves a default message file if it doesn't exist.
     * Only saves if the resource exists in the JAR.
     */
    private void saveDefaultMessages(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists() && plugin.getResource(fileName) != null) {
            plugin.saveResource(fileName, false);
            plugin.getLogger().info("Created default " + fileName + " file.");
        }
    }

    /**
     * Gets a raw message from config without any processing.
     */
    public String getRaw(String key) {
        return messages.getString(key, "");
    }

    /**
     * Checks if a key exists in the messages config.
     */
    public boolean hasKey(String key) {
        return messages.contains(key) && !getRaw(key).isEmpty();
    }

    /**
     * Gets a list of strings from config.
     */
    public List<String> getList(String key) {
        return messages.getStringList(key);
    }

    /**
     * Replaces {key} placeholders with values from messages file.
     * Only replaces placeholders that exist as keys in the config.
     * Prevents infinite recursion by tracking already processed keys.
     */
    private String replaceConfigPlaceholders(String message, Set<String> processedKeys) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fullMatch = matcher.group(0);  // {placeholder}
            String placeholder = matcher.group(1); // placeholder

            // Skip if already processed (prevents infinite recursion)
            if (processedKeys.contains(placeholder)) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(fullMatch));
                continue;
            }

            // Check if this placeholder exists in config
            if (!hasKey(placeholder)) {
                // Keep original placeholder for custom placeholders like {player}, {time}
                matcher.appendReplacement(result, Matcher.quoteReplacement(fullMatch));
                continue;
            }

            // Mark as processed
            Set<String> newProcessedKeys = new HashSet<>(processedKeys);
            newProcessedKeys.add(placeholder);

            // Get and recursively process the value
            String value = getRaw(placeholder);
            value = replaceConfigPlaceholders(value, newProcessedKeys);

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * Gets a formatted message with config placeholders replaced.
     */
    public String get(String key) {
        String message = getRaw(key);
        if (message.isEmpty()) {
            return "&cMissing message: " + key;
        }

        // Replace config placeholders (like {prefix}, {prefix-error}, etc.)
        Set<String> processedKeys = new HashSet<>();
        processedKeys.add(key); // Prevent self-reference
        message = replaceConfigPlaceholders(message, processedKeys);

        return message;
    }

    /**
     * Gets a formatted message with custom placeholders replaced.
     */
    public String get(String key, Map<String, String> placeholders) {
        String message = get(key);
        return applyPlaceholders(message, placeholders);
    }

    // ==================== SEND METHODS ====================

    /**
     * Sends a message to the sender.
     */
    public void send(CommandSender sender, String key) {
        send(sender, key, Map.of());
    }

    /**
     * Sends a message with placeholder replacements.
     */
    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = get(key, placeholders);
        sender.sendMessage(toComponent(msg));
    }

    // ==================== SEND TEXT METHODS ====================

    /**
     * Sends raw text (not from config) to sender.
     */
    public void sendText(CommandSender sender, String text) {
        sender.sendMessage(toComponent(text));
    }

    /**
     * Sends raw text with placeholder replacements.
     */
    public void sendText(CommandSender sender, String text, Map<String, String> placeholders) {
        text = applyPlaceholders(text, placeholders);
        sender.sendMessage(toComponent(text));
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Gets the current language code.
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Applies custom placeholders to a message.
     */
    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        for (var entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }

    /**
     * Converts legacy text with color codes to Adventure Component.
     */
    private Component toComponent(String legacyText) {
        return legacy.deserialize(legacyText);
    }

    /**
     * Helper method to create placeholder maps easily.
     * Usage: placeholders("player", "Steve", "time", "30")
     */
    public static Map<String, String> placeholders(String... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}