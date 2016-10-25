package org.openbakery.xcode

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.OutputAppender
import spock.lang.Specification

/**
 * Created by rene on 27.06.16.
 */
class XcodebuildSpecification extends Specification {

	//Project project

	CommandRunner commandRunner = Mock(CommandRunner)

	Xcodebuild xcodebuild

	OutputAppender outputAppender = new OutputAppender() {
		public void append(String output) {
		}
	}
	XcodebuildParameters parameters
	DestinationResolver destinationResolver

	def setup() {
		parameters = new XcodebuildParameters()
		parameters.dstRoot = new File("build/dst")
		parameters.objRoot = new File("build/obj")
		parameters.symRoot = new File("build/sym")
		parameters.sharedPrecompsDir = new File("build/shared")
		parameters.derivedDataPath = new File("build/derivedData")
		parameters.configuration = "Debug"
		parameters.simulator = true
		parameters.type = Type.iOS
		parameters.destination = [
						new Destination("iPhone 4s")
		]
// iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389
		destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7_1.txt"))
		xcodebuild = new Xcodebuild(commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
	}

	def cleanup() {
	}


	def "xcodebuild has command runner"() {
		expect:
		xcodebuild.commandRunner != null
	}


	def "xcodebuild has xcode "() {
		expect:
		xcodebuild.xcode != null
	}

	def "IllegalArgumentException thrown when no scheme or target given"() {
		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		IllegalArgumentException e = thrown()
		e.message == "No 'scheme' or 'target' specified, so do not know what to build"
	}

	def createCommandWithDerivedDataPath_And_DefaultDirectories(String... commands) {
		def command = []
		command.addAll(commands)
		command << "-derivedDataPath" << new File("build/derivedData").absolutePath
		command << "DSTROOT=" + new File("build/dst").absolutePath
		command << "OBJROOT=" + new File("build/obj").absolutePath
		command << "SYMROOT=" + new File("build/sym").absolutePath
		command << "SHARED_PRECOMPS_DIR=" + new File("build/shared").absolutePath
		return command
	}

	def createCommandWithDefaultDirectories(String... commands) {
		def command = []
		command.addAll(commands)
		command << "DSTROOT=" + new File("build/dst").absolutePath
		command << "OBJROOT=" + new File("build/obj").absolutePath
		command << "SYMROOT=" + new File("build/sym").absolutePath
		command << "SHARED_PRECOMPS_DIR=" + new File("build/shared").absolutePath
		return command
	}


	def "run command with expected scheme and expected default directories"() {
		def commandList

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		commandList == createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
				"-scheme", 'myscheme',
				"-workspace", 'myworkspace',
				"-configuration", "Debug",
				"CODE_SIGN_IDENTITY=",
				"CODE_SIGNING_REQUIRED=NO"
		)

	}


	def "run command with expected scheme and expected directories"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.type = Type.iOS


		xcodebuild.parameters.derivedDataPath = new File("build/myDerivedData").absoluteFile
		xcodebuild.parameters.dstRoot = new File("build/myDst").absoluteFile
		xcodebuild.parameters.objRoot = new File("build/myObj").absoluteFile
		xcodebuild.parameters.symRoot = new File("build/mySym").absoluteFile
		xcodebuild.parameters.sharedPrecompsDir = new File("build/myShared").absoluteFile

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
								   "-scheme", 'myscheme',
								   "-workspace", 'myworkspace',
								   "-configuration", "Debug",
								   "-derivedDataPath", new File("build/myDerivedData").absolutePath,
								   "DSTROOT=" + new File("build/myDst").absolutePath,
								   "OBJROOT=" + new File("build/myObj").absolutePath,
								   "SYMROOT=" + new File("build/mySym").absolutePath,
								   "SHARED_PRECOMPS_DIR=" + new File("build/myShared").absolutePath,
									 "-destination", "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
			]
		}
		commandList == expectedCommandList

	}


	def "run command with expected target and expected defaults"() {
		def commandList
		def expectedCommandList

		def target = 'mytarget'
		xcodebuild.parameters.target = target

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDefaultDirectories('xcodebuild',
					"-configuration", "Debug",
					"-target", 'mytarget'
			)
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList

	}

	def "run command without signIdentity"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", "Debug",
					"CODE_SIGN_IDENTITY=",
					"CODE_SIGNING_REQUIRED=NO"
			)
		}
		commandList == expectedCommandList
	}


	def "run command without signIdentity osx"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.type = Type.OSX

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", "Debug",
					"CODE_SIGN_IDENTITY=",
					"CODE_SIGNING_REQUIRED=NO"
			)
			expectedCommandList << "-destination" << "platform=OS X,arch=x86_64"

		}
		commandList == expectedCommandList
	}

	def "run command with arch"() {

		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.arch = ['myarch']


		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", "Debug",
					"ARCHS=myarch")
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"

		}
		commandList == expectedCommandList
	}


	def "run command with multiple arch"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false
		xcodebuild.parameters.arch = ['armv', 'armv7s']

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", "Debug",
					"CODE_SIGN_IDENTITY=",
					"CODE_SIGNING_REQUIRED=NO",
					"ARCHS=armv armv7s")
		}
		commandList == expectedCommandList
	}


	def "run command with workspace"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", "Debug")
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"

		}
		commandList == expectedCommandList
	}


	def "run command with workspace but without scheme"() {

		def commandList
		def expectedCommandList

		xcodebuild.parameters.target = 'mytarget'
		xcodebuild.parameters.workspace = 'myworkspace'

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDefaultDirectories('xcodebuild',
					"-configuration", "Debug",
					"-target", 'mytarget')
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList

	}


	def "run command scheme and simulatorbuild"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = true

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", 'Debug')
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}


	def "run command scheme and simulatorbuild and arch"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = true
		xcodebuild.parameters.arch = ['i386'];

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", 'Debug',
					"ARCHS=i386")
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}

	def "run command xcodeversion"() {
		def commandList
		def expectedCommandList

		xcodebuild.commandRunner = commandRunner
		commandRunner.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode") >> "/Applications/Xcode.app"
		commandRunner.runWithResult("/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild", "-version") >> "Xcode 5.1.1\nBuild version 5B1008"

		xcodebuild.parameters.target = 'mytarget'

		when:
		xcodebuild.xcode = new Xcode(commandRunner, "5B1008")

		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDefaultDirectories('/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild',
					"-configuration", 'Debug',
					"-target", 'mytarget')
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}


	def "xcodebuild fails"() {

		given:
		xcodebuild.parameters.target = "Test"
		commandRunner.run(_, _, _, _) >> {
			throw new CommandRunnerException()
		}

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		thrown(CommandRunnerException)

	}


	def "run command with additional parameters"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = true
		xcodebuild.parameters.additionalParameters = 'foobar';

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", 'Debug',
					"foobar")
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}

	def "run command with additional parameters array"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = true
		xcodebuild.parameters.additionalParameters = ['foo', 'bar']

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
					"-scheme", 'myscheme',
					"-workspace", 'myworkspace',
					"-configuration", 'Debug',
					"foo", "bar")
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}


	def "set execution directory"() {
		def directory
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute("foobar", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> directory = arguments[0] }
		directory == "foobar"
	}

	def "directory must not be null"() {
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute(null, outputAppender, null)

		then:
		IllegalArgumentException e = thrown()
		e.message == "directory must not be null"
	}


	def "output appender must not be null"() {
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute("", null, null)

		then:
		IllegalArgumentException e = thrown()
		e.message == "outputAppender must not be null"
	}


	def "output appender is set"() {
		def givenOutputAppender
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> givenOutputAppender = arguments[3] }
		givenOutputAppender == outputAppender
	}

	def "test command for OS X"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.type = 'OSX'
		xcodebuild.parameters.target = 'Test';

		when:
		xcodebuild.executeTest("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList = createCommandWithDefaultDirectories('script', '-q', '/dev/null',
					"xcodebuild",
					"-configuration", 'Debug',
					"-sdk", "macosx",
					"-target", 'Test',
					"CODE_SIGN_IDENTITY=",
					"CODE_SIGNING_REQUIRED=NO",
					"-destination", "platform=OS X,arch=x86_64")
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "test"
		}
		commandList == expectedCommandList
	}


	def "test command for iOS simulator"() {
		def commandList
		def expectedCommandList

		parameters.destination = [
						new Destination("iPad 2"),
						new Destination("iPhone 4s")
		]

		xcodebuild = new Xcodebuild(commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		//xcodebuild.parameters.configuredDestinations = extension.getDestinations()

		when:
		xcodebuild.executeTest("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('script', '-q', '/dev/null',
					"xcodebuild",
					"-scheme", 'myscheme',
					"-workspace", "myworkspace",
					"-configuration", 'Debug',
					"-destination", "platform=iOS Simulator,id=D72F7CC6-8426-4E0A-A234-34747B1F30DD",
					"-destination", "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389")
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "test"
		}

		commandList == expectedCommandList
	}

	def "test command for tvOS simulator"() {
		def commandList
		def expectedCommandList


		given:
		parameters.type = Type.tvOS
		parameters.destination = "Apple TV 1080p"
		parameters.target = 'Test';
		parameters.scheme = 'myscheme'
		parameters.workspace = 'myworkspace'
		xcodebuild = new Xcodebuild(commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.executeTest("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('script', '-q', '/dev/null',
					"xcodebuild",
					"-scheme", 'myscheme',
					"-workspace", "myworkspace",
					"-configuration", 'Debug',
					"-destination", "platform=tvOS Simulator,id=4395107C-169C-43D7-A403-C9030B6A205D")
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "test"
		}

		commandList == expectedCommandList
	}

	def "build command for tvOS simulator"() {
		def commandList
		def expectedCommandList


		given:
		parameters.type = Type.tvOS
		parameters.destination = "Apple TV 1080p"
		parameters.target = 'Test';
		parameters.scheme = 'myscheme'
		parameters.workspace = 'myworkspace'
		xcodebuild = new Xcodebuild(commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories(
					"xcodebuild",
					"-scheme", 'myscheme',
					"-workspace", "myworkspace",
					"-configuration", 'Debug')
			expectedCommandList << "-destination" << "platform=tvOS Simulator,id=4395107C-169C-43D7-A403-C9030B6A205D"
		}

		commandList == expectedCommandList
	}


	def "test command for iOS device"() {
		def commandList
		def expectedCommandList

		def destination = new Destination()
		destination.id = '83384347-6976-4E70-A54F-1CFECD1E02B1'

		parameters.destination  = [ destination ]
		parameters.simulator = false

		xcodebuild = new Xcodebuild(commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		//xcodebuild.parameters.configuredDestinations = extension.getDestinations()
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.executeTest("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('script', '-q', '/dev/null',
					"xcodebuild",
					"-scheme", 'myscheme',
					"-workspace", "myworkspace",
					"-configuration", 'Debug',
					"CODE_SIGN_IDENTITY=",
					"CODE_SIGNING_REQUIRED=NO",
					"-destination", "id=83384347-6976-4E70-A54F-1CFECD1E02B1")
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "test"
		}

		commandList == expectedCommandList
	}


	def "run command with target that has no scheme, and must not include derivedDataPath"() {

		def commandList
		def expectedCommandList

		xcodebuild.parameters.target = 'mytarget'
		xcodebuild.parameters.workspace = 'myworkspace'

		when:
		xcodebuild.execute("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDefaultDirectories('xcodebuild',
					"-configuration", "Debug",
					"-target", 'mytarget')
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList

	}


	def "run archive command with expected scheme and expected directories"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.type = Type.iOS


		xcodebuild.parameters.derivedDataPath = new File("build/myDerivedData").absoluteFile
		xcodebuild.parameters.dstRoot = new File("build/myDst").absoluteFile
		xcodebuild.parameters.objRoot = new File("build/myObj").absoluteFile
		xcodebuild.parameters.symRoot = new File("build/mySym").absoluteFile
		xcodebuild.parameters.sharedPrecompsDir = new File("build/myShared").absoluteFile

		when:
		xcodebuild.executeArchive("", outputAppender, null, "build/archive/myArchive.xcarchive")

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
														 "-derivedDataPath", new File("build/myDerivedData").absolutePath,
														 "DSTROOT=" + new File("build/myDst").absolutePath,
														 "OBJROOT=" + new File("build/myObj").absolutePath,
														 "SYMROOT=" + new File("build/mySym").absolutePath,
														 "SHARED_PRECOMPS_DIR=" + new File("build/myShared").absolutePath,
														 "-destination", "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389",
														 "archive",
														 "-archivePath",
														 "build/archive/myArchive.xcarchive"

			]
		}
		commandList == expectedCommandList

	}


	def "get default toolchain directory"() {
		given:
		commandRunner.runWithResult(["xcodebuild", "clean", "-showBuildSettings"]) >> "foo=bar"

		expect:
		xcodebuild.getToolchainDirectory() == "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain"
	}


	def "get default toolchain directory from build settings"() {
		given:
		File buildSettings = new File("src/test/Resource/xcodebuild-showBuildSettings.txt");
		commandRunner.runWithResult(["xcodebuild", "clean", "-showBuildSettings"]) >> FileUtils.readFileToString(buildSettings)

		expect:
		xcodebuild.getToolchainDirectory() == "/Applications/Xcode.app/Contents/Developer/Toolchains/Swift_2.3.xctoolchain"
	}


	def "get build settings with empty data should not crash"() {
		given:
		commandRunner.runWithResult(["xcodebuild", "clean", "-showBuildSettings"]) >> ""

		expect:
		xcodebuild.getToolchainDirectory() == "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain"
	}


	def "showBuildSettings should include the workspace if present"() {
		def expectedCommandList
		def commandList
		when:
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'

		xcodebuild.getToolchainDirectory()

		then:
		1 * commandRunner.runWithResult(_) >> { arguments -> commandList = arguments[0] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 'clean',
														 '-showBuildSettings',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace']
		}
		commandList == expectedCommandList


	}


	def "run build-for-testing"() {
		def commandList
		def expectedCommandList

		def destination = new Destination()
		destination.id = '83384347-6976-4E70-A54F-1CFECD1E02B1'

		parameters.destination = [destination]
		parameters.simulator = false

		xcodebuild = new Xcodebuild(commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.executeBuildForTesting("", outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('script', '-q', '/dev/null',
							"xcodebuild",
							"-scheme", 'myscheme',
							"-workspace", "myworkspace",
							"-configuration", 'Debug',
							"CODE_SIGN_IDENTITY=",
							"CODE_SIGNING_REQUIRED=NO",
							"-destination", "id=83384347-6976-4E70-A54F-1CFECD1E02B1")
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "build-for-testing"
		}

		commandList == expectedCommandList
	}

}
