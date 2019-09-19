package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.tools.CommandLineTools
import org.openbakery.util.FileHelper
import org.openbakery.xcode.Type
import org.slf4j.LoggerFactory
import java.io.File

class Archive(applicationBundleFile: File, archiveName: String, type: Type, simulator: Boolean, tools: CommandLineTools, watchApplicationBundleFile: File? = null) {
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
	private val type: Type = type
	private val simulator: Boolean = simulator


	fun create(destinationDirectory: File) : ApplicationBundle {
		val archiveDirectory = getArchiveDirectory(destinationDirectory)
		val applicationDirectory = copyApplication(destinationDirectory, archiveDirectory)
		copyOnDemandResources(archiveDirectory)
		copyDsyms(archiveDirectory)

		val applicationBundle = ApplicationBundle(applicationDirectory, type, simulator, tools.plistHelper)
		return applicationBundle
	}


	private fun copyApplication(destinationDirectory: File, archiveDirectory: File) : File {
		val applicationDirectory = File(archiveDirectory, "$products/$applications")
		applicationDirectory.mkdirs()
		fileHelper.copyTo(applicationBundleFile, applicationDirectory)
		return File(applicationDirectory, applicationBundleFile.name)
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
