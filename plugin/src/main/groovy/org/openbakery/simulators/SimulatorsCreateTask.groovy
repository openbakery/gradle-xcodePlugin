package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin

/**
 * Created by rene on 30.04.15.
 */
class SimulatorsCreateTask extends AbstractSimulatorTask {

	public SimulatorsCreateTask() {
		setDescription("Delete and creates all iOS Simulators");
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)

	}

	@TaskAction
	void run() {
		simulatorControl.killAll()
		simulatorControl.deleteAll()
		simulatorControl.createAll()
	}
}
