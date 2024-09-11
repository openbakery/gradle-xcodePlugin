package org.openbakery.xcode

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.simulators.SimulatorControl
import org.openbakery.testdouble.SimulatorControlFake
import spock.lang.Specification

class DestinationResolverSpecification extends Specification {

	Project project
	File projectDir
	XcodeBuildPluginExtension extension
	DestinationResolver destinationResolver
	SimulatorControl simulatorControl

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin
		extension = new XcodeBuildPluginExtension(project)
		simulatorControl = new SimulatorControlFake("simctl-list-xcode7_1.txt")
		destinationResolver = new DestinationResolver(simulatorControl)
	}



	def "available destinations for OS X"() {

		when:
		extension.type = Type.macOS

		then:
		destinationResolver.getDestinations(extension.getXcodebuildParameters()).size() == 1
	}


	def "XcodebuildParameters are created with iOS destination"() {
		when:
		Project project = ProjectBuilder.builder().build()
		extension = new XcodeBuildPluginExtension(project)
		extension.type = Type.iOS
		extension.destination = ['iPad 2']

		def parameters = extension.getXcodebuildParameters()
		def destinations = destinationResolver.getDestinations(parameters)

		then:

		destinations.size() == 1
		destinations[0].name == "iPad 2"

	}


	def "test configured devices only should add most recent runtime"() {
		when:
		extension.destination = ['iPad 2']
		def parameters = extension.getXcodebuildParameters()
		def destinations = destinationResolver.getDestinations(parameters)

		then:
		destinations.size() == 1
		destinations[0].name == "iPad 2"
		destinations[0].os == "9.1"
	}


	def "available destinations default xcode 7"() {
		when:
		destinationResolver.simulatorControl = new SimulatorControlFake("simctl-list-xcode7.txt");
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 11

	}


	def "available destinations default"() {
		when:

		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 22

	}

	def "available destinations default for 9.1 SDK"() {
		extension.destination {
			os = "9.1"
		}

		when:

		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 12

	}


	def "available destinations match"() {

		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPad Air'
			os = "9.1"
		}

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1

	}


	def "available destinations not match"() {
		given:
		extension.destination {
			platform = 'iOS Simulator'
			name = 'iPad Air'
			os = "8.0"
		}

		when:
		destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		thrown(IllegalStateException)
	}



	def "available destinations match simple single"() {
		given:
		extension.destination = 'iPad Air'

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == "iPad Air"
		destinations[0].os == "9.1"

	}

	def "available destinations match simple multiple"() {
		given:

		extension.destination = ['iPad Air', 'iPhone 4s']

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 2

	}


	def "set destinations twice"() {
		given:

		extension.destination = ['iPad Air', 'iPhone 5s']
		extension.destination = ['iPad Air', 'iPhone 4s']

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 2
		destinations[1].name == 'iPhone 4s'

	}

	def "resolves tvOS destination from the name"() {
		given:
		extension.type = Type.tvOS
		extension.destination = "Apple TV 1080p"

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == "Apple TV 1080p"
	}

	def "resolves tvOS destinations from the type"() {
		given:
		extension.type = Type.tvOS

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == "Apple TV 1080p"
	}


	def "resolve iPad Pro (12.9 inch)"() {
		given:
		simulatorControl = new SimulatorControlFake("simctl-list-xcode8.txt")
		destinationResolver = new DestinationResolver(simulatorControl)
		extension.destination = ['iPad Pro (12.9 inch)']

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == 'iPad Pro (12.9 inch)'
		destinations[0].id == 'C538D7F8-E581-44FF-9B17-5391F84642FB'
	}

	def "resolve iPad Pro (12.9 inch) works with iPad Pro (12.9-inch) (3rd generation)"() {
		given:
		simulatorControl = new SimulatorControlFake("simctl-list-xcode11.txt")
		destinationResolver = new DestinationResolver(simulatorControl)
		extension.destination = ['iPad Pro (12.9-inch)']

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == 'iPad Pro (12.9-inch) (3rd generation)'
		destinations[0].id == '49BE44FC-A802-4D79-9E7C-7D00D74061A4'
	}


	def "resolve iPhone 15"() {
		given:
		simulatorControl = new SimulatorControlFake(15)
		destinationResolver = new DestinationResolver(simulatorControl)
		extension.destination = ['iPhone 15']

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == 'iPhone 15'
		destinations[0].id == '74BBFC84-7187-4DF3-A1D1-8B04ADAA4904'
	}

	def "set destinations with os"() {
		given:

		extension.destination = '{"name":"iPad Air", "os":"9.1"}'

		when:
		def destinations = destinationResolver.getDestinations(extension.getXcodebuildParameters())

		then:
		destinations.size() == 1
		destinations[0].name == 'iPad Air'
		destinations[0].os== '9.1'

	}
}
