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

class XcodeBuildArchiveTask extends AbstractXcodeTask {

	XcodeBuildArchiveTask() {
		super()
		dependsOn('codesign')
		this.description = "Prepare the app bundle that it can be archive"
	}


	@TaskAction
	def archive() {
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		def appName = getAppBundleName()
		def baseName =  appName.substring(0, appName.size()-4)
		def ipaName =  baseName + ".ipa"
		def dsynName = baseName + ".app.dSYM"
		println(baseName)
		println(ipaName)
		println(dsynName)

		def zipFileName = baseName

		if (project.xcodebuild.bundleNameSuffix != null) {
			println "Rename App"

			File appFile = new File(appName)
			if (appFile.exists()) {
				appFile.renameTo(baseName + project.xcodebuild.bundleNameSuffix + ".app")
			}

			File ipaFile = new File(ipaName)
			if (ipaFile.exists()) {
				ipaFile.renameTo(baseName + project.xcodebuild.bundleNameSuffix + ".ipa")
			}

			File dsymFile = new File(dsynName)
			if (dsymFile.exists()) {
				dsymFile.renameTo(baseName + project.xcodebuild.bundleNameSuffix + ".app.dSYM")
			}
			zipFileName += project.xcodebuild.bundleNameSuffix

		}

		def ant = new AntBuilder()
		ant.zip(destfile: zipFileName + ".zip",
						basedir: buildOutputDirectory,
						includes: "*.app*/**")

	}
}
