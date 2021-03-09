package org.openbakery.simulators

class SimulatorDevicePair {

	public String identifier
	public SimulatorDevice watch
	public SimulatorDevice phone

	public SimulatorDevicePair(String identifier, SimulatorDevice watch, SimulatorDevice phone) {
		this.identifier = identifier
		this.watch = watch
		this.phone = phone
	}

	public SimulatorDevicePair(String line) {

		//   291E69F5-A889-47EA-87FA-7581E610E570 (disconnected)
		def tokenizer = new StringTokenizer(line, "()");
		if (tokenizer.hasMoreTokens()) {
			identifier = tokenizer.nextToken().trim();
		}
	}
}
