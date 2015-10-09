package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.CommandRunnerException
import org.openbakery.Type
import org.openbakery.XcodePlugin
import org.openbakery.signing.ProvisioningProfileReader

/**
 * Created by rene on 14.11.14.
 */
class PackageTask extends AbstractDistributeTask {


	public static final String PACKAGE_PATH = "package"
	File outputPath = new File(project.getBuildDir(), PACKAGE_PATH)


	private List<File> appBundles

	String applicationBundleName

	PackageTask() {
		super();
		setDescription("Signs the app bundle that was created by the build and creates the ipa");
		dependsOn(
						XcodePlugin.ARCHIVE_TASK_NAME,
						XcodePlugin.KEYCHAIN_CREATE_TASK_NAME,
						XcodePlugin.PROVISIONING_INSTALL_TASK_NAME,
		)
		finalizedBy(
						XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME
		)


	}

	@TaskAction
	void packageApplication() throws IOException {
		if (project.xcodebuild.isSimulatorBuildOf(Type.iOS)) {
			logger.lifecycle("not a device build, so no codesign and packaging needed");
			return;
		}

		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration");
		}

		File applicationFolder = createApplicationFolder();

		def applicationName = getApplicationNameFromArchive()
		copy(getApplicationBundleDirectory(), applicationFolder)


		applicationBundleName = applicationName + ".app"


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

		def isSigningAvailable = false
		def notSignedMessage = 'Bundles will not be signed.'
		if (project.xcodebuild.signing.identity == null) {
			logger.warn('No Signing Identity provided. ' + notSignedMessage)
		} else if (project.xcodebuild.signing.mobileProvisionFile == null) {
			logger.warn('No mobile provision file provided. ' + notSignedMessage)
		} else if (!project.xcodebuild.signing.keychainPathInternal.exists()) {
			logger.warn("No certificate or keychain found. " + notSignedMessage)
		} else {
			isSigningAvailable = true;
		}

		for (File bundle : appBundles) {

			if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
				embedProvisioningProfileToBundle(bundle)
			}
/*
			if (project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONEOS)) {
				File embeddedProvisionFile = new File(getAppContentPath(bundle) + "embedded.provisionprofile")
				embeddedProvisionFile.delete()
			}
			*/

			if (isSigningAvailable) {
				logger.lifecycle("codesign path: {}", bundle);
				codesign(bundle)
			} else {
				def output = services.get(StyledTextOutputFactory).create(PackageTask)
				output.withStyle(StyledTextOutput.Style.Failure).println("Bundle not signed: " + bundle)
			}
		}

		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
			createIpa(applicationFolder);
		} else {
			createPackage(appBundles.last());
		}

	}

	File getProvisionFileForBundle(File bundle) {
		String bundleIdentifier = getIdentifierForBundle(bundle)
		return getProvisionFileForIdentifier(bundleIdentifier)
	}

	File getProvisionFileForIdentifier(String bundleIdentifier) {

		def provisionFileMap = [:]

		for (File mobileProvisionFile : project.xcodebuild.signing.mobileProvisionFile) {
			ProvisioningProfileReader reader = new ProvisioningProfileReader(mobileProvisionFile, project, this.commandRunner)
			provisionFileMap.put(reader.getApplicationIdentifier(), mobileProvisionFile)
		}

		logger.debug("provisionFileMap: {}", provisionFileMap)

		for ( entry in provisionFileMap ) {
			if (entry.key.equalsIgnoreCase(bundleIdentifier) ) {
				return entry.value
			}
		}

		// match wildcard
		for ( entry in provisionFileMap ) {
			if (entry.key.equals("*")) {
				return entry.value
			}

			if (entry.key.endsWith("*")) {
				String key = entry.key[0..-2].toLowerCase()
				if (bundleIdentifier.toLowerCase().startsWith(key)) {
					return entry.value
				}
			}
		}

		def output = services.get(StyledTextOutputFactory).create(PackageTask)

		output.withStyle(StyledTextOutput.Style.Failure).println("No provisioning profile found for bundle identifier " + bundleIdentifier)
		output.withStyle(StyledTextOutput.Style.Description).println("Available bundle identifier are " + provisionFileMap.keySet())


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


	private void createZipPackage(File packagePath, String extension) {
		File packageBundle = new File(outputPath, getIpaFileName() + "." + extension)
		if (!packageBundle.parentFile.exists()) {
			packageBundle.parentFile.mkdirs()
		}

		File swiftSupportPath = addSwiftSupport(packagePath, applicationBundleName)
		if (swiftSupportPath != null) {
			createZip(packageBundle, packagePath.getParentFile(), packagePath, swiftSupportPath)
		} else {
			createZip(packageBundle, packagePath.getParentFile(), packagePath)
		}
	}

	private void createIpa(File payloadPath) {
		createZipPackage(payloadPath, "ipa")
	}

	private void createPackage(File packagePath) {

		createZipPackage(packagePath, "zip")
	}

	private void codesign(File bundle) {
		logger.debug("Codesign with Identity: {}", project.xcodebuild.getSigning().getIdentity())

		codeSignFrameworks(bundle)

		logger.debug("Codesign {}", bundle)

		def environment = ["DEVELOPER_DIR":project.xcodebuild.xcodePath + "/Contents/Developer/"]

		String bundleIdentifier = getIdentifierForBundle(bundle)
		File provisionFile = getProvisionFileForIdentifier(bundleIdentifier)
		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, project, this.commandRunner, this.plistHelper)
		String basename = FilenameUtils.getBaseName(provisionFile.path)
		File entitlementsFile = new File(outputPath, "entitlements_" + basename + ".plist")
		reader.extractEntitlements(entitlementsFile, bundleIdentifier)

		logger.debug("Using entitlementsFile {}", entitlementsFile)


		def codesignCommand = [
						"/usr/bin/codesign",
						"--force",
						"--entitlements",
						entitlementsFile.absolutePath,
						"--sign",
						project.xcodebuild.getSigning().getIdentity(),
						"--verbose",
						bundle.absolutePath,
						"--keychain",
						project.xcodebuild.signing.keychainPathInternal.absolutePath,
		]
		commandRunner.run(codesignCommand, environment)

	}

	private void codeSignFrameworks(File bundle) {

		File frameworksDirectory
		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
			frameworksDirectory = new File(bundle, "Frameworks");
		} else {
			frameworksDirectory = new File(bundle, "Contents/Frameworks");
		}

		if (frameworksDirectory.exists()) {

			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".dylib") || name.toLowerCase().endsWith(".framework");
				}
			};


			for (File file in frameworksDirectory.listFiles(filter)) {
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

	private String getIdentifierForBundle(File bundle) {
		File infoPlist

		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
			infoPlist = new File(bundle, "Info.plist");
		} else {
			infoPlist = new File(bundle, "Contents/Info.plist")
		}

		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier")
		return bundleIdentifier
	}

	private void embedProvisioningProfileToBundle(File bundle) {
		File mobileProvisionFile = getProvisionFileForBundle(bundle);
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

		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
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
		if (project.xcodebuild.type == Type.iOS) {
			return bundle.absolutePath + "/"
		}
		return bundle.absolutePath + "/Contents/"
	}

	def getIpaFileName() {
		if (project.xcodebuild.ipaFileName) {
			return project.xcodebuild.ipaFileName
		} else {
			return getApplicationNameFromArchive()
		}
	}
}
