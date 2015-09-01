package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.openbakery.PlistHelper
import org.openbakery.simulators.SimulatorControl
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunnerException
import org.openbakery.simulators.SimulatorApp

class SimulatorsInstallAppTask extends DefaultTask {
  SimulatorControl simulatorControl

  public SimulatorsInstallAppTask() {
    setDescription("Install app on iOS Simulators")
    dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
    dependsOn(XcodePlugin.SIMULATORS_START_TASK_NAME)
    simulatorControl = new SimulatorControl(project)
  }

  @TaskAction
  void run() {
    try {
       simulatorControl.simctl("install","booted", project.xcodebuild.applicationBundle.absolutePath)
    } catch (CommandRunnerException ex) {
        println "Unable to install"+project.xcodebuild.applicationBundle.absolutePath
        throw ex
    }
  }
}
