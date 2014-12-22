package org.openbakery.signing

import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.TaskAction
import org.openbakery.AbstractXcodeTask
import org.openbakery.XcodePlugin

/**
 * Created by rene on 14.11.14.
 */
class PackageTask extends AbstractXcodeTask {


	PackageTask() {
		super();
		setDescription("Signs the app bundle that was created by the build and creates the ipa");
		dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
	}


	@TaskAction
	void packageApplication() throws IOException {
		if (!project.xcodebuild.sdk.startsWith("iphoneos")) {
			logger.lifecycle("not a device build, so no codesign needed");
			return;
		}

		if (project.xcodebuild.signing == null) {
			throw new IllegalArgumentException("cannot signed with unknown signing configuration");
		}


		File payloadPath = createPayload();

		// copy the build app
		// use cp to preserve permission

		ant.exec(failonerror: "true",

						executable: '/bin/cp') {
			arg(value: '-rp')
			arg(value: project.xcodebuild.applicationBundle.absolutePath)
			arg(value: payloadPath.absolutePath)
		}
		//FileUtils.copyDirectoryToDirectory(project.xcodebuild.applicationBundle, payloadPath);

		List<File> appBundles = getAppBundles(payloadPath)

		for (File bundle : appBundles) {
			embedProvisioningProfileToBundle(bundle)
			codesign(bundle)
		}

		createIpa(payloadPath);
	}

	File getMobileProvisionFileForIdentifier(String bundleIdentifier) {

		def mobileProvisionFileMap = [:]

		for (File mobileProvisionFile : project.xcodebuild.signing.mobileProvisionFile) {
			ProvisioningProfileIdReader reader = new ProvisioningProfileIdReader(mobileProvisionFile)
			mobileProvisionFileMap.put(reader.getApplicationIdentifier(), mobileProvisionFile)
		}

		for ( entry in mobileProvisionFileMap ) {
			if (entry.key.equalsIgnoreCase(bundleIdentifier) ) {
				return entry.value
			}
		}

		// match wildcard
		for ( entry in mobileProvisionFileMap ) {
			if (entry.key.equals("*")) {
				return entry.value
			}

			if (entry.key.endsWith("*")) {
				String key = entry.key[0..-2]
				if (bundleIdentifier.toLowerCase().startsWith(key)) {
					return entry.value
				}
			}
		}

		return null

	}


	private void createIpa(File payloadPath) {
        def xcrunCommand = project.xcodebuild.xcrunCommand
        ant.exec(failonerror: 'true',
                executable: xcrunCommand
        ) {
            arg(value: '-sdk')
            arg(value: 'iphoneos')
            arg(value: 'PackageApplication')
            arg(value: '-v')
            arg(value: "${project.xcodebuild.getOutputPath()}/${project.xcodebuild.bundleName}.app")
            arg(value: "-o")
            arg(value: "${project.xcodebuild.getOutputPath()}/${project.xcodebuild.bundleName}.ipa")
        }
		//createZip(project.xcodebuild.ipaBundle, payloadPath.getParentFile(), payloadPath)

		/*
		// use /usr/bin/zip to preserve permission
		ant.exec(failonerror: 'true',
						executable: '/usr/bin/zip',
						dir: payloadPath.getParentFile().absolutePath) {
			arg(value: '--symlinks')
			arg(value: '--verbose')
			arg(value: '--recurse-paths')
			arg(value: project.xcodebuild.ipaBundle.absolutePath)
			arg(value: 'Payload')

		}
		*/


	}

	private void codesign(File bundle) {

		def codesignCommand = [
						"/usr/bin/codesign",
		"--force",
		"--preserve-metadata=identifier,entitlements",
		//"--preserve-metadata=identifier,entitlements,resource-rules",
		//"--resource-rules=" + bundle.absolutePath + "/ResourceRules.plist",
		"--sign",
		project.xcodebuild.getSigning().getIdentity(),
		bundle.absolutePath,
		"--keychain",
		project.xcodebuild.signing.keychainPathInternal.absolutePath,
		]
		commandRunner.run(codesignCommand)


	}

	private void embedProvisioningProfileToBundle(File bundle) {
		File infoPlist = new File(bundle, "Info.plist");
		String bundleIdentifier = getValueFromPlist(infoPlist.absolutePath, "CFBundleIdentifier")

		File mobileProvisionFile = getMobileProvisionFileForIdentifier(bundleIdentifier);
		if (mobileProvisionFile != null) {
			File embeddedProvisionFile = new File(bundle, "embedded.mobileprovision");
			FileUtils.copyFile(mobileProvisionFile, embeddedProvisionFile);
		}
	}





	private File createPayload() throws IOException {
		File payloadPath = new File(project.xcodebuild.signing.signingDestinationRoot, "Payload");
		if (payloadPath.exists()) {
			FileUtils.deleteDirectory(payloadPath);
		}
		payloadPath.mkdirs();
		return payloadPath;
	}
}
