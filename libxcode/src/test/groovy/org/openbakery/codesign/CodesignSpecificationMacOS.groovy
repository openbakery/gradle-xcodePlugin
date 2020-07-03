package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.bundle.Bundle
import org.openbakery.test.ApplicationDummyMacOS
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import org.openbakery.xcode.XcodeFake
import spock.lang.Specification

import static java.nio.file.Files.*

class CodesignSpecificationMacOS extends  Specification {

	Codesign codesign
	ApplicationDummyMacOS applicationDummy
	File tmpDirectory
	CommandRunner commandRunner = Mock(CommandRunner)
	PlistHelper plistHelper
	File keychainPath
	File xcentFile
	CodesignParameters parameters

	void setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		applicationDummy = new ApplicationDummyMacOS(tmpDirectory)

		keychainPath = new File(tmpDirectory, "gradle-test.keychain")
		plistHelper = new PlistHelper(new CommandRunner())

		parameters = new CodesignParameters()
		parameters.signingIdentity = ""
		parameters.keychain = keychainPath
		parameters.type = Type.iOS

		codesign = new Codesign(
			new XcodeFake(),
			parameters,
			commandRunner,
			plistHelper)

		parameters = new CodesignParameters()
		parameters.type = Type.macOS
		codesign.codesignParameters = parameters

	}

	def cleanup() {
		applicationDummy.cleanup()
		tmpDirectory.deleteDir()
	}

	def createCodeSignCommand(File bundle) {
		return ["/usr/bin/codesign",
						"--force",
						"--sign",
						"-",
						"--options=runtime",
						"--verbose",
						bundle.absolutePath]
	}

	def createFrameworkCodeSignCommand(File bundle) {
		return ["/usr/bin/codesign",
						"--force",
						"--sign",
						"-",
						"--deep",
						"--verbose",
						bundle.absolutePath
		]
	}


	def "codesign embedded framework that has one version"() {
		def commandLists = []

		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework()

		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		2 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[1] == createCodeSignCommand(bundle)
	}


	def "codesign embedded framework that has two versions"() {
		def commandLists = []

		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework()
		applicationDummy.createFramework("B")

		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		3 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[1] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/B"))
		commandLists[2] == createCodeSignCommand(bundle)
	}


	def "codesign embedded framework that has symlink to version"() {
		def commandLists = []

		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework()

		File link = new File(bundle, "Contents/Frameworks/My.framework/Versions/Current")
		File target = new File(bundle, "Contents/Frameworks/My.framework/Versions/A")

		createSymbolicLink(link.toPath(), target.toPath())

		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		2 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[1] == createCodeSignCommand(bundle)
	}



	def "codesign embedded app"() {
		def commandLists = []

		given:
		File bundle = applicationDummy.create()
		applicationDummy.createEmbeddedApp()


		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		2 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createCodeSignCommand(new File(bundle, "Contents/Frameworks/HelperApp.app"))
		commandLists[1] == createCodeSignCommand(bundle)
	}



	def "codesign embedded framework embedded library"() {
		def commandLists = []

		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework("A", "libFoobar")


		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		3 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Libraries/libFoobar.dylib"))
		commandLists[1] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[2] == createCodeSignCommand(bundle)
	}

	def "codesign multiple embedded framework embedded library"() {
		def commandLists = []

		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework("A", "libFoo")
		applicationDummy.createFramework("A", "libBar")


		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		4 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Libraries/libFoo.dylib"))
		commandLists[1] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Libraries/libBar.dylib"))
		commandLists[2] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[3] == createCodeSignCommand(bundle)
	}


	def "codesign executable in Resources"() {
		def commandLists = []
		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework("A", null, "Resources/executable")

		def executable = new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Resources/executable")
		executable.setExecutable(true)

		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		3 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createCodeSignCommand(executable)
		commandLists[1] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[2] == createCodeSignCommand(bundle)

	}


	def "codesign executable in Foo and Bar directroy"() {
		def commandLists = []
		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework("A", null, "Foo/executable")
		applicationDummy.createFramework("A", null, "Bar/executable")

		def foo = new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Foo/executable")
		foo.setExecutable(true)
		def bar = new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Bar/executable")
		bar.setExecutable(true)

		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		4 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createCodeSignCommand(foo)
		commandLists[1] == createCodeSignCommand(bar)
		commandLists[2] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[3] == createCodeSignCommand(bundle)

	}

	def "codesign ignore non executable files in Resources"() {
		def commandLists = []
		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework("A", null, "Resources/Test.png")

		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		2 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[1] == createCodeSignCommand(bundle)

	}

	def "codesign ignores folders in Resources"() {
		def commandLists = []
		given:
		File bundle = applicationDummy.create()
		applicationDummy.createFramework("A", null, "Resources/Test.png")

		def folder = new File(bundle, "Contents/Frameworks/My.framework/Versions/A/Resources/Folder")
		folder.mkdirs()


		when:
		codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


		then:
		2 * commandRunner.run(_, _) >> {
			arguments ->
				commandLists << arguments[0]
		}

		commandLists[0] == createFrameworkCodeSignCommand(new File(bundle, "Contents/Frameworks/My.framework/Versions/A"))
		commandLists[1] == createCodeSignCommand(bundle)

	}



}
