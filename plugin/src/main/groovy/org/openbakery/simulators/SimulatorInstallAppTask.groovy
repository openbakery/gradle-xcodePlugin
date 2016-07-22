package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.openbakery.CommandRunnerException

class SimulatorInstallAppTask extends AbstractSimulatorTask {

  public SimulatorInstallAppTask() {
    setDescription("Install app on iOS Simulators")
    dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
    dependsOn(XcodePlugin.SIMULATORS_START_TASK_NAME)
  }

  @TaskAction
  void run() {
    try {
      logger.lifecycle("Installing " + project.xcodebuild.applicationBundle.absolutePath);
      simulatorControl.simctl("install", "booted", project.xcodebuild.applicationBundle.absolutePath)
    } catch (CommandRunnerException ex) {
      println "Unable to install" + project.xcodebuild.applicationBundle.absolutePath
      throw ex
    }
  }
}
