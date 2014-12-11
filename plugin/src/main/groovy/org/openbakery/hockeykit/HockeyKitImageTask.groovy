/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openbakery.hockeykit


import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.openbakery.CommandRunner

import javax.imageio.ImageIO
import org.apache.commons.io.FileUtils
import java.awt.image.BufferedImage
import java.awt.RenderingHints
import org.gradle.api.tasks.TaskAction

class HockeyKitImageTask extends AbstractHockeyKitTask {

	private static final int IMAGE_WIDTH = 114

	public HockeyKitImageTask() {
		super()
		dependsOn("hockeykit-archive")
		this.description = "Creates the image that is used on the HockeyKit Server"
	}

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

	File uncrush(File iconFile) {

		logger.debug("uncrush icon {}", iconFile);
		//xcrun -sdk iphoneos pngcrush -revert-iphone-optimizations infile.png outfile.png

		def uncrushCommandList = [
			project.xcodebuild.xcrunCommand,
			"-sdk",
			"iphoneos",
			"pngcrush",
			"-revert-iphone-optimizations"
		]

		uncrushCommandList.add(iconFile.path);

		File outputFile = new File(project.hockeykit.outputDirectory, iconFile.getName());
		uncrushCommandList.add(outputFile.path);

		CommandRunner runner = new CommandRunner();
		runner.run(uncrushCommandList);
		return outputFile;
	}



	@TaskAction
	def imageCreate() {
		def infoplist = getAppBundleInfoPlist()
		logger.debug("infoplist: {}", infoplist)


		def iconKeys = [
						"CFBundleIconFiles",
						"CFBundleIcons:CFBundlePrimaryIcon:CFBundleIconFiles",
						"CFBundleIcons~ipad:CFBundlePrimaryIcon:CFBundleIconFiles"
		]

		ArrayList<String> iconList = new ArrayList<String>();
		for (String key : iconKeys) {
			def value = getValueFromPlist(infoplist, key);
			if (value != null) {
				iconList.addAll(value)
			}
		}



		File iconFile;
		TreeMap<Integer, String> iconMap = new TreeMap<Integer, String>()
		iconList.each {
			item ->
				try {
					def image

					String extension = FilenameUtils.getExtension(item);
					if (StringUtils.isEmpty(extension)) {
						iconFile = new File(project.xcodebuild.applicationBundle, item + "@2x.png");
						if (!iconFile.exists()) {
							iconFile = new File(project.xcodebuild.applicationBundle, item + ".png");
						}
					} else {
						iconFile = new File(project.xcodebuild.applicationBundle, item)
					}


					if (iconFile.exists()) {
						logger.debug("try to read iconFile: {}", iconFile)

						File uncrushedIconFile = uncrush(iconFile);

						image = ImageIO.read(uncrushedIconFile)

						iconMap.put(image.width, uncrushedIconFile)
					}
				} catch (Exception ex) {
					logger.error("Cannot read image {}, ", iconFile)
				}
		}
		logger.debug("Images to choose from: {}", iconMap)
		def outputDirectory = getOutputDirectory().getParent()

		def selectedImage = iconMap.get(114)

		def outputImageFile = new File(outputDirectory, "Icon.png")
		if (selectedImage != null) {
			logger.debug("Copy file {} to {}", selectedImage, outputImageFile)
			FileUtils.copyFile(selectedImage, outputImageFile)
		} else {
			if (iconMap.size() > 0) {
				selectedImage = iconMap.lastEntry().value
				logger.debug("Resize file {} to {}", selectedImage, outputImageFile)
				resizeImage(selectedImage, outputImageFile.absolutePath)
			}
		}


		// delete tmp png
		iconMap.values().each {
			item ->	item.delete();
		}

	}

}
