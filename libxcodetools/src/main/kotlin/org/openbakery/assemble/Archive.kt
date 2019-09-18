package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.tools.CommandLineTools
import org.openbakery.util.FileHelper
import org.slf4j.LoggerFactory
import java.io.File

class Archive(applicationBundleFile: File, archiveName: String, tools: CommandLineTools, watchApplicationBundleFile: File? = null) {
	private var applications = "Applications"
	private var products = "Products"

	companion object {
		val logger = LoggerFactory.getLogger("AppPackage")!!
	}

	private val applicationBundleFile: File = applicationBundleFile
	private val watchApplicationBundleFile: File? = watchApplicationBundleFile
	private val tools: CommandLineTools = tools
	private val fileHelper: FileHelper = FileHelper(CommandRunner())
	private val archiveName: String = archiveName


	fun create(destinationDirectory: File) : File {
		val archiveDirectory = copyApplication(destinationDirectory)
		copyOnDemandResources(archiveDirectory)
		copyDsyms(archiveDirectory)
		return archiveDirectory
	}


	private fun copyApplication(destinationDirectory: File) : File {
		val archiveDirectory = getArchiveDirectory(destinationDirectory)
		val applicationDirectory = File(archiveDirectory, "$products/$applications")
		applicationDirectory.mkdirs()
		fileHelper.copyTo(applicationBundleFile, applicationDirectory)
		return archiveDirectory
	}


	private fun copyOnDemandResources(archiveDirectory: File) {
		val onDemandResources = File(applicationBundleFile.parent, "OnDemandResources")
		if (onDemandResources.exists()) {
			val destination = File(archiveDirectory, products)
			fileHelper.copyTo(onDemandResources, destination)
		}
	}

	private fun copyDsyms(archiveDirectory: File) {
		val dSymDirectory = File(archiveDirectory, "dSYMs")
		dSymDirectory.mkdirs()
		copyDsyms(applicationBundleFile.parentFile, dSymDirectory)

		if (watchApplicationBundleFile != null) {
			copyDsyms(watchApplicationBundleFile, dSymDirectory)
		}
	}

	private fun copyDsyms(archiveDirectory: File, dSymDirectory: File) {
		archiveDirectory.walk().forEach {
			if (it.isDirectory && it.extension.toLowerCase() == "dsym") {
				fileHelper.copyTo(it, dSymDirectory)
			}
		}
	}
	/*
	def copyDsyms(File archiveDirectory, File dSymDirectory) {

		archiveDirectory.eachFileRecurse(FileType.DIRECTORIES) { directory ->
			if (directory.toString().toLowerCase().endsWith(".dsym")) {
				copy(directory, dSymDirectory)
			}
		}

	}

	 */


	private fun getArchiveDirectory(destinationDirectory: File): File {
		val archiveDirectory = File(destinationDirectory, this.archiveName + ".xcarchive")
		archiveDirectory.mkdirs()
		return archiveDirectory
	}


}
