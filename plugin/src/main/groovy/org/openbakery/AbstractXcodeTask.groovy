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
import org.gradle.api.DefaultTask

/**
 *
 * @author René Pirringer
 *
 */
abstract class AbstractXcodeTask extends DefaultTask {


	protected CommandRunner commandRunner

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
		logger.lifecycle("Copy '{}' -> '{}'", source, destination);
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

		ant.get(src: address, dest: toDirectory.getPath(), verbose:true)

		File destinationFile = new File(toDirectory, FilenameUtils.getName(address))
		return destinationFile.absolutePath

		return destinationFile.absolutePath
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
			return commandRunner.runWithResult([
							"/usr/libexec/PlistBuddy",
							plist,
							"-c",
							"Print :" + key])
		} catch (IllegalStateException ex) {
			return null
		}
	}



	def getAppBundleInfoPlist() {
		def convertedPlist = new File(project.buildDir, "Info.plist")
		if (convertedPlist.exists()) {
			return convertedPlist.absolutePath
		}

		File infoPlistFile = new File(project.xcodebuild.applicationBundle, "/Info.plist")
		if (infoPlistFile.exists()) {

			//plutil -convert xml1 "$BINARY_INFO_PLIST" -o "${INFO_PLIST}.plist"

			def convertCommand = [
							"plutil",
							"-convert",
							"xml1",
							infoPlistFile.absolutePath,
							"-o",
							convertedPlist.absolutePath
			]

			commandRunner.run(convertCommand)

			return convertedPlist.absolutePath
		}
		return null
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
