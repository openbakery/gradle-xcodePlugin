package org.openbakery

import groovy.xml.MarkupBuilder
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.lang.time.DurationFormatUtils
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.output.TestBuildOutputAppender
import org.openbakery.test.TestClass
import org.openbakery.test.TestResult
import org.openbakery.test.TestResultParser
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Xcodebuild






/**
 * User: rene
 * Date: 12.07.13
 * Time: 09:19
 */
class XcodeTestTask extends AbstractXcodeBuildTask {


	File outputDirectory = null
	File testLogsDirectory = null

	TestResultParser testResultParser = null

	XcodeTestTask() {
		super()
		dependsOn(
				XcodePlugin.XCODE_CONFIG_TASK_NAME,
				XcodePlugin.SIMULATORS_KILL_TASK_NAME
		)

		this.description = "Runs the unit tests for the Xcode project"
	}

	TestBuildOutputAppender createOutputAppender(List<Destination> destinations) {
		String name = getClass().getName()
		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(getClass(), LogLevel.LIFECYCLE);
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(getClass()).start(name, name);
		return new TestBuildOutputAppender(progressLogger, output, destinations)
	}

	@TaskAction
	def test() {

		parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)

		if (parameters.scheme == null && parameters.target == null) {
			throw new IllegalArgumentException("No 'scheme' or 'target' specified, so do not know what to build");
		}

		outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		testLogsDirectory = new File(parameters.derivedDataPath, "Logs/Test")
		testLogsDirectory.deleteDir()

		File outputFile = new File(outputDirectory, "xcodebuild-output.txt")
		commandRunner.setOutputFile(outputFile);


		def destinations = getDestinations()

		try {
			Xcodebuild xcodebuild = new Xcodebuild(commandRunner, xcode, parameters, destinations)
			logger.debug("Executing xcodebuild with {}", xcodebuild)
			xcodebuild.executeTest(project.projectDir.absolutePath, createOutputAppender(destinations), project.xcodebuild.environment)

		} catch (CommandRunnerException ex) {
			throw new Exception("Error attempting to run the unit tests!", ex);
		} finally {
			testResultParser = new TestResultParser(testLogsDirectory, destinations)
			testResultParser.parseAndStore(outputDirectory)
			int numberSuccess = testResultParser.numberSuccess()
			int numberErrors = testResultParser.numberErrors()
			if (numberErrors == 0) {
				logger.lifecycle("All " + numberSuccess + " tests were successful");
			} else {
				logger.lifecycle(numberSuccess + " tests were successful, and " + numberErrors + " failed");
			}
			if (numberErrors != 0) {
				throw new Exception("Not all unit tests are successful!")
			}

		}
	}

}
