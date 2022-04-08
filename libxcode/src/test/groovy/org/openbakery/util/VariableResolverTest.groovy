package org.openbakery.util

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openbakery.util.VariableResolver

class VariableResolverTest {

	File temporaryDirectory

	@BeforeEach
	void setUp() {
		temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
	}

	VariableResolver getVariableResolver(String productName = "Test", String target = "Test") {
		return new VariableResolver(productName, temporaryDirectory, target)
	}

	@Test
	void testProductName() {
		assert "Test".equals(variableResolver.resolve('$(PRODUCT_NAME)'))
		assert "Example".equals(getVariableResolver("Example").resolve('$(PRODUCT_NAME)'))
	}

	@Test
	void testProductNameCurlyBrackets() {
		assert "Test".equals(variableResolver.resolve('${PRODUCT_NAME}'))

		assert "Example".equals(getVariableResolver("Example").resolve('${PRODUCT_NAME}'))
	}

	@Test
	void testComplexCurlyBrackets() {
		def variableResolver = getVariableResolver("Example")
		assert 'This$IsAComplexExample'.equals(variableResolver.resolve('This$IsAComplex${PRODUCT_NAME}'))
		assert 'The Example is complex'.equals(variableResolver.resolve('The ${PRODUCT_NAME} is complex'))
	}

	@Test
	void testComplex() {
		def variableResolver = getVariableResolver("Example")
		assert 'This$IsAComplexExample'.equals(variableResolver.resolve('This$IsAComplex$(PRODUCT_NAME)'))
		assert 'The Example is complex'.equals(variableResolver.resolve('The $(PRODUCT_NAME) is complex'))
	}

	@Test
	void testBoth() {
		def variableResolver = getVariableResolver("Example")
		assert "Example Example".equals(variableResolver.resolve('${PRODUCT_NAME} $(PRODUCT_NAME)'))
	}

	@Test
	void testTargetName() {
		def variableResolver = getVariableResolver("Example", "MyTarget")
		assert "MyTarget".equals(variableResolver.resolve('$(TARGET_NAME)'))
	}

	@Test
	void testTargetNameWithTransformation() {
		def variableResolver = getVariableResolver("Example", "MyTarget")
		assert "MyTarget".equals(variableResolver.resolve('$(TARGET_NAME:c99extidentifier)'))
	}

	@Test
	void testUnknownVariable() {
		assert '$(FOOBAR)'.equals(variableResolver.resolve('$(FOOBAR)'))
	}
}
