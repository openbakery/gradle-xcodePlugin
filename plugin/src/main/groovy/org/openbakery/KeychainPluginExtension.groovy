package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class KeychainPluginExtension {
	def String mobileprovisionUri = null
	def String certificateUri = null
	def String certificatePassword = null
	def String keychainPassword = "This_is_the_default_keychain_password"
	def Object destinationRoot
	def String keychainName = 'gradle.keychain'

	private Project project

	public KeychainPluginExtension(Project project) {
		this.project = project;

		this.destinationRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("keychain")
		}
	}

	File getDestinationRoot() {
		return project.file(destinationRoot)
	}

	void setDestinationRoot(Object destinationRoot) {
		this.destinationRoot = destinationRoot
	}
	
}