package sk.yamiru.adminsuitechat.models;

public class ChatChannel {

    private final String id;
    private final String shortcut;
    private final String command;
    private final String permission;
    private final String prefix;
    private final String format;
    private final String consoleFormat;
    private final boolean logToFile;
    private final String logFilename;
    private final String luckPermsGroup;
    private final boolean isCustom;

    public ChatChannel(String id, String shortcut, String command, String permission, String prefix, 
                      String format, String consoleFormat, boolean logToFile, String logFilename,
                      String luckPermsGroup, boolean isCustom) {
        this.id = id;
        this.shortcut = shortcut;
        this.command = command;
        this.permission = permission;
        this.prefix = prefix;
        this.format = format;
        this.consoleFormat = consoleFormat;
        this.logToFile = logToFile;
        this.logFilename = logFilename;
        this.luckPermsGroup = luckPermsGroup;
        this.isCustom = isCustom;
    }

    public String getId() {
        return id;
    }

    public String getShortcut() {
        return shortcut;
    }

    public String getCommand() {
        return command;
    }

    public String getPermission() {
        return permission;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFormat() {
        return format;
    }

    public String getConsoleFormat() {
        return consoleFormat;
    }

    public boolean isLogToFile() {
        return logToFile;
    }

    public String getLogFilename() {
        return logFilename;
    }

    public String getLuckPermsGroup() {
        return luckPermsGroup;
    }

    public boolean isCustom() {
        return isCustom;
    }

    @Override
    public String toString() {
        return "ChatChannel{" +
                "id='" + id + '\'' +
                ", shortcut='" + shortcut + '\'' +
                ", command='" + command + '\'' +
                ", permission='" + permission + '\'' +
                ", logToFile=" + logToFile +
                ", isCustom=" + isCustom +
                '}';
    }
}
