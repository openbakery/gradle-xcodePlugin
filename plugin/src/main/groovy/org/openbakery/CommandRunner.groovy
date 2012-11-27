package org.openbakery

class CommandRunner {

    private def commandListToString(List<String> commandList) {
        def result = ""
        commandList.each {
            item -> result += item + " "
        }
        return "'" + result.trim() + "'"
    }

    def runCommand(String directory, List<String> commandList, Map<String, String> environment) {
        println "Run command: " + commandListToString(commandList)
        if (environment != null) {
            println "with additional environment variables: " + environment
        }
        def processBuilder = new ProcessBuilder(commandList)
        processBuilder.redirectErrorStream(true)
        processBuilder.directory(new File(directory))
        if (environment != null) {
            Map<String, String> env = processBuilder.environment()
            env.putAll(environment)
        }
        def process = processBuilder.start()
        process.inputStream.eachLine {
            println it
        }
        process.waitFor()
        if (process.exitValue() > 0) {
            throw new IllegalStateException("Command failed to run: " + commandListToString(commandList))
        }
    }

    def runCommand(String directory, List<String> commandList) {
        runCommand(directory, commandList, null)
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
            Map<String, String> env = processBuilder.environment()
            env.putAll(environment)
        }
        def process = processBuilder.start()
        def result = ""
        process.inputStream.eachLine {
            result += it
        }
        process.waitFor()
        if (process.exitValue() > 0) {
            throw new IllegalStateException("Command failed to run: " + commandListToString(commandList))
        }
        return result
    }
}