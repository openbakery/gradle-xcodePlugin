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
}
