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

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction

import java.text.SimpleDateFormat

class XcodeBuildArchiveTask extends AbstractXcodeTask {

	XcodeBuildArchiveTask() {
		super()

		dependsOn('xcodebuild')
		this.description = "Prepare the app bundle that it can be archive"
	}


	def getOutputDirectory() {
		def archiveDirectory = new File(project.getBuildDir(), "archive/")
		archiveDirectory.mkdirs()
		return archiveDirectory
	}




	def getIcons() {
		ArrayList<String> icons = new ArrayList<>();

		File applicationBundle = project.xcodebuild.applicationBundle
		def fileList = applicationBundle.list(
						[accept: { d, f -> f ==~ /Icon(-\d+)??\.png/ }] as FilenameFilter // matches Icon.png or Icon-72.png
		).toList()

		def applicationPath = "Applications/" + project.xcodebuild.applicationBundle.name

		for (String item in fileList) {
			icons.add(applicationPath + "/" + item)
		}


		return icons;
	}


	def createInfoPlist(def applicationsDirectory) {

		StringBuilder content = new StringBuilder();

		File appInfoPlist = new File(project.xcodebuild.applicationBundle, "Info.plist")

		def name = project.xcodebuild.bundleName
		def schemeName = name
		def applicationPath = "Applications/" + project.xcodebuild.applicationBundle.name
		def bundleIdentifier = getValueFromPlist(appInfoPlist, "CFBundleIdentifier")
		int time = System.currentTimeMillis() / 1000;

		def creationDate = formatDate(new Date());

		def shortBundleVersion = getValueFromPlist(appInfoPlist, "CFBundleShortVersionString")
		def bundleVersion = getValueFromPlist(appInfoPlist, "CFBundleVersion")



		List icons = getIcons()

		content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
		content.append("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n")
		content.append("<plist version=\"1.0\">\n")
		content.append("<dict>\n")
		content.append("	<key>ApplicationProperties</key>\n")
		content.append("	<dict>\n")
		content.append("		<key>ApplicationPath</key>\n")
		content.append("		<string>" + applicationPath + "</string>\n")
		content.append("		<key>CFBundleIdentifier</key>\n")
		content.append("		<string>" + bundleIdentifier + "</string>\n")

		if (shortBundleVersion != null) {
			content.append("		<key>CFBundleShortVersionString</key>\n")
			content.append("		<string>" + shortBundleVersion + "</string>\n")
		}

		if (bundleVersion != null) {
			content.append("		<key>CFBundleVersion</key>\n")
			content.append("		<string>" + bundleVersion + "</string>\n")
		}

		if (project.xcodebuild.getSigning().getIdentity()) {
			content.append("		<key>SigningIdentity</key>\n")
			content.append("		<string>" + project.xcodebuild.getSigning().getIdentity() + "</string>\n")

		}

		if (icons.size() > 0) {
			content.append("		<key>IconPaths</key>\n")
			content.append("		<array>\n")
			for (String icon : icons) {
				content.append("			<string>" + icon + "</string>\n")
			}
			content.append("		</array>\n")
		}

		content.append("	</dict>\n")
		content.append("	<key>ArchiveVersion</key>\n")
		content.append("	<integer>2</integer>\n")
		content.append("	<key>CreationDate</key>\n")
		content.append("	<date>" + creationDate + "</date>\n")
		content.append("	<key>Name</key>\n")
		content.append("	<string>" + name  + "</string>\n")
		content.append("	<key>SchemeName</key>\n")
		content.append("	<string>" + schemeName + "</string>\n")
		content.append("</dict>\n")
		content.append("</plist>")



		File infoPlist = new File(applicationsDirectory, "Info.plist")
		FileUtils.writeStringToFile(infoPlist, content.toString())


	}

	@TaskAction
	def archive() {
		if (project.xcodebuild.sdk.startsWith("iphoneos")) {
			logger.debug("Create xcarchive")

			// create xcarchive
			def applicationsDirectory = new File(project.xcodebuild.archiveDirectory, "Products/Applications")
			applicationsDirectory.mkdirs()

			copy(project.xcodebuild.applicationBundle, applicationsDirectory)


			def dSymDirectory = new File(project.xcodebuild.archiveDirectory, "dSYMs")
			dSymDirectory.mkdirs()

			List<File> appBundles = getAppBundles(project.xcodebuild.outputPath)

			for (File bundle : appBundles) {
				File dsymPath = new File(project.xcodebuild.outputPath, bundle.getName() + ".dSYM");
				if (dsymPath.exists()) {
					copy(dsymPath, dSymDirectory)
				}
			}


			createInfoPlist(project.xcodebuild.archiveDirectory)

		} else {
			logger.debug("Create zip archive")

			// create zip archive
			String zipFileName = project.xcodebuild.bundleName
			if (project.xcodebuild.bundleNameSuffix != null) {
				zipFileName += project.xcodebuild.bundleNameSuffix
			}
			zipFileName += ".zip"

			def zipFile = new File(project.getBuildDir(), "archive/" + zipFileName)
			def baseDirectory = project.xcodebuild.applicationBundle.parentFile

			createZip(zipFile, baseDirectory, project.xcodebuild.applicationBundle)
		}

		logger.debug("create archive done")

	}
}
