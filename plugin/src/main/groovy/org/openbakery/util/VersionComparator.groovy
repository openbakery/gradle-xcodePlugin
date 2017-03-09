package org.openbakery.util

import org.openbakery.xcode.Version

class VersionComparator implements Comparator<String>{


	@Override
	int compare(String first, String second) {
		Version firstVersion = new Version(first)
		Version secondVersion = new Version(second)
		return firstVersion.compareTo(secondVersion)
	}
}
