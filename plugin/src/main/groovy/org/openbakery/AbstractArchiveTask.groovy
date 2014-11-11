package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask

/**
 * User: rene
 * Date: 11/11/14
 */
class AbstractArchiveTask extends AbstractXcodeTask {

	void copyIpaToDirectory(File outputDirectory) {

		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		if (!project.xcodebuild.getIpaBundle().exists()) {
			throw new IllegalArgumentException("cannot find ipa: " + project.xcodebuild.getIpaBundle())
		}

		File destinationIpa
		if (project.xcodebuild.bundleNameSuffix != null) {
			destinationIpa = new File(outputDirectory, project.xcodebuild.productName + project.xcodebuild.bundleNameSuffix + ".ipa")
		} else {
			destinationIpa = new File(outputDirectory, project.xcodebuild.productName + ".ipa")
		}
		FileUtils.copyFile(project.xcodebuild.getIpaBundle(), destinationIpa)


		logger.lifecycle("Created ipa archive in {}", outputDirectory)
	}
}
