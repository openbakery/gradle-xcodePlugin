package org.openbakery.oclint

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.stubs.AntBuilderStub
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

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

	def commandRunnerMock
	@BeforeMethod
	void setUp() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir.mkdirs()
		project.apply plugin: org.openbakery.XcodePlugin

		ocLintTask = project.tasks.findByName(XcodePlugin.OCLINT_TASK_NAME);

		antBuilderStub = new AntBuilderStub()
		project.ant = antBuilderStub

		commandRunnerMock = new MockFor(CommandRunner)

	}

	@AfterMethod
	void cleanup() {
		FileUtils.deleteDirectory(projectDir)
	}

	@Test
	void create() {
		assertThat(ocLintTask, is(instanceOf(OCLintTask.class)))
	}

	void mockCommandRunner() {
		commandRunnerMock.demand.run {}
		commandRunnerMock.demand.run {}
		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()
	}

	@Test
	void outputDirectory() {
		mockCommandRunner()
		ocLintTask.run()
		File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")
		assertThat(outputDirectory.exists(), is(true))
	}


	@Test
	void download() {
		mockCommandRunner()

		ocLintTask.run()

		assertThat(antBuilderStub.get.size(), is(1));
		def getCall = antBuilderStub.get.first()
		assertThat(getCall, hasEntry("src", "http://archives.oclint.org/releases/0.8/oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz"));

	}


	@Test
	void gunzip() {
		mockCommandRunner()

		ocLintTask.run()

		assertThat(antBuilderStub.gunzip.size(), is(1));

		File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")
		def archive = new File(outputDirectory, 'oclint-0.8.1-x86_64-darwin-14.0.0.tar.gz').absolutePath
		def gunzip = antBuilderStub.gunzip.first()
		assertThat(gunzip, hasEntry("src", archive));
	}


	@Test
	void untar() {
		mockCommandRunner()

		ocLintTask.run()

		assertThat(antBuilderStub.untar.size(), is(1));

		File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")
		def archive = new File(outputDirectory, 'oclint-0.8.1-x86_64-darwin-14.0.0.tar').absolutePath
		def untar = antBuilderStub.untar.first()
		assertThat(untar, hasEntry("src", archive));
	}

	def mockOclintXcodebuild() {
		commandRunnerMock.demand.run { parameters ->

			File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")


			def expectedParameters = [
							new File(outputDirectory, 'oclint-0.8.1/bin/oclint-xcodebuild').absolutePath,
							'build/xcodebuild-output.txt']

			assertThat(parameters, is(equalTo(expectedParameters)))
		}
	}

	@Test
	void oclintXcodebuild() {
		mockOclintXcodebuild()
		commandRunnerMock.demand.run {}


		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}



	@Test
	void oclint() {

		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->



			File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")

			def expectedParameters = [
							new File(outputDirectory, 'oclint-0.8.1/bin/oclint-json-compilation-database').absolutePath,
							"--",
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
	void oclintReportType() {

		project.oclint.reportType = "pmd"
		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->



			File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")

			def expectedParameters = [
							new File(outputDirectory, 'oclint-0.8.1/bin/oclint-json-compilation-database').absolutePath,
							"--",
							"-report-type",
							"pmd",
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
	void oclintWithRules() {

		project.oclint.rules = [
						"LINT_LONG_LINE=300",
						"LINT_LONG_VARIABLE_NAME=64",
						"LINT_LONG_METHOD=150",
		]

		mockOclintXcodebuild()


		commandRunnerMock.demand.run { parameters ->



			File outputDirectory = project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("oclint")

			def expectedParameters = [
							new File(outputDirectory, 'oclint-0.8.1/bin/oclint-json-compilation-database').absolutePath,
							"--",
							"-report-type",
							"html",
							"-rc", "LINT_LONG_LINE=300",
							"-rc", "LINT_LONG_VARIABLE_NAME=64",
							"-rc", "LINT_LONG_METHOD=150",
							"-o",
							new File(outputDirectory, 'oclint.html').absolutePath,
			]

			assertThat(parameters, is(equalTo(expectedParameters)))
		}

		ocLintTask.commandRunner = commandRunnerMock.proxyInstance()

		ocLintTask.run()

		commandRunnerMock.verify ocLintTask.commandRunner
	}


}
