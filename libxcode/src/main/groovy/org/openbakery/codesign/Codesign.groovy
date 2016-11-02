package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.helpers.PlistHelper
import org.openbakery.xcode.Type
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 28.10.16.
 */
public class Codesign {
	private static Logger logger = LoggerFactory.getLogger(Codesign.class)

	String identity
	Type type
	File entitlementsFile
	private CommandRunner commandRunner
	PlistHelper plistHelper


	public Codesign(String identity, Type type, File entitlementsFile, CommandRunner commandRunner) {
		this.identity = identity
		this.type = type
		this.commandRunner = commandRunner
		this.entitlementsFile = entitlementsFile
		this.plistHelper = new PlistHelper(commandRunner)
	}

	void sign(File bundle) {
		/*
		logger.debug("Codesign with Identity: {}", identity)

		codeSignFrameworks(bundle)

		logger.debug("Codesign {}", bundle)

		String bundleIdentifier = getIdentifierForBundle(bundle)
		if (bundleIdentifier == null) {
			logger.debug("bundleIdentifier not found in bundle {}", bundle)
		}

		performCodesign(bundle, createEntitlementsFile(bundle, bundleIdentifier))
*/

	}
/*
	private void codeSignFrameworks(File bundle) {

			File frameworksDirectory
			if (this.type == Type.iOS) {
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
			codesignCommand << this.identity
			codesignCommand << "--verbose"
			codesignCommand << bundle.absolutePath
			codesignCommand << "--keychain"
			codesignCommand << project.xcodebuild.signing.keychainPathInternal.absolutePath

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

		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier")
		return bundleIdentifier
	}

	File createEntitlementsFile(File bundle, String bundleIdentifier) {

		if (entitlementsFile != null) {
			return entitlementsFile
		}

		File provisionFile = getProvisionFileForIdentifier(bundleIdentifier)
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

		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, project, this.commandRunner, this.plistHelper)
		String basename = FilenameUtils.getBaseName(provisionFile.path)
		File entitlementsFile = new File(outputPath, "entitlements_" + basename + ".plist")
		reader.extractEntitlements(entitlementsFile, bundleIdentifier, keychainAccessGroup)



		logger.info("Using entitlementsFile {}", entitlementsFile)
		return entitlementsFile
	}
*/
}
