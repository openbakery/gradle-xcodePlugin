package org.openbakery

import groovy.io.FileType
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Codesign
import org.openbakery.codesign.CodesignParameters
import org.openbakery.output.TestBuildOutputAppender
import org.openbakery.test.TestResult
import org.openbakery.test.TestResultParser
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild


/**
 * User: rene
 * Date: 25/10/16
 */
class XcodeTestRunTask extends AbstractXcodeTestTask {

	private List<Destination> destinationsCache

	private Object bundleDirectory
	@Internal File outputDirectory = null

	@Internal
	protected Codesign codesign = null

	protected boolean showProgress = false


	XcodeTestRunTask() {
		super()
		dependsOn(
			XcodePlugin.SIMULATORS_KILL_TASK_NAME
		)
		this.description = "Create a build for test of the Xcode project"
	}


	TestBuildOutputAppender createOutputAppender(List<Destination> destinations) {
		String name = getClass().getName()
		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(getClass(), LogLevel.LIFECYCLE);
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(getClass()).start(name, name);
		def result = new TestBuildOutputAppender(progressLogger, output, destinations)
		result.fullProgress = this.showProgress
		result
	}

	@TaskAction
	def testRun() {
		parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)
		parameters.xctestrun = getXcruntestFiles()

		if (parameters.xctestrun.size == 0) {
			throw new IllegalStateException("No tests found!")
		}

		File testLogsDirectory = new File(parameters.derivedDataPath, "Logs/Test")

		outputDirectory = new File(project.getBuildDir(), "test");
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		commandRunner.setOutputFile(new File(outputDirectory, "xcodebuild-output.txt"));


		if (runOnDevice()) {
			logger.lifecycle("Perform codesign")
			Codesign codesign = getCodesign()

			parameters.xctestrun.each() {

				String appBundle = getBundleFromFile(it, "TestHostPath")
				File appBundleFile = new File(it.parentFile, appBundle)

				codesign.sign(new Bundle(appBundleFile, parameters.type, plistHelper))

				String testBundle = getBundleFromFile(it, "TestBundlePath")
				File testBundleFile = new File(appBundleFile, testBundle)
				codesign.sign(new Bundle(testBundleFile, parameters.type, plistHelper))
			}

		}

		def destinations = getDestinations()
		try {
			Xcodebuild xcodebuild = new Xcodebuild(project.projectDir, commandRunner, xcode, parameters, destinations)

			parameters.xctestrun.each {
				xcodebuild.executeTestWithoutBuilding(createOutputAppender(destinations), project.xcodebuild.environment, it)
			}
		} catch (CommandRunnerException ex) {
			throw new Exception("Error attempting to run the unit tests!", ex);
		} finally {
			processTestResult(testLogsDirectory)
		}
	}


	String getBundleFromFile(File file, String key) {
		String bundle = plistHelper.getValueFromPlist(file, "Tests:" + key)
		if (bundle.startsWith("__TESTROOT__/")) {
			bundle = bundle - "__TESTROOT__/"
		}
		if (bundle.startsWith("__TESTHOST__/")) {
			bundle = bundle - "__TESTHOST__/"
		}
		return bundle
	}


	void setBundleDirectory(Object bundleDirectory) {
		this.bundleDirectory = bundleDirectory
	}

	@InputDirectory
	@Optional
	File getBundleDirectory() {
		if (bundleDirectory instanceof File) {
			return bundleDirectory
		}
		if (bundleDirectory != null) {
			return project.file(bundleDirectory)
		}
		return project.file(".")
	}


	@Internal
	List<File> getXcruntestFiles() {
		List<File> result = []
		getBundleDirectory().eachFileRecurse(FileType.FILES) {
			if (it.name.endsWith('.xctestrun')) {
				result << it.absoluteFile
			}
		}
		return result
	}

	boolean runOnDevice() {
		if (parameters.type != Type.macOS) {
			// os x does not have a simulator
			return !parameters.simulator
		}
		return true
	}

	Codesign getCodesign() {
		if (runOnDevice()) {
			if (codesign == null) {
				CodesignParameters parameters = new CodesignParameters()
				parameters.signingIdentity = getSigningIdentity()
				parameters.keychain = project.xcodebuild.signing.keychainPathInternal
				parameters.mobileProvisionFiles = project.xcodebuild.signing.mobileProvisionFile
				parameters.type = project.xcodebuild.type
				codesign = new Codesign(xcode, parameters, commandRunner, plistHelper)
				if (project.xcodebuild.signing.hasEntitlementsFile()) {
					codesign.useEntitlements(project.xcodebuild.signing.entitlementsFile)
				}
			}
		}
		return codesign
	}
}
