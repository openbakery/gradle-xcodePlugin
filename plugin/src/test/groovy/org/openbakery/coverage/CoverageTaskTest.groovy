package org.openbakery.coverage

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 30.01.15.
 */
class CoverageTaskTest {


	Project project
	CoverageTask coverageTask

	GMockController mockControl
	CommandRunner commandRunnerMock

	@BeforeMethod
	void setup() {
		mockControl = new GMockController()
		commandRunnerMock = mockControl.mock(CommandRunner)

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		coverageTask = project.getTasks().getByPath('coverage')

		coverageTask.setProperty("commandRunner", commandRunnerMock)
	}

	@AfterMethod
	void cleanUp() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	void mockInstall() {
		org.gradle.api.AntBuilder antMock = mockControl.mock(org.gradle.api.AntBuilder)
		project.ant = antMock
		antMock.get(src: "https://github.com/gcovr/gcovr/archive/3.2.zip", dest: project.coverage.outputDirectory, verbose:true)
		antMock.unzip(src: new File(project.coverage.outputDirectory, "3.2.zip"), dest: project.coverage.outputDirectory)
	}

	@Test
	void testCoverageXML() {
		mockInstall()

		project.coverage.outputFormat = 'xml'

		File outputFile = new File(project.coverage.outputDirectory, "coverage.xml")

		def gcovrCommand = new File(project.coverage.outputDirectory, 'gcovr-3.2/scripts/gcovr').absolutePath


		def gemInstallCommandList = [
						'python',
						gcovrCommand,
						'-r',
						'.',
						"--xml",
						"-o",
						outputFile.absolutePath];
		commandRunnerMock.run(gemInstallCommandList).times(1)


		mockControl.play {
			coverageTask.coverage()
		}

	}


	@Test
	void testCoverageHTML() {
		mockInstall()

		project.coverage.outputFormat = 'html'

		File outputFile = new File(project.coverage.outputDirectory, "coverage.html")

		def gcovrCommand = new File(project.coverage.outputDirectory, 'gcovr-3.2/scripts/gcovr').absolutePath


		def gemInstallCommandList = [
						'python',
						gcovrCommand,
						'-r',
						'.',
						"--html",
						"--html-details",
						"-o",
						outputFile.absolutePath];
		commandRunnerMock.run(gemInstallCommandList).times(1)


		mockControl.play {
			coverageTask.coverage()
		}

	}
}
