package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.CommandRunnerException
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.codesign.Codesign
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin
import org.openbakery.codesign.ProvisioningProfileReader

class PackageTask extends AbstractDistributeTask {

	public static final String PACKAGE_PATH = "package"
	File outputPath


	private List<File> appBundles

	String applicationBundleName
	StyledTextOutput output

	PackageTask() {
		super();
		setDescription("Signs the app bundle that was created by the build and creates the ipa");
		dependsOn(
						XcodePlugin.KEYCHAIN_CREATE_TASK_NAME,
						XcodePlugin.PROVISIONING_INSTALL_TASK_NAME,
		)
		finalizedBy(
						XcodePlugin.KEYCHAIN_REMOVE_SEARCH_LIST_TASK_NAME
		)

		output = services.get(StyledTextOutputFactory).create(PackageTask)

	}

	@TaskAction
	void packageApplication() throws IOException {
		if (project.xcodebuild.isSimulatorBuildOf(Type.iOS)) {
			logger.lifecycle("not a device build, so no codesign and packaging needed")
			return
		}
		outputPath = new File(project.getBuildDir(), PACKAGE_PATH)


		File applicationFolder = createApplicationFolder();

		def applicationName = getApplicationNameFromArchive()
		copy(getApplicationBundleDirectory(), applicationFolder)

		applicationBundleName = applicationName + ".app"

		File applicationPath = new File(applicationFolder, applicationBundleName)

		// copy onDemandResources
		File onDemandResources = new File(getProductsDirectory(), "OnDemandResources")
		if (onDemandResources.exists()) {
			copy(onDemandResources, applicationPath)
		}

		ApplicationBundle applicationBundle = new ApplicationBundle(applicationPath , project.xcodebuild.type, project.xcodebuild.simulator)
		appBundles = applicationBundle.getBundles()

		File resourceRules = new File(applicationFolder, applicationBundleName + "/ResourceRules.plist")
		if (resourceRules.exists()) {
			resourceRules.delete()
		}


		File infoPlist = getInfoPlistFile()

		try {
			plistHelper.deleteValueFromPlist(infoPlist, "CFBundleResourceSpecification")
		} catch (CommandRunnerException ex) {
			// ignore, this means that the CFBundleResourceSpecification was not in the infoPlist
		}

		def signSettingsAvailable = true;
		if (project.xcodebuild.signing.mobileProvisionFile == null) {
			logger.warn('No mobile provision file provided.')
			signSettingsAvailable = false;
		} else if (!project.xcodebuild.signing.keychainPathInternal.exists()) {
			logger.warn('No certificate or keychain found.')
			signSettingsAvailable = false;
		}

		Codesign codesign = new Codesign(xcode, getSigningIdentity(), project.xcodebuild.signing.keychainPathInternal, project.xcodebuild.signing.entitlementsFile, project.xcodebuild.signing.mobileProvisionFile, project.xcodebuild.type,  commandRunner, plistHelper)
		for (File bundle : appBundles) {

			if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
				removeFrameworkFromExtensions(bundle)
				removeUnneededDylibsFromBundle(bundle)
				embedProvisioningProfileToBundle(bundle)
			}

			if (signSettingsAvailable) {
				logger.info("Codesign app: {}", bundle);
				codesign.sign(bundle)
			} else {
				String message = "Bundle not signed: " + bundle
				output.withStyle(StyledTextOutput.Style.Failure).println(message)
			}
		}

		File appBundle = appBundles.last()
		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {

			boolean isAdHoc = isAdHoc(appBundle)
			createIpa(applicationFolder, !isAdHoc)
		} else {
			createPackage(appBundle)
		}

	}

	boolean isAdHoc(File appBundle) {
		File provisionFile = getProvisionFileForBundle(appBundle)
		if (provisionFile == null) {
			return false
		}
		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, this.commandRunner, this.plistHelper)
		return reader.isAdHoc()
	}

	def removeFrameworkFromExtensions(File bundle) {
		// appex extensions should not contain extensions
		if (FilenameUtils.getExtension(bundle.toString()).equalsIgnoreCase("appex"))  {
			File frameworksPath = new File(bundle, "Frameworks")
			if (frameworksPath.exists()) {
				FileUtils.deleteDirectory(frameworksPath)
			}
		}

	}

	def removeUnneededDylibsFromBundle(File bundle) {
		File libswiftRemoteMirror = new File(bundle, "libswiftRemoteMirror.dylib")
		if (libswiftRemoteMirror.exists()) {
			libswiftRemoteMirror.delete()
		}
	}

	File getProvisionFileForBundle(File bundle) {
		String bundleIdentifier = getIdentifierForBundle(bundle)
		return ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier, project.xcodebuild.signing.mobileProvisionFile, this.commandRunner, this.plistHelper)
	}


	def addSwiftSupport(File payloadPath,  String applicationBundleName) {
		File frameworksPath = new File(payloadPath, applicationBundleName + "/Frameworks")
		if (!frameworksPath.exists()) {
			return null
		}

		File swiftLibArchive = new File(getArchiveDirectory(), "SwiftSupport")

		if (swiftLibArchive.exists()) {
			copy(swiftLibArchive, payloadPath.getParentFile())
			return new File(payloadPath.getParentFile(), "SwiftSupport")
		}
		return null
	}


	private void createZipPackage(File packagePath, String extension, boolean includeSwiftSupport) {
		File packageBundle = new File(outputPath, getIpaFileName() + "." + extension)
		if (!packageBundle.parentFile.exists()) {
			packageBundle.parentFile.mkdirs()
		}

		File swiftSupportPath = null;
		if (includeSwiftSupport) {
			swiftSupportPath = addSwiftSupport(packagePath, applicationBundleName)
		}

		if (swiftSupportPath != null) {
			createZip(packageBundle, packagePath.getParentFile(), packagePath, swiftSupportPath)
		} else {
			createZip(packageBundle, packagePath.getParentFile(), packagePath)
		}
	}

	private void createIpa(File payloadPath, boolean addSwiftSupport) {
		createZipPackage(payloadPath, "ipa", addSwiftSupport)
	}

	private void createPackage(File packagePath) {

		createZipPackage(packagePath, "zip", false)
	}


/*
	List<String> getKeychainAccessGroupFromEntitlements(File bundle) {

		List<String> result = []
		File entitlementsFile = new File(bundle, "archived-expanded-entitlements.xcent")
		if (!entitlementsFile.exists()) {
			return result
		}

		String applicationIdentifier = plistHelper.getValueFromPlist(entitlementsFile, "application-identifier")
		if (StringUtils.isNotEmpty(applicationIdentifier)) {
			applicationIdentifier = applicationIdentifier.split("\\.")[0] + "."
		}
		List<String> keychainAccessGroups = plistHelper.getValueFromPlist(entitlementsFile, "keychain-access-groups")

		keychainAccessGroups.each { item ->
			if (item.startsWith(applicationIdentifier)) {
				result << item.replace(applicationIdentifier, ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX)
			} else {
				result << item
			}
		}

		return result
	}
	*/


	private String getIdentifierForBundle(File bundle) {
		File infoPlist

		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
			infoPlist = new File(bundle, "Info.plist");
		} else {
			infoPlist = new File(bundle, "Contents/Info.plist")
		}

		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier")
		return bundleIdentifier
	}


	private void embedProvisioningProfileToBundle(File bundle) {
		File mobileProvisionFile = getProvisionFileForBundle(bundle)
		if (mobileProvisionFile != null) {
			File embeddedProvisionFile

			String profileExtension = FilenameUtils.getExtension(mobileProvisionFile.absolutePath)
			embeddedProvisionFile = new File(getAppContentPath(bundle) + "embedded." + profileExtension)

			logger.info("provision profile - {}", embeddedProvisionFile)

			FileUtils.copyFile(mobileProvisionFile, embeddedProvisionFile)
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
