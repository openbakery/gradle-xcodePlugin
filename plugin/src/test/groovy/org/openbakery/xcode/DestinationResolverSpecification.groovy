package org.openbakery.xcode

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.simulators.SimulatorControl
import org.openbakery.testdouble.SimulatorControlStub
import spock.lang.Specification

/**
 * Created by rene on 10.08.16.
 */
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
		simulatorControl = new SimulatorControlStub("simctl-list-xcode7_1.txt")
		destinationResolver = new DestinationResolver(simulatorControl)
	}



	def "available destinations for OS X"() {

		when:
		extension.type = Type.OSX

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
		destinationResolver.simulatorControl = new SimulatorControlStub("simctl-list-xcode7.txt");
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

}
