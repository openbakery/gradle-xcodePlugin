package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunnerException

class SimulatorsRunAppTask extends AbstractXcodeTask {
  SimulatorControl simulatorControl

  public SimulatorsRunAppTask() {
    setDescription("Install app on iOS Simulators")
    dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
    dependsOn(XcodePlugin.SIMULATORS_CREATE_TASK_NAME)
    simulatorControl = new SimulatorControl(project)
  }

  private File getInfoPlistFile(List<File> appBundles) {
    return new File(getAppContentPath(appBundles.last()) + "Info.plist")
  }

  private String getAppContentPath(File bundle) {
    return bundle.absolutePath + "/"
  }

  void executeTask() {
    if (!project.xcodebuild.isSDK(XcodePlugin.SDK_IPHONESIMULATOR)) {
      throw new IllegalArgumentException("Can only run app in simulator if the sdk is " + XcodePlugin.SDK_IPHONESIMULATOR + " but was " + project.xcodebuild.sdk)
    }

    if (project.xcodebuild.infoPlist == null) {
      throw new IllegalArgumentException("No Info.plist was found! Check you xcode project settings if the specified target has a Info.plist set.")
    }

    def appBundles = getAppBundles(project.xcodebuild.outputPath)

    def infoPlist = getInfoPlistFile(appBundles)

    def bundleIdentifier = plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier")
    if (bundleIdentifier == null) {
      throw new IllegalArgumentException("No bundle identifier was found!")

    }

    simulatorControl.simctl("launch", "booted", bundleIdentifier)
  }
}
