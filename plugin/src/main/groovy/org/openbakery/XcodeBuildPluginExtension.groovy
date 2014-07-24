/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.text.DefaultEditorKit

enum Devices {
	UNIVERSAL,
	PHONE,
	PAD
}

class XcodeBuildPluginExtension {
	public final static KEYCHAIN_NAME_BASE = "gradle-"


	private static Logger logger = LoggerFactory.getLogger(XcodeBuildPluginExtension.class)


	def String infoPlist = null
	def String scheme = null
	def String configuration = 'Debug'
	def String sdk = 'iphonesimulator'
	def target = null
	def Object dstRoot
	def Object objRoot
	def Object symRoot
	def Object sharedPrecompsDir
	def String sourceDirectory = '.'
	def Signing signing = null
	def additionalParameters = null
	def String bundleNameSuffix = null
	def List<String> arch = null
	def String workspace = null
	boolean isOSX = false;
	Devices devices = Devices.UNIVERSAL;
	List<String> availableDevices = []

	def List<Destination> destinations = null

	/**
	 * internal parameters
	 */
	private final Project project

	public XcodeBuildPluginExtension(Project project) {
		this.project = project;
		this.signing = new Signing(project)

		this.dstRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("dst")
		}

		this.objRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("obj")
		}

		this.symRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("sym")
		}

		this.sharedPrecompsDir = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("shared")
		}

	}

	void setDstRoot(File dstRoot) {
		this.dstRoot = dstRoot
	}

	void setObjRoot(File objRoot) {
		this.objRoot = objRoot
	}

	void setSymRoot(File symRoot) {
		this.symRoot = symRoot
	}

	void setSharedPrecompsDir(File sharedPrecompsDir) {
		this.sharedPrecompsDir = sharedPrecompsDir
	}

	File getDstRoot() {
		return project.file(dstRoot)
	}

	File getObjRoot() {
		return project.file(objRoot)
	}

	File getSymRoot() {
		return project.file(symRoot)
	}

	File getSharedPrecompsDir() {
		return project.file(sharedPrecompsDir)
	}


	void signing(Closure closure) {
		ConfigureUtil.configure(closure, this.signing)
	}


	void destination(Closure closure) {
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		if (destinations == null) {
			destinations = new ArrayList<Destination>()
		}
		destinations.add(destination)
		logger.debug("adding destination: {}", destination)
	}

	List<Destination> getDestinations() {

		if (!this.destinations) {
			this.destinations = []
			switch (this.devices) {
				case Devices.PHONE:
					this.destinations = availableDevices.findAll {
						d -> d.name.contains("iPhone");
					};
					break;
				case Devices.PAD:
					this.destinations = availableDevices.findAll {
						d -> d.name.contains("iPad");
					};
					break;
				default:
					this.destinations.addAll(availableDevices);
					break;
			}
		}

			/*

			for (String simulator in availableSimulators) {
				Destination destination = new Destination();
				destination.platform = 'iOS Simulator'
				switch (this.devices) {
					case Devices.PHONE:
						if (name.contains("iPhone")) {
							destination.name = simulator;
							this.destinations.add(destination)
						}
						break;
					case Devices.PAD:
						if (name.contains("iPad")) {
							destination.name = simulator;
							this.destinations.add(destination)
						}
						break;
					default:
						destination.name = simulator;
						this.destinations.add(destination)
						break;
				}
			}
		}
*/
		logger.debug("this.destination: " + this.destinations);


		return this.destinations;
	}

	void setArch(Object arch) {
		if (arch instanceof List) {
			logger.debug("Arch is List: " + arch + " - " + arch.getClass().getName())
			this.arch = arch;
		} else {
			logger.debug("Arch is string: " + arch + " - " + arch.getClass().getName())
			this.arch = new ArrayList<String>();
			this.arch.add(arch.toString());
		}
	}


	void createiOS5DeviceList(CommandRunner commandRunner) {
		String xcodePath = commandRunner.runWithResult(["xcode-select", "-p"])
		File simulatorDirectory = new File(xcodePath, "Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices")
		String[] simulators = simulatorDirectory.list()
		for (String simulator in simulators) {
			Destination destination = new Destination();
			destination.platform = 'iOS Simulator'

			File infoPlistFile = new File(simulatorDirectory, simulator + "/Info.plist")
			String name = commandRunner.runWithResult([
										"/usr/libexec/PlistBuddy",
										infoPlistFile.absolutePath,
										"-c",
										"Print :displayName"
						])

			destination.name = name //FilenameUtils.getBaseName(simulator);
			availableDevices << destination;
		}
	}

	void finishConfiguration(Project project) {

		CommandRunner commandRunner = new CommandRunner()
		parseInfoFromProjectFile(project, commandRunner)

		if (isOSX) {
			return;
		}

		String version = commandRunner.runWithResult(["xcodebuild", "-version"])
		boolean isXcode5 = version.startsWith("Xcode 5");
		logger.debug("isXcode5 {}", isXcode5);


		if (isXcode5) {
			createiOS5DeviceList(commandRunner)
		} else {
			//simulatorDirectory = new File(xcodePath, "Platforms/iPhoneSimulator.platform/Developer/Library/CoreSimulator/Profiles/DeviceTypes")
		}

/*
		String[] simulators = simulatorDirectory.list()
		availableDevices = []
		for (String simulator in simulators) {
			Destination destination = new Destination();
			destination.platform = 'iOS Simulator'
			destination.name = FilenameUtils.getBaseName(simulator);
			availableDevices << destination;
		}
		*/

		logger.debug("availableSimulators: {}", availableDevices)

	}



	void parseInfoFromProjectFile(Project project, CommandRunner commandRunner) {

		logger.debug("using target: {}", this.target)
		def projectFileDirectory = project.projectDir.list(new SuffixFileFilter(".xcodeproj"))[0]
                def xcodeProjectDir = new File(project.projectDir, projectFileDirectory) // prepend project dir to support multi-project build
                def projectFile = new File(xcodeProjectDir, "project.pbxproj")

		def buildRoot = project.buildDir
		if (!buildRoot.exists()) {
			buildRoot.mkdirs()
		}

		def projectPlist = new File(buildRoot, "project.plist").absolutePath

		// convert ascii plist to xml so that commons configuration can parse it!
		commandRunner.run(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlist])

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(projectPlist))
		def rootObjectKey = config.getString("rootObject")
		logger.debug("rootObjectKey {}", rootObjectKey);

		List<String> list = config.getList("objects." + rootObjectKey + ".targets")

		for (target in list) {

			def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
			logger.debug("buildConfigurationList={}", buildConfigurationList)
			def targetName = config.getString("objects." + target + ".name")
			logger.debug("targetName: {}", targetName)


			if (targetName.equals(this.target)) {
				def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
				for (buildConfigurationsItem in buildConfigurations) {
					def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

					logger.debug("buildName: {} equals {}", buildName, this.configuration)

					if (buildName.equals(this.configuration)) {
						//String productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
						String sdkRoot = config.getString("objects." + buildConfigurationsItem + ".buildSettings.SDKROOT")
						if (StringUtils.isNotEmpty(sdkRoot) && sdkRoot.equalsIgnoreCase("macosx")) {
							// is os x build
							this.isOSX = true
						} else {
							String devicesString = config.getString("objects." + buildConfigurationsItem + ".buildSettings.TARGETED_DEVICE_FAMILY")

							if (devicesString.equals("1")) {
								this.devices = Devices.PHONE;
							} else if (devicesString.equals("2")) {
								this.devices = Devices.PAD;
							}

						}

						if (this.infoPlist == null) {
							infoPlist = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
							logger.info("infoPlist: {}", infoPlist)
						}

						logger.info("devices: {}", this.devices)
						logger.info("isOSX: {}", this.isOSX)
						return;
					}
				}
			}
		}
	}


}