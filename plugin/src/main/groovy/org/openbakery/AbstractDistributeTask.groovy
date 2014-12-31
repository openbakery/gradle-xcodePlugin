package org.openbakery

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask

/**
 * User: rene
 * Date: 11/11/14
 */
class AbstractDistributeTask extends AbstractXcodeTask {

	def getAppBundleInfoPlist() {

		/*
		def convertedPlist = new File(project.buildDir, "Info.plist")
		if (convertedPlist.exists()) {
			return convertedPlist.absolutePath
		}

		File infoPlistFile = new File(project.xcodebuild.applicationBundle, "/Info.plist")
		if (infoPlistFile.exists()) {

			//plutil -convert xml1 "$BINARY_INFO_PLIST" -o "${INFO_PLIST}.plist"

			def convertCommand = [
							"plutil",
							"-convert",
							"xml1",
							infoPlistFile.absolutePath,
							"-o",
							convertedPlist.absolutePath
			]

			commandRunner.run(convertCommand)

			return convertedPlist.absolutePath
		}

		*/
		File infoPlist = new File(project.xcodebuild.applicationBundle, "Info.plist")
		if (!infoPlist.exists()) {
			throw new IllegalStateException("Info.plist not found: " + infoPlist.absolutePath);
		}
		return infoPlist.absolutePath;
	}



	File getDestinationFile(File outputDirectory, String extension) {
		if (project.xcodebuild.bundleNameSuffix != null) {
			return new File(outputDirectory, project.xcodebuild.bundleName + project.xcodebuild.bundleNameSuffix + extension)
		}
		return new File(outputDirectory, project.xcodebuild.bundleName + extension)

	}

	File getDestinationBundleFile(File outputDirectory, File bundle) {
		if (!bundle.exists()) {
			throw new IllegalArgumentException("cannot find bundle: " + bundle)
		}

		String name = bundle.getName();
		String extension = ""
		int index = name.indexOf(".")
		if (index > 0) {
			extension = name.substring(index);
		}

		return getDestinationFile(outputDirectory, extension)
	}

	void copyBundleToDirectory(File outputDirectory, File bundle) {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}



		File destinationBundle = getDestinationBundleFile(outputDirectory, bundle)
		FileUtils.copyFile(project.xcodebuild.getIpaBundle(), destinationBundle)

		logger.lifecycle("Created bundle archive in {}", outputDirectory)
	}

	void copyIpaToDirectory(File outputDirectory) {
		copyBundleToDirectory(outputDirectory, project.xcodebuild.getIpaBundle())
	}


	void copyDsymToDirectory(File outputDirectory) {
		copyBundleToDirectory(outputDirectory, project.xcodebuild.getDSymBundle())
	}

	void createDsymZip(File outputDirectory) {

		def ant = new AntBuilder()
		ant.zip(destfile: getDsymZipFile(outputDirectory).absolutePath,
				basedir: project.xcodebuild.getOutputPath().absolutePath,
				includes: project.xcodebuild.bundleName + ".app.dSYM*/**")

	}


	File getIpaFile(File outputDirectory) {
		return getDestinationBundleFile(outputDirectory, project.xcodebuild.getIpaBundle())
	}

	File getDsymZipFile(File outputDirectory) {
		String baseZipName;
		if (project.xcodebuild.bundleNameSuffix != null) {
			baseZipName = project.xcodebuild.bundleName + project.xcodebuild.bundleNameSuffix
		} else {
			baseZipName = project.xcodebuild.bundleName
		}

		return new File(outputDirectory.path, baseZipName + "." + project.xcodebuild.productType + ".dSYM.zip")
	}
}
