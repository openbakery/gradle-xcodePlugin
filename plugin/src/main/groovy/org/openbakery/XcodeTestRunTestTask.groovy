package org.openbakery

import groovy.io.FileType
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import org.openbakery.xcode.Destination
import org.openbakery.xcode.XcodebuildParameters

/**
 * User: rene
 * Date: 25/10/16
 */
class XcodeTestRunTestTask extends AbstractXcodeTask {

	XcodebuildParameters parameters = new XcodebuildParameters()

	Object bundleDirectory

	XcodeTestRunTestTask() {
		super()
		dependsOn(
			XcodePlugin.SIMULATORS_KILL_TASK_NAME
		)
		this.description = "Create a build for test of the Xcode project"
	}

	@TaskAction
	def testRun() {
		parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)

		parameters.xctestrun = getXcruntestFiles()
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
