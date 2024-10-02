package org.openbakery.codesign

import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.openbakery.CommandRunner
import org.openbakery.bundle.Bundle
import org.openbakery.configuration.Configuration
import org.openbakery.configuration.ConfigurationFromMap
import org.openbakery.configuration.ConfigurationFromPlist
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files

import static groovy.io.FileType.DIRECTORIES

class Codesign {
	private static Logger logger = LoggerFactory.getLogger(Codesign.class)

	CodesignParameters codesignParameters
	private CommandRunner commandRunner
	PlistHelper plistHelper
	Xcode xcode


	Codesign(Xcode xcode, CodesignParameters codesignParameters, CommandRunner commandRunner, PlistHelper plistHelper) {
		this.xcode = xcode
		this.commandRunner = commandRunner
		this.plistHelper = plistHelper
		this.codesignParameters = codesignParameters
	}


	void sign(Bundle bundle) {
		this.sign(bundle, bundle.getBundleIdentifier())
	}

	void sign(Bundle bundle, String mainBundleIdentifier) {
		logger.debug("Codesign with Identity: {}", codesignParameters.signingIdentity)
		logger.debug("Codesign with mainBundleIdentifier: {}", mainBundleIdentifier)

		codeSignEmbeddedBundle(bundle.path)

		logger.debug("Codesign {}", bundle)

		File entitlements = null
		if (codesignParameters.signingIdentity != null) {
			entitlements = prepareEntitlementsForSigning(bundle, mainBundleIdentifier)
		}

		if (codesignParameters.type == Type.macOS) {
			performCodesign(bundle.path, entitlements, true, true)
		} else {
			performCodesign(bundle.path, entitlements, false, false)
		}

	}

	private File prepareEntitlementsForSigning(Bundle bundle, String mainBundleIdentifier) {
		File entitlements = codesignParameters.entitlementsFile
		logger.debug("prepareEntitlementsForSigning")
		if (entitlements != null) {
			if (!entitlements.exists()) {
				throw new IllegalArgumentException("given entitlements file does not exist: " + entitlements)
			}
			logger.info("Using given entitlements {}", entitlements)
		} else {
			logger.debug("createEntitlementsFile no entitlementsFile specified")
			String bundleIdentifier = getIdentifierForBundle(bundle.path)
			Configuration configuration = createConfiguration(bundle.path, mainBundleIdentifier, bundleIdentifier)

			logger.info("entitlements configuration for {}: {}", bundle, configuration)

			entitlements = createEntitlementsFile(bundleIdentifier, configuration)
			if (entitlements != null) {
				logger.info("Using entitlements extracted from the provisioning profile")
			}
		}
		return entitlements
	}

	/**
	 * creates the configuration for signing.
	 * Either it is using the parameters specified in the build.gradle using the `entitlement` parameter, or if not
	 * specified it uses the *.xcent file if it is present.
	 * @param bundle
	 * @return
	 */
	private Configuration createConfiguration(File bundle, String mainBundleIdentifier, String bundleIdentifier) {
		// for now we disable the merging for extension, except for the keychain-access-groups parameter
		logger.info("createConfiguration for Bundle {}", bundle)
		if (bundle.absolutePath.endsWith("appex")) {
			Map<String, String>entitlements = codesignParameters.getEntitlements(bundleIdentifier)
			if (entitlements != null) {
				logger.info("bundle entitlements ({}): {}", bundleIdentifier, entitlements)
				return new ConfigurationFromMap(entitlements)
			} else if (codesignParameters.entitlements != null) {
				logger.info("entitlements: {}", entitlements)
				def map = ["keychain-access-groups": ["\$(AppIdentifierPrefix)" + mainBundleIdentifier]]
				return new ConfigurationFromMap(map)
			}
			logger.info("empty entitlements}")
			return new ConfigurationFromMap([:])
		}

		Map<String, String>entitlements = codesignParameters.getEntitlements(bundleIdentifier)
		if (entitlements != null) {
			logger.debug("entitlements to merge: " + entitlements)
			return new ConfigurationFromMap(entitlements)
		} else if (codesignParameters.entitlements != null) {
			logger.info("Merging entitlements from the codesign entitlements parameter")
			logger.debug("entitlements to merge: " + codesignParameters.entitlements)
			return new ConfigurationFromMap(codesignParameters.entitlements)
		} else {
			File xcentFile = getXcentFile(bundle)
			if (xcentFile != null) {
				logger.debug("Merging entitlements from the xcent file found in the archive")
				return new ConfigurationFromPlist(xcentFile)
			}
		}
		logger.debug("No entitlements configuration found for merging, so use only the plain entitlements extracted from the provisioning profile")
		return new ConfigurationFromMap([:])
	}

	private void codeSignEmbeddedBundle(File bundle) {


		if (codesignParameters.type == Type.iOS) {
			embeddedBundleEntriesForIOS(bundle).each {
				performCodesign(it)
			}
		} else {
			embeddedBundleEntriesForMacOS(bundle).each {
				if (it.name.endsWith(".app")) {
					performCodesign(it, null, true, true)
				} else if (it.isFile() && !it.name.endsWith("dylib")) {
					performCodesign(it, null, true, true)
				} else {
					performCodesign(it, null, true, false)
				}
			}
		}

	}


	private static List<File>embeddedBundleEntriesForIOS(File bundle) {
		File directory = new File(bundle, "Frameworks")
		if (!directory.exists()) {
			return []
		}
		List<File> result = []

		result.addAll(getFrameworkLibraries(directory))

		directory.traverse(maxDepth: 0) { file ->
			if (file.getName().toLowerCase().endsWith(".framework") ||
				file.getName().toLowerCase().endsWith(".app")) {

				result.add(file)
			}
		}
		return result
	}

	private List<File> embeddedBundleEntriesForMacOS(File bundle) {

		List<File> result = []
		File directory = new File(bundle, "Contents/Frameworks")
		if (!directory.exists()) {
			return result
		}

		directory.traverse(type: DIRECTORIES, maxDepth: 0) { file ->

			if (!file.isDirectory()) {
				return
			}

			if (file.getName().toLowerCase().endsWith(".framework")) {
				result.addAll(getFrameworkVersions(file))
			}
			if (file.getName().toLowerCase().endsWith(".app")) {
				result.add(file)
			}

		}

		return result
	}

	static List<File> getFrameworkVersions(File directory) {
		logger.info("getFrameworkVersion in {}", directory)


		List<File> result = []
		new File(directory, "Versions").traverse(type: DIRECTORIES, maxDepth: 0) { file ->
			if (Files.isSymbolicLink(file.toPath())) {
				return
			}

			// collect all binaries in the subdirectory
			file.traverse(type: DIRECTORIES, maxDepth: 0) { subdirectory ->
				result.addAll(getFrameworkResourceExecutables(subdirectory))
			}


			result.addAll(getFrameworkLibraries(new File(file, "Libraries")))
			result << file
		}
		return result
	}

	static List<File> getFrameworkLibraries(File directory) {
		logger.info("getFrameworkVersionLibraries in {}", directory)
		List<File> result = []
		if (!directory.exists()) {
			return result
		}

		directory.traverse(maxDepth: 0) { file ->
			if (file.getName().toLowerCase().endsWith(".dylib")) {
				result << file
			}
		}

		return result
	}

	static List<File> getFrameworkResourceExecutables(File directory) {
		logger.info("getFrameworkResourceExecutables in {}", directory)
		List<File> result = []
		if (!directory.exists()) {
			return result
		}

		directory.traverse(maxDepth: 0) { file ->
			if (!file.isDirectory() && file.canExecute()) {
				result << file
			}
		}

		return result

	}

	public void performCodesign(File bundle) {
		this.performCodesign(bundle, null, false, false)
	}

	public void performCodesign(File bundle, File entitlements, boolean deep, boolean hardenRuntime) {
		logger.info("performCodesign {}", bundle)

		List<String> codesignCommand = []
		codesignCommand << "/usr/bin/codesign"
		codesignCommand << "--force"

		if (entitlements != null) {
			codesignCommand << "--entitlements"
			codesignCommand << entitlements.absolutePath
		}

		codesignCommand << "--sign"
		if (codesignParameters.signingIdentity != null) {
			codesignCommand << codesignParameters.signingIdentity
		} else {
			codesignCommand << "-"
		}
		if (deep) {
			codesignCommand << "--deep"
		}
		if (hardenRuntime) {
			codesignCommand << "--options=runtime"
		}
		codesignCommand << "--verbose"
		codesignCommand << bundle.absolutePath

		if (codesignParameters.signingIdentity != null) {
			codesignCommand << "--keychain"
			codesignCommand << codesignParameters.keychain.absolutePath
		}

		def environment = ["DEVELOPER_DIR": xcode.getPath() + "/Contents/Developer/"]
		commandRunner.run(codesignCommand, environment)

	}


	private String getIdentifierForBundle(File bundle) {
		File infoPlist

		if (codesignParameters.type == Type.iOS) {
			infoPlist = new File(bundle, "Info.plist");
		} else {
			infoPlist = new File(bundle, "Contents/Info.plist")
		}

		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier")
		return bundleIdentifier
	}

	ProvisioningProfileReader createProvisioningProfileReader(String bundleIdentifier, File provisionFile) {
		if (provisionFile == null) {
			if (codesignParameters.type == Type.iOS) {
				throw new IllegalStateException("No provisioning profile found for bundle identifier: " + bundleIdentifier)
			}
			// on OS X this is valid
			return null
		}

		return new ProvisioningProfileReader(provisionFile, this.commandRunner, this.plistHelper, codesignParameters.keychain)
	}

	File createEntitlementsFile(String bundleIdentifier, Configuration configuration) {
		// the settings from the xcent file are merge with the settings from entitlements from the provisioning profile
		if (bundleIdentifier == null) {
			logger.debug("not bundleIdentifier specified")
			return null
		}

		logger.info("createEntitlementsFile for bundleIdentifier {}", bundleIdentifier)


		ProvisioningProfileReader reader = ProvisioningProfileReader.getReaderForIdentifier(bundleIdentifier, codesignParameters.mobileProvisionFiles, this.commandRunner, codesignParameters.keychain, this.plistHelper)
		if (reader == null) {
			return null
		}

		// set keychain access group
		List<String> keychainAccessGroup = []

		if (reader != null) {
			String applicationPrefix = reader.getApplicationIdentifierPrefix()
			keychainAccessGroup = getKeychainAccessGroupFromEntitlements(configuration, applicationPrefix)
		}

		String basename = FilenameUtils.getBaseName(reader.provisioningProfile.path)
		File tmpDir = new File(System.getProperty("java.io.tmpdir"))
		File extractedEntitlementsFile = new File(tmpDir, "entitlements_" + basename + ".plist")
		reader.extractEntitlements(extractedEntitlementsFile, bundleIdentifier, keychainAccessGroup, configuration)
		extractedEntitlementsFile.deleteOnExit()
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
			logger.debug("Found xcent file in the archive: {}", result)
			return result
		}
		return null
	}


	List<String> getKeychainAccessGroupFromEntitlements(Configuration configuration, String applicationPrefix) {
		logger.info("getKeychainAccessGroupFromEntitlements configuration {}", configuration)
		List<String> result = []
		applicationPrefix = applicationPrefix + "."

		logger.info("configuration keys: {}", configuration.keys)
		logger.info("using application prefix: {}", applicationPrefix)
		List<String> keychainAccessGroups = configuration.getStringArray("keychain-access-groups")
		logger.info("keychain-access-groups from configuration: {}", result)

		keychainAccessGroups.each { item ->
			if (StringUtils.isNotEmpty(applicationPrefix) && item.startsWith(applicationPrefix)) {
				result << item.replace(applicationPrefix, ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX)
			} else {
				result << item
			}
		}
		logger.info("modified keychain-access-group: {}", result)
		return result
	}

}
