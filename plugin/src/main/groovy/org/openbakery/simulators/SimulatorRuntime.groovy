package org.openbakery.simulators

/**
 * Created by rene on 30.04.15.
 */
class SimulatorRuntime {
	String name
	String version
	String buildNumber
	String identifier
	String shortIdentifier
	boolean available = true

	public SimulatorRuntime(String line) {

		def tokenizer = new StringTokenizer(line, "()");
		if (tokenizer.hasMoreTokens()) {
			name = tokenizer.nextToken().trim();
		}

		if (tokenizer.hasMoreTokens()) {
			String[] versions = tokenizer.nextToken().split(" -")
			if (versions.length > 0) {
				version = versions[0].trim()
			}
			if (versions.length > 1) {
				buildNumber = versions[1].trim()
			}
		}

		if (tokenizer.hasMoreTokens()) {
			tokenizer.nextToken(); // is space
		}

		if (tokenizer.hasMoreTokens()) {
			identifier = tokenizer.nextToken().trim();
			shortIdentifier = identifier - "com.apple.CoreSimulator.SimRuntime."
		}

		if (tokenizer.hasMoreTokens()) {
			tokenizer.nextToken(); // is space
		}

		if (tokenizer.hasMoreTokens()) {
			available = !tokenizer.nextToken().startsWith("unavailable")
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
