package net.simpvp.Jail;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.maxmind.geoip.Country;
import com.maxmind.geoip.LookupService;
import org.bukkit.entity.Player;

public class GeoIP {

	private static LookupService asn4 = null;
	private static LookupService asn6 = null;
	private static LookupService country4 = null;
	private static LookupService country6 = null;

	// Key is ASN, value is reason it's added to list.
	public static HashMap<Integer, String> bad_asns = null;

	public static void init() {
		String geoipDatabaseDir = Jail.instance.getConfig().getString("geoipDatabase");

		if (geoipDatabaseDir == null) {
			Jail.instance.getLogger().info("Not using geoip database");
			return;
		}

		if (!geoipDatabaseDir.endsWith("/")) {
			geoipDatabaseDir += "/";
		}

		try {
			asn4 = new LookupService(geoipDatabaseDir + "GeoIPASNum.dat", LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);
			asn6 = new LookupService(geoipDatabaseDir + "GeoIPASNumv6.dat", LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);
			country4 = new LookupService(geoipDatabaseDir + "GeoIP.dat", LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);
			country6 = new LookupService(geoipDatabaseDir + "GeoIPv6.dat", LookupService.GEOIP_MEMORY_CACHE | LookupService.GEOIP_CHECK_CACHE);

			Jail.instance.getLogger().info("Initiated geoip database");
		} catch (Exception e) {
			Jail.instance.getLogger().severe("Error instantiating GeoIP database " + e);
			e.printStackTrace();
			return;
		}

		try {
			File f = new File(Jail.instance.getDataFolder(), "bad_asns.yaml");
			if (f.exists()) {
				Configuration c = YamlConfiguration.loadConfiguration(f);
				bad_asns = new HashMap<>();
				for (Map<String, Object> value : (List<Map<String, Object>>) c.getList("badasns")) {
					Integer asn = (Integer) value.get("asn");
					String reason = (String) value.get("reason");
					bad_asns.put(asn, reason);
				}

				Jail.instance.getLogger().info("Read bad_asns.yaml");
			}
		} catch (Exception e) {
			Jail.instance.getLogger().severe("Error instantiating bad AS list " + e);
			e.printStackTrace();
			bad_asns = null;
		}
	}

	public static void close() {
		if (asn4 != null) {
			asn4.close();
			asn4 = null;
		}
		if (asn6 != null) {
			asn6.close();
			asn6 = null;
		}
		if (country4 != null) {
			country4.close();
			country4 = null;
		}
		if (country6 != null) {
			country6.close();
			country6 = null;
		}
	}

	public static String getAs(InetAddress ip) {
		if (asn4 == null || asn6 == null) {
			return null;
		}

		String as = null;
		if (ip instanceof Inet4Address) {
			as = asn4.getOrg(ip);
		} else if (ip instanceof Inet6Address) {
			as = asn6.getOrgV6(ip);
		}
		if (as == null) {
			return null;
		}

		return as;
	}

	public static Integer getAsn(InetAddress ip) {
		return getAsn(getAs(ip));
	}

	private static Pattern ASN_REGEX = Pattern.compile("^AS([0-9]+).*");
	public static Integer getAsn(String as) {
		if (as == null) {
			return null;
		}

		Matcher m = ASN_REGEX.matcher(as);
		if (!m.find()) {
			Jail.instance.getLogger().info("Jail ASN regex did not match: " + as);
			return null;
		}
		String match = m.group(1);
		return Integer.valueOf(match);
	}

	public static String getCountry(InetAddress ip) {
		if (country4 == null || country6 == null) {
			return null;
		}

		Country country = null;
		if (ip instanceof Inet4Address) {
			country = country4.getCountry(ip);
		} else if (ip instanceof Inet6Address) {
			country = country6.getCountryV6(ip);
		}
		if (country == null) {
			return null;
		}

		return country.getCode();
	}

	public static String check_asn(InetAddress address) {
		if (GeoIP.bad_asns == null || GeoIP.bad_asns.isEmpty()) {
			return null;
		}

		String as = GeoIP.getAs(address);
		Integer asn = GeoIP.getAsn(as);
		if (asn == null) {
			return null;
		}

		String reason = GeoIP.bad_asns.get(asn);
		if (reason == null) {
			return null;
		}
		return reason;
	}
}
