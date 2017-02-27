package org.openbakery.xcode

import spock.lang.Specification

/**
 * Created by rene on 09.08.16.
 */
class XcodebuildParametersSpecification extends Specification {


	XcodebuildParameters first =  new XcodebuildParameters()
	XcodebuildParameters second = new XcodebuildParameters()

	def setup() {
		first.type = Type.macOS
		first.simulator = false
		first.target = "ExampleOSX"
		first.scheme = "ExampleScheme"
		first.workspace = "workspace"
		first.configuration = "configuration"
		first.additionalParameters = "additionalParameters"
		first.devices =  Devices.UNIVERSAL
		first.configuredDestinations = ["iPhone 4s"]

	}


	def "target is merged"() {
		when:
		second.target = "Test1"
		first.merge(second)

		then:
		first.target == "Test1"
	}

	def "scheme is merged"() {
		when:
		second.scheme = "MyScheme"
		first.merge(second)

		then:
		first.scheme == "MyScheme"
	}


	def "simulator is merged"() {
		when:
		second.simulator = true
		first.merge(second)

		then:
		first.simulator == true
	}



	def "simulator is merged reverse"() {
		when:
		first.simulator = true
		second.simulator = false
		first.merge(second)

		then:
		first.simulator == false
	}

	def "type is merged"() {
		when:
		second.type = Type.iOS
		first.merge(second)

		then:
		first.type == Type.iOS
	}


	def "workspace is merged"() {
		when:
		second.workspace = "MyWorkspace"
		first.merge(second)

		then:
		first.workspace == "MyWorkspace"
	}

	def "additionalParameters is merged"() {
		when:
		second.additionalParameters = "x"
		first.merge(second)

		then:
		first.additionalParameters == "x"
	}


	def "configuration is merged"() {
		when:
		second.configuration = "x"
		first.merge(second)

		then:
		first.configuration == "x"
	}

	def "arch is merged"() {
		when:
		second.arch = ["i386"]
		first.merge(second)

		then:
		first.arch == ["i386"]
	}


	def "configuredDestinations is merged"() {
		when:
		second.configuredDestinations = ["iPad 2"]
		first.merge(second)

		then:
		first.configuredDestinations.size() == 1
		first.configuredDestinations[0] == "iPad 2"
	}

	def "devices is merged"() {
		when:
		second.devices = Devices.WATCH
		first.merge(second)

		then:
		first.devices == Devices.WATCH
	}

}
