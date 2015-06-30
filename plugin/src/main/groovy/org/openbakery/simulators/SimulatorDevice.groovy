package org.openbakery.simulators

/**
 * Created by rene on 30.04.15.
 */
class SimulatorDevice {

	public String name
	public String identifier
	public String state

	public SimulatorDevice(String line) {
		//   iPhone 4s (73C126C8-FD53-44EA-80A3-84F5F19508C0) (Shutdown)
		def tokenizer = new StringTokenizer(line, "()");
		if (tokenizer.hasMoreTokens()) {
			name = tokenizer.nextToken().trim();
		}

		if (tokenizer.hasMoreTokens()) {
			identifier = tokenizer.nextToken().trim();
		}

		if (tokenizer.hasMoreTokens()) {
			tokenizer.nextToken(); // is space
		}
		if (tokenizer.hasMoreTokens()) {
			state = tokenizer.nextToken().trim();
		}

	}


	@Override
	public String toString() {
		return "SimulatorDevice{" +
						"name='" + name + '\'' +
						", identifier='" + identifier + '\'' +
						", state='" + state + '\'' +
						'}';
	}

	boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false

		SimulatorDevice that = (SimulatorDevice) o

		if (identifier != that.identifier) return false

		return true
	}

	int hashCode() {
		return identifier.hashCode()
	}
}
