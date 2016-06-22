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
package org.openbakery.crashlytics

import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import org.openbakery.AbstractDistributeTask
import org.openbakery.XcodePlugin
import org.openbakery.output.ConsoleOutputAppender

class CrashlyticsUploadTask extends AbstractDistributeTask {

	CrashlyticsUploadTask() {
		super()
		dependsOn(XcodePlugin.PACKAGE_TASK_NAME)
		this.description = "Upload the IPA to crashlytics for crash reports"
	}

	@TaskAction
	def upload() throws IOException {

		if (project.crashlytics.apiKey == null) {
			throw new IllegalArgumentException("Cannot upload to Crashlytics because API Key is missing")
		}

		if (project.crashlytics.buildSecret == null) {
			throw new IllegalArgumentException("Cannot upload to Crashlytics because Build Secret is missing")
		}

		File submitCommand = new File(project.getProjectDir(), project.crashlytics.submitCommand)
		if (!submitCommand.exists()) {
			throw new IllegalArgumentException("Cannot upload to Crashlytics because Submit command was not found")
		}

		def commandList = [
				submitCommand.absolutePath,
				project.crashlytics.apiKey,
				project.crashlytics.buildSecret,
				'-ipaPath',
				'"' + getIpaBundle().absolutePath + '"'
		]

		if (!project.crashlytics.emails.isEmpty()) {
			commandList.push("-emails")
			commandList.push('"' + project.crashlytics.emails.join(',') + '"')
		}
		if (!project.crashlytics.groupAliases.isEmpty()) {
			commandList.push("-groupAliases")
			commandList.push('"' + project.crashlytics.groupAliases.join(',') + '"')
		}

		if (project.crashlytics.notesPath != null) {

			File notesPath = new File(project.getProjectDir(), project.crashlytics.notesPath)
			if (!notesPath.exists()) {
				throw new IllegalArgumentException("Cannot upload to Crashlytics note file was not found")
			}

			commandList.push("-notesPath")
			commandList.push('"' + notesPath.absolutePath + '"')
		}

		if (project.crashlytics.notifications != null) {
			commandList.push("-notifications")
			commandList.push(project.crashlytics.notifications ? 'YES' : 'NO')
		}

		def environment = ["DEVELOPER_DIR":project.xcodebuild.xcodePath + "/Contents/Developer/"]

		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(CrashlyticsUploadTask.class)
		commandRunner.run(commandList, environment, new ConsoleOutputAppender(output))


	}
}