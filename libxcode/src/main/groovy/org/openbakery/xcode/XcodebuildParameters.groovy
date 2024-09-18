package org.openbakery.xcode

import groovy.json.JsonSlurper
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class XcodebuildParameters {

	private static Logger logger = LoggerFactory.getLogger(XcodebuildParameters.class)

	String projectFile
	String scheme
	String target
	Boolean simulator
	Type type
	String workspace
	String configuration
	File dstRoot
	File objRoot
	File symRoot
	File sharedPrecompsDir
	File derivedDataPath
	List<String> arch
	def additionalParameters
	Set<Destination> configuredDestinations
	List<File> xctestrun
	Boolean bitcode
	Boolean codeCoverage
	File applicationBundle


	public XcodebuildParameters() {
	}

	@Override
	public String toString() {
		return "XcodebuildParameters {" +
						", projectFile='" + projectFile + '\'' +
						", scheme='" + scheme + '\'' +
						", target='" + target + '\'' +
						", simulator=" + simulator +
						", type=" + type +
						", workspace='" + workspace + '\'' +
						", configuration='" + configuration + '\'' +
						", bitcode=" + bitcode +
						", dstRoot=" + dstRoot +
						", objRoot=" + objRoot +
						", symRoot=" + symRoot +
						", sharedPrecompsDir=" + sharedPrecompsDir +
						", derivedDataPath=" + derivedDataPath +
						", arch=" + arch +
						", additionalParameters=" + additionalParameters +
						", configuredDestinations=" + configuredDestinations +
						", xctestrun=" + xctestrun +
						", applicationBundle=" + applicationBundle +
						'}'
	}


	XcodebuildParameters merge(XcodebuildParameters other) {

		if (other.projectFile != null) {
			projectFile = other.projectFile
		}
		if (other.target != null) {
			target = other.target
		}
		if (other.scheme != null) {
			scheme = other.scheme
		}
		if (other.simulator != null) {
			simulator = other.simulator
		}
		if (other.type != null) {
			type = other.type
		}
		if (other.workspace != null) {
			workspace = other.workspace
		}
		if (other.additionalParameters != null) {
			additionalParameters = other.additionalParameters
		}
		if (other.configuration != null) {
			configuration = other.configuration
		}
		if (other.arch != null) {
			arch = other.arch
		}
		if (other.configuredDestinations != null) {
			configuredDestinations = other.configuredDestinations
		}
		if (other.bitcode != null) {
			bitcode = other.bitcode
		}
		if (other.codeCoverage != null) {
			codeCoverage = other.codeCoverage
		}
		if (other.applicationBundle != null) {
			applicationBundle = other.applicationBundle
		}

		return this
	}



	void setDestination(def destination) {

		if (destination instanceof List) {
			configuredDestinations = [] as Set
			destination.each { singleDestination ->
				setDestination(singleDestination)
			}
			return
		}

		if (configuredDestinations == null) {
			configuredDestinations = [] as Set
		}

		if (destination instanceof Destination) {
			configuredDestinations << destination
			return
		}

		def newDestination = new Destination()

		try {
			def json = new JsonSlurper()
			def object = json.parseText(destination.toString())
			if (object instanceof Map) {
				newDestination.name = object["name"]
				newDestination.os = object["os"]
			}
		} catch (Exception ignored) {
			newDestination.name = destination.toString()
		}
		configuredDestinations << newDestination
	}

	boolean isSimulatorBuildOf(Type expectedType) {
		logger.debug("type: {}", this.type)
		if (this.type != expectedType) {
			logger.debug("is no simulator build")
			return false
		}
		logger.debug("is simulator build {}", this.simulator)
		return this.simulator
	}

	String getApplicationBundleName() {
		return applicationBundle.name
	}

	String getBundleName() {
		return FilenameUtils.getBaseName(getApplicationBundleName())
	}

	File getOutputPath() {
		if (type == Type.iOS) {
			if (simulator) {
				return new File(getSymRoot(), "${configuration}-iphonesimulator")
			} else {
				return new File(getSymRoot(), "${configuration}-iphoneos")
			}
		}
		return new File(getSymRoot(), configuration)
	}

	File getWatchOutputPath() {
		if (type == Type.iOS) {
			if (simulator) {
				return new File(getSymRoot(), "${configuration}-watchosimulator")
			} else {
				return new File(getSymRoot(), "${configuration}-watchos")
			}
		}
		return new File(getSymRoot(), configuration)
	}

}
