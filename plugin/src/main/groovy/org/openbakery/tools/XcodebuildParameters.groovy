package org.openbakery.tools

import org.openbakery.Destination
import org.openbakery.Devices
import org.openbakery.Type
import org.openbakery.XcodeBuildPluginExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 04.08.16.
 */
class XcodebuildParameters {
	private static Logger logger = LoggerFactory.getLogger(XcodebuildParameters.class)

	String scheme
	String target
	boolean simulator
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
	List<Destination> allDestinations
	Devices devices




	public XcodebuildParameters() {
	}

	public XcodebuildParameters(XcodeBuildPluginExtension extension) {
		scheme = extension.scheme
		target = extension.target
		simulator = extension.simulator
		type = extension.type
		workspace = extension.workspace
		configuration = extension.configuration
		dstRoot = extension.dstRoot
		objRoot = extension.objRoot
		symRoot = extension.symRoot
		sharedPrecompsDir = extension.sharedPrecompsDir
		derivedDataPath = extension.derivedDataPath
		if (extension.arch != null) {
			arch = extension.arch.clone()
		}
		additionalParameters = extension.additionalParameters
		devices = extension.devices
		allDestinations = extension.getAllDestinations()
		configuredDestinations = extension.destinations
	}


	boolean isSimulatorBuildOf(Type expectedType) {
		if (type != expectedType) {
			logger.debug("is no simulator build")
			return false;
		}
		logger.debug("is simulator build {}", this.simulator)
		return this.simulator;
	}


	List<Destination> getDestinations() {

			logger.debug("getAvailableDestinations")
			def availableDestinations = []


			if (type == Type.OSX) {
				availableDestinations << new Destination("OS X", "OS X", "10.x")
				return availableDestinations
			}

			if (isSimulatorBuildOf(Type.iOS)) {
				// filter only on simulator builds

				logger.debug("is a simulator build")
				if (this.configuredDestinations != null) {

					logger.debug("checking destinations if they are available: {}", this.configuredDestinations)
					for (Destination destination in this.configuredDestinations) {
						availableDestinations.addAll(findMatchingDestinations(destination))
					}

					if (availableDestinations.isEmpty()) {
						logger.error("No matching simulators found for specified destinations: {}", this.configuredDestinations)
						throw new IllegalStateException("No matching simulators found!")
					}
				} else {

					logger.info("There was no destination configured that matches the available. Therefor all available destinations where taken.")


					switch (this.devices) {
						case Devices.PHONE:
							availableDestinations = allDestinations.findAll {
								d -> d.name.contains("iPhone");
							};
							break;
						case Devices.PAD:
							availableDestinations = allDestinations.findAll {
								d -> d.name.contains("iPad");
							};
							break;
						default:
							availableDestinations.addAll(allDestinations);
							break;
					}
				}
			} else if (this.configuredDestinations != null) {
				logger.debug("is a device build so add all given device destinations")
				// on the device build add the given destinations
				availableDestinations.addAll(this.configuredDestinations)
			}


			logger.debug("availableDestinations: " + availableDestinations);

			return availableDestinations
		}

	List<Destination> findMatchingDestinations(Destination destination) {
		def result = [];


		logger.debug("finding matching destination for: {}", destination)

		for (Destination device in allDestinations) {
			if (!matches(destination.platform, device.platform)) {
				//logger.debug("{} does not match {}", device.platform, destination.platform);
				continue
			}
			if (!matches(destination.name, device.name)) {
				//logger.debug("{} does not match {}", device.name, destination.name);
				continue
			}
			if (!matches(destination.arch, device.arch)) {
				//logger.debug("{} does not match {}", device.arch, destination.arch);
				continue
			}
			if (!matches(destination.id, device.id)) {
				//logger.debug("{} does not match {}", device.id, destination.id);
				continue
			}
			if (!matches(destination.os, device.os)) {
				//logger.debug("{} does not match {}", device.os, destination.os);
				continue
			}

			logger.debug("FOUND matching destination: {}", device)

			result << device

		}


		return result.asList();
	}


	boolean matches(String first, String second) {
		if (first != null && second == null) {
			return true;
		}

		if (first == null && second != null) {
			return true;
		}

		if (first.equals(second)) {
			return true;
		}

		if (second.matches(first)) {
			return true;
		}

		return false;

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
						", destinations=" + getDestinations() +
						'}';
	}
}
