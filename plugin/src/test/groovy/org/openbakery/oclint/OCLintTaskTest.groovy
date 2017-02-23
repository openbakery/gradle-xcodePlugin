package org.openbakery.oclint

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.testdouble.AntBuilderStub
import org.junit.After
import org.junit.Before
import org.junit.Test

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 22.07.15.
 */
class OCLintTaskTest {


	File projectDir
	Project project

	OCLintTask ocLintTask
	AntBuilderStub antBuilderStub = new AntBuilderStub()

	String downloadURL = "https://github.com/oclint/oclint/releases/download/v0.11/oclint-0.11-x86_64-darwin-15.6.0.tar.gz"
	String downloadURLForSierra = "https://github.com/oclint/oclint/releases/download/v0.11.1/oclint-0.11.1-x86_64-darwin-16.3.0.tar.gz"
	String oclintPath = 'oclint-0.11'

	File tmpDirectory
	File outputDirectory

	String savedOSVersion

	def commandRunnerMock
	@Before
	void setUp() {
		savedOSVersion = System.getProperty("os.version")

		System.setProperty("os.version", "10.11.0")

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir.mkdirs()
		project.apply plugin: org.openbakery.XcodePlugin

		ocLintTask = project.tasks.findByName(XcodePlugin.OCLINT_REPORT_TASK_NAME);

		antBuilderStub = new AntBuilderStub()
		project.ant = antBuilderStub

		commandRunnerMock = new MockFor(CommandRunner)
		tmpDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("tmp/oclint")
		outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("report/oclint")

	}

	@After
	void cleanup() {
		System.setProperty("os.version", savedOSVersion)
		FileUtils.deleteDirectory(projectDir)
	}

	String filename(String URLString) {
		return FilenameUtils.getName(new URL(URLString).getPath())
	}

	String destinationPath(String URLString) {
		new File(tmpDirectory, filename(URLString)).absolutePath
	}

	@Test
	void filenameFromURL() {
		// just make sure that the filename function does the right thing ;-)
		assertThat(filename(downloadURLForSierra), equalTo("oclint-0.11.1-x86_64-darwin-16.3.0.tar.gz"))
	}

	@Test
	void create() {
		assertThat(ocLintTask, is(instanceOf(OCLintTask.class)))
	}

	void mockCommandRunner() {
		commandRunnerMock.demand.run {}
		commandRunnerMock.demand.run {}
		commandRunnerMock.demand.run {}
		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()
	}

	@Test
	void outputDirectory() {
		mockCommandRunner()
		ocLintTask.run()
		assertThat(outputDirectory.exists(), is(true))
	}


	@Test
	void download() {
		mockCommandRunner()
		ocLintTask.run()
		assertThat(antBuilderStub.get.size(), is(1))
		def getCall = antBuilderStub.get.first()
		assertThat(getCall, hasEntry("src", downloadURL))
		File downloadDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("tmp/oclint")
		assertThat(getCall, hasEntry("dest", downloadDirectory))
	}


	@Test
	void downloadOnSierra() {
		System.setProperty("os.version", "10.12.0")
		mockCommandRunner()
		ocLintTask.run()
		assertThat(antBuilderStub.get.size(), is(1))
		def getCall = antBuilderStub.get.first()
		assertThat(getCall, hasEntry("src", downloadURLForSierra))
	}

	void mockUntar() {
		commandRunnerMock.demand.run { parameters ->


					def expectedParameters = [
									'tar',
									'xzf',
									destinationPath(downloadURL),
									'-C',
									tmpDirectory.absolutePath
					]
					assertThat(parameters, is(equalTo(expectedParameters)))
				}
	}

	@Test
	void untar() {
		mockUntar()

		commandRunnerMock.demand.run {}
		commandRunnerMock.demand.run {}
		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()
		commandRunnerMock.verify ocLintTask.commandRunner

	}

	def mockOclintXcodebuild() {
		commandRunnerMock.demand.run { parameters ->

			File tmpDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("tmp/oclint")


			def expectedParameters = [
							new File(tmpDirectory, oclintPath + '/bin/oclint-xcodebuild').absolutePath,
							'build/xcodebuild-output.txt']

			assertThat(parameters, is(equalTo(expectedParameters)))
		}
	}

	@Test
	void oclintXcodebuild() {
		mockUntar()
		mockOclintXcodebuild()
		commandRunnerMock.demand.run {}


		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


	def expectedParameters(String reportType) {

		return [
						new File(tmpDirectory, oclintPath + '/bin/oclint-json-compilation-database').absolutePath,
						"--",
						"-max-priority-1=0",
						"-max-priority-2=10",
						"-max-priority-3=20",
						"-report-type",
						reportType,
						"-o",
						new File(outputDirectory, 'oclint.html').absolutePath,
		]
	}

	@Test
	void oclint() {
		mockUntar()
		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->
			assertThat(parameters, is(equalTo(expectedParameters("html"))))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}

	@Test
	void oclintReportType() {

		project.oclint.reportType = "pmd"
		mockUntar()
		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->
			assertThat(parameters, is(equalTo(expectedParameters("pmd"))))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


	@Test
	void oclintWithRules() {

		project.oclint.rules = [
						"LINT_LONG_LINE=300",
						"LINT_LONG_VARIABLE_NAME=64",
						"LINT_LONG_METHOD=150",
		]

		mockUntar()
		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->

			def expectedParameters = [
							new File(tmpDirectory, oclintPath + '/bin/oclint-json-compilation-database').absolutePath,
							"--",
							"-max-priority-1=0",
							"-max-priority-2=10",
							"-max-priority-3=20",
							"-report-type",
							"html",
							"-rc=LINT_LONG_LINE=300",
							"-rc=LINT_LONG_VARIABLE_NAME=64",
							"-rc=LINT_LONG_METHOD=150",
							"-o",
							new File(outputDirectory, 'oclint.html').absolutePath,
			]

			assertThat(parameters, is(equalTo(expectedParameters)))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


	@Test
	void oclintWithExclude() {

		mockUntar()
		mockOclintXcodebuild()

		project.oclint.excludes = [
						"Pods",
						"Test",
		]


		commandRunnerMock.demand.run { parameters ->

			def expectedParameters = [
							new File(tmpDirectory, oclintPath + '/bin/oclint-json-compilation-database').absolutePath,
							"-e",
							"Pods",
							"-e",
							"Test",
							"--",
							"-max-priority-1=0",
							"-max-priority-2=10",
							"-max-priority-3=20",
							"-report-type",
							"html",
							"-o",
							new File(outputDirectory, 'oclint.html').absolutePath,
			]

			assertThat(parameters, is(equalTo(expectedParameters)))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


	@Test
	void oclint_priority() {
		project.oclint.maxPriority1 = 100
		project.oclint.maxPriority2 = 200
		project.oclint.maxPriority3 = 300

		mockUntar()
		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->
			def expectedParameters = [
							new File(tmpDirectory, oclintPath + '/bin/oclint-json-compilation-database').absolutePath,
							"--",
							"-max-priority-1=100",
							"-max-priority-2=200",
							"-max-priority-3=300",
							"-report-type",
							"html",
							"-o",
							new File(outputDirectory, 'oclint.html').absolutePath,
			]

			assertThat(parameters, is(equalTo(expectedParameters)))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


	@Test
	void oclint_disableRule() {

		project.oclint.disableRules = [
						"UnusedMethodParameter",
						"UselessParentheses",
						"IvarAssignmentOutsideAccessorsOrInit"
		]

		mockUntar()
		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->

			def expectedParameters = [
							new File(tmpDirectory, oclintPath + '/bin/oclint-json-compilation-database').absolutePath,
							"--",
							"-max-priority-1=0",
							"-max-priority-2=10",
							"-max-priority-3=20",
							"-report-type",
							"html",
							"-disable-rule=UnusedMethodParameter",
							"-disable-rule=UselessParentheses",
							"-disable-rule=IvarAssignmentOutsideAccessorsOrInit",
							"-o",
							new File(outputDirectory, 'oclint.html').absolutePath,
			]

			assertThat(parameters, is(equalTo(expectedParameters)))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


	@Test
	void oclint_bin_directory() {
		assertThat(ocLintTask.oclintBinDirectory(), equalTo(new File(ocLintTask.getTemporaryDirectory("oclint"), "oclint-0.11/bin")))
	}


	@Test
	void oclint_bin_directory_sierra() {
		System.setProperty("os.version", "10.12.0")
		assertThat(ocLintTask.oclintBinDirectory(), equalTo(new File(ocLintTask.getTemporaryDirectory("oclint"), "oclint-0.11.1/bin")))
	}

}
