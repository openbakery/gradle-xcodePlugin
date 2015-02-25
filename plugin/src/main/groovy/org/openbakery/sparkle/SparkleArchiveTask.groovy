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

import org.gradle.api.tasks.TaskAction
import org.gradle.api.DefaultTask
import org.openbakery.XcodePlugin

/**
 *
 * @author Stefan Gugarel
 *
 */
class SparkleArchiveTask extends DefaultTask {

	SparkleArchiveTask() {
		super()
		dependsOn(
						XcodePlugin.PACKAGE_TASK_NAME,
						XcodePlugin.SPARKLE_NOTES_TASK_NAME
		)
		this.description = "Compresses app to ZIP when building Mac Apps with Sparkle"
	}

	@TaskAction
	def archiveApp() {

		if(!project.sparkle.appDirectory.exists()) {
		 	throw new IllegalArgumentException("Invalid app name specified for Sparkle: " + project.sparkle.appName + " Path: " + project.sparkle.appDirectory)
	 	}

		if (!project.sparkle.outputDirectory.exists()) {
			project.sparkle.outputDirectory.mkdirs();
		}

		// with using ditto here symlinks in frameworks are handled correctly
		ant.exec(failonerror: "true",
				executable: 'ditto') {
			arg(value: '-c')
			arg(value: '-k')
			arg(value: '--sequesterRsrc')
			arg(value: '--keepParent')
			arg(value: project.sparkle.appDirectory.path)
			arg(value: project.sparkle.outputDirectory.path + "/" + project.sparkle.appName +  ".zip")
		}
	}
}
