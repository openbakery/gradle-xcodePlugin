package org.openbakery

/*
 * User: rene
 */
class Destination {

	String platform = null
	String name = null
	String arch = null
	String id = null
	String os = null


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

	boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false

		Destination that = (Destination) o

		if (arch != that.arch) return false
		if (id != that.id) return false
		if (name != that.name) return false
		if (os != that.os) return false
		if (platform != that.platform) return false

		return true
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
}
