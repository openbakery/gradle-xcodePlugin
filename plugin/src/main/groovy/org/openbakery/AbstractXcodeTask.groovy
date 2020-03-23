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
import org.apache.commons.lang.StringUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Security
import org.openbakery.simulators.SimulatorControl
import org.openbakery.xcode.DestinationResolver
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import org.openbakery.util.PlistHelper

import java.text.SimpleDateFormat

/**
 *
 * @author René Pirringer
 *
 */
abstract class AbstractXcodeTask extends DefaultTask {


	public CommandRunner commandRunner

	public PlistHelper plistHelper
	public Xcode xcode
	protected SimulatorControl simulatorControl
	protected DestinationResolver destinationResolver

	@Internal
	Security security


	AbstractXcodeTask() {
		commandRunner = new CommandRunner()
		plistHelper = new PlistHelper(commandRunner)
		security = new Security(commandRunner)
	}


	/**
	 * Copies a file to a new location
	 *
	 * @param source
	 * @param destination
	 */
	def copy(File source, File destination) {
		logger.debug("Copy '{}' -> '{}'", source, destination);

		if (!source.exists()) {
			logger.info("source does not exist " + source)
			return
		}
		File destinationPath = new File(destination, source.getName())

		ant.exec(failonerror: "true",
								executable: 'ditto') {
			arg(value: source.absolutePath)
			arg(value: destinationPath.absolutePath)
		}

		//commandRunner.run("ditto", source.absolutePath, destinationPath.absolutePath)
	}

	/**
	 * Downloads a file from the given address and stores it in the given directory
	 *
	 * @param toDirectory
	 * @param address
	 * @return
	 */
	def download(File toDirectory, String address) {
		if (StringUtils.isEmpty(address)) {
			throw new IllegalArgumentException("Cannot download, because no address was given")
		}

		if (!toDirectory.exists()) {
			toDirectory.mkdirs()
		}

		try {
			ant.get(src: address, dest: toDirectory.getPath(), verbose:true)
		} catch (Exception ex) {
			logger.error("cannot download file from the given location: {}", address)
			throw ex
		}

		File destinationFile = new File(toDirectory, FilenameUtils.getName(address))
		return destinationFile.absolutePath
	}

	@Internal
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
		return result
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

	List<Bundle> getAppBundles(File appPath) {
		ApplicationBundle applicationBundle = new ApplicationBundle(
			new File(appPath,project.xcodebuild.applicationBundle.name),
			project.xcodebuild.type,
			project.xcodebuild.simulator,
			this.plistHelper
		)
		return applicationBundle.getBundles()
	}

	File getTemporaryDirectory(String path) {
		File tmp = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("tmp")
		return new File(tmp, path)
	}


	@Internal
	Xcode getXcode() {
		if (xcode == null) {
			xcode = new Xcode(commandRunner, getProjectXcodeVersion())
		}
		return xcode
	}

	@Internal
	String getProjectXcodeVersion() {
		return project.xcodebuild.xcodeVersion
	}

	@Internal
	DestinationResolver getDestinationResolver() {
		if (destinationResolver == null) {
			destinationResolver = new DestinationResolver(getSimulatorControl())
		}
		return destinationResolver
	}

	@Internal
	SimulatorControl getSimulatorControl() {
		if (simulatorControl == null) {
			simulatorControl = new SimulatorControl(this.commandRunner, getXcode())
		}
		return simulatorControl
	}

	@Internal
	String getSigningIdentity() {
		if (project.xcodebuild.signing.identity != null) {
			return project.xcodebuild.signing.identity
		}
		if (project.xcodebuild.signing.getKeychainPathInternal().exists()) {
			return security.getIdentity(project.xcodebuild.signing.getKeychainPathInternal())
		}
		return null
	}
}
