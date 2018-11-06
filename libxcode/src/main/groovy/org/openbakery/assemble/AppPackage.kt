package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.util.FileHelper
import java.io.File


class AppPackage(archive: File) {

	var archive: File
	var fileHelper : FileHelper

	init {
		this.archive = archive
		this.fileHelper = FileHelper(CommandRunner())
	}


	fun addSwiftSupport(payloadPath: File, applicationBundleName: String) : File? {

		val frameworksPath = File(payloadPath, "$applicationBundleName/Frameworks")
		if (!frameworksPath.exists()) {
			return null
		}

		val swiftLibArchive = File(this.archive, "SwiftSupport")

		if (swiftLibArchive.exists()) {
			fileHelper.copyTo(swiftLibArchive, payloadPath.parentFile)
			return File(payloadPath.parentFile, "SwiftSupport")
		}
		return null
	}


}
