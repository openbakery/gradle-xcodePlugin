package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class KeychainPluginExtension {
	def String mobileprovisionUri = null
	def String certificateUri = null
	def String certificatePassword = null
	def String keychainPassword = "This_is_the_default_keychain_password"
	def String destinationRoot = 'build'
	def String keychainName = 'gradle.keychain'
	
}