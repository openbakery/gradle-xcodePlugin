package org.openbakery

import org.gradle.api.tasks.TaskAction
import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import javax.imageio.ImageIO

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.12
 * Time: 22:30
 * To change this template use File | Settings | File Templates.
 */
class HockeykitKitImageTask extends AbstractHockeykitTask {



    @TaskAction
    def imageCreate() {
        def infoplist = getInfoPlist()
        println infoplist
        XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(infoplist));
        def list = config.getList("CFBundleIconFiles");
        TreeMap<Integer, String> iconMap = new TreeMap<Integer, String>()
        list.each {
            item ->
            println item;

            def image = ImageIO.read(new File(item));
            iconMap.put(image.width, item);
        }
        println iconMap;
        def outputDirectory = getOutputDirectory();

        def selectedImage = iconMap.get(114)
        if (selectedImage == null) {
            iconMap.lastEntry();

        }


    }

}
