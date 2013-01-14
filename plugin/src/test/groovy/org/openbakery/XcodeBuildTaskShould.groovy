package org.openbakery

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class XcodeBuildTaskShould {

	Project project
	XcodeBuildTask xcodeBuildTask

	GMockController mockControl = new GMockController()
	CommandRunner commandRunnerMock
	List<String> expectedCommandList

	String currentDir = new File('').getAbsolutePath()

	@BeforeMethod
	def setup() {
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		project.apply plugin: org.openbakery.XcodePlugin
		xcodeBuildTask = project.task('xcodeBuild', type: XcodeBuildTask)
		xcodeBuildTask.commandRunner = commandRunnerMock

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
		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(expectedCommandList).times(1)

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
		expectedCommandList.add("DSTROOT=" + currentDir + "/build/dst")
		expectedCommandList.add("OBJROOT=" + currentDir + "/build/obj")
		expectedCommandList.add("SYMROOT=" + currentDir + "/build/sym")
		expectedCommandList.add("SHARED_PRECOMPS_DIR=" + currentDir + "/build/shared")
	}

	@Test
	void run_command_with_expected_scheme_and_expected_dirs() {
		addExpectedScheme()

		project.xcodebuild.dstRoot = currentDir + '/mydst'
		project.xcodebuild.objRoot = currentDir + '/myobj'
		project.xcodebuild.symRoot = currentDir + '/mysym'
		project.xcodebuild.sharedPrecompsDir = currentDir + '/myshared'

		expectedCommandList.add("DSTROOT=" + project.xcodebuild.dstRoot)
		expectedCommandList.add("OBJROOT=" + project.xcodebuild.objRoot)
		expectedCommandList.add("SYMROOT=" + project.xcodebuild.symRoot)
		expectedCommandList.add("SHARED_PRECOMPS_DIR=" + project.xcodebuild.sharedPrecompsDir)

		commandRunnerMock.runCommand(expectedCommandList).times(1)

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

		commandRunnerMock.runCommand(expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_signIdentity() {
		addExpectedScheme()

		def signIdentity = 'mysign'
		project.xcodebuild.signIdentity = signIdentity
		expectedCommandList.add("CODE_SIGN_IDENTITY=" + signIdentity)

		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_arch() {
		addExpectedScheme()

		project.xcodebuild.arch = 'myarch'
		expectedCommandList.add("-arch")
		expectedCommandList.add(project.xcodebuild.arch)

		addExpectedDefaultDirs()

		commandRunnerMock.runCommand(expectedCommandList).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

}
