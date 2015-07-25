package org.openbakery.oclint

import org.gradle.api.Project

/**
 * Created by rene on 22.07.15.
 */
class OCLintPluginExtension {

	def String reportType = "html"
	def Boolean excludePods = true

	def rules = []

	private final Project project

	public OCLintPluginExtension(Project project) {
		this.project = project
	}


}
