package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.codesign.CodesignParameters
import org.openbakery.testdouble.LipoFake
import org.openbakery.testdouble.XcodeFake
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import spock.lang.Specification

class AppPackageSpecification extends Specification {

	AppPackage appPackage
	CommandRunner commandRunner = Mock(CommandRunner)

	def lipo = Mock(Lipo.class)

	def setup() {

		def tools = new CommandLineTools(commandRunner, new PlistHelper(commandRunner), lipo)
		def applicationPath = new File("Dummy")
		def applicationBundle = new ApplicationBundle(applicationPath, Type.iOS, false)
		appPackage = new AppPackage(applicationBundle, applicationPath, new CodesignParameters(), tools)
	}

	def tearDown() {
		appPackage = null
	}

	def "has archive path"() {
		expect:
		appPackage.archive instanceof File
	}

	/*
	def "get app binary archs"() {
		when:
		appPackage.addSwiftSupport()

		then:
		1 * lipo.getArchs("Dummy")
	}
	*/




}
