package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.xcode.Destination
import org.openbakery.XcodePlugin

class SimulatorStartTask extends AbstractSimulatorTask {

	public SimulatorStartTask() {
		setDescription("Start iOS Simulators")
		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
	}



	@TaskAction
	void run() {


		Destination destination = getDestination()

		SimulatorDevice device = simulatorControl.getDevice(destination)

		simulatorControl.killAll()
		simulatorControl.runDevice(device)
		simulatorControl.waitForDevice(device)
	}
}
