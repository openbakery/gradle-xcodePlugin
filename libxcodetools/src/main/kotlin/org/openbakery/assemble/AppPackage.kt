package org.openbakery.assemble

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.internal.impldep.com.esotericsoftware.minlog.Log
import org.openbakery.CommandRunner
import org.openbakery.bundle.ApplicationBundle
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.Codesign
import org.openbakery.codesign.CodesignParameters
import org.openbakery.codesign.ProvisioningProfileReader
import org.openbakery.codesign.ProvisioningProfileType
import org.openbakery.tools.CommandLineTools
import org.openbakery.util.FileHelper
import org.openbakery.util.ZipArchive
import org.openbakery.xcode.Type
import org.openbakery.xcode.Xcode
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FilenameFilter

class AppPackage(
	applicationBundle: ApplicationBundle,
	archive: File,
	tools: CommandLineTools,
	codesign: Codesign) {

	companion object {
		val logger = LoggerFactory.getLogger("AppPackage")!!
	}

	private val archive: File = archive
	private val tools: CommandLineTools = tools
	private val fileHelper: FileHelper = FileHelper(CommandRunner())
	private val codesignParameters: CodesignParameters = codesign.codesignParameters
	private val applicationBundle: ApplicationBundle = applicationBundle
	private val codesign: Codesign = codesign



	private val mainBundleProvisioningProfileReader by lazy {
		var bundleIdentifier = applicationBundle.mainBundle.bundleIdentifier
		ProvisioningProfileReader.getReaderForIdentifier(bundleIdentifier, codesignParameters.mobileProvisionFiles,	tools.commandRunner, tools.plistHelper)
	}

	private fun getProvisioningProfile(bundle: Bundle) : File? {
		val identifier = bundle.bundleIdentifier
		val reader = ProvisioningProfileReader.getReaderForIdentifier(identifier, codesignParameters.mobileProvisionFiles,	tools.commandRunner, tools.plistHelper)
		return reader?.provisioningProfile
	}



	fun createPackage(outputPath: File, name: String) {

		var includeSupportFolders = false
		var extension = "zip"

		if (applicationBundle.type == Type.iOS) {
			var profileType = getProvisioningProfileType()
			includeSupportFolders = profileType == ProvisioningProfileType.AppStore
			extension = "ipa"
		}

		val packagePath = File(outputPath, "$name.$extension")
		createPackage(packagePath, includeSupportFolders)
	}


	private fun createPackage(zipFile: File, includeSupportFolders: Boolean) {
		logger.debug("create package with {}", zipFile)
		if (!zipFile.parentFile.exists()) {
			zipFile.parentFile.mkdirs()
		}

		val baseDirectory = applicationBundle.baseDirectory

		var zipArchive = ZipArchive(zipFile, baseDirectory)
		zipArchive.add(applicationBundle.payloadDirectory)

		if (includeSupportFolders) {
			var swiftSupportFolder = addSwiftSupport()
			if (swiftSupportFolder != null) {
				zipArchive.add(swiftSupportFolder)
			}
			enumerateExtensionSupportFolders(baseDirectory, zipArchive)
		}

		var bcSymbolMapsPath = File(baseDirectory, "BCSymbolMaps")
		if (bcSymbolMapsPath.exists()) {
			zipArchive.add(bcSymbolMapsPath)
		}

		zipArchive.add(File(baseDirectory, "Symbols"))

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


	fun addSwiftSupport(): File? {
		logger.debug("add SwiftSupport")

		val frameworksPath = File(applicationBundle.applicationPath, "Frameworks")
		if (!frameworksPath.exists()) {
			logger.debug("frameworks path does not exist, so we are done. {}", frameworksPath)
			return null
		}

		val swiftLibArchive = File(this.archive, "SwiftSupport")

		if (!swiftLibArchive.exists()) {
			logger.debug("swiftLibArchive path does not exist, so we are done. {}", swiftLibArchive)

			return null
		}

		fileHelper.copyTo(swiftLibArchive, applicationBundle.baseDirectory)

		updateArchsForSwiftLibs(frameworksPath)
		return File(applicationBundle.baseDirectory, "SwiftSupport")
	}

	fun addSymbols() {
		val dSymPath = File(archive.absolutePath, "dSYMs")
		if (dSymPath.exists()) {
			logger.debug("Adding Symbols..")
			tools.commandRunner.run("mkdir", "-p", "${applicationBundle.baseDirectory.absolutePath}/Symbols")
			println(dSymPath.listFiles { _, name -> name.endsWith(".dSYM") }.orEmpty().map { it.absolutePath })
			for (dsym in dSymPath.listFiles { _, name -> name.endsWith(".dSYM") }.orEmpty()) {
				tools.commandRunner.run(
					"xcrun",
					"symbols",
					"-noTextInSOD",
					"-noDaemon",
					"--arch",
					"all",
					"--symbolsPackageDir",
					"${applicationBundle.baseDirectory}/Symbols",
					dsym.absolutePath
				)
			}

		} else {
			throw IllegalStateException("Tried adding Symbols, but archive does not contain dSYMs to generate them in ${dSymPath.absolutePath}.")
		}

	}


	fun updateArchsForSwiftLibs(frameworksPath : File) {
		logger.debug("updateArchsForSwiftLibs for {}", frameworksPath)
		val binaryArchs = tools.lipo.getArchs(applicationBundle.mainBundle.executable)
			.plus(listOf("armv7", "armv7s"))
			.distinct()

		for (file in frameworksPath.listFiles()) {
			if (file.extension == "dylib") {
				tools.lipo.removeUnsupportedArchs(file, binaryArchs)
			}
		}
	}

	fun getProvisioningProfileType(): ProvisioningProfileType? {
		return mainBundleProvisioningProfileReader?.profileType
	}



	/*
	The prepare function removes swift support dylibs that does not need to be included.
	Also adds the proper embedded provisioning profiles to the main bundle but also to the extensions
	 */
	fun prepareBundles() {

		for (bundle in applicationBundle.bundles) {
			if (applicationBundle.type == Type.iOS) {
				removeUnneededDylibsFromBundle(bundle)
				embedProvisioningProfileToBundle(bundle)
			}
		}
	}


	fun codesign() {
		val mainBundleIdentifier = applicationBundle.mainBundle.bundleIdentifier
		for (bundle in applicationBundle.bundles) {
			codesign.sign(bundle, mainBundleIdentifier)
		}
	}


	private fun removeUnneededDylibsFromBundle(bundle: Bundle) {
		val libswiftRemoteMirror = File(bundle.path, "libswiftRemoteMirror.dylib")
		if (libswiftRemoteMirror.exists()) {
			libswiftRemoteMirror.delete()
		}
	}

	private fun embedProvisioningProfileToBundle(bundle: Bundle) {
		val mobileProvisionFile = getProvisioningProfile(bundle) ?: return
		val profileExtension = FilenameUtils.getExtension(mobileProvisionFile.absolutePath)
		val embeddedProvisionFile = File(bundle.path, "embedded.$profileExtension")
		logger.info("provision profile - {}", embeddedProvisionFile)
		FileUtils.copyFile(mobileProvisionFile, embeddedProvisionFile)
	}


}
