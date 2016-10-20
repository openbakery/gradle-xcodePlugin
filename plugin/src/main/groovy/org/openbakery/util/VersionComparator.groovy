package org.openbakery.util

import org.openbakery.xcode.Version

/**
 * Created by rene on 29.10.15.
 */
class VersionComparator implements Comparator<String>{


	@Override
	int compare(String first, String second) {
		Version firstVersion = new Version(first)
		Version secondVersion = new Version(second)
		return firstVersion.compareTo(secondVersion)
	}
}
