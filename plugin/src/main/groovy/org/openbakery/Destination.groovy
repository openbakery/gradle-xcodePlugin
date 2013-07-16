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
}
