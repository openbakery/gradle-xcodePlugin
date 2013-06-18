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

class XcodeBuildTask extends DefaultTask {


	CommandRunner commandRunner
	ProvisioningProfileIdReader provisioningProfileIdReader

	XcodeBuildTask() {
		super()
		this.description = "Builds the Xcode project"
		commandRunner = new CommandRunner()
		provisioningProfileIdReader = new ProvisioningProfileIdReader()
	}

	@TaskAction
	def xcodebuild() {
		if (project.xcodebuild.scheme == null && project.xcodebuild.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}


		def commandList = [
						"xcodebuild"
		]


		if (project.xcodebuild.scheme) {
			commandList.add("-scheme");
			commandList.add(project.xcodebuild.scheme);

			// workspace makes only sense when using scheme
			if (project.xcodebuild.workspace != null) {
				commandList.add("-workspace")
				commandList.add(project.xcodebuild.workspace)
			}

			if (project.xcodebuild.sdk != null) {
				commandList.add("-sdk")
				commandList.add(project.xcodebuild.sdk)
				if (project.xcodebuild.sdk.equals("iphonesimulator") && project.xcodebuild.arch == null) {
					commandList.add("ONLY_ACTIVE_ARCH=NO")
					commandList.add("-arch")
					commandList.add("i386")
				}
			}

			if (project.xcodebuild.configuration != null) {
				commandList.add("-configuration")
				commandList.add(project.xcodebuild.configuration)
			}


		} else {
			commandList.add("-configuration")
			commandList.add(project.xcodebuild.configuration)
			commandList.add("-sdk")
			commandList.add(project.xcodebuild.sdk)
			commandList.add("-target")
			commandList.add(project.xcodebuild.target)
		}




		if (project.xcodebuild.signing != null && project.xcodebuild.signing.identity != null) {
			commandList.add("CODE_SIGN_IDENTITY=" + project.xcodebuild.signing.identity)
		}

		if (project.xcodebuild.arch != null) {
			commandList.add("-arch")
			commandList.add(project.xcodebuild.arch)
		}



		if (project.xcodebuild.additionalParameters instanceof List) {
			for (String value in project.xcodebuild.additionalParameters) {
				commandList.add(value)
			}
		} else {
			if (project.xcodebuild.additionalParameters != null) {
				commandList.add(project.xcodebuild.additionalParameters)
			}
		}

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

		commandRunner.runCommand(commandList)
		println "Done"
		println "--------------------------------------------------------------------------------"
		println "--------------------------------------------------------------------------------"


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

			commandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
			commandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
			commandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
			commandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)

			commandRunner.runCommand(commandList)
		}
	}


}
