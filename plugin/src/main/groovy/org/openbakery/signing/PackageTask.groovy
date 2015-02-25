package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractDistributeTask
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin

/**
 * Created by rene on 14.11.14.
 */
class PackageTask extends AbstractDistributeTask {


	public static final String PACKAGE_PATH = "package"

	PackageTask() {
		super();
		setDescription("Signs the app bundle that was created by the build and creates the ipa");
		dependsOn(XcodePlugin.ARCHIVE_TASK_NAME)
	}

	@TaskAction
	void packageApplication() throws IOException {
		if (!project.xcodebuild.sdk.startsWith("iphoneos")) {
			logger.lifecycle("not a device build, so no codesign and packaging needed");
			return;
		}

		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration");
		}

		File payloadPath = createPayload();

		def applicationName = getApplicationNameFromArchive()
		copy(getApplicationBundleDirectory(), payloadPath)

		def applicationBundleName = applicationName + ".app"

		List<File> appBundles = getAppBundles(payloadPath, applicationBundleName)

		for (File bundle : appBundles) {
			embedProvisioningProfileToBundle(bundle)
			codesign(bundle)
		}

		createIpa(payloadPath, applicationBundleName);
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

	private void createIpa(File payloadPath, String applicationBundleName) {

		File ipaBundle = new File(project.getBuildDir(), PACKAGE_PATH + "/" + getApplicationNameFromArchive() + ".ipa")
		if (!ipaBundle.parentFile.exists()) {
			ipaBundle.parentFile.mkdirs()
		}


		File frameworksPath = new File(payloadPath, applicationBundleName + "/Frameworks")
		if (frameworksPath.exists() && frameworksPath.listFiles().length > 0) {

			File swiftSupportPath = new File(payloadPath.getParentFile(), "SwiftSupport")
			swiftSupportPath.mkdirs()
			frameworksPath.listFiles().each() {
				copy(it, swiftSupportPath)
			}

			createZip(ipaBundle, payloadPath.getParentFile(), payloadPath, swiftSupportPath)
		} else {
			createZip(ipaBundle, payloadPath.getParentFile(), payloadPath)
		}

	}

	private void codesign(File bundle) {

		logger.lifecycle("Codesign with Identity: {}", project.xcodebuild.getSigning().getIdentity())
		def codesignCommand = [
						"/usr/bin/codesign",
		"--force",
		"--preserve-metadata=identifier,entitlements",
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




}
