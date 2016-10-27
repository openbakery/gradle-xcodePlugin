package org.openbakery.simulators

import org.openbakery.AbstractXcodeTask
import org.openbakery.xcode.Destination
import org.openbakery.xcode.DestinationResolver

/**
 * Created by rene on 27.06.16.
 */
class AbstractSimulatorTask extends AbstractXcodeTask {


	public AbstractSimulatorTask() {
	}


	Destination getDestination() {
		return getDestinationResolver().getDestinations(project.xcodebuild.getXcodebuildParameters()).first()
	}



}
