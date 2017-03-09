package org.openbakery.coverage

import org.gradle.api.Project

class CoveragePluginExtension {

	def Object outputDirectory
	def String exclude = null
	def String include = null
	def String outputFormat = null

	private final Project project

	public CoveragePluginExtension(Project project) {
		this.project = project
		this.outputDirectory = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("report/coverage")
		}
	}

	File getOutputDirectory() {
		return project.file(outputDirectory)
	}

	void setOutputDirectory(Object outputDirectory) {
		this.outputDirectory = outputDirectory
	}


	String[] getOutputParameter() {
		if (outputFormat != null) {
			if (outputFormat.toLowerCase().equals("xml")) {
				return ["--xml"]
			}
			if (outputFormat.toLowerCase().equals("html")) {
				return ["--html", "--html-details"]
			}
		}
		return [];
	}


}
