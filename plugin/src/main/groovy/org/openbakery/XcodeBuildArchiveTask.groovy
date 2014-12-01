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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.Task
import org.openbakery.signing.CodesignTask

class XcodeBuildArchiveTask extends AbstractXcodeTask {

	XcodeBuildArchiveTask() {
		super()

		dependsOn('xcodebuild', 'package')
		this.description = "Prepare the app bundle that it can be archive"
	}

	def renameFileTo(String name) {


	}

	@TaskAction
	def archive() {
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		def ipaName =  project.xcodebuild.bundleName + ".ipa"
		def dsynName = project.xcodebuild.bundleName + "." + project.xcodebuild.productType + ".dSYM"
		logger.debug("ipaName: {}", ipaName)
		logger.debug("dsymName: {}", dsynName)



		def zipFileName = new File(project.getBuildDir(), project.xcodebuild.bundleName).absolutePath

		if (project.xcodebuild.bundleNameSuffix != null) {
			logger.debug("Rename App")

			File applicationBundle = project.xcodebuild.getApplicationBundle();
			//File appFile = new File(buildOutputDirectory, appName)
			if (applicationBundle.exists()) {

				applicationBundle.renameTo(new File(applicationBundle.parentFile,  project.xcodebuild.bundleName + project.xcodebuild.bundleNameSuffix + "." + project.xcodebuild.productType))
			}

			File ipaFile = new File(ipaName)
			if (ipaFile.exists()) {
				ipaFile.renameTo(new File(ipaFile.parentFile, project.xcodebuild.bundleName + project.xcodebuild.bundleNameSuffix + ".ipa"))
			}

			File dsymFile = new File(dsynName)
			if (dsymFile.exists()) {
				dsymFile.renameTo(dsymFile.parentFile, project.xcodebuild.bundleName + project.xcodebuild.bundleNameSuffix + "." + project.xcodebuild.productType + ".dSYM")
			}
			zipFileName += project.xcodebuild.bundleNameSuffix

		}

		def ant = new AntBuilder()
		ant.zip(destfile: zipFileName + ".zip",
						basedir: buildOutputDirectory,
						includes: "*.app*/**")

	}
}
