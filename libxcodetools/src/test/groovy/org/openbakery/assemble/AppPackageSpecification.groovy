package org.openbakery.assemble

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Codesign
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
	CodesignParameters codesignParameters
	ApplicationBundle applicationBundle
	def codesign = Mock(Codesign.class)

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		def archivePath = new File(tmpDirectory, "App.xcarchive")
		applicationDummy = new ApplicationDummy(archivePath)

		def archiveAppPath = applicationDummy.create()
		applicationDummy.createSwiftLibs()
		applicationDummy.createPluginBundle()
		def tools = new CommandLineTools(commandRunner, new PlistHelper(new CommandRunner()), lipo)

		def applicationDestination = new File(tmpDirectory, "App")

		FileHelper fileHelper = new FileHelper(new CommandRunner())
		fileHelper.copyTo(archiveAppPath, applicationDestination)

		applicationPath = new File(applicationDestination, archiveAppPath.getName())

		applicationBundle = new ApplicationBundle(applicationPath, Type.iOS, false, tools.plistHelper)

		codesignParameters = new CodesignParameters()

		codesign.codesignParameters >> codesignParameters

		appPackage = new AppPackage(applicationBundle, archivePath, tools, codesign)
	}

	def cleanup() {
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

	def "applicationBundle has two bundles"() {
		expect:
		applicationBundle.bundles.size() == 2
	}



	def "entitlements keychain-access-group is used for extension"() {
		given:
		def commandList
		File entitlementsFile
		XMLPropertyListConfiguration entitlements

		codesignParameters.entitlements = [
				"keychain-access-groups"  : ["\$(AppIdentifierPrefix)org.openbakery.test.Example",
											 "\$(AppIdentifierPrefix)org.openbakery.test.ExampleWidget"]
		]

		XcodeFake xcode = new XcodeFake()

		when:
		appPackage.codesign()

		then:
		2 * codesign.sign(_, "org.openbakery.test.Example")
	}



}
