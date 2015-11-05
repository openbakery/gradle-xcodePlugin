package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.Type
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.XcodePlugin
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * User: rene
 * Date: 25/11/14
 */
class XcodeConfigTaskTest {

	XcodeConfigTask xcodeConfigTask
	Project project
	GMockController mockControl
	CommandRunner commandRunnerMock



	@Before
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)


		File projectDir = new File("../example/iOS/Example")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeConfigTask = project.getTasks().getByName(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		xcodeConfigTask.setProperty("commandRunner", commandRunnerMock)

		project.xcodebuild.target = "Example"

	}

	@After
	void cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}


	void mockFindSimctl() {
		def commandList = ["xcrun", "-sdk", "iphoneos", "-find", "simctl"]
		commandRunnerMock.runWithResult(commandList).returns("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl").times(1)
	}

	void mockSimctlList() {
		mockSimctlList("src/test/Resource/simctl-list.txt")
	}

	void mockSimctlList(String filePath) {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/usr/bin/simctl", "list"]

		String simctlOutput = FileUtils.readFileToString(new File(filePath))

		commandRunnerMock.runWithResult(commandList).returns(simctlOutput).times(1)
	}

	void mockXcodePath() {
		//def commandList = ["xcode-select", "-p"]
		//commandRunnerMock.runWithResult(commandList).returns("build").times(1)

		project.xcodebuild.xcodePath = "build";
		File simulatorDirectory = new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad");
		simulatorDirectory.mkdirs()
	}



	void mockXcodeVersion() {
		def commandList = [
						"xcodebuild",
						"-version",
		]
		commandRunnerMock.runWithResult(commandList).returns("Xcode 6.0\nBuild version 6A313").times(1)
	}

	void mockXcode5Version() {
		def commandList = [
						"build/Contents/Developer/usr/bin/xcodebuild",
						"-version",
		]
		commandRunnerMock.runWithResult(commandList).returns("Xcode 5.0\nBuild version 5A123").times(1)
	}

	void mockDisplayName() {
		def commandList = [
											"/usr/libexec/PlistBuddy",
											new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
											"-c",
											"Print :displayName"
							]
		commandRunnerMock.runWithResult(commandList).returns("iPad").times(1)


	}

	void mockNewerEquivalentDevice(String result) {
		def commandList = [
						"/usr/libexec/PlistBuddy",
						new File("build/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/PrivateFrameworks/SimulatorHost.framework/Versions/A/Resources/Devices/iPad/Info.plist").getAbsolutePath(),
						"-c",
						"Print :newerEquivalentDevice"
		]

		if (result == null) {
			commandRunnerMock.runWithResult(commandList).raises(new CommandRunnerException(""));
		} else {
			commandRunnerMock.runWithResult(commandList).returns(result)
		}

	}






	@Test
	void testNonExistingTarget () {

		project.xcodebuild.target = "test"

		mockControl.play {
			try {
				xcodeConfigTask.configuration()
				fail("Expected IllegalArgumentException was not thrown")
			} catch (IllegalArgumentException ex) {
				assert ex.getMessage().equals("Target 'test' not found in project")
			}
		}

	}
}
