package org.openbakery.packaging

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.AbstractDistributeTask
import org.openbakery.CommandRunnerException
import org.openbakery.xcode.Type
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
			logger.lifecycle("not a device build, so no codesign and packaging needed");
			return;
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


		for (File bundle : appBundles) {

			if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
				removeFrameworkFromExtensions(bundle)
				removeUnneededDylibsFromBundle(bundle)
				embedProvisioningProfileToBundle(bundle)
			}

			if (signSettingsAvailable) {
				logger.info("Codesign app: {}", bundle);
				codesign(bundle)
			} else {
				String message = "Bundle not signed: " + bundle
				output.withStyle(StyledTextOutput.Style.Failure).println(message)
			}
		}

		File appBundle = appBundles.last()
		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {

			boolean isAdHoc = isAdHoc(appBundle)
			createIpa(applicationFolder, !isAdHoc);
		} else {
			createPackage(appBundle);
		}

	}

	boolean isAdHoc(File appBundle) {
		File provisionFile = getProvisionFileForBundle(appBundle)
		if (provisionFile == null) {
			return false
		}
		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, project, this.commandRunner, this.plistHelper)
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
		return getProvisionFileForIdentifier(bundleIdentifier)
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

	private void codesign(File bundle) {
		logger.debug("Codesign with Identity: {}", project.xcodebuild.getSigning().getIdentity())

		codeSignFrameworks(bundle)

		logger.debug("Codesign {}", bundle)

		String bundleIdentifier = getIdentifierForBundle(bundle)
		if (bundleIdentifier == null) {
			logger.debug("bundleIdentifier not found in bundle {}", bundle)
		}

		performCodesign(bundle, createEntitlementsFile(bundle, bundleIdentifier))


	}

	File createEntitlementsFile(File bundle, String bundleIdentifier) {

		if (project.xcodebuild.signing.entitlementsFile != null) {
			return project.xcodebuild.signing.entitlementsFile
		}

		File provisionFile = getProvisionFileForIdentifier(bundleIdentifier)
		if (provisionFile == null) {
			if (project.xcodebuild.type == Type.iOS) {
				throw new IllegalStateException("No provisioning profile found for bundle identifier: " + bundleIdentifier)
			}
			// on OS X this is valid
			return null
		}

		// set keychain access group

		//BuildConfiguration buildConfiguration = project.xcodebuild.getBuildConfiguration()
		//def keychainAccessGroup = plistHelper.getValueFromPlist(buildConfiguration.entitlements, "keychain-access-groups")
		List<String> keychainAccessGroup = getKeychainAccessGroupFromEntitlements(bundle)

		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, project, this.commandRunner, this.plistHelper)
		String basename = FilenameUtils.getBaseName(provisionFile.path)
		File entitlementsFile = new File(outputPath, "entitlements_" + basename + ".plist")
		reader.extractEntitlements(entitlementsFile, bundleIdentifier, keychainAccessGroup)



		logger.info("Using entitlementsFile {}", entitlementsFile)
		return entitlementsFile
	}

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

				performCodesign(file, null)

			}
		}
	}

	private void performCodesign(File bundle, File entitlements) {
		logger.info("performCodesign {}", bundle)

		def codesignCommand = []
		codesignCommand << "/usr/bin/codesign"
		codesignCommand << "--force"

		if (entitlements != null) {
			codesignCommand << "--entitlements"
			codesignCommand << entitlements.absolutePath
		}

		codesignCommand << "--sign"
		codesignCommand << project.xcodebuild.getSigning().getIdentity()
		codesignCommand << "--verbose"
		codesignCommand << bundle.absolutePath
		codesignCommand << "--keychain"
		codesignCommand << project.xcodebuild.signing.keychainPathInternal.absolutePath

		def environment = ["DEVELOPER_DIR":xcode.getPath() + "/Contents/Developer/"]
		commandRunner.run(codesignCommand, environment)

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

			logger.info("provision profile - {}", embeddedProvisionFile);

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
