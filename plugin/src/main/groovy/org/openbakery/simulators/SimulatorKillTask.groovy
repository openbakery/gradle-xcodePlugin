package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.CommandRunner
import org.openbakery.Type
import org.openbakery.XcodePlugin

/**
 * Created by rene on 05.11.15.
 */
class SimulatorKillTask extends DefaultTask {

	SimulatorControl simulatorControl


	public SimulatorKillTask() {
		setDescription("Deletes contents and settings for all iOS Simulators")
		dependsOn(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		this.setOnlyIf {
			return project.xcodebuild.isSimulatorBuildOf(Type.iOS)
		}
		simulatorControl = new SimulatorControl(project, new CommandRunner())
	}


	@TaskAction
	void run() {
		simulatorControl.killAll()
	}
}
