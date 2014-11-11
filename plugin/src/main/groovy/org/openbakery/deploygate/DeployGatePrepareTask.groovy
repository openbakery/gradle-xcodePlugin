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
import org.openbakery.AbstractArchiveTask
import org.openbakery.AbstractXcodeTask

class DeployGatePrepareTask extends AbstractArchiveTask {

	DeployGatePrepareTask() {
		super()
		dependsOn("codesign")
		this.description = "Prepare the app bundle to publish with using deploygate"
	}

	@TaskAction
	def archive() {

		copyIpaToDirectory(project.deploygate.outputDirectory);

	}
}
