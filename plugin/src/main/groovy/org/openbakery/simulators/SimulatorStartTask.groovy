package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.Destination
import org.openbakery.Type
import org.openbakery.XcodePlugin
import org.openbakery.tools.DestinationResolver

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
