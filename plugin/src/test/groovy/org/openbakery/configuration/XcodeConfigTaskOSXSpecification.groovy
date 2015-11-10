package org.openbakery.configuration

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import spock.lang.Specification

/**
 * Created by rene on 10.03.15.
 */
class XcodeConfigTaskOSXSpecification extends Specification {

	XcodeConfigTask xcodeConfigTask
	Project project

	def setup() {
		File projectDir = new File("../example/OSX/ExampleOSX")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project.apply plugin: org.openbakery.XcodePlugin

		xcodeConfigTask = project.getTasks().getByName(XcodePlugin.XCODE_CONFIG_TASK_NAME)

		project.xcodebuild.target = "ExampleOSX"

	}

	def cleanup() {
		FileUtils.deleteDirectory(new File("build/Platforms"))
		FileUtils.deleteDirectory(new File("build/Contents"))
	}


	def "load OS X config"() {
		when:
		xcodeConfigTask.configuration()

		then:
		xcodeConfigTask.xcodeProjectFile.isOSX
	}

}
