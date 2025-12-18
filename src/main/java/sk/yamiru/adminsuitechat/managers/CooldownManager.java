package sk.yamiru.adminsuitechat.managers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Long> cooldowns;

    public CooldownManager() {
        this.cooldowns = new ConcurrentHashMap<>();
    }

    public void setCooldown(UUID playerId, int seconds) {
        cooldowns.put(playerId, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean hasCooldown(UUID playerId) {
        Long expireTime = cooldowns.get(playerId);
        if (expireTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() >= expireTime) {
            cooldowns.remove(playerId);
            return false;
        }
        
        return true;
    }

    public long getRemainingCooldown(UUID playerId) {
        Long expireTime = cooldowns.get(playerId);
        if (expireTime == null) {
            return 0;
        }
        
        long remaining = (expireTime - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    public void removeCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void clearAll() {
        cooldowns.clear();
    }
}
