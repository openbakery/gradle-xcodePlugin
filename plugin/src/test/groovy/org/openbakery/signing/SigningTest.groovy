package org.openbakery.signing

import groovy.mock.interceptor.MockFor
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.CommandRunner
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

/**
 * Created by rene on 17.04.15.
 */
class SigningTest {

	Signing signing
	Project project
	File projectDir

	@BeforeMethod
	void setup() {

		projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		signing = new Signing(project)
	}

	@Test
	void testIdentity() {
		signing.identity = "Me!"
		assert signing.identity.equals("Me!")
	}

	@Test
	void testSingleIdentity() {
		def commandRunnerMock = new MockFor(CommandRunner)

		commandRunnerMock.demand.runWithResult { parameters ->
			def expectedParameters = ["security", "find-identity", "-v", "-p", "codesigning", signing.keychainPathInternal.absolutePath ]
			if (parameters.equals(expectedParameters)) {
				return FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-single.txt"))
			}
			println "parameters expected: " + expectedParameters
			println "but was: " + parameters
	 }

		signing.commandRunner = commandRunnerMock.proxyInstance()
		assert signing.identity.equals("Developer ID Application: MyCompany")
		commandRunnerMock.verify signing.commandRunner
	}


	@Test
	void testMultipleIdentity() {
		def commandRunnerMock = new MockFor(CommandRunner)

		commandRunnerMock.demand.runWithResult { parameters ->
			def expectedParameters = ["security", "find-identity", "-v", "-p", "codesigning", signing.keychainPathInternal.absolutePath ]
			if (parameters.equals(expectedParameters)) {
				return FileUtils.readFileToString(new File("src/test/Resource/security-find-identity-multiple.txt"))
			}
			println "parameters expected: " + expectedParameters
			println "but was: " + parameters
	 }

		signing.commandRunner = commandRunnerMock.proxyInstance()
		assert signing.identity == null
		commandRunnerMock.verify signing.commandRunner
	}

}
