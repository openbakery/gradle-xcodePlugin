package org.openbakery.testdouble

import org.apache.commons.io.FileUtils
import org.openbakery.simulators.SimulatorControl

class SimulatorControlFake extends SimulatorControl {

	File simctlListOutput

	ArrayList<String>lastExecutedCommand

	public SimulatorControlFake() {
		super(null, new XcodeFake("12.0"))
		simctlListOutput =  new File("../libtest/src/main/Resource/simctl-list-xcode12-full.json")
	}

	public SimulatorControlFake(String filename) {
		this(new File("../libtest/src/main/Resource/", filename))
	}

	public SimulatorControlFake(File file) {
		super(null, new XcodeFake("11.0"))
		simctlListOutput = file
	}


	@Override
	String executeWithResult(String... commands) {
		lastExecutedCommand = commands
		if (commands.first() == "list") {
			return FileUtils.readFileToString(simctlListOutput)
		}
		return null;
	}

	@Override
	void execute(String... commands) {
		lastExecutedCommand = commands
	}
}
