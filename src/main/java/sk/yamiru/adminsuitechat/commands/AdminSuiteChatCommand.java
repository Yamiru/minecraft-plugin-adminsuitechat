package sk.yamiru.adminsuitechat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

import java.util.ArrayList;
import java.util.List;

public class AdminSuiteChatCommand implements CommandExecutor, TabCompleter {

    private final AdminSuiteChat plugin;

    public AdminSuiteChatCommand(AdminSuiteChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("adminsuitechat.reload")) {
                    sender.sendMessage(plugin.getMessageUtils().getMessage("no-permission"));
                    return true;
                }
                
                plugin.reloadConfiguration();
                sender.sendMessage(plugin.getMessageUtils().getMessage("config-reloaded"));
                return true;

            case "list":
                if (!sender.hasPermission("adminsuitechat.list")) {
                    sender.sendMessage(plugin.getMessageUtils().getMessage("no-permission"));
                    return true;
                }
                
                listChannels(sender);
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessageUtils().colorize("&6&l▬▬▬▬▬ AdminSuiteChat ▬▬▬▬▬"));
        sender.sendMessage(plugin.getMessageUtils().colorize("&e/asc reload &7- Reload configuration"));
        sender.sendMessage(plugin.getMessageUtils().colorize("&e/asc list &7- List all channels"));
        sender.sendMessage(plugin.getMessageUtils().colorize("&6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    private void listChannels(CommandSender sender) {
        sender.sendMessage(plugin.getMessageUtils().colorize("&6&l▬▬▬▬▬ Channel List ▬▬▬▬▬"));
        sender.sendMessage(" ");
        
        // Staff channels (len zapnuté)
        List<ChatChannel> staffChannels = plugin.getChannelManager().getStaffChannels();
        if (!staffChannels.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtils().colorize("&6&lStaff Channels:"));
            for (ChatChannel channel : staffChannels) {
                // Display: shortcut | command
                String usage = channel.getShortcut();
                if (channel.getCommand() != null && !channel.getCommand().isEmpty()) {
                    usage += " &8| &f/" + channel.getCommand();
                }
                
                sender.sendMessage(plugin.getMessageUtils().colorize(
                    " &8▸ &e" + channel.getId().toUpperCase() + " &7- &f" + usage
                ));
            }
            sender.sendMessage(" ");
        }
        
        // Custom channels (len zapnuté)
        List<ChatChannel> customChannels = plugin.getChannelManager().getCustomChannels();
        if (!customChannels.isEmpty()) {
            sender.sendMessage(plugin.getMessageUtils().colorize("&d&lCustom Channels:"));
            for (ChatChannel channel : customChannels) {
                // Display: shortcut | command
                String usage = channel.getShortcut();
                if (channel.getCommand() != null && !channel.getCommand().isEmpty()) {
                    usage += " &8| &f/" + channel.getCommand();
                }
                
                sender.sendMessage(plugin.getMessageUtils().colorize(
                    " &8▸ &e" + channel.getId().toUpperCase() + " &7- &f" + usage
                ));
            }
            sender.sendMessage(" ");
        }
        
        // Total
        int totalChannels = plugin.getChannelManager().getAllChannels().size();
        sender.sendMessage(plugin.getMessageUtils().colorize("&7Total: &f" + totalChannels + " channels"));
        
        // HelpMe
        sender.sendMessage(" ");
        sender.sendMessage(plugin.getMessageUtils().colorize("&6&lHelpMe:"));
        sender.sendMessage(plugin.getMessageUtils().colorize(" &8▸ &e/helpme <message>"));
        if (plugin.getConfig().getBoolean("helpme.helpme2.enabled", true)) {
            sender.sendMessage(plugin.getMessageUtils().colorize(" &8▸ &e/helpme2 <message>"));
        }
        
        sender.sendMessage(plugin.getMessageUtils().colorize("&6&l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("adminsuitechat.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("adminsuitechat.list")) {
                completions.add("list");
            }
        }
        
        return completions;
    }
}
