package org.openbakery.simulators

import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Type

class SimulatorBootTask extends AbstractSimulatorTask {

	public SimulatorBootTask() {
			setDescription("Boot the first simulator found at the destinations")
			this.setOnlyIf {
				isSimulator()
			}
		}

		@Internal
		protected boolean isSimulator() {
			project.xcodebuild.isSimulatorBuildOf(Type.iOS) || project.xcodebuild.isSimulatorBuildOf(Type.tvOS)
		}

		@TaskAction
		void run() {
			Destination destination = getDestination()
			simulatorControl.boot(destination.id)
		}

}
