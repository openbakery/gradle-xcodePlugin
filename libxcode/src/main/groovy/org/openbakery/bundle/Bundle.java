package org.openbakery.bundle;

import java.io.File;


/**
 * This class should hold the information for a Bundle within the application like
 * the path, bundleIdentifier and also the provisioning profile that is used for this bundle
 */
public class Bundle {

	public final File path;

	//public String identifier
	//public File provisioningProfile

	public Bundle(File path) {
		this.path = path;
	}

	public Bundle(String path) {
		this.path = new File(path);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Bundle bundle = (Bundle) o;

		return path.equals(bundle.path);
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	@Override
	public String toString() {
		return "Bundle{" +
			"path=" + path +
			'}';
	}
}
