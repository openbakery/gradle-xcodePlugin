//
//

package org.openbakery

import org.gradle.api.tasks.TaskAction

class XcodeUniversalLibraryTask extends AbstractXcodeTask {

	String libraryPathForTarget(String target) {
		return (project.xcodebuild.symRoot.path + "/" + project.xcodebuild.configuration + "-" + target + "/" + "lib" + project.xcodebuild.target + ".a")
	}

	@TaskAction
	def universalLibrary() {
		def deviceLibrary = new File(libraryPathForTarget("iphoneos"))
		def simulatorLibrary = new File(libraryPathForTarget("iphonesimulator"))

		if (!deviceLibrary.exists()) {
			logger.quiet "Library for device does not exist in:" + deviceLibrary.path
		}

		if (!simulatorLibrary.exists()) {
			logger.quiet "Library for simulator does not exist in:" + simulatorLibrary.path
		}

		if (!deviceLibrary.exists() || !simulatorLibrary.exists()) {
			logger.quiet("Unable to create universal library")
			return;
		}

		try {
			commandRunner.run(["lipo", "-create", deviceLibrary.path, simulatorLibrary.path, "-output", project.xcodebuild.buildRoot.path + "/lib" + project.xcodebuild.target + ".a"])
		} catch (Exception e) {
			logger.quiet("command failed: {}", e.getMessage(), e);
		}
	}
}
