package org.openbakery.xcode

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.output.OutputAppender
import spock.lang.Specification

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
		xcodebuild = new Xcodebuild(new File("buildDirectory"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
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
		xcodebuild.execute(outputAppender, null)

		then:
		IllegalArgumentException e = thrown()
		e.message == "No 'scheme' or 'target' specified, so do not know what to build"
	}

	def createCommandWithDisabledCodesign(String... commands) {
		def command = []
		command.addAll(commands)
		addDisabledCodesigningParameters(command)
		return command
	}

	def createCommandWithDerivedDataPath_And_DefaultDirectories(String... commands) {
		def command = []
		command.addAll(commands)
		addDisabledCodesigningParameters(command)
		addDerivedDataPathParameters(command)
		addDefaultDirectoriesParameters(command)
		return command
	}

	def createCommandWithDefaultDirectories(String... commands) {
		def command = []
		command.addAll(commands)
		addDisabledCodesigningParameters(command)
		addDefaultDirectoriesParameters(command)
		return command
	}

	def addDerivedDataPathParameters(def command) {
		command << "-derivedDataPath" << new File("build/derivedData").absolutePath

	}
	def addDefaultDirectoriesParameters(def command) {
		command << "DSTROOT=" + new File("build/dst").absolutePath
		command << "OBJROOT=" + new File("build/obj").absolutePath
		command << "SYMROOT=" + new File("build/sym").absolutePath
		command << "SHARED_PRECOMPS_DIR=" + new File("build/shared").absolutePath
	}

	def addDisabledCodesigningParameters(def command) {
		command << "CODE_SIGN_IDENTITY="
		command << "CODE_SIGNING_REQUIRED=NO"
		//command << "CODE_SIGN_ENTITLEMENTS=" // disable for no, should be only done by build for test
		//command << "CODE_SIGNING_ALLOWED=NO"
	}



	def "run command with expected scheme and expected default directories"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug",
			)
		}
		commandList == expectedCommandList

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
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = ['xcodebuild',
														 "-scheme", 'myscheme',
														 "-workspace", 'myworkspace',
														 "-configuration", "Debug",
														 "CODE_SIGN_IDENTITY=",
														 "CODE_SIGNING_REQUIRED=NO",
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
		xcodebuild.execute(outputAppender, null)

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
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug"
			)
		}
		commandList == expectedCommandList
	}


	def "run command without signIdentity osx"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.type = Type.macOS

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug"
			)
			expectedCommandList << "-destination" << "platform=OS X,arch=x86_64"

		}
		commandList == expectedCommandList
	}

	def "no codesign for iOS simualtor build"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.simulator = true

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug"
			)
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"

		}
		commandList == expectedCommandList
	}

	def "run command with arch"() {

		def commandList
		def expectedCommandList = []

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.arch = ['myarch']


		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList << 'xcodebuild'
			expectedCommandList <<"-scheme" << 'myscheme'
			expectedCommandList <<"-workspace"<< 'myworkspace'
			expectedCommandList <<"-configuration" << "Debug"
			addDisabledCodesigningParameters(expectedCommandList)
			expectedCommandList <<"ARCHS=myarch"
			expectedCommandList << "-derivedDataPath" << new File("build/derivedData").absolutePath
			addDefaultDirectoriesParameters(expectedCommandList)
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}


	def "run command with multiple arch"() {
		def commandList
		def expectedCommandList = []

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false
		xcodebuild.parameters.arch = ['armv', 'armv7s']

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList << 'xcodebuild'
			expectedCommandList <<"-scheme" << 'myscheme'
			expectedCommandList <<"-workspace"<< 'myworkspace'
			expectedCommandList <<"-configuration" << "Debug"
			addDisabledCodesigningParameters(expectedCommandList)
			expectedCommandList << "ARCHS=armv armv7s"
			expectedCommandList << "-derivedDataPath" << new File("build/derivedData").absolutePath
			addDefaultDirectoriesParameters(expectedCommandList)
		}
		commandList == expectedCommandList
	}


	def "run command with workspace"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug"
			)
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
		xcodebuild.execute(outputAppender, null)

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



	def "run command scheme and simulatorbuild"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = true

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", 'Debug'
			)
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}


	def "run command scheme and simulatorbuild and arch"() {
		def commandList
		def expectedCommandList = []

		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = true
		xcodebuild.parameters.arch = ['i386'];

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {

			expectedCommandList << 'xcodebuild'
			expectedCommandList <<"-scheme" << 'myscheme'
			expectedCommandList <<"-workspace"<< 'myworkspace'
			expectedCommandList <<"-configuration" << "Debug"
			addDisabledCodesigningParameters(expectedCommandList)
			expectedCommandList << "ARCHS=i386"
			expectedCommandList << "-derivedDataPath" << new File("build/derivedData").absolutePath
			addDefaultDirectoriesParameters(expectedCommandList)
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

		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDefaultDirectories('/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild',
							"-configuration", 'Debug',
							"-target", 'mytarget'
			)
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
		xcodebuild.execute(outputAppender, null)

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
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", 'Debug')
			expectedCommandList << "foobar"
			addDerivedDataPathParameters(expectedCommandList)
			addDefaultDirectoriesParameters(expectedCommandList)
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
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", 'Debug')
			expectedCommandList << "foo" << "bar"
			addDerivedDataPathParameters(expectedCommandList)
			addDefaultDirectoriesParameters(expectedCommandList)
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
		}
		commandList == expectedCommandList
	}


	def "set execution directory"() {
		def directory
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> directory = arguments[0] }
		directory.endsWith("foobar")
	}

	def "directory must not be null"() {
		xcodebuild = new Xcodebuild(null, commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		IllegalArgumentException e = thrown()
		e.message == "projectDirectory must not be null"
	}


	def "output appender must not be null"() {
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute(null, null)

		then:
		IllegalArgumentException e = thrown()
		e.message == "outputAppender must not be null"
	}


	def "output appender is set"() {
		def givenOutputAppender
		xcodebuild.parameters.scheme = 'myscheme'

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> givenOutputAppender = arguments[3] }
		givenOutputAppender == outputAppender
	}

	def "test command for OS X"() {
		def commandList
		def expectedCommandList

		xcodebuild.parameters.type = 'macOS'
		xcodebuild.parameters.target = 'Test';

		when:
		xcodebuild.executeTest(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('script', '-q', '/dev/null',
							"xcodebuild",
							"-configuration", 'Debug',
							"-sdk", "macosx",
							"-target", 'Test'
			)
			expectedCommandList << "-destination" << "platform=OS X,arch=x86_64"
			addDefaultDirectoriesParameters(expectedCommandList)
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

		xcodebuild = new Xcodebuild(new File("."), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		//xcodebuild.parameters.configuredDestinations = extension.getDestinations()

		when:
		xcodebuild.executeTest(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('script', '-q', '/dev/null',
							"xcodebuild",
							"-scheme", 'myscheme',
							"-workspace", "myworkspace",
							"-configuration", 'Debug')
			expectedCommandList <<"-destination" << "platform=iOS Simulator,id=D72F7CC6-8426-4E0A-A234-34747B1F30DD"
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
			addDerivedDataPathParameters(expectedCommandList)
			addDefaultDirectoriesParameters(expectedCommandList)
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
		xcodebuild = new Xcodebuild(new File("."), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.executeTest(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('script', '-q', '/dev/null',
							"xcodebuild",
							"-scheme", 'myscheme',
							"-workspace", "myworkspace",
							"-configuration", 'Debug')
			expectedCommandList << "-destination" << "platform=tvOS Simulator,id=4395107C-169C-43D7-A403-C9030B6A205D"
			addDerivedDataPathParameters(expectedCommandList)
			addDefaultDirectoriesParameters(expectedCommandList)
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
		xcodebuild = new Xcodebuild(new File("."), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDerivedDataPath_And_DefaultDirectories(
							"xcodebuild",
							"-scheme", 'myscheme',
							"-workspace", "myworkspace",
							"-configuration", 'Debug'
			)
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

		xcodebuild = new Xcodebuild(new File("."), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		//xcodebuild.parameters.configuredDestinations = extension.getDestinations()
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.executeTest(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('script', '-q', '/dev/null',
					"xcodebuild",
					"-scheme", 'myscheme',
					"-workspace", "myworkspace",
					"-configuration", 'Debug')

			expectedCommandList << "-destination" << "id=83384347-6976-4E70-A54F-1CFECD1E02B1"
			addDerivedDataPathParameters(expectedCommandList)
			addDefaultDirectoriesParameters(expectedCommandList)
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
		xcodebuild.execute(outputAppender, null)

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
		xcodebuild.executeArchive(outputAppender, null, "build/archive/myArchive.xcarchive")

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug"
			)
			expectedCommandList << "-derivedDataPath" << new File("build/myDerivedData").absolutePath
			expectedCommandList << "DSTROOT=" + new File("build/myDst").absolutePath
			expectedCommandList << "OBJROOT=" + new File("build/myObj").absolutePath
			expectedCommandList << "SYMROOT=" + new File("build/mySym").absolutePath
			expectedCommandList << "SHARED_PRECOMPS_DIR=" + new File("build/myShared").absolutePath
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=8C8C43D3-B53F-4091-8D7C-6A4B38051389"
			expectedCommandList << "archive"
			expectedCommandList << "-archivePath"
			expectedCommandList << "build/archive/myArchive.xcarchive"

		}
		commandList == expectedCommandList

	}


	def "get default toolchain directory"() {
		given:
		commandRunner.runWithResult(_,["xcodebuild", "clean", "-showBuildSettings"]) >> "foo=bar"

		expect:
		xcodebuild.getToolchainDirectory() == "/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain"
	}


	def "get default toolchain directory from build settings"() {
		given:
		File buildSettings = new File("src/test/Resource/xcodebuild-showBuildSettings.txt");
		commandRunner.runWithResult(_,["xcodebuild", "clean", "-showBuildSettings"]) >> FileUtils.readFileToString(buildSettings)

		expect:
		xcodebuild.getToolchainDirectory() == "/Applications/Xcode.app/Contents/Developer/Toolchains/Swift_2.3.xctoolchain"
	}


	def "get build settings with empty data should not crash"() {
		given:
		commandRunner.runWithResult(_,["xcodebuild", "clean", "-showBuildSettings"]) >> ""

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
		1 * commandRunner.runWithResult(_, _) >> { arguments -> commandList = arguments[1] }
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

		xcodebuild = new Xcodebuild(new File("."), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.executeBuildForTesting(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }


		interaction {
			expectedCommandList = createCommandWithDisabledCodesign('script', '-q', '/dev/null',
							"xcodebuild",
							"-scheme", 'myscheme',
							"-workspace", "myworkspace",
							"-configuration", 'Debug')

			expectedCommandList << "-destination" << "id=83384347-6976-4E70-A54F-1CFECD1E02B1"
			addDerivedDataPathParameters(expectedCommandList)
			addDefaultDirectoriesParameters(expectedCommandList)
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "build-for-testing"
		}

		commandList == expectedCommandList
	}


	def "run testing-without-build for device"() {
		given:
		def commandList
		def expectedCommandList = []

		def destination = new Destination()
		destination.id = '83384347-6976-4E70-A54F-1CFECD1E02B1'
		xcodebuild.parameters.destination = [destination]
		xcodebuild.parameters.simulator = false

		xcodebuild = new Xcodebuild(new File("."), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
		xcodebuild.parameters.xctestrun = [
		        new File("example.xctestrun")
		]


		when:
		xcodebuild.executeTestWithoutBuilding(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }

		interaction {
			expectedCommandList << 'script' << '-q' << '/dev/null'
			expectedCommandList <<  "xcodebuild"
			expectedCommandList << "-destination" << "id=83384347-6976-4E70-A54F-1CFECD1E02B1"
			expectedCommandList << "-derivedDataPath" << new File("build/derivedData").absolutePath
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "-xctestrun" << new File("example.xctestrun").absolutePath
			expectedCommandList << "test-without-building"
		}

		commandList == expectedCommandList

	}



	def "run command in directory"() {
		def commandList
		def directory

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.simulator = false

		when:
		xcodebuild.execute(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments ->
			directory = arguments[0]
			commandList = arguments[1]
		}

		directory.endsWith("buildDirectory")

		commandList == createCommandWithDerivedDataPath_And_DefaultDirectories('xcodebuild',
				"-scheme", 'myscheme',
				"-workspace", 'myworkspace',
				"-configuration", "Debug"
		)

	}

	def "set execution directory for archive"() {
		def directory
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.executeArchive(outputAppender, null, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> directory = arguments[0] }
		directory.endsWith("foobar")
	}

	def "set execution directory for executeBuildForTesting"() {
		def directory
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.executeBuildForTesting(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> directory = arguments[0] }
		directory.endsWith("foobar")
	}

	def "set execution directory for executeTest"() {
		def directory
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.executeTest(outputAppender, null)

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> directory = arguments[0] }
		directory.endsWith("foobar")
	}


	def "set execution directory for showBuildSettings"() {
		def directory
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))

		when:
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'

		xcodebuild.getToolchainDirectory()

		then:
		1 * commandRunner.runWithResult(_, _) >> { arguments -> directory = arguments[0] }
		directory.endsWith("foobar")
	}


	def "run xcodebuild with bitcode enabled"() {
		def commandList
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'

		when:
		xcodebuild.parameters.bitcode = true
		xcodebuild.execute(outputAppender, null)


		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		commandList.contains('OTHER_CFLAGS="-fembed-bitcode"')
		commandList.contains('BITCODE_GENERATION_MODE=bitcode')
	}


	def "run xcodebuild with bitcode disabled"() {
		def commandList
		xcodebuild = new Xcodebuild(new File("foobar"), commandRunner, new XcodeFake(), parameters, destinationResolver.getDestinations(parameters))
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'

		when:
		xcodebuild.parameters.bitcode = false
		xcodebuild.execute(outputAppender, null)


		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		!commandList.contains('OTHER_CFLAGS="-fembed-bitcode"')
		!commandList.contains('BITCODE_GENERATION_MODE=bitcode')
	}

}
