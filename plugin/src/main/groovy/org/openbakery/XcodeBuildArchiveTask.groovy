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

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import static groovy.io.FileType.FILES

class XcodeBuildArchiveTask extends AbstractXcodeTask {

	public static final String ARCHIVE_FOLDER = "archive"

    XcodeBuildArchiveTask() {
		super()

		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
		this.description = "Prepare the app bundle that it can be archive"
	}


	def getOutputDirectory() {
		def archiveDirectory = new File(project.getBuildDir(), ARCHIVE_FOLDER)
		archiveDirectory.mkdirs()
		return archiveDirectory
	}




	def getiOSIcons() {
		ArrayList<String> icons = new ArrayList<>();

		File applicationBundle = project.xcodebuild.applicationBundle
		def fileList = applicationBundle.list(
						[accept: { d, f -> f ==~ /Icon(-\d+)??\.png/ }] as FilenameFilter // matches Icon.png or Icon-72.png
		).toList()

		def applicationPath = "Applications/" + project.xcodebuild.applicationBundle.name

		for (String item in fileList) {
			icons.add(applicationPath + "/" + item)
		}


		return icons
	}

	def getMacOSXIcons() {
		File appInfoPlist  = new File(project.xcodebuild.applicationBundle, "Contents/Info.plist")
		ArrayList<String> icons = new ArrayList<>();

		def icnsFileName = plistHelper.getValueFromPlist(appInfoPlist, "CFBundleIconFile")

		if (icnsFileName == null || icnsFileName == "") {
			return icons
		}

		def iconPath = "Applications/" + project.xcodebuild.applicationBundle.name + "/Contents/Resources/" + icnsFileName + ".icns"
		icons.add(iconPath)

		return icons
	}



	def getValueFromBundleInfoPlist(File bundle, String key) {
		File appInfoPlist
		if (project.xcodebuild.type == Type.OSX) {
			appInfoPlist = new File(bundle, "Contents/Info.plist")
		} else {
			appInfoPlist = new File(bundle, "Info.plist")
		}
		return plistHelper.getValueFromPlist(appInfoPlist, key)
	}


	def createInfoPlist(def applicationsDirectory) {

		StringBuilder content = new StringBuilder();


		def name = project.xcodebuild.bundleName
		def schemeName = name
		def applicationPath = "Applications/" + project.xcodebuild.applicationBundle.name
		def bundleIdentifier = getValueFromBundleInfoPlist(project.xcodebuild.applicationBundle, "CFBundleIdentifier")
		int time = System.currentTimeMillis() / 1000;

		def creationDate = formatDate(new Date());

		def shortBundleVersion = getValueFromBundleInfoPlist(project.xcodebuild.applicationBundle, "CFBundleShortVersionString")
		def bundleVersion = getValueFromBundleInfoPlist(project.xcodebuild.applicationBundle, "CFBundleVersion")

		List icons = new ArrayList<String>()

		if (project.xcodebuild.type == Type.iOS) {
			icons = getiOSIcons()
		} else {
			icons = getMacOSXIcons()
		}

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



	def createFrameworks(def applicationsDirectory) {

		File frameworksPath = new File(applicationsDirectory, "Products/Applications/" + project.xcodebuild.applicationBundle.name + "/Frameworks")


		if (frameworksPath.exists()) {
			File swiftSupportDirectory = new File(getArchiveDirectory(), "SwiftSupport");
			if (!swiftSupportDirectory.exists()) {
				swiftSupportDirectory.mkdirs()
			}

			def libNames = []
			frameworksPath.eachFile() {
				libNames.add(it.getName())
			}

			File swiftLibs = new File(project.xcodebuild.xcodePath + "/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphoneos")

			swiftLibs.eachFile() {
				if (libNames.contains(it.name)) {
					copy(it, swiftSupportDirectory)
				}
			}


		}

	}

	def createEntitlements(File bundle) {

		if (project.xcodebuild.type != Type.iOS) {
			logger.warn("Entitlements handling is only implemented for iOS!")
			return
		}

		String bundleIdentifier = getValueFromBundleInfoPlist(bundle, "CFBundleIdentifier")
		if (bundleIdentifier == null) {
			logger.debug("No entitlements embedded, because no bundle identifier found in bundle {}", bundle)
			return
		}
		BuildConfiguration buildConfiguration = project.xcodebuild.getBuildConfiguration(bundleIdentifier)
		if (buildConfiguration == null) {
			logger.debug("No entitlements embedded, because no buildConfiguration for bundle identifier {}", bundleIdentifier)
			return
		}

		File destinationDirectory = getDestinationDirectoryForBundle(bundle)

		FileUtils.copyFileToDirectory(new File(project.projectDir, buildConfiguration.entitlements), new File(destinationDirectory, "archived-expanded-entitlements.xcent"))
	}


	@TaskAction
	def archive() {

		if (project.xcodebuild.isSimulatorBuildOf(Type.iOS)) {
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
			return
		}

		logger.debug("Create xcarchive")

		// create xcarchive
		copy(project.xcodebuild.applicationBundle, getApplicationsDirectory())


		def dSymDirectory = new File(getArchiveDirectory(), "dSYMs")
		dSymDirectory.mkdirs()

		List<File> appBundles = getAppBundles(project.xcodebuild.outputPath)

		for (File bundle : appBundles) {
			File dsymPath = new File(project.xcodebuild.outputPath, bundle.getName() + ".dSYM");
			if (dsymPath.exists()) {
				copy(dsymPath, dSymDirectory)
			}
			createEntitlements(bundle)
		}


		createInfoPlist(getArchiveDirectory())

		createFrameworks(getArchiveDirectory())


		if (project.xcodebuild.type == Type.iOS) {
			File applicationFolder = new File(getArchiveDirectory(), "Products/Applications/" + project.xcodebuild.applicationBundle.name)
			convertInfoPlistToBinary(applicationFolder)
		}


		logger.debug("create archive done")

	}

	File getApplicationsDirectory() {
		File applicationsDirectory = new File(getArchiveDirectory(), "Products/Applications")
		applicationsDirectory.mkdirs()
		return applicationsDirectory
	}

	File getDestinationDirectoryForBundle(File bundle) {
		String relative = project.xcodebuild.outputPath.toURI().relativize(bundle.toURI()).getPath();
		return new File(getApplicationsDirectory(), relative)
	}

	def convertInfoPlistToBinary(File archiveDirectory) {

		archiveDirectory.eachFileRecurse(FILES) {
			if (it.name.endsWith('.plist')) {
				logger.debug("convert plist to binary {}", it)
				def commandList = ["/usr/bin/plutil", "-convert", "binary1", it.absolutePath]
				try {
					commandRunner.run(commandList)
				} catch (CommandRunnerException ex) {
					logger.lifecycle("Unable to convert!")
				}
			}
		}

	}


	def removeResourceRules(File appDirectory) {

		File resourceRules = new File(appDirectory, "ResourceRules.plist")
		logger.lifecycle("delete {}", resourceRules)
		if (resourceRules.exists()) {
			resourceRules.delete()
		}

		logger.lifecycle("remove CFBundleResourceSpecification from {}", new File(appDirectory, "Info.plist"))

		setValueForPlist(new File(appDirectory, "Info.plist"), "Delete: CFBundleResourceSpecification")

	}


	File getArchiveDirectory() {

		def archiveDirectoryName =  XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/" +  project.xcodebuild.bundleName

		if (project.xcodebuild.bundleNameSuffix != null) {
			archiveDirectoryName += project.xcodebuild.bundleNameSuffix
		}
		archiveDirectoryName += ".xcarchive"

		def archiveDirectory = new File(project.getBuildDir(), archiveDirectoryName)
		archiveDirectory.mkdirs()
		return archiveDirectory
	}


}
