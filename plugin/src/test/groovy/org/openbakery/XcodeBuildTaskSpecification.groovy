package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification



/**
 * Created by rene on 01.10.15.
 */
class XcodeBuildTaskSpecification extends Specification {

	Project project

	XcodeBuildTask xcodeBuildTask


	CommandRunner commandRunner = Mock(CommandRunner);


	def setup() {

		project = ProjectBuilder.builder().build()
		project.buildDir = new File('build').absoluteFile

		//File outputFile = new File(project.buildDir, "xcodebuild-output.txt" )
		//FileUtils.writeStringToFile(outputFile, "dummy")
		//commandRunnerMock.setOutputFile(outputFile)

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_TASK_NAME)
		xcodeBuildTask.commandRunner = commandRunner

	}

	def expectedDefaultDirectories() {
		return [
						"-derivedDataPath", new File("build/derivedData").absolutePath,
						"DSTROOT=" + new File("build/dst").absolutePath,
						"OBJROOT=" + new File("build/obj").absolutePath,
						"SYMROOT=" + new File("build/sym").absolutePath,
						"SHARED_PRECOMPS_DIR=" + new File("build/shared").absolutePath
		]
	}

	def createCommand(String... commands) {
		def command = []
		command.addAll(commands)
		return command
	}

	def createCommandWithDefaultDirectories(String... commands) {
		def command = createCommand(commands)
		command.addAll(expectedDefaultDirectories())
		return command
	}

	def "IllegalArgumentException_when_no_scheme_or_target_given"() {
		when:
		xcodeBuildTask.xcodebuild()

		then:
		thrown(IllegalArgumentException)
	}

	def "run command with expected scheme and expected default directories"() {
		def commandList

		project.xcodebuild.sdk = 'iphoneos';
		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'


		when:
		xcodeBuildTask.xcodebuild()

		then:
		1 * commandRunner.run(_,_,_,_) >> {arguments-> commandList=arguments[1]}
		commandList == createCommandWithDefaultDirectories('xcodebuild',
										"-scheme", 'myscheme',
										"-workspace", 'myworkspace',
										"-configuration", "Debug",
										"CODE_SIGN_IDENTITY=",
										"CODE_SIGNING_REQUIRED=NO"
		)

	}


	def "run command with expected scheme and expected directories"() {
		def commandList

		project.xcodebuild.scheme = 'myscheme'
		project.xcodebuild.workspace = 'myworkspace'
		project.xcodebuild.sdk = 'iphoneSimulator';


		project.xcodebuild.derivedDataPath = new File("build/myDerivedData").absoluteFile
		project.xcodebuild.dstRoot = new File("build/myDst").absoluteFile
		project.xcodebuild.objRoot = new File("build/myObj").absoluteFile
		project.xcodebuild.symRoot = new File("build/mySym").absoluteFile
		project.xcodebuild.sharedPrecompsDir = new File("build/myShared").absoluteFile

		when:
		xcodeBuildTask.xcodebuild()

		then:
		1 * commandRunner.run(_, _, _, _) >> { arguments -> commandList = arguments[1] }
		interaction {
			commandList == ['xcodebuild',
							"-scheme", 'myscheme',
							"-workspace", 'myworkspace',
							"-configuration", "Debug",
							"CODE_SIGN_IDENTITY=",
							"CODE_SIGNING_REQUIRED=NO",
							"-derivedDataPath", new File("build/myDerivedData").absolutePath,
							"DSTROOT=" + new File("build/myDst").absolutePath,
							"OBJROOT=" + new File("build/myObj").absolutePath,
							"SYMROOT=" + new File("build/mySym").absolutePath,
							"SHARED_PRECOMPS_DIR=" + new File("build/myShared").absolutePath
			]
		}

	}

		/*

	@Test
	void run_command_with_expected_target_and_expected_defaults() {
		// currently order is important
		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")
		expectedCommandList.add("-sdk")
		expectedCommandList.add(XcodePlugin.SDK_IPHONESIMULATOR)

		def target = 'mytarget'
		project.xcodebuild.target = target
		expectedCommandList.add("-target")
		expectedCommandList.add(target)

		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}



	@Test
	public void run_command_without_signIdentity() {
		addExpectedScheme()
		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		def signIdentity = ""
		project.xcodebuild.signing.identity = ""

		addExpectNoSigning()
		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}


	@Test
	public void run_command_without_signIdentity_osx() {
		addExpectedScheme()
		project.xcodebuild.sdk = 'macosx';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		def signIdentity = ""
		project.xcodebuild.signing.identity = ""

		addExpectNoSigning()
		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_arch() {
		addExpectedScheme()

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		addExpectNoSigning()

		project.xcodebuild.arch = ['myarch']

		expectedCommandList.add("ARCHS=myarch")


		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}


	@Test
	public void run_command_with_muiltple_arch() {
		addExpectedScheme()

		project.xcodebuild.sdk = 'iphoneos';
		expectedCommandList.add("-sdk")
		expectedCommandList.add('iphoneos')

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		addExpectNoSigning()

		project.xcodebuild.arch = ['armv', 'armv7s']

		expectedCommandList.add("ARCHS=armv armv7s")


		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	public void run_command_with_workspace() {
		project.xcodebuild.scheme = 'myscheme'
		expectedCommandList.add("-scheme")
		expectedCommandList.add(project.xcodebuild.scheme)


		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-workspace")
		expectedCommandList.add("myworkspace")

		project.xcodebuild.sdk = 'iphoneSimulator';
		expectedCommandList.add("-sdk")
		expectedCommandList.add(project.xcodebuild.sdk)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")


		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}


	@Test
	void run_command_with_workspace_but_without_scheme() {
		// currently order is important
		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")
		expectedCommandList.add("-sdk")
		expectedCommandList.add(XcodePlugin.SDK_IPHONESIMULATOR)

		def target = 'mytarget'
		project.xcodebuild.target = target
		project.xcodebuild.workspace = 'myworkspace'
		expectedCommandList.add("-target")
		expectedCommandList.add(target)

		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}



	@Test
	void run_command_scheme_and_simulatorbuild() {
		addExpectedScheme()

		expectedCommandList.add("-sdk")
		expectedCommandList.add(XcodePlugin.SDK_IPHONESIMULATOR)

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	void run_command_scheme_and_simulatorbuild_and_arch() {
		addExpectedScheme()

		expectedCommandList.add("-sdk")
		expectedCommandList.add(XcodePlugin.SDK_IPHONESIMULATOR)

		project.xcodebuild.arch = ['i368'];

		expectedCommandList.add("ONLY_ACTIVE_ARCH=NO");

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")

		expectedCommandList.add("ARCHS=i368");


		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

		mockControl.play {
			xcodeBuildTask.xcodebuild()
		}
	}




	@Test
	void run_command_xcodeversion() {

		commandRunnerMock.runWithResult("mdfind", "kMDItemCFBundleIdentifier=com.apple.dt.Xcode").returns("/Applications/Xcode.app")
		commandRunnerMock.runWithResult("/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild", "-version").returns("Xcode 5.1.1\nBuild version 5B1008")
		project.xcodebuild.commandRunner = commandRunnerMock



		expectedCommandList?.clear()
		expectedCommandList = ["/Applications/Xcode.app/Contents/Developer/usr/bin/xcodebuild"]

		expectedCommandList.add("-configuration")
		expectedCommandList.add("Debug")
		expectedCommandList.add("-sdk")
		expectedCommandList.add(XcodePlugin.SDK_IPHONESIMULATOR)

		def target = 'mytarget'
		project.xcodebuild.target = target
		expectedCommandList.add("-target")
		expectedCommandList.add(target)

		addExpectedDefaultDirs()

		commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)



		mockControl.play {
			project.xcodebuild.version = '5B1008';

			xcodeBuildTask.xcodebuild()
		}
	}

	@Test
	void dependsOn() {
		def dependsOn  = xcodeBuildTask.getDependsOn()

		assert dependsOn.contains(XcodePlugin.XCODE_CONFIG_TASK_NAME)
		assert dependsOn.contains(XcodePlugin.INFOPLIST_MODIFY_TASK_NAME)
	}

	*/


}
