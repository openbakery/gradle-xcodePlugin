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


import org.apache.commons.io.FilenameUtils
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.openbakery.configuration.XcodeConfig
import org.openbakery.internal.XcodeBuildSpec

import java.text.SimpleDateFormat

/**
 *
 * @author René Pirringer
 *
 */
abstract class AbstractXcodeTask extends DefaultTask {

	XcodeBuildSpec buildSpec
	CommandRunner commandRunner
	PlistHelper plistHelper
	XcodeConfig config

	AbstractXcodeTask() {
		this.commandRunner = new CommandRunner()
		this.buildSpec = new XcodeBuildSpec(project, project.xcodebuild.buildSpec)
		this.plistHelper = new PlistHelper(project, commandRunner)
		this.config = new XcodeConfig(project, this.buildSpec)
	}


	void configureTask() {
		this.config.configuration();
	}


	abstract void executeTask();


	@TaskAction
	void run() {
		configureTask();
		executeTask();
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
						executable: 'rsync') {
			arg(value: '-avz')
			arg(value: source.absolutePath)
			arg(value: destination.absolutePath)
		}
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

	def createZip(File zipFile, File fileToZip) {
		createZip(zipFile, fileToZip.parentFile, fileToZip)
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

		if (project.xcodebuild.isSdk(XcodePlugin.SDK_IPHONEOS)) {
			plugins = new File(appBundle, "PlugIns")
		} else {
			plugins = new File(appBundle, "Contents/Frameworks")
		}


		if (plugins.exists()) {

			for (File pluginBundle : plugins.listFiles()) {

				if (pluginBundle.isDirectory()) {

					if (pluginBundle.name.endsWith(".framework")) {

						// Frameworks have to be signed with this path
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
