package org.openbakery.bundle

import org.openbakery.xcode.Type
import spock.lang.Specification


class ApplicationBundleSpecification extends Specification {



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

}
