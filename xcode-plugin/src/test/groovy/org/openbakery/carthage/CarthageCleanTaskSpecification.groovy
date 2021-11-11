package org.openbakery.carthage

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class CarthageCleanTaskSpecification extends Specification {


	Project project
	File projectDir
	CarthageCleanTask carthageCleanTask;


	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin

		carthageCleanTask = project.getTasks().getByPath('carthageClean')
	}


	def "has carthageDelete task"() {
		expect:
		carthageCleanTask instanceof CarthageCleanTask
	}


	def "perform carthageDelete task"() {
		given:
		File cartageDirectory = new File(projectDir, "Carthage")
		cartageDirectory.mkdirs()

		when:
		carthageCleanTask.clean()

		then:
		!cartageDirectory.exists()

	}
}
