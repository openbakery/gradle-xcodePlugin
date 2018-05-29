package org.openbakery.util

import org.openbakery.xcode.Version
import spock.lang.Specification

class SystemUtilTest extends Specification {
	def "Should properly resolve system version from system property"() {

		setup:
		System.setProperty("os.version", systemVersion);

		when:
		Version version = SystemUtil.getOsVersion()

		then:
		version.major == major
		version.minor == minor
		version.maintenance == maintenance

		where:
		systemVersion | major | minor | maintenance
		"0.0"         | 0     | 0     | -1
		"10.3"        | 10    | 3     | -1
		"10.0.1"      | 10    | 0     | 1
		"10.1.1"      | 10    | 1     | 1
		"10.8.1"      | 10    | 8     | 1
	}
}
