package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.testdouble.XcodeFake
import org.openbakery.tools.Lipo
import spock.lang.Specification

class LipoRemoveArchSpecification extends Specification {

	Lipo lipo
	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		lipo = new Lipo(new XcodeFake(), commandRunner)
	}

	def tearDown() {
		lipo = null
		commandRunner = null
	}

	def mockInfo(String binary, String archs) {
		def commandList = ["/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
						   "-info",
						   new File(binary).absolutePath]
		String result = "Architectures in the fat file: Dummy are: " + archs
		commandRunner.runWithResult(commandList) >> result
	}


	def "remove arch execute proper command"() {
		def commandList

		when:
		lipo.removeArch(new File("my.dylib"), "arm64e")
		then:
		1 * commandRunner.run(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			new File("my.dylib").absolutePath,
			"-remove",
			"arm64e",
			"-output",
			new File("my.dylib").absolutePath
		]
	}

	def "remove arch execute proper command for arm64"() {
		def commandList

		when:
		lipo.removeArch(new File("second.dylib"), "arm64")
		then:
		1 * commandRunner.run(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			new File("second.dylib").absolutePath,
			"-remove",
			"arm64",
			"-output",
			new File("second.dylib").absolutePath
		]
	}


	def "remove unsupported archs from binary"() {
		def commandList

		given:
		mockInfo("my.dylib", "armv7 arm64 arm64e")

		when:
		lipo.removeUnsupportedArchs(new File("my.dylib"), ["armv7","arm64"])

		then:
		1 * commandRunner.run([
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			new File("my.dylib").absolutePath,
			"-remove",
			"arm64e",
			"-output",
			new File("my.dylib").absolutePath
		])

	}

	def "remove two unsupported archs from binary"() {
		def commandList

		given:
		mockInfo("my.dylib", "armv7 arm64 arm64e")

		when:
		lipo.removeUnsupportedArchs(new File("my.dylib"), ["arm64"])

		then:
		1 * commandRunner.run([
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			new File("my.dylib").absolutePath,
			"-remove",
			"arm64e",
			"-output",
			new File("my.dylib").absolutePath
		])
		1 * commandRunner.run([
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			new File("my.dylib").absolutePath,
			"-remove",
			"armv7",
			"-output",
			new File("my.dylib").absolutePath
		])
	}

	def "remove no unsupported archs from binary"() {
		def commandList

		given:
		mockInfo("my.dylib", "armv7 arm64 arm64e")

		when:
		lipo.removeUnsupportedArchs(new File("my.dylib"), ["armv7", "arm64", "arm64e"])

		then:
		0 * commandRunner.run(_)

	}

}
