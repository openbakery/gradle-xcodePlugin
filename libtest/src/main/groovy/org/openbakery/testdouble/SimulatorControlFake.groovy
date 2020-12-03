package org.openbakery.testdouble

import org.apache.commons.io.FileUtils
import org.openbakery.simulators.SimulatorControl

class SimulatorControlFake extends SimulatorControl {

	File simctlListOutput

	public SimulatorControlFake(String filename) {
		this(new File("src/test/Resource/", filename))
	}

	public SimulatorControlFake(File file) {
		super(null, null)
		simctlListOutput = file
	}


	@Override
	String simctl(String... commands) {
		if (commands == ["list"]) {
			return FileUtils.readFileToString(simctlListOutput)
		}
		return null;
	}
}
