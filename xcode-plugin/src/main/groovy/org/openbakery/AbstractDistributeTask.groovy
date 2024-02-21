package org.openbakery

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.Internal
import org.openbakery.packaging.PackageTask

import java.util.regex.Pattern

/**
 * User: rene
 * Date: 11/11/14
 */
class AbstractDistributeTask extends AbstractXcodeTask {

	private File archiveDirectory;

	@Internal
	File getProductsDirectory() {
		File productsDirectory = new File(getArchiveDirectory(), "Products")
		if (!productsDirectory.exists()) {
			throw new IllegalStateException("products directory not found: " + productsDirectory.absolutePath)
		}
		return productsDirectory
	}

	@Internal
	File getApplicationBundleDirectory() {
		File appBundleDirectory = new File(getProductsDirectory(), "Applications/" + getApplicationNameFromArchive() + ".app")
		if (!appBundleDirectory.exists()) {
			throw new IllegalStateException("app directory not found: " + appBundleDirectory.absolutePath);
		}
		return appBundleDirectory;
	}

	@Internal
	def getAppBundleInfoPlist() {

		File infoPlist = new File(getApplicationBundleDirectory(), "Info.plist")

		if (!infoPlist.exists()) {
			throw new IllegalStateException("Info.plist not found: " + infoPlist.absolutePath);
		}
		return infoPlist;
	}


	File getDestinationFile(File outputDirectory, String extension) {
		if (project.xcodebuild.bundleNameSuffix != null) {
			return new File(outputDirectory, getApplicationNameFromArchive() + project.xcodebuild.bundleNameSuffix + extension)
		}
		return new File(outputDirectory, getApplicationNameFromArchive() + extension)
	}

	File getDestinationBundleFile(File outputDirectory, File bundle) {
		if (!bundle.exists()) {
			throw new IllegalArgumentException("cannot find bundle: " + bundle)
		}

		String name = bundle.getName();
		String extension = ""
		int index = name.indexOf(".")
		if (index > 0) {
			extension = name.substring(index);
		}

		return getDestinationFile(outputDirectory, extension)
	}


	File copyBundleToDirectory(File outputDirectory, File bundle) {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}



		File destinationBundle = getDestinationBundleFile(outputDirectory, bundle)
		FileUtils.copyFile(getIpaBundle()	, destinationBundle)

		logger.lifecycle("Created bundle archive in {}", outputDirectory)
		return destinationBundle;
	}

	File copyIpaToDirectory(File outputDirectory) {
		return copyBundleToDirectory(outputDirectory, getIpaBundle())
	}


	File copyDsymToDirectory(File outputDirectory) {
		return copyBundleToDirectory(outputDirectory, getDSymBundle())
	}

	File getBundlePathByExtension(String extension) {
		File packageDirectory = new File(project.getBuildDir(), PackageTask.PACKAGE_PATH)

		if (!packageDirectory.exists()) {
			throw new IllegalStateException("package does not exist: " + packageDirectory)
		}

		Pattern pattern = Pattern.compile(".*\." + extension + "$")
		def fileList = packageDirectory.list(
						[accept: { d, f -> f ==~ pattern }] as FilenameFilter
		).toList()


		if (fileList.isEmpty()) {
			throw new IllegalStateException("No bundle with extension '" + extension + "' found")
		}

		if (fileList.size() > 1) {
			throw new IllegalStateException("Multiple bundles with extension '" + extension + "' found")
		}

		return new File(packageDirectory, fileList.get(0))
	}

	@Internal
	File getIpaBundle() {
		return getBundlePathByExtension("ipa")
	}

	@Internal
	File getAppBundle() {
		return getBundlePathByExtension("app")
	}


	@Internal
	File getDSymBundle() {
		File dSym = new File(getArchiveDirectory(), "dSYMs/" + getApplicationNameFromArchive() + ".app.dSYM");
		if (!dSym.exists()) {
			throw new IllegalStateException("dSYM not found: " + dSym)
		}
		return dSym;
	}


	@Internal
	def getArchiveDirectory() {
		if (archiveDirectory != null) {
			return archiveDirectory;
		}
		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER)
		if (!archiveDirectory.exists()) {
			throw new IllegalStateException("Archive does not exist: " + archiveDirectory)
		}

		def fileList = archiveDirectory.list(
						[accept: { d, f -> f ==~ /.*xcarchive/ }] as FilenameFilter
		).toList()
		if (fileList.isEmpty()) {
			throw new IllegalStateException("No xcarchive found")
		}
		return new File(archiveDirectory, fileList.get(0))

	}

	@Internal
	def getApplicationNameFromArchive() {
		def fileList = new File(getArchiveDirectory(), "Products/Applications").list(
						[accept: { d, f -> f ==~ /.*app/ }] as FilenameFilter
		).toList()

		if (fileList.isEmpty()) {
			throw new IllegalStateException("No app	found")
		}
		return fileList.get(0) - ".app"
	}






}
