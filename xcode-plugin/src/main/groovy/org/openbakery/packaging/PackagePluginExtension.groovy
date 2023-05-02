package org.openbakery.packaging

import org.gradle.api.Project

class PackagePluginExtension {
	boolean packageSymbols = true

	private final Project project

	PackagePluginExtension(Project project) {
		this.project = project
	}
}
