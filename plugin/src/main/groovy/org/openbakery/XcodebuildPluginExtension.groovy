package org.openbakery

import org.gradle.api.Project
import org.gradle.api.Plugin

class XcodebuildPluginExtension {
	def String infoPlist = null;
	def String configuration = 'Debug'
	def String sdk = 'iphonesimulator'
	def String target = 'unknown'
	def String dstRoot = 'build/Dst'
	def String objRoot = 'build/Obj'
	def String symRoot = 'build/Sym'
	def String sharedPrecompsDir = 'build/shared'
	def String sourceDirectory = '.'
	def String signIdentity = null
	def String additionalParameters = null
    def String archiveVersion = null
	
	
}