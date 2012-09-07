package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.configuration.plist.PropertyListConfiguration
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine
import org.gradle.api.Task
import org.gradle.api.Action


class AbstractXcodeTask extends DefaultTask {

	@Override
	Task doFirst(Action<? super Task> action) {
		new File(project.xcodebuild.buildRoot).mkdirs()
		return super.doFirst(action);
	}

	/**
	 * Converts the command list array to a nice readable string
	 *
	 * @param commandList
	 * @return a readable string of the command
	 */
	def commandListToString(List<String> commandList) {
		def result = ""
		commandList.each {
			item -> result += item + " "
		}
		return "'" + result.trim() + "'"
	}

	/**
	 * Copies a file to a new location
	 *
	 * @param source
	 * @param destination
	 */
	def copy(File source, File destination) {
		println "Copy '" + source + "' -> '" + destination + "'"
		FileUtils.copyFile(source, destination)
	}

	/**
	 * Downloads a file from the given address and stores it in the given directory
	 *
	 * @param toDirectory
	 * @param address
	 * @return
	 */
	def download(String toDirectory, String address) {
		File destinationDirectory = new File(toDirectory)
		if (!destinationDirectory.exists()) {
			destinationDirectory.mkdir()
		}
		File destinationFile = new File(destinationDirectory, address.tokenize("/")[-1])
		def file = new FileOutputStream(destinationFile)
		def out = new BufferedOutputStream(file)
		out << new URL(address).openStream()
		out.close()
		return destinationFile.absolutePath
	}


	def runCommand(String directory, List<String> commandList, Map<String, String> environment) {
		println "Run command: " + commandListToString(commandList)
		if (environment != null) {
			println "with additional environment variables: " + environment
		}
		def processBuilder = new ProcessBuilder(commandList)
		processBuilder.redirectErrorStream(true)
		processBuilder.directory(new File(directory))
		if (environment != null) {
			Map<String, String> env = processBuilder.environment()
			env.putAll(environment)
		}
		def process = processBuilder.start()
		process.inputStream.eachLine {
			println it
		}
		process.waitFor()
		if (process.exitValue() > 0) {
			throw new IllegalStateException("Command failed to run: " + commandListToString(commandList))
		}
	}

	def runCommand(String directory, List<String> commandList) {
		runCommand(directory, commandList, null)
	}

	def runCommand(List<String> commandList) {
		runCommand(".", commandList)
	}

	def runCommandWithResult(List<String> commandList) {
		runCommandWithResult(".", commandList)
	}

	def runCommandWithResult(String directory, List<String> commandList) {
		runCommandWithResult(directory, commandList, null)
	}

	def runCommandWithResult(String directory, List<String> commandList, Map<String, String> environment) {
		//print commandListToString(commandList)
		def processBuilder = new ProcessBuilder(commandList)
		processBuilder.redirectErrorStream(true)
		processBuilder.directory(new File(directory))
		if (environment != null) {
			Map<String, String> env = processBuilder.environment()
			env.putAll(environment)
		}
		def process = processBuilder.start()
		def result = ""
		process.inputStream.eachLine {
			result += it
		}
		process.waitFor()
		if (process.exitValue() > 0) {
			throw new IllegalStateException("Command failed to run: " + commandListToString(commandList))
		}
		return result
	}

	/**
	 *
	 * @return the absolute path to the generated app bundle
	 */
	def getAppBundleName() {
		//println project.xcodebuild.symRoot
		def buildOutputDirectory = new File(project.xcodebuild.symRoot + "/" + project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
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
		println "Using Info.plist: " + infoPlist
		return infoPlist
	}

	def getAppBundleInfoPlist() {
		File infoPlistFile = new File(getAppBundleName() + "/Info.plist")
		if (infoPlistFile.exists()) {

			def convertedPlist = new File(project.xcodebuild.buildRoot, FilenameUtils.getName(infoPlistFile.getName()))
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
		def projectFileDirectory = new File(".").list(new SuffixFileFilter(".xcodeproj"))[0]
		def projectFile = new File(projectFileDirectory, "project.pbxproj")

		def buildRoot = new File(project.xcodebuild.buildRoot)
		if (!buildRoot.exists()) {
			buildRoot.mkdirs()
		}

		def projectPlist = project.xcodebuild.buildRoot + "/project.plist"

		// convert ascii plist to xml so that commons configuration can parse it!
		runCommand(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlist])

		XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(projectPlist))
		def rootObjectKey = config.getString("rootObject")
		println rootObjectKey

		List<String> list = config.getList("objects." + rootObjectKey + ".targets")

		for (target in list) {

			def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
			println "buildConfigurationList=" + buildConfigurationList
			def targetName = config.getString("objects." + target + ".name")
			println "targetName: " + targetName


			if (targetName.equals(project.xcodebuild.target)) {
				def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
				for (buildConfigurationsItem in buildConfigurations) {
					def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

					println "  buildName: " + buildName + " equals " + project.xcodebuild.configuration

					if (buildName.equals(project.xcodebuild.configuration)) {
						def productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
						def plistFile = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
						println "  productName: " + productName
						println "  plistFile: " + plistFile
						return plistFile
					}
				}
			}
		}
	}

	def getProvisioningProfileId() {
		File provisionDestinationFile = new File(project.provisioning.destinationRoot)
		println provisionDestinationFile
		if (!provisionDestinationFile.exists()) {
			return
		}

		def fileList = provisionDestinationFile.list(
						[accept: {d, f -> f ==~ /.*mobileprovision/ }] as FilenameFilter
		).toList()

		if (fileList.size() > 0) {
			def mobileprovisionContent = new File(provisionDestinationFile, fileList[0]).text
			def matcher = mobileprovisionContent =~ "<key>UUID</key>\\s*\\n\\s*<string>(.*?)</string>"
			uuid = matcher[0][1]
			return uuid;
		}
		return null;
	}
}