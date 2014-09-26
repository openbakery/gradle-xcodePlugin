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
		dependsOn("xcodebuild")
		this.description = "Signs the app bundle that was created by xcodebuild"
	}

	/**
	 * Patches the original PackageApplication script and returns the absolute file name of the patched script.
	 * The patched PackageApplication contains a new keychain parameter where the keychain can be specified that should be used
	 *
	 * @return
	 */
	String preparePackageApplication() {

		def commandListFindPackageApplication = [
						project.xcodebuild.xcrunCommand,
						"-sdk",
						project.xcodebuild.sdk,
						"--find",
						"PackageApplication"
		];
		def packageApplicationFile = commandRunner.runWithResult(commandListFindPackageApplication).trim();

		File destinationFile = new File(project.xcodebuild.signing.signingDestinationRoot.absolutePath, "PackageApplication")

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

		logger.debug("SymRoot: {}", project.xcodebuild.symRoot)
		def buildOutputDirectory = new File(project.xcodebuild.symRoot, project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
		def fileList = buildOutputDirectory.list(
						[accept: {d, f -> f ==~ /.*app/ }] as FilenameFilter
		).toList()
		if (fileList.size() == 0) {
			throw new IllegalStateException("No App Found in directory " + buildOutputDirectory.absolutePath)
		}
		def appName = buildOutputDirectory.absolutePath + "/" + fileList[0]
		def ipaName = appName.substring(0, appName.size()-4) + ".ipa"
		logger.lifecycle("Signing {} to create {}", appName, ipaName)

		String packageApplicationScript = preparePackageApplication()

		def commandList = [
						packageApplicationScript,
						"-v",
						appName,
						"-o",
						ipaName,
						"--keychain",
						project.xcodebuild.signing.keychainPathInternal.absolutePath
		]

		//--keychain /Users/rene/workspace/coconatics/ELO/elo-ios/build/keychain/gradle-1403850484215.keychain

		if (project.xcodebuild.signing.identity != null && project.xcodebuild.signing.mobileProvisionFile != null) {
			commandList.add("--sign");
			commandList.add(project.xcodebuild.signing.identity)
			commandList.add("--embed");
			commandList.add(project.xcodebuild.signing.mobileProvisionFile.absolutePath)
		}




		def codesignAllocateCommand = commandRunner.runWithResult([project.xcodebuild.xcrunCommand, "-find", "codesign_allocate"]).trim();
		def environment = [CODESIGN_ALLOCATE:codesignAllocateCommand]
		commandRunner.run(".", commandList, environment, null)
	}

}
