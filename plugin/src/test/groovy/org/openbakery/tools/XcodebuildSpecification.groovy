package org.openbakery.tools

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.Destination
import org.openbakery.Devices
import org.openbakery.Type
import org.openbakery.XcodeBuildPluginExtension
import org.openbakery.output.ConsoleOutputAppender
import org.openbakery.output.OutputAppender
import org.openbakery.stubs.SimulatorControlStub
import org.openbakery.stubs.XcodeStub
import spock.lang.Specification

/**
 * Created by rene on 27.06.16.
 */
class XcodebuildSpecification extends Specification {

	//Project project

	CommandRunner commandRunner = Mock(CommandRunner)

	Xcodebuild xcodebuild

	OutputAppender outputAppender = new ConsoleOutputAppender()
	XcodeBuildPluginExtension extension
	DestinationResolver destinationResolver

	def setup() {
		File projectDir = new File(".")
		Project project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		extension = new XcodeBuildPluginExtension(project)
		destinationResolver = new DestinationResolver(new SimulatorControlStub("simctl-list-xcode7.txt"))
		xcodebuild = new Xcodebuild(commandRunner, new XcodeStub(), extension.xcodebuildParameters, destinationResolver.getDestinations(extension.xcodebuildParameters))
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
		1 * commandRunner.run(_,_,_,_) >> {arguments-> commandList=arguments[1]}

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
														 "-destination", "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"

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
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"

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
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
							"-configuration", 'Debug' )
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
		}
		commandList == expectedCommandList
	}



	def "xcodebuild fails"() {

		given:
		xcodebuild.parameters.target = "Test"
		commandRunner.run(_,_,_,_) >> {
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
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
			expectedCommandList << "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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


		extension.destination = ['iPad 2', 'iPhone 4s']
		xcodebuild = new Xcodebuild(commandRunner, new XcodeStub(), extension.xcodebuildParameters, destinationResolver.getDestinations(extension.xcodebuildParameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.configuredDestinations = extension.getDestinations()

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
							"-destination", "platform=iOS Simulator,id=83384347-6976-4E70-A54F-1CFECD1E02B1",
							"-destination", "platform=iOS Simulator,id=5C8E1FF3-47B7-48B8-96E9-A12740DBC58A")
			expectedCommandList << "-enableCodeCoverage" << "yes"
			expectedCommandList << "test"
		}

		commandList == expectedCommandList
	}


	def "test command for iOS device"() {
		def commandList
		def expectedCommandList


		extension.destination { id = '83384347-6976-4E70-A54F-1CFECD1E02B1' }
		extension.simulator = false

		def xcodebuildParameters = extension.xcodebuildParameters

		xcodebuild = new Xcodebuild(commandRunner, new XcodeStub(), xcodebuildParameters, destinationResolver.getDestinations(xcodebuildParameters))

		xcodebuild.parameters.type = Type.iOS
		xcodebuild.parameters.target = 'Test';
		xcodebuild.parameters.scheme = 'myscheme'
		xcodebuild.parameters.workspace = 'myworkspace'
		xcodebuild.parameters.configuredDestinations = extension.getDestinations()
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
			expectedCommandList <<  "-destination" << "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61"
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
														 "-destination", "platform=iOS Simulator,id=5F371E1E-AFCE-4589-9158-8C439A468E61",
														 "archive",
														 "-archivePath",
														 "build/archive/myArchive.xcarchive"

			]
		}
		commandList == expectedCommandList

	}

}
