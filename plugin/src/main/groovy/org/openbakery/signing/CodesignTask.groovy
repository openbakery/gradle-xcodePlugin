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
package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.apache.tools.ant.util.ResourceUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask

/**
 *
 * @author René Pirringer
 *
 */
class CodesignTask extends AbstractXcodeTask {

	CodesignTask() {
		super()
		dependsOn('keychain-create', 'provisioning-install', 'infoplist-modify')
		this.description = "Signs the app bundle that was created by xcodebuild"
	}

	/**
	 * Patches the original PackageApplication script and returns the absolute file name of the patched script.
	 * The patched PackageApplication contains a new keychain parameter where the keychain can be specified that should be used
	 *
	 * @return
	 */
	String preparePackageApplication() {

		File destinationFile = new File(project.xcodebuild.signing.signingDestinationRoot.absolutePath, "PackageApplication")
		if (destinationFile.exists()) {
			return destinationFile.absolutePath
		}

		def commandListFindPackageApplication = [
						project.xcodebuild.xcrunCommand,
						"-sdk",
						project.xcodebuild.sdk,
						"--find",
						"PackageApplication"
		];
		def packageApplicationFile = commandRunner.runWithResult(commandListFindPackageApplication).trim();



		FileUtils.copyFile(new File(packageApplicationFile), destinationFile);


		String fileContents = FileUtils.readFileToString(destinationFile)

		StringBuilder modifiedContent = new StringBuilder()

		for (String line in fileContents.split("\n")) {

			if (modifiedContent.length() > 0) {
				modifiedContent.append("\n")
			}
			modifiedContent.append(line)

			if (line.equals("             \"output|o=s\",")) {
				modifiedContent.append("\n")
				modifiedContent.append("             \"keychain|k=s\",")
			}

			if (line.equals("    push(@codesign_args, \$destApp);")) {
				modifiedContent.append("\n")
				modifiedContent.append("    if ( \$opt{keychain} ) {\n")
				modifiedContent.append("      push(@codesign_args, '--keychain');\n")
				modifiedContent.append("      push(@codesign_args, \$opt{keychain});\n")
				modifiedContent.append("    }")
			}

		}

		FileUtils.writeStringToFile(destinationFile, modifiedContent.toString());

		destinationFile.setExecutable(true)
		return destinationFile.absolutePath

	}

	@TaskAction
	def codesign() {
		if (!project.xcodebuild.sdk.startsWith("iphoneos")) {
			logger.lifecycle("not a device build, so no codesign needed")
			return
		}
		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration")
		}

		File applicationBundle = project.xcodebuild.getApplicationBundle();
		File ipaBundle = project.xcodebuild.getIpaBundle();

		logger.lifecycle("Signing {} to create {}", applicationBundle, ipaBundle)

		String packageApplicationScript = preparePackageApplication()

		def commandList = [
						packageApplicationScript,
						"-v",
						applicationBundle.absolutePath,
						"-o",
						ipaBundle.absolutePath,
						"--keychain",
						project.xcodebuild.signing.keychainPathInternal.absolutePath
		]


		if (project.xcodebuild.signing.identity != null && project.xcodebuild.signing.mobileProvisionFile != null) {
			commandList.add("--sign");
			commandList.add(project.xcodebuild.signing.identity)
			commandList.add("--embed");
			commandList.add(project.xcodebuild.signing.mobileProvisionFile.absolutePath)
		}

		if (project.xcodebuild.signing.plugin) {
			commandList.add("--plugin");
			commandList.add(project.xcodebuild.signing.plugin)
		}


		def codesignAllocateCommand = commandRunner.runWithResult([project.xcodebuild.xcrunCommand, "-find", "codesign_allocate"]).trim();
		def environment = [CODESIGN_ALLOCATE:codesignAllocateCommand]
		commandRunner.run(".", commandList, environment, null)
	}

}
