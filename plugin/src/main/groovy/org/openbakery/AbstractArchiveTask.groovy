package org.openbakery

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask

/**
 * User: rene
 * Date: 11/11/14
 */
class AbstractArchiveTask extends AbstractXcodeTask {

	void copyBundleToDirectory(File outputDirectory, File bundle) {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}

		if (!bundle.exists()) {
			throw new IllegalArgumentException("cannot find bundle: " + bundle)
		}

		String name = bundle.getName();
		String extension = ""
		int index = name.indexOf(".")
		if (index > 0) {
			extension = name.substring(index);
		}

		File destinationBundle
		if (project.xcodebuild.bundleNameSuffix != null) {
			destinationBundle = new File(outputDirectory, project.xcodebuild.productName + project.xcodebuild.bundleNameSuffix + extension)
		} else {
			destinationBundle = new File(outputDirectory, project.xcodebuild.productName + extension)
		}
		FileUtils.copyFile(project.xcodebuild.getIpaBundle(), destinationBundle)


		logger.lifecycle("Created {} archive in {}", extension, outputDirectory)
	}

	void copyIpaToDirectory(File outputDirectory) {
		copyBundleToDirectory(outputDirectory, project.xcodebuild.getIpaBundle())
	}


	void copyDsymToDirectory(File outputDirectory) {
		copyBundleToDirectory(outputDirectory, project.xcodebuild.getDSymBundle())
	}
}
