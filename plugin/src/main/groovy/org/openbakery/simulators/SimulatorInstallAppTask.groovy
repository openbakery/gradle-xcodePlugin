package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.openbakery.CommandRunnerException
import org.openbakery.codesign.Codesign
import org.openbakery.codesign.CodesignParameters

class SimulatorInstallAppTask extends AbstractSimulatorTask {

	Codesign codesign

	public SimulatorInstallAppTask() {
		setDescription("Install app on iOS Simulators")
		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
		dependsOn(XcodePlugin.SIMULATORS_START_TASK_NAME)
		codesign = new Codesign(xcode, new CodesignParameters(), commandRunner, plistHelper)
	}

	@TaskAction
	void run() {
		try {
			logger.lifecycle("Signing " + project.xcodebuild.applicationBundle.absolutePath)
			codesign.sign(project.xcodebuild.applicationBundle)
			logger.lifecycle("Installing " + project.xcodebuild.applicationBundle.absolutePath)
			simulatorControl.simctl("install", "booted", project.xcodebuild.applicationBundle.absolutePath)
		} catch (CommandRunnerException ex) {
			println "Unable to install" + project.xcodebuild.applicationBundle.absolutePath
			throw ex
		}
	}
}
