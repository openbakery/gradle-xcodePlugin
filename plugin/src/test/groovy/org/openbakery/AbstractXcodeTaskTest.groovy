package org.openbakery

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 01.12.14.
 */
class AbstractXcodeTaskTest {

	Project project
	AbstractXcodeTask xcodeTask;

	GMockController mockControl
	CommandRunner commandRunnerMock


	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		xcodeTask = project.getTasks().getByPath(XcodePlugin.XCODE_CONFIG_TASK_NAME)

		xcodeTask.setProperty("commandRunner", commandRunnerMock)

	}


	@Test
	void getInfoListValue() {
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIdentifier"]
		commandRunnerMock.runWithResult(commandList).returns("com.example.Example")

		String result;
		mockControl.play {
			result = xcodeTask.getValueFromPlist("Info.plist", "CFBundleIdentifier")
		}

		assert result.equals("com.example.Example");
	}



	@Test
	void getArrayFromInfoListValue() {
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIdentifier"]
		commandRunnerMock.runWithResult(commandList).returns("Array {\n" +
						"    AppIcon29x29\n" +
						"    AppIcon40x40\n" +
						"    AppIcon57x57\n" +
						"    AppIcon60x60\n" +
						"}")

		def result;
		mockControl.play {
			result = xcodeTask.getValueFromPlist("Info.plist", "CFBundleIdentifier")
		}


		assert result instanceof List
		assert result.size() == 4
		assert result.get(0).equals("AppIcon29x29")
	}


	@Test
	void getInfoPlistValue_Missing() {
		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Print :CFBundleIconFiles"]
		commandRunnerMock.runWithResult(commandList).raises(new CommandRunnerException())

		def result;
		mockControl.play {
			result = xcodeTask.getValueFromPlist("Info.plist", "CFBundleIconFiles")
		}


		assert result == null

	}
}
