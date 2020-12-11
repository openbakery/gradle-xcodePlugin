package org.openbakery.carthage

class CarthageParameters {

	Boolean cache
	Boolean xcframework

	CarthageParameters merge(CarthageParameters other) {
		if (other.cache != null) {
			cache = other.cache
		}

		if (other.xcframework != null) {
			xcframework = other.xcframework
		}

		return this
	}
}
