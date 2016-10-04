package org.openbakery.simulators

import org.openbakery.util.StringHelper

/**
 * Created by rene on 30.04.15.
 */
class SimulatorDevice {

	public String name
	public String identifier
	public String state
	public boolean available = true

	public SimulatorDevice(String line) {
		//   iPhone 4s (73C126C8-FD53-44EA-80A3-84F5F19508C0) (Shutdown)
    // or
		//   iPad Pro (12.9 inch) (C538D7F8-E581-44FF-9B17-5391F84642FB) (Shutdown)
		// or
		//    Resizable iPad (B33E6523-6E44-42EA-A8B6-AEFB6873E9E8) (Shutdown) (unavailable, device type profile not found)

		String[] tokens = line.split(" ") as List

		int index = findIdentifier(tokens)

		if (index < 0) {
			return
		}

		name = tokens[0..index-1].join(" ").trim()
		identifier = StringHelper.removeBrackets(tokens[index])

		index++
		if (tokens.length > index) {
			state = StringHelper.removeBrackets(tokens[index])
		}

		index++
		if (tokens.length > index) {
			available = !StringHelper.removeBrackets(tokens[index]).startsWith("unavailable")
		}
	}


	int findIdentifier(String[] tokens) {
		int index = -1
		tokens.eachWithIndex { String entry, int i ->
			if (entry ==~ /\(\w{8}-\w{4}-\w{4}-\w{4}-\w{12}\)/) {
				index = i
			}
		}
		return index
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
