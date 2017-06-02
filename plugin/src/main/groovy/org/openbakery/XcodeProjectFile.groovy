package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.openbakery.xcode.Devices
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This class parses the xcodeproject file and sets the proper values to project.xcodebuild
 *
 * Note: I have created this class to do a more complete parsing in the future:
 * Additional Note: The BuildTargetConfiguration and BuildConfiguration classes are now here to have more infos from the project file
 *
 * This file should be moved to the libxcode subproject, and the dependency to project.xcodebuild should be removed
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

	void loadConfig() {
		if (config != null) {
			// already loaded
			return
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
	}

	HashMap<String, BuildTargetConfiguration> getProjectSettings() {
		loadConfig()
		List<String> buildConfigurationNames = getBuildConfigurationNames(rootObjectKey)

		BuildTargetConfiguration projectBuildConfiguration = createProjectBuildConfiguration(rootObjectKey, buildConfigurationNames)

		HashMap<String, BuildTargetConfiguration> projectSettings = new HashMap<>()
		getTargets().each { String targetName, String target ->
			projectSettings.put(targetName, createBuildConfiguration(target, targetName, projectBuildConfiguration));
		}
		return projectSettings
	}


	BuildTargetConfiguration getBuildTargetConfiguration(String target) {
		HashMap<String, BuildTargetConfiguration> settings = getProjectSettings()
		if (settings.containsKey(target)) {
			return settings[target]
		}
		return null
	}


	BuildConfiguration getBuildConfiguration(String target, String configuration) {
		BuildTargetConfiguration settings = getBuildTargetConfiguration(target)
		if (settings != null && settings.buildSettings.containsKey(configuration)) {
			return settings.buildSettings[configuration]
		}
		return null
	}

	/* remove this method and update project.xcodebuild settings on the proper places */
	void parse() {
		this.project.logger.debug("Parse project file: " + projectFile.absolutePath)
		if (!this.projectFile.exists()) {
			throw new IllegalArgumentException("Project file does not exist: " + this.projectFile)
		}

		if (project.xcodebuild.target == null) {
			throw new IllegalArgumentException("'xcodebuild.target' is null");
		}

		BuildConfiguration settings = getBuildConfiguration(project.xcodebuild.target, project.xcodebuild.configuration)
		logger.debug("rootObjectKey {}", rootObjectKey);
		verifyTarget(project.xcodebuild.target)


		if (StringUtils.isEmpty(project.xcodebuild.productName)) {
			project.xcodebuild.productName = settings.productName
		}
		project.xcodebuild.productType = settings.productType
		String sdkRoot = settings.sdkRoot

		if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase("macosx")) {
			this.isOSX = true
		} else {
			project.xcodebuild.devices = settings.devices
		}

		if (project.xcodebuild.infoPlist == null) {
			project.xcodebuild.infoPlist = settings.infoplist
			logger.debug("infoPlist: {}", project.xcodebuild.infoPlist)
		}

	}


	Map<String, String> getTargets() {
		Map<String, String> targets = new HashMap<String, String>();
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			targets.put(targetName, target);
		}
		return targets;
	}

	void updateBuildSettings(BuildConfiguration buildSettings, String config, String targetIdentifier, String targetName) {

		String buildConfiguration = createBuildConfiguration(targetIdentifier, config)
		buildSettings.infoplist = getBuildSetting(buildConfiguration, "INFOPLIST_FILE")
		buildSettings.bundleIdentifier = getBuildSetting(buildConfiguration, "PRODUCT_BUNDLE_IDENTIFIER")
		buildSettings.productName = getBuildSetting(buildConfiguration, "PRODUCT_NAME")
		if (buildSettings.productName != null && buildSettings.productName.contains('$(TARGET_NAME)')) {
			buildSettings.productName = buildSettings.productName.replace('$(TARGET_NAME)', targetName)
		}
		buildSettings.sdkRoot = getBuildSetting(buildConfiguration, "SDKROOT")
		buildSettings.entitlements = getBuildSetting(buildConfiguration, "CODE_SIGN_ENTITLEMENTS")


		String type = getValueFromTarget(targetName, ".productType")
		if (type != null && type.endsWith("-extension")) {
			buildSettings.productType = "appex"
		} else {
			buildSettings.productType = "app"
		}


		String deviceFamily =  getBuildSetting(buildConfiguration, "TARGETED_DEVICE_FAMILY")
		if (deviceFamily == "1") {
			buildSettings.devices = Devices.PHONE
		} else if (deviceFamily == "2") {
			buildSettings.devices = Devices.PAD
		} else if (deviceFamily == "4") {
			buildSettings.devices = Devices.WATCH
		} else {
			buildSettings.devices = Devices.UNIVERSAL
		}

	}


	BuildTargetConfiguration createProjectBuildConfiguration(String target, List<String> buildConfigurationNames) {
		BuildTargetConfiguration result = new BuildTargetConfiguration()

		for (String configurationName : buildConfigurationNames) {
			BuildConfiguration buildConfiguration = new BuildConfiguration(target)
			updateBuildSettings(buildConfiguration, configurationName, target, "")
			result.buildSettings[configurationName] = buildConfiguration
		}
		return result
	}

	BuildTargetConfiguration createBuildConfiguration(String targetIdentifier, String target, BuildTargetConfiguration projectBuildConfiguration) {
		BuildTargetConfiguration result = new BuildTargetConfiguration()

		projectBuildConfiguration.buildSettings.each { buildConfigurationName, buildConfiguration ->
			BuildConfiguration configuration = new BuildConfiguration(target, buildConfiguration)
			configuration.targetIdentifier = targetIdentifier
			updateBuildSettings(configuration, buildConfigurationName, targetIdentifier, target)
			result.buildSettings[buildConfigurationName] = configuration
		}

		return result
	}

	String getBuildSetting(String buildConfiguration, String key) {
		return getString("objects.${buildConfiguration}.buildSettings.${key}")
	}

	void verifyTarget(String forTargetName) {
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName == forTargetName) {
				return
			}
		}
		throw new IllegalArgumentException("Target '" + forTargetName + "' not found in project")
	}

	String getValueFromTarget(String forTargetName, String key) {
		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName == forTargetName) {
				return getString("objects." + target + key)
			}
		}

	}


	List<String> getBuildConfigurationNames(String targetIdentifier) {

		ArrayList<String> result = new ArrayList<>()
		String buildConfigurationList = getString("objects." + targetIdentifier + ".buildConfigurationList")
		def buildConfigurations = getList("objects." + buildConfigurationList + ".buildConfigurations")
		for (buildConfigurationsItem in buildConfigurations) {
			def buildName = getString("objects." + buildConfigurationsItem + ".name")
			result.add(buildName)
		}
		return result
	}

	String createBuildConfiguration(String target, String configuration) {
		String buildConfigurationList = getString("objects." + target + ".buildConfigurationList")
		def buildConfigurations = getList("objects." + buildConfigurationList + ".buildConfigurations")
		for (buildConfigurationsItem in buildConfigurations) {
			def buildName = getString("objects." + buildConfigurationsItem + ".name")

			if (buildName.equalsIgnoreCase(configuration)) {
				return buildConfigurationsItem
			}
		}
		return null
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
