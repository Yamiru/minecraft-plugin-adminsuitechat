package sk.yamiru.adminsuitechat.managers;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChannelManager {

    private final AdminSuiteChat plugin;
    private final Map<String, ChatChannel> channels;
    private final Map<String, String> shortcutToChannel;
    private YamlConfiguration channelsConfig;

    public ChannelManager(AdminSuiteChat plugin) {
        this.plugin = plugin;
        this.channels = new LinkedHashMap<>();
        this.shortcutToChannel = new HashMap<>();
        loadChannelsConfig();
    }

    private void loadChannelsConfig() {
        File channelsFile = new File(plugin.getDataFolder(), "channels.yml");
        
        if (!channelsFile.exists()) {
            plugin.saveResource("channels.yml", false);
        }
        
        channelsConfig = YamlConfiguration.loadConfiguration(channelsFile);
    }

    public void reloadChannelsConfig() {
        loadChannelsConfig();
        loadChannels();
    }

    public void loadChannels() {
        channels.clear();
        shortcutToChannel.clear();

        // Load staff channels
        ConfigurationSection channelsSection = channelsConfig.getConfigurationSection("channels");
        if (channelsSection != null) {
            for (String key : channelsSection.getKeys(false)) {
                loadChannel("channels." + key, key, false);
            }
        }

        // Load custom channels
        ConfigurationSection customSection = channelsConfig.getConfigurationSection("custom-channels");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                loadChannel("custom-channels." + key, key, true);
            }
        }

        plugin.getLogger().info("Loaded " + channels.size() + " channels");
    }

    private void loadChannel(String path, String id, boolean isCustom) {
        if (!channelsConfig.getBoolean(path + ".enabled", true)) {
            plugin.debugMessage("Channel " + id + " is disabled, skipping");
            return;
        }

        String shortcut = channelsConfig.getString(path + ".shortcut", "@" + id);
        String command = channelsConfig.getString(path + ".command", id);
        String permission = channelsConfig.getString(path + ".permission", "adminsuitechat." + id);
        String prefix = channelsConfig.getString(path + ".prefix", "&7[" + id.toUpperCase() + "]&r");
        String format = channelsConfig.getString(path + ".format", "{prefix} {player}: {message}");
        String consoleFormat = channelsConfig.getString(path + ".console-format", "[" + id.toUpperCase() + "] {player}: {message}");
        boolean logToFile = channelsConfig.getBoolean(path + ".log-to-file", true);
        String logFilename = channelsConfig.getString(path + ".log-filename", id + ".log");
        String luckPermsGroup = channelsConfig.getString(path + ".luckperms-group", "");

        ChatChannel channel = new ChatChannel(
                id, shortcut, command, permission, prefix, format,
                consoleFormat, logToFile, logFilename, luckPermsGroup, isCustom
        );

        channels.put(id, channel);
        shortcutToChannel.put(shortcut.toLowerCase(), id);
        
        plugin.debugMessage("Loaded channel: " + id + " | shortcut: '" + shortcut + "' | command: /" + command + " | permission: " + permission);
    }

    public ChatChannel getChannel(String id) {
        return channels.get(id);
    }

    public ChatChannel getChannelByShortcut(String shortcut) {
        String channelId = shortcutToChannel.get(shortcut.toLowerCase());
        return channelId != null ? channels.get(channelId) : null;
    }

    public Collection<ChatChannel> getAllChannels() {
        return channels.values();
    }

    public List<ChatChannel> getStaffChannels() {
        List<ChatChannel> staffChannels = new ArrayList<>();
        for (ChatChannel channel : channels.values()) {
            if (!channel.isCustom()) {
                staffChannels.add(channel);
            }
        }
        return staffChannels;
    }

    public List<ChatChannel> getCustomChannels() {
        List<ChatChannel> customChannels = new ArrayList<>();
        for (ChatChannel channel : channels.values()) {
            if (channel.isCustom()) {
                customChannels.add(channel);
            }
        }
        return customChannels;
    }

    public boolean channelExists(String id) {
        return channels.containsKey(id);
    }

    public Map<String, ChatChannel> getChannels() {
        return new LinkedHashMap<>(channels);
    }

    public void syncLuckPermsPermissions() {
        if (!plugin.isLuckPermsEnabled()) {
            return;
        }

        // Call LuckPerms integration sync method
        plugin.getLuckPermsIntegration().syncPermissions();
    }
}
