package org.openbakery

import groovy.text.SimpleTemplateEngine
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 16.02.15.
 */
class VariableResolver {
	private static Logger logger = LoggerFactory.getLogger(VariableResolver.class)

	private Project project
	private SimpleTemplateEngine templateEngine

	VariableResolver(Project project) {
		this.project = project
		templateEngine = new SimpleTemplateEngine()
	}

	/**
	 * Replaces the variables in the given string with the actual value. e.g. ${PRODUCT_NAME} get replaced by the real product name
	 *
	 * @param text
	 * @return
	 */
	String resolve(String text) {
		try {
		return templateEngine.createTemplate(text).make(binding())
		} catch (MissingPropertyException ex) {
			logger.error(ex.getMessage(), ex)
			return text
		}
	}



	def binding() {
		return [
						"PRODUCT_NAME": project.xcodebuild.productName,
						"SRC_ROOT"    : project.projectDir
		];
	}
}
