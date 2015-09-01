package org.openbakery.simulators

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.openbakery.XcodePlugin
import org.openbakery.PlistHelper
import org.openbakery.simulators.SimulatorControl
import org.openbakery.AbstractXcodeTask
import org.openbakery.CommandRunnerException
import org.openbakery.simulators.SimulatorApp

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
    if (project.xcodebuild.isSDK(XcodePlugin.SDK_MACOSX)) {
      return bundle.absolutePath + "/Contents/"
    }
    return bundle.absolutePath + "/"
  }

  @TaskAction
  void run() {
    if (project.xcodebuild.infoPlist == null) {
      throw new IllegalArgumentException("No Info.plist was found! Check you xcode project settings if the specified target has a Info.plist set.")
    }
     
    def appBundles = getAppBundles(project.xcodebuild.outputPath)

    def infoPlist = getInfoPlistFile(appBundles)

    def bundleIdentifier = plistHelper.getValueFromPlist(infoPlist, "CFBundleIdentifier")

    try {
      simulatorControl.simctl("launch","booted", bundleIdentifier)
    } catch (CommandRunnerException ex) {
      org.codehaus.groovy.runtime.StackTraceUtils.sanitize(ex).printStackTrace()
      println "Unable to run "+bundleIdentifier
      throw ex
    } 
  }
}
