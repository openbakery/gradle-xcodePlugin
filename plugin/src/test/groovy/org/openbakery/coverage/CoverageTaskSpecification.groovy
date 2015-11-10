package org.openbakery.coverage

import org.apache.commons.io.FileUtils
import org.gmock.GMockController
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.openbakery.util.AntBuilderStub
import spock.lang.Specification

/**
 * Created by rene on 30.01.15.
 */
class CoverageTaskSpecification extends Specification {


	Project project
	CoverageTask coverageTask

	CommandRunner commandRunner = Mock(CommandRunner)
	org.gradle.api.AntBuilder antBuilder = new AntBuilderStub()

	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		coverageTask = project.getTasks().getByPath('coverage')

		coverageTask.commandRunner = commandRunner

		project.ant = antBuilder
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "coverage XML"() {
		given:
		project.coverage.outputFormat = 'xml'

		File outputFile = new File(project.coverage.outputDirectory, "coverage.xml")

		def gcovrCommand = new File(project.coverage.outputDirectory, 'gcovr-3.2/scripts/gcovr').absolutePath

		when:
		coverageTask.coverage()

		then:
		1 * commandRunner.run(['python',
								gcovrCommand,
								'-r',
								'.',
								"--xml",
								"-o",
								outputFile.absolutePath])


		antBuilder.commands["get"]["src"] == "https://github.com/gcovr/gcovr/archive/3.2.zip"
		antBuilder.commands["unzip"]["src"].name == "3.2.zip"
	}



	def testCoverageHTML() {
		given:
		project.coverage.outputFormat = 'html'
		File outputFile = new File(project.coverage.outputDirectory, "coverage.html")
		def gcovrCommand = new File(project.coverage.outputDirectory, 'gcovr-3.2/scripts/gcovr').absolutePath


		when:
		coverageTask.coverage()

		then:

		1 * commandRunner.run( [
								'python',
								gcovrCommand,
								'-r',
								'.',
								"--html",
								"--html-details",
								"-o",
								outputFile.absolutePath])

	}

}
