package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.test.ApplicationDummy
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Extension
import spock.lang.Specification

class ArchiveSpecification extends Specification {

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

		archive = new Archive(applicationPath, "Example", tools, null)
	}

	def tearDown() {
		archive = null
		applicationDummy.cleanup()
		applicationDummy = null
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

	def "copy OnDemandResources"() {
		when:
		applicationDummy.createOnDemandResources()

		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)


		File onDemandResourcesDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/OnDemandResources")
		File infoPlist_onDemandResourcesDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/OnDemandResources/org.openbakery.test.Example.SampleImages.assetpack/Info.plist")

		then:
		onDemandResourcesDirectory.exists()
		infoPlist_onDemandResourcesDirectory.exists()
	}

	def "copy OnDemandResources.plist"() {
		when:
		applicationDummy.createOnDemandResources()

		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File onDemandResourcesPlist = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/OnDemandResources.plist")

		then:
		onDemandResourcesPlist.exists()
	}

	def "copy Dsym"() {
		when:
		applicationDummy.createDsyms()
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File dsymFile = new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM")

		then:
		dsymFile.exists()
	}


	def copyMultipleDsyms() {
		when:
		applicationDummy.createDsyms()
		applicationDummy.createDsyms(Extension.today)
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		then:
		new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/ExampleTodayWidget.appex.dSYM").exists()
		new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM").exists()
	}


	def copyFrameworkDsyms() {
		given:
		File extensionDirectory = new File(applicationDummy.applicationBundle, "OBInjector/OBInjector.framework.dSYM")
		extensionDirectory.mkdirs()

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File dsymFile = new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/OBInjector.framework.dSYM")

		then:
		dsymFile.exists()
	}


	def copyWatchOSDsyms() {
		given:
		def watchApplicationDummy = new ApplicationDummy(new File(tmpDirectory, "build"), "sym/Release-watchos")
		watchApplicationDummy.create()
		File extensionDirectory = new File(watchApplicationDummy.applicationBundle, "Example-Watch.dSYM")
		extensionDirectory.mkdirs()

		archive = new Archive(applicationPath, "Example", tools, watchApplicationDummy.applicationBundle)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File dsymFile = new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/Example-Watch.dSYM")

		then:
		dsymFile.exists()
	}

}
