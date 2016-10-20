package org.openbakery.xcode

import org.apache.commons.lang.StringUtils

/*
 * User: rene
 */
class Destination {

	String platform = null
	String name = null
	String arch = null
	String id = null
	String os = null

	Destination() {
	}

	Destination(String platform, String name, String os) {
		this.platform = platform
		this.name = name
		this.os = os
	}

	@Override
	public java.lang.String toString() {
		return "Destination{" +
						"platform='" + platform + '\'' +
						", name='" + name + '\'' +
						", arch='" + arch + '\'' +
						", id='" + id + '\'' +
						", os='" + os + '\'' +
						'}';
	}

	boolean equals(other) {
		if (this.is(other)) return true
		if (getClass() != other.class) return false

		Destination otherDestination = (Destination) other

		if (id != null && StringUtils.equals(id, otherDestination.id)) {
			return true;
		}

		if (StringUtils.equalsIgnoreCase(arch, otherDestination.arch) &&
						StringUtils.equalsIgnoreCase(name, otherDestination.name) &&
						StringUtils.equalsIgnoreCase(os, otherDestination.os) &&
						StringUtils.equalsIgnoreCase(platform, otherDestination.platform)) {
			return true
		}
		return false
	}

	int hashCode() {
		int result
		result = (platform != null ? platform.hashCode() : 0)
		result = 31 * result + (name != null ? name.hashCode() : 0)
		result = 31 * result + (arch != null ? arch.hashCode() : 0)
		result = 31 * result + (id != null ? id.hashCode() : 0)
		result = 31 * result + (os != null ? os.hashCode() : 0)
		return result
	}


	public java.lang.String toPrettyString() {
		StringBuilder builder = new StringBuilder()
		builder.append(name)
		if (!StringUtils.isEmpty(platform)) {
			builder.append("/")
			builder.append(platform)
		}
		if (!StringUtils.isEmpty(os)) {
			builder.append("/")
			builder.append(os)
		}
		return builder.toString()
	}
}
