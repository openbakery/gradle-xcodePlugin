package org.openbakery.simulators

/**
 * Created by rene on 30.04.15.
 */
class SimulatorRuntime {
	String name
	String version
	String buildNumber
	String identifier


	public SimulatorRuntime(String line) {
		// pattern to parse this:
		// iOS 7.1 (7.1 - 11D167) (com.apple.CoreSimulator.SimRuntime.iOS-7-1)
		def PATTERN = ~/^(\w*\s\d+\.\d+)\s\((\d+\.\d+)\s-\s(\w+)\)\s\(([^\)]*)\)/

		def matcher = PATTERN.matcher(line)
		if (matcher.find()) {
			name = matcher[0][1]
			version = matcher[0][2]
			buildNumber = matcher[0][3]
			identifier = matcher[0][4]
		}
	}


	@Override
	public String toString() {
		return "SimulatorRuntime{" +
						"name='" + name + '\'' +
						", version='" + version + '\'' +
						", buildNumber='" + buildNumber + '\'' +
						", identifier='" + identifier + '\'' +
						'}';
	}

	boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false
		SimulatorRuntime that = (SimulatorRuntime) o
		if (identifier != that.identifier) return false
		return true
	}

	int hashCode() {
		return identifier.hashCode()
	}
}
