package org.openbakery

import org.gradle.api.Project


class HockeyAppPluginExtension {
	def Object outputDirectory
	def String apiToken = null
	def String notes = "This build was uploaded using the gradle xcodePlugin"
	def String status = 2
	def String notify = 1
	def String notesType = 1

	private final Project project

	public HockeyAppPluginExtension(Project project) {
		this.project = project
		this.outputDirectory = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("hockeyapp")
		}
	}

	File getOutputDirectory() {
		return project.file(outputDirectory)
	}

	void setOutputDirectory(Object outputDirectory) {
		this.outputDirectory = outputDirectory
	}

}
