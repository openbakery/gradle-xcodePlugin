package org.openbakery.simulators

import org.openbakery.xcode.Type
import org.openbakery.xcode.Version

class SimulatorRuntime {
	String name
	Version version
	String buildNumber
	String identifier
	String shortIdentifier
	boolean available = true
	Type type

	public SimulatorRuntime(String line) {

		def tokenizer = new StringTokenizer(line, "()")
		if (tokenizer.hasMoreTokens()) {
			name = tokenizer.nextToken().trim();
			type = Type.typeFromString(name)
		}

		if (tokenizer.hasMoreTokens()) {
			String[] versions = tokenizer.nextToken().split(" -")
			if (versions.length > 0) {
				version = new Version(versions[0].trim())
			}
			if (versions.length > 1) {
				buildNumber = versions[1].trim()
			}
		}

		if (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken().trim()
			if (token.length() == 0) {
				// is space, so skip
				if (tokenizer.hasMoreTokens()) {
					token = tokenizer.nextToken()
				}
			}

			if (token.startsWith("-")) {
				token = (token - "-").trim()
			}

			identifier = token.trim()
			shortIdentifier = identifier - "com.apple.CoreSimulator.SimRuntime."
		}

		if (tokenizer.hasMoreTokens()) {
			tokenizer.nextToken() // is space
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
