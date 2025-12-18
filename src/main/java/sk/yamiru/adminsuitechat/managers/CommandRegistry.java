package sk.yamiru.adminsuitechat.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.commands.ChannelCommand;
import sk.yamiru.adminsuitechat.models.ChatChannel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CommandRegistry {

    private final AdminSuiteChat plugin;
    private final List<String> registeredCommands;
    private CommandMap commandMap;

    public CommandRegistry(AdminSuiteChat plugin) {
        this.plugin = plugin;
        this.registeredCommands = new ArrayList<>();
        this.commandMap = getCommandMap();
    }

    /**
     * Get Bukkit's CommandMap via reflection
     */
    private CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to get CommandMap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Register all channel commands dynamically
     */
    public void registerChannelCommands() {
        if (commandMap == null) {
            plugin.getLogger().severe("CommandMap is null - cannot register channel commands!");
            return;
        }

        // Unregister old commands first
        unregisterAllCommands();

        int registeredCount = 0;

        for (ChatChannel channel : plugin.getChannelManager().getAllChannels()) {
            String commandName = channel.getCommand();

            if (commandName == null || commandName.isEmpty()) {
                plugin.debugMessage("Skipping command registration for channel " + channel.getId() + " - no command name");
                continue;
            }

            // Create and register command
            if (registerChannelCommand(commandName, channel)) {
                registeredCount++;
                registeredCommands.add(commandName);
            }
        }

        plugin.consoleMessage("&a✓ Registered " + registeredCount + " channel commands");
    }

    /**
     * Register a single channel command
     */
    private boolean registerChannelCommand(String commandName, ChatChannel channel) {
        try {
            // Create command
            ChannelCommandWrapper cmd = new ChannelCommandWrapper(
                commandName,
                "Send message to " + channel.getId().toUpperCase() + " channel",
                "/" + commandName + " <message>",
                new ChannelCommand(plugin, channel)
            );

            // Register with Bukkit
            commandMap.register("adminsuitechat", cmd);

            plugin.debugMessage("Registered command: /" + commandName + " for channel " + channel.getId());
            return true;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register command /" + commandName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Unregister all channel commands
     */
    public void unregisterAllCommands() {
        if (commandMap == null || registeredCommands.isEmpty()) {
            return;
        }

        try {
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var knownCommands = (java.util.Map<String, Command>) knownCommandsField.get(commandMap);

            for (String cmdName : registeredCommands) {
                Command cmd = knownCommands.remove(cmdName);
                if (cmd != null) {
                    cmd.unregister(commandMap);
                    plugin.debugMessage("Unregistered command: /" + cmdName);
                }
                // Also remove with plugin prefix
                knownCommands.remove("adminsuitechat:" + cmdName);
            }

            registeredCommands.clear();

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to unregister commands: " + e.getMessage());
        }
    }

    /**
     * Inner class for command wrapper
     */
    private static class ChannelCommandWrapper extends Command {
        private final ChannelCommand executor;

        public ChannelCommandWrapper(String name, String description, String usage, ChannelCommand executor) {
            super(name);
            this.setDescription(description);
            this.setUsage(usage);
            this.executor = executor;
        }

        @Override
        public boolean execute(org.bukkit.command.CommandSender sender, String commandLabel, String[] args) {
            return executor.onCommand(sender, this, commandLabel, args);
        }
    }
}
