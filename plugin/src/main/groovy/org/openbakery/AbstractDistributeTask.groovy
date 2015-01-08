package org.openbakery

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.DefaultTask
import org.openbakery.signing.PackageTask

/**
 * User: rene
 * Date: 11/11/14
 */
class AbstractDistributeTask extends AbstractXcodeTask {

	private File archiveDirectory;

	File getApplicationBundleDirectory() {
		File appBundleDirectory = new File(getArchiveDirectory(), "Products/Applications/" + getApplicationNameFromArchive() + ".app")
		if (!appBundleDirectory.exists()) {
			throw new IllegalStateException("app directory not found: " + appBundleDirectory.absolutePath);
		}
		return appBundleDirectory;
	}


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

	void copyBundleToDirectory(File outputDirectory, File bundle) {
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs()
		}



		File destinationBundle = getDestinationBundleFile(outputDirectory, bundle)
		FileUtils.copyFile(getIpaBundle()	, destinationBundle)

		logger.lifecycle("Created bundle archive in {}", outputDirectory)
	}

	void copyIpaToDirectory(File outputDirectory) {
		copyBundleToDirectory(outputDirectory, getIpaBundle())
	}


	void copyDsymToDirectory(File outputDirectory) {
		copyBundleToDirectory(outputDirectory, getDSymBundle())
	}

	File getIpaBundle() {
		File packageDirectory = new File(project.getBuildDir(), PackageTask.PACKAGE_PATH)

		if (!packageDirectory.exists()) {
			throw new IllegalStateException("package does not exist: " + packageDirectory)
		}

		def fileList = packageDirectory.list(
						[accept: { d, f -> f ==~ /.*ipa/ }] as FilenameFilter
		).toList()


		if (fileList.isEmpty()) {
			throw new IllegalStateException("No ipa found")
		}

		return new File(packageDirectory, fileList.get(0))
	}



	File getDSymBundle() {
		File dSym = new File(getArchiveDirectory(), "dSYMs/" + getApplicationNameFromArchive() + ".app.dSYM");
		if (!dSym.exists()) {
			throw new IllegalStateException("dSYM not found: " + dSym)
		}
		return dSym;
	}



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

	def getApplicationNameFromArchive() {
		return getArchiveDirectory().name - ".xcarchive"
	}






}
