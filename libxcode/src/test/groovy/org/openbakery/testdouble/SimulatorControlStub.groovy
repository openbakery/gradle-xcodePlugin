package org.openbakery.testdouble

import org.apache.commons.io.FileUtils
import org.openbakery.simulators.SimulatorControl

/**
 * Created by rene on 27.10.16.
 */
class SimulatorControlFake extends SimulatorControl {

	File simctlListOutput

	public SimulatorControlFake(String filename) {
		super(null, null)
		simctlListOutput = new File("src/test/Resource/", filename);
	}

	@Override
	String simctl(String... commands) {
		if (commands == ["list"]) {
			return FileUtils.readFileToString(simctlListOutput)
		}
		return null;
	}
}