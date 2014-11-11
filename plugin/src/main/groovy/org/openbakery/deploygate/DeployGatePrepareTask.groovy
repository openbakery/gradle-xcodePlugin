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
package org.openbakery.deploygate

import org.gradle.api.tasks.TaskAction
import org.apache.commons.io.FilenameUtils
import org.apache.ivy.util.FileUtil
import org.apache.commons.io.FileUtils
import org.openbakery.AbstractXcodeTask

class DeployGatePrepareTask extends AbstractXcodeTask {

	DeployGatePrepareTask() {
		super()
		dependsOn("codesign")
		this.description = "Prepare the app bundle to publish with using deploygate"
	}

	@TaskAction
	def archive() {
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)

		def ipaName = project.xcodebuild.productName + ".ipa"

		if (!project.deploygate.outputDirectory.exists()) {
			project.deploygate.outputDirectory.mkdirs()
		}

		if (project.xcodebuild.bundleNameSuffix != null) {
			logger.debug("Rename App")

			File ipaFile = new File(ipaName)
			if (ipaFile.exists()) {
				ipaName = project.xcodebuild.productName + project.xcodebuild.bundleNameSuffix + ".ipa";
				ipaFile.renameTo(ipaName)
			}
		}

		logger.debug("project.deploygate.outputDirectory {}", project.deploygate.outputDirectory)
		FileUtils.copyFileToDirectory(new File(ipaName), project.deploygate.outputDirectory)
	}
}
