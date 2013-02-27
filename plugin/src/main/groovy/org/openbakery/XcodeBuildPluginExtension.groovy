package org.openbakery

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal

import java.util.concurrent.Callable

class XcodeBuildPluginExtension {
	def String infoPlist = null
	def String scheme = null
	def String configuration = 'Debug'
	def String sdk = 'iphonesimulator'
	def target = null
	def String unitTestTarget = null
	def Object dstRoot
	def Object objRoot
	def Object symRoot
	def Object sharedPrecompsDir
	def String sourceDirectory = '.'
	def String signIdentity = null
	def additionalParameters = null
	def String bundleNameSuffix = null
	def String arch = null
	def String workspace = null

	private final Project project

	public XcodeBuildPluginExtension(Project project) {
		this.project = project;

		this.dstRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("dst")
		}

		this.objRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("obj")
		}

		this.symRoot = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("sym")
		}

		this.sharedPrecompsDir = {
			return project.getFileResolver().withBaseDir(project.getBuildDir()).resolve("shared")
		}

	}

	void setDstRoot(File dstRoot) {
		this.dstRoot = dstRoot
	}

	void setObjRoot(File objRoot) {
		this.objRoot = objRoot
	}

	void setSymRoot(File symRoot) {
		this.symRoot = symRoot
	}

	void setSharedPrecompsDir(File sharedPrecompsDir) {
		this.sharedPrecompsDir = sharedPrecompsDir
	}

	File getDstRoot() {
		return project.file(dstRoot)
	}

	File getObjRoot() {
		return project.file(objRoot)
	}

	File getSymRoot() {
		return project.file(symRoot)
	}

	File getSharedPrecompsDir() {
		return project.file(sharedPrecompsDir)
	}


}