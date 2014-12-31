package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodePlugin

/**
 * Created by rene on 14.11.14.
 */
class PackageTask extends AbstractXcodeTask {

	PackageTask() {
		super();
		setDescription("Signs the app bundle that was created by the build and creates the ipa");
		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
	}

	@TaskAction
	void packageApplication() throws IOException {
		if (!project.xcodebuild.sdk.startsWith("iphoneos")) {
			logger.lifecycle("not a device build, so no codesign needed");
			return;
		}

		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration");
		}

		File payloadPath = createPayload();

		// copy the build app
		// use cp to preserve permission
		copy(project.xcodebuild.applicationBundle, payloadPath)

		List<File> appBundles = getAppBundles(payloadPath)
		for (File bundle : appBundles) {
			embedProvisioningProfileToBundle(bundle)
			codesign(bundle)
		}

		File swiftSupport = createSwiftSupportFolder(payloadPath)
		createIpa(payloadPath, swiftSupport);
	}

	File getMobileProvisionFileForIdentifier(String bundleIdentifier) {

		def mobileProvisionFileMap = [:]

		for (File mobileProvisionFile : project.xcodebuild.signing.mobileProvisionFile) {
			ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader(mobileProvisionFile)
			mobileProvisionFileMap.put(reader.getApplicationIdentifier(), mobileProvisionFile)
		}

		for ( entry in mobileProvisionFileMap ) {
			if (entry.key.equalsIgnoreCase(bundleIdentifier) ) {
				return entry.value
			}
		}

		// match wildcard
		for ( entry in mobileProvisionFileMap ) {
			if (entry.key.equals("*")) {
				return entry.value
			}

			if (entry.key.endsWith("*")) {
				String key = entry.key[0..-2]
				if (bundleIdentifier.toLowerCase().startsWith(key)) {
					return entry.value
				}
			}
		}

		return null
	}

	private void createIpa(File... files) {
		createZip(project.xcodebuild.ipaBundle, project.xcodebuild.signing.signingDestinationRoot, files)
	}

	private void codesign(File bundle) {

		def codesignCommand = [
						"/usr/bin/codesign",
		"--force",
		"--preserve-metadata=identifier,entitlements",
		//"--preserve-metadata=identifier,entitlements,resource-rules",
		//"--resource-rules=" + bundle.absolutePath + "/ResourceRules.plist",
		"--sign",
		project.xcodebuild.getSigning().getIdentity(),
		bundle.absolutePath,
		"--keychain",
		project.xcodebuild.signing.keychainPathInternal.absolutePath,
		]
		commandRunner.run(codesignCommand)
	}

	private void embedProvisioningProfileToBundle(File bundle) {
		File infoPlist = new File(bundle, "Info.plist");
		String bundleIdentifier = getValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier")

		File mobileProvisionFile = getMobileProvisionFileForIdentifier(bundleIdentifier);
		if (mobileProvisionFile != null) {
			File embeddedProvisionFile = new File(bundle, "embedded.mobileprovision");
			FileUtils.copyFile(mobileProvisionFile, embeddedProvisionFile);
		}
	}

	private File createSigningDestination(String name) throws IOException {
		File destination = new File(project.xcodebuild.signing.signingDestinationRoot, name);
		if (destination.exists()) {
			FileUtils.deleteDirectory(destination);
		}
		destination.mkdirs();
		return destination;
	}

	private File createPayload() throws IOException {
		createSigningDestination("Payload")
	}

	private File createSwiftSupportFolder(File appPath) {
		File appBundle = new File(appPath, project.xcodebuild.applicationBundle.name)
		File frameworks = new File(appBundle, "Frameworks")
		File swiftSupport = createSigningDestination("SwiftSupport")
		if (frameworks.exists()) {
			def xcodePath = project.xcodebuild.xcodePath
			if (null == xcodePath) {
				xcodePath = "/Applications/Xcode.app"
			}
			def swiftLibsPath = "${xcodePath}/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphoneos/".toString()
			for (File swiftLib : frameworks.listFiles()) {
				File toCopy = new File(swiftLibsPath, swiftLib.name);
				copy(toCopy, swiftSupport)
			}
		}
		return swiftSupport
	}
}
