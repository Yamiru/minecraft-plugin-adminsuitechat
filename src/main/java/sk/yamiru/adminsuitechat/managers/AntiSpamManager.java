package sk.yamiru.adminsuitechat.managers;

import org.bukkit.entity.Player;
import sk.yamiru.adminsuitechat.AdminSuiteChat;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpamManager {

    private final AdminSuiteChat plugin;
    private final Map<UUID, MessageTracker> trackers;
    private boolean enabled;
    private int maxMessagesPerSecond;
    private int blockDuration;
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes
    private long lastCleanup = System.currentTimeMillis();

    public AntiSpamManager(AdminSuiteChat plugin) {
        this.plugin = plugin;
        this.trackers = new ConcurrentHashMap<>();
        loadSettings();
    }

    private void loadSettings() {
        enabled = plugin.getConfig().getBoolean("settings.anti-spam.enabled", true);
        maxMessagesPerSecond = plugin.getConfig().getInt("settings.anti-spam.max-messages-per-second", 3);
        blockDuration = plugin.getConfig().getInt("settings.anti-spam.block-duration-seconds", 5);
    }

    /**
     * Check if player is allowed to send message
     */
    public boolean canSendMessage(Player player) {
        if (!enabled) return true;
        if (player.hasPermission("adminsuitechat.bypass.antispam")) return true;

        // Automatic cleanup of old trackers
        cleanupIfNeeded();

        UUID playerId = player.getUniqueId();
        MessageTracker tracker = trackers.computeIfAbsent(playerId, k -> new MessageTracker());

        return tracker.canSendMessage();
    }

    /**
     * Record message sent by player
     */
    public void recordMessage(Player player) {
        if (!enabled) return;

        UUID playerId = player.getUniqueId();
        MessageTracker tracker = trackers.computeIfAbsent(playerId, k -> new MessageTracker());
        tracker.recordMessage();
    }

    /**
     * Cleanup inactive trackers to save memory
     */
    private void cleanupIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanup < CLEANUP_INTERVAL) return;

        lastCleanup = currentTime;
        trackers.entrySet().removeIf(entry -> entry.getValue().isInactive(currentTime));
    }

    /**
     * Clear all tracking data
     */
    public void clear() {
        trackers.clear();
    }

    /**
     * Inner class to track messages per player
     * Optimized using circular buffer
     */
    private class MessageTracker {
        private final long[] messageTimes;
        private int currentIndex;
        private long blockedUntil;
        private long lastActivity;

        public MessageTracker() {
            this.messageTimes = new long[maxMessagesPerSecond];
            this.currentIndex = 0;
            this.blockedUntil = 0;
            this.lastActivity = System.currentTimeMillis();
        }

        public boolean canSendMessage() {
            long currentTime = System.currentTimeMillis();
            lastActivity = currentTime;

            // Check if player is currently blocked
            if (currentTime < blockedUntil) {
                return false;
            }

            // Check if too many messages in last second
            long oneSecondAgo = currentTime - 1000;
            int recentMessages = 0;

            for (long messageTime : messageTimes) {
                if (messageTime > oneSecondAgo) {
                    recentMessages++;
                }
            }

            if (recentMessages >= maxMessagesPerSecond) {
                // Block player for specified duration
                blockedUntil = currentTime + (blockDuration * 1000L);
                return false;
            }

            return true;
        }

        public void recordMessage() {
            messageTimes[currentIndex] = System.currentTimeMillis();
            currentIndex = (currentIndex + 1) % maxMessagesPerSecond;
            lastActivity = System.currentTimeMillis();
        }

        public boolean isInactive(long currentTime) {
            // Remove tracker if player hasn't sent message in 10 minutes
            return currentTime - lastActivity > 600000;
        }
    }
}
