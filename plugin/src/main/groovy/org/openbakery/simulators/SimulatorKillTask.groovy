package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin

class SimulatorKillTask extends AbstractSimulatorTask {

	public SimulatorKillTask() {
		setDescription("Deletes contents and settings for all Simulators")
		this.setOnlyIf {
			isSimulator()
		}
	}

	boolean isSimulator() {
		project.xcodebuild.isSimulatorBuildOf(Type.iOS) || project.xcodebuild.isSimulatorBuildOf(Type.tvOS)
	}

	@TaskAction
	void run() {
		simulatorControl.killAll()
	}
}
