package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.codesign.Security
import spock.lang.Specification

/**
 * Created by rene on 21.12.16.
 */
class AbstractXcodeBuildTaskSpecification extends Specification {


	Project project
	AbstractXcodeBuildTask xcodeBuildTask
	File projectDir



	def setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File('build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		xcodeBuildTask = project.getTasks().getByPath(XcodePlugin.XCODE_BUILD_TASK_NAME)
	}

	def cleanup() {
			FileUtils.deleteDirectory(project.projectDir)
	}


	def "test get identity when is set manually"() {
		when:
		project.xcodebuild.signing.identity = "my identity"

		then:
		xcodeBuildTask.getSigningIdentity() == "my identity"
	}


	def "security is initialized"() {
		expect:
		xcodeBuildTask.security != null
		xcodeBuildTask.security instanceof Security
	}

	def "test get identity read from keychain"() {
		when:
		project.xcodebuild.signing.identity = null

		Security security = Mock(Security)
		security.getIdentity(project.xcodebuild.signing.getKeychainPathInternal()) >> "my identity from security"
		xcodeBuildTask.security = security

		then:
		xcodeBuildTask.getSigningIdentity() == "my identity from security"
	}
}
