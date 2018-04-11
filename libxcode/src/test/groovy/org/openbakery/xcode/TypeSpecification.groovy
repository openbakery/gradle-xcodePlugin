package org.openbakery.xcode

import spock.lang.Specification

class TypeSpecification extends Specification {

	def "typeFromString give proper type"() {
		expect:
		Type.typeFromString(value) == type

		where:
		value                                                                 | type
		"macOS"                                                               | Type.macOS
		"MacOS"                                                               | Type.macOS
		"macos"                                                               | Type.macOS
		"OSX"                                                                 | Type.macOS
		"osx"                                                                 | Type.macOS
		"Osx"                                                                 | Type.macOS
		"ios"                                                                 | Type.iOS
		"IOS"                                                                 | Type.iOS
		"iOS"                                                                 | Type.iOS
		"iOS 7.1 (7.1 - 11D167) (com.apple.CoreSimulator.SimRuntime.iOS-7-1)" | Type.iOS
		"tvOS"                                                                | Type.tvOS
		"tvos"                                                                | Type.tvOS
	}
}
