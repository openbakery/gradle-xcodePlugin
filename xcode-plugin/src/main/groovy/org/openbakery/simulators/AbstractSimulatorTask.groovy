package org.openbakery.simulators

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.openbakery.AbstractXcodeTask
import org.openbakery.xcode.Destination

class AbstractSimulatorTask extends AbstractXcodeTask {

	public AbstractSimulatorTask() {
	}


	@Internal
	Destination getDestination() {
		return getDestinationResolver().getDestinations(project.xcodebuild.getXcodebuildParameters()).first()
	}



}
