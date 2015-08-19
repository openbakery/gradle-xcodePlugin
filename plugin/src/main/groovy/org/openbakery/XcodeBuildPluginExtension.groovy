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
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.openbakery.internal.XcodeBuildSpec
import org.openbakery.signing.Signing
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class XcodeBuildPluginExtension {
	public final static KEYCHAIN_NAME_BASE = "gradle-"


	private static Logger logger = LoggerFactory.getLogger(XcodeBuildPluginExtension.class)

	XcodeBuildSpec buildSpec


	Object derivedDataPath
	String sourceDirectory = '.'
	Signing signing = null
	List<String> arch = null
	String version = null


	List<Destination> availableSimulators = []

	Set<Destination> destinations = null

	String xcodePath = null
	CommandRunner commandRunner
	VariableResolver variableResolver;

	/**
	 * internal parameters
	 */
	private final Project project

	public XcodeBuildPluginExtension(Project project) {

		this.project = project;
		this.signing = new Signing(project)
		this.buildSpec = new XcodeBuildSpec(project);

		this.variableResolver = new VariableResolver(project.projectDir, this.buildSpec)
		commandRunner = new CommandRunner()


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


	File getDerivedDataPath() {
		return project.file(derivedDataPath)
	}

	void signing(Closure closure) {
		ConfigureUtil.configure(closure, this.signing)
	}


	boolean isDeviceBuild() {
		return this.isSdk(XcodePlugin.SDK_IPHONEOS)
	}

	boolean isSimulatorBuild() {
		return this.isSdk(XcodePlugin.SDK_IPHONESIMULATOR)
	}


	void destination(Closure closure) {
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		if (destinations == null) {
			destinations = [] as Set
		}

		destinations << destination
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

	// TODO: move to Build Spec
	List<Destination> getAvailableDestinations() {
		def availableDestinations = []

		if (isSdk(XcodePlugin.SDK_MACOSX)) {
			availableDestinations << new Destination("OS X", "OS X", "10.x")
			return availableDestinations
		}


		if (isSimulatorBuild()) {
			// filter only on simulator builds
			for (Destination destination in this.destinations) {
				availableDestinations.addAll(findMatchingDestinations(destination))
			}

			if (availableDestinations.isEmpty()) {
				logger.info("There was no destination configured that matches the available. Therefor all available destinations where taken.")

				switch (this.buildSpec.devices) {
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
			// on the device build add the given destinations
			availableDestinations.addAll(this.destinations)

		}

		logger.debug("availableDestinations: " + availableDestinations);

		return availableDestinations
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





	boolean isSdk(String expectedSDK) {
		return this.buildSpec.isSdk(expectedSDK)
	}

	void setTarget(String target) {
		this.buildSpec.target = target
	}

	void setScheme(String scheme) {
		this.buildSpec.scheme = scheme
	}

	void setConfiguration(String configuration) {
		this.buildSpec.configuration = configuration
	}


	void setSdk(String sdk) {
		this.buildSpec.sdk = sdk
	}


	void setIpaFileName(String ipaFileName) {
		this.buildSpec.ipaFileName = ipaFileName
	}

	void setWorkspace(String workspace) {
		this.buildSpec.setWorkspace(workspace)
	}

	void setProductName(String productName) {
		this.buildSpec.setProductName(productName)
	}

	void setDevices(Devices devices) {
		this.buildSpec.setDevices(devices)
	}

	void setInfoPlist(String infoPlist) {
		this.buildSpec.setInfoPlist(infoPlist)
	}

	void setBundleName(String bundleName) {
		this.buildSpec.setBundleName(bundleName)
	}

	void setProductType(String productType) {
		this.buildSpec.setProductType(productType)
	}


	void setSymRoot(Object symRoot) {
		this.buildSpec.setSymRoot(symRoot)
	}

	void setDstRoot(File dstRoot) {
		this.buildSpec.setDstRoot(dstRoot)
	}

	void setObjRoot(File objRoot) {
		this.buildSpec.setObjRoot(objRoot)
	}

	void setSharedPrecompsDir(File sharedPrecompsDir) {
		this.buildSpec.setSharedPrecompsDir(sharedPrecompsDir)
	}

	void setBundleNameSuffix(String bundleNameSuffix) {
		this.buildSpec.setBundleNameSuffix(bundleNameSuffix)
	}

	void setAdditionalParameters(Object additionalParameters) {
		this.buildSpec.setAdditionalParameters(additionalParameters)
	}

	void setArch(Object parameters) {
		this.buildSpec.setArch(parameters)
	}

	void setEnvironment(Object parameters) {
		this.buildSpec.setEnvironment(parameters)
	}

	Signing getSigning() {
		return this.buildSpec.signing
	}


	public XcodeBuildSpec spec() {
		return new XcodeBuildSpec(project)
	}

}