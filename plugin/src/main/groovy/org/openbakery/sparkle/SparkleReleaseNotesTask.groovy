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
package org.openbakery.sparkle

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
	
import org.pegdown.PegDownProcessor


/**
 *
 * @author Stefan Gugarel
 *
 */
class SparkleReleaseNotesTask extends DefaultTask {

	SparkleReleaseNotesTask() {
		super()
		this.description = "Creates release notes when building Mac Apps with Sparkle"
	}
	
	@TaskAction
	def createReleaseNotes() {

		File changeLogFile = new File("Changelog.md")

		if (changeLogFile.exists()) {

			if (!project.sparkle.outputDirectory.exists()) {
				project.sparkle.outputDirectory.mkdirs();
			}

			String changelogString = FileUtils.readFileToString(changeLogFile)

			PegDownProcessor pegDownProcessor = new PegDownProcessor();
			String changelogHTML = pegDownProcessor.markdownToHtml(changelogString)

			new File(project.sparkle.outputDirectory.path + "/releasenotes.html").write(changelogHTML, "UTF-8");

		} else {
			println "No changelog found!"
		}
	}
}