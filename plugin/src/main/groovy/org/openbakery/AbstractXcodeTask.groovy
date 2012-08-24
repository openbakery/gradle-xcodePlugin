package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.filefilter.SuffixFileFilter
import org.apache.commons.io.FilenameUtils


class AbstractXcodeTask extends DefaultTask {

    /**
     * Converts the command list array to a nice readable string
     *
     * @param commandList
     * @return a readable string of the command
     */
    def commandListToString(List<String> commandList) {
        def result = "";
        commandList.each{
            item -> result += item + " ";
        }
        return "'"  + result.trim() + "'";
    }

    /**
     * Copies a file to a new location
     *
     * @param source
     * @param destination
     */
    def copy(File source, File destination) {
        println "Copy '" + source + "' -> '" + destination + "'"
        FileUtils.copyFile(source, destination);
    }

    /**
     * Downloads a file from the given address and stores it in the given directory
     *
     * @param toDirectory
     * @param address
     * @return
     */
    def download(String toDirectory, String address) {
        File destinationDirectory = new File(toDirectory);
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdir();
        }
        File destinationFile = new File(destinationDirectory, address.tokenize("/")[-1]);
        def file = new FileOutputStream(destinationFile)
        def out = new BufferedOutputStream(file)
        out << new URL(address).openStream()
        out.close()
        return destinationFile.absolutePath
    }


    def runCommand(String directory, List<String> commandList, Map<String, String> environment) {
        println "Run command: " + commandListToString(commandList)
        if (environment != null) {
            println "with additional environment variables: " + environment;
        }
        def processBuilder = new ProcessBuilder(commandList)
        processBuilder.redirectErrorStream(true)
        processBuilder.directory(new File(directory))
        if (environment != null) {
            Map<String, String> env = processBuilder.environment();
            env.putAll(environment);
        }
        def process = processBuilder.start()
        process.inputStream.eachLine {
            println it
        }
        process.waitFor();
        if (process.exitValue() > 0) {
            throw new IllegalStateException("Command failed to run: " + commandListToString(commandList));
        }
    }

    def runCommand(String directory, List<String> commandList) {
        runCommand(directory, commandList, null);
    }

    def runCommand(List<String> commandList) {
        runCommand(".", commandList)
    }

    def runCommandWithResult(List<String> commandList) {
        runCommandWithResult(".", commandList)
    }

    def runCommandWithResult(String directory, List<String> commandList) {
        runCommandWithResult(directory, commandList, null)
    }

    def runCommandWithResult(String directory, List<String> commandList, Map<String, String> environment) {
        //print commandListToString(commandList)
        def processBuilder = new ProcessBuilder(commandList)
        processBuilder.redirectErrorStream(true)
        processBuilder.directory(new File(directory))
        if (environment != null) {
            Map<String, String> env = processBuilder.environment();
            env.putAll(environment);
        }
        def process = processBuilder.start()
        def result = "";
        process.inputStream.eachLine {
            result += it
        }
        process.waitFor();
        if (process.exitValue() > 0) {
            throw new IllegalStateException("Command failed to run: " + commandListToString(commandList));
        }
        return result;
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
        ).toList();
        if (fileList.count == 0) {
            throw new IllegalStateException("No App Found in directory " + buildOutputDirectory.absolutePath);
        }
        return buildOutputDirectory.absolutePath + "/" + fileList[0];
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
                "Print :"+ key]);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    /**
     *
     * @return the path the the Info.plist for this project
     */
    def getInfoPlist() {
        def infoPlist = project.xcodebuild.infoPlist

        if (infoPlist == null) {

            def fileList =  FileUtils.iterateFiles(
                    new File("."),
                    new SuffixFileFilter("Info.plist"),
                    TrueFileFilter.INSTANCE)
            infoPlist = fileList.next();
        }
        println "Using Info.plist: " + infoPlist;
        return infoPlist.absolutePath
    }

    def getAppBundleInfoPlist() {
        File infoPlistFile = new File(getAppBundleName() + "/Info.plist");
        if (infoPlistFile.exists()) {

            def convertedPlist = new File(project.xcodebuild.buildRoot, FilenameUtils.getName(infoPlistFile.getName()));
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

            return convertedPlist.absolutePath;
        }
        return null;
    }
}