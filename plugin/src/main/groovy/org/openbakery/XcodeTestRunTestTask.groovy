package org.openbakery

import groovy.io.FileType
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.util.ConfigureUtil
import org.openbakery.output.TestBuildOutputAppender
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Xcodebuild
import org.openbakery.xcode.XcodebuildParameters

/**
 * User: rene
 * Date: 25/10/16
 */
class XcodeTestRunTestTask extends AbstractXcodeTask {

	XcodebuildParameters parameters = new XcodebuildParameters()
	private List<Destination> destinationsCache

	Object bundleDirectory

	XcodeTestRunTestTask() {
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
		return new TestBuildOutputAppender(progressLogger, output, destinations)
	}

	@TaskAction
	def testRun() {
		parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)

		parameters.xctestrun = getXcruntestFiles()
		def destinations = getDestinations()
		Xcodebuild xcodebuild = new Xcodebuild(commandRunner, xcode, parameters, destinations)
		xcodebuild.executeTestWithoutBuilding(project.projectDir.absolutePath, createOutputAppender(destinations), project.xcodebuild.environment)

	}

	void destination(Closure closure) {
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		setDestination(destination)
	}

	void setDestination(def destination) {
		parameters.setDestination(destination)
	}

	List<Destination> getDestinations() {
		if (destinationsCache == null) {
			destinationsCache = getDestinationResolver().getDestinations(parameters)
		}
		return destinationsCache
	}


	void setBundleDirectory(Object bundleDirectory) {
		this.bundleDirectory = bundleDirectory
	}

	File getBundleDirectory() {
		if (bundleDirectory instanceof File) {
			return bundleDirectory
		}
		if (bundleDirectory != null) {
			return project.file(bundleDirectory)
		}
		return new File(".")
	}


	def getXcruntestFiles() {
		List<File> result = []
		getBundleDirectory().eachFileRecurse(FileType.FILES) {
		    if(it.name.endsWith('.xctestrun')) {
					result << it
		    }
		}
		return result
	}
}
