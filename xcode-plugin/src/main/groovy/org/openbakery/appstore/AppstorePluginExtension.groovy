package org.openbakery.appstore

import org.gradle.api.Project

class AppstorePluginExtension {

	def String apiKey = null
	def String apiIssuer = null
	def String publicId = null
	def String appleId = null
	def String username = null
	def String password = null
	def String ascProvider = null
	def String bundleVersion = null
	def String shortBundleVersion = null
	def String bundleIdentifier = null
	def Boolean useNewUpload = null

	public AppstorePluginExtension(Project project) {
	}
}
