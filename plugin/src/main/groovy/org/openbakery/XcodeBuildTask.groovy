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

import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.output.XcodeBuildOutputAppender
import org.openbakery.tools.Xcodebuild

class XcodeBuildTask extends AbstractXcodeBuildTask {

	XcodeBuildTask() {
		super()

		dependsOn(
						XcodePlugin.XCODE_CONFIG_TASK_NAME,
						XcodePlugin.INFOPLIST_MODIFY_TASK_NAME,
		)
		this.description = "Builds the Xcode project"
	}

	@TaskAction
	def build() {
		if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		if (!project.getBuildDir().exists()) {
			project.getBuildDir().mkdirs()
		}

		File outputFile = new File(project.getBuildDir(), "xcodebuild-output.txt")
		commandRunner.setOutputFile(outputFile)

		Xcodebuild xcodebuild = new Xcodebuild(commandRunner, xcode, project.xcodebuild)
		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(XcodeBuildTask.class, LogLevel.LIFECYCLE);
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(XcodeBuildTask.class).start("XcodeBuildTask", "XcodeBuildTask");

		xcodebuild.execute(project.projectDir.absolutePath, new XcodeBuildOutputAppender(progressLogger, output), project.xcodebuild.environment)
		logger.lifecycle("Done")
	}

}
