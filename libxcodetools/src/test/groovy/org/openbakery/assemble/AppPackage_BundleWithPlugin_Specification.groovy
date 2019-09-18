package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.CodesignParameters
import org.openbakery.codesign.ProvisioningProfileType
import org.openbakery.test.ApplicationDummy
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.FileHelper
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Extension
import org.openbakery.xcode.Type
import spock.lang.Specification

class AppPackage_BundleWithPlugin_Specification extends Specification {

	AppPackage appPackage
	CommandRunner commandRunner = Mock(CommandRunner)
	ApplicationDummy applicationDummy

	def lipo = Mock(Lipo.class)
	File tmpDirectory
	File applicationPath

	CodesignParameters codesignParameters

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		def archivePath = new File(tmpDirectory, "App.xcarchive")
		applicationDummy = new ApplicationDummy(archivePath)

		def archiveAppPath = applicationDummy.create(false, true)
		applicationDummy.createSwiftLibs()
		applicationDummy.createPlugin()

		def tools = new CommandLineTools(commandRunner, new PlistHelper(new CommandRunner()), lipo)


		def applicationDestination = new File(tmpDirectory, "App")

		FileHelper fileHelper = new FileHelper(new CommandRunner())
		fileHelper.copyTo(archiveAppPath, applicationDestination)

		applicationPath = new File(applicationDestination, archiveAppPath.getName())

		def applicationBundle = new ApplicationBundle(applicationPath, Type.iOS, false, tools.plistHelper)

		codesignParameters = new CodesignParameters()
		codesignParameters.mobileProvisionFiles = applicationDummy.mobileProvisionFile
		appPackage = new AppPackage(applicationBundle, archivePath, codesignParameters, tools)
	}

	def tearDown() {
		appPackage = null
		applicationDummy.cleanup()
		applicationDummy = null
		FileUtils.deleteDirectory(tmpDirectory)
	}

	def "applicationBundle has two bundles"() {
		expect:
		appPackage.applicationBundle.bundles.size() == 2
	}

	def addRemoteMirrorLib() {
		for (Bundle bundle : appPackage.applicationBundle.bundles) {
			FileUtils.writeStringToFile(new File(bundle.path, "libswiftRemoteMirror.dylib"), "dummy");
		}
	}

	def "applicationBundle has libswiftRemoteMirror.dylib"() {
		when:
		addRemoteMirrorLib()

		then:
		(new File(applicationPath, "libswiftRemoteMirror.dylib").exists())
		(new File(applicationPath, "PlugIns/ExampleTodayWidget.appex/libswiftRemoteMirror.dylib").exists())
	}

	def "prepareBundles removes libswiftRemoteMirror.dylib"() {
		given:
		addRemoteMirrorLib()

		when:
		appPackage.prepareBundles()

		then:
		!(new File(applicationPath, "libswiftRemoteMirror.dylib").exists())
		!(new File(applicationPath, "PlugIns/ExampleTodayWidget.appex/libswiftRemoteMirror.dylib").exists())
	}

	def "prepareBundles embed provisioning to main app"() {
		given:
		addRemoteMirrorLib()
		def mainProfile = applicationDummy.getMobileProvisionFile(ProvisioningProfileType.AppStore)

		when:
		appPackage.prepareBundles()
		def expectedEmbeddedProfile = new File(applicationPath, "embedded.mobileprovision")

		then:
		expectedEmbeddedProfile.exists()
		FileUtils.checksumCRC32(expectedEmbeddedProfile) == FileUtils.checksumCRC32(mainProfile)
	}


	def "prepareBundles embed provisioning to extension"() {
		given:
		addRemoteMirrorLib()
		def extensionProfile = applicationDummy.getMobileProvisionFileForExtension(Extension.today)

		when:
		appPackage.prepareBundles()

		def expectedEmbeddedProfile = new File(applicationPath, "PlugIns/ExampleTodayWidget.appex/embedded.mobileprovision")

		then:
		expectedEmbeddedProfile.exists()

		FileUtils.checksumCRC32(expectedEmbeddedProfile) == FileUtils.checksumCRC32(extensionProfile)

	}

}
