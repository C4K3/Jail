package org.c4k3.Jail;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuit implements Listener {

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled=true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Jail.jailed_players.remove(event.getPlayer().getUniqueId());
	}

}