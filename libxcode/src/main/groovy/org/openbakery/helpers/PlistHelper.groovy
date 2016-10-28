package org.openbakery.helpers

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.CommandRunnerException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Stefan Gugarel on 02/03/15.
 */
class PlistHelper {

	private static Logger logger = LoggerFactory.getLogger(PlistHelper.class)

	private File projectDirectory
	private CommandRunner commandRunner

	PlistHelper(File projectDirectory, CommandRunner commandRunner) {
		this.projectDirectory = projectDirectory
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
	def getValueFromPlist(plist, key) {
		if (plist instanceof File) {
			plist = plist.absolutePath
		}

		try {
			String result = commandRunner.runWithResult([
					"/usr/libexec/PlistBuddy",
					plist,
					"-c",
					"Print :" + key])

			if (result == null) {
				return null
			}

			if (result.startsWith("Array {")) {

				ArrayList<String> resultArray = new ArrayList<String>();

				String[] tokens = result.split("\n");

				for (int i = 1; i < tokens.length - 1; i++) {
					resultArray.add(tokens[i].trim());
				}
				return resultArray
			}
			return result;
		} catch (IllegalStateException ex) {
			return null
		} catch (CommandRunnerException ex) {
			return null
		}
	}

	void setValueForPlist(def plist, String key, List values) {
		deleteValueFromPlist(plist, key)
		addValueForPlist(plist, key, values)
	}

	void addValueForPlist(def plist, String key, List values) {
		commandForPlist(plist, "Add :" + key + " array")
		values.eachWithIndex { value, index ->
			commandForPlist(plist, "Add :" + key + ": string " + value)
		}
	}

	void addValueForPlist(def plist, String key, String value) {
		commandForPlist(plist, "Add :" + key + " string " + value)
	}


	void setValueForPlist(def plist, String key, String value) {
		commandForPlist(plist, "Set :" + key + " " + value)
	}


	void commandForPlist(def plist, String command) {
		File infoPlistFile;
		if (plist instanceof File) {
			infoPlistFile = plist
		} else {
			infoPlistFile = new File(projectDirectory, plist)
		}
		if (!infoPlistFile.exists()) {
			throw new IllegalStateException("Info Plist does not exist: " + infoPlistFile.absolutePath);
		}

		logger.debug("Set Info Plist Value: {}", command)
		commandRunner.run([
				"/usr/libexec/PlistBuddy",
				infoPlistFile.absolutePath,
				"-c",
				command
		])
	}

	void deleteValueFromPlist(def plist, String key) {
		commandForPlist(plist, "Delete " + key);
	}


	void createForPlist(def plist) {
		File infoPlistFile;
		if (plist instanceof File) {
			infoPlistFile = plist
		} else {
			infoPlistFile = new File(projectDirectory, plist)
		}

		FileUtils.writeStringToFile(infoPlistFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
				"<plist version=\"1.0\">\n" +
				"<dict>\n</dict>\n" +
				"</plist>")
	}
}
