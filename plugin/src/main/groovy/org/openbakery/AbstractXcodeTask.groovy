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

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask

import java.text.SimpleDateFormat

/**
 *
 * @author René Pirringer
 *
 */
abstract class AbstractXcodeTask extends DefaultTask {


	public CommandRunner commandRunner

	public PlistHelper plistHelper

	AbstractXcodeTask() {
		commandRunner = new CommandRunner()

		plistHelper = new PlistHelper(project, commandRunner)
	}


	/**
	 * Copies a file to a new location
	 *
	 * @param source
	 * @param destination
	 */
	def copy(File source, File destination) {
		logger.debug("Copy '{}' -> '{}'", source, destination);

		// use rsync to preserve the file permissions (I want to stay compatible with java 1.6 and there is no option for this)
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

		if (project.xcodebuild.isDeviceBuildOf(Type.iOS)) {
			addPluginsToAppBundle(appBundle, bundles)
			addWatchToAppBundle(appBundle, bundles)
		} else if (project.xcodebuild.type == Type.OSX) {
			addEmbeddedAppToBundle(appBundle, bundles)
		}
		bundles.add(appBundle)
		return bundles;
	}

	def addEmbeddedAppToBundle(File appPath, ArrayList<File> appBundle) {

		appPath.listFiles().each { File file ->
			if (file.isDirectory() && !FileUtils.isSymlink(file)) {
				addEmbeddedAppToBundle(file, appBundle)
				if (file.name.endsWith(".app")) {
					appBundle.add(file)
				}
			}
		}

	}

	private void addPluginsToAppBundle(File appBundle, ArrayList<File> bundles) {
		File plugins
		plugins = new File(appBundle, "PlugIns")
		if (plugins.exists()) {
			for (File pluginBundle : plugins.listFiles()) {
				if (pluginBundle.isDirectory()) {

					if (pluginBundle.name.endsWith(".framework")) {
						// Frameworks have to be signed with this path
						bundles.add(new File(pluginBundle, "/Versions/Current"))
					}	else if (pluginBundle.name.endsWith(".appex")) {

						for (File appexBundle : pluginBundle.listFiles()) {
							if (appexBundle.isDirectory() && appexBundle.name.endsWith(".app")) {
								bundles.add(appexBundle)
							}
						}
						bundles.add(pluginBundle)
					} else if (pluginBundle.name.endsWith(".app")) {
						bundles.add(pluginBundle)
					}
				}
			}
		}
	}

	private void addWatchToAppBundle(File appBundle, ArrayList<File> bundles) {
			File watchDirectory
			watchDirectory = new File(appBundle, "Watch")
			if (watchDirectory.exists()) {
				for (File bundle : watchDirectory.listFiles()) {
					if (bundle.isDirectory()) {
						if (bundle.name.endsWith(".app")) {
							addPluginsToAppBundle(bundle, bundles)
							bundles.add(bundle)
						}
					}
				}
			}
		}
}
