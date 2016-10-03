package net.simpvp.Jail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SQLite {

	private static Jail plugin = Jail.instance;

	private static Connection conn = null;

	/**
	 * Opens the SQLite connection.
	 */
	public static void connect() {

		String database = "jdbc:sqlite:plugins/Jail/jail.sqlite";

		try {
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection(database);

			Statement st = conn.createStatement();

			/* Get database version */
			ResultSet rs = st.executeQuery("PRAGMA user_version;");

			int user_version = 0;
			while (rs.next())
				user_version = rs.getInt("user_version");

			rs.close();

			switch (user_version) {

				/* Database is brand new. Create tables */
				case 0: {
						plugin.getLogger().info("Database not yet created. Creating ...");
						String query = "CREATE TABLE jailedplayers"
							+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ "uuid BLOB,"
							+ "playername TEXT,"
							+ "reason TEXT,"
							+ "jailer TEXT,"
							+ "world TEXT,"
							+ "x INT,"
							+ "y INT,"
							+ "z INT,"
							+ "jailedtime INT,"
							/* to_be_released is now also used for to-be-jailed players
							 * least significant bit = to_be_released, 2-bit = online*/
							+ "to_be_released INT);"
							+ "CREATE TABLE jailedips"
							+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ "ip TEXT NOT NULL,"
							+ "name TEXT NOT NULL,"
							+ "uuid BLOB,"
							+ "UNIQUE(ip, name));"
							+ "PRAGMA user_version = 2;";
						st.executeUpdate(query);
						break;
				}
				case 1: {
						plugin.getLogger().info("Migrating database to version 2 ...");
						String query = "CREATE TABLE jailedips"
							+ "(id INTEGER PRIMARY KEY AUTOINCREMENT,"
							+ "ip TEXT NOT NULL,"
							+ "name TEXT NOT NULL,"
							+ "uuid BLOB,"
							+ "UNIQUE(ip, name));"
							+ "PRAGMA user_version = 2;";
						st.executeUpdate(query);
						break;
				}

			}

			st.close();

		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage() );
			return;
		}

	}

	/**
	 * Closes the database connection.
	 */
	public static void close() {
		try {
			conn.close();
		} catch(Exception e) {
			plugin.getLogger().info(e.getMessage());
		}

	}

	/**
	 * Gets the info on a jailed person, returns null if player is not jailed
	 * @param uuid UUID of the player
	 * @return JailedPlayer object of info about the player
	 */
	public static JailedPlayer get_player_info(UUID uuid) {
		JailedPlayer ret = null;

		try {
			String query = "SELECT * FROM jailedplayers WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Location location = new Location(
						plugin.getServer().getWorld(rs.getString("world")), 
						(double) rs.getInt("x") + 0.5, 
						(double) rs.getInt("y"), 
						(double) rs.getInt("z") + 0.5);
				ret = new JailedPlayer(uuid, rs.getString("playername"), 
						rs.getString("reason"), rs.getString("jailer"), 
						location, rs.getInt("jailedtime"),
						(rs.getInt("to_be_released") & 1) != 0,
						(rs.getInt("to_be_released") & 2) == 0);
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}

		return ret;
	}

	/**
	 * Gets the info on a jailed person, returns null if player is not jailed
	 * @param playername The jailed player's name
	 * @return JailedPlayer object of info about the player
	 */
	public static JailedPlayer get_player_info(String playername) {
		JailedPlayer ret = null;

		try {
			String query = "SELECT * FROM jailedplayers WHERE playername = ? COLLATE NOCASE";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, playername);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				Location location = new Location(
						plugin.getServer().getWorld(rs.getString("world")), 
						(double) rs.getInt("x") + 0.5, 
						(double) rs.getInt("y"), 
						(double) rs.getInt("z") + 0.5);
				ret = new JailedPlayer(UUID.fromString(rs.getString("uuid")),
						rs.getString("playername"), 
						rs.getString("reason"), rs.getString("jailer"), 
						location, rs.getInt("jailedtime"),
						(rs.getInt("to_be_released") & 1) != 0,
						(rs.getInt("to_be_released") & 2) == 0);
			}

			rs.close();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}

		return ret;
	}

	/**
	 * Inserts the info on a jailed player into the database
	 * @param jailedplayer JailedPlayer info
	 */
	public static void insert_player_info(JailedPlayer jailedplayer) {
		try {
			String query = "INSERT INTO jailedplayers (uuid, playername, reason, jailer, world, x, y, z, jailedtime, to_be_released)"
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, jailedplayer.uuid.toString());
			st.setString(2, jailedplayer.playername);
			st.setString(3, jailedplayer.reason);
			st.setString(4, jailedplayer.jailer);
			st.setString(5, jailedplayer.location.getWorld().getName());
			st.setInt(6, jailedplayer.location.getBlockX());
			st.setInt(7, jailedplayer.location.getBlockY());
			st.setInt(8, jailedplayer.location.getBlockZ());
			st.setInt(9, jailedplayer.jailed_time);
			st.setInt(10, jailedplayer.get_to_be_released());

			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Delete a player from the jailed players table
	 * @param uuid UUID of the player
	 */
	public static void delete_player_info(UUID uuid) {
		try {
			String query = "DELETE FROM jailedplayers WHERE uuid = ? ";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			st.executeUpdate();
			st.close();

			query = "DELETE FROM jailedips WHERE uuid = ?";
			st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}

	}

	/**
	 * Set a player to be released.
	 * @param uuid UUID of the player to be released
	 */
	public static void set_to_be_released(UUID uuid) {
		try {
			String query = "UPDATE jailedplayers SET to_be_released = 1 WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			st.executeUpdate();
			st.close();

			query = "DELETE FROM jailedips WHERE uuid = ?";
			st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Sets that a player has been online.
	 * @param uuid UUID of the player that has been online
	 */
	public static void set_has_been_online(UUID uuid) {
		try {
			String query = "UPDATE jailedplayers SET to_be_released = 0 WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, uuid.toString());
			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Updates the stored location for a given player.
	 * @param uuid UUID of the player
	 * @param location The location to be updated to
	 */
	public static void update_player_location(UUID uuid, Location location) {
		try {
			String query = "UPDATE jailedplayers SET world = ?,"
				+ "x = ?, y = ?, z = ? WHERE uuid = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, location.getWorld().getName());
			st.setInt(2, location.getBlockX());
			st.setInt(3, location.getBlockY());
			st.setInt(4, location.getBlockZ());
			st.setString(5, uuid.toString());
			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}
	}

	/**
	 * Gets how many players have been jailed in total
	 */
	public static int amount_of_jailed_players() {
		int ret = -1;
		try {
			String query = "SELECT COUNT(*) FROM jailedplayers";
			PreparedStatement st = conn.prepareStatement(query);
			ResultSet rs = st.executeQuery();
			while (rs.next())
				ret = rs.getInt("count(*)");

			rs.close();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}

		return ret;
	}

	/**
	 * Gets the name of all jailed players from the given ip
	 */
	public static String get_ip_jailed(String ip) {
		String ret = null;
		try {
			String query = "SELECT * FROM jailedips WHERE ip = ?";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, ip);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				if (ret == null) {
					ret = rs.getString("name");
				} else {
					ret += ", " + rs.getString("name");
				}
			}
			rs.close();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}

		return ret;
	}

	/**
	 * Inserts the given player into the jailedips database.
	 */
	public static void insert_ip_jailed(Player player) {
		try {
			String query = "INSERT OR IGNORE INTO jailedips(ip, name, uuid) VALUES(?, ?, ?)";
			PreparedStatement st = conn.prepareStatement(query);
			st.setString(1, player.getAddress().getHostString());
			st.setString(2, player.getName());
			st.setString(3, player.getUniqueId().toString());
			st.executeUpdate();
			st.close();
		} catch (Exception e) {
			plugin.getLogger().info(e.getMessage());
		}
	}

}

