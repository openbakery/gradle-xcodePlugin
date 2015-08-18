package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodePlugin

/**
 * Created by rene on 30.04.15.
 */
class SimulatorsCreateTask extends AbstractXcodeTask {
	SimulatorControl simulatorControl


	public SimulatorsCreateTask() {
		setDescription("Delete and creates all iOS Simulators");
		simulatorControl = new SimulatorControl(project)

	}

	void executeTask() {
		simulatorControl.deleteAll()
		simulatorControl.createAll()
	}
}
