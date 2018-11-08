package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.openbakery.CommandRunnerException
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Codesign
import org.openbakery.codesign.CodesignParameters
import org.openbakery.xcode.Type

class SimulatorInstallAppTask extends AbstractSimulatorTask {

	Codesign codesign

	public SimulatorInstallAppTask() {
		setDescription("Install app on iOS Simulators")
		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
		dependsOn(XcodePlugin.SIMULATORS_START_TASK_NAME)
	}

	@TaskAction
	void run() {
		try {
			logger.lifecycle("Signing " + project.xcodebuild.applicationBundle.absolutePath)
			getCodesign().sign(new Bundle(project.xcodebuild.applicationBundle, Type.iOS))
			logger.lifecycle("Installing " + project.xcodebuild.applicationBundle.absolutePath)
			simulatorControl.simctl("install", "booted", project.xcodebuild.applicationBundle.absolutePath)
		} catch (CommandRunnerException ex) {
			println "Unable to install" + project.xcodebuild.applicationBundle.absolutePath
			throw ex
		}
	}


	Codesign getCodesign() {
		if (!codesign) {
			CodesignParameters parameters = new CodesignParameters()
			parameters.type = project.xcodebuild.type
			codesign = new Codesign(xcode, parameters, commandRunner, plistHelper)
		}
		return codesign
	}
}
