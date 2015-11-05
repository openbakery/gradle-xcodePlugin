package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.Type
import org.openbakery.XcodePlugin

class SimulatorsStartTask extends DefaultTask {
	SimulatorControl simulatorControl

	public SimulatorsStartTask() {
		setDescription("Start iOS Simulators")
		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
		simulatorControl = new SimulatorControl(project, new CommandRunner())
	}

	@TaskAction
	void run() {

		SimulatorRuntime runtime = simulatorControl.getMostRecentRuntime(Type.iOS)

		if (runtime == null) {
			throw new IllegalStateException("No simulator runtime found for " + Type.iOS)
		}

		List<SimulatorDevice> deviceList = simulatorControl.getDevices(runtime)

		if (deviceList == null || deviceList.size() == 0) {
			throw new IllegalStateException("No device found runtime " + runtime)
		}

		def device = deviceList.get(0)

		simulatorControl.killAll()
		simulatorControl.runDevice(device)
		simulatorControl.waitForDevice(device)
	}
}
