package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
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

	Project project
	File projectFile

	private String rootObjectKey;
	private XMLPropertyListConfiguration config

	boolean isOSX = false;

	XcodeProjectFile(Project project, File projectFile) {
		this.project = project
		this.projectFile = projectFile
	}


	void parse() {
		this.project.logger.lifecycle("Parse project file: " + projectFile.absolutePath)
		if (!this.projectFile.exists()) {
			throw new IllegalArgumentException("Project file does not exist: " + this.projectFile)
		}

		if (project.xcodebuild.target == null) {
			throw new IllegalArgumentException("'xcodebuild.target' is null");
		}



		def buildRoot = project.buildDir
		if (!buildRoot.exists()) {
			buildRoot.mkdirs()
		}

		File projectPlistFile = new File(buildRoot, "project.plist")

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
			project.xcodebuild.productType = "appex"
		}


		String buildConfiguration = getBuildConfiguration()

		if (StringUtils.isEmpty(project.xcodebuild.productName)) {
			String key = "objects." + buildConfiguration + ".buildSettings.PRODUCT_NAME"
			VariableResolver resolver = new VariableResolver(project);
			project.xcodebuild.productName = resolver.resolve(getString(key))
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
				project.xcodebuild.devices = Devices.PHONE;
			} else if (devicesString.equals("2")) {
				project.xcodebuild.devices = Devices.PAD;
			}

		}

		if (project.xcodebuild.infoPlist == null) {
			String key = "objects." + buildConfiguration + ".buildSettings.INFOPLIST_FILE"
			project.xcodebuild.infoPlist = getString(key)
			logger.info("infoPlist: {}", project.xcodebuild.infoPlist)
		}
	}

	void verifyTarget() {
		String forTargetName = project.xcodebuild.target
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName.equals(forTargetName)) {
				return;
			}
		}
		throw new IllegalArgumentException("Target '" + project.xcodebuild.target + "' not found in project")
	}

	String getValueFromTarget(String key) {
		String forTargetName = project.xcodebuild.target
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName.equals(forTargetName)) {
				return getString("objects." + target + key)
			}
		}

	}


	String getBuildConfiguration(String key) {
		String forBuildName = project.xcodebuild.configuration

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
		String forTargetName = project.xcodebuild.target

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
