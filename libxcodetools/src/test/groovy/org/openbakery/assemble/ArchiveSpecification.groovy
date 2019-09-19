package org.openbakery.assemble

import org.openbakery.bundle.ApplicationBundle
import org.openbakery.test.ApplicationDummy
import org.openbakery.xcode.Extension
import org.openbakery.xcode.Type

class ArchiveSpecification extends Archive_BaseSpecification {

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


	def "copy multipledDsyms"() {
		when:
		applicationDummy.createDsyms()
		applicationDummy.createDsyms(Extension.today)
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		then:
		new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/ExampleTodayWidget.appex.dSYM").exists()
		new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/Example.app.dSYM").exists()
	}


	def "copy framework Dsyms"() {
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


	def "copy watchOS Dsyms"() {
		given:
		def watchApplicationDummy = new ApplicationDummy(new File(tmpDirectory, "build"), "sym/Release-watchos")
		watchApplicationDummy.create()
		File extensionDirectory = new File(watchApplicationDummy.applicationBundle, "Example-Watch.dSYM")
		extensionDirectory.mkdirs()

		archive = new Archive(applicationPath, "Example", Type.iOS, false, tools, watchApplicationDummy.applicationBundle)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File dsymFile = new File(tmpDirectory, "build/archive/Example.xcarchive/dSYMs/Example-Watch.dSYM")

		then:
		dsymFile.exists()
	}


	def "create archive returns an ApplicationBundle"() {
		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		def result = archive.create(destinationDirectory)

		then:
		result instanceof ApplicationBundle
	}

	def "create archive returns an ApplicationBundle with proper application path"() {
			when:
			def destinationDirectory = new File(tmpDirectory, "build/archive")
			ApplicationBundle applicationBundle = archive.create(destinationDirectory)

			then:
			applicationBundle.applicationPath.absolutePath.endsWith("Products/Applications/Example.app")
		}


}
