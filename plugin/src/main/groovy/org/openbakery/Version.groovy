package org.openbakery

class Version implements Comparable<Version> {

	public int major = -1
	public int minor = -1
	public int maintenance = -1

	public String suffix = null

	public Version() {
	}

	public Version(String version) {
		Scanner versionScanner = new Scanner(version);
		versionScanner.useDelimiter("\\.");

		try {
			if (versionScanner.hasNext()) {
				major = versionScanner.nextInt()
			}

			if (versionScanner.hasNext()) {
				minor = versionScanner.nextInt()
			}

			if (versionScanner.hasNext()) {
				maintenance = versionScanner.nextInt()
			}
		} catch (InputMismatchException ex) {
			suffix = versionScanner.next()
		}


	}

	@Override
	int compareTo(Version version) {
		if (this.major != version.major) {
			return this.major - version.major
		}
		if (this.minor != version.minor) {
			return this.minor - version.minor
		}
		if (this.maintenance != version.maintenance) {
			return this.maintenance - version.maintenance
		}
		if (this.suffix != null && version.suffix == null) {
			return 1
		}
		if (this.suffix == null && version.suffix != null) {
			return -1
		}
		if (this.suffix != null && version.suffix != null) {
			return this.suffix.compareTo(version.suffix)
		}
		return 0
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder()

		if (this.major >= 0) {
			builder.append(major)
		}

		if (this.minor >= 0) {
			builder.append(".")
			builder.append(minor)
		}

		if (this.maintenance>= 0) {
			builder.append(".")
			builder.append(maintenance)
		}

		if (this.suffix != null) {
			if (builder.length() > 0 ){
				builder.append(".")
			}
			builder.append(suffix)
		}

		return builder.toString();
	}

	@Override
	boolean equals(Object other) {
		if (other instanceof Version) {
			return this.compareTo(other) == 0
		}
		return false
	}
}
