package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Created by rene on 30.04.15.
 */
class SimulatorsCleanTask extends DefaultTask {

	SimulatorControl simulatorControl


	public SimulatorsCleanTask() {
		setDescription("Deletes contents and settings for all iOS Simulators");
		simulatorControl = new SimulatorControl(project)
	}


	@TaskAction
	void run() {
		simulatorControl.eraseAll()
	}
}
