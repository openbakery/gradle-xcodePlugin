package org.openbakery.codesign

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.openbakery.CommandRunner
import org.openbakery.configuration.Configuration
import org.openbakery.configuration.ConfigurationFromMap
import org.openbakery.configuration.ConfigurationFromPlist
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 28.10.16.
 */
public class Codesign {
	private static Logger logger = LoggerFactory.getLogger(Codesign.class)

	CodesignParameters codesignParameters
	Type type
	/*
	 	user this entitlements file for codesigning, nothing is extracted from the mobile provisioning profile
	 */
	File entitlementsFile
	private CommandRunner commandRunner
	PlistHelper plistHelper
	Xcode xcode


	public Codesign(Xcode xcode, CodesignParameters codesignParameters, Type type, CommandRunner commandRunner, PlistHelper plistHelper) {
		this.xcode = xcode
		this.type = type
		this.commandRunner = commandRunner
		this.plistHelper = plistHelper
		this.codesignParameters = codesignParameters
	}

	void useEntitlements(File entitlementsFile) {
		if (entitlementsFile == null || !entitlementsFile.exists()) {
			throw new IllegalArgumentException("given entitlements file does not exist: " + entitlementsFile)
		}
		this.entitlementsFile = entitlementsFile
	}


	void sign(File bundle) {
		logger.debug("Codesign with Identity: {}", codesignParameters.signingIdentity)

		codeSignFrameworks(bundle)

		logger.debug("Codesign {}", bundle)

		File entitlements = this.entitlementsFile

		if (entitlements == null) {
			logger.debug("createEntitlementsFile no entitlementsFile specified")
			Configuration configuration
			File xcentFile = getXcentFile(bundle)
			if (xcentFile != null) {
				configuration = new ConfigurationFromPlist(xcentFile)
			} else {
				configuration = new ConfigurationFromMap([:])
			}
			String bundleIdentifier = getIdentifierForBundle(bundle)
			entitlements = createEntitlementsFile(bundleIdentifier, configuration)
		}


		performCodesign(bundle, entitlements)

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
			codesignCommand << codesignParameters.signingIdentity
			codesignCommand << "--verbose"
			codesignCommand << bundle.absolutePath
			codesignCommand << "--keychain"
			codesignCommand << codesignParameters.keychain.absolutePath

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


	File createEntitlementsFile(String bundleIdentifier, Configuration configuration) {
		// the settings from the xcent file are merge with the settings from entitlements from the provisioning profile
		if (bundleIdentifier == null) {
			logger.debug("not bundleIdentifier specified")
			return null
		}

		if (entitlementsFile != null) {
			logger.debug("createEntitlementsFile no entitlementsFile specified")
			return entitlementsFile
		}

		logger.debug("createEntitlementsFile for identifier {}", bundleIdentifier)

		File provisionFile = ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier, codesignParameters.mobileProvisionFiles, this.commandRunner, this.plistHelper)
		if (provisionFile == null) {
			if (this.type == Type.iOS) {
				throw new IllegalStateException("No provisioning profile found for bundle identifier: " + bundleIdentifier)
			}
			// on OS X this is valid
			return null
		}

		// set keychain access group

		List<String> keychainAccessGroup = getKeychainAccessGroupFromEntitlements(configuration)

		ProvisioningProfileReader reader = new ProvisioningProfileReader(provisionFile, this.commandRunner, this.plistHelper)
		String basename = FilenameUtils.getBaseName(provisionFile.path)
		File tmpDir = new File(System.getProperty("java.io.tmpdir"))
		File extractedEntitlementsFile = new File(tmpDir, "entitlements_" + basename + ".plist")
		reader.extractEntitlements(extractedEntitlementsFile, bundleIdentifier, keychainAccessGroup, configuration)
		extractedEntitlementsFile.deleteOnExit()

		logger.info("Using entitlementsFile {}", extractedEntitlementsFile)

		return extractedEntitlementsFile
	}

	File getXcentFile(File bundle) {
		def fileList = bundle.list(
						[accept: { d, f -> f ==~ /.*xcent/ }] as FilenameFilter
		)
		if (fileList == null || fileList.toList().isEmpty()) {
			return null
		}
		File result = new File(bundle, fileList.toList().get(0))
		if (result.exists()) {
			return result
		}
		return null
	}


	List<String> getKeychainAccessGroupFromEntitlements(Configuration configuration) {
		List<String> result = []

		String applicationIdentifier = configuration.getString("application-identifier")
		if (StringUtils.isNotEmpty(applicationIdentifier)) {
			applicationIdentifier = applicationIdentifier.split("\\.")[0] + "."
		}
		List<String> keychainAccessGroups = configuration.getStringArray("keychain-access-groups")

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
