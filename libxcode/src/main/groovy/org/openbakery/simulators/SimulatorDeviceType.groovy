package org.openbakery.simulators

import org.openbakery.util.StringHelper

class SimulatorDeviceType {

	public String name
	public String identifier
	public String shortIdentifier

	public SimulatorDeviceType(String line) {
		//iPhone 4s (com.apple.CoreSimulator.SimDeviceType.iPhone-4s)
		// or
		// iPad Pro (9.7-inch) (com.apple.CoreSimulator.SimDeviceType.iPad-Pro--9-7-inch-)
		def tokens = line.split(" ") as List

		int index = tokens.size() -1

		if (index < 1) {
			return
		}

		identifier = StringHelper.removeBrackets(tokens[index])
		shortIdentifier = identifier - "com.apple.CoreSimulator.SimDeviceType."
		name = tokens[0..index-1].join(" ").trim()

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

		if (shortIdentifier.startsWith("Apple-TV") && simulatorRuntime.shortIdentifier.startsWith("tvOS")) {
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
