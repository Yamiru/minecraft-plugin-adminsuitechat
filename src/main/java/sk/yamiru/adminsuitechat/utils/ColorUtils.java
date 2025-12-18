package sk.yamiru.adminsuitechat.utils;

import org.bukkit.ChatColor;
import sk.yamiru.adminsuitechat.AdminSuiteChat;

public class ColorUtils {

    private final AdminSuiteChat plugin;

    public ColorUtils(AdminSuiteChat plugin) {
        this.plugin = plugin;
    }

    public String colorize(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return ChatColor.stripColor(colorize(message));
    }

    public String getConsoleFormat(String message) {
        if (plugin.isColoredConsole()) {
            return colorize(message);
        } else {
            return stripColor(message);
        }
    }
}
