package org.openbakery

class AbstractHockeykitTask extends AbstractXcodeTask {

	/**
	 * Method to get the destination directory where the output of the generated files for hockeykit should be stored.
	 *
	 * @return the output directory as absolute path
	 */
	def getOutputDirectory() {
		def infoplist = getAppBundleInfoPlist()
		def bundleIdentifier = getValueFromPlist(infoplist, "CFBundleIdentifier")
		File outputDirectory = new File(project.hockeykit.outputDirectory, bundleIdentifier + "/" + project.hockeykit.versionDirectoryName)
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}
		return outputDirectory.absolutePath
	}
}
