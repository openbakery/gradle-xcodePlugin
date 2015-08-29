package org.openbakery.simulators

import org.gradle.api.logging.LogLevel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException


class SimulatorApp {
	
  private static Logger logger = LoggerFactory.getLogger(SimulatorApp.class)

	CommandRunner commandRunner
  String simulatorName

	SimulatorApp(CommandRunner commandRunner = null) {
    this.commandRunner = commandRunner ? commandRunner : new CommandRunner()
	}

  public SimulatorApp killAll() {
    // kill a running simulator
    logger.info("Killing old simulators")
    try {
      commandRunner.run("killall", "iOS Simulator")
    } catch (CommandRunnerException ex) {
      // ignore, this exception means that no simulator was running
    }
    try {
      commandRunner.run("killall", "Simulator") // for xcode 7
    } catch (CommandRunnerException ex) {
      // ignore, this exception means that no simulator was running
    }
    return this
	}

  public SimulatorApp runDevice(String deviceIdentifier) {
    commandRunner.run("open","-b","com.apple.iphonesimulator","--args","-CurrentDeviceUDID",deviceIdentifier)
    return this
	}
}
