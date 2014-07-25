package org.openbakery

import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 25.07.14.
 */
class InfoPlistModifyTaskTest {


	Project project
	InfoPlistModifyTask task

	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		project.xcodebuild.infoPlist = "Info.plist"


		task = project.tasks.findByName('infoplist-modify')
		task.setProperty("commandRunner", commandRunnerMock)



	}


	@Test
	void testModifyBundleIdentifier() {
		project.infoplist.bundleIdentifier = 'org.openbakery.Example'

		List<String> commandList
		commandList?.clear()
		commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", "Set :CFBundleIdentifier " + project.infoplist.bundleIdentifier]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}

	@Test
	void testModifyCommand_single() {
		project.infoplist.commands = "Add CFBundleURLTypes:0:CFBundleURLName string"

		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", project.infoplist.commands[0]]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}

	@Test
	void testModifyCommand_multiple() {
		project.infoplist.commands = ["Add CFBundleURLTypes:0:CFBundleURLName string", "Add CFBundleURLTypes:0:CFBundleURLSchemes array" ]


		def commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", project.infoplist.commands[0]]
		commandRunnerMock.run(commandList).times(1)


		commandList = ["/usr/libexec/PlistBuddy", "Info.plist", "-c", project.infoplist.commands[1]]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}
}
