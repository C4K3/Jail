package net.simpvp.Jail;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.plugin.java.JavaPlugin;

public class Jail extends JavaPlugin {

	public static Jail instance;

	public static HashSet<UUID> jailed_players = new HashSet<UUID>();

	public Jail() {
		instance = this;
	}

	public void onEnable() {
		Config.loadConfig();
		GeoIP.init();
		SQLite.connect();
		getServer().getPluginManager().registerEvents(new JailedEventListener(), this);
		getServer().getPluginManager().registerEvents(new PlayerLogin(), this);
		getServer().getPluginManager().registerEvents(new PlayerQuit(), this);
		getServer().getPluginManager().registerEvents(new AntiVPNCommand(instance), this);
		Commands commands = new Commands();
		getCommand("jail").setExecutor(commands);
		getCommand("unjail").setExecutor(commands);
		getCommand("jailinfo").setExecutor(commands);
		getCommand("jailreload").setExecutor(commands);
		getCommand("novpns").setExecutor(commands);
		getCommand("jailnotifications").setExecutor(commands);
	}

	public void onDisable() {
		SQLite.close();
		GeoIP.close();
	}

}
