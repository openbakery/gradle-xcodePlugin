package org.openbakery

import org.gradle.api.Project

class HockeyKitPluginExtension {
	def String displayName = null
	def String versionDirectoryName = "0"
	def Object outputDirectory

	private final Project project

	public HockeyKitPluginExtension(Project project) {
		this.project = project
		this.outputDirectory = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("hockeykit")
		}
	}

	File getOutputDirectory() {
		return project.file(outputDirectory)
	}

	void setOutputDirectory(Object outputDirectory) {
		this.outputDirectory = outputDirectory
	}
}
