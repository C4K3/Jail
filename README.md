Jail
=====

A simple Bukkit plugin that lets admins jail players as punishment. 

Commands
-----
All commands are usable only be ops.

```
/jail <player> <reason> [-a]
```
Jail *player* for *reason*. To also publically announce it, use the *-a* switch.

```
/unjail <player>
```
Unjail the given *player*. Works also for offline players.

```
/jailinfo [player]
```
If used without arguments, gives general info about the jail. If a player is specified, says when that player was jailed for what and by whom.

```
/jailreload
```
- Reloads the configuration.

Installation
-----
Drop the jar into the plugins folder, the next time the server is started the plugin will load with default settings, and create a config.yml in the Jail directory.

Configuration
-----
The plugin is configured using the config.yml file. It should be automatically generated. All possible settings are described in comments in the config file, and can also be viewed in the config.yml file here.
