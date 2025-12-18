package sk.yamiru.adminsuitechat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import sk.yamiru.adminsuitechat.commands.AdminSuiteChatCommand;
import sk.yamiru.adminsuitechat.commands.HelpMeCommand;
import sk.yamiru.adminsuitechat.integrations.LuckPermsIntegration;
import sk.yamiru.adminsuitechat.integrations.AdminSuiteChatExpansion;
import sk.yamiru.adminsuitechat.listeners.ChatListener;
import sk.yamiru.adminsuitechat.managers.*;
import sk.yamiru.adminsuitechat.utils.ColorUtils;
import sk.yamiru.adminsuitechat.utils.MessageUtils;

public final class AdminSuiteChat extends JavaPlugin {

    private static AdminSuiteChat instance;
    private ChannelManager channelManager;
    private CooldownManager cooldownManager;
    private MessageUtils messageUtils;
    private ColorUtils colorUtils;
    private LogManager logManager;
    private AntiSpamManager antiSpamManager;
    private LuckPermsIntegration luckPermsIntegration;
    private CommandRegistry commandRegistry;
    
    private boolean placeholderAPIEnabled = false;
    private boolean luckPermsEnabled = false;
    private boolean coloredConsole = true;
    private boolean pluginEnabled = true;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Copy all language files
        copyLanguageFiles();
        
        // Check if plugin is globally enabled
        pluginEnabled = getConfig().getBoolean("settings.enabled", true);
        
        // Check console color setting
        coloredConsole = getConfig().getBoolean("settings.colored-console", true);
        
        // Initialize utilities
        this.colorUtils = new ColorUtils(this);
        this.messageUtils = new MessageUtils(this);
        this.channelManager = new ChannelManager(this);
        this.cooldownManager = new CooldownManager();
        this.antiSpamManager = new AntiSpamManager(this);
        this.logManager = new LogManager(this);
        this.commandRegistry = new CommandRegistry(this);
        
        // Load configuration
        channelManager.loadChannels();
        
        // Initialize integrations
        checkPlaceholderAPI();
        checkLuckPerms();
        
        // Register commands
        registerCommands();
        
        // Register channel commands dynamically
        if (getConfig().getBoolean("settings.use-channel-commands", true)) {
            commandRegistry.registerChannelCommands();
        }
        
        // Register listeners
        registerListeners();
        
        // Initialize logging
        if (getConfig().getBoolean("logging.enabled", true)) {
            logManager.initialize();
        }
        
        // Schedule automatic archive cleanup
        scheduleArchiveCleanup();
        
        // Print startup messages
        printStartupBanner();
    }
    
    private void copyLanguageFiles() {
        String[] languages = {
            "en_US", "sk_SK", "cs_CZ", "pl_PL", "ru_RU", 
            "de_DE", "fr_FR", "es_ES", "it_IT", "bg_BG",
            "sl_SI", "sr_RS", "pt_PT", "pt_BR", "nl_NL",
            "sv_SE", "da_DK", "no_NO", "fi_FI", "hu_HU",
            "ro_RO", "uk_UA", "hr_HR", "et_EE", "lt_LT",
            "zh_CN", "ja_JP", "ko_KR", "tr_TR", "ar_SA"
        };
        
        java.io.File langDir = new java.io.File(getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }
        
        int copiedCount = 0;
        for (String lang : languages) {
            String fileName = "lang/" + lang + ".yml";
            java.io.File langFile = new java.io.File(getDataFolder(), fileName);
            
            if (!langFile.exists()) {
                try {
                    saveResource(fileName, false);
                    copiedCount++;
                } catch (Exception e) {
                    getLogger().warning("Could not copy language file: " + fileName);
                }
            }
        }
        
        if (copiedCount > 0) {
            consoleMessage("&a✓ Copied " + copiedCount + " language files");
        }
    }

    @Override
    public void onDisable() {
        // Unregister channel commands
        if (commandRegistry != null) {
            commandRegistry.unregisterAllCommands();
        }
        
        // Rotate and compress logs on shutdown
        if (logManager != null && getConfig().getBoolean("logging.rotation-enabled", true)) {
            logManager.rotateAndCompress();
        }
        
        // Clear cooldowns
        if (cooldownManager != null) {
            cooldownManager.clearAll();
        }
        
        // Clear anti-spam data
        if (antiSpamManager != null) {
            antiSpamManager.clear();
        }
        
        consoleMessage("&c✗ AdminSuiteChat has been disabled!");
    }

    private void registerCommands() {
        getCommand("adminsuitechat").setExecutor(new AdminSuiteChatCommand(this));
        
        HelpMeCommand helpMeCommand = new HelpMeCommand(this);
        getCommand("helpme").setExecutor(helpMeCommand);
        getCommand("helpme2").setExecutor(helpMeCommand); // Same handler for both commands
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    private void checkPlaceholderAPI() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            
            // Register our own expansion
            if (new AdminSuiteChatExpansion(this).register()) {
                consoleMessage("&a✓ PlaceholderAPI detected - integration enabled!");
            }
        } else {
            consoleMessage("&7PlaceholderAPI not found - using basic formatting");
        }
    }

    private void checkLuckPerms() {
        try {
            // Try to get LuckPerms service - this will throw ClassNotFoundException if LP is not installed
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(luckPermsClass);
            
            if (provider != null && getConfig().getBoolean("settings.luckperms-sync", true)) {
                luckPermsEnabled = true;
                // Pass the provider object to LuckPermsIntegration which will handle the casting
                luckPermsIntegration = new LuckPermsIntegration(this, provider.getProvider());
                luckPermsIntegration.syncPermissions();
                consoleMessage("&a✓ LuckPerms detected - sync enabled!");
            } else {
                consoleMessage("&7LuckPerms found but sync disabled in config");
            }
        } catch (ClassNotFoundException e) {
            // LuckPerms not installed - this is fine
            consoleMessage("&7LuckPerms not found - sync disabled");
        } catch (Exception e) {
            // Any other error
            consoleMessage("&eLuckPerms integration error: " + e.getMessage());
            getLogger().warning("Could not integrate with LuckPerms: " + e.getMessage());
        }
    }

    private void scheduleArchiveCleanup() {
        if (!getConfig().getBoolean("logging.rotation.enabled", true)) {
            return;
        }
        
        int retentionDays = getConfig().getInt("logging.rotation.retention-days", 30);
        if (retentionDays <= 0) {
            debugMessage("Archive retention is disabled (retention-days = 0)");
            return;
        }
        
        // Run cleanup every 24 hours (1728000 ticks = 24 hours)
        long intervalTicks = 20L * 60L * 60L * 24L; // 24 hours in ticks
        
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (logManager != null && logManager.isEnabled()) {
                logManager.cleanOldArchives();
            }
        }, intervalTicks, intervalTicks); // Start after 24h, repeat every 24h
        
        debugMessage("Scheduled automatic archive cleanup every 24 hours");
    }

    private void printStartupBanner() {
        String banner = getConfig().getString("console.startup-banner", "")
                .replace("{version}", getDescription().getVersion());
        
        for (String line : banner.split("\n")) {
            consoleMessage(line);
        }
        consoleMessage(" ");
        
        int channelCount = channelManager.getAllChannels().size();
        consoleMessage(messageUtils.getMessage("channels-loaded", "{count}", String.valueOf(channelCount)));
        
        if (getConfig().getBoolean("logging.enabled", true)) {
            consoleMessage("&a✓ Logging system enabled");
        }
        
        if (getConfig().getBoolean("settings.debug", false)) {
            consoleMessage("&e⚠ Debug mode is ENABLED");
        }
        
        consoleMessage(" ");
        consoleMessage("&b━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        consoleMessage("&f  Plugin by &6yamiru");
        consoleMessage("&f  Visit &3yamiru.com");
        consoleMessage("&b━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        consoleMessage(" ");
    }

    public void reloadConfiguration() {
        reloadConfig();
        
        // Reload language
        messageUtils.loadLanguage();
        
        // Reload channels
        channelManager.reloadChannelsConfig();
        
        // Reload settings
        pluginEnabled = getConfig().getBoolean("settings.enabled", true);
        coloredConsole = getConfig().getBoolean("settings.colored-console", true);
        
        // Re-register channel commands
        if (commandRegistry != null && getConfig().getBoolean("settings.use-channel-commands", true)) {
            commandRegistry.registerChannelCommands();
        }
        
        // Reinitialize logging
        if (getConfig().getBoolean("logging.enabled", true)) {
            logManager.initialize();
        }
        
        // Resync LuckPerms if enabled
        if (luckPermsEnabled && luckPermsIntegration != null) {
            luckPermsIntegration.syncPermissions();
        }
        
        consoleMessage(messageUtils.getMessage("config-reloaded"));
    }

    public void togglePlugin() {
        pluginEnabled = !pluginEnabled;
        getConfig().set("settings.enabled", pluginEnabled);
        saveConfig();
    }

    /**
     * Send colored message to console
     */
    public void consoleMessage(String message) {
        if (coloredConsole) {
            getServer().getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', message)
            );
        } else {
            getServer().getConsoleSender().sendMessage(
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message))
            );
        }
    }

    /**
     * Send debug message to console
     */
    public void debugMessage(String message) {
        if (getConfig().getBoolean("settings.debug", false)) {
            consoleMessage("&7[DEBUG] &f" + message);
        }
    }

    // Getters
    public static AdminSuiteChat getInstance() {
        return instance;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public ColorUtils getColorUtils() {
        return colorUtils;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public AntiSpamManager getAntiSpamManager() {
        return antiSpamManager;
    }

    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled && getConfig().getBoolean("settings.use-placeholderapi", true);
    }

    public boolean isLuckPermsEnabled() {
        return luckPermsEnabled;
    }

    public boolean isColoredConsole() {
        return coloredConsole;
    }

    public boolean isPluginEnabled() {
        return pluginEnabled;
    }
}
