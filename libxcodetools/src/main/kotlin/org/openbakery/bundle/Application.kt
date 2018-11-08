package org.openbakery.bundle

import org.openbakery.CommandRunner
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Type
import java.io.File

// this should be replaced by the Bundle
class Application(path: File, type: Type) {

	private val plistHelper by lazy {
		PlistHelper(CommandRunner())
	}

	var type: Type = type
	var path: File = path


	fun getInfoPlist(): File {
		if (type == Type.macOS) {
			return File(path, "Contents/Info.plist")
		}
		return File(path, "Info.plist")
	}


	fun getBundleIdentifier(): String? {
		return plistHelper.getStringFromPlist(getInfoPlist(), "CFBundleIdentifier")
	}


}
