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

import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.openbakery.signing.Signing
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public enum Devices {
	UNIVERSAL,
	PHONE,
	PAD,
	WATCH
}

public enum Type {
	iOS("iOS"),
	OSX("OSX"),
	tvOS("tvOS");


	String value;

	public Type(String value) {
		this.value = value;
	}

	public static Type typeFromString(String string) {

		for (Type type in Type.values()) {
			if (type.value.equalsIgnoreCase(string)) {
				return type;
			}
		}
		return iOS;
	}
}


class XcodeBuildPluginExtension {
	public final static KEYCHAIN_NAME_BASE = "gradle-"


	private static Logger logger = LoggerFactory.getLogger(XcodeBuildPluginExtension.class)


	String infoPlist = null
	String scheme = null
	String configuration = 'Debug'
	boolean simulator = true
	Type type = Type.iOS

	String target = null
	Object dstRoot
	Object objRoot
	Object symRoot
	Object sharedPrecompsDir
	Object derivedDataPath
	String sourceDirectory = '.'
	Signing signing = null
	def additionalParameters = null
	String bundleNameSuffix = null
	List<String> arch = null
	String workspace = null
	String version = null
	Map<String, String> environment = null
	String productName = null
	String bundleName = null
	String productType = "app"
	String ipaFileName = null

	Devices devices = Devices.UNIVERSAL;
	List<Destination> availableSimulators = []

	Set<Destination> destinations = null

	String xcodePath = null
	CommandRunner commandRunner
	VariableResolver variableResolver
	PlistHelper plistHelper

	HashMap<String, BuildTargetConfiguration> projectSettings = new HashMap<>()


	/**
	 * internal parameters
	 */
	private final Project project

	public XcodeBuildPluginExtension(Project project) {
		this.project = project;
		this.signing = new Signing(project)
		this.variableResolver = new VariableResolver(project)
		commandRunner = new CommandRunner()
		plistHelper = new PlistHelper(this.project, commandRunner)


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

		this.derivedDataPath = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("derivedData")
		}

	}

	String getWorkspace() {
		if (workspace != null) {
			return workspace
		}
		String[] fileList = project.projectDir.list(new SuffixFileFilter(".xcworkspace"))
		if (fileList.length) {
			return fileList[0]
		}
		return null
	}

	void setDerivedDataPath(File derivedDataPath) {
		this.derivedDataPath = derivedDataPath
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

	File getDerivedDataPath() {
		return project.file(derivedDataPath)
	}

	void signing(Closure closure) {
		ConfigureUtil.configure(closure, this.signing)
	}


	boolean isSimulatorBuildOf(Type expectedType) {
		if (type != expectedType) {
			return false;
		}
		return this.simulator;
	}

	boolean isDeviceBuildOf(Type expectedType) {
		if (type != expectedType) {
			return false;
		}
		return !this.simulator
	}

	void destination(Closure closure) {
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		if (destinations == null) {
			destinations = [] as Set
		}

		destinations << destination

/*
		if (isSimulatorBuild()) {
			// filter only on simulator builds
			destinations.addAll(findMatchingDestinations(destination))
		} else {
			destinations << destination
		}

		*/
	}

	boolean matches(String first, String second) {
		if (first != null && second == null) {
			return true;
		}

		if (first == null && second != null) {
			return true;
		}

		if (first.equals(second)) {
			return true;
		}

		if (second.matches(first)) {
			return true;
		}

		return false;

	}

	List<Destination> findMatchingDestinations(Destination destination) {
		def result = [];


		logger.debug("finding matching destination for: {}", destination)

		for (Destination device in availableSimulators) {
			if (!matches(destination.platform, device.platform)) {
				//logger.debug("{} does not match {}", device.platform, destination.platform);
				continue
			}
			if (!matches(destination.name, device.name)) {
				//logger.debug("{} does not match {}", device.name, destination.name);
				continue
			}
			if (!matches(destination.arch, device.arch)) {
				//logger.debug("{} does not match {}", device.arch, destination.arch);
				continue
			}
			if (!matches(destination.id, device.id)) {
				//logger.debug("{} does not match {}", device.id, destination.id);
				continue
			}
			if (!matches(destination.os, device.os)) {
				//logger.debug("{} does not match {}", device.os, destination.os);
				continue
			}

			logger.debug("FOUND matching destination: {}", device)

			result << device

		}


		return result.asList();
	}

	List<Destination> getAvailableDestinations() {

		logger.debug("getAvailableDestinations")
		def availableDestinations = []


		if (type == Type.OSX) {
			availableDestinations << new Destination("OS X", "OS X", "10.x")
			return availableDestinations
		}

		if (isSimulatorBuildOf(Type.iOS)) {
			// filter only on simulator builds

			logger.debug("is a simulator build")
			if (this.destinations != null) {

				logger.debug("checking destinations if they are available: {}", this.destinations)
				for (Destination destination in this.destinations) {
					availableDestinations.addAll(findMatchingDestinations(destination))
				}

				if (availableDestinations.isEmpty()) {
					logger.error("No matching simulators found for specified destinations: {}", this.destinations)
					throw new IllegalStateException("No matching simulators found!")
				}
			} else {

				logger.info("There was no destination configured that matches the available. Therefor all available destinations where taken.")

				switch (this.devices) {
					case Devices.PHONE:
						availableDestinations = availableSimulators.findAll {
							d -> d.name.contains("iPhone");
						};
						break;
					case Devices.PAD:
						availableDestinations = availableSimulators.findAll {
							d -> d.name.contains("iPad");
						};
						break;
					default:
						availableDestinations.addAll(availableSimulators);
						break;
				}
			}
		} else if (this.destinations != null) {
			logger.debug("is a device build so add all given device destinations")
			// on the device build add the given destinations
			availableDestinations.addAll(this.destinations)
		}


		logger.debug("availableDestinations: " + availableDestinations);

		return availableDestinations
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

	void setEnvironment(Object environment) {
		if (environment == null) {
			return
		}

		if (environment instanceof Map) {
			logger.debug("environment is Map: " + environment + " - " + environment.getClass().getName())
			this.index = index;
		} else {
			logger.debug("environment is string: " + environment + " - " + environment.getClass().getName())
			this.environment = new HashMap<String, String>();

			String environmentString = environment.toString()
			int index = environmentString.indexOf("=")
			if (index == -1) {
				environment.put(environmentString, null)
			} else {
				environment.put(environmentString.substring(0, index),environmentString.substring(index + 1))
			}
		}
	}





	void setVersion(String version) {
		this.version = version
		String installedXcodes = commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode")


		for (String xcode : installedXcodes.split("\n")) {


			File xcodeBuildFile = new File(xcode, "Contents/Developer/usr/bin/xcodebuild");
			if (xcodeBuildFile.exists()) {

				String xcodeVersion = commandRunner.runWithResult(xcodeBuildFile.absolutePath, "-version");

				def VERSION_PATTERN = ~/Xcode\s([^\s]*)\nBuild\sversion\s([^\s]*)/
				def matcher = VERSION_PATTERN.matcher(xcodeVersion)
				if (matcher.matches()) {
					String versionString = matcher[0][1]
					String buildNumberString = matcher[0][2]

					if (versionString.startsWith(version)) {
						xcodePath = xcode
						return
					}

					if (buildNumberString.equals(version)) {
						xcodePath = xcode
						return
					}
				}


			}
		}

		throw new IllegalStateException("No Xcode found with build number " + version);
	}

	String getXcodePath() {

		if (xcodePath == null) {
			String result = commandRunner.runWithResult("xcode-select", "-p")
			xcodePath = result - "/Contents/Developer"
		}
		return xcodePath;

	}



	String getXcodebuildCommand() {
		if (xcodePath != null) {
			return xcodePath + "/Contents/Developer/usr/bin/xcodebuild"
		}
		return "xcodebuild"
	}

	String getXcrunCommand() {
		if (xcodePath != null) {
			return xcodePath + "/Contents/Developer/usr/bin/xcrun"
		}
		return "xcrun"
	}


	String getValueFromInfoPlist(key) {
		try {
			logger.debug("project.projectDir {}", project.projectDir)
			File infoPlistFile = new File(project.projectDir, infoPlist)
			logger.debug("get value {} from plist file {}", key, infoPlistFile)
			return commandRunner.runWithResult([
							"/usr/libexec/PlistBuddy",
							infoPlistFile.absolutePath,
							"-c",
							"Print :" + key])
		} catch (IllegalStateException ex) {
			return null
		}
	}

	String getBundleName() {
		if (bundleName != null) {
			return bundleName
		}
		bundleName = getValueFromInfoPlist("CFBundleName")

		bundleName = variableResolver.resolve(bundleName);

		if (StringUtils.isEmpty(bundleName)) {
			bundleName = this.productName
		}
		return bundleName
	}

	File getOutputPath() {
		String path = getConfiguration()
		if (type == Type.iOS) {
			if (simulator) {
				path += "-iphonesimulator"
			} else {
				path += "-iphoneos"
			}
		}
		return new File(getSymRoot(), path)
	}


	BuildConfiguration getParent(BuildConfiguration buildSettings) {
		BuildConfiguration result = buildSettings
		File infoPlist = new File(project.projectDir, buildSettings.infoplist);
		String bundleIdentifier = plistHelper.getValueFromPlist(infoPlist, "WKCompanionAppBundleIdentifier")
		if (bundleIdentifier != null) {

			projectSettings.each { String key, BuildTargetConfiguration buildConfiguration ->

				BuildConfiguration settings = buildConfiguration.buildSettings[configuration];
				if (settings != null && settings.bundleIdentifier.equalsIgnoreCase(bundleIdentifier)) {
					result = settings
					return
				}
			}
		}
		return result;

	}


	File getApplicationBundle() {

		BuildTargetConfiguration buildConfiguration = projectSettings[target]
		if (buildConfiguration != null) {
			BuildConfiguration buildSettings = buildConfiguration.buildSettings[configuration];
			if (buildSettings != null && buildSettings.sdkRoot.equalsIgnoreCase("watchos")) {
				BuildConfiguration parent = getParent(buildSettings)
				return new File(getOutputPath(), parent.productName + "." + this.productType)
			}
		}
		return new File(getOutputPath(), getBundleName() + "." + this.productType)
	}






	void setType(String type) {
		this.type = Type.typeFromString(type);
	}


	void setSdk(String sdk) {
		throw new IllegalArgumentException("Settings the 'sdk' is not supported anymore. Use the 'type' parameter instead")
	}


	boolean getSimulator() {
		if (type == Type.OSX) {
			return false
		}
		return this.simulator
	}
}