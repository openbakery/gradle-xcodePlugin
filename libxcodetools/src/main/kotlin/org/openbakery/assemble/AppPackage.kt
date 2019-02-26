package org.openbakery.assemble

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

class AppPackage(applicationBundle: ApplicationBundle, archive: File, codesignParameters: CodesignParameters, tools: CommandLineTools) {

	companion object {
		val logger = LoggerFactory.getLogger("AppPackage")!!
	}

	private val archive: File = archive
	private val tools: CommandLineTools = tools
	private val fileHelper: FileHelper = FileHelper(CommandRunner())
	private val codesignParameters: CodesignParameters = codesignParameters
	private val applicationBundle: ApplicationBundle = applicationBundle


	private val provisioningProfileReader by lazy {
		var bundleIdentifier = applicationBundle.mainBundle.bundleIdentifier
		ProvisioningProfileReader.getReaderForIdentifier(bundleIdentifier, codesignParameters.mobileProvisionFiles,	tools.commandRunner, tools.plistHelper)
	}


	fun getProvisioningProfile() : File? {
		return provisioningProfileReader?.provisioningProfile
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

		val frameworksPath = File(applicationBundle.applicationPath, "Frameworks")
		if (!frameworksPath.exists()) {
			return null
		}

		val swiftLibArchive = File(this.archive, "SwiftSupport")

		if (!swiftLibArchive.exists()) {
			return null
		}

		fileHelper.copyTo(swiftLibArchive, applicationBundle.baseDirectory)

		updateArchsForSwiftLibs(frameworksPath)
		return File( applicationBundle.baseDirectory, "SwiftSupport")
	}


	fun updateArchsForSwiftLibs(frameworksPath : File) {
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
		return provisioningProfileReader?.profileType
	}



	fun prepareBundles(applicationBundle: ApplicationBundle) {

		for (bundle in applicationBundle.bundles) {

			if (applicationBundle.type == Type.iOS) {
				removeUnneededDylibsFromBundle(bundle)
				//embedProvisioningProfileToBundle(bundle)
			}

		}

	}

	fun codesign(applicationBundle: ApplicationBundle, xcode: Xcode) {
		logger.debug("codesign: {}", applicationBundle)
		val codesign = Codesign(xcode, codesignParameters, tools.commandRunner, tools.plistHelper)
		for (bundle in applicationBundle.bundles) {
			codesign.sign(bundle)
		}
	}


	fun removeUnneededDylibsFromBundle(bundle: Bundle) {
		val libswiftRemoteMirror = File(bundle.path, "libswiftRemoteMirror.dylib")
		if (libswiftRemoteMirror.exists()) {
			libswiftRemoteMirror.delete()
		}
	}

	/*
	private fun embedProvisioningProfileToBundle(bundle: Bundle) {
		val mobileProvisionFile = getProvisioningProfile()
	}
	*/


	/* // migrate this to kotline
		private void embedProvisioningProfileToBundle(File bundle) {
		File mobileProvisionFile = getProvisionFileForBundle(bundle)
		if (mobileProvisionFile != null) {
			File embeddedProvisionFile

			String profileExtension = FilenameUtils.getExtension(mobileProvisionFile.absolutePath)
			embeddedProvisionFile = new File(getAppContentPath(bundle) + "embedded." + profileExtension)

			logger.info("provision profile - {}", embeddedProvisionFile)

			FileUtils.copyFile(mobileProvisionFile, embeddedProvisionFile)
		}
	}

	 */
}
