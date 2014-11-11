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
package org.openbakery.hockeyapp

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractArchiveTask
import org.openbakery.AbstractXcodeTask

class HockeyAppPrepareTask extends AbstractArchiveTask {

	HockeyAppPrepareTask() {
		super()
		dependsOn("codesign")
		this.description = "Prepare the app bundle and dSYM to publish with using hockeyapp"
	}


	@TaskAction
	def archive() {

		copyIpaToDirectory(project.hockeyapp.outputDirectory)
		copyDsymToDirectory(project.hockeyapp.outputDirectory)


		logger.debug("project.hockeyapp.outputDirectory {}", project.hockeyapp.outputDirectory)

		if (project.xcodebuild.bundleNameSuffix != null) {
			baseZipName = project.xcodebuild.productName + project.xcodebuild.bundleNameSuffix
		} else {
			baseZipName = project.xcodebuild.productName
		}

		logger.debug("baseZipName {}",  baseZipName)

		def ant = new AntBuilder()
		ant.zip(destfile: project.hockeyapp.outputDirectory.path + "/" + baseZipName + "." + project.xcodebuild.productType + ".dSYM.zip",
						basedir: project.xcodebuild.getOutputPath().absolutePath,
						includes: "*dSYM*/**")


	}
}
