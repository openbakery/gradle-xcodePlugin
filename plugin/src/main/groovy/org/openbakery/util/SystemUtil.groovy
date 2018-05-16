package org.openbakery.util

import org.openbakery.xcode.Version

class SystemUtil {
	static Version getOsVersion() {
		Version result = new Version()
		String versionString = System.getProperty("os.version")
		Scanner scanner = new Scanner(versionString).useDelimiter("\\.")
		if (scanner.hasNext()) {
			result.major = scanner.nextInt()
		}
		if (scanner.hasNext()) {
			result.minor = scanner.nextInt()
		}
		if (scanner.hasNext()) {
			result.maintenance = scanner.nextInt()
		}
		return result
	}

	public static boolean isValidUri(String value) {
		boolean result = true
		try {
			new File(new URI(value))
		} catch (Exception exception) {
			result = false
		}

		return result
	}

	public static boolean isValidUrl(String value) {
		boolean result = true
		try {
			new File(new URL(value))
		} catch (Exception exception) {
			result = false
		}

		return result
	}
}
