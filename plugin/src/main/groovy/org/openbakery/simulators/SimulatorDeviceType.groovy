package org.openbakery.simulators

/**
 * Created by rene on 30.04.15.
 */
class SimulatorDeviceType {

	public String name
	public String identifier
	public String shortIdentifier

	public SimulatorDeviceType(String line) {
		//iPhone 4s (com.apple.CoreSimulator.SimDeviceType.iPhone-4s)

		def PATTERN = ~/^(.*)?\s\((.*)?\)/

		def matcher = PATTERN.matcher(line)
		if (matcher.find()) {
			name = matcher[0][1]
			identifier = matcher[0][2]
			shortIdentifier = identifier - "com.apple.CoreSimulator.SimDeviceType."
		}

	}


	@Override
	public String toString() {
		return "SimulatorDeviceType{" +
						"name='" + name + '\'' +
						", identifier='" + identifier + '\'' +
						'}';
	}

	boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false

		SimulatorDeviceType that = (SimulatorDeviceType) o

		if (identifier != that.identifier) return false

		return true
	}

	int hashCode() {
		return identifier.hashCode()
	}


	boolean canCreateWithRuntime(SimulatorRuntime simulatorRuntime) {
		if (shortIdentifier.startsWith("Apple-Watch") && simulatorRuntime.shortIdentifier.startsWith("watchOS")) {
			return true
		}

		if (shortIdentifier.startsWith("iPhone") ||
						shortIdentifier.startsWith("iPad") ||
						shortIdentifier.endsWith("iPhone") ||
						shortIdentifier.endsWith("iPad")) {
			return simulatorRuntime.shortIdentifier.startsWith("iOS")
		}
		return false
	}
}
