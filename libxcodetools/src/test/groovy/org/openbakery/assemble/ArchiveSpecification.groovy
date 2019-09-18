package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.test.ApplicationDummy
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.PlistHelper
import spock.lang.Specification

class ArchiveSpecification extends Specification {

	Archive archive
	CommandRunner commandRunner = Mock(CommandRunner)
	ApplicationDummy applicationDummy

	File tmpDirectory


	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		def lipo = Mock(Lipo.class)
		def tools = new CommandLineTools(commandRunner, new PlistHelper(commandRunner), lipo)

		applicationDummy = new ApplicationDummy(new File(tmpDirectory, "build/sym/Release-iphoneos"))
		def applicationPath = applicationDummy.create()


		/*

		applicationDummy.createSwiftLibs()


		def applicationDestination = new File(tmpDirectory, "App")

			FileHelper fileHelper = new FileHelper(new CommandRunner())
		fileHelper.copyTo(archiveAppPath, applicationDestination)

		applicationPath = new File(applicationDestination, archiveAppPath.getName())

		def applicationBundle = new ApplicationBundle(applicationPath, Type.iOS, false)
*/
		archive = new Archive(applicationPath, "Example", tools)
	}

	def tearDown() {
		archive = null
		//applicationDummy.cleanup()
		//applicationDummy = null
		FileUtils.deleteDirectory(tmpDirectory)
	}

	def "archive instance is present"() {
		expect:
		archive != null
	}


	def "archiveDirectory"() {
		when:

		def destinationDirectory = new File(tmpDirectory, "build/archive")

		archive.create(destinationDirectory)

		File archiveDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive")

		then:
		archiveDirectory.exists()
		archiveDirectory.isDirectory()
	}

}
