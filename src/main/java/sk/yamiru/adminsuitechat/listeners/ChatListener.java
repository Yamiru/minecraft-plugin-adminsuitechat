package sk.yamiru.adminsuitechat.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

public class ChatListener implements Listener {

    private final AdminSuiteChat plugin;

    public ChatListener(AdminSuiteChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Check if plugin is globally enabled
        if (!plugin.isPluginEnabled()) {
            return;
        }

        String message = event.getMessage();
        Player player = event.getPlayer();
        
        // Debug: Show what message we received
        plugin.debugMessage("Chat event: " + player.getName() + " said: " + message);

        // Security: Check message length to prevent spam
        if (message.length() > 256) {
            player.sendMessage(plugin.getMessageUtils().getMessage("message-too-long"));
            event.setCancelled(true);
            return;
        }

        // Check if message starts with channel shortcut
        for (ChatChannel channel : plugin.getChannelManager().getAllChannels()) {
            String shortcut = channel.getShortcut();
            
            // Debug: Show checking
            plugin.debugMessage("Checking shortcut: '" + shortcut + "' against message: '" + message + "'");
            
            // Check if message starts with shortcut
            // Support both "@a message" and "@amessage" formats
            boolean matchesExact = message.startsWith(shortcut + " ");
            boolean matchesNoSpace = message.length() > shortcut.length() && 
                                    message.startsWith(shortcut) && 
                                    message.charAt(shortcut.length()) != ' ';
            
            if (matchesExact || (matchesNoSpace && message.length() > shortcut.length() + 1)) {
                event.setCancelled(true);
                
                plugin.debugMessage("Shortcut matched! Channel: " + channel.getId());
                
                // Strip color codes from message to prevent abuse
                message = stripColorCodes(message);
                
                // Anti-spam check
                if (!plugin.getAntiSpamManager().canSendMessage(player)) {
                    player.sendMessage(plugin.getMessageUtils().getMessage("anti-spam-blocked"));
                    plugin.debugMessage(player.getName() + " blocked by anti-spam");
                    return;
                }
                
                // Permission check
                if (!player.hasPermission(channel.getPermission())) {
                    player.sendMessage(plugin.getMessageUtils().getMessage("no-permission"));
                    plugin.debugMessage(player.getName() + " tried to use channel " + channel.getId() + " without permission");
                    return;
                }
                
                // Extract message after shortcut
                String actualMessage;
                if (matchesExact) {
                    actualMessage = message.substring(shortcut.length() + 1).trim();
                } else {
                    actualMessage = message.substring(shortcut.length()).trim();
                }
                
                if (actualMessage.isEmpty()) {
                    player.sendMessage(plugin.getMessageUtils().colorize("&cUsage: " + shortcut + " <message>"));
                    return;
                }
                
                // Security: Additional sanitization
                actualMessage = sanitizeMessage(actualMessage);
                
                // Record message for anti-spam
                plugin.getAntiSpamManager().recordMessage(player);
                
                // Format message for players
                String formattedMessage = plugin.getMessageUtils().formatMessage(
                    player,
                    channel.getFormat(),
                    channel.getPrefix(),
                    actualMessage
                );
                
                // Send to channel
                sendToChannel(formattedMessage, channel.getPermission());
                
                // Log to file if enabled for this channel
                if (plugin.getLogManager() != null && plugin.getLogManager().isEnabled() && channel.isLogToFile()) {
                    plugin.getLogManager().logToFile(channel.getId(), player.getName(), actualMessage);
                }
                
                // Log to console with colors
                String consoleMsg = channel.getConsoleFormat()
                    .replace("{player}", player.getName())
                    .replace("{message}", actualMessage);
                plugin.consoleMessage(consoleMsg);  // Use consoleMessage for colored output
                
                plugin.debugMessage("Message sent to channel " + channel.getId() + " by " + player.getName());
                return;
            }
        }
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
     * Strip color codes from user input
     */
    private String stripColorCodes(String message) {
        return message.replaceAll("&[0-9a-fk-or]", "");
    }

    /**
     * Sanitize message to prevent exploits
     */
    private String sanitizeMessage(String message) {
        // Remove any potential command injections
        message = message.replaceAll("[/\\\\]", "");
        
        // Limit consecutive special characters
        message = message.replaceAll("([!@#$%^&*()_+=\\[\\]{}|;:'\",.<>?/\\\\`~-])\\1{3,}", "$1$1$1");
        
        return message.trim();
    }
}
