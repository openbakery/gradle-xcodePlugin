package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class ProvisioningPluginExtension {
	def String mobileprovisionUri = null
	def Object destinationRoot

	private String mobileprovisionFile = null
	private Project project

	public ProvisioningPluginExtension(Project project) {
		this.project = project;

		this.destinationRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("provisioning")
		}
	}

	File getDestinationRoot() {
		return project.file(destinationRoot)
	}

	void setDestinationRoot(Object destinationRoot) {
		this.destinationRoot = destinationRoot
	}
}