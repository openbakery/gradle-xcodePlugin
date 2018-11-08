package org.openbakery.util

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class PlistHelper {

	private static Logger logger = LoggerFactory.getLogger(PlistHelper.class)

	private CommandRunner commandRunner


	PlistHelper(CommandRunner commandRunner) {
		this.commandRunner = commandRunner
	}


/**
	 * Reads the value for the given key from the given plist
	 *
	 * @param plist
	 * @param key
     * @param commandRunner The commandRunner to execute commands (This is espacially needed for Unit Tests)
 	 *
	 * @return returns the value for the given key
	 */
	def getValueFromPlist(File plist, String key) {

		try {
			String result = commandRunner.runWithResult([
					"/usr/libexec/PlistBuddy",
					plist.absolutePath,
					"-c",
					"Print :" + key])

			if (result == null) {
				return null
			}

			if (result.startsWith("Array {")) {

				ArrayList<String> resultArray = new ArrayList<String>()

				String[] tokens = result.split("\n")

				for (int i = 1; i < tokens.length - 1; i++) {
					resultArray.add(tokens[i].trim())
				}
				return resultArray
			}
			return result
		} catch (IllegalStateException ex) {
			return null
		} catch (CommandRunnerException ex) {
			return null
		}
	}

	String getStringFromPlist(File plist, String key) {
		def value = getValueFromPlist(plist, key)
		if (value instanceof String) {
			return value
		}
		return null
	}

	void setValueForPlist(File plist, String key, List values) {
		deleteValueFromPlist(plist, key)
		addValueForPlist(plist, key, values)
	}

	void addValueForPlist(File plist, String key, List values) {
		commandForPlist(plist, "Add :" + key + " array")
		values.eachWithIndex { value, index ->
			commandForPlist(plist, "Add :" + key + ": string " + value)
		}
	}

	void addValueForPlist(File plist, String key, String value) {
		commandForPlist(plist, "Add :" + key + " string " + value)
	}

	void addValueForPlist(File plist, String key, Number value) {
		if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) {
			commandForPlist(plist, "Add :" + key + " real " + value)
		} else {
			commandForPlist(plist, "Add :" + key + " integer " + value.intValue())
		}
	}

	void addValueForPlist(File plist, String key, Boolean value) {
		if (value) {
			commandForPlist(plist, "Add :" + key + " bool true")
		} else {
			commandForPlist(plist, "Add :" + key + " bool false")
		}
	}


	void setValueForPlist(File plist, String key, String value) {
		commandForPlist(plist, "Set :" + key + " " + value)
	}


	void commandForPlist(File plist, String command) {
		if (!plist.exists()) {
			throw new IllegalStateException("Info Plist does not exist: " + plist.absolutePath);
		}

		logger.debug("Set Info Plist Value: {}", command)
		commandRunner.run([
				"/usr/libexec/PlistBuddy",
				plist.absolutePath,
				"-c",
				command
		])
	}

	void deleteValueFromPlist(File plist, String key) {
		commandForPlist(plist, "Delete " + key);
	}


	void create(File plist) {

		FileUtils.writeStringToFile(plist, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
				"<plist version=\"1.0\">\n" +
				"<dict>\n</dict>\n" +
				"</plist>")
	}
}
