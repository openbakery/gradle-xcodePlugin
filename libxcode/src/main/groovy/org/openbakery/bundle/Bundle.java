package org.openbakery.bundle;

import java.io.File;

import org.openbakery.CommandRunner;
import org.openbakery.util.PlistHelper;
import org.openbakery.xcode.Type;


/**
 * This class should hold the information for a Bundle within the application like
 * the path, bundleIdentifier and also the provisioning profile that is used for this bundle
 */
public class Bundle {

	public final File path;
	public final Type type;

	//public String identifier
	//public File provisioningProfile

	public Bundle(File path, Type type) {
		this.path = path;
		this.type = type;
	}

	public Bundle(String path, Type type) {
		this(new File(path), type);
	}

	public File getInfoPlist() {
		if (type == Type.macOS) {
			return new File(path, "Contents/Info.plist");
		}
		return new File(path, "Info.plist");
	}

	public String getBundleIdentifier() {
		return getStringFromPlist("CFBundleIdentifier");
	}

	public File getExecutable() {
		return new File(path, getStringFromPlist("CFBundleExecutable"));
	}

	private String getStringFromPlist(String key) {
		PlistHelper helper = new PlistHelper(new CommandRunner());
		return helper.getStringFromPlist(getInfoPlist(), key);
	}


	@Override
	public String toString() {
		return "Bundle{" +
			"path=" + path +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Bundle)) return false;

		Bundle bundle = (Bundle) o;

		if (!path.equals(bundle.path)) return false;
		return type == bundle.type;
	}

	@Override
	public int hashCode() {
		int result = path.hashCode();
		result = 31 * result + type.hashCode();
		return result;
	}
}
