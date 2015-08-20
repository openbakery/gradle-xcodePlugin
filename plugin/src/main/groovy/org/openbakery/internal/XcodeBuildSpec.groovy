package org.openbakery.internal

import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.openbakery.CommandRunner
import org.openbakery.Devices
import org.openbakery.PlistHelper
import org.openbakery.VariableResolver
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.XcodePlugin
import org.openbakery.signing.Signing
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
	List<String> additionalParameters
	List<String> arch
	Signing signing
	Map<String, String> environment = null



	private XcodeBuildSpec parent = null
	private CommandRunner commandRunner = new CommandRunner()
	private VariableResolver variableResolver
	private Project project
	private PlistHelper plistHelper

	public XcodeBuildSpec(Project project) {
		this(project, null)
	}

	public XcodeBuildSpec(Project project, XcodeBuildSpec parent) {
		this.project = project
		this.parent = parent
		this.variableResolver = new VariableResolver(project.projectDir, this)
		this.plistHelper = new PlistHelper(project, commandRunner)
		if (parent != null) {
			this.signing = new Signing(project, parent.signing)
		} else {
			this.signing = new Signing(project)
		}
	}

	void signing(Closure closure) {
		ConfigureUtil.configure(closure, this.signing)
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
		String infoPlist = getInfoPlist()
		if (infoPlist) {
			return new File(project.projectDir, getInfoPlist())
		}
		return null;
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

	String getValueFromInfoPlist(key) {
		File infoPlist =  getInfoPlistFile()
		if (infoPlist != null) {
			return plistHelper.getValueFromPlist(getInfoPlistFile(), key)
		}
		return null
		/*
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
		*/
	}


	String getBundleName() {
		if (!StringUtils.isEmpty(this.bundleName)) {
			return this.variableResolver.resolve(this.bundleName)
		}
		if (this.parent != null) {
			String parentBundleName = this.parent.getBundleName()
			if (!StringUtils.isEmpty(parentBundleName)) {
				return parentBundleName
			}
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


	List<String>getAdditionalParameters() {
		if (this.additionalParameters != null) {
			return this.additionalParameters
		}
		if (this.parent != null) {
			return this.parent.additionalParameters
		}
		return null
	}

	void setAdditionalParameters(Object parameters) {
		if (parameters instanceof List) {
			this.additionalParameters = parameters;
		} else {
			this.additionalParameters = []
			this.additionalParameters << parameters.toString()
		}
	}

	List<String>getArch() {
		if (this.arch != null) {
			return this.arch
		}
		if (this.parent != null) {
			return this.parent.arch
		}
		return null
	}

	void setArch(Object parameters) {
		if (parameters instanceof List) {
			this.arch = parameters;
		} else {
			this.arch = []
			this.arch << parameters.toString()
		}
	}



	void setEnvironment(Object parameters) {

		if (parameters instanceof Map) {
			this.environment = new LinkedHashMap<String, String>();
			for (Map.Entry entry : parameters) {
				// convert to a String, String map
				this.environment.put(entry.key.toString(), entry.value.toString())
			}
		} else {
			logger.debug("environment is string: " + environment)
			this.environment = new LinkedHashMap<String, String>();

			String environmentString = parameters.toString()
			int index = environmentString.indexOf("=")
			if (index > 0) {
				this.environment.put(environmentString.substring(0, index),environmentString.substring(index + 1))
			}
		}
	}


	Map<String, String>getEnvironment() {
		if (this.environment != null) {
			return environment
		}
		if (this.parent != null) {
			return this.parent.environment
		}
		return null
	}


	void with(XcodeBuildSpec newParent) {
		logger.debug("new parent: {}\n\n", newParent)
		if (newParent == this) {
			throw new IllegalArgumentException("self cannot be a parent of itself")
		}

		if (this.parent != null) {
			newParent.with(this.parent)
		}
		this.parent = newParent
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("XcodeBuildSpec{");
		if (target != null) {
			builder.append(" target='" + target + '\'')
		}
		if (scheme != null) {
			builder.append(" scheme='" + scheme + '\'')
		}
		if (configuration != null) {
			builder.append(" configuration='" + configuration + '\'')
		}
		if (sdk != null) {
			builder.append(" sdk='" + sdk + '\'')
		}
		if (workspace != null) {
			builder.append(" workspace='" + workspace + '\'')
		}
		if (productName != null) {
			builder.append(" productName='" + productName + '\'')
		}
		if (devices != null) {
			builder.append(" devices=" + devices)
		}
		if (arch != null) {
			builder.append(" arch=" + arch)
		}
		if (signing != null) {
			builder.append(" signing=" + signing)
		}
		if (environment != null) {
			builder.append(" environment=" + environment)
		}
		if (additionalParameters != null) {
			builder.append(" additionalParameters=" + additionalParameters)
		}
		if (ipaFileName != null) {
			builder.append(" ipaFileName='" + ipaFileName + '\'')
		}
		if (productType != null) {
			builder.append(" productType='" + productType + '\'')
		}
		if (bundleName != null) {
			builder.append(" bundleName='" + bundleName + '\'')
		}
		if (bundleNameSuffix != null) {
			builder.append(" bundleNameSuffix='" + bundleNameSuffix + '\'')
		}
		if (parent != null) {
			builder.append(" parent=" + parent)
		}
		builder.append('}')
		return builder.toString()
	}
}
