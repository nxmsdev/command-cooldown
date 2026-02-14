# CommandCooldown

A simple PaperMC command anti-spam (cooldown) plugin for Minecraft 1.21.1 servers.

## Features

- Global command cooldown (time between any commands)
- Per-command cooldown overrides (specific commands can have different cooldown than global)
- Bypass permission for staff/admins
- Language support (Polish / English) selectable in `config.yml`
- Messages stored in separate files: `messages_pl.yml` and `messages_en.yml`

## Permissions

| Permission | Description |
|:-|:-|
| `commandcooldown.info` | Allows using the info command (default: true) |
| `commandcooldown.set` | Allows setting global/per-command cooldowns |
| `commandcooldown.remove` | Allows removing per-command cooldown |
| `commandcooldown.list` | Allows listing per-command cooldowns |
| `commandcooldown.reload` | Allows reloading plugin config/messages |
| `commandcooldown.bypass` | Bypasses all cooldowns |
| `commandcooldown.admin` | Grants access to all CommandCooldown commands |

## Commands

Main command is `/commandcooldown` with aliases: `/cc`, `/ok`, `/opoznieniekomend`.

### English commands (recommended: `/cc` or `/commandcooldown`)

| Command | Description |
|:-|:-|
| `/cc help` | Shows help message |
| `/cc info` | Shows current global cooldown |
| `/cc set <seconds>` | Sets global cooldown |
| `/cc set <command> <seconds>` | Sets cooldown for a specific command |
| `/cc remove <command>` | Removes custom cooldown from a command |
| `/cc list` | Lists commands with custom cooldowns |
| `/cc reload` | Reloads config and messages |

### Polish commands (recommended: `/ok` or `/opoznieniekomend`)

| Command | Description |
|:-|:-|
| `/ok pomoc` | Wyświetla pomoc |
| `/ok info` | Pokazuje globalne opóźnienie |
| `/ok ustaw <sekundy>` | Ustawia globalne opóźnienie |
| `/ok ustaw <komenda> <sekundy>` | Ustawia opóźnienie dla komendy |
| `/ok usun <komenda>` | Usuwa indywidualne opóźnienie komendy |
| `/ok lista` | Lista komend z indywidualnym opóźnieniem |
| `/ok przeladuj` | Przeładowuje konfigurację i wiadomości |

### English ↔ Polish mapping

| English subcommand | Polish subcommand |
|:-|:-|
| `help` | `pomoc` |
| `set` | `ustaw` |
| `remove` | `usun` |
| `list` | `lista` |
| `reload` | `przeladuj` |
| `info` | `info` |

## Configuration

### Language selection

In `config.yml`:

```yml
language: pl   # or en
```
## Other

Author: [@nxmsdev](https://github.com/nxmsdev)

License: [CC BY-NC 4.0](https://creativecommons.org/licenses/by-nc/4.0/legalcode)
