package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.codesign.CodesignParameters
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import spock.lang.Specification

class AppPackageSpecification extends Specification {

	AppPackage appPackage
	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {
		def applicationPath = new File("Dummy")
		def applicationBundle = new ApplicationBundle(applicationPath, Type.iOS, false)
		appPackage = new AppPackage(applicationBundle, applicationPath, new CodesignParameters(), commandRunner, new PlistHelper(commandRunner))
	}

	def tearDown() {
		appPackage = null
	}

	def "has archive path"() {
		expect:
		appPackage.archive instanceof File

	}
}
