package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.test.ApplicationDummy
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import spock.lang.Specification

class Archive_BaseSpecification extends Specification {

	Archive archive
	CommandRunner commandRunner = Mock(CommandRunner)
	ApplicationDummy applicationDummy

	File tmpDirectory
	File applicationPath
	CommandLineTools tools

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		def lipo = Mock(Lipo.class)
		tools = new CommandLineTools(commandRunner, new PlistHelper(commandRunner), lipo)

		applicationDummy = new ApplicationDummy(new File(tmpDirectory, "build"), "sym/Release-iphoneos")
		applicationPath = applicationDummy.create()

		archive = new Archive(applicationPath, "Example", Type.iOS, false, tools, null)
	}

	def cleanup() {
		archive = null
		applicationDummy.cleanup()
		applicationDummy = null
		FileUtils.deleteDirectory(tmpDirectory)
	}
}
