package org.openbakery.deploygate

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * User: rene
 * Date: 11/11/14
 */
class DeployGatePrepareTaskTest {

	Project project
	DeployGatePrepareTask deployGatePrepareTask;

	GMockController mockControl

	@BeforeMethod
	void setup() {
		mockControl = new GMockController()

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productName = 'Test'
		project.xcodebuild.infoPlist = 'Info.plist'

		deployGatePrepareTask = project.getTasks().getByPath('deploygate-prepare')

	}


	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	@Test
	void testArchive() {

		FileUtils.writeStringToFile(project.xcodebuild.getIpaBundle(), "dummy")


		mockControl.play {
			deployGatePrepareTask.archive()
		}

		File expectedIpa = new File(project.buildDir, "deploygate/Test.ipa")
		assert expectedIpa.exists()
	}

	@Test
	void testArchiveWithSuffix() {

		project.xcodebuild.bundleNameSuffix = '-SUFFIX'

		FileUtils.writeStringToFile(project.xcodebuild.getIpaBundle(), "dummy")


		mockControl.play {
			deployGatePrepareTask.archive()
		}

		File expectedIpa = new File(project.buildDir, "deploygate/Test-SUFFIX.ipa")
		assert expectedIpa.exists()
	}
}
