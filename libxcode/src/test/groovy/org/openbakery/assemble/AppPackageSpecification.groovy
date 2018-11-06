package org.openbakery.assemble

import spock.lang.Specification

class AppPackageSpecification extends Specification {

	AppPackage appPackage
	//CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		appPackage = new AppPackage(new File("Dummy"))
	}

	def tearDown() {
		appPackage = null
	}

	def "has archive path"() {
		expect:
		appPackage.archive instanceof File

	}
}
