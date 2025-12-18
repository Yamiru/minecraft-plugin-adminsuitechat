package sk.yamiru.adminsuitechat.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sk.yamiru.adminsuitechat.AdminSuiteChat;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class MessageUtils {

    private final AdminSuiteChat plugin;
    private YamlConfiguration langConfig;
    private String currentLanguage;

    public MessageUtils(AdminSuiteChat plugin) {
        this.plugin = plugin;
        loadLanguage();
    }

    public void loadLanguage() {
        currentLanguage = plugin.getConfig().getString("settings.language", "en_US");
        
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        File langFile = new File(langFolder, currentLanguage + ".yml");
        
        // Copy from resources if doesn't exist
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + currentLanguage + ".yml", false);
        }
        
        // Load language file
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // Load defaults from resources
        try (InputStream stream = plugin.getResource("lang/" + currentLanguage + ".yml")) {
            if (stream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
                langConfig.setDefaults(defConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load default language file: " + e.getMessage());
        }
    }

    public String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String formatMessage(Player player, String format, String prefix, String message) {
        String formatted = format
                .replace("{prefix}", colorize(prefix))
                .replace("{player}", player.getName())
                .replace("{message}", message);

        // Apply PlaceholderAPI if available
        if (plugin.isPlaceholderAPIEnabled()) {
            formatted = PlaceholderAPI.setPlaceholders(player, formatted);
        }

        return colorize(formatted);
    }

    public String formatConsoleMessage(String format, String playerName, String message) {
        String formatted = format
                .replace("{player}", playerName)
                .replace("{message}", message);
        
        // For console, keep Minecraft colors but they'll be stripped by logger
        // This allows the format to work but outputs plain text
        return ChatColor.stripColor(colorize(formatted));
    }

    public String getMessage(String path) {
        String message = langConfig.getString("messages." + path);
        if (message == null) {
            plugin.getLogger().warning("Missing message key: " + path + " in language: " + currentLanguage);
            return colorize("&cMissing message: " + path);
        }
        return colorize(message);
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    public String getLanguageName() {
        return langConfig.getString("language.name", currentLanguage);
    }
}
