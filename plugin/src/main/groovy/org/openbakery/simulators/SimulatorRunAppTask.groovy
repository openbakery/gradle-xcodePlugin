package org.openbakery.simulators

import org.gradle.api.tasks.TaskAction
import org.openbakery.xcode.Type
import org.openbakery.XcodePlugin

class SimulatorRunAppTask extends AbstractSimulatorTask {

  public SimulatorRunAppTask() {
    setDescription("Install app on iOS Simulators")
    dependsOn(XcodePlugin.XCODE_BUILD_TASK_NAME)
    dependsOn(XcodePlugin.SIMULATORS_INSTALL_APP_TASK_NAME)
  }

  private File getInfoPlistFile(List<File> appBundles) {
    return new File(getAppContentPath(appBundles.last()) + "Info.plist")
  }

  private String getAppContentPath(File bundle) {
    return bundle.absolutePath + "/"
  }

  @TaskAction
  void run() {
    if (!project.xcodebuild.isSimulatorBuildOf(Type.iOS) && !project.xcodebuild.isSimulatorBuildOf(Type.tvOS)) {
      throw new IllegalArgumentException("Build is not a simulator build for iOS: Is " + project.xcodebuild.type + " and simulator flag is " + project.xcodebuild.simulator )
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
