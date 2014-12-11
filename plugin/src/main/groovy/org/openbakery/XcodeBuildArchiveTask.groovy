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

		String zipFileName = project.xcodebuild.bundleName
		if (project.xcodebuild.bundleNameSuffix != null) {
			zipFileName += project.xcodebuild.bundleNameSuffix
		}
		zipFileName += ".zip"


		def zipFile = new File(project.getBuildDir(), zipFileName)

		File baseDirectory = project.xcodebuild.applicationBundle.parentFile
		if (project.xcodebuild.sdk.startsWith("iphoneos")) {

			createZip(zipFile, baseDirectory, project.xcodebuild.applicationBundle, project.xcodebuild.ipaBundle, project.xcodebuild.getDSymBundle())
		} else {
			createZip(zipFile, baseDirectory, project.xcodebuild.applicationBundle)
		}

	}
}
