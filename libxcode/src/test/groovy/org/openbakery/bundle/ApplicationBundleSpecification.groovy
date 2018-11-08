package org.openbakery.bundle

import org.openbakery.test.ApplicationDummy
import org.openbakery.xcode.Type
import spock.lang.Specification

class ApplicationBundleSpecification  extends Specification {

	ApplicationDummy applicationDummy

	def setup() {
		def tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		applicationDummy = new ApplicationDummy(tmpDirectory)
	}

	def tearDown() {
		applicationDummy.cleanup()
		applicationDummy = null
	}

	ApplicationBundle createApplicationBundle(String filePath, Type type = Type.iOS) {
		def applicationPath = new File(filePath)
		return new ApplicationBundle(applicationPath, type, false)
	}


	def "test bundleName from path"(def bundleName, def filePath) {
		expect:
		bundleName == createApplicationBundle(filePath).getBundleName()

		where:
		bundleName   | filePath
		"bundle.app" | "my/bundle.app"
		"1.app"      | "my/1.app"
		"my.app"     | "a/long/path/my/my.app"
		"bar"        | "foo/bar"
	}

	def "test application base path"() {
		when:
		def bundle  = createApplicationBundle("Example.app")

		then:
		bundle.mainBundle.path == new File("Example.app")
	}



}
