package org.openbakery

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.12
 * Time: 22:52
 * To change this template use File | Settings | File Templates.
 */
class AbstractHockeykitTask extends AbstractXcodeTask {

    /**
     * Method to get the destination directory where the output of the generated files for hockeykit should be stored.
     *
     * @return the output directory as absolute path
     */
    def getOutputDirectory() {
        def infoplist = getAppBundleInfoPlist()
        def bundleIdentifier = getValueFromPlist(infoplist, "CFBundleIdentifier")
        File outputDirectory = new File(project.hockeykit.outputDirectory + "/" + bundleIdentifier + "/" + project.hockeykit.version);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        return outputDirectory.absolutePath
    }
}
