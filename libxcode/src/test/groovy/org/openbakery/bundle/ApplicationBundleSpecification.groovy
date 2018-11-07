package org.openbakery.bundle

import org.openbakery.test.ApplicationDummy
import org.openbakery.xcode.Type
import spock.lang.Specification


class ApplicationBundleSpecification extends Specification {

	ApplicationDummy applicationDummy

	def setup() {
		def tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		applicationDummy = new ApplicationDummy(tmpDirectory)
	}

	def tearDown() {
		applicationDummy.cleanup()
		applicationDummy = null
	}


	def "test bundleName from path"(def bundleName, def filePath) {
		expect:
		bundleName == new ApplicationBundle(new File(filePath), Type.iOS, false).getBundleName()

		where:
		bundleName   | filePath
		"bundle.app" | "my/bundle.app"
		"1.app"      | "my/1.app"
		"my.app"     | "a/long/path/my/my.app"
		"bar"        | "foo/bar"
	}


	def "test application"() {
		when:
		def bundle = new ApplicationBundle(new File("Example.app"), Type.iOS, false)

		then:
		bundle.application instanceof Application
	}


	def "test application base path"() {
		when:
		def applicationPath = new File("Example.app")

		def bundle = new ApplicationBundle(applicationPath, Type.iOS, false)

		then:
		bundle.application.path == applicationPath
	}


	def "test infoPlist for iOS App"() {
		when:
		def applicationPath = new File("Example.app")
		def bundle = new ApplicationBundle(applicationPath, Type.iOS, false)

		then:
		bundle.application.infoPlist == new File("Example.app/Info.plist")
	}


	def "test infoPlist for macOS App"() {
		when:
		def applicationPath = new File("Example.app")
		def bundle = new ApplicationBundle(applicationPath, Type.macOS, false)

		then:
		bundle.application.infoPlist == new File("Example.app/Contents/Info.plist")
	}

	def "test bundleIdentifier for iOS App"() {
		when:
		applicationDummy.create()
		def bundle = new ApplicationBundle(applicationDummy.applicationBundle, Type.iOS, false)

		then:
		bundle.application.bundleIdentifier == "org.openbakery.test.Example"
	}



}
