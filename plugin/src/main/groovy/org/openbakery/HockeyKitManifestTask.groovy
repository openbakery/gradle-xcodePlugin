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
package org.openbakery

import org.gradle.api.tasks.TaskAction
import groovy.xml.MarkupBuilder
import org.apache.commons.io.FilenameUtils

class HockeyKitManifestTask extends AbstractHockeykitTask {

	static final String XML_DEF_LINE = '<?xml version="1.0" encoding="UTF-8"?>'
	static final String DOCTYPE_LINE = '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">'


	HockeyKitManifestTask() {
		super()
		dependsOn("hockeykit-archive")
		this.description = "Creates the manifest that is needed to deploy on a HockeyKit Server"
	}

	@TaskAction
	def createManifest() {

		def appName = getAppBundleName()
		def basename = FilenameUtils.getBaseName(appName)

		def infoPlist = getAppBundleInfoPlist()

		def bundleIdentifier = getValueFromPlist(infoPlist, "CFBundleIdentifier")
		def bundleVersion = getValueFromPlist(infoPlist, "CFBundleVersion")
		def bundleDisplayName = getValueFromPlist(infoPlist,"CFBundleDisplayName")

		def outputDirectory = getOutputDirectory()
		def manifestFilename = outputDirectory + "/" + basename + ".plist"


		def title = project.hockeykit.displayName
		if (title == null) {
			title = bundleDisplayName
		}

		def subtitle = getValueFromPlist(infoPlist, "CFBundleShortVersionString")
		if (subtitle == null) {
			subtitle = getValueFromPlist(infoPlist, "CFBundleVersion")
		}

		def writer = new BufferedWriter(new FileWriter(manifestFilename))
		writer.writeLine(XML_DEF_LINE)
		writer.writeLine(DOCTYPE_LINE)
		def xml = new MarkupBuilder(writer)
		xml.plist(version: "1.0") {
			dict() {
				key('items')
				array() {
					dict() {
						key('assets')
						array() {
							dict() {
								key('kind')
								string('software-package')
								key('url')
								string('__URL__')
							}
						}
						key('metadata')
						dict() {
							key('bundle-identifier')
							string(bundleIdentifier)
							key('bundle-version')
							string(bundleVersion)
							key('kind')
							string('software')
							key('title')
							string(title)
							key('subtitle')
							string(subtitle)
						}
					}
				}
			}
		}
		writer.close()
	}

}
