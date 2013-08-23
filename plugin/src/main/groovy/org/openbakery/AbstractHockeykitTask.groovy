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

/**
 *
 * @author René Pirringer
 *
 */
abstract class AbstractHockeykitTask extends AbstractXcodeTask {

	/**
	 * Method to get the destination directory where the output of the generated files for hockeykit should be stored.
	 *
	 * @return the output directory as absolute path
	 */
	def getOutputDirectory() {
		def infoplist = getAppBundleInfoPlist()
		def bundleIdentifier = getValueFromPlist(infoplist, "CFBundleIdentifier")
		File outputDirectory = new File(project.hockeykit.outputDirectory, bundleIdentifier + "/" + project.hockeykit.versionDirectoryName)
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		return outputDirectory.absolutePath
	}
}
