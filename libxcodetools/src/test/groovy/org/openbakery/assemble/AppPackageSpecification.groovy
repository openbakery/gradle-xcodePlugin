package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.codesign.CodesignParameters
import org.openbakery.test.ApplicationDummy
import org.openbakery.testdouble.LipoFake
import org.openbakery.testdouble.XcodeFake
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.FileHelper
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import spock.lang.Specification

class AppPackageSpecification extends Specification {

	AppPackage appPackage
	CommandRunner commandRunner = Mock(CommandRunner)
	ApplicationDummy applicationDummy

	def lipo = Mock(Lipo.class)
	File tmpDirectory
	File applicationPath

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		def archivePath = new File(tmpDirectory, "App.xcarchive")
		applicationDummy = new ApplicationDummy(archivePath)

		def archiveAppPath = applicationDummy.create()
		applicationDummy.createSwiftLibs()
		def tools = new CommandLineTools(commandRunner, new PlistHelper(new CommandRunner()), lipo)


		def applicationDestination = new File(tmpDirectory, "App")

		FileHelper fileHelper = new FileHelper(new CommandRunner())
		fileHelper.copyTo(archiveAppPath, applicationDestination)

		applicationPath = new File(applicationDestination, archiveAppPath.getName())

		def applicationBundle = new ApplicationBundle(applicationPath, Type.iOS, false, tools.plistHelper)


		appPackage = new AppPackage(applicationBundle, archivePath, new CodesignParameters(), tools)
	}

	def tearDown() {
		appPackage = null
		applicationDummy.cleanup()
		applicationDummy = null
		FileUtils.deleteDirectory(tmpDirectory)
	}

	def "has archive path"() {
		expect:
		appPackage.archive instanceof File
	}


	def "remove archs from swift dynlib for app archs armv7 & arm64"() {
		given:
		lipo.getArchs(new File(applicationPath, "ExampleExecutable")) >> ["armv7", "arm64"]

		when:
		appPackage.addSwiftSupport()

		then:
		1 * lipo.removeUnsupportedArchs(new File(applicationPath, "Frameworks/libswiftCore.dylib"), ["armv7", "arm64", "armv7s"])
	}

	def "remove archs from swift dynlib for app arch armv7"() {
		given:
		lipo.getArchs(new File(applicationPath, "ExampleExecutable")) >> ["armv7"]

		when:
		appPackage.addSwiftSupport()

		then:
		1 * lipo.removeUnsupportedArchs(new File(applicationPath, "Frameworks/libswiftCoreGraphics.dylib"), ["armv7", "armv7s"])
	}

	def "remove archs from swift dynlib for app arch armv64"() {
		given:
		lipo.getArchs(new File(applicationPath, "ExampleExecutable")) >> ["arm64"]

		when:
		appPackage.addSwiftSupport()

		then:
		1 * lipo.removeUnsupportedArchs(new File(applicationPath, "Frameworks/libswiftCoreGraphics.dylib"), ["arm64", "armv7", "armv7s"])
	}

	def "remove archs only from dylibs"() {
		given:
		lipo.getArchs(new File(applicationPath, "ExampleExecutable")) >> ["armv7"]

		File framework = new File(applicationPath, "Frameworks/Test.framework")
		framework.mkdirs()

		when:
		appPackage.addSwiftSupport()

		then:
		0 * lipo.removeUnsupportedArchs(new File(applicationPath, "Frameworks/Test.framework"), ["armv7"])
	}

}
