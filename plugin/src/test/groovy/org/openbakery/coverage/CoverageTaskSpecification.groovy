package org.openbakery.coverage

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodeProjectFile
import org.openbakery.testdouble.AntBuilderStub
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

		project.xcodebuild.projectFile = "Test.xcodeproj"
		coverageTask = project.getTasks().getByPath('coverage')

		coverageTask.commandRunner = commandRunner

		project.ant = antBuilder
	}

	def cleanup() {

		FileUtils.deleteDirectory(project.projectDir)
		FileUtils.deleteDirectory(project.xcodebuild.derivedDataPath)

	}

	def createFile(String name) {
		File file = new File(project.projectDir, name)
		FileUtils.writeStringToFile(file, name)
		return file
	}

	def "gcovr coverage XML"() {
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


		antBuilder.get.src == ["https://github.com/gcovr/gcovr/archive/3.2.zip"]
		antBuilder.unzip.src[0].name == "3.2.zip"
	}


	def "gcovr coverage HTML"() {
		given:
		project.coverage.outputFormat = 'html'
		File outputFile = new File(project.coverage.outputDirectory, "coverage.html")
		def gcovrCommand = new File(project.coverage.outputDirectory, 'gcovr-3.2/scripts/gcovr').absolutePath


		when:
		coverageTask.coverage()

		then:

		1 * commandRunner.run([
						'python',
						gcovrCommand,
						'-r',
						'.',
						"--html",
						"--html-details",
						"-o",
						outputFile.absolutePath])

	}

	def "profile data coverage"() {
		given:
		project.ant = antBuilder
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("MyApp")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.profileData.toString().endsWith("Coverage.profdata")
	}

	def "profile data coverage with target"() {
		given:
		project.xcodebuild.target = "MyApp"
		coverageTask.binary = createFile("MyApp")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		File profData = new File(project.xcodebuild.derivedDataPath, "Build/Intermediates/CodeCoverage/MyApp/Coverage.profdata")
		FileUtils.writeStringToFile(profData, "Dummy")

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.profileData != null
	}

	def "binary coverage"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.binary.toString().endsWith("foobar")
	}

	def "profile data binary with target"() {
		given:

		File projectDir =  new File("../example/iOS/ExampleWatchkit")
		XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "ExampleWatchkit.xcodeproj/project.pbxproj"))

		Project localProject = ProjectBuilder.builder().withProjectDir(projectDir).build()
		localProject.apply plugin: org.openbakery.XcodePlugin
		localProject.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()
		localProject.xcodebuild.target = "ExampleWatchkit"

		coverageTask = localProject.getTasks().getByPath('coverage')
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		File profData = new File(localProject.xcodebuild.derivedDataPath, "Build/Intermediates/CodeCoverage/ExampleWatchkit/Coverage.profdata")
		FileUtils.writeStringToFile(profData, "Dummy")

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.profileData.toString().endsWith("Build/Intermediates/CodeCoverage/ExampleWatchkit/Coverage.profdata")
		coverageTask.report.binary.toString().endsWith("ExampleWatchkit/build/sym/Debug-iphonesimulator/ExampleWatchkit.app/ExampleWatchkit")
		thrown(IllegalArgumentException)

		cleanup:
		profData.delete()
	}

	def "include coverage"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)
		coverageTask.include = "*.m"

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.include == "*.m"
		coverageTask.report.exclude == null
	}

	def "include coverage extern"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)
		project.coverage.include = "*.m"

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.include == "*.m"
		coverageTask.report.exclude == null
	}


	def "exclude coverage"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)
		coverageTask.exclude = "*.h"

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.exclude == "*.h"
		coverageTask.report.include == null
	}

	def "exclude coverage extern"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)
		project.coverage.exclude = "*.h"

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.exclude == "*.h"
		coverageTask.report.include == null
	}

	def "html coverage"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)
		coverageTask.type = "hTML"

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.type == Report.Type.HTML
	}

	def "html coverage extern"() {
		given:
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)
		project.coverage.outputFormat = "hTML"

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.type == Report.Type.HTML
	}


	def "report output path"() {
		given:
		project.coverage.outputDirectory = new File(project.projectDir, "myCoverage")
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.destinationPath == project.coverage.outputDirectory
	}


	def "report create"() {
		given:
		coverageTask.report = Mock(Report)
		coverageTask.profileData = new File(project.projectDir, "Coverage.profdata")


		when:
		coverageTask.coverage()

		then:
		1 * coverageTask.report.create()
	}


	def "report title"() {
		given:
		project.xcodebuild.projectFile = "../directory/My Project.xcodeproj"
		project.coverage.outputDirectory = new File(project.projectDir, "myCoverage")
		coverageTask.profileData = createFile("Coverage.profdata")
		coverageTask.binary = createFile("foobar")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.title == "My Project"
	}


	def "profile data coverage with target for Xcode 7.3"() {
		given:
		project.xcodebuild.target = "MyApp"
		coverageTask.binary = createFile("MyApp")
		coverageTask.report.commandRunner = Mock(org.openbakery.coverage.command.CommandRunner)

		File profData = new File(project.xcodebuild.derivedDataPath, "Build/Intermediates/CodeCoverage/Coverage.profdata")
		FileUtils.writeStringToFile(profData, "Dummy")

		when:
		coverageTask.coverage()

		then:
		coverageTask.report.profileData != null
	}


	def "coverage path container report"() {
		expect:
		project.coverage.outputDirectory.absolutePath.contains("report/")
	}
}
