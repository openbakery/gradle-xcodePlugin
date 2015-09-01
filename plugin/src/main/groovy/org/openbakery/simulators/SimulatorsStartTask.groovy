package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin

class SimulatorsStartTask extends DefaultTask {
  SimulatorControl simulatorControl

  public SimulatorsStartTask() {
    setDescription("Start iOS Simulators")
    dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
    dependsOn(XcodePlugin.SIMULATORS_CREATE_TASK_NAME)
    simulatorControl = new SimulatorControl(project)
  }

  @TaskAction
  void run() {
    List<SimulatorRuntime> runtimes = simulatorControl.getRuntimes()
    if (runtimes.size() == 0) {
      logger.lifecycle("No simulator runtime found")
      return
    }

    SimulatorRuntime runtime = runtimes.get(0)
    List<SimulatorDevice> deviceList = simulatorControl.getDevices(runtimes)

    if (deviceList.size() == 0) {
      logger.lifecycle("No device for for runtime {}", runtime)
      return
    }
    def device = deviceList.get(0);

    simulatorControl.killAll()
    simulatorControl.runDevice(device)
    simulatorControl.waitForDevice(device)
  }
}
