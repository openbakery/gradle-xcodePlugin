package org.openbakery.util

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class VariableResolver {
	private static Logger logger = LoggerFactory.getLogger(VariableResolver.class)

	private Project project

	VariableResolver(Project project) {
		this.project = project
	}

	/**
	 * Replaces the variables in the given string with the actual value. e.g. ${PRODUCT_NAME} od $(PRODUCT_NAME) get
	 * replaced by the real product name.
	 *
	 * Also handles a single build settings variable transformation like $(PRODUCT_NAME:c99extidentifier).
	 * See http://codeworkshop.net/posts/xcode-build-setting-transformations
	 *
	 * Note the specified transformation is not applied since we are just replacing the entire variable expression with
	 * a value from the extension.
	 *
	 * @param text
	 * @return
	 */
	String resolve(String text) {
		if (text == null) {
			return null
		}
		if (!text.contains("\$")) {
			// Skip resolution if the text doesn't contain any variables.
			return text
		}

		String result = text
		binding().each() { key, value ->
			if (value != null) {
				/*
				 * RegEx pattern matching any of these:
				 * $(VARIABLE) ${VARIABLE} $(VARIABLE:c99extidentifier)
				 */
				String regex = "\\\$(\\(|\\{)$key(:\\w+)?(\\)|\\})"
				if (!result.matches(regex)) {
					logger.debug("$result NOT found in $text using regex: $regex")
				}
				result = result.replaceAll(regex, value)
			}
		}

		return result
	}

	def binding() {
		return [
			"PRODUCT_NAME": project.xcodebuild.productName,
			"SRC_ROOT"    : project.projectDir.absolutePath,
			"TARGET_NAME" : project.xcodebuild.target
		]
	}
}