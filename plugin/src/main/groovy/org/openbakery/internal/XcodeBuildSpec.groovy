package org.openbakery.internal

import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.openbakery.CommandRunner
import org.openbakery.Devices
import org.openbakery.VariableResolver
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 14.08.15.
 */
class XcodeBuildSpec {

	private static Logger logger = LoggerFactory.getLogger(XcodeBuildSpec.class)


	String version
	String target
	String scheme
	String configuration
	String sdk
	String ipaFileName
	String workspace
	Object symRoot
	Object dstRoot
	Object objRoot
	Object sharedPrecompsDir
	String productName
	Devices devices
	String infoPlist
	String productType
	String bundleName
	String bundleNameSuffix



	private XcodeBuildSpec parent = null
	private CommandRunner commandRunner = new CommandRunner()
	private VariableResolver variableResolver
	private Project project

	public XcodeBuildSpec(Project project) {
		this(project, null)
	}

	public XcodeBuildSpec(Project project, XcodeBuildSpec parent) {
		this.project = project
		this.parent = parent
		this.variableResolver = new VariableResolver(project.projectDir, this)
	}

	String getVersion() {
		if (!StringUtils.isEmpty(this.version)) {
			return this.version
		}
		if (this.parent != null) {
			return this.parent.version
		}
		return null
	}

	String getTarget() {
		if (!StringUtils.isEmpty(this.target)) {
			return this.target
		}
		if (this.parent != null) {
			return this.parent.getTarget()
		}
		return null
	}

	String getScheme() {
		if (!StringUtils.isEmpty(this.scheme)) {
			return this.scheme
		}
		if (this.parent != null) {
			return this.parent.scheme
		}
		return null
	}

	boolean isSdk(String expectedSDK) {
		return getSdk().toLowerCase().startsWith(expectedSDK)
	}

	String getSdk() {
		if (!StringUtils.isEmpty(this.sdk)) {
			return this.sdk
		}

		if (this.parent != null) {
			return this.parent.getSdk()
		}

		return XcodePlugin.SDK_IPHONESIMULATOR
	}


	String getConfiguration() {
		if (!StringUtils.isEmpty(this.configuration)) {
			return this.configuration
		}
		if (this.parent != null) {
			return this.parent.configuration
		}
		return 'Debug'
	}

	String getIpaFileName() {
		if (!StringUtils.isEmpty(this.ipaFileName)) {
			return this.ipaFileName
		}
		if (this.parent != null) {
			return this.parent.ipaFileName
		}
		return null
	}

	File getOutputPath() {

		if (this.isSdk(XcodePlugin.SDK_MACOSX)) {
			return new File(getSymRoot(), getConfiguration())
		}
		return new File(getSymRoot(), getConfiguration() + "-" + getSdk())
	}

	String getWorkspace() {
		if (workspace != null) {
			return workspace
		}

		if (parent != null) {
			return parent.getWorkspace();
		}

		if (project.projectDir != null) {

			String[] fileList = project.projectDir.list(new SuffixFileFilter(".xcworkspace"))
			if (fileList.length) {
				return fileList[0]
			}
		}
		return null
	}


	File getSymRoot() {
		if (this.symRoot != null) {
			return project.file(this.symRoot)
		}
		if (this.parent != null) {
			return this.parent.getSymRoot()
		}
		return this.project.getFileResolver().withBaseDir(this.project.getBuildDir()).resolve("sym")
	}


	File getDstRoot() {
		if (this.dstRoot != null) {
			return project.file(this.dstRoot)
		}
		if (this.parent != null) {
			return this.parent.getDstRoot()
		}
		return this.project.getFileResolver().withBaseDir(this.project.getBuildDir()).resolve("dst")
	}

	File getObjRoot() {
		if (this.objRoot != null) {
			return project.file(this.objRoot)
		}
		if (this.parent != null) {
			return this.parent.getObjRoot()
		}
		return this.project.getFileResolver().withBaseDir(this.project.getBuildDir()).resolve("obj")
	}

	File getSharedPrecompsDir() {
		if (this.sharedPrecompsDir != null) {
			return project.file(this.sharedPrecompsDir)
		}
		if (this.parent != null) {
			return this.parent.getSharedPrecompsDir()
		}
		return this.project.getFileResolver().withBaseDir(this.project.getBuildDir()).resolve("shared")
	}



	Devices getDevices() {
		if (this.devices != null) {
			return devices
		}
		if (this.parent != null) {
			return this.parent.devices
		}
		return Devices.UNIVERSAL
	}

	String getProductName() {
		if (!StringUtils.isEmpty(this.productName)) {
			return this.productName
		}
		if (this.parent != null) {
			return this.parent.productName
		}
		return null
	}

	String getInfoPlist() {
		if (!StringUtils.isEmpty(this.infoPlist)) {
			return this.infoPlist
		}
		if (this.parent != null) {
			return this.parent.infoPlist
		}
		return null
	}

	File getInfoPlistFile() {
		return new File(project.projectDir, getInfoPlist())
	}


	String getProductType() {
		if (!StringUtils.isEmpty(this.productType)) {
			return this.productType
		}
		if (this.parent != null) {
			return this.parent.getProductType()
		}
		return "app"
	}

	// TODO: replace this with the PListHelper
	String getValueFromInfoPlist(key) {
		try {
			File infoPlistFile = getInfoPlistFile()
			if (infoPlistFile.exists()) {
				logger.debug("get value {} from plist file {}", key, infoPlistFile)
				return commandRunner.runWithResult([
								"/usr/libexec/PlistBuddy",
								infoPlistFile.absolutePath,
								"-c",
								"Print :" + key])
			}
		} catch (IllegalStateException ex) {
			// ignore, null is retured
		}
		return null
	}


	String getBundleName() {
		if (!StringUtils.isEmpty(this.bundleName)) {
			return this.variableResolver.resolve(this.bundleName)
		}
		if (this.parent != null) {
			return this.parent.getBundleName()
		}
		String name = getValueFromInfoPlist("CFBundleName")
		if (!StringUtils.isEmpty(name)) {
			return this.variableResolver.resolve(name)
		}
		return this.productName
	}


	File getApplicationBundle() {
		return new File(this.getOutputPath(), getBundleName() + "." + getProductType())
	}

	String getBundleNameSuffix() {
		if (!StringUtils.isEmpty(this.bundleNameSuffix)) {
			return this.bundleNameSuffix
		}
		if (this.parent != null) {
			return this.parent.bundleNameSuffix
		}
		return null
	}

}
