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

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class XcodeBuildTaskTest {

	Project project
	XcodeBuildTask xcodeBuildTask

	GMockController mockControl = new GMockController()
	CommandRunner commandRunnerMock
	List<String> expectedCommandList

	String currentDir = new File('').getAbsolutePath()
<<<<<<< HEAD
	String projectDir
=======
    String projectDir
>>>>>>> xcode5

	@BeforeMethod
	def setup() {
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
<<<<<<< HEAD
		projectDir = project.projectDir.absolutePath
=======
        projectDir = project.projectDir.absolutePath
>>>>>>> xcode5
		project.apply plugin: org.openbakery.XcodePlugin

		xcodeBuildTask = project.getTasks().getByPath('xcodebuild')
		xcodeBuildTask.setProperty("commandRunner", commandRunnerMock)

		expectedCommandList?.clear()
		expectedCommandList = ["xcodebuild"]
	}

	@Test(expectedExceptions = [IllegalArgumentException.class])
	public void throw_IllegalArgumentException_when_no_scheme_or_target_given() {
		xcodeBuildTask.xcodebuild()
	}


	@Test
	void run_command_with_expected_scheme_and_expected_default_dirs() {
		addExpectedScheme()

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	def void addExpectedScheme() {
		project.xcodebuild.scheme = 'myscheme'
		expectedCommandList.add("-scheme")
		expectedCommandList.add(project.xcodebuild.scheme)
	}

	def void addExpectedDefaultDirs() {
		expectedCommandList.add("DSTROOT=" + currentDir + "${File.separator}build${File.separator}dst")
		expectedCommandList.add("OBJROOT=" + currentDir + "${File.separator}build${File.separator}obj")
		expectedCommandList.add("SYMROOT=" + currentDir + "${File.separator}build${File.separator}sym")
		expectedCommandList.add("SHARED_PRECOMPS_DIR=" + currentDir + "${File.separator}build${File.separator}shared")
	}

	@Test
	void run_command_with_expected_scheme_and_expected_dirs() {
		addExpectedScheme()

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		project.xcodebuild.dstRoot = new File(currentDir + "${File.separator}mydst")
		project.xcodebuild.objRoot = new File(currentDir + "${File.separator}myobj")
		project.xcodebuild.symRoot = new File(currentDir + "${File.separator}mysym")
		project.xcodebuild.sharedPrecompsDir = new File(currentDir + "${File.separator}myshared")

		expectedCommandList.add("DSTROOT=" + project.xcodebuild.dstRoot.absolutePath)
		expectedCommandList.add("OBJROOT=" + project.xcodebuild.objRoot.absolutePath)
		expectedCommandList.add("SYMROOT=" + project.xcodebuild.symRoot.absolutePath)
		expectedCommandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir.absolutePath)

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	void run_command_with_expected_target_and_expected_defaults() {
		// currently order is important
		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")
		expectedCommandList.add("-sdk")
		expectedCommandList.add("iphonesimulator")

		def target = 'mytarget'
		project.xcodebuild.target = target
		expectedCommandList.add("-target")
		expectedCommandList.add(target)

		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_signIdentity() {
		addExpectedScheme()

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		def signIdentity = 'mysign'
		project.xcodebuild.signing.identity = signIdentity
		expectedCommandList.add("CODE_SIGN_IDENTITY=" + signIdentity)

		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_arch() {
		addExpectedScheme()

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		project.xcodebuild.arch = 'myarch'

		expectedCommandList.add("-arch")
		expectedCommandList.add(project.xcodebuild.arch)


		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_workspace() {
		addExpectedScheme()

		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-workspace")
		expectedCommandList.add("myworkspace")

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}


	@Test
	void run_command_with_workspace_but_without_scheme() {
		// currently order is important
		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")
		expectedCommandList.add("-sdk")
		expectedCommandList.add("iphonesimulator")

		def target = 'mytarget'
		project.xcodebuild.target = target
		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-target")
		expectedCommandList.add(target)

		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}



	@Test
	void run_command_scheme_and_simulatorbuild() {
		addExpectedScheme()


		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-workspace")
		expectedCommandList.add("myworkspace")

		expectedCommandList.add("-sdk")
		expectedCommandList.add("iphonesimulator")


		expectedCommandList.add("ONLY_ACTIVE_ARCH=NO")
		expectedCommandList.add("-arch");
		expectedCommandList.add("i386")

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	void run_command_scheme_and_simulatorbuild_and_arch() {
		addExpectedScheme()


		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-workspace")
		expectedCommandList.add("myworkspace")

		expectedCommandList.add("-sdk")
		expectedCommandList.add("iphonesimulator")

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		project.xcodebuild.arch = 'i386'

		expectedCommandList.add("-arch");
		expectedCommandList.add(project.xcodebuild.arch)


		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(projectDir, expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}


}
