package org.openbakery

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test
import org.openbakery.util.VariableResolver

class VariableResolverTest {

	VariableResolver resolver
	Project project

	@Before
	void setUp() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")

		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: XcodePlugin

		resolver = new VariableResolver(project)
	}

	@Test
	void testProductName() {
		project.xcodebuild.productName = 'Test'
		assert "Test".equals(resolver.resolve('$(PRODUCT_NAME)'))

		project.xcodebuild.productName = 'Example'
		assert "Example".equals(resolver.resolve('$(PRODUCT_NAME)'))
	}

	@Test
	void testProductNameCurlyBrackets() {
		project.xcodebuild.productName = 'Test'
		assert "Test".equals(resolver.resolve('${PRODUCT_NAME}'))

		project.xcodebuild.productName = 'Example'
		assert "Example".equals(resolver.resolve('${PRODUCT_NAME}'))
	}

	@Test
	void testComplexCurlyBrackets() {
		project.xcodebuild.productName = 'Example'
		assert 'This$IsAComplexExample'.equals(resolver.resolve('This$IsAComplex${PRODUCT_NAME}'))

		project.xcodebuild.productName = 'Example'
		assert 'The Example is complex'.equals(resolver.resolve('The ${PRODUCT_NAME} is complex'))
	}

	@Test
	void testComplex() {
		project.xcodebuild.productName = 'Example'
		assert 'This$IsAComplexExample'.equals(resolver.resolve('This$IsAComplex$(PRODUCT_NAME)'))


		project.xcodebuild.productName = 'Example'
		assert 'The Example is complex'.equals(resolver.resolve('The $(PRODUCT_NAME) is complex'))
	}

	@Test
	void testBoth() {
		project.xcodebuild.productName = 'Example'
		assert "Example Example".equals(resolver.resolve('${PRODUCT_NAME} $(PRODUCT_NAME)'))
	}

	@Test
	void testTargetName() {
		project.xcodebuild.target = 'MyTarget'
		assert "MyTarget".equals(resolver.resolve('$(TARGET_NAME)'))
	}

	@Test
	void testTargetNameWithTransformation() {
		project.xcodebuild.target = 'MyTarget'
		assert "MyTarget".equals(resolver.resolve('$(TARGET_NAME:c99extidentifier)'))
	}

	@Test
	void testUnknownVariable() {
		assert '$(FOOBAR)'.equals(resolver.resolve('$(FOOBAR)'))
	}
}
