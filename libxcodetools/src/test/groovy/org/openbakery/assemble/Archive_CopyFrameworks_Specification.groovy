package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.test.ApplicationDummy
import org.openbakery.testdouble.XcodeFake
import org.openbakery.tools.CommandLineTools
import org.openbakery.tools.Lipo
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification


class Archive_CopyFrameworks_Specification extends Specification {

	CommandRunner commandRunner = Mock(CommandRunner)
	ApplicationDummy applicationDummy

	File tmpDirectory
	File applicationPath
	File xcodePath

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		applicationDummy = new ApplicationDummy(new File(tmpDirectory, "build"), "sym/Release-iphoneos")
		applicationPath = applicationDummy.create()
		xcodePath = new File(tmpDirectory, "Xcode.app")
	}

	def cleanup() {
		applicationDummy.cleanup()
		FileUtils.deleteDirectory(tmpDirectory)
		applicationDummy = null
	}

	Archive createArchive(String buildSettings = "") {
		def lipo = new Lipo(createXcodeBuild(buildSettings))
		def tools = new CommandLineTools(commandRunner, new PlistHelper(commandRunner), lipo)

		return new Archive(applicationPath, "Example", Type.iOS, false, tools, null)
	}

	XcodeFake createXcode() {
		XcodeFake xcode = new XcodeFake()
		new File(xcodePath, "Contents/Developer/usr/bin").mkdirs()
		new File(xcodePath, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		xcode.path = xcodePath.absolutePath
		return xcode
	}

	Xcodebuild createXcodeBuild(String buildSettings = "") {
		XcodeFake xcode = createXcode()
		commandRunner.runWithResult(_, ["xcodebuild", "clean", "-showBuildSettings"]) >> buildSettings
		return new Xcodebuild(tmpDirectory, commandRunner, xcode, new XcodebuildParameters(), [])
	}

	def createSwiftLibs(Xcodebuild xcodebuild, boolean includeWatchLib = false, String platform = "iphoneos", String swiftPath = "swift") {
		def swiftLibs = [
			"libswiftCore.dylib",
			"libswiftCoreGraphics.dylib",
			"libswiftDarwin.dylib",
			"libswiftDispatch.dylib",
			"libswiftFoundation.dylib",
			"libswiftObjectiveC.dylib",
			"libswiftSecurity.dylib",
			"libswiftUIKit.dylib"
		]

		if (includeWatchLib) {
			swiftLibs.add(0, "libswiftWatchKit.dylib")
		}

		File swiftLibsDirectory = new File(xcodebuild.getToolchainDirectory(), "usr/lib/$swiftPath/$platform")
		swiftLibsDirectory.mkdirs()

		swiftLibs.each { item ->
			File lib = new File(swiftLibsDirectory, item)
			FileUtils.writeStringToFile(lib, "bar")
		}

		return swiftLibs
	}

	void mockSwiftLibs(Xcodebuild xcodebuild, File watchAppDirectory = null, String platform = "iphoneos", String swiftPath = "swift") {
		def includeWatchLibs = watchAppDirectory != null

		def swiftLibs = createSwiftLibs(xcodebuild, includeWatchLibs, platform, swiftPath)
		def libFiles = swiftLibs[0..4].collect { new File(applicationPath, "Frameworks/" + it) }

		if (includeWatchLibs) {
			def watchSwiftLibs = createSwiftLibs(xcodebuild, true, "watchos")
			libFiles += watchSwiftLibs[0..4].collect { new File(watchAppDirectory, "Frameworks/" + it) }
		}

		libFiles.forEach { FileUtils.writeStringToFile(it, "foo") }
	}


	def "copy swift framework"() {
		given:
		def archive = createArchive()
		mockSwiftLibs(archive.tools.lipo.xcodebuild)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)


		File libswiftCore = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
	}


	def "copy swift framework for Xcode11"() {
		given:
		def archive = createArchive()
		new File(archive.tools.lipo.xcodebuild.getToolchainDirectory(), "usr/lib/swift/iphoneos").mkdirs()

		mockSwiftLibs(archive.tools.lipo.xcodebuild, null, "iphoneos", "swift-5")

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)


		File libswiftCore = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
	}


	List<String> bitcodeStripCommand(File dylib, Xcodebuild xcodebuild, String platform = "iphoneos") {
		return [
			"/usr/bin/xcrun",
			"bitcode_strip",
			"${xcodebuild.toolchainDirectory}/usr/lib/swift/${platform}/${dylib.name}",
			"-r",
			"-o",
			dylib.absolutePath
		]
	}


	def "copy swift framework with bitcode enabled"() {
		given:
		def archive = createArchive()
		mockSwiftLibs(archive.tools.lipo.xcodebuild)
		File libswiftCore = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		List<String> bitcodeStrip = bitcodeStripCommand(libswiftCore, archive.tools.lipo.xcodebuild)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory, true)

		then:
		libswiftCore.exists()
		0 * commandRunner.run(bitcodeStrip)
	}

	def "copy swift framework with bitcode disabled"() {
		given:
		def archive = createArchive()
		mockSwiftLibs(archive.tools.lipo.xcodebuild)

		File libswiftCore = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		List<String> bitcodeStrip = bitcodeStripCommand(libswiftCore, archive.tools.lipo.xcodebuild)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory, false)

		then:
		1 * commandRunner.run(bitcodeStrip)
	}


	def "copy swift with non default toolchain"() {
		given:
		def buildSettings = "  TOOLCHAIN_DIR = " + xcodePath.absolutePath + "/Contents/Developer/Toolchains/Swift.xctoolchain\n"
		def archive = createArchive(buildSettings)
		mockSwiftLibs(archive.tools.lipo.xcodebuild)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File libswiftCore = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftCore.dylib")
		File supportLibswiftDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")


		then:
		libswiftCore.exists()
		supportLibswiftDirectory.list().length == 5
		supportLibswiftCore.exists()
		FileUtils.readFileToString(supportLibswiftCore).equals("bar")
	}

	void createFrameworkLib(String item) {
		File lib = new File(applicationPath, "Frameworks/" + item)
		FileUtils.writeStringToFile(lib, "foo")
	}

	def "copy framework without swift libs but has custom framework"() {
		given:
		def archive = createArchive()
		createSwiftLibs(archive.tools.lipo.xcodebuild)
		createFrameworkLib("myFramework.dylib")

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		File myFramework = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/myFramework.dylib")
		File supportLibswiftDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File supportLibswiftCore = new File(supportLibswiftDirectory, "libswiftCore.dylib")

		then:
		myFramework.exists()
		!supportLibswiftDirectory.exists()
		!supportLibswiftCore.exists()
	}


	def "copy watchkit swift framework"() {
		given:
		def buildSettings = "PLATFORM_DIR = " + xcodePath.absolutePath + "/Contents/Developer/Platforms/iPhoneOS.platform\n"
		def archive = createArchive(buildSettings)
		createSwiftLibs(archive.tools.lipo.xcodebuild)

		def watchAppDirectory = applicationDummy.createWatchApp("Example")
		mockSwiftLibs(archive.tools.lipo.xcodebuild, watchAppDirectory)

		File watchosLibswiftWatchKit = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Watch/Example.app/Frameworks/libswiftWatchKit.dylib")
		File watchosSupportLibswiftDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/SwiftSupport/watchos")
		File watchosSupportLibswiftWatchKit = new File(watchosSupportLibswiftDirectory, "libswiftWatchKit.dylib")
		List<String> watchosBitcodeStrip = bitcodeStripCommand(watchosLibswiftWatchKit, archive.tools.lipo.xcodebuild, "watchos")

		File iphoneosLibswiftWatchKit = new File(tmpDirectory, "build/archive/Example.xcarchive/Products/Applications/Example.app/Frameworks/libswiftWatchKit.dylib")
		File iphoneosSupportLibswiftDirectory = new File(tmpDirectory, "build/archive/Example.xcarchive/SwiftSupport/iphoneos")
		File iphoneosSupportLibswiftWatchKit = new File(iphoneosSupportLibswiftDirectory, "libswiftWatchKit.dylib")
		List<String> iphoneosBitcodeStrip = bitcodeStripCommand(iphoneosLibswiftWatchKit, archive.tools.lipo.xcodebuild)

		when:
		def destinationDirectory = new File(tmpDirectory, "build/archive")
		archive.create(destinationDirectory)

		def value = 1

		then:
		watchosLibswiftWatchKit.exists()
		watchosSupportLibswiftDirectory.list().length == 5
		watchosSupportLibswiftWatchKit.exists()
		FileUtils.readFileToString(watchosSupportLibswiftWatchKit).equals("bar")
		0 * commandRunner.run(watchosBitcodeStrip)

		iphoneosLibswiftWatchKit.exists()
		iphoneosSupportLibswiftDirectory.list().length == 5
		iphoneosSupportLibswiftWatchKit.exists()
		FileUtils.readFileToString(iphoneosSupportLibswiftWatchKit).equals("bar")
		1 * commandRunner.run(iphoneosBitcodeStrip)
	}

}
