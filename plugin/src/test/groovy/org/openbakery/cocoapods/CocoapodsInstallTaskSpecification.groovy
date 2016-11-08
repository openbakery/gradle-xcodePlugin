package org.openbakery.cocoapods

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import spock.lang.Specification


/**
 * Created by rene on 11.11.14.
 */
class CocoapodsInstallTaskSpecification extends Specification {


	Project project
	CocoapodsInstallTask cocoapodsTask;

	CommandRunner commandRunner = Mock(CommandRunner)

	def setup() {

		File projectDir =  new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin:org.openbakery.XcodePlugin

		cocoapodsTask = project.getTasks().getByPath('cocoapodsInstall')

		cocoapodsTask.commandRunner = commandRunner

	}


	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}


	def "run pod setup"() {
		given:
		commandRunner.runWithResult("which", "pod") >> "/usr/local/bin/pod"

		when:
		cocoapodsTask.install()

		then:
		1 * commandRunner.run(["/usr/local/bin/pod", "setup"], _)
	}

	def "install pods"() {
		given:
		cocoapodsTask.dependsOn(XcodePlugin.COCOAPODS_BOOTSTRAP_TASK_NAME)
		commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir") >> "/tmp/gems"

		when:
		cocoapodsTask.install()

		then:
		1 * commandRunner.run(["/tmp/gems/bin/pod", "install"], _)

	}


	def "install pods use global pods"() {
		given:
		commandRunner.runWithResult("which", "pod") >> "/usr/local/bin/pod"

		when:
		cocoapodsTask.install()

		then:
		1 * commandRunner.run(["/usr/local/bin/pod", "install"], _)

	}

	def "skip install"() {
		given:
		File podfileLock = new File(project.projectDir , "Podfile.lock")
		FileUtils.writeStringToFile(podfileLock, "Dummy")

		File manifest = new File(project.projectDir , "Pods/Manifest.lock")
		FileUtils.writeStringToFile(manifest, "Dummy")

		when:
		cocoapodsTask.install()

		then:
		0 * commandRunner.run(["/tmp/gems/bin/pod", "install"], _)
	}


	def "Reinstall Pods"() {
		given:
		cocoapodsTask.dependsOn(XcodePlugin.COCOAPODS_BOOTSTRAP_TASK_NAME)

		File podfileLock = new File(project.projectDir , "Podfile.lock")
		FileUtils.writeStringToFile(podfileLock, "Dummy")

		File manifest = new File(project.projectDir , "Pods/Manifest.lock")
		FileUtils.writeStringToFile(manifest, "Foo")

		commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir") >> "/tmp/gems"

		when:
		cocoapodsTask.install()

		then:
		1 * commandRunner.run(["/tmp/gems/bin/pod", "install"], _)

	}

	def "refresh dependencies"() {
		given:
		cocoapodsTask.dependsOn(XcodePlugin.COCOAPODS_BOOTSTRAP_TASK_NAME)

		File podfileLock = new File(project.projectDir , "Podfile.lock")
		FileUtils.writeStringToFile(podfileLock, "Dummy")

		File manifest = new File(project.projectDir , "Pods/Manifest.lock")
		FileUtils.writeStringToFile(manifest, "Dummy")

		project.getGradle().getStartParameter().setRefreshDependencies(true)

		commandRunner.runWithResult("ruby", "-rubygems", "-e", "puts Gem.user_dir") >> "/tmp/gems"

		when:
		cocoapodsTask.install()

		then:
		1 * commandRunner.run(["/tmp/gems/bin/pod", "install"], _)

	}
}
