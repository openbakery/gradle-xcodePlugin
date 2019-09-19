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
		return this.create(destinationDirectory, false)
	}

	fun create(destinationDirectory: File, bitcodeEnabled: Boolean) : ApplicationBundle {

		val archiveDirectory = getArchiveDirectory(destinationDirectory)
		val applicationDirectory = copyApplication(destinationDirectory, archiveDirectory)
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
			if (it.extension.toLowerCase() == "dylib") {
				libNames.add(it.name)
			}
		}
		logger.debug("swiftlibs to add: {}", libNames)
		val swiftLibs = File(tools.lipo.xcodebuild.toolchainDirectory, "usr/lib/swift/${applicationBundle.platformName}")

		swiftLibs.walk().forEach {
			if (libNames.contains(it.name)) {
				val swiftSupportDirectory = getSwiftSupportDirectory(archiveDirectory, platformName)
				fileHelper.copyTo(it, swiftSupportDirectory)

				if (!bitcodeEnabled) {
					val destination = File(applicationBundle.frameworksPath, it.name)
					val commandList = listOf("/usr/bin/xcrun", "bitcode_strip", it.absolutePath, "-r", "-o", destination.absolutePath)
					tools.lipo.xcodebuild.commandRunner.run(commandList)
				}

			}
		}
	}

	private fun getSwiftSupportDirectory(archiveDirectory: File, platformName: String) : File {
		val swiftSupportDirectory = File(archiveDirectory, "SwiftSupport/$platformName")
		if (!swiftSupportDirectory.exists()) {
			swiftSupportDirectory.mkdirs()
		}
		return swiftSupportDirectory
	}

	/*
	def createFrameworks(Xcodebuild xcodebuild, ApplicationBundle appBundle, boolean bitcode) {
			File frameworksPath = appBundle.frameworksPath
			if (frameworksPath.exists()) {
				def libNames = []
				frameworksPath.eachFile() {
					libNames.add(it.getName())
				}

				logger.debug("swiftlibs to add: {}", libNames)

				File swiftLibs = new File(getXcode().getToolchainDirectory(), "usr/lib/swift/$appBundle.platformName")

				swiftLibs.eachFile() {
					logger.debug("candidate for copy? {}: {}", it.name, libNames.contains(it.name))
					if (libNames.contains(it.name)) {
						copy(it, getSwiftSupportDirectory(appBundle.platformName))

						if (!bitcode) {
							File destination = new File(frameworksPath, it.getName())
							commandRunner.run(["/usr/bin/xcrun", "bitcode_strip", it.absolutePath, "-r", "-o", destination.absolutePath])
						}
					}
				}
			}

			ApplicationBundle watchAppBundle = appBundle.watchAppBundle
			if (watchAppBundle != null) {
				createFrameworks(xcodebuild, watchAppBundle, true)
			}
		}
*/

}
