package org.openbakery.simulators

import org.gradle.api.Project
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.openbakery.XcodePlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by rene on 30.04.15.
 */
class SimulatorControl {


	enum Section {
		DEVICE_TYPE("== Device Types =="),
		RUNTIMES("== Runtimes =="),
		DEVICES("== Devices ==")

		private final String identifier
		Section(String identifier) {
			this.identifier = identifier
		}


		public static Section isSection(String line) {
			for (Section section : Section.values()) {
				if (section.identifier.equals(line)) {
					return section;
				}
			}
			return null
		}

	}


	private static Logger logger = LoggerFactory.getLogger(SimulatorControl.class)

	CommandRunner commandRunner = new CommandRunner()

	String simctlCommand

	ArrayList<SimulatorDeviceType> deviceTypes
	ArrayList<SimulatorRuntime> runtimes
	HashMap<SimulatorRuntime, List<SimulatorDevice>>devices;
	HashMap<String, SimulatorDevice> identifierToDevice;

	Project project

	public SimulatorControl(Project project) {
		this.project = project
	}

	void parse() {
		runtimes = new ArrayList<>()
		devices = new HashMap<>()
		deviceTypes = new ArrayList<>()
    identifierToDevice = new HashMap<>()

		Section section = null;
		String simctlList = simctl("list")

		ArrayList<SimulatorDevice> simulatorDevices = null
		for (String line in simctlList.split("\n")) {

			Section isSection = Section.isSection(line);
			if (isSection != null) {
				section = isSection
				continue
			}

			switch (section) {
				case Section.DEVICE_TYPE:
					deviceTypes.add(new SimulatorDeviceType(line))
					break
				case Section.RUNTIMES:
					SimulatorRuntime runtime = new SimulatorRuntime(line)
					runtimes.add(runtime)
					break
				case Section.DEVICES:

					SimulatorRuntime isRuntime = parseDevicesRuntime(line)
					if (isRuntime != null) {
						simulatorDevices = new ArrayList<>()
						devices.put(isRuntime, simulatorDevices)
						continue
					}
					if (line.startsWith("--")) {
						// unknown runtime, so we are done
						simulatorDevices = null
					}

					if (simulatorDevices != null) {
						SimulatorDevice device = new SimulatorDevice(line)
						simulatorDevices.add(device)
            identifierToDevice[device.identifier]=device
					}

					break


			}
		}
	}

	SimulatorRuntime parseDevicesRuntime(String line) {
		for (SimulatorRuntime runtime in runtimes) {
			if (line.equals("-- " + runtime.name + " --")) {
				return runtime
			}
		}
		return null
	}


	public void waitForDevice(SimulatorDevice device, int timeoutMS=10000) {
     def start = System.currentTimeMillis()
     while( (System.currentTimeMillis()-start) < timeoutMS ) {
		   parse()
       def polledDevice = identifierToDevice[device.identifier]
       if ( polledDevice != null && polledDevice.state == "Booted" )
          return
       sleep(500)
     }
     throw new Exception("Timeout waiting for "+device)
	}

	List<SimulatorRuntime> getRuntimes() {
		if (runtimes == null) {
			parse()
		}
		return runtimes
	}

	List <SimulatorDevice> getDevices(SimulatorRuntime runtime) {
		return getDevices().get(runtime)
	}

	HashMap<SimulatorRuntime, List<SimulatorDevice>> getDevices() {
		if (devices == null) {
			parse()
		}
		return devices;
	}

	List<SimulatorDeviceType> getDeviceTypes() {
		if (deviceTypes == null) {
			parse()
		}
		return deviceTypes
	}


	String simctl(String... commands) {
		if (simctlCommand == null) {
			simctlCommand = commandRunner.runWithResult([project.xcodebuild.xcrunCommand, "-sdk", XcodePlugin.SDK_IPHONEOS, "-find", "simctl"]);
		}

		ArrayList<String>parameters = new ArrayList<>()
		parameters.add(simctlCommand)
		parameters.addAll(commands)
		return commandRunner.runWithResult(parameters);
	}




	void deleteAll() {

		for (Map.Entry<SimulatorRuntime, List<SimulatorDevice>> entry : getDevices().entrySet()) {
			for (SimulatorDevice device in entry.getValue()) {
				if (device.available) {
					println "Delete simulator: '" + device.name + "' " + device.identifier
					simctl("delete", device.identifier)
				}
			}
		}
	}


	void createAll() {
		for (SimulatorRuntime runtime in getRuntimes()) {

			if (runtime.available) {

				for (SimulatorDeviceType deviceType in getDeviceTypes()) {

					if (deviceType.canCreateWithRuntime(runtime)) {
						logger.debug("create '" + deviceType.name + "' '" + deviceType.identifier + "' '" + runtime.identifier + "'")
						try {
							simctl("create", deviceType.name, deviceType.identifier, runtime.identifier)
							println "Create simulator: '" + deviceType.name + "' for " + runtime.version
						} catch (CommandRunnerException ex) {
							println "Unable to create simulator: '" + deviceType.name + "' for " + runtime.version
						}
					}
				}
			}
		}
	}

	void eraseAll() {
		for (Map.Entry<SimulatorRuntime, List<SimulatorDevice>> entry : getDevices().entrySet()) {
			for (SimulatorDevice device in entry.getValue()) {
				if (device.available) {
					println "Erase simulator: '" + device.name + "' " + device.identifier
					simctl("erase", device.identifier)
				}
			}
		}
	}
}
