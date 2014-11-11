package org.openbakery.cocoapods

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.testng.annotations.AfterTest
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static org.hamcrest.Matchers.anything

/**
 * Created by rene on 11.11.14.
 */
class CocoapodsTaskTest {


	Project project
	CocoapodsTask cocoapodsTask;

	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin:org.openbakery.XcodePlugin

		cocoapodsTask = project.getTasks().getByPath('cocoapods')

		cocoapodsTask.setProperty("commandRunner", commandRunnerMock)
	}


	@AfterTest
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void testInstallPods() {

		def gemInstallCommandList = ["gem", "install", "-N", "--user-install", "cocoapods"];
		commandRunnerMock.run(gemInstallCommandList).times(1)

		def installDirectoryCommand = ["ruby", "-rubygems", "-e", "puts Gem.user_dir"];
		commandRunnerMock.runWithResult(installDirectoryCommand).returns("/tmp/gems").times(1)


		def podInstallCommandList = ["/tmp/gems/bin/pod", "install"];
		commandRunnerMock.run(podInstallCommandList, anything()).times(1)


		mockControl.play {
			cocoapodsTask.install()
		}

	}


	@Test
	void testSkipInstall() {
		File podsDirectory = new File(project.projectDir , "Pods")
		podsDirectory.mkdirs()

		def gemInstallCommandList = ["gem", "install", "-N", "--user-install", "cocoapods"];
		commandRunnerMock.run(gemInstallCommandList).never()

		mockControl.play {
			cocoapodsTask.install()
		}


	}

	@Test
	void testReinstallPods() {
		File podsDirectory = new File(project.projectDir , "Pods")
		podsDirectory.mkdirs()

		project.getGradle().getStartParameter().setRefreshDependencies(true)

		testInstallPods()

	}

}
