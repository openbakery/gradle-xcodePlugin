package org.openbakery.testdouble

import org.apache.commons.io.FileUtils
import org.openbakery.simulators.SimulatorControl

class SimulatorControlFake extends SimulatorControl {

	File simctlListOutput

	public SimulatorControlFake(String filename) {
		this(new File("../libtest/src/main/Resource/", filename))
	}

	public SimulatorControlFake(File file) {
		super(null, null)
		simctlListOutput = file
		xcode = new XcodeFake("11.0")
	}


	@Override
	String simctlWithResult(String... commands) {
		if (commands == ["list"]) {
			return FileUtils.readFileToString(simctlListOutput)
		}
		return null;
	}
}
