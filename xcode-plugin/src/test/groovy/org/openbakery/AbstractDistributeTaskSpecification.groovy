package org.openbakery

import org.apache.tools.ant.util.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class AbstractDistributeTaskSpecification extends Specification {

	Project project
	AbstractDistributeTask distributeTask


	CommandRunner commandRunner = Mock(CommandRunner)
	File outputDirectory



	def setup() {
		File projectDir = new File("../example/iOS/ExampleWatchkit")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), 'gradle-xcodebuild/build').absoluteFile

		project.apply plugin: org.openbakery.XcodePlugin

		distributeTask = project.tasks.findByName(XcodePlugin.PACKAGE_TASK_NAME);


		//XcodeProjectFile xcodeProjectFile = new XcodeProjectFile(project, new File(projectDir, "ExampleWatchkit.xcodeproj/project.pbxproj"));
		//project.xcodebuild.projectSettings = xcodeProjectFile.getProjectSettings()


		//distributeTask.commandRunner = commandRunner
	}

	def cleanup() {
		FileUtils.delete(project.buildDir)
	}


	def "application name from archive"() {
		given:
		File app = new File(project.buildDir, "archive/ExampleWatchkit WatchKit App.xcarchive/Products/Applications/ExampleWatchkit.app")
		app.mkdirs()


		when:
		File bundleDirectory = distributeTask.getApplicationBundleDirectory()

		then:
		bundleDirectory.absolutePath.endsWith("ExampleWatchkit WatchKit App.xcarchive/Products/Applications/ExampleWatchkit.app")


	}
}
