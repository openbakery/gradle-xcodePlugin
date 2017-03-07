package org.openbakery

import org.openbakery.xcode.Devices

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

	@Override
	String toString() {
		StringBuilder builder = new StringBuilder("BuildConfiguration[")
		builder.append("infoplist=")
		builder.append(infoplist)
		builder.append(", bundleIdentifier=")
		builder.append(bundleIdentifier)
		builder.append(", productName=")
		builder.append(productName)
		builder.append(", sdkRoot=")
		builder.append(sdkRoot)
		builder.append(", devices=")
		builder.append(devices)
		builder.append(", entitlements=")
		builder.append(entitlements)
		return builder.toString()
	}
}

class BuildTargetConfiguration {

	HashMap<String, BuildConfiguration> buildSettings = new HashMap<>()

}

