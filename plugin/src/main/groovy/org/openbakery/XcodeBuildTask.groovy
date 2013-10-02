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
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction
import org.gradle.logging.StyledTextOutput
import org.gradle.logging.StyledTextOutputFactory
import org.gradle.util.ConfigureUtil
import org.openbakery.output.XcodeBuildOutputAppender

class XcodeBuildTask extends AbstractXcodeBuildTask {

	XcodeBuildTask() {
		super()
		dependsOn('keychain-create', 'provisioning-install', 'infoplist-modify')
		this.description = "Builds the Xcode project"
	}

	@TaskAction
	def xcodebuild() {
		if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		def commandList = createCommandList()


		if (project.xcodebuild.additionalParameters instanceof List) {
			for (String value in project.xcodebuild.additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (project.xcodebuild.additionalParameters != null) {
				commandList.add(project.xcodebuild.additionalParameters)
			}
		}

		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(XcodeBuildTask.class);


		commandRunner.runCommand("${project.projectDir.absolutePath}", commandList, new XcodeBuildOutputAppender(output))
		logger.quiet("Done")
	}

}
