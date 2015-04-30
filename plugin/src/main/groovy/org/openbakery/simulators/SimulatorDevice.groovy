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
		def PATTERN = ~/^(.*)?\s\((.*)?\)\s\((.*)?\)/

		def matcher = PATTERN.matcher(line)
		if (matcher.find()) {
			name = matcher[0][1].trim()
			identifier = matcher[0][2]
			state = matcher[0][3]
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
