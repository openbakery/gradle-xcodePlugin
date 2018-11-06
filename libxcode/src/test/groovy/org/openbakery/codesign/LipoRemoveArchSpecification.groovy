package org.openbakery.codesign

import org.openbakery.CommandRunner
import org.openbakery.xcode.XcodeFake
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

	def "remove arch execute proper command"() {
		def commandList

		when:
		lipo.removeArch("my.dylib", "arm64e")
		then:
		1 * commandRunner.run(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"my.dylib",
			"-remove",
			"arm64e",
			"-output",
			"my.dylib"
		]
	}

	def "remove arch execute proper command for arm64"() {
		def commandList

		when:
		lipo.removeArch("second.dylib", "arm64")
		then:
		1 * commandRunner.run(_) >> {
			arguments ->
				commandList = arguments[0]
		}
		commandList == [
			"/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/lipo",
			"second.dylib",
			"-remove",
			"arm64",
			"-output",
			"second.dylib"
		]
	}

}
