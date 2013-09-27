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
import org.gradle.util.ConfigureUtil

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

<<<<<<< HEAD
		def uuid = provisioningProfileIdReader.readProvisioningProfileIdFromDestinationRoot(project.xcodebuild.signing.mobileProvisionDestinationRoot)
		if (uuid != null) {
			commandList.add("PROVISIONING_PROFILE=" + uuid);
		}
		commandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
		commandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
		commandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
		commandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)



		if (project.xcodebuild.signing.keychainPathInternal.exists()) {
			commandList.add('OTHER_CODE_SIGN_FLAGS=--keychain ' + project.xcodebuild.signing.keychainPathInternal.path);
		}

=======
>>>>>>> xcode5
		commandRunner.runCommand("${project.projectDir.absolutePath}", commandList)
		println "Done"
		println "--------------------------------------------------------------------------------"
		println "--------------------------------------------------------------------------------"

<<<<<<< HEAD

		if (project.xcodebuild.unitTestTarget != null &&
						project.xcodebuild.scheme == null &&
						project.xcodebuild.configuration != null &&
						project.xcodebuild.sdk.startsWith("iphonesimulator")
		) {

			println "Run unit test target: " + project.xcodebuild.unitTestTarget
			println "--------------------------------------------------------------------------------"
			println "--------------------------------------------------------------------------------"
			commandList = [
							"xcodebuild",
							"-configuration",
							project.xcodebuild.configuration,
							"-sdk",
							project.xcodebuild.sdk,
							"-target",
							project.xcodebuild.unitTestTarget,
							"TEST_AFTER_BUILD=YES",
							"TEST_HOST="
			]

			if (project.xcodebuild.arch != null) {
				commandList.add("-arch")
				commandList.add(project.xcodebuild.arch)
			}

			commandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
			commandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
			commandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
			commandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)

			commandRunner.runCommand("${project.projectDir.absolutePath}", commandList)
		}
=======
>>>>>>> xcode5
	}

}
