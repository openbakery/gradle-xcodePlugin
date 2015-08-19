package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.*

/**
 * Created by rene on 19.08.15.
 */
class XcodeBuildTaskSpecification extends Specification {


	Project project
	File projectDir
	XcodeBuildTask task
	File infoPlist

	def commandRunner = Mock(CommandRunner)

	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.apply plugin: org.openbakery.XcodePlugin


		projectDir.mkdirs()


		task = project.tasks.findByName(XcodePlugin.XCODE_BUILD_TASK_NAME)
		task.commandRunner = commandRunner

		task.buildSpec.target = "Test"

	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "task and configuration merged build spec"() {
		setup:
		project.xcodebuild.target = "Another"
		project.xcodebuild.configuration = "Debug"
		task.target = 'myTarget'
		task.configuration = "Release"


		when:
		task.executeTask()

		then:
		1 * commandRunner.run(*_) >>  { arguments ->
			assertThat(arguments[1], contains(
							'xcodebuild',
							'-configuration', 'Release',
							'-sdk', XcodePlugin.SDK_IPHONESIMULATOR,
							'-target', 'myTarget',
							'-derivedDataPath', project.xcodebuild.derivedDataPath.absolutePath,
							'DSTROOT=' + task.buildSpec.dstRoot.absolutePath,
							'OBJROOT=' + task.buildSpec.objRoot.absolutePath,
							'SYMROOT=' + task.buildSpec.symRoot.absolutePath,
							'SHARED_PRECOMPS_DIR=' + task.buildSpec.sharedPrecompsDir.absolutePath
			))

		}

	}


	def "scheme merge build spec"() {
		setup:
		project.xcodebuild.scheme = "bogus"
		task.scheme = 'MyScheme'

		when:
		task.executeTask()

		then:
		1 * commandRunner.run(*_) >>  { arguments ->
			assertThat(arguments[1], contains(
							'xcodebuild',
							'-scheme', 'MyScheme',
							'-sdk', XcodePlugin.SDK_IPHONESIMULATOR,
							'-configuration', 'Debug',
							'-derivedDataPath', project.xcodebuild.derivedDataPath.absolutePath,
							'DSTROOT=' + task.buildSpec.dstRoot.absolutePath,
							'OBJROOT=' + task.buildSpec.objRoot.absolutePath,
							'SYMROOT=' + task.buildSpec.symRoot.absolutePath,
							'SHARED_PRECOMPS_DIR=' + task.buildSpec.sharedPrecompsDir.absolutePath
			))

		}

	}


	def "additionalParameters merge build spec"() {
		setup:
		project.xcodebuild.scheme = "bogus"
		task.scheme = 'MyScheme'
		task.additionalParameters = ["foo", "bar"]

		when:
		task.executeTask()

		then:
		1 * commandRunner.run(*_) >>  { arguments ->
			assertThat(arguments[1], contains(
							'xcodebuild',
							'-scheme', 'MyScheme',
							'-sdk', XcodePlugin.SDK_IPHONESIMULATOR,
							'-configuration', 'Debug',
							'-derivedDataPath', project.xcodebuild.derivedDataPath.absolutePath,
							'DSTROOT=' + task.buildSpec.dstRoot.absolutePath,
							'OBJROOT=' + task.buildSpec.objRoot.absolutePath,
							'SYMROOT=' + task.buildSpec.symRoot.absolutePath,
							'SHARED_PRECOMPS_DIR=' + task.buildSpec.sharedPrecompsDir.absolutePath,
							'foo',
							'bar'
			))

		}

	}


	def "arch merge build spec"() {
		setup:
		project.xcodebuild.scheme = "bogus"
		task.scheme = 'MyScheme'
		task.arch = 'x86_64'

		when:
		task.executeTask()

		then:
		1 * commandRunner.run(*_) >>  { arguments ->
			assertThat(arguments[1], contains(
							'xcodebuild',
							'-scheme', 'MyScheme',
							'-sdk', XcodePlugin.SDK_IPHONESIMULATOR,
							'-configuration', 'Debug',
							'ARCHS=x86_64',
							'-derivedDataPath', project.xcodebuild.derivedDataPath.absolutePath,
							'DSTROOT=' + task.buildSpec.dstRoot.absolutePath,
							'OBJROOT=' + task.buildSpec.objRoot.absolutePath,
							'SYMROOT=' + task.buildSpec.symRoot.absolutePath,
							'SHARED_PRECOMPS_DIR=' + task.buildSpec.sharedPrecompsDir.absolutePath
			))

		}

	}

	/*
	@Test
			void run_command_with_with_merged_build_spec_2() {




				// currently order is important
				expectedCommandList.add("-configuration")
				expectedCommandList.add("configuration")
				expectedCommandList.add("-sdk")
				expectedCommandList.add(XcodePlugin.SDK_IPHONEOS)

				expectedCommandList.add("-target")
				expectedCommandList.add("mytarget")

				addExpectedDefaultDirs()

				commandRunnerMock.run(projectDir, expectedCommandList, null, anything()).times(1)

				mockControl.play {
					xcodeBuildTask.executeTask()
				}
			}
			*/

}
