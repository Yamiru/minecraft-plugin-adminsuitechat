package sk.yamiru.adminsuitechat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

public class ChannelCommand implements CommandExecutor {

    private final AdminSuiteChat plugin;
    private final ChatChannel channel;

    public ChannelCommand(AdminSuiteChat plugin, ChatChannel channel) {
        this.plugin = plugin;
        this.channel = channel;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if plugin is globally enabled
        if (!plugin.isPluginEnabled()) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("plugin-disabled-global"));
            return true;
        }

        // Players only
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Permission check
        if (!player.hasPermission(channel.getPermission())) {
            sender.sendMessage(plugin.getMessageUtils().getMessage("no-permission"));
            return true;
        }

        // Check if message provided
        if (args.length == 0) {
            player.sendMessage(plugin.getMessageUtils().colorize(
                "&cUsage: /" + command.getName() + " <message>"
            ));
            return true;
        }

        // Anti-spam check
        if (!plugin.getAntiSpamManager().canSendMessage(player)) {
            player.sendMessage(plugin.getMessageUtils().getMessage("anti-spam-blocked"));
            plugin.debugMessage(player.getName() + " blocked by anti-spam");
            return true;
        }

        // Join message
        String message = String.join(" ", args);

        // Security: Sanitize message
        message = sanitizeMessage(message);

        if (message.isEmpty()) {
            return true;
        }

        // Record message for anti-spam
        plugin.getAntiSpamManager().recordMessage(player);

        // Format message for players
        String formattedMessage = plugin.getMessageUtils().formatMessage(
            player,
            channel.getFormat(),
            channel.getPrefix(),
            message
        );

        // Format message for console
        String consoleMessage = plugin.getMessageUtils().formatConsoleMessage(
            channel.getConsoleFormat(),
            player.getName(),
            message
        );

        // Send to channel
        sendToChannel(formattedMessage, channel.getPermission());

        // Log to file
        if (plugin.getConfig().getBoolean("logging.enabled", true)) {
            boolean logToFile = plugin.getConfig().getBoolean(
                (channel.isCustom() ? "custom-channels." : "channels.") + 
                channel.getId() + ".log-to-file", true
            );

            if (logToFile) {
                plugin.getLogManager().logToFile(channel.getId(), player.getName(), message);
            }
        }

        // Log to console if enabled
        if (plugin.getConfig().getBoolean("logging.console.enabled", true)) {
            plugin.consoleMessage(consoleMessage);
        }

        plugin.debugMessage("Message sent to channel " + channel.getId() + " via command by " + player.getName());

        return true;
    }

    /**
     * Send message to channel
     */
    private void sendToChannel(String chatMessage, String permission) {
        // Send to players with permission
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission(permission)) {
                onlinePlayer.sendMessage(chatMessage);
            }
        }
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
