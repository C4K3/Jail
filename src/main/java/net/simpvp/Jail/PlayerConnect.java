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

public class PlayerConnect implements Listener,CommandExecutor{
	
	private int hours_required = 0;
	
	public PlayerConnect() {
		hours_required = Jail.instance.getConfig().getInt("novpns");
		Jail.instance.getLogger().info("NoVpns set to " + hours_required);
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Player player = event.getPlayer();
		if (!vpnAllowed(player)) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Please turn off your VPN to connect");
			Jail.instance.getLogger().info("Blocking " + player.getDisplayName() + " from joining with a vpn");
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			if (!player.isOp()) {
				String msg = (hours_required < 0) ?
						"Vpns are currently allowed" :
						"Currently blocking players using vpns with less than " + hours_required + " hours played";
				player.sendMessage(msg);
				return true;
			}
		}
		
		if (args.length == 0) {
			sender.sendMessage(ChatColor.GREEN + "/novpn will prevent players using vpns with less than the set amount of hours from joining. It is currently set to " + hours_required);
			return true;
		}
		
		try {
			hours_required = Integer.parseInt(args[0]);
		} catch (Exception e) {
			sender.sendMessage("Invalid integer " + args[0]);
			return true;
		}
		
		Jail.instance.reloadConfig();
		Jail.instance.getConfig().set("novpns", hours_required);
		Jail.instance.saveConfig();
		
		String m = ChatColor.GREEN + "/novpns hours required set to " + hours_required;
		sender.sendMessage(m);
		Jail.instance.getLogger().info(m);
		
		return true;
	}
	
	/* Return true if player is allowed to join with a vpn */
	private boolean vpnAllowed(Player p) {
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
		if (hours < hours_required && vpn_check(p)) {
			return false;
		}
		
		
		return true;
	}
	
	/* Return true if player has a vpn */
	private boolean vpn_check(Player player) {
		if (GeoIP.bad_asns == null || GeoIP.bad_asns.isEmpty()) {
			return false;
		}

		String as = GeoIP.getAs(player.getAddress().getAddress());
		Integer asn = GeoIP.getAsn(as);
		if (asn == null) {
			return false;
		}

		String reason = GeoIP.bad_asns.get(asn);
		if (reason == null) {
			return false;
		}

		return true;
	}
}
