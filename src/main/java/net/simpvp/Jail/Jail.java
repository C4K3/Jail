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
		getCommand("jail").setExecutor(new Commands());
		getCommand("unjail").setExecutor(new Commands());
		getCommand("jailinfo").setExecutor(new Commands());
		getCommand("jailreload").setExecutor(new Commands());
		getCommand("novpns").setExecutor(new AntiVPNCommand(instance));
	}

	public void onDisable() {
		SQLite.close();
		GeoIP.close();
	}

}
