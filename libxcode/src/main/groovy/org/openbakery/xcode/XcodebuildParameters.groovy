package org.openbakery.xcode

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 04.08.16.
 */
class XcodebuildParameters {

	private static Logger logger = LoggerFactory.getLogger(XcodebuildParameters.class)


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
	Devices devices
	List<File> xctestrun


	public XcodebuildParameters() {
	}

	@Override
	public String toString() {
		return "XcodebuildParameters {" +
						", scheme='" + scheme + '\'' +
						", target='" + target + '\'' +
						", simulator=" + simulator +
						", type=" + type +
						", workspace='" + workspace + '\'' +
						", configuration='" + configuration + '\'' +
						", dstRoot=" + dstRoot +
						", objRoot=" + objRoot +
						", symRoot=" + symRoot +
						", sharedPrecompsDir=" + sharedPrecompsDir +
						", derivedDataPath=" + derivedDataPath +
						", arch=" + arch +
						", additionalParameters=" + additionalParameters +
						", configuredDestinations=" + configuredDestinations +
						", xctestrun=" + xctestrun +
						'}';
	}


	XcodebuildParameters merge(XcodebuildParameters other) {
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
		if (other.devices != null) {
			devices = other.devices
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
		newDestination.name = destination.toString()
		configuredDestinations << newDestination
	}

	boolean isSimulatorBuildOf(Type expectedType) {
		if (this.type != expectedType) {
			logger.debug("is no simulator build")
			return false;
		}
		logger.debug("is simulator build {}", this.simulator)
		return this.simulator;
	}
}
