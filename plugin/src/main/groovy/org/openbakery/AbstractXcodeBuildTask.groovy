package org.openbakery

import groovy.transform.TypeChecked
import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.progress.ProgressLogger
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.gradle.util.ConfigureUtil
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.output.XcodeBuildOutputAppender
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Devices
import org.openbakery.xcode.Type
import org.openbakery.xcode.XcodebuildParameters

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * User: rene
 * Date: 15.07.13
 * Time: 11:57
 */
@TypeChecked
abstract class AbstractXcodeBuildTask extends AbstractXcodeTask {

	XcodebuildParameters parameters = new XcodebuildParameters()

	private List<Destination> destinationsCache

	private static final Pattern PATTERN = ~/^\s{4}friendlyName:\s(?<friendlyName>[^\n]+)/

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
		Destination destination = new Destination()
		ConfigureUtil.configure(closure, destination)
		setDestination(destination)
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

	XcodeBuildPluginExtension getXcodeExtension() {
		return project.getExtensions()
				.getByType(XcodeBuildPluginExtension.class)
	}

	String getBundleIdentifier() {
		File infoPlist = new File(project.projectDir, getXcodeExtension().infoPlist)
		return plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier")
	}

	InfoPlistExtension getInfoPlistExtension() {
		return project.getExtensions().getByType(InfoPlistExtension.class)
	}

	File getProvisioningFile() {
		List<File> provisioningList = getProvisioningUriList()
				.collect { it -> new File(new URI(it)) }

		println "provisioningList : " + provisioningList

		return Optional.ofNullable(ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier,
				provisioningList,
				commandRunner,
				plistHelper)).orElseThrow {
			new IllegalArgumentException("Cannot resolve a valid provisioning " +
					"profile for bundle identifier : " + bundleIdentifier)
		}
	}

	List<String> getProvisioningUriList() {
		return getXcodeExtension().signing.mobileProvisionURI
	}

	String getSignatureFriendlyName() {
		return Optional.ofNullable(getKeyContent()
				.split(System.getProperty("line.separator"))
				.find { PATTERN.matcher(it).matches() })
				.map { PATTERN.matcher(it) }
				.filter { Matcher it -> it.matches() }
				.map { Matcher it ->
			return it.group("friendlyName")
		}
		.orElseThrow {
			new IllegalArgumentException("Failed to resolve the code signing identity from the certificate ")
		}
	}

	private String getKeyContent() {
		final String certificatePassword = getCertificatePassword()
		File file = xcodeExtension.signing
				.certificate
				.get()
				.asFile

		return commandRunner.runWithResult(["openssl",
											"pkcs12",
											"-nokeys",
											"-in",
											file.absolutePath,
											"-passin",
											"pass:" + certificatePassword])
	}

	private File getCertificateFile() {
		return new File(URI.create(getCertificateString()))
	}

	private String getCertificateString() throws IllegalArgumentException {
//		return Optional.ofNullable(getXcodeExtension().signing.certificate)
//				.filter { String it -> it != null && !it.isEmpty() }
//				.orElseThrow {
//			new IllegalArgumentException("The certificateURI value is null or empty : " + getXcodeExtension().signing.certificateURI)
//		}

		return getXcodeExtension()
				.signing
				.certificate
	}

	private String getCertificatePassword() {
		return Optional.ofNullable(getXcodeExtension().signing.certificatePassword.getOrNull())
				.filter { it != null && !it.isEmpty() }
				.orElseThrow {
			new IllegalArgumentException("The signing certificate password is not defined")
		}
	}
}
