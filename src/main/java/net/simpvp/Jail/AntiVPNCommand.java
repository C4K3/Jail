package net.simpvp.Jail;

import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import net.md_5.bungee.api.ChatColor;

import java.net.InetAddress;

public class AntiVPNCommand implements Listener,CommandExecutor{
	
	public static int hours_required;

	private long last_sent_message = -1;

	public AntiVPNCommand(Jail plugin) {
		hours_required = plugin.getConfig().getInt("novpns");
		plugin.getLogger().info("NoVpns set to " + hours_required);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled=true)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		String reason = GeoIP.check_asn(event.getAddress());
		String as = GeoIP.getAs(event.getAddress());
		if (!requiredConditions(player) && reason != null) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Please turn off your VPN to connect");

			String msg = String.format("[NoVPNs] Blocking %s from joining from %s. Reason: %s", player.getName(), as, reason);
			Jail.instance.getLogger().info(msg);

			long now = System.currentTimeMillis() / 1000L;
			if (last_sent_message + 60 < now) {
				last_sent_message = now;

				for (Player p : Jail.instance.getServer().getOnlinePlayers()) {
					if (!p.isOp()) {
						continue;
					}

					p.sendMessage(ChatColor.RED + msg);
				}
			}
		} else {
			String msg = String.format("%s is joining from %s", player.getName(), as);
			if (reason != null) {
				msg += String.format(" (bad network: %s)", reason);
			}
			Jail.instance.getLogger().info(msg);
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Jail.instance.reloadConfig();
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			if (!player.isOp()) {
				sender.sendMessage(ChatColor.RED + "You must be an admin to use this command");
				return true;
			}
		}
		
		if (args.length == 0) {
			sender.sendMessage(ChatColor.GREEN + "/novpn will prevent players using vpns with less than the set amount of hours from joining. It is currently set to " + hours_required);
			return true;
		}

		int previous_hours_required = hours_required;
		
		try {
			hours_required = Integer.parseInt(args[0]);
		} catch (Exception e) {
			sender.sendMessage("Invalid integer " + args[0]);
			return true;
		}
		
		Jail.instance.reloadConfig();
		Jail.instance.getConfig().set("novpns", hours_required);
		Jail.instance.saveConfig();
		
		Commands.announce_message(String.format("Changed /novpns from %d to %d", previous_hours_required, hours_required), player);
		return true;
	}
	
	/* Return true if player meets all required conditions */
	private boolean requiredConditions(Player p) {

		/* OPs can join unconditionally */
		if (p.isOp()) {
			return true;
		}

		/* Whitelisted players can join unconditionally */
		if (p.isWhitelisted()) {
			return true;
		}
		
		OfflinePlayer offplayer = Jail.instance.getServer().getOfflinePlayer(p.getUniqueId());
		int hours = -1;
		if (offplayer.hasPlayedBefore()) {
			int played_ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
			hours = played_ticks / (20 * 60 * 60);
		}
		
		if (hours < hours_required) {
			return false;
		}
		return true;
	}

}
