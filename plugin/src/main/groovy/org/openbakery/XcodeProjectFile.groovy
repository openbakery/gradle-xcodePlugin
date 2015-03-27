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

	boolean isOSX = false;

	XcodeProjectFile(Project project, File projectFile) {
		this.project = project
		this.projectFile = projectFile
	}


	void parse() {

		if (!this.projectFile.exists()) {
			throw new IllegalArgumentException("Project file does not exist: " + this.projectFile)
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

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(projectPlistFile)
		def rootObjectKey = config.getString("rootObject")
		logger.debug("rootObjectKey {}", rootObjectKey);

		List<String> list = config.getList("objects." + rootObjectKey + ".targets")

		for (target in list) {

			def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
			logger.debug("buildConfigurationList={}", buildConfigurationList)
			def targetName = config.getString("objects." + target + ".name")
			logger.debug("targetName: {}", targetName)


			if (targetName.equals(project.xcodebuild.target)) {

				if (StringUtils.isEmpty(project.xcodebuild.productName)) {
					project.xcodebuild.productName = config.getString("objects." + target + ".productName")
				}
				String type = config.getString("objects." + target + ".productType")
				if (StringUtils.isNotEmpty(type) &&  type.equalsIgnoreCase("com.apple.product-type.app-extension")) {
					project.xcodebuild.productType = "appex"
				}

				def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
				for (buildConfigurationsItem in buildConfigurations) {
					def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

					logger.debug("buildName: {} equals {}", buildName, project.xcodebuild.configuration)

					if (buildName.equals(project.xcodebuild.configuration)) {
						//String productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
						String sdkRoot = config.getString("objects." + buildConfigurationsItem + ".buildSettings.SDKROOT")
						if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase("macosx")) {
							// is os x build
							this.isOSX = true
						} else {
							String devicesString = config.getString("objects." + buildConfigurationsItem + ".buildSettings.TARGETED_DEVICE_FAMILY")

							if (devicesString.equals("1")) {
								project.xcodebuild.devices = Devices.PHONE;
							} else if (devicesString.equals("2")) {
								project.xcodebuild.devices = Devices.PAD;
							}

						}

						if (project.xcodebuild.infoPlist == null) {
							project.xcodebuild.infoPlist = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
							logger.info("infoPlist: {}", project.xcodebuild.infoPlist)
						}

						logger.info("devices: {}", project.xcodebuild.devices)
						logger.info("isOSX: {}", this.isOSX)
						return;
					}
				}
			}

		}
		logger.warn("WARNING: given target '" + project.xcodebuild.target + "' in the xcode project file")
	}


}
