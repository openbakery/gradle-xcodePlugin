package org.openbakery.simulators

import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin

/**
 * Created by rene on 27.06.16.
 */
class AbstractSimulatorTask extends AbstractXcodeTask {

	SimulatorControl simulatorControl

	public AbstractSimulatorTask() {
		simulatorControl = new SimulatorControl(project, commandRunner, xcode)
	}

}
