package org.openbakery.appcenter

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodePlugin
import spock.lang.Specification

class AbstractAppCenterTaskSpecification extends Specification {

	Project project
	AbstractAppCenterTask task

	void setup() {
		File projectDir = File.createTempDir()
		project = ProjectBuilder.builder()
			.withProjectDir(projectDir)
			.build()
		project.apply plugin: org.openbakery.XcodePlugin

		task = project.getTasks().getByPath(XcodePlugin.APPCENTER_IPA_UPLOAD_TASK_NAME)
		assert task != null
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def "timeout"() {
		when:
		task.readTimeout(150)

		then:
		task.httpUtil.readTimeoutInSeconds == 150
	}
}
