package org.openbakery.testdouble

import org.apache.commons.io.FileUtils
import org.openbakery.simulators.SimulatorControl

class SimulatorControlStub extends SimulatorControl {

	File simctlListOutput

	public SimulatorControlStub(String filename) {
		super(null, null)
		simctlListOutput = new File("../libxcode/src/test/Resource/", filename);
	}

	@Override
	String simctl(String... commands) {
		if (commands == ["list"]) {
			return FileUtils.readFileToString(simctlListOutput)
		}
		return null;
	}
}
