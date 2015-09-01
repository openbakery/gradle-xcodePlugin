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

class SimulatorsStartTask extends DefaultTask {
  SimulatorControl simulatorControl
  SimulatorApp SimulatorApp

  public SimulatorsStartTask() {
    setDescription("Start iOS Simulators")
    dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
    dependsOn(XcodePlugin.SIMULATORS_CREATE_TASK_NAME)
    simulatorControl = new SimulatorControl(project)
    simulatorApp = new SimulatorApp()
  }

  @TaskAction
  void run() {
    def runtimes = simulatorControl.getRuntimes()

    def device = simulatorControl.getDevices(runtimes.get(0)).get(0);

    simulatorApp.killAll().runDevice(device.identifier)
    simulatorControl.waitForDevice(device)
  }
}
