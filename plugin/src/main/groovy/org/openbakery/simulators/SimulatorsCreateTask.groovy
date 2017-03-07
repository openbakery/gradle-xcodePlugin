package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction

class SimulatorsCreateTask extends AbstractSimulatorTask {

	public SimulatorsCreateTask() {
		setDescription("Delete and creates all iOS Simulators");
	}

	@TaskAction
	void run() {
		simulatorControl.killAll()
		simulatorControl.deleteAll()
		simulatorControl.createAll()
	}
}
