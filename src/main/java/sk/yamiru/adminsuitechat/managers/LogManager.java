package sk.yamiru.adminsuitechat.managers;

import org.bukkit.Bukkit;
import sk.yamiru.adminsuitechat.AdminSuiteChat;
import sk.yamiru.adminsuitechat.models.ChatChannel;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class LogManager {

    private final AdminSuiteChat plugin;
    private Path logDirectory;
    private Path archiveDirectory;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat filenameDateFormat;
    private boolean enabled;

    public LogManager(AdminSuiteChat plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        enabled = plugin.getConfig().getBoolean("logging.enabled", true);
        if (!enabled) {
            return;
        }

        // Setup directories
        logDirectory = plugin.getDataFolder().toPath().resolve("logs");
        archiveDirectory = plugin.getDataFolder().toPath().resolve("logs").resolve("archives");

        // Create directories if they don't exist
        try {
            Files.createDirectories(logDirectory);
            Files.createDirectories(archiveDirectory);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create log directories: " + e.getMessage());
            enabled = false;
            return;
        }

        // Setup date formats
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        filenameDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

        plugin.debugMessage("LogManager initialized - Directory: " + logDirectory.toString());

        // Archives are created only on server shutdown
        // During server runtime, logs are written to logs/ folder
        
        // Clean old archives based on max-archives limit if they exist
        cleanArchivesByCount();
    }

    /**
     * Log message to file
     */
    public void logToFile(String channelId, String playerName, String message) {
        if (!enabled) return;

        // Get channel from ChannelManager
        ChatChannel channel = plugin.getChannelManager().getChannel(channelId);
        if (channel == null) return;

        // Log to channel-specific file if enabled
        if (channel.isLogToFile()) {
            writeToFile(channel.getLogFilename(), channelId, playerName, message);
        }
    }

    /**
     * Log HelpMe request to file
     */
    public void logHelpMe(String helpmeId, String playerName, String message) {
        if (!enabled) return;

        // Check if logging is enabled for this helpme channel
        boolean logRequests = plugin.getConfig().getBoolean("helpme." + helpmeId + ".log-requests", false);
        if (!logRequests) return;

        // Write to helpme1.log (or helpme2.log, etc.)
        writeToFile(helpmeId + ".log", helpmeId.toUpperCase(), playerName, message);
    }

    /**
     * Write to specific log file (async, non-blocking)
     * Runtime rotation disabled - logs only rotated on server shutdown
     */
    private void writeToFile(String filename, String channel, String playerName, String message) {
        // Run async to avoid blocking main thread
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path logFile = logDirectory.resolve(filename);
                
                // Format log entry: [timestamp] [CHANNEL] player: message
                String timestamp = dateFormat.format(new Date());
                String logEntry = String.format("[%s] [%s] %s: %s%n", 
                        timestamp, channel.toUpperCase(), playerName, message);
                
                // Use BufferedWriter for better performance
                try (BufferedWriter writer = Files.newBufferedWriter(logFile, 
                        StandardOpenOption.CREATE, 
                        StandardOpenOption.APPEND)) {
                    writer.write(logEntry);
                }
                
                plugin.debugMessage("Logged to file: " + filename);
                
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to write to log file " + filename + ": " + e.getMessage());
            }
        });
    }

    /**
     * Rotate logs and compress them - all logs into one archive with rotation
     */
    public void rotateAndCompress() {
        if (!enabled) return;
        if (!plugin.getConfig().getBoolean("logging.rotation-enabled", true)) return;

        try {
            // Get all .log files
            java.util.List<Path> logFiles = Files.walk(logDirectory, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .toList();
            
            if (logFiles.isEmpty()) {
                plugin.debugMessage("No log files to compress");
                return;
            }

            plugin.consoleMessage("&eRotating and compressing log files...");

            // Get next archive number with rotation
            int nextNumber = getNextArchiveNumberWithRotation();
            String archiveName = "logs" + nextNumber + ".tar.gz";
            Path archivePath = archiveDirectory.resolve(archiveName);

            // Delete old archive if it exists (rotation overwrites)
            if (Files.exists(archivePath)) {
                Files.delete(archivePath);
                plugin.debugMessage("Overwriting old archive: " + archiveName);
            }

            // Create tar.gz archive with ALL log files
            compressDirectory(logDirectory, archivePath);

            // Clear old logs
            clearDirectory(logDirectory);

            plugin.consoleMessage("&a✓ Compressed all logs to: " + archiveName);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to rotate logs: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get next archive number with rotation (1-5, then back to 1)
     * Example: 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1...
     */
    private int getNextArchiveNumberWithRotation() throws IOException {
        int maxArchives = plugin.getConfig().getInt("logging.max-archives", 5);
        int maxNumber = 0;
        
        if (Files.exists(archiveDirectory)) {
            // Find highest existing archive number
            for (Path path : Files.walk(archiveDirectory, 1)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().matches("logs\\d+\\.tar\\.gz"))
                    .toList()) {
                
                String filename = path.getFileName().toString();
                String numberStr = filename.substring(4, filename.length() - 7);
                
                try {
                    int number = Integer.parseInt(numberStr);
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Increment and rotate (1-5, then back to 1)
        int nextNumber = maxNumber + 1;
        
        // If we exceed max, rotate back to 1
        if (nextNumber > maxArchives) {
            nextNumber = 1;
        }
        
        plugin.debugMessage("Next archive number: " + nextNumber + " (max: " + maxArchives + ")");
        return nextNumber;
    }

    /**
     * Compress directory to tar.gz using proper TAR format
     */
    private void compressDirectory(Path sourceDir, Path targetFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(targetFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             BufferedOutputStream bos = new BufferedOutputStream(gzos)) {

            // Get all .log files
            java.util.List<Path> logFiles = Files.walk(sourceDir, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".log"))
                    .sorted()
                    .toList();

            if (logFiles.isEmpty()) {
                plugin.debugMessage("No log files to compress");
                return;
            }

            // Write TAR entries
            for (Path file : logFiles) {
                String fileName = file.getFileName().toString();
                byte[] fileContent = Files.readAllBytes(file);
                
                // Create TAR header (512 bytes)
                byte[] header = new byte[512];
                
                // File name (offset 0, length 100)
                byte[] nameBytes = fileName.getBytes("UTF-8");
                System.arraycopy(nameBytes, 0, header, 0, Math.min(nameBytes.length, 100));
                
                // File mode (offset 100, length 8) - "0000644\0"
                String mode = "0000644\0";
                System.arraycopy(mode.getBytes("UTF-8"), 0, header, 100, 8);
                
                // Owner ID (offset 108, length 8) - "0000000\0"
                String uid = "0000000\0";
                System.arraycopy(uid.getBytes("UTF-8"), 0, header, 108, 8);
                
                // Group ID (offset 116, length 8) - "0000000\0"
                String gid = "0000000\0";
                System.arraycopy(gid.getBytes("UTF-8"), 0, header, 116, 8);
                
                // File size in octal (offset 124, length 12)
                String size = String.format("%011o\0", fileContent.length);
                System.arraycopy(size.getBytes("UTF-8"), 0, header, 124, 12);
                
                // Modification time in octal (offset 136, length 12)
                long mtime = Files.getLastModifiedTime(file).toMillis() / 1000;
                String mtimeStr = String.format("%011o\0", mtime);
                System.arraycopy(mtimeStr.getBytes("UTF-8"), 0, header, 136, 12);
                
                // Checksum placeholder (offset 148, length 8) - fill with spaces first
                for (int i = 148; i < 156; i++) {
                    header[i] = ' ';
                }
                
                // Type flag (offset 156, length 1) - '0' for regular file
                header[156] = '0';
                
                // Calculate checksum
                int checksum = 0;
                for (byte b : header) {
                    checksum += (b & 0xFF);
                }
                
                // Write checksum (offset 148, length 8)
                String checksumStr = String.format("%06o\0 ", checksum);
                System.arraycopy(checksumStr.getBytes("UTF-8"), 0, header, 148, 8);
                
                // Write header
                bos.write(header);
                
                // Write file content
                bos.write(fileContent);
                
                // Pad to 512-byte boundary
                int remainder = fileContent.length % 512;
                if (remainder != 0) {
                    byte[] padding = new byte[512 - remainder];
                    bos.write(padding);
                }
                
                plugin.debugMessage("Added to archive: " + fileName);
            }
            
            // Write two empty 512-byte blocks to mark end of archive
            byte[] endMarker = new byte[1024];
            bos.write(endMarker);
            
        }
    }

    /**
     * Clear directory contents
     */
    private void clearDirectory(Path directory) throws IOException {
        // Only delete .log files in the logs directory, not in subdirectories (archives)
        Files.walk(directory, 1)  // maxDepth 1 = only current directory, not subdirectories
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log"))  // Only .log files
                .forEach(file -> {
                    try {
                        Files.delete(file);
                        plugin.debugMessage("Deleted log file: " + file.getFileName());
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to delete file: " + e.getMessage());
                    }
                });
    }

    /**
     * Clean old archives based on retention days
     */
    public void cleanOldArchives() {
        int retentionDays = plugin.getConfig().getInt("logging.archive-retention-days", 30);
        if (retentionDays <= 0) return; // Keep forever

        long cutoffTime = System.currentTimeMillis() - (retentionDays * 24L * 60 * 60 * 1000);
        int deletedCount = 0;

        try {
            java.util.List<Path> toDelete = Files.walk(archiveDirectory, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".tar.gz"))
                    .filter(path -> {
                        try {
                            long fileTime = Files.getLastModifiedTime(path).toMillis();
                            return fileTime < cutoffTime;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .toList();

            for (Path path : toDelete) {
                try {
                    long fileTime = Files.getLastModifiedTime(path).toMillis();
                    long ageInDays = (System.currentTimeMillis() - fileTime) / (24L * 60 * 60 * 1000);
                    Files.delete(path);
                    plugin.debugMessage("Deleted old archive: " + path.getFileName() + " (age: " + ageInDays + " days)");
                    deletedCount++;
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to delete archive: " + e.getMessage());
                }
            }
            
            if (deletedCount > 0) {
                plugin.consoleMessage("&a✓ Cleaned " + deletedCount + " old archive(s) (older than " + retentionDays + " days)");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to clean old archives: " + e.getMessage());
        }
    }

    /**
     * Manually clear all logs (command)
     */
    public void clearLogs() {
        if (!enabled) return;

        try {
            clearDirectory(logDirectory);
            plugin.consoleMessage("&a✓ Log files cleared!");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to clear logs: " + e.getMessage());
        }
    }

    /**
     * Compress old log files on server startup
     */
    private void compressOldLogsOnStartup() {
        try {
            // Check if there are any .log files in logs directory
            boolean hasLogFiles = Files.walk(logDirectory, 1)
                    .filter(Files::isRegularFile)
                    .anyMatch(path -> path.toString().endsWith(".log"));

            if (!hasLogFiles) {
                plugin.debugMessage("No log files to compress on startup");
                return;
            }

            plugin.consoleMessage("&eCompressing old log files from previous session...");

            // Get next archive number with rotation
            int nextNumber = getNextArchiveNumberWithRotation();
            String archiveName = "logs" + nextNumber + ".tar.gz";
            Path archivePath = archiveDirectory.resolve(archiveName);
            
            // Delete old archive if it exists (rotation overwrites)
            if (Files.exists(archivePath)) {
                Files.delete(archivePath);
                plugin.debugMessage("Overwriting old archive: " + archiveName);
            }

            // Create tar.gz archive
            compressDirectory(logDirectory, archivePath);

            // Clear old logs (but not the archives folder)
            clearOldLogs();

            plugin.consoleMessage("&a✓ Compressed old logs to: " + archiveName);

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to compress old logs on startup: " + e.getMessage());
        }
    }


    /**
     * Clear old log files (but keep archives folder)
     */
    private void clearOldLogs() throws IOException {
        Files.walk(logDirectory, 1)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".log"))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        plugin.debugMessage("Deleted old log: " + path.getFileName());
                    } catch (IOException e) {
                        plugin.getLogger().warning("Failed to delete log file: " + e.getMessage());
                    }
                });
    }

    /**
     * Clean archives based on max count limit
     * With rotation, this usually won't delete anything since we overwrite old archives
     */
    private void cleanArchivesByCount() {
        int maxArchives = plugin.getConfig().getInt("logging.max-archives", 5);
        if (maxArchives <= 0) return; // Keep all

        try {
            // Get all archive files sorted by number (oldest first)
            java.util.List<Path> archives = Files.walk(archiveDirectory, 1)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches("logs\\d+\\.tar\\.gz"))
                    .sorted((p1, p2) -> {
                        String n1 = p1.getFileName().toString().substring(4, p1.getFileName().toString().length() - 7);
                        String n2 = p2.getFileName().toString().substring(4, p2.getFileName().toString().length() - 7);
                        return Integer.compare(Integer.parseInt(n1), Integer.parseInt(n2));
                    })
                    .toList();

            // With rotation, we should only have max archives
            // But if someone changes max-archives to smaller number, clean up extras
            int toDelete = archives.size() - maxArchives;
            if (toDelete > 0) {
                plugin.consoleMessage("&eCleaning old archives (keeping last " + maxArchives + ")...");
                
                for (int i = 0; i < toDelete; i++) {
                    Path archive = archives.get(i);
                    Files.delete(archive);
                    plugin.consoleMessage("&7Deleted old archive: " + archive.getFileName());
                }
                
                plugin.consoleMessage("&a✓ Cleaned " + toDelete + " old archive(s)");
            }

        } catch (IOException e) {
            plugin.getLogger().warning("Failed to clean archives by count: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
