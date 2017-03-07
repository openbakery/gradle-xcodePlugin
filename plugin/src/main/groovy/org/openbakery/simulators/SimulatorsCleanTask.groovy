package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction

class SimulatorsCleanTask extends AbstractSimulatorTask {



	public SimulatorsCleanTask() {
		setDescription("Deletes contents and settings for all iOS Simulators")
	}


	@TaskAction
	void run() {
		simulatorControl.eraseAll()
	}
}
