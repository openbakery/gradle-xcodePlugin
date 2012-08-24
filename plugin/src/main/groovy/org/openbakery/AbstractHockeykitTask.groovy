package org.openbakery

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.12
 * Time: 22:52
 * To change this template use File | Settings | File Templates.
 */
class AbstractHockeykitTask extends AbstractXcodeTask {

    def getOutputDirectory() {
        File outputDirectory = new File(project.hockeykit.outputDirectory + "/" + bundleIdentifier + "/" + project.hockeykit.version);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }
        return outputDirectory.absolutePath
    }
}
