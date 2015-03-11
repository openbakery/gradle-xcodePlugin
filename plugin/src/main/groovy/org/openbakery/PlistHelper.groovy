package org.openbakery

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by Stefan Gugarel on 02/03/15.
 */
class PlistHelper {

	private static Logger logger = LoggerFactory.getLogger(PlistHelper.class)

	private Project project

	PlistHelper(Project project) {
		this.project = project
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
	def getValueFromPlist(plist, key, commandRunner) {
		if (plist instanceof File) {
			plist = plist.absolutePath
		}

		try {
			String result = commandRunner.runWithResult([
					"/usr/libexec/PlistBuddy",
					plist,
					"-c",
					"Print :" + key])

			if (result.startsWith("Array {")) {

				ArrayList<String> resultArray = new ArrayList<String>();

				String[] tokens = result.split("\n");

				for (int i = 1; i < tokens.length - 1; i++) {
					resultArray.add(tokens[i].trim());
				}
				return resultArray;
			}
			return result;
		} catch (IllegalStateException ex) {
			return null
		} catch (CommandRunnerException ex) {
			return null
		}
	}

	String setValueForPlist(def plist, String key, String value, CommandRunner commandRunner) {
		setValueForPlist(plist, "Set :" + key + " " + value, commandRunner)
	}


	String setValueForPlist(def plist, String command, CommandRunner commandRunner) {
		File infoPlistFile;
		if (plist instanceof File) {
			infoPlistFile = plist
		} else {
			infoPlistFile = new File(project.projectDir, plist)
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

}
