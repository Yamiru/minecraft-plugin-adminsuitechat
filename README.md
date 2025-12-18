# AdminSuiteChat

<div align="center">

![Version](https://img.shields.io/badge/version-1.0-blue)
![Minecraft](https://img.shields.io/badge/minecraft-1.19--1.21-green)
![License](https://img.shields.io/badge/license-MIT-green)

**Professional Admin Communication Plugin for Minecraft Servers**

[Features](#features) • [Installation](#installation) • [Configuration](#configuration) • [Commands](#commands) • [Support](#support)

</div>

---

## 📋 Overview

AdminSuiteChat is a comprehensive admin communication plugin designed for professional Minecraft server management. It provides dedicated chat channels for different staff roles, an anonymous help request system, and advanced logging capabilities.

### ✨ Key Features

- **9 Pre-configured Channels**: Admin, Developer, Moderator, Helper, Builder, Staff, VIP, VIP+, and 3 custom channels
- **Smart Shortcuts**: Quick access with `@a`, `@d`, `@m`, etc.
- **HelpMe System**: Anonymous help requests with cooldown protection
- **Advanced Logging**: Automatic log rotation with TAR.GZ compression
- **30 Languages**: Full multilingual support with auto-detection
- **LuckPerms Integration**: Optional permission sync (works without it too)
- **PlaceholderAPI Support**: Display player info in messages
- **Anti-Spam Protection**: Rate limiting and flood prevention
- **Colored Console**: Easy-to-read colored console output
- **Async Operations**: No server lag from logging operations

---

## 🚀 Features

### Communication Channels

Each channel has:
- ✅ Unique permission nodes
- ✅ Custom color schemes
- ✅ Dedicated shortcuts
- ✅ Optional logging
- ✅ LuckPerms group sync

**Pre-configured Channels:**
- `@a` - Admin channel (red)
- `@d` - Developer channel (cyan)
- `@m` - Moderator channel (blue)
- `@h` - Helper channel (green)
- `@b` - Builder channel (orange)
- `@s` - Staff channel (yellow)
- `@v` - VIP channel (purple)
- `@vp` - VIP+ channel (dark purple)
- `@c1` - Custom channel 1 (gray)

### HelpMe System

Professional help request system with:
- Anonymous requests (player name hidden from sender)
- Configurable cooldowns
- Bypass permissions for staff
- Automatic logging
- Console notifications
- Multi-instance support (helpme1, helpme2)

### Logging System

Advanced logging with:
- **Automatic Rotation**: Creates archives on server shutdown
- **TAR.GZ Compression**: Industry-standard format
- **Smart Cleanup**: Keeps only last N archives (configurable)
- **Async Writing**: No performance impact
- **Per-Channel Logs**: Separate logs for each channel

**Archive Rotation Example:**
```
Shutdown 1 → logs1.tar.gz
Shutdown 2 → logs2.tar.gz
...
Shutdown 5 → logs5.tar.gz
Shutdown 6 → logs1.tar.gz (overwrites, rotation begins)
```

---

## 📦 Installation

### Requirements

- **Minecraft**: 1.19 - 1.21+
- **Server**: Spigot, Paper, Purpur, or Folia
- **Java**: 17 or higher

### Optional Dependencies

- **LuckPerms**: For permission group sync
- **PlaceholderAPI**: For placeholder support

### Setup

1. **Download** the latest release
2. **Place** `AdminSuiteChat-1.0.jar` in your `plugins/` folder
3. **Restart** your server
4. **Configure** the plugin in `plugins/AdminSuiteChat/config.yml`
5. **Reload** with `/asc reload`

---

## ⚙️ Configuration

### Basic Configuration

```yaml
# Main Settings
settings:
  enabled: true
  debug: false
  colored-console: true
  language: en_US
  luckperms-sync: true

# Logging Configuration
logging:
  enabled: true
  rotation-enabled: true
  max-archives: 5  # Keep last 5 archives
  archive-compression: true

# HelpMe System
helpme:
  helpme1:
    enabled: true
    cooldown-seconds: 60
    log-requests: true
```

### Channel Configuration

Edit `plugins/AdminSuiteChat/channels.yml`:

```yaml
channels:
  admin:
    enabled: true
    shortcut: "@a"
    permission: "adminsuitechat.admin"
    format: "&4&l[ADMIN]&r &c{player}&8: &f{message}"
    log-to-file: false
    luckperms-group: "admin"
```

---

## 🎮 Commands

### Main Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/asc reload` | Reload configuration | `adminsuitechat.reload` |
| `/asc list` | show /asc list | `adminsuitechat.list` |

### Channel Commands

| Command | Shortcut | Description | Permission |
|---------|----------|-------------|------------|
| `/admin <message>` | `@a <message>` | Send to admin channel | `adminsuitechat.admin` |
| `/developer <message>` | `@d <message>` | Send to developer channel | `adminsuitechat.developer` |
| `/moderator <message>` | `@m <message>` | Send to moderator channel | `adminsuitechat.moderator` |
| `/helper <message>` | `@h <message>` | Send to helper channel | `adminsuitechat.helper` |
| `/builder <message>` | `@b <message>` | Send to builder channel | `adminsuitechat.builder` |
| `/staff <message>` | `@s <message>` | Send to staff channel | `adminsuitechat.staff` |
| `/vip <message>` | `@v <message>` | Send to VIP channel | `adminsuitechat.vip` |
| `/vipplus <message>` | `@vp <message>` | Send to VIP+ channel | `adminsuitechat.vipplus` |

### HelpMe Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/helpme <message>` | Request help from staff | `adminsuitechat.helpme` |
| `/helpme2 <message>` | Alternative help channel | `adminsuitechat.helpme2` |

**Bypass Cooldown**: `adminsuitechat.helpme.bypass`

---

## 🔐 Permissions

### Main Permissions

```yaml
adminsuitechat.*                    # All permissions
adminsuitechat.reload               # Reload configuration
adminsuitechat.admin                # Access admin channel
adminsuitechat.developer            # Access developer channel
adminsuitechat.moderator            # Access moderator channel
adminsuitechat.helper               # Access helper channel
adminsuitechat.builder              # Access builder channel
adminsuitechat.staff                # Access staff channel
adminsuitechat.vip                  # Access VIP channel
adminsuitechat.vipplus              # Access VIP+ channel
adminsuitechat.helpme               # Use /helpme command
adminsuitechat.helpme.see           # See helpme requests
adminsuitechat.helpme.bypass        # Bypass cooldown
adminsuitechat.list                 # Use /asc list or /adminsuitechat list
```

### Permission Setup (LuckPerms)

```bash
# Admin group
lp group admin permission set adminsuitechat.admin
lp group admin permission set adminsuitechat.helpme.see

# Moderator group
lp group moderator permission set adminsuitechat.moderator
lp group moderator permission set adminsuitechat.helpme.see

# VIP group
lp group vip permission set adminsuitechat.vip
```

---

## 📊 Technical Details

### Performance

- **Async Logging**: All file operations run asynchronously
- **Optimized I/O**: BufferedWriter for efficient file writing
- **No TPS Impact**: Logging operations don't affect server performance
- **Smart Caching**: Minimal memory footprint

### Archive System

**Format**: TAR.GZ (GNU tar compatible)
**Rotation**: Circular (1→2→3→4→5→1...)
**Compression**: GZIP with standard TAR headers
**Extraction**: `tar -xzf logs1.tar.gz`

### Compatibility

- ✅ Spigot 1.19+
- ✅ Paper 1.19+
- ✅ Purpur 1.19+
- ✅ Works with/out LuckPerms
- ✅ Works with/out PlaceholderAPI

---

## 🌍 Supported Languages

30 languages with auto-detection:

`en_US`, `sk_SK`, `cs_CZ`, `pl_PL`, `ru_RU`, `de_DE`, `fr_FR`, `es_ES`, `it_IT`, `pt_PT`, `pt_BR`, `nl_NL`, `da_DK`, `sv_SE`, `no_NO`, `fi_FI`, `hu_HU`, `ro_RO`, `bg_BG`, `hr_HR`, `sr_RS`, `uk_UA`, `et_EE`, `lv_LV`, `lt_LT`, `el_GR`, `tr_TR`, `ar_SA`, `zh_CN`, `ja_JP`, `ko_KR`

---

## 🛠️ Configuration Examples

### Example 1: Disable Channel Logging

```yaml
channels:
  admin:
    log-to-file: false  # No logs for admin channel
```

### Example 2: Custom Cooldown

```yaml
helpme:
  helpme1:
    cooldown-seconds: 120  # 2 minutes
```

### Example 3: More Archives

```yaml
logging:
  max-archives: 10  # Keep last 10 archives
```

### Example 4: Custom Channel

```yaml
custom-channels:
  support:
    enabled: true
    shortcut: "@sup"
    command: "support"
    permission: "adminsuitechat.support"
    format: "&e[SUPPORT] {player}: {message}"
```

---

## 🐛 Troubleshooting

### Common Issues

**Issue**: Archives not created
**Solution**: Check `logging.rotation-enabled: true` in config.yml

**Issue**: LuckPerms error on startup
**Solution**: Plugin works without LuckPerms. Disable sync with `luckperms-sync: false`

**Issue**: Cannot extract archives
**Solution**: Use `tar -xzf logs1.tar.gz` or 7-Zip on Windows

### Debug Mode

Enable debug logging:
```yaml
settings:
  debug: true
```

Then check console for detailed logs.

---

## 📞 Support

### Links

- **Website**: [Modrinth](https://modrinth.com/project/adminsuitechat)
- **Issues**: [GitHub Issues](https://github.com/yamiru/adminsuitechat/issues)
- **Wiki**: [Full Documentation](https://github.com/yamiru/adminsuitechat/wiki)

### Contact

For support, please:
1. Check this README
2. Read the [Wiki](https://github.com/yamiru/adminsuitechat/wiki)
3. Search [existing issues](https://github.com/yamiru/adminsuitechat/issues)
4. Create a [new issue](https://github.com/yamiru/adminsuitechat/issues/new) if needed

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Credits

**Made with ❤️ by [yamiru](https://yamiru.com)**

Special thanks to:
- Spigot/Paper community
- LuckPerms developers
- PlaceholderAPI developers
- All beta testers

---

## 📈 Statistics

- **Lines of Code**: 2,500+
- **Languages Supported**: 30
- **Channels Available**: 9 + 3 custom
- **Commands**: 15+
- **Permissions**: 20+

---

<div align="center">

**[⬆ Back to Top](#adminsuitechat)**

Made with ❤️ by yamiru • [yamiru.com](https://yamiru.com)

</div>
