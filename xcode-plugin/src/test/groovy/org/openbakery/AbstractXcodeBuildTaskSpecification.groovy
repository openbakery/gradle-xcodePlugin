package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.codesign.Security
import spock.lang.Specification

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
		given:
		File keychain = new File(projectDir, "my.keychain")
		FileUtils.writeStringToFile(keychain, "dummy")
		project.xcodebuild.signing.keychain = keychain
		project.xcodebuild.signing.identity = null

		when:
		Security security = Mock(Security)
		security.getIdentity(project.xcodebuild.signing.getKeychainPathInternal()) >> "my identity from security"
		xcodeBuildTask.security = security

		then:
		xcodeBuildTask.getSigningIdentity() == "my identity from security"

		cleanup:
		keychain.delete()
	}

	def "test get identity null and keychain does not exist should return null"() {
		when:
		project.xcodebuild.signing.identity = null
		project.xcodebuild.signing.keychain = new File("my.keychain")

		then:
		xcodeBuildTask.getSigningIdentity() == null
	}

}
