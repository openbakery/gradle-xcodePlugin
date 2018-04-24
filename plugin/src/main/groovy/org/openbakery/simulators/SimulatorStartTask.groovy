package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction

class SimulatorStartTask extends AbstractSimulatorTask {

    public SimulatorStartTask() {
        setDescription("Start iOS Simulators")
    }


    @TaskAction
    void run() {
        simulatorControl.getDevice(getDestination())
                .ifPresent { device ->
                    simulatorControl.killAll()
                    simulatorControl.runDevice(device)
                    simulatorControl.waitForDevice(device)
                }
    }
}
