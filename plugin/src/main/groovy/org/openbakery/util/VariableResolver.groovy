package org.openbakery.util

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 16.02.15.
 */
class VariableResolver {
	private static Logger logger = LoggerFactory.getLogger(VariableResolver.class)

	private Project project

	VariableResolver(Project project) {
		this.project = project
	}

	/**
	 * Replaces the variables ${...}
	 *
	 * @param text
	 * @return
	 */
	String resolveCurlyBrackets(String text) {
		String result = text
		binding().each() { key, value ->
			if (value != null) {
				result = result.replaceAll('\\$\\{' + key + '\\}', value)
			}
		}
		return result
	}

	/**
	 * Replaces the variables in the given string with the actual value. e.g. ${PRODUCT_NAME} od $(PRODUCT_NAME) get replaced by the real product name
	 *
	 * @param text
	 * @return
	 */
	String resolve(String text) {
		if (text == null) {
			return null
		}
		String result = text
		binding().each() { key, value ->
			if (value != null) {
				result = result.replaceAll('\\$\\(' + key + '\\)', value)
			}
		}
		return resolveCurlyBrackets(result)
	}


	def binding() {
		return [
						"PRODUCT_NAME": project.xcodebuild.productName,
						"SRC_ROOT"    : project.projectDir.absolutePath,
						"TARGET_NAME" : project.xcodebuild.target
		];
	}
}
