package org.openbakery

/**
 * Created by rene on 08.10.15.
 */


class BuildConfiguration {
	String infoplist
	String bundleIdentifier
	String productName
	String sdkRoot
	Devices devices
	String entitlements

	BuildConfiguration parent;

	public BuildConfiguration() {
	}

	public BuildConfiguration(BuildConfiguration parent) {
		this.parent = parent;
	}

	String getInfoplist() {
		if (infoplist != null) {
			return infoplist
		}
		if (parent != null) {
			return parent.infoplist
		}
		return null
	}

	String getBundleIdentifier() {
		if (bundleIdentifier != null) {
			return bundleIdentifier
		}
		if (parent != null) {
			return parent.bundleIdentifier
		}
		return null
	}

	String getProductName() {
		if (productName != null) {
			return productName;
		}
		if (parent != null) {
			return parent.productName
		}
		return null
	}

	String getSdkRoot() {
		if (sdkRoot != null) {
			return sdkRoot
		}
		if (parent != null) {
			return parent.sdkRoot
		}
		return null
	}

	String getEntitlements() {
		if (entitlements != null) {
			return entitlements
		}
		if (parent != null) {
			return parent.entitlements
		}
		return null
	}
}

class BuildTargetConfiguration {

	HashMap<String, BuildConfiguration> buildSettings = new HashMap<>()


}

