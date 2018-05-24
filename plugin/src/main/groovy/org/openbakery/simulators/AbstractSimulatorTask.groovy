package org.openbakery.simulators

import org.openbakery.AbstractXcodeTask
import org.openbakery.xcode.Destination

class AbstractSimulatorTask extends AbstractXcodeTask {


	public AbstractSimulatorTask() {
	}


	Destination getDestination() {
		return getDestinationResolver()
				.getDestinations(project.xcodebuild.getXcodebuildParameters())
				.first()
	}


}
