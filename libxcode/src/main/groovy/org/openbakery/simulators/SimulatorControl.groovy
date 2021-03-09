package org.openbakery.simulators

import groovy.json.JsonSlurper
import org.openbakery.*
import org.openbakery.xcode.Destination
import org.openbakery.xcode.Type
import org.openbakery.xcode.Version
import org.openbakery.xcode.Xcode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SimulatorControl {



	enum Section {
		DEVICE_TYPE("== Device Types =="),
		RUNTIMES("== Runtimes =="),
		DEVICES("== Devices =="),
		DEVICE_PAIRS("== Device Pairs ==")

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

	CommandRunner commandRunner
	Xcode xcode

	ArrayList<SimulatorDeviceType> deviceTypes
	ArrayList<SimulatorRuntime> runtimes
	HashMap<SimulatorRuntime, List<SimulatorDevice>> devices
	ArrayList<SimulatorDevicePair> devicePairs



	public SimulatorControl(CommandRunner commandRunner, Xcode xcode) {
		this.commandRunner = commandRunner
		this.xcode = xcode
	}


	void parse() {
		runtimes = new ArrayList<>()
		devices = new HashMap<>()
		deviceTypes = new ArrayList<>()
		devicePairs = new ArrayList<>()

		if (xcode.version.major < 12) {
			parseLegacy()
		} else {
			parseJson()
		}
	}

	void parseJson() {
		String simctlList = simctlWithResult("list", "--json")

		def jsonSlurper = new JsonSlurper()
		def jsonData = jsonSlurper.parseText(simctlList)

		if (jsonData.devicetypes instanceof ArrayList) {
			jsonData.devicetypes.eachWithIndex { item, index ->
				deviceTypes << new SimulatorDeviceType(item.name, item.identifier)
			}
		}

		if (jsonData.runtimes instanceof ArrayList) {
			jsonData.runtimes.eachWithIndex { item, index ->
				def runtime = new SimulatorRuntime(
					item.name,
					item.version,
					item.buildversion,
					item.identifier,
					item.isAvailable)
				this.runtimes << runtime
			}
		}


		if (jsonData.devices instanceof Map) {

			jsonData.devices.each { key, value ->
				def runtimeDevices = new ArrayList<>()

				value.eachWithIndex { item, index ->
					def device = new SimulatorDevice(item.name, item.udid, item.state, item.isAvailable)
					runtimeDevices << device
				}

				SimulatorRuntime runtime = getRuntimeFromIdentifier(key.toString())
				if (runtime != null) {
					devices[runtime] = runtimeDevices
				}
			}
		}

		if (jsonData.pairs instanceof Map) {
			jsonData.pairs.each { identifier, value ->
				def watch = getDeviceWithIdentifier(value.watch.udid)
				def phone = getDeviceWithIdentifier(value.phone.udid)

				if (watch != null && phone != null) {
					devicePairs << new SimulatorDevicePair(identifier, watch, phone)
				}
			}
		}

	}

	void parseLegacy() {
		Section section = null
		String simctlList = simctlWithResult("list")

		ArrayList<SimulatorDevice> simulatorDevices = null

		SimulatorDevicePair pair = null

		for (String line in simctlList.split("\n")) {

			Section isSection = Section.isSection(line)
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
					}

					break
				case Section.DEVICE_PAIRS:


					if (line ==~ /^\s+Watch.*/) {
						pair.watch = parseIdentifierFromDevicePairs(line)
					} else if (line ==~ /^\s+Phone.*/) {
						pair.phone = parseIdentifierFromDevicePairs(line)
					} else {
						// is new device pair
						pair = new SimulatorDevicePair(line)
						devicePairs.add(pair)
					}

					break


			}
		}
		Collections.sort(runtimes, new SimulatorRuntimeComparator())

	}

	SimulatorDevice parseIdentifierFromDevicePairs(String line) {
		def tokenizer = new StringTokenizer(line, "()");
		if (tokenizer.hasMoreTokens()) {
			// ignore first token
			tokenizer.nextToken()
		}
		if (tokenizer.hasMoreTokens()) {
			def identifier =  tokenizer.nextToken().trim()
			return getDeviceWithIdentifier(identifier)
		}
		return null

	}


	SimulatorRuntime parseDevicesRuntime(String line) {
		for (SimulatorRuntime runtime in runtimes) {
			if (line.equals("-- " + runtime.name + " --")) {
				return runtime
			}
		}
		return null
	}

	SimulatorRuntime getRuntimeFromIdentifier(String identifier) {
		for (SimulatorRuntime runtime in runtimes) {
			if (runtime.identifier == identifier) {
				return runtime
			}
		}
		return null
	}


	public void waitForDevice(SimulatorDevice device, int timeoutMS = 10000) {
		def start = System.currentTimeMillis()
		while ((System.currentTimeMillis() - start) < timeoutMS) {
			parse()
			def polledDevice = getDeviceWithIdentifier(device.identifier)
			if (polledDevice != null && polledDevice.state == "Booted")
				return
			sleep(500)
		}
		throw new Exception("Timeout waiting for " + device)
	}

	List<SimulatorRuntime> getRuntimes() {
		if (runtimes == null) {
			parse()
		}
		return runtimes
	}


	List<SimulatorRuntime> getRuntimes(String name) {
		List<SimulatorRuntime> result = []
		for (SimulatorRuntime runtime in getRuntimes()) {
			if (runtime.available && runtime.getName().startsWith(name)) {
				result << runtime
			}
		}
		Collections.sort(result, new SimulatorRuntimeComparator())
		return result
	}


	List<SimulatorRuntime> getRuntimes(Type type) {
		logger.debug("getRuntimes for {}", type)
		ArrayList<SimulatorRuntime> result = new ArrayList<>()

		for (SimulatorRuntime runtime in getRuntimes()) {
			if (runtime.type == type) {
				result.add(runtime)
			}
		}
		Collections.sort(result, new SimulatorRuntimeComparator())
		logger.debug("getRuntimes result {}", result)
		return result
	}

	SimulatorRuntime getMostRecentRuntime(Type type) {
		List<SimulatorRuntime> runtimes = getRuntimes(type);
		if (runtimes.size() > 0) {
			return runtimes.get(0)
		}
		return null;
	}



	SimulatorDevice getDevice(SimulatorRuntime simulatorRuntime, String name) {
		for (SimulatorDevice device in getDevices(simulatorRuntime)) {
			if (device.name.equalsIgnoreCase(name)) {
				return device
			}
		}
		null
	}


	SimulatorRuntime getRuntime(Destination destination) {
		for (SimulatorRuntime runtime in getRuntimes()) {
			if (runtime.type == Type.iOS && runtime.version == new Version(destination.os)) {
				return runtime
			}
		}
		return null
	}

	SimulatorDevice getDevice(Destination destination) {
		SimulatorRuntime runtime = getRuntime(destination);
		if (runtime != null) {

			for (SimulatorDevice device in getDevices(runtime)) {
				if (device.name.equalsIgnoreCase(destination.name)) {
					return device
				}
			}
		}
		return null
	}

	SimulatorDevice getDeviceWithIdentifier(String identifier) {
		for (Map.Entry<SimulatorRuntime, List<SimulatorDevice>> entry in devices.entrySet()) {
			for (SimulatorDevice device in entry.value) {
				if (device.identifier == identifier) {
					return device
				}
			}
		}
		return null
	}


	List <SimulatorDevice> getDevices(SimulatorRuntime runtime) {
		return getDevices().get(runtime)
	}


	HashMap<SimulatorRuntime, List<SimulatorDevice>> getDevices() {
		if (devices == null) {
			parse()
		}
		return devices
	}

	List<SimulatorDeviceType> getDeviceTypes() {
		if (deviceTypes == null) {
			parse()
		}
		return deviceTypes
	}

	List<SimulatorDevicePair> getDevicePairs() {
		if (devicePairs == null) {
			parse()
		}
		return devicePairs

	}


	String simctlWithResult(String... commands) {
		ArrayList<String>parameters = new ArrayList<>()
		parameters.add(xcode.getSimctl())
		parameters.addAll(commands)
		return commandRunner.runWithResult(parameters)
	}


	void simctl(String... commands) {
		ArrayList<String>parameters = new ArrayList<>()
		parameters.add(xcode.getSimctl())
		parameters.addAll(commands)
		commandRunner.run(parameters)
	}



	void deleteAll() {

		for (Map.Entry<SimulatorRuntime, List<SimulatorDevice>> entry : getDevices().entrySet()) {
			for (SimulatorDevice device in entry.getValue()) {
				if (device.available) {
					println "Delete simulator: '" + device.name + "' " + device.identifier
					simctlWithResult("delete", device.identifier)
				}
			}
		}
	}

	void createAll() {

		if (xcode.version.major < 12) {
			create(getDeviceTypes())
			return
		}

		def types = getDeviceTypes(["iPhone-8",
																"iPhone-8-Plus",
																"iPhone-11",
																"iPhone-11-Pro",
																"iPhone-11-Pro-Max",
																"iPhone-SE--2nd-generation-",
																"iPhone-12-mini",
																"iPhone-12",
																"iPhone-12-Pro",
																"iPhone-12-Pro-Max",
																"iPod-touch--7th-generation-",
																"iPad-Pro--9-7-inch-",
																"iPad-Pro--11-inch---2nd-generation-",
																"iPad-Pro--12-9-inch---4th-generation-",
																"iPad--8th-generation-",
																"iPad-Air--4th-generation-"])
		create(types)
	}

	void create(List<SimulatorDeviceType> deviceTypes) {
		for (SimulatorRuntime runtime in getRuntimes()) {

			if (runtime.available) {

				for (SimulatorDeviceType deviceType in deviceTypes) {

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
		pair()
	}

	void pair() {
		parse() // read the created ids again

		SimulatorRuntime iOSRuntime = getMostRecentRuntime(Type.iOS)
		SimulatorRuntime watchOSRuntime = getMostRecentRuntime(Type.watchOS)

		def identifiers = getPairingIdentifiers(iOSRuntime.version)

		identifiers.each {phoneIdentifierString, watchIdentifierString ->

			def phone = getDeviceWithTypeIdentifier(phoneIdentifierString, Type.iOS)
			def watch = getDeviceWithTypeIdentifier(watchIdentifierString, Type.watchOS)

			if (phone != null && watch != null) {
				logger.debug("pair phone: {}", phone)
				logger.debug("with watch: {}", watch)
				try {
					simctl("pair", phone.identifier, watch.identifier)
				} catch (CommandRunnerException ignored) {
					println "Unable to pair watch '" + watch.name + "' with phone '" + phone.name + "'"
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

	public void killAll() {
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
	}

	public void runDevice(SimulatorDevice device) {
		SimulatorRuntime runtime = getRuntime(device)
		if (runtime == null) {
			throw new IllegalArgumentException("cannot find runtime for device: " + device)
		}

		try {
			commandRunner.run([xcode.getPath() + "/Contents/Developer/usr/bin/instruments", "-w", device.identifier])
		} catch (CommandRunnerException ex) {
			// ignore, because the result of this command is a failure, but the simulator should be launched
		}
	}

	SimulatorRuntime getRuntime(SimulatorDevice simulatorDevice) {

		for (Map.Entry<SimulatorRuntime, List<SimulatorDevice>> runtime : devices) {
			for (SimulatorDevice device : runtime.value) {
				if (device.equals(simulatorDevice)) {
					return runtime.key
				}
			}
		}

		return null
	}

	List<Destination> getAllDestinations(Type type) {
		logger.debug("getAllDestinations for {}", type)
		def allDestinations = []

		getRuntimes(type).each { runtime ->
			allDestinations.addAll(getAllDestinations(type, runtime))
		}

		return allDestinations
	}

	List<Destination> getAllDestinations(Type type, SimulatorRuntime runtime) {
		def allDestinations = []

		getDevices(runtime).each { device ->
			Destination destination = new Destination()
			destination.platform = type.value + ' Simulator'
			destination.name = device.name
			destination.os = runtime.version.toString()
			destination.id = device.identifier
			allDestinations << destination
		}
		return allDestinations
	}


	SimulatorDeviceType getDeviceType(String identifier) {
		for (SimulatorDeviceType type : deviceTypes) {
			if (type.identifier == identifier) {
				return type
			}
			if (type.shortIdentifier == identifier) {
				return type
			}
		}
		return null
	}


	List<SimulatorDeviceType> getDeviceTypes(List<String> identifiers) {
		def result = new ArrayList<SimulatorDeviceType>()
		for (String identifier : identifiers) {
			def type = getDeviceType(identifier)
			if (type != null) {
				result << type
			}
		}
		return result
	}

	SimulatorDevice getDeviceWithTypeIdentifier(String identifier, Type type) {
		SimulatorDeviceType deviceType = getDeviceType(identifier)
		SimulatorRuntime runtime = getMostRecentRuntime(type)
		for (SimulatorDevice device : getDevices(runtime)) {
			if (device.name == deviceType.name) {
				return device
			}
		}
		return null
	}

	Map<String, String> getPairingIdentifiers(Version iOSVersion) {

		def pairing = [
			"9"   : [
				"iPhone-6"     : "Apple-Watch-38mm",
				"iPhone-6-Plus": "Apple-Watch-42mm"
			],
			"10"  : [
				"iPhone-6s"     : "Apple-Watch-38mm",
				"iPhone-6s-Plus": "Apple-Watch-42mm",
				"iPhone-7"      : "Apple-Watch-Series-2-38mm",
				"iPhone-7-Plus" : "Apple-Watch-Series-2-42mm"
			],
			"11"  : [
				"iPhone-6s"     : "Apple-Watch-38mm",
				"iPhone-6s-Plus": "Apple-Watch-42mm",
				"iPhone-7"      : "Apple-Watch-Series-2-38mm",
				"iPhone-7-Plus" : "Apple-Watch-Series-2-42mm"
			],
			"11.1": [
				"iPhone-7"     : "Apple-Watch-Series-2-38mm",
				"iPhone-7-Plus": "Apple-Watch-Series-2-42mm",
				"iPhone-8"     : "Apple-Watch-Series-3-38mm",
				"iPhone-8-Plus": "Apple-Watch-Series-3-42mm"
			],
			"12"  : [
				"iPhone-7"     : "Apple-Watch-Series-2-38mm",
				"iPhone-7-Plus": "Apple-Watch-Series-2-42mm",
				"iPhone-X"     : "Apple-Watch-Series-3-38mm",
				"iPhone-XS"    : "Apple-Watch-Series-4-40mm",
				"iPhone-XS-Max": "Apple-Watch-Series-4-44mm"
			],
			"13"  : [
				"iPhone-11": "Apple-Watch-Series-5-40mm",
				"iPhone-11-Pro-Max": "Apple-Watch-Series-5-44mm"
			],
			"14"  : [
				"iPhone-12-mini": "Apple-Watch-Series-5-40mm",
				"iPhone-12": "Apple-Watch-Series-5-44mm",
				"iPhone-12-Pro": "Apple-Watch-Series-6-40mm",
				"iPhone-12-Pro-Max": "Apple-Watch-Series-6-44mm"
			]
		]

		if (iOSVersion.major == 11 && iOSVersion.minor > 0) {
			return pairing["11.1"]
		}

		return pairing[iOSVersion.major.toString()]
	}
}
