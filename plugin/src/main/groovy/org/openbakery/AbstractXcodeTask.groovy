package org.openbakery

import org.apache.commons.configuration.plist.XMLPropertyListConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task

class AbstractXcodeTask extends DefaultTask {

    CommandRunner commandRunner

    AbstractXcodeTask() {
        commandRunner = new CommandRunner()
    }

    @Override
    Task doFirst(Action<? super Task> action) {
        // TODO clarify why each (sub-)task need to create "project/xcodebuild/buildRoot"
        new File(project.xcodebuild.buildRoot).mkdirs()
        return super.doFirst(action);
    }

    /**
     * Copies a file to a new location
     *
     * @param source
     * @param destination
     */
    def copy(File source, File destination) {
        println "Copy '" + source + "' -> '" + destination + "'"
        FileUtils.copyFile(source, destination)
    }

    /**
     * Downloads a file from the given address and stores it in the given directory
     *
     * @param toDirectory
     * @param address
     * @return
     */
    def download(String toDirectory, String address) {
        File destinationDirectory = new File(toDirectory)
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdir()
        }
        File destinationFile = new File(destinationDirectory, address.tokenize("/")[-1])
        def file = new FileOutputStream(destinationFile)
        def out = new BufferedOutputStream(file)
        out << new URL(address).openStream()
        out.close()
        return destinationFile.absolutePath
    }


    def runCommand(String directory, List<String> commandList, Map<String, String> environment) {
        commandRunner.runCommand(directory, commandList, environment)
    }

    def runCommand(String directory, List<String> commandList) {
        commandRunner.runCommand(directory, commandList)
    }

    def runCommand(List<String> commandList) {
        commandRunner.runCommand(commandList)
    }

    def runCommandWithResult(List<String> commandList) {
        commandRunner.runCommandWithResult(commandList)
    }

    def runCommandWithResult(String directory, List<String> commandList) {
        commandRunner.runCommandWithResult(directory, commandList)
    }

    def runCommandWithResult(String directory, List<String> commandList, Map<String, String> environment) {
        commandRunner.runCommandWithResult(directory, commandList, environment)
    }

    /**
     *
     * @return the absolute path to the generated app bundle
     */
    def getAppBundleName() {
        //println project.xcodebuild.symRoot
        def buildOutputDirectory = new File(project.xcodebuild.symRoot + "/" + project.xcodebuild.configuration + "-" + project.xcodebuild.sdk)
        def fileList = buildOutputDirectory.list(
                [accept: {d, f -> f ==~ /.*app/ }] as FilenameFilter
        ).toList()
        if (fileList.size() == 0) {
            throw new IllegalStateException("No App Found in directory " + buildOutputDirectory.absolutePath)
        }
        return buildOutputDirectory.absolutePath + "/" + fileList[0]
    }

    /**
     * Reads the value for the given key from the given plist
     *
     * @param plist
     * @param key
     * @return returns the value for the given key
     */
    def getValueFromPlist(plist, key) {
        try {
            return runCommandWithResult([
                    "/usr/libexec/PlistBuddy",
                    plist,
                    "-c",
                    "Print :" + key])
        } catch (IllegalStateException ex) {
            return null
        }
    }

    /**
     *
     * @return the path the the Info.plist for this project
     */
    def getInfoPlist() {
        def infoPlist = project.xcodebuild.infoPlist

        if (infoPlist == null) {
            infoPlist = getInfoPlistFromProjectFile()
        }
        println "Using Info.plist: " + infoPlist
        return infoPlist
    }

    def getAppBundleInfoPlist() {
        File infoPlistFile = new File(getAppBundleName() + "/Info.plist")
        if (infoPlistFile.exists()) {

            def convertedPlist = new File(project.xcodebuild.buildRoot, FilenameUtils.getName(infoPlistFile.getName()))
            //plutil -convert xml1 "$BINARY_INFO_PLIST" -o "${INFO_PLIST}.plist"

            def convertCommand = [
                    "plutil",
                    "-convert",
                    "xml1",
                    infoPlistFile.absolutePath,
                    "-o",
                    convertedPlist.absolutePath
            ]

            runCommand(convertCommand)

            return convertedPlist.absolutePath
        }
        return null
    }

    def getInfoPlistFromProjectFile() {
        def projectFileDirectory = new File(".").list(new SuffixFileFilter(".xcodeproj"))[0]
        def projectFile = new File(projectFileDirectory, "project.pbxproj")

        def buildRoot = new File(project.xcodebuild.buildRoot)
        if (!buildRoot.exists()) {
            buildRoot.mkdirs()
        }

        def projectPlist = project.xcodebuild.buildRoot + "/project.plist"

        // convert ascii plist to xml so that commons configuration can parse it!
        runCommand(["plutil", "-convert", "xml1", projectFile.absolutePath, "-o", projectPlist])

        XMLPropertyListConfiguration config = new XMLPropertyListConfiguration(new File(projectPlist))
        def rootObjectKey = config.getString("rootObject")
        println rootObjectKey

        List<String> list = config.getList("objects." + rootObjectKey + ".targets")

        for (target in list) {

            def buildConfigurationList = config.getString("objects." + target + ".buildConfigurationList")
            println "buildConfigurationList=" + buildConfigurationList
            def targetName = config.getString("objects." + target + ".name")
            println "targetName: " + targetName


            if (targetName.equals(project.xcodebuild.target)) {
                def buildConfigurations = config.getList("objects." + buildConfigurationList + ".buildConfigurations")
                for (buildConfigurationsItem in buildConfigurations) {
                    def buildName = config.getString("objects." + buildConfigurationsItem + ".name")

                    println "  buildName: " + buildName + " equals " + project.xcodebuild.configuration

                    if (buildName.equals(project.xcodebuild.configuration)) {
                        def productName = config.getString("objects." + buildConfigurationsItem + ".buildSettings.PRODUCT_NAME")
                        def plistFile = config.getString("objects." + buildConfigurationsItem + ".buildSettings.INFOPLIST_FILE")
                        println "  productName: " + productName
                        println "  plistFile: " + plistFile
                        return plistFile
                    }
                }
            }
        }
    }
}