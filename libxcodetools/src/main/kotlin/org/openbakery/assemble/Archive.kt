package org.openbakery.assemble

import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.tools.CommandLineTools
import org.openbakery.util.FileHelper
import org.openbakery.xcode.Type
import org.slf4j.LoggerFactory
import java.io.File

/**
 * This class creates an .xcarchive for the application bundle
 * Note: The implementation is not complete. Several methods must be migrated from the XcodeBuildArchiveTask to this class
 *
 * The idea is that this class just creates the xcarchive, but has no dependency and knowledge about gradle
 */
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
		return this.create(destinationDirectory, false)
	}

	fun create(destinationDirectory: File, bitcodeEnabled: Boolean) : ApplicationBundle {

		val archiveDirectory = getArchiveDirectory(destinationDirectory)
		val applicationDirectory = copyApplication(archiveDirectory)
		copyOnDemandResources(archiveDirectory)
		copyDsyms(archiveDirectory)

		val applicationBundle = ApplicationBundle(applicationDirectory, type, simulator, tools.plistHelper)
		if (type == Type.iOS) {
			copyFrameworks(applicationBundle, archiveDirectory, applicationBundle.platformName, bitcodeEnabled)
		}

		if (applicationBundle.watchAppBundle != null) {
			copyFrameworks(applicationBundle.watchAppBundle, archiveDirectory, applicationBundle.watchAppBundle.platformName, true)
		}
		return applicationBundle
	}


	private fun copyApplication(archiveDirectory: File) : File {
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
			if (it.isDirectory && it.extension.lowercase() == "dsym") {
				fileHelper.copyTo(it, dSymDirectory)
			}
		}
	}

	private fun getArchiveDirectory(destinationDirectory: File): File {
		val archiveDirectory = File(destinationDirectory, this.archiveName + ".xcarchive")
		archiveDirectory.mkdirs()
		return archiveDirectory
	}

	private fun copyFrameworks(applicationBundle: ApplicationBundle, archiveDirectory: File, platformName: String, bitcodeEnabled: Boolean) {
		if (!applicationBundle.frameworksPath.exists()) {
			logger.debug("framework path does not exists, so we are done")
			return
		}
		var libNames = ArrayList<String>()

		applicationBundle.frameworksPath.walk().forEach {
			if (it.extension.lowercase() == "dylib") {
				libNames.add(it.name)
			}
		}
		logger.debug("swift libraries to add: {}", libNames)

		val swiftLibraryDirectories = getSwiftLibraryDirectories(platformName)

		libNames.forEach { libraryName ->
			val library = getSwiftLibrary(swiftLibraryDirectories, libraryName)
			if (library != null) {
				val swiftSupportDirectory = getSwiftSupportDirectory(archiveDirectory, platformName)
				fileHelper.copyTo(library, swiftSupportDirectory)

				if (!bitcodeEnabled) {
					val destination = File(applicationBundle.frameworksPath, library.name)
					val commandList = listOf("/usr/bin/xcrun", "bitcode_strip", library.absolutePath, "-r", "-o", destination.absolutePath)
					tools.lipo.xcodebuild.commandRunner.run(commandList)
				}
			}
		}
	}

	private fun getSwiftLibrary(libraryDirectories: List<File>, name: String) : File? {
		for (directory in libraryDirectories) {
			val result = getSwiftLibrary(directory, name)
			if (result != null) {
				return result
			}
		}
		return null
	}

	private fun getSwiftLibrary(libraryDirectory: File, name: String) : File?{
		val result = File(libraryDirectory, name)
		if (result.exists()) {
			return result
		}
		return null
	}

	private fun getSwiftLibraryDirectories(platformName: String) : List<File> {
		var result = ArrayList<File>()
		val baseDirectory = File(tools.lipo.xcodebuild.toolchainDirectory, "usr/lib/")
		baseDirectory.listFiles().forEach {
			if (it.isDirectory && it.name.startsWith("swift")) {
				val platformDirectory = File(it, platformName)
				if (platformDirectory.exists()) {
					result.add(platformDirectory)
				}
			}
		}
		return result
	}

	private fun getSwiftSupportDirectory(archiveDirectory: File, platformName: String) : File {
		val swiftSupportDirectory = File(archiveDirectory, "SwiftSupport/$platformName")
		if (!swiftSupportDirectory.exists()) {
			swiftSupportDirectory.mkdirs()
		}
		return swiftSupportDirectory
	}


}
