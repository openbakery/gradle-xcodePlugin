package org.openbakery.carthage

class CarthageParameters {

	Boolean cache
	Boolean xcframework
	String command

	CarthageParameters merge(CarthageParameters other) {
		if (other.cache != null) {
			cache = other.cache
		}

		if (other.xcframework != null) {
			xcframework = other.xcframework
		}

		if (other.command != null) {
			command = other.command
		}

		return this
	}

	public String toString() {
		return "CarthageParameters[cache=${cache}, xcframework=${xcframework}, command=${command}]"
	}
}
