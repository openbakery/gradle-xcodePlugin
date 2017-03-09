package org.openbakery.codesign

import org.openbakery.xcode.Type
import spock.lang.Specification

class CodesignParametersSpecification extends Specification {




	def "test merge missing"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		second.signingIdentity = "Me"

		when:
		first.mergeMissing(second)

		then:
		first.signingIdentity == "Me"
	}

	def "test merge identity only if not present"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		first.signingIdentity = "Foo"
		second.signingIdentity = "Me"

		when:
		first.mergeMissing(second)

		then:
		first.signingIdentity == "Foo"
	}


	def "test merge keychain if missing"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		second.keychain= new File("second")

		when:
		first.mergeMissing(second)

		then:
		first.keychain == new File("second")
	}

	def "test merge keychain only if missing"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		first.keychain = new File("first")
		second.keychain= new File("second")

		when:
		first.mergeMissing(second)

		then:
		first.keychain == new File("first")
	}


	def "test merge provisioning profiles missing"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		second.mobileProvisionFiles = [ new File("second") ]

		when:
		first.mergeMissing(second)

		then:
		first.mobileProvisionFiles == [ new File("second") ]
	}


	def "test merge provisioning profiles only if missing"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		first.mobileProvisionFiles = [ new File("first") ]
		second.mobileProvisionFiles = [ new File("second") ]

		when:
		first.mergeMissing(second)

		then:
		first.mobileProvisionFiles == [ new File("first") ]
	}

	def "test merge type missing"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		second.type = Type.watchOS

		when:
		first.mergeMissing(second)

		then:
		first.type == Type.watchOS
	}

	def "test do not merge type is already set"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		first.type = Type.tvOS

		when:
		first.mergeMissing(second)

		then:
		first.type == Type.tvOS
	}


	def "test merge entitlementsPath"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		first.entitlementsFile = new File("Test")

		when:
		first.mergeMissing(second)

		then:
		first.entitlementsFile == new File("Test")
	}

	def "test do not merge entitlementsPath if already set"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		first.entitlementsFile = new File("Test")
		second.entitlementsFile = new File("Second")

		when:
		first.mergeMissing(second)

		then:
		first.entitlementsFile == new File("Test")
	}

	def "test merge entitlements"() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		second.entitlements = [ "second" : "value" ]

		when:
		first.mergeMissing(second)

		then:
		first.entitlements == [ "second": "value"]
	}

	def "test do not merge entitlements is already set "() {
		def first = new CodesignParameters()
		def second = new CodesignParameters()
		given:
		second.entitlements = [ "second": "value" ]

		when:
		first.mergeMissing(second)

		then:
		first.entitlements == [ "second": "value"]
	}

}
