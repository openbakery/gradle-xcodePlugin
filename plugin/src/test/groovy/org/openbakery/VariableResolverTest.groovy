package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

/**
 * Created by rene on 16.02.15.
 */
class VariableResolverTest {

	VariableResolver resolver;
	Project project;

	@BeforeTest
	void setUp() {

		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin


		resolver = new VariableResolver(project);
	}

	@Test
	void testProductName() {
		project.xcodebuild.productName = 'Test'
		assert "Test".equals(resolver.resolve('${PRODUCT_NAME}'))

		project.xcodebuild.productName = 'Example'
		assert "Example".equals(resolver.resolve('${PRODUCT_NAME}'))
	}




	@Test
	void testUnknownVariable() {
		assert '${FOOBAR}'.equals(resolver.resolve('${FOOBAR}'))
	}

}
