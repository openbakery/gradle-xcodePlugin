package org.openbakery

import org.openbakery.xcode.Version
import spock.lang.Specification

class VersionTest extends Specification {


	def "parse 1.0.0"() {
		when:
		Version version = new Version("1.0.0")

		then:
		version.major == 1
		version.minor == 0
		version.maintenance == 0
	}

	def "parse 1.2.0"() {
		when:
		Version version = new Version("1.2.0")

		then:
		version.major == 1
		version.minor == 2
		version.maintenance == 0
	}


	def "parse 1.2.3"() {
		when:
		Version version = new Version("1.2.3")

		then:
		version.major == 1
		version.minor == 2
		version.maintenance == 3
	}


	def "parse 1"() {
		when:
		Version version = new Version("1")

		then:
		version.major == 1
		version.minor == -1
		version.maintenance == -1
	}

	def "parse 1.2"() {
		when:
		Version version = new Version("1.2")

		then:
		version.major == 1
		version.minor == 2
		version.maintenance == -1
	}


	def "parse 0.2"() {
		when:
		Version version = new Version("0.2")

		then:
		version.major == 0
		version.minor == 2
		version.maintenance == -1
	}


	def "parse 0.0.2"() {
		when:
		Version version = new Version("0.0.2")

		then:
		version.major == 0
		version.minor == 0
		version.maintenance == 2
	}

	def "parse 2.a"() {
		when:
		Version version = new Version("2.a")

		then:
		version.major == 2
		version.minor == -1
		version.maintenance == -1
		version.suffix == "a"
	}

	def "parse 2_a"() {
		when:
		Version version = new Version("2_a")

		then:
		version.major == -1
		version.minor == -1
		version.maintenance == -1
		version.suffix == "2_a"
		version.toString() == "2_a"
	}

	def "parse 2a"() {
		when:
		Version version = new Version("2a")

		then:
		version.major == -1
		version.minor == -1
		version.maintenance == -1
		version.suffix == "2a"
		version.toString() == "2a"
	}

	def "parse 1.2.3_a and test toString"() {
		when:
		Version version = new Version("1.2.3_a")

		then:
		version.toString() == "1.2.3_a"
	}
}
