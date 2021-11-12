package org.openbakery.codesign

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.tools.Lipo
import org.openbakery.xcode.Xcode
import org.openbakery.xcode.Xcodebuild
import org.openbakery.xcode.XcodebuildParameters
import spock.lang.Specification

class LipoArchSpecification extends Specification {

	Lipo lipo
	CommandRunner commandRunner = Mock(CommandRunner)
	File tmpDirectory

	def setup() {
		tmpDirectory = new File(System.getProperty("java.io.tmpdir"), "gxp-test")
		def xcodebuild = new Xcodebuild(tmpDirectory, commandRunner, new XcodeFake(), new XcodebuildParameters(), [])
		commandRunner.runWithResult(_,["xcodebuild", "clean", "-showBuildSettings"]) >> ""
		lipo = new Lipo(xcodebuild)
	}

	def cleanup() {
		FileUtils.deleteDirectory(tmpDirectory)
		lipo = null
		commandRunner = null
	}

	def "lipo instance is present"() {
		expect:
		lipo != null
	}

	def "lipo has xcodebuild"() {
		expect:
		lipo.xcodebuild instanceof Xcodebuild
	}

	def "lipo has a command runner"() {
		expect:
		lipo.xcodebuild.commandRunner instanceof CommandRunner
	}

	def "get default archs from binary"() {
		given:
		commandRunner

		when:
		def archs = lipo.getArchs(new File(""))

		then:
		archs.size() == 2
		archs.contains("arm64")
		archs.contains("armv7")
		!archs.contains("arm64e")
	}

	def "get archs executes lipo"() {
		def commandList

		def file = new File("Dummy")

		when:
		lipo.getArchs(file)
		then:
		1 * commandRunner.runWithResult(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"-info",
			file.absolutePath
		]
	}


	def "get archs executes lipo with propery binary name"() {
		def commandList

		when:
		lipo.getArchs(new File("A/Binary"))
		then:
		1 * commandRunner.runWithResult(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"-info",
			new File("A/Binary").absolutePath
		]
	}

	def mockInfo(File binary, List<String> archs) {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
						   "-info",
						   binary.absolutePath]

		def isFatBinary = archs.size() > 1
		def prefix = isFatBinary ? "Architectures in the fat file: Dummy are:" : "Non-fat file: Dummy is architecture:"
		def result = prefix + " " + archs.join(" ") + " "

		commandRunner.runWithResult(commandList) >> result
	}

	def "get archs executes lipo and process the result properly"() {
		given:
		mockInfo(new File("Dummy"), ["armv7", "arm64"])

		when:
		def archs = lipo.getArchs(new File("Dummy"))

		then:
		archs.size() == 2
		archs.contains("arm64")
		archs.contains("armv7")
		!archs.contains("arm64e")

	}


	def "get archs executes lipo and process the result properly with arm64e"() {

		given:
		mockInfo(new File("Dummy"), ["armv7", "arm64", "arm64e"])

		when:
		def archs = lipo.getArchs(new File("Dummy"))

		then:
		archs.size() == 3
		archs.contains("arm64")
		archs.contains("armv7")
		archs.contains("arm64e")
	}

	def "get archs executes lipo and process the result properly with non-fat binary"() {

		given:
		mockInfo(new File("Dummy"), ["arm64"])


		when:
		def archs = lipo.getArchs(new File("Dummy"))

		then:
		archs.size() == 1
		archs.contains("arm64")
	}


	def "get lipo default path"() {
		given:

		expect:
		lipo.getLipoCommand() == '/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo'
	}

	/*
	def "get lipo with custom toolchain path"() {
		given:
		xcode = new Xcode(new CommandRunner(), "11", "/a/custom/toolchain/path")

		expect:
		xcode.getLipo() == '/a/custom/toolchain/path/usr/bin/lipo'
	}
	*/


}
