package org.openbakery

import com.sun.org.apache.xpath.internal.operations.Bool
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.util.ConfigureUtil
import org.openbakery.codesign.Security
import org.openbakery.output.XcodeBuildOutputAppender
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Type
import org.openbakery.xcode.XcodebuildParameters

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
abstract class AbstractXcodeBuildTask extends AbstractXcodeTask {

	@Internal
	XcodebuildParameters parameters = new XcodebuildParameters()


	private List<Destination> destinationsCache

	AbstractXcodeBuildTask() {
		super()
	}

	void setProjectFile(String projectFile) {
		parameters.projectFile = projectFile
	}

	@Input
	@Optional
	String getProjectFile() {
		return parameters.projectFile
	}

	void setTarget(String target) {
		parameters.target = target
	}

	@Input
	@Optional
	String getTarget() {
		return parameters.target
	}

	void setScheme(String scheme) {
		parameters.scheme = scheme
	}

	@Input
	@Optional
	String getScheme() {
		return parameters.scheme
	}

	void setSimulator(Boolean simulator) {
		parameters.simulator = simulator
	}

	@Input
	@Optional
	Boolean getSimulator() {
		return parameters.simulator
	}

	void setType(Type type) {
		parameters.type = type
	}

	@Input
	@Optional
	Type getType() {
		return parameters.type
	}

	void setWorkspace(String workspace) {
		parameters.workspace = workspace
	}

	@Input
	@Optional
	String getWorkspace() {
		return parameters.workspace
	}

	void setAdditionalParameters(def additionalParameters) {
		parameters.additionalParameters = additionalParameters
	}


	@Input
	@Optional
	List<String>getAdditionalParameters() {
		if (parameters.additionalParameters instanceof List) {
			return parameters.additionalParameters
		}
		return [parameters.additionalParameters.toString()]
	}

	void setConfiguration(String configuration) {
		parameters.configuration = configuration
	}

	@Input
	@Optional
	String getConfiguration() {
		return parameters.configuration
	}

	void setArch(List<String> arch) {
		parameters.arch = arch
	}

	@Input
	@Optional
	List<String> getArch() {
		return parameters.getArch()
	}

	void setConfiguredDestinations(Set<Destination> configuredDestination) {
		parameters.configuredDestinations = configuredDestination
	}

	@Input
	@Optional
	Set<Destination>getConfiguredDestinations() {
		return parameters.configuredDestinations
	}


	void destination(Closure closure) {
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		setDestination(destination)
	}

	void setDestination(def destination) {
		parameters.setDestination(destination)
	}

	@Internal
	List<Destination> getDestinations() {
		logger.debug("getDestinations parameters {}", parameters)

		if (destinationsCache == null) {
			destinationsCache = getDestinationResolver().getDestinations(parameters)
		}
		return destinationsCache
	}


	XcodeBuildOutputAppender createXcodeBuildOutputAppender(String name) {
		StyledTextOutput output = getServices().get(StyledTextOutputFactory.class).create(XcodeBuildTask.class, LogLevel.LIFECYCLE);
		ProgressLoggerFactory progressLoggerFactory = getServices().get(ProgressLoggerFactory.class) as ProgressLoggerFactory;
		ProgressLogger progressLogger = progressLoggerFactory.newOperation(XcodeBuildTask.class).start(name, name);
		return new XcodeBuildOutputAppender(progressLogger, output)
	}

	void setCodeCoverage(Boolean codeCoverage) {
		parameters.codeCoverage = codeCoverage
	}

	@Input
	@Optional
	String getCodeCoverage() {
		return parameters.codeCoverage
	}
}
