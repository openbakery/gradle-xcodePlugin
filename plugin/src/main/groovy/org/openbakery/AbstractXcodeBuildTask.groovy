package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.util.ConfigureUtil
import org.openbakery.output.XcodeBuildOutputAppender
import org.openbakery.simulators.SimulatorControl
import org.openbakery.tools.DestinationResolver
import org.openbakery.tools.Xcode
import org.openbakery.tools.XcodebuildParameters

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
abstract class AbstractXcodeBuildTask extends AbstractXcodeTask {

	XcodebuildParameters parameters = new XcodebuildParameters()

	SimulatorControl simulatorControl
	DestinationResolver destinationResolver

	private List<Destination> destinationsCache

	AbstractXcodeBuildTask() {
		super()
	}


	void setTarget(String target) {
		parameters.target = target
	}

	void setScheme(String scheme) {
		parameters.scheme = scheme
	}

	void setSimulator(Boolean simulator) {
		parameters.simulator = simulator
	}

	void setType(Type type) {
		parameters.type = type
	}

	void setWorkspace(String workspace) {
		parameters.workspace = workspace
	}

	void setAdditionalParameters(String additionalParameters) {
		parameters.additionalParameters = additionalParameters
	}

	void setConfiguration(String configuration) {
		parameters.configuration = configuration
	}

	void setArch(List<String> arch) {
		parameters.arch = arch
	}

	void setConfiguredDestinations(Set<Destination> configuredDestination) {
		parameters.configuredDestinations = configuredDestination
	}

	void setDevices(Devices devices) {
		parameters.devices = devices
	}


	void destination(Closure closure) {
		parameters.destination(closure)
	}

	void setDestination(def destination) {
		parameters.setDestination(destination)
	}

	List<Destination> getDestinations() {
		if (destinationsCache == null) {
			destinationsCache = getDestinationResolver().getDestinations(parameters)
		}
		return destinationsCache
	}

	XcodeBuildOutputAppender createXcodeBuildOutputAppender(String name) {
		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(XcodeBuildTask.class, LogLevel.LIFECYCLE);
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class);
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(XcodeBuildTask.class).start(name, name);
		return new XcodeBuildOutputAppender(progressLogger, output)
	}
	DestinationResolver getDestinationResolver() {
		if (destinationResolver == null) {
			destinationResolver = new DestinationResolver(getSimulatorControl())
		}
		return destinationResolver
	}


	SimulatorControl getSimulatorControl() {
		if (simulatorControl == null) {
			simulatorControl = new SimulatorControl(this.commandRunner, xcode)
		}
		return simulatorControl
	}

}
