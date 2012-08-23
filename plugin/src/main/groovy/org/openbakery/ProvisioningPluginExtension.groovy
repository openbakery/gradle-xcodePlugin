package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class ProvisioningPluginExtension {
	def String mobileprovisionUri = null
	def String destinationRoot = 'build'
    def String mobileprovisionFile = null
}