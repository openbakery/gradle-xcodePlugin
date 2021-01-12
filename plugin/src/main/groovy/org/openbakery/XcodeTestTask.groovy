package org.openbakery

import groovy.xml.MarkupBuilder
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.lang.time.DurationFormatUtils
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Internal
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
class XcodeTestTask extends AbstractXcodeTestTask {


	@Internal File outputDirectory = null
	private File testLogsDirectory = null

	XcodeTestTask() {
		super()
		dependsOn(
			XcodePlugin.XCODE_CONFIG_TASK_NAME,
			XcodePlugin.SIMULATORS_KILL_TASK_NAME,
			XcodePlugin.COCOAPODS_INSTALL_TASK_NAME,
			XcodePlugin.CARTHAGE_BOOTSTRAP_TASK_NAME
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
		logger.debug("test")
		parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)
		logger.debug("test parameters {}", parameters)

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

		logger.debug("before getDestinations parameters {}", parameters)
		def destinations = getDestinations()
		logger.debug("after getDestinations parameters {}", parameters)
		logger.debug("test destinations {}", destinations)


		try {
			Xcodebuild xcodebuild = new Xcodebuild(project.projectDir, commandRunner, xcode, parameters, destinations)
			logger.debug("Executing xcodebuild with {}", xcodebuild)
			xcodebuild.executeTest(createOutputAppender(destinations), project.xcodebuild.environment)

		} catch (CommandRunnerException ex) {
			throw new Exception("Error attempting to run the unit tests!", ex);
		} finally {
			processTestResult(testLogsDirectory)
		}
	}

}
