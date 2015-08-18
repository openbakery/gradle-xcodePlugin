package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodePlugin

/**
 * Created by rene on 30.04.15.
 */
class SimulatorsCleanTask extends AbstractXcodeTask {

	SimulatorControl simulatorControl


	public SimulatorsCleanTask() {
		setDescription("Deletes contents and settings for all iOS Simulators")
		simulatorControl = new SimulatorControl(project)
	}


	void executeTask() {
		simulatorControl.eraseAll()
	}
}
