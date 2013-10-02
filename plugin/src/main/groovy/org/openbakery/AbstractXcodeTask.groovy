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
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task

/**
 *
 * @author René Pirringer
 *
 */
abstract class AbstractXcodeTask extends DefaultTask {


	CommandRunner commandRunner

	AbstractXcodeTask() {
		commandRunner = new CommandRunner()
	}


	/**
	 * Copies a file to a new location
	 *
	 * @param source
	 * @param destination
	 */
	def copy(File source, File destination) {
		logger.quiet("Copy '{}' -> '{}'", source, destination);
		FileUtils.copyFile(source, destination)
	}

	/**
	 * Downloads a file from the given address and stores it in the given directory
	 *
	 * @param toDirectory
	 * @param address
	 * @return
	 */
	def download(File toDirectory, String address) {
		if (!toDirectory.exists()) {
			toDirectory.mkdirs()
		}
		File destinationFile = new File(toDirectory, address.tokenize("/")[-1])
		def file = new FileOutputStream(destinationFile)
		def out = new BufferedOutputStream(file)
		out << new URL(address).openStream()
		out.close()
		return destinationFile.absolutePath
	}


	def runCommand(String directory, List<String> commandList, Map<String, String> environment) {
		commandRunner.runCommand(directory, commandList, environment, null)
	}

	def runCommand(String directory, List<String> commandList) {
		commandRunner.runCommand(directory, commandList)
	}

	def runCommand(List<String> commandList) {
		commandRunner.runCommand(commandList)
	}

	def runCommandWithResult(List<String> commandList) {
		commandRunner.runCommandWithResult(commandList)
	}

	def runCommandWithResult(String directory, List<String> commandList) {
		commandRunner.runCommandWithResult(directory, commandList)
	}

	def runCommandWithResult(String directory, List<String> commandList, Map<String, String> environment) {
		commandRunner.runCommandWithResult(directory, commandList, environment)
	}

	/**
	 *
	 * @return the absolute path to the generated app bundle
	 */
	def getAppBundleName() {
		//println project.xcodebuild.symRoot
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		def fileList = buildOutputDirectory.list(
						[accept: {d, f -> f ==~ /.*app/ }] as FilenameFilter
		).toList()
		if (fileList.size() == 0) {
			throw new IllegalStateException("No App Found in directory " + buildOutputDirectory.absolutePath)
		}
		return buildOutputDirectory.absolutePath + "/" + fileList[0]
	}

	/**
	 * Reads the value for the given key from the given plist
	 *
	 * @param plist
	 * @param key
	 * @return returns the value for the given key
	 */
	def getValueFromPlist(plist, key) {
		try {
			return runCommandWithResult([
							"/usr/libexec/PlistBuddy",
							plist,
							"-c",
							"Print :" + key])
		} catch (IllegalStateException ex) {
			return null
		}
	}

	/**
	 *
	 * @return the path the the Info.plist for this project
	 */
	def getInfoPlist() {
		def infoPlist = project.xcodebuild.infoPlist

		if (infoPlist == null) {
			infoPlist = getInfoPlistFromProjectFile()
		}
		logger.debug("Using Info.plist: {}", infoPlist);
		return infoPlist
	}

	def getAppBundleInfoPlist() {
		File infoPlistFile = new File(getAppBundleName() + "/Info.plist")
		if (infoPlistFile.exists()) {

			def convertedPlist = new File(project.buildDir, FilenameUtils.getName(infoPlistFile.getName()))
			//plutil -convert xml1 "$BINARY_INFO_PLIST" -o "${INFO_PLIST}.plist"

			def convertCommand = [
							"plutil",
							"-convert",
							"xml1",
							infoPlistFile.absolutePath,
							"-o",
							convertedPlist.absolutePath
			]

			runCommand(convertCommand)

			return convertedPlist.absolutePath
		}
		return null
	}

	def getInfoPlistFromProjectFile() {
		def projectFileDirectory = project.projectDir.list(new SuffixFileFilter(".xcodeproj"))[0]
		def projectFile = new File(projectFileDirectory, "project.pbxproj")

		def buildRoot = project.buildDir
		if (!buildRoot.exists()) {
			buildRoot.mkdirs()
		}

		def projectPlist = new File(buildRoot, "project.plist").absolutePath

		// convert ascii plist to xml so that commons configuration can parse it!
		runCommand(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlist])

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(projectPlist))
		def rootObjectKey = config.getString("rootObject")
		logger.debug("rootObjectKey {}", rootObjectKey);

		List<String> list = config.getList("objects." + rootObjectKey + ".targets")

		for (target in list) {

			def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
			logger.debug("buildConfigurationList={}", buildConfigurationList)
			def targetName = config.getString("objects." + target + ".name")
			logger.debug("targetName: {}", targetName)


			if (targetName.equals(project.xcodebuild.target)) {
				def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
				for (buildConfigurationsItem in buildConfigurations) {
					def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

					logger.debug("buildName: {} equals {}", buildName, project.xcodebuild.configuration)

					if (buildName.equals(project.xcodebuild.configuration)) {
						def productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
						def plistFile = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
						logger.debug("productName: {}", productName)
						logger.debug("plistFile: {}", plistFile)
						return plistFile
					}
				}
			}
		}
	}

	def getOSVersion() {
		Version result = new Version()
		String versionString = System.getProperty("os.version")
		Scanner scanner = new Scanner(versionString).useDelimiter("\\.")
		if (scanner.hasNext()) {
			result.major = scanner.nextInt()
		}
		if (scanner.hasNext()) {
			result.minor = scanner.nextInt()
		}
		if (scanner.hasNext()) {
			result.maintenance = scanner.nextInt();
		}
		return result;

	}
}