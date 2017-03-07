package org.openbakery.codesign

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.openbakery.CommandRunner
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class Codesign {
	private static Logger logger = LoggerFactory.getLogger(Codesign.class)

	String identity
	Type type
	File entitlementsFile
	private CommandRunner commandRunner
	PlistHelper plistHelper
	List<File> mobileProvisionFiles
	File keychainPath
	Xcode xcode


	public Codesign(Xcode xcode, String identity, File keychainPath,  File entitlementsFile, List<File> mobileProvisionFiles, Type type, CommandRunner commandRunner, PlistHelper plistHelper) {
		this.xcode = xcode
		this.identity = identity
		this.keychainPath = keychainPath
		this.type = type
		this.commandRunner = commandRunner
		this.entitlementsFile = entitlementsFile
		this.mobileProvisionFiles = mobileProvisionFiles
		this.plistHelper = plistHelper
	}

	void sign(File bundle) {
		logger.debug("Codesign with Identity: {}", identity)

		codeSignFrameworks(bundle)

		logger.debug("Codesign {}", bundle)

		String bundleIdentifier = getIdentifierForBundle(bundle)
		if (bundleIdentifier == null) {
			logger.debug("bundleIdentifier not found in bundle {}", bundle)
		}

		performCodesign(bundle, createEntitlementsFile(bundle, bundleIdentifier))

	}

	private void codeSignFrameworks(File bundle) {

			File frameworksDirectory
			if (this.type == Type.iOS) {
				frameworksDirectory = new File(bundle, "Frameworks")
			} else {
				frameworksDirectory = new File(bundle, "Contents/Frameworks")
			}

			if (frameworksDirectory.exists()) {

				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.toLowerCase().endsWith(".dylib") || name.toLowerCase().endsWith(".framework")
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
			codesignCommand << this.identity
			codesignCommand << "--verbose"
			codesignCommand << bundle.absolutePath
			codesignCommand << "--keychain"
			codesignCommand << keychainPath.absolutePath

			def environment = ["DEVELOPER_DIR":xcode.getPath() + "/Contents/Developer/"]
			commandRunner.run(codesignCommand, environment)

		}


	private String getIdentifierForBundle(File bundle) {
		File infoPlist

		if (this.type == Type.iOS) {
			infoPlist = new File(bundle, "Info.plist");
		} else {
			infoPlist = new File(bundle, "Contents/Info.plist")
		}

		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier")
		return bundleIdentifier
	}

	File createEntitlementsFile(File bundle, String bundleIdentifier) {

		if (bundleIdentifier == null) {
			return null
		}

		if (entitlementsFile != null) {
			return entitlementsFile
		}

		File provisionFile = ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier, this.mobileProvisionFiles, this.commandRunner, this.plistHelper)
		if (provisionFile == null) {
			if (this.type == Type.iOS) {
					throw new IllegalStateException("No provisioning profile found for bundle identifier: " + bundleIdentifier)
			}
			// on OS X this is valid
			return null
		}

		// set keychain access group

		//BuildConfiguration buildConfiguration = project.xcodebuild.getBuildConfiguration()
		//def keychainAccessGroup = plistHelper.getValueFromPlist(buildConfiguration.entitlements, "keychain-access-groups")
		List<String> keychainAccessGroup = getKeychainAccessGroupFromEntitlements(bundle)

		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, this.commandRunner, this.plistHelper)
		String basename = FilenameUtils.getBaseName(provisionFile.path)
		File tmpDir = new File(System.getProperty("java.io.tmpdir"))
		File entitlementsFile = new File(tmpDir, "entitlements_" + basename + ".plist")
		reader.extractEntitlements(entitlementsFile, bundleIdentifier, keychainAccessGroup)
		entitlementsFile.deleteOnExit()

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

}
