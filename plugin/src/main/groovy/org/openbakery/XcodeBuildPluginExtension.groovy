package org.openbakery

class XcodeBuildPluginExtension {
	def String infoPlist = null
	def String scheme = null
	def String configuration = 'Debug'
	def String sdk = 'iphonesimulator'
	def target = null
	def String unitTestTarget = null
	def String buildRoot = 'build'
	def String dstRoot = buildRoot + '/dst'
	def String objRoot = buildRoot + '/obj'
	def String symRoot = buildRoot + '/sym'
	def String sharedPrecompsDir = buildRoot + '/shared'
	def String sourceDirectory = '.'
	def String signIdentity = null
	def additionalParameters = null
	def String bundleNameSuffix = null
	def String arch = null
}