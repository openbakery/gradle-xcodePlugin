package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractDistributeTask
import org.openbakery.CommandRunnerException
import org.openbakery.XcodePlugin

/**
 * Created by rene on 14.11.14.
 */
class PackageTask extends AbstractDistributeTask {


	public static final String PACKAGE_PATH = "package"
	File outputPath = new File(project.getBuildDir(), PACKAGE_PATH)


	private List<File> appBundles

	PackageTask() {
		super();
		setDescription("Signs the app bundle that was created by the build and creates the ipa");
		dependsOn(XcodePlugin.ARCHIVE_TASK_NAME)
	}

	@TaskAction
	void packageApplication() throws IOException {
		if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONESIMULATOR)) {
			logger.lifecycle("not a device build, so no codesign and packaging needed");
			return;
		}

		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration");
		}

		if (project.xcodebuild.signing.identity == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing identity");
		}

		File applicationFolder = createApplicationFolder();

		def applicationName = getApplicationNameFromArchive()
		copy(getApplicationBundleDirectory(), applicationFolder)


		def applicationBundleName = applicationName + ".app"


		appBundles = getAppBundles(applicationFolder, applicationBundleName)

		File resourceRules = new File(applicationFolder, applicationBundleName + "/ResourceRules.plist")
		if (resourceRules.exists()) {
			resourceRules.delete()
		}


		File infoPlist = getInfoPlistFile()

		try {
			plistHelper.setValueForPlist(infoPlist, "Delete CFBundleResourceSpecification")
		} catch (CommandRunnerException ex) {
			// ignore, this means that the CFBundleResourceSpecification was not in the infoPlist
		}


		for (File bundle : appBundles) {

			if (!bundle.absolutePath.endsWith(".framework/Versions/Current")) {
				embedProvisioningProfileToBundle(bundle)
			}

			logger.lifecycle("codesign path: {}", bundle);

			codesign(bundle)
		}

		createIpa(applicationFolder, applicationBundleName);
	}

	File getMobileProvisionFileForIdentifier(String bundleIdentifier) {

		def mobileProvisionFileMap = [:]

		for (File mobileProvisionFile : project.xcodebuild.signing.mobileProvisionFile) {
			ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader(mobileProvisionFile, project)
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


	def addSwiftSupport(File payloadPath,  String applicationBundleName) {

		File frameworksPath = new File(payloadPath, applicationBundleName + "/Frameworks")
		if (!frameworksPath.exists()) {
			return null
		}

		File swiftLibArchive = new File(getArchiveDirectory(), "SwiftSupport")

		copy(swiftLibArchive, payloadPath.getParentFile())
		return new File(payloadPath.getParentFile(), "SwiftSupport");;

	}


	private void createIpa(File payloadPath, String applicationBundleName) {

		File ipaBundle = new File(outputPath, getApplicationNameFromArchive() + ".ipa")
		if (!ipaBundle.parentFile.exists()) {
			ipaBundle.parentFile.mkdirs()
		}

		File swiftSupportPath = addSwiftSupport(payloadPath, applicationBundleName)
		if (swiftSupportPath != null) {
			createZip(ipaBundle, payloadPath.getParentFile(), payloadPath, swiftSupportPath)
		} else {
			createZip(ipaBundle, payloadPath.getParentFile(), payloadPath)
		}
		/*
		File frameworksPath = new File(payloadPath, applicationBundleName + "/Frameworks")
		if (frameworksPath.exists()) {
			File swiftSupportPath = addSwiftSupport(payloadPath.getParentFile(), frameworksPath.listFiles())

			createZip(ipaBundle, payloadPath.getParentFile(), payloadPath, swiftSupportPath)
		} else {
			createZip(ipaBundle, payloadPath.getParentFile(), payloadPath)
		}
		*/

	}

	private void codesign(File bundle) {
		logger.lifecycle("Codesign with Identity: {}", project.xcodebuild.getSigning().getIdentity())

		codeSignSwiftLibs(bundle)

		logger.lifecycle("Codesign {}", bundle)

		def codesignCommand = [
						"/usr/bin/codesign",
						"--force",
						"--preserve-metadata=identifier,entitlements",
						"--sign",
						project.xcodebuild.getSigning().getIdentity(),
						"--verbose",
						bundle.absolutePath,
						"--keychain",
						project.xcodebuild.signing.keychainPathInternal.absolutePath,
		]
		commandRunner.run(codesignCommand)

	}

	private void codeSignSwiftLibs(File bundle) {

		File frameworksDirectory = new File(bundle, "Frameworks");

		if (frameworksDirectory.exists()) {

			FilenameFilter dylibFilter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".dylib");
				}
			};


			for (File file in frameworksDirectory.listFiles(dylibFilter)) {
				logger.lifecycle("Codesign {}", file)
				def codesignCommand = [
								"/usr/bin/codesign",
								"--force",
								"--sign",
								project.xcodebuild.getSigning().getIdentity(),
								"--verbose",
								file.absolutePath,
								"--keychain",
								project.xcodebuild.signing.keychainPathInternal.absolutePath,
				]
				commandRunner.run(codesignCommand)
			}
		}
	}

	private void embedProvisioningProfileToBundle(File bundle) {
        File infoPlist

		if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONEOS)) {
			infoPlist = new File(bundle, "Info.plist");
		} else {
			infoPlist = new File(bundle, "Contents/Info.plist")
		}

		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier")

		File mobileProvisionFile = getMobileProvisionFileForIdentifier(bundleIdentifier);
		if (mobileProvisionFile != null) {
			File embeddedProvisionFile

			String profileExtension = FilenameUtils.getExtension(mobileProvisionFile.absolutePath)
			embeddedProvisionFile = new File(getAppContentPath(bundle) + "embedded." + profileExtension)

			logger.lifecycle("provision profile - {}", embeddedProvisionFile);

			FileUtils.copyFile(mobileProvisionFile, embeddedProvisionFile);
		}
	}

	private File createSigningDestination(String name) throws IOException {
		File destination = new File(outputPath, name);
		if (destination.exists()) {
			FileUtils.deleteDirectory(destination);
		}
		destination.mkdirs();
		return destination;
	}

	private File createApplicationFolder() throws IOException {

		if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONEOS)) {
			return createSigningDestination("Payload")
		} else {
			// same folder as signing
			if (!outputPath.exists()) {
				outputPath.mkdirs()
			}
			return outputPath
		}
	}

    private File getInfoPlistFile() {
		return new File(getAppContentPath() + "Info.plist")
    }

	private String getAppContentPath() {

		return getAppContentPath(appBundles.last())
	}

	private String getAppContentPath(File bundle) {
		if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONEOS)) {
			return bundle.absolutePath + "/"
		}
		return bundle.absolutePath + "/Contents/"
	}
}
