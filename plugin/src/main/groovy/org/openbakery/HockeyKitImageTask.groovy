package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.apache.commons.io.FilenameUtils
import javax.imageio.ImageIO

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.12
 * Time: 17:10
 * To change this template use File | Settings | File Templates.
 */
class HockeyKitImageTask extends AbstractXcodeTask {

    HockeyKitImageTask() {
        super();
    }

    @TaskAction
    def createImage() {
        def appName = getAppBundleName()
        def basename = FilenameUtils.getBaseName(appName);
        def infoPlist = getAppBundleInfoPlist()
        def icons = getValueFromPlist(infoPlist, "CFBundleIconFiles")
        icons.each {
            icon ->
            println icon
            //def img = ImageIO.read(new File(icon));
            //println("Width:"+img.getWidth()+" Height:"+img.getHeight());

        }

    }
}
