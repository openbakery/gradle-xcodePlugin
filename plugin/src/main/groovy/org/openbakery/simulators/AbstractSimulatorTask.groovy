package org.openbakery.simulators

import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunner
import org.openbakery.Destination
import org.openbakery.XcodePlugin
import org.openbakery.tools.DestinationResolver

/**
 * Created by rene on 27.06.16.
 */
class AbstractSimulatorTask extends AbstractXcodeTask {

	SimulatorControl simulatorControl
	DestinationResolver destinationResolver

	public AbstractSimulatorTask() {
	}


	Destination getDestination() {
		return getDestinationResolver().getDestinations(project.xcodebuild.getXcodebuildParameters()).first()
	}


	DestinationResolver getDestinationResolver() {
		if (destinationResolver == null) {
			destinationResolver = new DestinationResolver(getSimulatorControl())
		}
		return destinationResolver
	}

	SimulatorControl getSimulatorControl() {
		if (simulatorControl == null) {
			simulatorControl = new SimulatorControl(project, this.commandRunner, xcode)
		}
		return simulatorControl
	}

}
