package org.openbakery.xcode

import spock.lang.Specification

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


	def "bitcode is merged"() {
		when:
		second.bitcode = true
		first.merge(second)

		then:
		first.bitcode == true
	}

	def "codeCoverage is merged"() {
		when:
		second.codeCoverage = true
		first.merge(second)

		then:
		first.codeCoverage == true
	}

	def "applicationBundle is merged"() {
		when:
		second.applicationBundle = new File("MyApp.app")
		first.merge(second)

		then:
		first.applicationBundle == new File("MyApp.app")
	}

	def "applicationBundleName is from applicationBundle"() {
		when:
		first.applicationBundle = new File("/tmp/MyApp.app")

		then:
		first.applicationBundleName == "MyApp.app"
	}

	def "bundleName is from applicationBundle"() {
		when:
		first.applicationBundle = new File("/tmp/MyApp.app")

		then:
		first.bundleName == "MyApp"
	}

	def "outputPath for debug iOS simulator build"() {
		when:
		first.configuration = "debug"
		first.type = Type.iOS
		first.symRoot = new File("sym")
		first.simulator = true

		then:
		first.outputPath == new File("sym/debug-iphonesimulator")
	}

	def "outputPath with different configurations"(simulator, configuration, type, outputPath) {
		when:
		first.configuration = configuration
		first.type = type
		first.simulator = simulator
		first.symRoot = new File("sym")

		then:
		first.outputPath == new File(outputPath)

		where:
		simulator | configuration | type       | outputPath
		false     | "debug"       | Type.iOS   | "sym/debug-iphoneos"
		false     | "release"     | Type.iOS   | "sym/release-iphoneos"
		true      | "debug"       | Type.iOS   | "sym/debug-iphonesimulator"
		true      | "release"     | Type.iOS   | "sym/release-iphonesimulator"
		false     | "release"     | Type.macOS | "sym/release"
	}

}
