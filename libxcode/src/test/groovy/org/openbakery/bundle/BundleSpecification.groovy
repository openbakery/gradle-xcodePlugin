package org.openbakery.bundle

import org.openbakery.test.ApplicationDummy
import org.openbakery.xcode.Type
import spock.lang.Specification


class BundleSpecification extends Specification {

	ApplicationDummy applicationDummy

	def setup() {
		def tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		applicationDummy = new ApplicationDummy(tmpDirectory)
	}

	def tearDown() {
		applicationDummy.cleanup()
		applicationDummy = null
	}


	def "test infoPlist for iOS App"() {
		when:
		def bundle = new Bundle("Example.app", Type.iOS)

		then:
		bundle.infoPlist == new File("Example.app/Info.plist")
	}


	def "test infoPlist for macOS App"() {
		when:
		def bundle = new Bundle("Example.app", Type.macOS)


		then:
		bundle.infoPlist == new File("Example.app/Contents/Info.plist")
	}

	def "test bundleIdentifier for iOS App"() {
		when:
		def path = applicationDummy.create()
		def bundle = new Bundle(path, Type.iOS)

		then:
		bundle.bundleIdentifier == "org.openbakery.test.Example"
	}


	def "test executable for iOS App"() {
		when:
		def path = applicationDummy.create()
		def expectedExecutable = new File(path, "ExampleExecutable")
		def bundle = new Bundle(path, Type.iOS)

		then:
		bundle.executable.absolutePath == expectedExecutable.absolutePath
	}


}
