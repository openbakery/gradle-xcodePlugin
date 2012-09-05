package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class InfoPlistExtension {
	def String bundleIdentifier = null
	def String bundleIdentifierSuffix = null
	def String version = null
	def String versionSuffix = null
	def String versionPrefix = null
	def String shortVersionString = null
	def String shortVersionStringSuffix = null
	def String shortVersionStringPrefix = null
}