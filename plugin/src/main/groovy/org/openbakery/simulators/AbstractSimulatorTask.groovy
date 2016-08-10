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
		simulatorControl = new SimulatorControl(project, commandRunner, xcode)
		destinationResolver = new DestinationResolver(simulatorControl)
	}


	Destination getDestination() {
		return destinationResolver.getDestinations(project.xcodebuild.getXcodebuildParameters()).first()
	}

}
