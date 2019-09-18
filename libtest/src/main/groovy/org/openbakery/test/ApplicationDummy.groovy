package org.openbakery.test

import org.apache.commons.io.FileUtils
import org.openbakery.CommandRunner
import org.openbakery.bundle.Bundle
import org.openbakery.codesign.ProvisioningProfileType
import org.openbakery.testdouble.PlistHelperStub
import org.openbakery.util.PlistHelper
import org.openbakery.xcode.Extension
import org.openbakery.xcode.Type

class ApplicationDummy {


	File directory
	File payloadAppDirectory
	File applicationBundle
	PlistHelperStub plistHelperStub = new PlistHelperStub()


	List<File>mobileProvisionFile = []

	public ApplicationDummy(File directory) {
		this.directory = directory
		File payloadDirectory = new File(directory, "Payload")
		payloadAppDirectory = new File(payloadDirectory, "Example.app");
		applicationBundle = new File(directory, "Products/Applications/Example.app")
	}

	void cleanup() {
		FileUtils.deleteDirectory(directory)
	}

	File create(ProvisioningProfileType profileType) {
		// create dummy app
		File appDirectory = applicationBundle
		if (!appDirectory.exists()) {
			appDirectory.mkdirs()
		}

		String bundleIdentifier =  "org.openbakery.test.Example"

		FileUtils.writeStringToFile(new File(appDirectory, "Example"), "dummy")
		FileUtils.writeStringToFile(new File(appDirectory, "ResourceRules.plist"), "dummy")

		File infoPlist = new File(appDirectory, "Info.plist")

		PlistHelper helper = new PlistHelper(new CommandRunner())
		helper.create(infoPlist)
		helper.addValueForPlist(infoPlist, "CFBundleIdentifier", bundleIdentifier)
		helper.addValueForPlist(infoPlist, "CFBundleExecutable", "ExampleExecutable")

		plistHelperStub.setValueForPlist(infoPlist, "CFBundleIdentifier", bundleIdentifier)

		File profile = getMobileProvisionFile(profileType)
		if (profile != null) {
			mobileProvisionFile.add(profile)
		}

		return appDirectory
	}

	File getMobileProvisionFile(ProvisioningProfileType profileType) {
		switch (profileType) {
			case ProvisioningProfileType.Development:
				return new File("../libtest/src/main/Resource/Development.mobileprovision")
			case ProvisioningProfileType.AdHoc:
				return new File("../libtest/src/main/Resource/test.mobileprovision")
			case ProvisioningProfileType.Enterprise:
				return new File("../libtest/src/main/Resource/Enterprise.mobileprovision")
			case ProvisioningProfileType.AppStore:
				return new File("../libtest/src/main/Resource/Appstore.mobileprovision")
		}
	}

	File getMobileProvisionFileForExtension(Extension extension) {
		switch (extension) {
			case Extension.today:
				return new File("../libtest/src/main/Resource/extension.mobileprovision")
			case Extension.sticker:
				return new File("src/test/Resource/test2.mobileprovision")
		}
	}

	Bundle createBundle(boolean adHoc = true, boolean includeProvisioning = true) {
		return new Bundle(create(adHoc, includeProvisioning), Type.iOS, plistHelperStub)
	}

	File create(boolean adHoc = true, boolean includeProvisioning = true) {
		if (!includeProvisioning) {
			return create(null)
		}

		ProvisioningProfileType profileType = adHoc ? ProvisioningProfileType.AdHoc : ProvisioningProfileType.AppStore
		return create(profileType)
	}

	Bundle createPluginBundle(Extension extension = Extension.today) {
		return new Bundle(createPlugin(extension), Type.iOS, plistHelperStub)
	}

	File createPlugin(Extension extension = Extension.today) {
		File mobileProvision = getMobileProvisionFileForExtension(extension)
		switch (extension) {
			case Extension.today:
				createExtension("ExampleTodayWidget", "org.openbakery.test.ExampleWidget", mobileProvision)
                break
			case Extension.sticker:
				createExtension("ExampleStickerPack", "org.openbakery.test.ExampleSticker", mobileProvision)
				File messageExtensionSupportDirectory = new File(directory, "MessagesApplicationExtensionSupport")
				messageExtensionSupportDirectory.mkdirs()
				File messageExtensionSupportStub = new File(messageExtensionSupportDirectory, "MessagesApplicationExtensionStub")
				FileUtils.writeStringToFile(messageExtensionSupportStub, "fixture")
				break
		}

	}

	void createSwiftLibs(File applicationDirectory, File rootDirectory) {
		File libSwiftCore = new File(applicationDirectory, "Frameworks/libswiftCore.dylib")
		FileUtils.writeStringToFile(libSwiftCore, "dummy")
		File libSwiftCoreArchive = new File(rootDirectory, "SwiftSupport/libswiftCore.dylib")
		FileUtils.writeStringToFile(libSwiftCoreArchive, "dummy")
		File libswiftCoreGraphics = new File(applicationDirectory, "Frameworks/libswiftCoreGraphics.dylib")
		FileUtils.writeStringToFile(libswiftCoreGraphics, "dummy")

	}

	void createSwiftLibs() {
		createSwiftLibs(applicationBundle, directory)
	}

	void createFramework() {
		File framework = new File(applicationBundle, "Frameworks/My.framework")
		framework.mkdirs()
		File frameworkFile = new File(applicationBundle, "Frameworks/My.framework/My")
		FileUtils.writeStringToFile(frameworkFile, "dummy")
	}

	void createBCSymbolMaps() {
		File bcsymbolmapsDirectory = new File(directory, "BCSymbolMaps")
		bcsymbolmapsDirectory.mkdirs()
		FileUtils.writeStringToFile(new File(bcsymbolmapsDirectory, "14C60358-AC0B-35CF-A079-042050D404EE.bcsymbolmap"), "dummy")
		FileUtils.writeStringToFile(new File(bcsymbolmapsDirectory, "2154C009-2AC2-3241-9E2E-D8B8046B03C8.bcsymbolmap"), "dummy")
		FileUtils.writeStringToFile(new File(bcsymbolmapsDirectory, "23CFBC47-4B7D-391C-AB95-48408893A14A.bcsymbolmap"), "dummy")
	}

	private File createExtension(String name, String bundleIdentifier, File mobileProvision) {
		String widgetPath = "PlugIns/${name}.appex"
		File widgetsDirectory = new File(applicationBundle, widgetPath)
		FileUtils.writeStringToFile(new File(widgetsDirectory, name), "dummy");

		File infoPlistWidget = new File(payloadAppDirectory, widgetPath + "/Info.plist");
		plistHelperStub.setValueForPlist(infoPlistWidget, "CFBundleIdentifier", bundleIdentifier)

		File applicationBundleWidgetInfoPlist = new File(applicationBundle, widgetPath + "/Info.plist");

		PlistHelper helper = new PlistHelper(new CommandRunner())
		helper.create(applicationBundleWidgetInfoPlist)
		helper.addValueForPlist(applicationBundleWidgetInfoPlist, "CFBundleIdentifier", bundleIdentifier)

		mobileProvisionFile.add(mobileProvision)
		return widgetsDirectory
	}
}
