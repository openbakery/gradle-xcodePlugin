package org.openbakery.bundle

import org.openbakery.CommandRunner
import org.openbakery.assemble.AppPackage
import org.openbakery.codesign.CodesignParameters
import org.openbakery.test.ApplicationDummy
import org.openbakery.util.PlistHelper
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

	AppPackage createAppPackage(String filePath, Type type = Type.iOS) {
		def applicationPath = new File(filePath)
		def commandRunner = new CommandRunner()
		def applicationBundle = new ApplicationBundle(applicationPath, type, false)
		return new AppPackage(applicationBundle, applicationPath, new CodesignParameters(), commandRunner, new PlistHelper(commandRunner))

	}


	def "test bundleName from path"(def bundleName, def filePath) {
		expect:
		bundleName == createAppPackage(filePath).applicationBundle.getBundleName()

		where:
		bundleName   | filePath
		"bundle.app" | "my/bundle.app"
		"1.app"      | "my/1.app"
		"my.app"     | "a/long/path/my/my.app"
		"bar"        | "foo/bar"
	}


	def "test application"() {
		when:
		def bundle = createAppPackage("Example.app")

		then:
		bundle.application instanceof Application
	}


	def "test application base path"() {
		when:
		def bundle = createAppPackage("Example.app")

		then:
		bundle.application.path == new File("Example.app")
	}


	def "test infoPlist for iOS App"() {
		when:
		def bundle = createAppPackage("Example.app")

		then:
		bundle.application.infoPlist == new File("Example.app/Info.plist")
	}


	def "test infoPlist for macOS App"() {
		when:
		def bundle = createAppPackage("Example.app", Type.macOS)


		then:
		bundle.application.infoPlist == new File("Example.app/Contents/Info.plist")
	}

	def "test bundleIdentifier for iOS App"() {
		when:
		def path = applicationDummy.create()
		def bundle = createAppPackage(path.toString())

		then:
		bundle.application.bundleIdentifier == "org.openbakery.test.Example"
	}



}
