package org.openbakery.testdouble

import org.apache.commons.io.FileUtils

class XcodeDummy {

	File temporaryDirectory

	File xcodeDirectory
	File xcodebuild

	XcodeDummy() {
		temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "xcodedummy")
		xcodeDirectory = new File(temporaryDirectory, "/Applications/Xcode.app")
		xcodeDirectory.mkdirs()


		def toolsPath = new File(xcodeDirectory.absolutePath, "/Contents/Developer/usr/bin/")
		toolsPath.mkdirs()

		xcodebuild = new File(toolsPath, "xcodebuild")

		FileUtils.writeStringToFile(xcodebuild, "dummy")
	}


	void cleanup() {
		FileUtils.deleteDirectory(temporaryDirectory)
	}



}
