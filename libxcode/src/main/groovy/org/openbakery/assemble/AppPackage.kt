package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.util.FileHelper
import org.openbakery.util.ZipArchive
import org.slf4j.LoggerFactory
import java.io.File

class AppPackage(archive: File, commandRunner: CommandRunner) {

	companion object {
		val logger = LoggerFactory.getLogger("AppPackage")!!
	}

	var archive: File = archive
	var commandRunner: CommandRunner = commandRunner
	var fileHelper: FileHelper = FileHelper(commandRunner)


	fun createPackage(zipFile: File, applicationBundle: ApplicationBundle, includeSupportFolders: Boolean) {
		logger.debug("create package with {}", zipFile)
		if (!zipFile.parentFile.exists()) {
			zipFile.parentFile.mkdirs()
		}

		val baseDirectory = applicationBundle.baseDirectory

		var zipArchive = ZipArchive(zipFile, baseDirectory)
		zipArchive.add(applicationBundle.payloadDirectory)

		if (includeSupportFolders) {
			var swiftSupportFolder = addSwiftSupport(applicationBundle)
			if (swiftSupportFolder != null) {
				zipArchive.add(swiftSupportFolder)
			}
			enumerateExtensionSupportFolders(baseDirectory, zipArchive)
		}

		var bcSymbolMapsPath = File(baseDirectory, "BCSymbolMaps")
		if (bcSymbolMapsPath.exists()) {
			zipArchive.add(bcSymbolMapsPath)
		}

		zipArchive.create()
	}

	private fun enumerateExtensionSupportFolders(folder: File, zipArchive: ZipArchive) {
		var folderNames = listOf("MessagesApplicationExtensionSupport", "WatchKitSupport2")
		for (name in folderNames) {
			var supportFolder = File(folder, name)
			if (supportFolder.exists()) {
				zipArchive.add(supportFolder)
			}
		}
	}


	fun addSwiftSupport(applicationBundle: ApplicationBundle): File? {

		val frameworksPath = File(applicationBundle.applicationPath, "Frameworks")
		if (!frameworksPath.exists()) {
			return null
		}

		val swiftLibArchive = File(this.archive, "SwiftSupport")

		if (swiftLibArchive.exists()) {
			fileHelper.copyTo(swiftLibArchive, applicationBundle.baseDirectory)
			return File( applicationBundle.baseDirectory, "SwiftSupport")
		}
		return null
	}


}
