package org.openbakery


import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import javax.imageio.ImageIO
import org.apache.commons.io.FileUtils
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import org.gradle.api.tasks.TaskAction

class HockeyKitImageTask extends AbstractHockeykitTask {

    private static final int IMAGE_WIDTH = 114

    def resizeImage(File fromImage, toImage) {
        def image = ImageIO.read( fromImage)

        new BufferedImage( IMAGE_WIDTH, IMAGE_WIDTH, image.type ).with { i ->
            createGraphics().with {
                setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC )
                drawImage( image, 0, 0, HockeyKitImageTask.IMAGE_WIDTH, HockeyKitImageTask.IMAGE_WIDTH, null )
                dispose()
            }
            ImageIO.write( i, 'png', new File(toImage) )
        }
    }

    @TaskAction
    def imageCreate() {
        def infoplist = getAppBundleInfoPlist()
        println infoplist
        XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(infoplist))
        def list = config.getList("CFBundleIconFiles")
        if (list.isEmpty()) {
            list = config.getList("CFBundleIcons.CFBundlePrimaryIcon.CFBundleIconFiles")
        }
        TreeMap<Integer, String> iconMap = new TreeMap<Integer, String>()
        list.each {
            item ->
            try {
               def image
					File iconFile;

					// get fileName of info.plist in project / target
					def infoPlist = getInfoPlist()
					def infoPlistFile = new File(infoPlist)

					// get path from info.plist
					String absolutePath = infoPlistFile.getAbsolutePath();
					String appPath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));

					if (project.infoplist.iconPath) {

						// appPath + additional iconPath + name of iconFile
						iconFile = new File(appPath + File.separator + project.infoplist.iconPath + File.separator + item)

					} else {

						// appPath + additional iconPath + name of iconFile
						iconFile = new File(appPath + File.separator + item)
					}

					image = ImageIO.read(iconFile)

					iconMap.put(image.width, iconFile)
            } catch (Exception ex) {
                println "Cannot read image " + item + " so ignore it";
            }
        }
        println "Images to choose from: " + iconMap
        def outputDirectory = new File(getOutputDirectory()).getParent()

        def selectedImage = iconMap.get(114)

        def outputImageFile = new File(outputDirectory, "Icon.png")
        if (selectedImage != null) {
            println "Copy file " + selectedImage + " to " + outputImageFile
            FileUtils.copyFile(new File(selectedImage), outputImageFile)
        } else {
            selectedImage = iconMap.lastEntry().value
            println "Resize file " + selectedImage + " to " + outputImageFile
            resizeImage(selectedImage, outputImageFile.absolutePath)
        }

    }

}
