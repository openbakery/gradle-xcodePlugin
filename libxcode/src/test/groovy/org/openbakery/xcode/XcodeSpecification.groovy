package org.openbakery.xcode

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import spock.lang.Specification
import spock.lang.Unroll

class XcodeSpecification extends Specification {

	Xcode xcode

	CommandRunner commandRunner = Mock(CommandRunner)

	static File xcode11 = new File(File.createTempDir(), "Xcode11.app")
	static File xcode7_1_1 = new File(File.createTempDir(), "Xcode7.1.1.app")
	static File xcode6_1 = new File(File.createTempDir(), "Xcode6-1.app")
	static File xcode6_0 = new File(File.createTempDir(), "Xcode6.app")
	static File xcode5_1 = new File(File.createTempDir(), "Xcode5.app")

	def setup() {
		xcode = Spy(Xcode, constructorArgs: [commandRunner])

		new File(xcode11, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode7_1_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode6_1, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode6_0, "Contents/Developer/usr/bin").mkdirs()
		new File(xcode5_1, "Contents/Developer/usr/bin").mkdirs()

		new File(xcode11, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode7_1_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode6_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode6_0, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
		new File(xcode5_1, "Contents/Developer/usr/bin/xcodebuild").createNewFile()
	}

	def cleanup() {
		FileUtils.deleteDirectory(xcode11)
		FileUtils.deleteDirectory(xcode7_1_1)
		FileUtils.deleteDirectory(xcode6_1)
		FileUtils.deleteDirectory(xcode6_0)
		FileUtils.deleteDirectory(xcode5_1)
	}

	def "test default xcode path"() {
		given:
		useDefaultXcode()

		expect:
		xcode.getPath().equals("/Applications/Xcode.app")
	}

	def useXcode(String version) {
		mockInstalledXcodeVersions()

		xcode = new Xcode(commandRunner, version)
	}

	def mockInstalledXcodeVersions() {
		commandRunner.runWithResult(xcode5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 5.1.1\nBuild version 5B1008")
		commandRunner.runWithResult(xcode6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 6.0\nBuild version 6A000")
		commandRunner.runWithResult(xcode6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 6.4\nBuild version 6E35b")
		commandRunner.runWithResult(xcode7_1_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 7.1.1\nBuild version 7B1005")
		commandRunner.runWithResult(xcode11.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> ("Xcode 11.0\nBuild version 11M392r")


		def pathResult = [
			xcode5_1.absolutePath,
			xcode6_0.absolutePath,
			xcode6_1.absolutePath,
			xcode7_1_1.absolutePath,
			xcode11.absolutePath,
		].join("\n")

		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> pathResult
	}

	def useDefaultXcode() {
		commandRunner.runWithResult("xcode-select", "-p") >> ("/Applications/Xcode.app/Contents/Developer")
		commandRunner.runWithResult("xcodebuild", "-version") >> ("Xcode 10\nBuild version 10B1008")


	}

	@Unroll
	def "xcodebuild of Xcode 5 is used"() {
		given:
		useXcode("5B1008")

		expect:
		xcode.getXcodebuild()
			.endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")

		where:
		version  | _
		"5B1008" | _
		"5.1"    | _
		"5.1.1"  | _
	}

	def "xcodeVersion Simple not found"() {
		when:
		useXcode("5.1.2")

		then:
		thrown(IllegalStateException)
	}

	def "xcodeVersion select last"() {
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> xcode6_1.absolutePath + "\n" + xcode6_0.absolutePath + "\n" + xcode5_1.absolutePath
		commandRunner.runWithResult(xcode6_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcode6_0.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 6.0\nBuild version 6A000"
		commandRunner.runWithResult(xcode5_1.absolutePath + "/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		when:
		xcode = new Xcode(commandRunner, '5B1008')

		then:
		xcode.getXcodebuild().endsWith("Xcode5.app/Contents/Developer/usr/bin/xcodebuild")
	}


	def "xcodeVersion select not found"() {
		useXcode("5B1008")

		when:
		xcode = new Xcode(commandRunner, '5B1009')

		then:
		thrown(IllegalStateException)
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

	def "xcode 11 version"() {
		given:

		useXcode("11")

		expect:
		xcode.getVersion() != null
		xcode.getVersion().major == 11
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

	def "altool with xcode 11"() {
		given:
		useXcode("11")

		expect:
		xcode.version.major == 11
		xcode.getAltool().contains('Xcode11.app')
		xcode.getAltool().endsWith('Contents/Developer/usr/bin/altool')
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
		setup:
		useXcode(version)

		expect:
		xcode.version instanceof Version
		xcode.version.major == major
		xcode.version.minor == minor
		xcode.version.maintenance == maintenance

		where:
		version | major | minor | maintenance
		"5.1.1" | 5     | 1     | 1
		"6.0"   | 6     | 0     | -1
		"6.4"   | 6     | 4     | -1
		"7.1.1" | 7     | 1     | 1
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


	def "simctl default path"() {
		given:
		useDefaultXcode()

		expect:
		xcode.getSimctl() == '/Applications/Xcode.app/Contents/Developer/usr/bin/simctl'
	}

	def "Should be able to resolve a Xcode version by string without exception when valid"() {

		when:
		mockInstalledXcodeVersions()
		xcode.setVersionFromString(versionString)

		then:
		1 * xcode.selectXcode(new File(file, Xcode.XCODE_CONTENT_XCODE_BUILD))
		xcode.version.toString() == (versionString + "." + buildVersion)
		noExceptionThrown()

		where:
		versionString | buildVersion | file
		"5.1.1"       | "5B1008"     | xcode5_1
		"6.0"         | "6A000"      | xcode6_0
		"7.1.1"       | "7B1005"     | xcode7_1_1
	}

	def "Should raise an exception when resolving a invalid Xcode instance by version string"() {
		when:
		mockInstalledXcodeVersions()
		xcode.setVersionFromString(versionString)

		then:
		thrown(exception)
		0 * xcode.selectXcode(_)

		where:
		versionString | exception
		"5.1.3"       | IllegalStateException
		"10.0"        | IllegalStateException
		"7.1.3"       | IllegalStateException
		null          | IllegalArgumentException
	}

	def "Should return a map of environment values containing the developer dir key for valid xcode version"() {
		when:
		mockInstalledXcodeVersions()
		Map<String, String> envValues = xcode.getXcodeSelectEnvironmentValue(versionString)

		then:
		noExceptionThrown()
		envValues.get(Xcode.DEVELOPER_DIR) == new File(file, Xcode.XCODE_CONTENT_DEVELOPER).absolutePath

		where:
		versionString | file
		"5.1.1"       | xcode5_1
		"6.0"         | xcode6_0
		"7.1.1"       | xcode7_1_1
	}

	def "get toolchain path"() {
		given:
		useDefaultXcode()

		expect:
		xcode.getToolchainDirectory() == '/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain'
	}


	def "get build version returns proper version"() {
		given:
		commandRunner.runWithResult("xcodebuild", "-version") >> versionString

		when:
		def parsedBuildVersion = xcode.getBuildVersion()

		then:
		parsedBuildVersion == buildVersion

		where:
		versionString | buildVersion
		"Xcode 10\nBuild version 10B1008"       | "10B1008"
		"Xcode 12.0\nBuild version 12A7209"     | "12A7209"
	}


}
