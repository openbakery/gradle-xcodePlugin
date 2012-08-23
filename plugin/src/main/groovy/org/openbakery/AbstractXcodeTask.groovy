package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.InvalidUserDataException


class AbstractXcodeTask extends DefaultTask {

    def commandListToString(List<String> commandList) {
        def result = "";
        commandList.each{
            item -> result += item + " ";
        }
        return "'"  + result.trim() + "'";
    }

    def copy(File source, File destination) {
        println "Copy '" + source + "' -> '" + destination + "'"

        def input = source.newDataInputStream()
        def output = destination.newDataOutputStream()

        output << input

        input.close()
        output.close()

    }

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

    def getValueFromPlist(plist, value) {
        try {
            return runCommandWithResult([
                "/usr/libexec/PlistBuddy",
                plist,
                "-c",
                "Print :"+ value]);
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    def getInfoPlist() {
        def infoPlist = project.xcodebuild.infoPlist

        if (infoPlist == null) {
            fileList = new File('.').list(
                    [accept:{d, f-> f ==~ /.*Info.plist/ }] as FilenameFilter
            ).toList();
            infoPlist = fileList[0]
        }

        return infoPlist
    }

    def getAppBundleInfoPlist() {
        File infoPlistFile = new File(getAppBundleName() + "/Info.plist");
        if (infoPlistFile.exists()) {
            return infoPlistFile.absolutePath;
        }
        return null;
    }
}