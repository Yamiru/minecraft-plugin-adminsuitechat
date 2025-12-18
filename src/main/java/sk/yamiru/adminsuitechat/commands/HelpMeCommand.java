package sk.yamiru.adminsuitechat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.yamiru.adminsuitechat.AdminSuiteChat;

public class HelpMeCommand implements CommandExecutor {

    private final AdminSuiteChat plugin;

    public HelpMeCommand(AdminSuiteChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if HelpMe is enabled
        if (!plugin.getConfig().getBoolean("helpme.helpme1.enabled", true)) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("helpme-disabled"));
            return true;
        }

        // Check permission
        if (!sender.hasPermission("adminsuitechat.helpme")) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("no-permission"));
            return true;
        }

        // Players only
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Check arguments
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("helpme-usage"));
            return true;
        }

        // Check cooldown (with bypass permission)
        boolean hasBypass = player.hasPermission("adminsuitechat.helpme.bypass");
        
        if (!hasBypass) {
            int cooldown = plugin.getConfig().getInt("helpme.helpme1.cooldown-seconds", 60);
            if (plugin.getCooldownManager().hasCooldown(player.getUniqueId())) {
                long remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId());
                sender.sendMessage(plugin.getMessageUtils().getMessage(
                    "helpme-cooldown",
                    "{time}", String.valueOf(remaining)
                ));
                return true;
            }
        }

        // Join message
        String message = String.join(" ", args);

        // Security: Sanitize message
        message = sanitizeMessage(message);

        // Check message length
        int maxLength = plugin.getConfig().getInt("helpme.helpme1.max-message-length", 256);
        if (message.length() > maxLength) {
            message = message.substring(0, maxLength);
        }

        // Format message for admins
        String prefix = plugin.getConfig().getString("helpme.helpme1.prefix", "&c&l[HELP]&r");
        String format = plugin.getConfig().getString("helpme.helpme1.format", "{prefix} &7{player} needs help: &f{message}");
        String formattedMessage = plugin.getMessageUtils().colorize(
            format.replace("{prefix}", prefix)
                  .replace("{player}", player.getName())
                  .replace("{message}", message)
        );

        // Send to admins
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("adminsuitechat.helpme.see")) {
                onlinePlayer.sendMessage(formattedMessage);
            }
        }

        // Log to console with colors
        String consoleFormat = plugin.getConfig().getString("helpme.helpme1.console-format", "&4[HELPME]&r {player}: {message}");
        String consoleMsg = consoleFormat
            .replace("{player}", player.getName())
            .replace("{message}", message);
        plugin.consoleMessage(consoleMsg);  // Use consoleMessage for colored output

        // Log to file
        if (plugin.getLogManager() != null && plugin.getLogManager().isEnabled()) {
            plugin.getLogManager().logHelpMe("helpme1", player.getName(), message);
        }

        // Always send confirmation to sender
        player.sendMessage(plugin.getMessageUtils().getMessage("helpme-sent"));
        
        // Set cooldown (if not bypassed)
        if (!hasBypass) {
            int cooldown = plugin.getConfig().getInt("helpme.helpme1.cooldown-seconds", 60);
            plugin.getCooldownManager().setCooldown(player.getUniqueId(), cooldown);
        }

        return true;
    }

    /**
     * Sanitize message to prevent exploits
     */
    private String sanitizeMessage(String message) {
        // Strip color codes
        message = message.replaceAll("&[0-9a-fk-or]", "");
        
        // Remove potential command injections
        message = message.replaceAll("[/\\\\]", "");
        
        // Limit length
        if (message.length() > 256) {
            message = message.substring(0, 256);
        }
        
        // Limit consecutive special characters
        message = message.replaceAll("([!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/\\\\`~-])\\1{3,}", "$1$1$1");
        
        return message.trim();
    }
}
