package org.openbakery.tools

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
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

	def useXcode5_1() {
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcode5_1.absolutePath + "\n"  + xcode6_0.absolutePath + "\n" + xcode6_1.absolutePath
		commandRunner.runWithResult(xcode5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 5.1.1\nBuild version 5B1008")
	}

	def useXcode7_1_1() {
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcodebuild7_1_1.absolutePath + "\n"  + xcodebuild6_1.absolutePath
		commandRunner.runWithResult(xcodebuild7_1_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 7.1.1\nBuild version 7B1005")
	}

	def "xcodebuild of Xcode 5 is used"() {
		useXcode5_1()

		when:
		xcode = new Xcode(commandRunner, '5B1008')

		then:
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
		commandRunner.runWithResult("xcode-select", "-p") >> ("/Applications/Xcode.app/Contents/Developer")

		expect:
		xcode.getAltool() == '/Applications/Xcode.app/Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool'
	}

	def "altool default xcode 7.1.1"() {
		useXcode7_1_1()

		expect:
		xcode.getAltool().contains('Xcode7.1.1.app')
		xcode.getAltool().endsWith('Contents/Applications/Application Loader.app/Contents/Frameworks/ITunesSoftwareService.framework/Support/altool')

	}
}
