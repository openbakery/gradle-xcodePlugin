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
		this.project.logger.info("Parse project file: " + projectFile.absolutePath)
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

		BuildConfiguration projectBuildConfiguration = createProjectBuildConfiguration(rootObjectKey)

		HashMap<String, BuildConfiguration> projectSettings = new HashMap<>()
		getTargets().each { String targetName, String target ->
			projectSettings.put(targetName, createBuildConfiguration(target, targetName, projectBuildConfiguration));
		}

		project.xcodebuild.projectSettings = projectSettings



		verifyTarget()


		String type = getValueFromTarget(".productType")
		if (type.endsWith("-extension")) {
			project.xcodebuild.productType = "appex"
		}


		String buildConfiguration = getBuildConfiguration()

		if (StringUtils.isEmpty(project.xcodebuild.productName)) {
			String key = "objects." + buildConfiguration + ".buildSettings.PRODUCT_NAME"
			VariableResolver resolver = new VariableResolver(project);
			project.xcodebuild.productName = resolver.resolve(getString(key))
		}

		String rootBuildConfigurationsItem = getRootBuildConfigurationsItem()

		String sdkRoot = getString("objects.${buildConfiguration}.buildSettings.SDKROOT")
		if (sdkRoot == null) {
			sdkRoot = getString("objects." + rootBuildConfigurationsItem + ".buildSettings.SDKROOT")
		}

		project.xcodebuild.setSdkRoot(sdkRoot)

		if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase("macosx")) {
			this.isOSX = true
		} else {

			String devicesString = getString("objects.${buildConfiguration}.buildSettings.TARGETED_DEVICE_FAMILY")

			if (devicesString.equals("1")) {
				project.xcodebuild.devices = Devices.PHONE;
			} else if (devicesString.equals("2")) {
				project.xcodebuild.devices = Devices.PAD;
			} else if (devicesString.equals("4")) {
				project.xcodebuild.devices = Devices.WATCH;
			}

		}

		if (project.xcodebuild.infoPlist == null) {
			String key = "objects." + buildConfiguration + ".buildSettings.INFOPLIST_FILE"
			project.xcodebuild.infoPlist = getString(key)
			logger.info("infoPlist: {}", project.xcodebuild.infoPlist)
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

	void updateBuildSettings(BuildSettings buildSettings, String config, String target, String targetName) {

		String buildConfiguration = getBuildConfiguration(target, config)
		buildSettings.infoplist = getBuildSetting(buildConfiguration, target, "INFOPLIST_FILE")
		buildSettings.bundleIdentifier = getBuildSetting(buildConfiguration, target, "PRODUCT_BUNDLE_IDENTIFIER")
		buildSettings.productName = getBuildSetting(buildConfiguration, target, "PRODUCT_NAME")
		if (buildSettings.productName.equals('$(TARGET_NAME)')) {
			buildSettings.productName = targetName
		}
		buildSettings.sdkRoot = getBuildSetting(buildConfiguration, target, "SDKROOT")

		String deviceFamily =  getBuildSetting(buildConfiguration, target, "TARGETED_DEVICE_FAMILY")
		if (deviceFamily.equals("1")) {
			buildSettings.devices = Devices.PHONE;
		} else if (deviceFamily.equals("2")) {
			buildSettings.devices = Devices.PAD;
		} else if (deviceFamily.equals("4")) {
			buildSettings.devices = Devices.WATCH;
		} else {
			buildSettings.devices = Devices.UNIVERSAL;
		}

	}


	BuildConfiguration createProjectBuildConfiguration(String target) {
		BuildConfiguration result = new BuildConfiguration()
		result.debug = new BuildSettings();
		updateBuildSettings(result.debug, "debug", target, "");
		result.release = new BuildSettings();
		updateBuildSettings(result.release, "release", target, "");
		return result
	}

	BuildConfiguration createBuildConfiguration(String target, String targetName, BuildConfiguration projectBuildConfiguration) {
		BuildConfiguration result = new BuildConfiguration()

		result.debug = new BuildSettings(projectBuildConfiguration.debug);
		updateBuildSettings(result.debug, "debug", target, targetName);
		result.release = new BuildSettings(projectBuildConfiguration.release);
		updateBuildSettings(result.release, "release", target, targetName);

		return result
	}

	String getBuildSetting(String buildConfiguration, String target, String key) {
		return getString("objects.${buildConfiguration}.buildSettings.${key}")
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


	String getBuildConfiguration(String target, String configuration) {


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

	String getBuildConfiguration() {
		String forTargetName = project.xcodebuild.target

		List<String> list = getList("objects." + rootObjectKey + ".targets")
		for (target in list) {
			def targetName = getString("objects." + target + ".name")
			if (targetName.equals(forTargetName)) {
				return getBuildConfiguration(target, project.xcodebuild.configuration)
			}
		}
		throw new IllegalArgumentException("No Build configuration for for target: " + forTargetName)
	}


	String getRootBuildConfigurationsItem() {
		return getBuildConfiguration(rootObjectKey, project.xcodebuild.configuration)
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
