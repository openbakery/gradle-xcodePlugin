package org.openbakery.tools

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.Type
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.stubs.SimulatorControlStub
import spock.lang.Specification

/**
 * Created by rene on 10.08.16.
 */
class DestinationResolverSpecification extends Specification {

	Project project
	File projectDir
	XcodeBuildPluginExtension extension;

	def setup() {
		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin
		extension = new XcodeBuildPluginExtension(project)
	}


	def "XcodebuildParameters are created with iOS destination"() {
		when:
		File projectDir =  new File("../example/OSX/ExampleOSX")
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		extension.type = Type.iOS
		extension.destination = ['iPad 2']

		def parameters = extension.getXcodebuildParameters()
		def destinations = extension.destinationResolver.getDestinations(parameters)

		then:

		destinations.size() == 1
		destinations[0].name == "iPad 2"

	}


	def "test configured devices only should add most recent runtime"() {
		when:
		extension.destinationResolver.simulatorControl = new SimulatorControlStub("simctl-list-xcode7_1.txt")
		extension.destination = ['iPad 2']
		def parameters = extension.getXcodebuildParameters()
		def destinations = extension.destinationResolver.getDestinations(parameters)

		then:
		destinations.size() == 1
		destinations[0].name == "iPad 2"
		destinations[0].os == "9.1"


	}


}
