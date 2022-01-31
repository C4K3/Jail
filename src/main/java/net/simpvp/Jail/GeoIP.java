package net.simpvp.Jail;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.net.InetAddress;
import java.util.ArrayList;

public class PlayerLogin implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerLogin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		SQLite.insert_ip(player);
		final JailedPlayer jailedplayer = SQLite.get_jailed_player(player.getUniqueId());
		if (jailedplayer == null) {
			String jailed_friends = SQLite.get_ip_jailed(
					player.getAddress().getHostString());
			if (jailed_friends != null) {
				Jail.instance.getLogger().info(player.getName()
						+ " shares IPs with jailed players: " + jailed_friends);
				for (Player p : Jail.instance.getServer().getOnlinePlayers()) {
					if (p.isOp()) {
						p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + player.getName()
								+ " shares IPs with jailed players: " + jailed_friends);
					}
				}
			}

			InetAddress address = player.getAddress().getAddress();
			String reason = GeoIP.check_asn(address);
			if (reason != null) {
				String as = GeoIP.getAs(address);
				if (as != null) {
					message_admins(player, as, reason);
				}
			}

			return;
		}

		if (jailedplayer.to_be_released) {
			/* as the player will not have an entity associated with them at this
			 * stage, we have to try for a bit to see when they're done logging in
			 * and then teleport them then */
			final long start = System.currentTimeMillis();

			new BukkitRunnable() {
				@Override
				public void run() {
					if (System.currentTimeMillis() > start + 10 * 1000) {
						this.cancel();
						Jail.instance.getLogger().info("Unable to unjail " + jailedplayer.playername);
					}

					Player player = Jail.instance.getServer().getPlayer(jailedplayer.uuid);
					if (player != null) {
						player.teleport(jailedplayer.location);
						SQLite.delete_player_info(player.getUniqueId());
						Jail.instance.getLogger().info("Releasing " + jailedplayer.playername + " from jail.");
						player.sendMessage(ChatColor.GREEN + "You have been released from jail.");
						this.cancel();
					}
				}
			}.runTaskTimer(Jail.instance, 20, 20);

			return;
		} else if (!jailedplayer.online) {
			jailedplayer.online = true;
			SQLite.update_player_location(jailedplayer.uuid,
					player.getLocation());
			SQLite.set_has_been_online(jailedplayer.uuid);
		}

		SQLite.insert_ip_jailed(player);
		Jail.instance.getLogger().info("Jailed player " + player.getName() + " has connected.");
		jailedplayer.add();
	}

	public void message_admins(Player player, String as, String reason) {
		String msg = String.format("%s is joining from bad network %s. Listed reason: %s", player.getName(), as, reason);
		Jail.instance.getLogger().info(msg);
		for (Player p : Jail.instance.getServer().getOnlinePlayers()) {
			if (!p.isOp()) {
				continue;
			}

			p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + msg);
		}
	}
}
