package org.openbakery.archiving

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.ExpectedException
import org.junit.rules.TemporaryFolder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.XcodeService
import org.openbakery.xcode.Type
import spock.lang.Shared
import spock.lang.Specification

import static org.openbakery.xcode.Xcodebuild.*

class XcodeBuildArchiveTaskIosAndTvOSTest extends Specification {

	@Rule
	public ExpectedException exception = ExpectedException.none()

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder()

	Project project
	XcodeBuildArchiveTaskIosAndTvOS subject
	CommandRunner mockCommandRunner = Mock(CommandRunner)
	XcodeService.XcodeApp mockXcodeApp = Mock(XcodeService.XcodeApp)
	XcodeService mockXcodeService = Mock(XcodeService)
	File outputDir
	File fakeXccConfig

	@Shared
	File fakeXcodeRuntime

	private static final String TEST_SCHEME = "test-scheme"

	void setup() {
		project = ProjectBuilder.builder()
				.withProjectDir(testProjectDir.root)
				.build()

		project.buildDir = testProjectDir.newFile("build.gradle")
		project.apply plugin: XcodePlugin

		outputDir = testProjectDir.newFolder("output")
		fakeXccConfig = testProjectDir.newFile("test.xcconfig")
		fakeXcodeRuntime = testProjectDir.newFile("test.app")

		subject = project.getTasks().getByName(XcodeBuildArchiveTaskIosAndTvOS.NAME) as XcodeBuildArchiveTaskIosAndTvOS
		assert subject != null

		subject.xcodeServiceProperty.set(mockXcodeService)
		subject.commandRunnerProperty.set(mockCommandRunner)
		subject.scheme.set(TEST_SCHEME)
		subject.buildType.set(Type.iOS)
		subject.outputArchiveFile.set(outputDir)
		subject.xcConfigFile.set(fakeXccConfig)
	}

	def "The xcode archive should be called with the right configuration"() {
		when:
		subject.archive()

		then:
		noExceptionThrown()

		1 * mockCommandRunner.run([EXECUTABLE,
								   ACTION_ARCHIVE,
								   ARGUMENT_SCHEME, TEST_SCHEME,
								   ARGUMENT_ARCHIVE_PATH, outputDir.absolutePath,
								   ARGUMENT_XCCONFIG, fakeXccConfig.absolutePath], _)
	}

	def "Should be able to customise xcode version"() {
		when:
		mockXcodeApp.contentDeveloperFile >> fakeXcodeRuntime
		mockXcodeService.getInstallationForVersion(version) >> mockXcodeApp

		subject.xcodeVersion.set(version)
		subject.archive()

		then:
		noExceptionThrown()

		1 * mockCommandRunner.run([EXECUTABLE,
								   ACTION_ARCHIVE,
								   ARGUMENT_SCHEME, TEST_SCHEME,
								   ARGUMENT_ARCHIVE_PATH, outputDir.absolutePath,
								   ARGUMENT_XCCONFIG, fakeXccConfig.absolutePath],
				['DEVELOPER_DIR': fakeXcodeRuntime.absolutePath.toString()])

		where:
		version | envValues
		"9.3"   | _
		"9.2"   | _
	}

	def "The xcode version configuration should be optional"() {
		when:
		mockXcodeApp.contentDeveloperFile >> fakeXcodeRuntime
		subject.xcodeVersion.set(null)
		subject.archive()

		then:
		noExceptionThrown()

		1 * mockCommandRunner.run([EXECUTABLE,
								   ACTION_ARCHIVE,
								   ARGUMENT_SCHEME, TEST_SCHEME,
								   ARGUMENT_ARCHIVE_PATH, outputDir.absolutePath,
								   ARGUMENT_XCCONFIG, fakeXccConfig.absolutePath], [:])
	}
}
