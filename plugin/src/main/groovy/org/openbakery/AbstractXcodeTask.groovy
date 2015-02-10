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

import java.text.SimpleDateFormat

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
		logger.debug("Copy '{}' -> '{}'", source, destination);

		// use cp to preserve the file permissions (I want to stay compatible with java 1.6 and there is no option for this)
		ant.exec(failonerror: "true",
						executable: '/bin/cp') {
			arg(value: '-rp')
			arg(value: source.absolutePath)
			arg(value: destination.absolutePath)
		}

		//FileUtils.copyFile(source, destination)
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
	}


	/**
	 * Reads the value for the given key from the given plist
	 *
	 * @param plist
	 * @param key
	 * @return returns the value for the given key
	 */
	def getValueFromPlist(plist, key) {
		if (plist instanceof File) {
			plist = plist.absolutePath
		}

		try {
			String result = commandRunner.runWithResult([
							"/usr/libexec/PlistBuddy",
							plist,
							"-c",
							"Print :" + key])

			if (result.startsWith("Array {")) {

				ArrayList<String> resultArray = new ArrayList<String>();

				String[] tokens = result.split("\n");

				for (int i = 1; i < tokens.length - 1; i++) {
					resultArray.add(tokens[i].trim());
				}
				return resultArray;
			}
			return result;
		} catch (IllegalStateException ex) {
			return null
		} catch (CommandRunnerException ex) {
			return null
		}
	}


	String setValueForPlist(def plist, String key, String value) {
		setValueForPlist(plist, "Set :" + key + " " + value)
	}


	String setValueForPlist(def plist, String command) {
		File infoPlistFile;
		if (plist instanceof File) {
			infoPlistFile = plist
		} else {
			infoPlistFile = new File(project.projectDir, plist)
		}
		if (!infoPlistFile.exists()) {
			throw new IllegalStateException("Info Plist does not exist: " + infoPlistFile.absolutePath);
		}

		logger.quiet("Set Info Plist Value: {}", command)
		commandRunner.run([
						"/usr/libexec/PlistBuddy",
						infoPlistFile.absolutePath,
						"-c",
						command
		])
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


	def createZip(File fileToZip) {
		File zipFile = new File(fileToZip.parentFile, FilenameUtils.getBaseName(fileToZip.getName()) + ".zip")
		createZip(zipFile, zipFile.parentFile, fileToZip);
	}



	def createZip(File zipFile, File baseDirectory, File... filesToZip) {
		// we want to preserve the permissions, so use the zip command line tool
		// maybe this can be replaced by Apache Commons Compress


		if (!zipFile.parentFile.exists()) {
			zipFile.parentFile.mkdirs()
		}

		logger.debug("create zip file: {}: {} ", zipFile.absolutePath, zipFile.parentFile.exists())
		logger.debug("baseDirectory: {} ", baseDirectory)

		for (File file : filesToZip) {
			logger.debug("create of: {}: {}", file, file.exists() )
		}

		def arguments = []
		arguments << '--symlinks';
		arguments << '--verbose';
		arguments << '--recurse-paths';
		arguments << zipFile.absolutePath;
		for (File file : filesToZip) {
			arguments << file.getName()
		}

		logger.debug("arguments: {}", arguments)

		ant.exec(failonerror: 'true',
						executable: '/usr/bin/zip',
						dir: baseDirectory) {

			for (def argument : arguments) {
				arg(value: argument)
			}
		}
	}

	/**
	 * formats the date as ISO 8601 date
	 * @param date
	 * @return
	 */
	def formatDate(date) {
		TimeZone timeZone = TimeZone.getTimeZone("UTC")
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
		dateFormat.setTimeZone(timeZone)
		return dateFormat.format(date)
	}


	List<File> getAppBundles(File appPath) {
		return getAppBundles(appPath, project.xcodebuild.applicationBundle.name)
	}

	List<File> getAppBundles(File appPath, String applicationBundleName) {

		ArrayList<File> bundles = new ArrayList<File>();

		File appBundle = new File(appPath, applicationBundleName)


		File plugins

		if (project.xcodebuild.sdk.startsWith(XcodePlugin.SDK_IPHONEOS)) {
			plugins = new File(appBundle, "PlugIns")
		} else {
			plugins = new File(appBundle, "Contents/Frameworks")
		}


		if (plugins.exists()) {

			for (File pluginBundle : plugins.listFiles()) {

				if (pluginBundle.isDirectory()) {

					if (pluginBundle.name.endsWith(".framework")) {

						// Framworks have to be signed with this path
						bundles.add(new File(pluginBundle, "/Versions/Current"))
					} else {
						bundles.add(pluginBundle)
					}
				}
			}
		}

		bundles.add(appBundle)

		return bundles;

	}
}
