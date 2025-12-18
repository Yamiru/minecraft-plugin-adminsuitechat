package sk.yamiru.adminsuitechat.integrations;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

public class AdminSuiteChatExpansion extends PlaceholderExpansion {

    private final AdminSuiteChat plugin;

    public AdminSuiteChatExpansion(AdminSuiteChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "adminsuitechat";
    }

    @Override
    public @NotNull String getAuthor() {
        return "yamiru";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        // %adminsuitechat_hasaccess_<channel>%
        if (identifier.startsWith("hasaccess_")) {
            String channelId = identifier.substring("hasaccess_".length());
            ChatChannel channel = plugin.getChannelManager().getChannel(channelId);
            if (channel != null) {
                return player.hasPermission(channel.getPermission()) ? "true" : "false";
            }
            return "false";
        }

        // %adminsuitechat_channel_count%
        if (identifier.equals("channel_count")) {
            return String.valueOf(plugin.getChannelManager().getAllChannels().size());
        }

        // %adminsuitechat_accessible_channels%
        if (identifier.equals("accessible_channels")) {
            int count = 0;
            for (ChatChannel channel : plugin.getChannelManager().getAllChannels()) {
                if (player.hasPermission(channel.getPermission())) {
                    count++;
                }
            }
            return String.valueOf(count);
        }

        // %adminsuitechat_isadmin%
        if (identifier.equals("isadmin")) {
            return player.hasPermission("adminsuitechat.admin") ? "true" : "false";
        }

        // %adminsuitechat_ismoderator%
        if (identifier.equals("ismoderator")) {
            return player.hasPermission("adminsuitechat.moderator") ? "true" : "false";
        }

        // %adminsuitechat_ishelper%
        if (identifier.equals("ishelper")) {
            return player.hasPermission("adminsuitechat.helper") ? "true" : "false";
        }

        // %adminsuitechat_status%
        if (identifier.equals("status")) {
            return plugin.isPluginEnabled() ? "enabled" : "disabled";
        }

        return null;
    }
}
