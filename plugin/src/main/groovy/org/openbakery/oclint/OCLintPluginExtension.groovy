package org.openbakery.oclint

import org.gradle.api.Project

class OCLintPluginExtension {

	def String reportType = "html"

	def rules = []
	def disableRules = []
	def excludes = []

	def maxPriority1 = 0
	def maxPriority2 = 10
	def maxPriority3 = 20


	private final Project project

	public OCLintPluginExtension(Project project) {
		this.project = project
	}


}
