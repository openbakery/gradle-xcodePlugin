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

import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.xcode.Extension
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcodebuild

import java.util.regex.Pattern

import static groovy.io.FileType.FILES

class XcodeBuildArchiveTask extends AbstractXcodeBuildTask {

    public static final String ARCHIVE_FOLDER = "archive"

    private static final Pattern ICON_FILE_MATCHER = ~/Icon(-\d+)??\.png/

    XcodeBuildArchiveTask() {
        super()

        dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
        // when creating an xcarchive for iOS then the provisioning profile is need for the team id so that the entitlements is setup properly
        dependsOn(XcodePlugin.PROVISIONING_INSTALL_TASK_NAME)
        this.description = "Prepare the app bundle that it can be archive"
    }

    @OutputDirectory
    File getOutputDirectory() {
        def archiveDirectory = new File(project.getBuildDir(), ARCHIVE_FOLDER)
        archiveDirectory.mkdirs()
        return archiveDirectory
    }


    private ArrayList<String> getiOSIcons() {
        return parameters.applicationBundle
                .list()
                .findAll { it.matches(ICON_FILE_MATCHER) }
                .collect { "Applications/" + parameters.applicationBundleName + "/" + it }
    }

    ArrayList<String> getMacOSXIcons() {
        File appInfoPlist = new File(parameters.applicationBundle, "Contents/Info.plist")
        return Optional.ofNullable(plistHelper.getValueFromPlist(appInfoPlist, "CFBundleIconFile"))
                .filter { it -> return !it.empty }
                .map { it -> ["Applications/" + parameters.applicationBundleName + "/Contents/Resources/" + it + ".icns"] }
                .orElse([])
    }

    def getValueFromBundleInfoPlist(File bundle, String key) {
        File appInfoPlist
        if (parameters.type == Type.macOS) {
            appInfoPlist = new File(bundle, "Contents/Info.plist")
        } else {
            appInfoPlist = new File(bundle, "Info.plist")
        }
        return plistHelper.getValueFromPlist(appInfoPlist, key)
    }


    def createInfoPlist(def applicationsDirectory) {

        StringBuilder content = new StringBuilder();


        def name = parameters.bundleName
        def schemeName = name
        def applicationPath = "Applications/" + parameters.applicationBundleName
        def bundleIdentifier = getValueFromBundleInfoPlist(parameters.applicationBundle, "CFBundleIdentifier")
        int time = System.currentTimeMillis() / 1000;

        def creationDate = formatDate(new Date());

        def shortBundleVersion = getValueFromBundleInfoPlist(parameters.applicationBundle, "CFBundleShortVersionString")
        def bundleVersion = getValueFromBundleInfoPlist(parameters.applicationBundle, "CFBundleVersion")

        List<String> icons
        if (parameters.type == Type.iOS || parameters.type == Type.tvOS) {
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

        if (getSigningIdentity()) {
            content.append("		<key>SigningIdentity</key>\n")
            content.append("		<string>" + getSigningIdentity() + "</string>\n")

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
        content.append("	<string>" + name + "</string>\n")
        content.append("	<key>SchemeName</key>\n")
        content.append("	<string>" + schemeName + "</string>\n")
        content.append("</dict>\n")
        content.append("</plist>")

        File infoPlist = new File(applicationsDirectory, "Info.plist")
        FileUtils.writeStringToFile(infoPlist, content.toString())
    }


    def createFrameworks(def archiveDirectory, Xcodebuild xcodebuild) {

        File frameworksPath = new File(archiveDirectory, "Products/Applications/" + parameters.applicationBundleName + "/Frameworks")
        if (frameworksPath.exists()) {


            def libNames = []
            frameworksPath.eachFile() {
                libNames.add(it.getName())
            }

            logger.debug("swiftlibs to add: {}", libNames);

            File swiftLibs = new File(xcodebuild.getToolchainDirectory(),
                    "usr/lib/swift/" + getSwiftLibFolderName())

            logger.debug("swiftlibs to add: {}", swiftLibs);

            swiftLibs.eachFile() {
                logger.debug("candidate for copy? {}: {}", it.name, libNames.contains(it.name))
                if (libNames.contains(it.name)) {
                    copy(it, getSwiftSupportDirectory())
                }
            }
        }

    }

    public String getSwiftLibFolderName() {
        String result
        switch (parameters.type) {
            case Type.tvOS:
                result = "appletvos"
                break

            case Type.iOS:
                result = "iphoneos"
                break

            default:
                break
        }

        return result
    }

    def getSwiftSupportDirectory() {
        def swiftSupportPath = "SwiftSupport"

        if (xcode.version.major > 6) {
            swiftSupportPath += "/" + getSwiftLibFolderName()
        }

        File swiftSupportDirectory = new File(getArchiveDirectory(), swiftSupportPath)
        if (!swiftSupportDirectory.exists()) {
            swiftSupportDirectory.mkdirs()
        }
        return swiftSupportDirectory
    }

    def deleteDirectoryIfEmpty(File base, String child) {
        File directory = new File(base, child)
        if (directory.exists() && directory.list().length == 0) {
            directory.deleteDir();
        }
    }

    def deleteEmptyFrameworks(File applicationsDirectory) {
        // if frameworks directory is emtpy
        File appPath = new File(applicationsDirectory, "Products/Applications/" + parameters.applicationBundleName)
        deleteDirectoryIfEmpty(appPath, "Frameworks")


    }

    def deleteFrameworksInExtension(File applicationsDirectory) {


        File plugins = new File(applicationsDirectory, parameters.applicationBundleName + "/Plugins")
        if (!plugins.exists()) {
            return
        }

        plugins.eachFile(FileType.DIRECTORIES) { file ->
            if (file.toString().endsWith(".appex")) {
                File frameworkDirectory = new File(file, "Frameworks");
                if (frameworkDirectory.exists()) {
                    FileUtils.deleteDirectory(frameworkDirectory)
                }
            }
        }

    }


    def copyDsyms(File archiveDirectory, File dSymDirectory) {

        archiveDirectory.eachFileRecurse(FileType.DIRECTORIES) { directory ->
            if (directory.toString().toLowerCase().endsWith(".dsym")) {
                copy(directory, dSymDirectory)
            }
        }

    }

    def createEntitlements(File bundle) {

        if (parameters.type != Type.iOS && parameters.type != Type.tvOS) {
            logger.warn("Entitlements handling is only implemented for iOS and tvOS!")
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
        if (buildConfiguration.entitlements) {
            File entitlementFile = new File(destinationDirectory, "archived-expanded-entitlements.xcent")
            FileUtils.copyFile(new File(project.projectDir, buildConfiguration.entitlements), entitlementFile)
            modifyEntitlementsFile(entitlementFile, bundleIdentifier)
        }
    }

    def modifyEntitlementsFile(File entitlementFile, String bundleIdentifier) {
        if (!entitlementFile.exists()) {
            logger.warn("Entitlements File does not exist {}", entitlementFile)
            return
        }

        String applicationIdentifier = "UNKNOWN00ID";
        // if UNKNOWN00ID this means that not application identifier is found an this value is used as fallback
        File provisioningProfile = ProvisioningProfileReader.getProvisionFileForIdentifier(bundleIdentifier, project.xcodebuild.signing.mobileProvisionFile, this.commandRunner, this.plistHelper)
        if (provisioningProfile != null && provisioningProfile.exists()) {
            ProvisioningProfileReader reader = new ProvisioningProfileReader(provisioningProfile, commandRunner)
            applicationIdentifier = reader.getApplicationIdentifierPrefix()
        }

        plistHelper.addValueForPlist(entitlementFile, "application-identifier", applicationIdentifier + "." + bundleIdentifier)

        List<String> keychainAccessGroups = plistHelper.getValueFromPlist(entitlementFile, "keychain-access-groups")

        if (keychainAccessGroups != null && keychainAccessGroups.size() > 0) {
            def modifiedKeychainAccessGroups = []
            keychainAccessGroups.each() { group ->
                modifiedKeychainAccessGroups << group.replace(ProvisioningProfileReader.APPLICATION_IDENTIFIER_PREFIX, applicationIdentifier + ".")
            }
            plistHelper.setValueForPlist(entitlementFile, "keychain-access-groups", modifiedKeychainAccessGroups)
        }
    }

    def createExtensionSupportDirectory(File bundle, Xcodebuild xcodebuild) {
        String extensionIdentifier = getValueFromBundleInfoPlist(bundle, "NSExtension:NSExtensionPointIdentifier")
        if (extensionIdentifier == null) {
            logger.debug("No support directory created, because no extension identifier found in bundle {}", bundle)
            return
        }

        Extension extension = Extension.extensionFromIdentifier(extensionIdentifier)
        if (extension == null) {
            logger.warn("Extension type not supported", extensionIdentifier)
            return
        }

        switch (extension) {
            case Extension.sticker:
                File supportDirectory = new File(getArchiveDirectory(), "MessagesApplicationExtensionSupport")
                if (supportDirectory.mkdirs()) {
                    File stub = new File(xcodebuild.getPlatformDirectory(), "/Library/Application Support/MessagesApplicationExtensionStub/MessagesApplicationExtensionStub")
                    copy(stub, supportDirectory)
                }
                break
            default:
                break
        }
    }

    @TaskAction
    def archive() {
        parameters = project.xcodebuild.xcodebuildParameters.merge(parameters)
        if (parameters.isSimulatorBuildOf(Type.iOS) || parameters.isSimulatorBuildOf(Type.tvOS)) {
            logger.debug("Create zip archive")

            // create zip archive
            String zipFileName = parameters.bundleName
            if (project.xcodebuild.bundleNameSuffix != null) {
                zipFileName += project.xcodebuild.bundleNameSuffix
            }
            zipFileName += ".zip"

            def zipFile = new File(project.getBuildDir(), "archive/" + zipFileName)
            def baseDirectory = parameters.applicationBundle.parentFile

            createZip(zipFile, baseDirectory, parameters.applicationBundle)
            return
        }

        logger.debug("Create xcarchive")
        Xcodebuild xcodebuild = new Xcodebuild(project.projectDir, commandRunner, xcode, parameters, getDestinations())

        if (project.xcodebuild.useXcodebuildArchive) {

            File outputFile = new File(project.getBuildDir(), "xcodebuild-archive-output.txt")
            commandRunner.setOutputFile(outputFile)

            xcodebuild.executeArchive(createXcodeBuildOutputAppender("XcodeBuildArchive"), project.xcodebuild.environment, getArchiveDirectory().absolutePath)

            return
        }

        // create xcarchive
        // copy application bundle
        copy(parameters.applicationBundle, getApplicationsDirectory())

        File onDemandResources = new File(parameters.outputPath, "OnDemandResources")
        if (onDemandResources.exists()) {
            copy(onDemandResources, getProductsDirectory())
        }

        // copy onDemandResources

        def dSymDirectory = new File(getArchiveDirectory(), "dSYMs")
        dSymDirectory.mkdirs()
        copyDsyms(parameters.outputPath, dSymDirectory)

        List<File> appBundles = getAppBundles(parameters.outputPath)
        for (File bundle : appBundles) {
            createEntitlements(bundle)
            createExtensionSupportDirectory(bundle, xcodebuild)
        }

        File applicationsDirectory = getApplicationsDirectory()

        File archiveDirectory = getArchiveDirectory()
        createInfoPlist(archiveDirectory)
        createFrameworks(archiveDirectory, xcodebuild)
        deleteEmptyFrameworks(archiveDirectory)
        deleteXCTestIfExists(applicationsDirectory)
        deleteFrameworksInExtension(applicationsDirectory)
        copyBCSymbolMaps(archiveDirectory)

        if (project.xcodebuild.type == Type.iOS || project.xcodebuild.type == Type.tvOS) {
            File applicationFolder = new File(getArchiveDirectory(), "Products/Applications/" + parameters.applicationBundleName)
            convertInfoPlistToBinary(applicationFolder)

            removeUnneededDylibsFromBundle(applicationFolder)
        }

        logger.debug("create archive done")
    }

    def copyBCSymbolMaps(File archiveDirectory) {
        if (!parameters.bitcode) {
            logger.debug("bitcode is not activated, so to not create BCSymbolMaps")
            return
        }
        File bcSymbolMapsDirectory = new File(archiveDirectory, "BCSymbolMaps")
        bcSymbolMapsDirectory.mkdirs()

        parameters.outputPath.eachFileRecurse { file ->
            if (file.toString().endsWith(".bcsymbolmap")) {
                FileUtils.copyFileToDirectory(file, bcSymbolMapsDirectory)
            }
        }

    }

    // TODO: Define a `exportOptionsPlist` to avoid that kind of issue
    def removeUnneededDylibsFromBundle(File bundle) {
        File libswiftRemoteMirror = new File(bundle, "libswiftRemoteMirror.dylib")
        if (libswiftRemoteMirror.exists()) {
            libswiftRemoteMirror.delete()
        }
    }

    def deleteXCTestIfExists(File applicationsDirectory) {
        File plugins = new File(applicationsDirectory, project.xcodebuild.applicationBundle.name + "/Contents/Plugins")
        if (!plugins.exists()) {
            return
        }
        plugins.eachFile(FileType.DIRECTORIES) { file ->
            if (file.toString().endsWith("xctest")) {
                FileUtils.deleteDirectory(file)
                return true
            }
        }
    }

    File getProductsDirectory() {
        File productsDirectory = new File(getArchiveDirectory(), "Products")
        productsDirectory.mkdirs()
        return productsDirectory
    }

    File getApplicationsDirectory() {
        File applicationsDirectory = new File(getProductsDirectory(), "Applications")
        applicationsDirectory.mkdirs()
        return applicationsDirectory
    }

    File getDestinationDirectoryForBundle(File bundle) {
        String relative = parameters.outputPath.toURI().relativize(bundle.toURI()).getPath();
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

        def archiveDirectoryName = XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/" + project.xcodebuild.bundleName

        if (project.xcodebuild.bundleNameSuffix != null) {
            archiveDirectoryName += project.xcodebuild.bundleNameSuffix
        }
        archiveDirectoryName += ".xcarchive"

        def archiveDirectory = new File(project.getBuildDir(), archiveDirectoryName)
        archiveDirectory.mkdirs()
        return archiveDirectory
    }


}
