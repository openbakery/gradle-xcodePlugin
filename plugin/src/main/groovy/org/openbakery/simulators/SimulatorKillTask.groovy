package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.Type
import org.openbakery.XcodePlugin

/**
 * Created by rene on 05.11.15.
 */
class SimulatorKillTask extends AbstractSimulatorTask {

	public SimulatorKillTask() {
		setDescription("Deletes contents and settings for all Simulators")
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
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
