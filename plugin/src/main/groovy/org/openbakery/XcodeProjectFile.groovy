package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.lang.StringUtils
import org.openbakery.internal.XcodeBuildSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class parses the xcodeproject file and sets the proper values to project.xcodebuild
 *
 * Note: I have created this class to do a more complete parsing in the future
 *
 * Created by rene on 16.02.15.
 */
class XcodeProjectFile {

	private static Logger logger = LoggerFactory.getLogger(XcodeProjectFile.class)

	CommandRunner commandRunner = new CommandRunner()

	File projectFile
	File buildDirectory
	XcodeBuildSpec buildSpec
	VariableResolver resolver

	private String rootObjectKey;
	private XMLPropertyListConfiguration config

	boolean isOSX = false;

	XcodeProjectFile(File projectFile, File buildDirectory, XcodeBuildSpec buildSpec) {
		this.projectFile = projectFile
		this.buildDirectory = buildDirectory
		this.buildSpec = buildSpec
		this.resolver = new VariableResolver(projectFile.parentFile, buildSpec);

	}


	void parse() {
		logger.info("Parse project file: " + projectFile.absolutePath)
		if (!this.projectFile.exists()) {
			throw new IllegalArgumentException("Project file does not exist: " + this.projectFile)
		}

		if (buildSpec.target == null) {
			throw new IllegalArgumentException("'target' is null");
		}

		if (!buildDirectory.exists()) {
			buildDirectory.mkdirs()
		}

		File projectPlistFile = new File(buildDirectory, "project.plist")

		if (projectPlistFile.exists()) {
			projectPlistFile.delete()
		}

		// convert ascii plist to xml so that commons configuration can parse it!
		commandRunner.run(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlistFile.absolutePath])

		config = new XMLPropertyListConfiguration(projectPlistFile)
		rootObjectKey = getString("rootObject")


		logger.debug("rootObjectKey {}", rootObjectKey);

		verifyTarget()


		String type = getValueFromTarget(".productType")
		if ("com.apple.product-type.app-extension".equalsIgnoreCase(type)) {
			buildSpec.productType = "appex"
		}


		String buildConfiguration = getBuildConfiguration()

		if (StringUtils.isEmpty(buildSpec.productName)) {
			String key = "objects." + buildConfiguration + ".buildSettings.PRODUCT_NAME"
			buildSpec.productName = resolver.resolve(getString(key))
		}

		String rootBuildConfigurationsItem = getRootBuildConfigurationsItem()

		String sdkRoot = getString("objects.{buildConfiguration}.buildSettings.SDKROOT")
		if (sdkRoot == null) {
			sdkRoot = getString("objects." + rootBuildConfigurationsItem + ".buildSettings.SDKROOT")
		}

		if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase(XcodePlugin.SDK_MACOSX)) {
			this.isOSX = true
		} else {

			String devicesString = getString("objects.{buildConfiguration}.buildSettings.TARGETED_DEVICE_FAMILY")

			if (devicesString.equals("1")) {
				buildSpec.devices = Devices.PHONE;
			} else if (devicesString.equals("2")) {
				buildSpec.devices = Devices.PAD;
			}

		}
		logger.info("infoPlist: {}", buildSpec.infoPlist)
		if (buildSpec.infoPlist == null) {
			String key = "objects." + buildConfiguration + ".buildSettings.INFOPLIST_FILE"
			buildSpec.infoPlist = getString(key)
			logger.info("infoPlist: {}", buildSpec.infoPlist)
		}
	}

	void verifyTarget() {
		String forTargetName = buildSpec.target
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName.equals(forTargetName)) {
				return;
			}
		}
		throw new IllegalArgumentException("Target '" + buildSpec.target + "' not found in project")
	}

	String getValueFromTarget(String key) {
		String forTargetName = buildSpec.target
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName.equals(forTargetName)) {
				return getString("objects." + target + key)
			}
		}

	}


	String getBuildConfiguration(String key) {
		String forBuildName = buildSpec.configuration

		String buildConfigurationList = getString("objects." + key + ".buildConfigurationList")
		def buildConfigurations = getList("objects." + buildConfigurationList + ".buildConfigurations")
		for (buildConfigurationsItem in buildConfigurations) {
			def buildName = getString("objects." + buildConfigurationsItem + ".name")

			if (buildName.equals(forBuildName)) {
				return buildConfigurationsItem
			}
		}
	}

	String getBuildConfiguration() {
		String forTargetName = buildSpec.target

		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName.equals(forTargetName)) {
				return getBuildConfiguration(target)
			}
		}
		throw new IllegalArgumentException("No Build configuration for for target: " + forTargetName)
	}


	String getRootBuildConfigurationsItem() {
		return getBuildConfiguration(rootObjectKey)
	}


	private List getList(String key) {
		List result = config.getList(key)
		logger.debug("List {}={}", key, result)
		return result
	}

	private String getString(String key) {
		String result = config.getString(key)
		logger.debug("String {}={}", key, result)
		return result
	}
}
