package net.simpvp.Jail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.UUID;

import org.json.JSONObject;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Commands implements CommandExecutor {
	//FIXME: Write docs for entire class
	private static Jail plugin = Jail.instance;

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		Player player = null;
		if (sender instanceof Player){
			player = (Player) sender;
		}

		if (player != null && !player.isOp()) {
			send_message("You do not have permission to use this command.", player, ChatColor.RED);
			return true;
		}

		try {
			if (label.equals("jail")) {
				jail_player(player, args);
			} else if (label.equals("unjail")) {
				unjail_player(player, args);
			} else if (label.equals("jailinfo")) {
				show_info(player, args);
			} else if (label.equals("jailreload")) {
				Config.loadConfig();
				GeoIP.init();
				AntiVPNCommand.hours_required = plugin.getConfig().getInt("novpns");;
				send_message("Jail config has been reloaded.", player, ChatColor.GOLD);
			}
		} catch (java.sql.SQLException e) {
			Jail.instance.getLogger().severe("Error handling command " + label + ": " + e);
			e.printStackTrace();
			send_message("Encountered exception running command.", player, ChatColor.RED);
		}

		return true;
	}

	private void send_message(String message, Player player, ChatColor color) {
		plugin.getLogger().info(message);
		if (player != null)
			player.sendMessage(color + message);
	}

	public static void announce_message(String message, Player player) {
		String playername = "";
		if (player == null) {
			playername = "CONSOLE";
		} else {
			playername = player.getName();
		}

		plugin.getLogger().info("[" + playername + ": " + message + "]");
		for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
			if (onlinePlayer.isOp()) {
				onlinePlayer.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "[" + playername + ": " + message + "]");
			}
		}
	}

	private void jail_player(Player player, String[] args) {
		if (args.length < 2) {
			send_message("Invalid usage. Correct usage is /jail <player> <reason>", player, ChatColor.RED);
			return;
		}

		@SuppressWarnings("deprecation") /* Warnings have been read */
		Player target = plugin.getServer().getPlayer(args[0]);

		UUID jailer_uuid = null;
		String jailer = null;
		if (player == null) {
			jailer = "CONSOLE";
		} else {
			jailer = player.getName();
			jailer_uuid = player.getUniqueId();
		}

		boolean announce = false;
		String reason = "";
		for (int i = 1; i < args.length; i++) {
			if (args[i].equals("-a")) {
				announce = true;
			} else {
				reason += " " + args[i];
			}
		}

		if (reason.equals("")) {
			send_message("Invalid usage. You must specify a reason for jailing.", player, ChatColor.RED);
			return;
		}

		if (target == null) {
			get_uuid(args[0], reason, jailer_uuid, jailer, announce);
		} else {
			effect_jail(target.getUniqueId(), target.getName(), reason, jailer_uuid, jailer, announce);
		}

	}

	private void get_uuid(final String target_name, final String reason, final UUID jailer_uuid, final String jailer, final boolean announce) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + target_name);
					URLConnection conn = url.openConnection();
					conn.connect();
					BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
					String resp = reader.readLine();
					if (resp == null) {
						Jail.instance.getLogger().severe("Empty response from mojang API");
						send_error(target_name, jailer_uuid);
						return;
					}

					JSONObject j = new JSONObject(resp);
					final String name = j.getString("name");
					String uuid_str = j.getString("id");
					uuid_str = uuid_str.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");

					final UUID uuid = UUID.fromString(uuid_str);
					new BukkitRunnable() {
						@Override
						public void run() {
							effect_jail(uuid, name, reason,
									jailer_uuid, jailer, announce);
						}
					}.runTask(plugin);

				} catch (Exception e) {
					Jail.instance.getLogger().severe("Error getting player UUID: " + e);
					e.printStackTrace();
					send_error(target_name, jailer_uuid);
				}

			}

			private void send_error(final String target_name, final UUID jailer_uuid) {
				new BukkitRunnable() {
					@Override
					public void run() {
						send_message("Error retrieving data for " + target_name,
								plugin.getServer().getPlayer(jailer_uuid),
								ChatColor.RED);
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);

	}

	/**
	 * Actually carries out the jailing of the player.
	 */
	private void effect_jail(UUID uuid, String target_name, String reason,
			UUID jailer_uuid, String sjailer, boolean announce) {

		Player jailer = null;
		if (jailer_uuid != null) {
			jailer = plugin.getServer().getPlayer(jailer_uuid);
			if (jailer != null)
				sjailer = jailer.getName();
		}

		JailedPlayer tmp = SQLite.get_jailed_player(uuid);
		if (tmp != null) {
			if (tmp.to_be_released) {
				SQLite.set_has_been_online(uuid);
				announce_message(tmp.playername
						+ " was set to be unjailed, undid that so they will remain jailed.", jailer);
			} else {
				send_message("That player is already jailed, see /jailinfo " + tmp.playername + " for more information.", jailer, ChatColor.RED);
			}
			return;
		}

		OfflinePlayer offline_tmp = plugin.getServer().getOfflinePlayer(uuid);
		if (offline_tmp.getFirstPlayed() == 0) {
			send_message("That player has never played on this server, you cannot jail them. If you wish to prevent them from joining, ban them instead.", jailer, ChatColor.RED);
			return;
		}

		boolean is_online;
		Location location;
		Player target = plugin.getServer().getPlayer(uuid);
		if (target == null) {
			is_online = false;
			location = new Location(plugin.getServer().getWorld("world"), 0, 63, 0);
		} else {
			is_online = true;
			location = target.getLocation();
			target_name = target.getName();
		}

		JailedPlayer jailedplayer = new JailedPlayer(uuid, target_name,
				reason, sjailer, location,
				(int) (System.currentTimeMillis() / 1000L),
				false, is_online);

		if (target != null) {
			target.teleport(Config.get_spawn());
			jailedplayer.add();
			SQLite.insert_ip_jailed(target);
		}

		announce_message("Jailing " + target_name + " for:" + reason, jailer);
		if (announce) {
			for (Player p : plugin.getServer().getOnlinePlayers())
				p.sendMessage(ChatColor.LIGHT_PURPLE + "[Server] Jailing " + target_name + " for:" + reason);
		} else if (target != null) {
			target.sendMessage(ChatColor.RED + "You have been jailed for:" + reason);
		}

		jailedplayer.insert();
	}

	private void unjail_player(Player player, String[] args) throws java.sql.SQLException {
		@SuppressWarnings("deprecation") /* deprecated for reasons not relevant here */
		Player target = plugin.getServer().getPlayer(args[0]);

		if (target != null) {
			JailedPlayer jailedplayer = SQLite.get_jailed_player(target.getUniqueId());

			if (jailedplayer != null) {
				announce_message("Unjailing " + target.getName(), player);
				target.teleport(jailedplayer.location);
				Jail.jailed_players.remove(jailedplayer.uuid);
				SQLite.delete_player_info(jailedplayer.uuid);
				target.sendMessage(ChatColor.GREEN + "You have been released from jail.");
				return;
			}
		}

		ArrayList<JailedPlayer> jailed_players = SQLite.get_jailed_players(args[0]);

		if (jailed_players.isEmpty()) {
			send_message("Could not find any jailed players matching that name.", player, ChatColor.RED);
			return;
		}

		if (jailed_players.size() >= 2) {
			show_info(player, jailed_players);
			send_message("Found multiple jailed players matching that name. Please select by uuid.", player, ChatColor.RED);
			return;
		}

		JailedPlayer jailedplayer = jailed_players.get(0);
		target = plugin.getServer().getPlayer(jailedplayer.uuid);

		if (target != null) {
			announce_message("Unjailing " + target.getName(), player);
			target.teleport(jailedplayer.location);
			Jail.jailed_players.remove(jailedplayer.uuid);
			SQLite.delete_player_info(jailedplayer.uuid);
			target.sendMessage(ChatColor.GREEN + "You have been released from jail.");
		} else if (jailedplayer.online == false) {
			SQLite.delete_player_info(jailedplayer.uuid);
			announce_message("Unjailing never-jailed player " + jailedplayer.playername, player);
		} else {
			SQLite.set_to_be_released(jailedplayer.uuid);
			announce_message("Unjailing offline player " + jailedplayer.playername, player);
		}
	}

	private void show_info(Player player, String[] args) throws java.sql.SQLException {
		if (args.length > 1) {
			send_message("Invalid arguments. Correct usage is /jailinfo [player]", player, ChatColor.RED);
			return;
		}

		if (args.length == 1) {
			@SuppressWarnings("deprecation")
			Player target = Jail.instance.getServer().getPlayerExact(args[0]);
			ArrayList<JailedPlayer> jailed_players = null;

			if (target == null) {
				jailed_players = SQLite.get_jailed_players(args[0]);
			} else {
				JailedPlayer j = SQLite.get_jailed_player(target.getUniqueId());
				if (j == null) {
					send_message("That player does not appear to be jailed.", player, ChatColor.RED);
					return;
				}
				jailed_players = new ArrayList<>();
				jailed_players.add(j);
			}

			show_info(player, jailed_players);
		} else if (args.length == 0) {
			send_message("The jail is in world " + Config.world + ". The two jail corners are at: " + Config.x1 + " " + Config.y1 + " " + Config.z1 + ", and " + Config.x2 + " " + Config.y2 + " " + Config.z2, player, ChatColor.GREEN);
			send_message("The jail spawn point is at " + Config.spawn.getWorld().getName() + " " + Config.spawn.getBlockX() + " " + Config.spawn.getBlockY() + " " + Config.spawn.getBlockZ(), player, ChatColor.GREEN);
			send_message("There are " + Jail.jailed_players.size() + " jailed players online.", player, ChatColor.GREEN);
			send_message("In total there are " + SQLite.amount_of_jailed_players() + " jailed players.", player, ChatColor.GREEN);
		}
	}

	private void show_info(Player player, ArrayList<JailedPlayer> jailed_players) {
		if (jailed_players == null || jailed_players.isEmpty()) {
			send_message("Could not find any jailed players matching that name.", player, ChatColor.RED);
			return;
		}

		for (JailedPlayer j : jailed_players) {
			send_message(j.get_info(), player, ChatColor.GOLD);
		}
	}
}
