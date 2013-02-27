package org.openbakery

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal


class TestFlightPluginExtension {
	def Object outputDirectory
	def String apiToken = null
	def String teamToken = null
	def String notes = "This build was uploaded using the gradle xcodePlugin"

	private final Project project

	public TestFlightPluginExtension(Project project) {
		this.project = project
		this.outputDirectory = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("testflight")
		}
	}

	File getOutputDirectory() {
		return project.file(outputDirectory)
	}

	void setOutputDirectory(Object outputDirectory) {
		this.outputDirectory = outputDirectory
	}
}
