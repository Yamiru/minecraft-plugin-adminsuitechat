package sk.yamiru.adminsuitechat.integrations;

import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

import java.lang.reflect.Method;

public class LuckPermsIntegration {

    private final AdminSuiteChat plugin;
    private final Object luckPerms;

    public LuckPermsIntegration(AdminSuiteChat plugin, Object luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    /**
     * Sync channel permissions with LuckPerms groups using reflection
     */
    public void syncPermissions() {
        try {
            plugin.consoleMessage("&eSyncing permissions with LuckPerms...");
            
            int syncedCount = 0;
            
            // Get GroupManager using reflection
            Method getGroupManagerMethod = luckPerms.getClass().getMethod("getGroupManager");
            Object groupManager = getGroupManagerMethod.invoke(luckPerms);
            
            for (ChatChannel channel : plugin.getChannelManager().getAllChannels()) {
                String groupName = channel.getLuckPermsGroup();
                
                if (groupName != null && !groupName.isEmpty()) {
                    try {
                        // Get group by name
                        Method getGroupMethod = groupManager.getClass().getMethod("getGroup", String.class);
                        Object group = getGroupMethod.invoke(groupManager, groupName);
                        
                        if (group != null) {
                            // Get group data
                            Method dataMethod = group.getClass().getMethod("data");
                            Object groupData = dataMethod.invoke(group);
                            
                            // Build permission node
                            Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                            Method builderMethod = nodeClass.getMethod("builder", String.class);
                            Object nodeBuilder = builderMethod.invoke(null, channel.getPermission());
                            
                            Method buildMethod = nodeBuilder.getClass().getMethod("build");
                            Object permissionNode = buildMethod.invoke(nodeBuilder);
                            
                            // Add permission to group
                            Method addMethod = groupData.getClass().getMethod("add", nodeClass);
                            addMethod.invoke(groupData, permissionNode);
                            
                            // Save group using UserManager
                            Method getUserManagerMethod = luckPerms.getClass().getMethod("getGroupManager");
                            Object userManager = getUserManagerMethod.invoke(luckPerms);
                            Method saveGroupMethod = userManager.getClass().getMethod("saveGroup", group.getClass());
                            saveGroupMethod.invoke(userManager, group);
                            
                            syncedCount++;
                            plugin.debugMessage("Synced permission " + channel.getPermission() + " to group " + groupName);
                        }
                    } catch (Exception e) {
                        plugin.debugMessage("Could not sync group " + groupName + ": " + e.getMessage());
                    }
                }
            }
            
            if (syncedCount > 0) {
                plugin.consoleMessage("&a✓ Synced " + syncedCount + " permission(s) with LuckPerms");
            } else {
                plugin.consoleMessage("&7No permissions to sync with LuckPerms");
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to sync with LuckPerms: " + e.getMessage());
            plugin.consoleMessage("&cFailed to sync with LuckPerms - check console for details");
        }
    }
}
