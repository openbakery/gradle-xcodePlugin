package org.openbakery.signing

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

class SigningTest {

	Signing signing
	Project project
	File projectDir

	@Before
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

}
