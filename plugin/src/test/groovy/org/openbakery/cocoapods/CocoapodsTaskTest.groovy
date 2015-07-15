package org.openbakery.cocoapods

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.testng.annotations.AfterMethod
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


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void testInstallPods() {

		commandRunnerMock.run("gem", "install", "-N", "--user-install", "cocoapods").times(1)

		commandRunnerMock.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir").returns("/tmp/gems").times(1)


		def podSetupCommandList = ["/tmp/gems/bin/pod", "setup"];
		commandRunnerMock.run(podSetupCommandList, anything()).times(1)


		def podInstallCommandList = ["/tmp/gems/bin/pod", "install"];
		commandRunnerMock.run(podInstallCommandList, anything()).times(1)


		mockControl.play {
			cocoapodsTask.install()
		}

	}


	@Test
	void testSkipInstall() {
		File podfileLock = new File(project.projectDir , "Podfile.lock")
		FileUtils.writeStringToFile(podfileLock, "Dummy")

		File manifest = new File(project.projectDir , "Pods/Manifest.lock")
		FileUtils.writeStringToFile(manifest, "Dummy")

		commandRunnerMock.run("gem", "install", "-N", "--user-install", "cocoapods").never()

		mockControl.play {
			cocoapodsTask.install()
		}


	}

	@Test
	void testReinstallPods() {
		File podfileLock = new File(project.projectDir , "Podfile.lock")
		FileUtils.writeStringToFile(podfileLock, "Dummy")

		File manifest = new File(project.projectDir , "Pods/Manifest.lock")
		FileUtils.writeStringToFile(manifest, "Foo")

		testInstallPods()

	}

	@Test
	void testRefreshDependencies() {
		File podfileLock = new File(project.projectDir , "Podfile.lock")
		FileUtils.writeStringToFile(podfileLock, "Dummy")

		File manifest = new File(project.projectDir , "Pods/Manifest.lock")
		FileUtils.writeStringToFile(manifest, "Dummy")

		project.getGradle().getStartParameter().setRefreshDependencies(true)

		testInstallPods()

	}

}
