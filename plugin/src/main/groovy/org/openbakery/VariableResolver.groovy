package org.openbakery

import org.gradle.api.Project
import org.openbakery.internal.XcodeBuildSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 16.02.15.
 */
class VariableResolver {
	private static Logger logger = LoggerFactory.getLogger(VariableResolver.class)

	private XcodeBuildSpec buildSpec
	private File projectDirectory


	VariableResolver(File projectDirectory, XcodeBuildSpec buildSpec) {
		this.projectDirectory = projectDirectory
		this.buildSpec = buildSpec
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
						"PRODUCT_NAME": buildSpec.productName,
						"SRC_ROOT"    :projectDirectory.absolutePath,
						"TARGET_NAME" : buildSpec.target
		];
	}
}
