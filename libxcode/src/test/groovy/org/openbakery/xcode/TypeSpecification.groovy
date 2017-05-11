package org.openbakery.xcode

import spock.lang.Specification

class TypeSpecification extends Specification {


	def "typeFromString give proper type"() {
		expect:
		Type.typeFromString("macOS") == Type.macOS
		Type.typeFromString("MacOS") == Type.macOS
		Type.typeFromString("macos") == Type.macOS
		Type.typeFromString("OSX") == Type.macOS
		Type.typeFromString("osx") == Type.macOS
		Type.typeFromString("Osx") == Type.macOS
		Type.typeFromString("ios") == Type.iOS
		Type.typeFromString("IOS") == Type.iOS
		Type.typeFromString("iOS") == Type.iOS
		Type.typeFromString("tvOS") == Type.tvOS
		Type.typeFromString("tvos") == Type.tvOS
	}
}
