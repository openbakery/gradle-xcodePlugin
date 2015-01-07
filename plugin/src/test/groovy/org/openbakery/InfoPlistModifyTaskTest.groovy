package org.openbakery

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 25.07.14.
 */
class InfoPlistModifyTaskTest {


	Project project
	InfoPlistModifyTask task
	File infoPlist

	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	def setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile

		project.apply plugin: org.openbakery.XcodePlugin

		task = project.tasks.findByName('infoplist-modify')
		task.setProperty("commandRunner", commandRunnerMock)


		def archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Example.xcarchive/Products/Applications/Example.app")

		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Info.plist")

		FileUtils.write(infoPlist, "dummy")

	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	@Test
	void testModifyBundleIdentifier() {
		project.infoplist.bundleIdentifier = 'org.openbakery.Example'

		List<String> commandList
		commandList?.clear()
		commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Set :CFBundleIdentifier " + project.infoplist.bundleIdentifier]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}

	@Test
	void testModifyCommand_single() {
		project.infoplist.commands = "Add CFBundleURLTypes:0:CFBundleURLName string"

		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", project.infoplist.commands[0]]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}

	@Test
	void testModifyCommand_multiple() {
		project.infoplist.commands = ["Add CFBundleURLTypes:0:CFBundleURLName string", "Add CFBundleURLTypes:0:CFBundleURLSchemes array" ]


		def commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", project.infoplist.commands[0]]
		commandRunnerMock.run(commandList).times(1)


		commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", project.infoplist.commands[1]]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}


	@Test
	void testModifyVersion() {
		project.infoplist.version = '1.0.0'

		List<String> commandList
		commandList?.clear()
		commandList = ["/usr/libexec/PlistBuddy", infoPlist.absolutePath, "-c", "Set :CFBundleVersion 1.0.0"]
		commandRunnerMock.run(commandList).times(1)


		mockControl.play {
			task.prepare()
		}

	}
}
