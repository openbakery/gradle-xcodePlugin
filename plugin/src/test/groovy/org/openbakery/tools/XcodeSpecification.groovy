package org.openbakery.tools

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.Version
import spock.lang.Specification

/**
 * Created by rene on 27.06.16.
 */
class XcodeSpecification extends Specification {

	Xcode xcode

	CommandRunner commandRunner = Mock(CommandRunner)

	File xcode7_1_1
	File xcode6_1
	File xcode6_0
	File xcode5_1

	def setup() {

		xcode = new Xcode(commandRunner)

		xcode7_1_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode7.1.1.app")
		xcode6_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode6-1.app")
		xcode6_0 = new File(System.getProperty("java.io.tmpdir"), "Xcode6.app")
		xcode5_1 = new File(System.getProperty("java.io.tmpdir"), "Xcode5.app")
		new File(xcode7_1_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode6_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode6_0, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode5_1, "Contents/Developer/usr/bin").mkdirs()

		new File(xcode7_1_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode6_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode6_0, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode5_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()


	}

	def cleanup() {
		FileUtils.deleteDirectory(xcode7_1_1)
		FileUtils.deleteDirectory(xcode6_1)
		FileUtils.deleteDirectory(xcode6_0)
		FileUtils.deleteDirectory(xcode5_1)
	}

	def useXcode(String version) {
		commandRunner.runWithResult(xcode5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 5.1.1\nBuild version 5B1008")
		commandRunner.runWithResult(xcode6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 6.0\nBuild version 6A000")
		commandRunner.runWithResult(xcode6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")
		commandRunner.runWithResult(xcode7_1_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 7.1.1\nBuild version 7B1005")
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcode5_1.absolutePath + "\n"  + xcode6_0.absolutePath + "\n" + xcode6_1.absolutePath + "\n" +  xcode7_1_1.absolutePath

		xcode = new Xcode(commandRunner, version)
	}


	def useDefaultXcode() {
		commandRunner.runWithResult("xcode-select", "-p") >> ("/Applications/Xcode.app/Contents/Developer")
	}

	def "xcodebuild of Xcode 5 is used"() {
		given:
		useXcode("5B1008")

		expect:
		xcode.getXcodebuild().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	def "version is not null"() {
		given:
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 7.3.1\nBuild version 7D1014")

		expect:
		xcode.getVersion() != null
		xcode.getVersion().major == 7
		xcode.getVersion().minor == 3
		xcode.getVersion().maintenance == 1
	}


	def "altool default path"() {
		given:
		useDefaultXcode()

		expect:
		xcode.getAltool() == '/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool'
	}

	def "altool with xcode 7.1.1"() {
		given:
		useXcode("7.1")

		expect:
		xcode.getAltool().contains('Xcode7.1.1.app')
		xcode.getAltool().endsWith('Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool')
	}

	def "xcrun default path"() {
		given:
		useDefaultXcode()

		expect:
		xcode.getXcrun() == '/Applications/Xcode.app/Contents/Developer/usr/bin/xcrun'
	}


	def "xcrun with xcode 7.1.1"() {
		given:
		useXcode("7.1")

		expect:
		xcode.getXcrun().endsWith('Xcode7.1.1.app/Contents/Developer/usr/bin/xcrun')
	}



	def "set xcode version"() {
		useXcode("7.1")


		expect:
		xcode.version instanceof Version
		xcode.version.major == 7
		xcode.version.minor == 1
		xcode.version.maintenance == 1
	}

	def "set xcode version with xcode 6"() {
		useXcode("6.4")

		expect:
		xcode.version instanceof Version
		xcode.version.major == 6
		xcode.version.minor == 4
		xcode.version.maintenance == -1
	}

	def "get xcode version"() {
		given:
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")

		when:
		Version version = xcode.version

		then:
		version instanceof Version
		version.major == 6
		version.minor == 4
		version.maintenance == -1
	}

}
