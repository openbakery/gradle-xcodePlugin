package org.openbakery

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 24.07.14.
 */
class XcodeBuildPluginExtensionTest {


	Project project
	XcodeBuildPluginExtension extension;
	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		extension = new XcodeBuildPluginExtension(project)
	}


	void mockFindSimctl() {
		List<String> commandList
		commandList?.clear()
		commandList = ["xcrun", "-sdk", "iphoneos", "-find", "simctl"]
		commandRunnerMock.runWithResult(commandList).returns("/Applications/Xcode.app/Contents/Developer/usr/bin/simctl").times(1)
	}




	@Test
	void testCreateDeviceList_parseDevices() {

		mockFindSimctl()

		List<String> commandList
		commandList?.clear()
		commandList = ["/Applications/Xcode.app/Contents/Developer/usr/bin/simctl", "list"]

		String simctlOutput = FileUtils.readFileToString(new File("src/test/Resource/simctl-output.txt"))

		commandRunnerMock.runWithResult(commandList).returns(simctlOutput).times(1)

		mockControl.play {
			extension.createDeviceList(commandRunnerMock)
		}

		assert extension.availableDevices.size() == 14 : "expected 14 elements in the availableDevices list"

	}

}
