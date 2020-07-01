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

		commandLists[0] == ["/usr/bin/codesign", "--force", "--sign", "-", "--deep", "--verbose", new File(bundle, "Contents/Frameworks/My.framework/Versions/A").absolutePath]
		commandLists[1] == ["/usr/bin/codesign", "--force", "--sign", "-", "--verbose", bundle.absolutePath]
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

		commandLists[0] == ["/usr/bin/codesign", "--force", "--sign", "-", "--deep", "--verbose", new File(bundle, "Contents/Frameworks/My.framework/Versions/A").absolutePath]
		commandLists[1] == ["/usr/bin/codesign", "--force", "--sign", "-", "--deep", "--verbose", new File(bundle, "Contents/Frameworks/My.framework/Versions/B").absolutePath]
		commandLists[2] == ["/usr/bin/codesign", "--force", "--sign", "-", "--verbose", bundle.absolutePath]
	}


	def "codesign embedded framework that has symlink to version"() {
			def commandLists = []

			given:
			File bundle = applicationDummy.create()
			applicationDummy.createFramework()

			File link =  new File(bundle, "Contents/Frameworks/My.framework/Versions/Current")
			File target =  new File(bundle, "Contents/Frameworks/My.framework/Versions/A")

			createSymbolicLink(link.toPath(), target.toPath())

			when:
			codesign.sign(new Bundle(bundle, Type.macOS, plistHelper))


			then:
			2 * commandRunner.run(_, _) >> {
				arguments ->
					commandLists << arguments[0]
			}

			commandLists[0] == ["/usr/bin/codesign", "--force", "--sign", "-", "--deep", "--verbose", new File(bundle, "Contents/Frameworks/My.framework/Versions/A").absolutePath]
			commandLists[1] == ["/usr/bin/codesign", "--force", "--sign", "-", "--verbose", bundle.absolutePath]
		}

}
