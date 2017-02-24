package org.openbakery.codesign

import spock.lang.Specification

/**
 * Created by rene on 24.02.17.
 */
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
}
