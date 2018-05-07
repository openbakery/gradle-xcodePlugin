# 0.15.4 (TBA)

# 0.15.3 (May 7, 2018)

Bugfixes
* Enabled codesigning for iOS Simuator builds, otherwise unit tests with binary frameworks does not workspace. Issue #378
* Updated WatchKit Support. Issue #390
* Updated Cartage Support
 * Only build specific platform. Issue #384
 * Bootstrap support: Issue # 389
* Support for transformations on build settings. Issue #382
* Updated path for the codecoverage in Xcode 9. Issue #381
* Update keychain partition list. Issue #375

# 0.15.2 (January 24, 2018)

Changes
* Increased version number, because it looks like that on plugins.gradle.org the version 0.15.1 does not work.

# 0.15.1 (January 8, 2018)

Changes
* Some updates to support Xcode 9: Issue #369
* The entitlments for extensions are not modified using the values from the `entitlements` parameters

Bugfixes
* the BCSymbolMaps should be in the xcarchive root directory


# 0.15.0 (June 2, 2017)

Changes
* renamed Type.OSX to Type.macOS, you can use both 'OSX' and 'macOS' for the xcodebuild.type parameter
* Introduced an entitlements parameter for signing that can contain a map with the entitlments settings that should be added
* Added validation of signing certificate (if it is valid and has not expired): Issue #348
* When installing an App to the iOS simulator, the App gets codesigned first: Issue #346
* Added Bitcode support. This can be enabled using the new `bitcode` flag. e.g. `xcodebuild { bitcode = true }`: Issue #233


# 0.14.5 (January 26, 2017)

Changes
* Upgraded to OCLint 0.11: Issue #332
* Better output when compiling swift source file that also shows errors and warnings.

Bugfixes
* Fixed signing when using macOS Sierra: Issue #316
* Include the onDemandResources when creating the archive and package: Issue #309

# 0.14.4 (November 29, 2016)

Changes
* Added a XcodebuildForTest task that create build for running unit tests (see xcodebuild build-for-testing)
  Here a testbundle is created that contains the xctestrun file and the App bundle
* Added a XcodeTestRunTestTask that executes the unit tests from the test bundle that was created with XcodebuildForTest
  Here only the tests are executed, no build is taking place here.
* Codesign is also disabled for iOS simulator builds

Bugfixes
* Added the workspace parameter when getting the build settings: Issue #304
* Fixed archive fails when executed outside the build directory: Issue #312
* Fixed that the build directory can be changed for the package task: Issue #314
* Unit test on the device is now possible again with the new XcodebuildForTest and XcodeTestRunTestTask: Issue #274

# 0.14.3 (October 20, 2016)

Changes
* Cocoapods is now only bootstraped if it does not exist globally. Issue #299
* Add that the dSYMs from the framework are also included in the xcarchive
* User the SHA instead of the developer identifier for codesigning. (Needed for macOS Sierra)
* Updated the CoverageReport lib that contains fix for issue #303

Bugfixes
* Added `clean` to the `xcodebuild -showBuildSettings` command which should fix a hang when using core data: Issue #298

# 0.14.2 (September 26, 2016)

Changes
* Xcode 8 Support
 * App Bundle should not contain the libswiftRemoteMirror.dylib
* Added tvOS support by Electryc. Issue #295
* XcodebuildTask and XcodeTestTask can now override the global xcodebuild settings for building for supporting multiproject build
* Added an option that a custom entitlements file can be specified for codesigning
* SimulatorControl now also creates the tvOS simulator
* Initial Carthage support. Carthage is not bootstraped, so make sure that Carthage is installed if your project uses it.
* Embedded provisioning file now honors the team-id when expanding the identifiers
* Updated that the test result is not taken from the xcodebuild output, but from the TestSummaries.plist, because there are causes where the xcodebuild output stopps.
* Test Result from the TestSummaries.plist is merged the the infos from the xcodebuild output like duration and stdout
* Archive supports now creating the xcarchive using the xcodebuild command. For this set the parameter `xcodebuild.useXcodebuildArchive` to true. (See issue also #293)

Bugfixes
* `version` parameter for settings the xcode version was ignored
* Updated the XcodeBuildArchiveTask that the configured swift toolchain is used, to that the proper swift libs are included into the archive
* change the plist decode from the provisioning profile that a exit code != 0 does not crash the build: Issue #297
* Make "derivedDataPath" optional #291


Note: There is no 0.14.1 version. (Reason is a typo)

# 0.14.0 (July 22, 2016)

Changes
* OCLint report is stored now in build/report/oclint: #263
* Updated OCLint to 0.10.3
* Set Coverage report directory to build/report/coverage
* Set CPD report directory to build/report/cpd/


Bugfixes
* Added that the SwiftSupport folder is only included in Appstore builds, but not in AdHoc builds: #232
* Make compatible with gradle 2.14. (Older version of gradle will not work anymore with this version)


# 0.13.1 (Mai 2, 2016)

Bugfixes
* Added that in the ipa app extentions for iOS apps do not contain frameworks, because the should be in the base app
* Fixed that the SwiftSupport is only created for Swift projects: #260

# 0.13.0 (March 4, 2016)

Changes
* Added new code coverate support for the new reports Xcode 7 creates: #225
 * This ables coverage reports for Swift projects
 * Also added support for coverage reports for OS X projects
* Updated OCLint to 0.10.2
* Added 'cocoapodsUpdate' task: Issue: #241
* Added that the project file can be specified as parameter

Bugfixes
* Fixed that empty Frameworks directory gets deleted: issue #242
* Fixed that on simulatorCreate the simualtors gets killed first, to avoid a failure is a simulator is running


# 0.12.7 (January 27, 2016 )

Bugfixes
* Added support for wildcard identifiers when extracting the entitlment from the provisioning profile: fixes issue #230
* Fixed that when building a commandline tool that has no info plist that it does not fail: issue #235


# 0.12.5 (Dezember 16, 2015)

Bugfixes
* Reverted the removal of the SwiftSupport, because the appstore needs this. Issue #231


# 0.12.4 (Dezember 9, 2015)

Bugfixes
* Fixed that the Keychain Access Group is propery set in the entitlements: Issue #226 
* Fixed memory problem with the CommandRunner: Issue #222
* Fixed that the SwiftSupport in not included anymore, because it is not needed
* For simulator builds also the identifier is used. This avoids build failues if a simulator of the same type and name exists twice


# 0.12.3 (November 11, 2015)

Changes
* Removed Xcode 5 support

Bugfixes
* simulatorStart works now
* simulatorRunApp starts now the configured app in the simulator
* simpler destination syntax for specifing the simulators
* Fixed that the keychain-access-group is not removed from the entitlements during signing: Issue #220

# 0.12.2 (October 28, 2015)

Bugfixes
* Codesigning failed with some projects that contains frameworks

# 0.12.1 (October 28, 2015)

Changes
* Watchkit support. Project with Xcode 6 and Watchkit can now be build and is properly signed

Bugfixes
* Build failes if the specified simualtor is not found: Issue #219
* All build configurations are now processed: Issue #218x

# 0.12.0 (October 16, 2015)

Changes
* Better Xcode 7 and 7.1 support
* watchOS support. Now iOS Apps with a watchOS App included can be build and signed.
 * For this the sdk parameter was removed and replaced by the type and simualtor parameter
* The xcodebuild task does not sign anymore more. 
 * Signing task only place in the package tasks
 * The proper entitlements for signing are now read from the provisioning profile
 * The keychain is created prior to the package task.
* The package task is not depended to the archive task anymore. The reason is that sometimes you can package archives that aready exists.

Bugfixes
* xcodebuild task doesn't fail build on compiler errors: Issue #214
* Build failed because of invalid "simctl delete": Issue #211

# 0.11.6 (September 4, 2015)

Changes
* Reverted the WatchKit pull request, because of build failure. See Issue #185, #205

# 0.11.5 (September 1, 2015)

Changes
* Added support for CPD reports. (Thanks to rahulsom) Issue #191
* Added that OCLint rules can be disabled
* Added support for WatchKit (Thanks to icecrystal23) Issue #185
* Fixed an issue where codesign and crashlytics would use the wrong version of developer tools (Thanks to icecrystal23) Issue #197
* Added new simulator task for starting the simulator and running an app in the simulator (Thanks to jeduden) Issue #204

## 0.11.4 (July 28, 2015)

Changes
* added OCLint support: Fixes #57

## 0.11.3 (Juli 17, 2015)

Changes

* Much nicer build output and shorter output. Only warnings and errors are printed. The rest is shown in the progress.
* Added derivedDataPath parameter and if not set the derivedData are in the build/deriveData directory
* Added that the iOS9 support:
 * The simulator for iOS9 can not be deleted and created

Bugfixes

* The cocoapods are now updated if the Podfile.lock and Manifest.lock are not equal: (Thanks to rahulsom)
* Handle case where a test suite doesn't state that it has completed (Thanks to jaleksynas)

## 0.11.2 (Mai 19, 2015)

Bugfixes

* Change that the gradle keychains are not added to the search list, to avoid that a wrong keychain is taken during codesign. Issue #181
* Fixed codesigning when the project contains frameworks (iOS)

## 0.11.1 (May 7, 2015)

Bugfixes

* Reverted: Disabled codesiging at xcodebuild. Reason is that the entitlements are not added, therfor the signed app cannot be uploaded to itunes connect
* Fixed that the conversion to binary does not fail if the plist is readonly: issue #179

## 0.11.0 (April 30, 2015)

Changes

* Renamed plugin id to org.openbakery.xcode-plugin
  * plugin is now on https://plugins.gradle.org
* Signing identity is now optional, because it is read from the keychain
* Crashlytics support: Thanks to @achretien
* Added new simulator commands:
 * simulatorsCreate: All simulators can be recreated. Here all simulators are deleted and created again
 * simulatorsClean: Content and settings of all simulators can be erased
* Disabled codesiging at xcodebuild, is done at the package task only
 * Keychain and Provisioning tasks dependencies moved from xcodebuild task to the package task

Bugfixes

* Keychain path only works with absolute path: Issue #150


## 0.10.3 (March 31, 2015)

Changes

* OS X support:
 * 'archive' task creates an xcarchive
 * You can now create a signed app with the 'package' task

Bugfixes

* Tests on unavailable device: #161

## 0.10.2.1 (March 27, 2015)


Bugfixes

* Fixed method call on null object: Issue #164
* Fixed logic error: Issue #156


## 0.10.2 (Februar 20, 2015)

Changes

* Removed Testflight because it shut down on the Februar 26, 2015

Bugfixes

* Fixed Deploygate upload: Issue #143
* Fixed Deploygate message with non Ascii charaters: iusse #144 (Thanks to katsutomu)


## 0.10.1 (Februar 3, 2015)

Bugfixes

* Fixed archive for Swift apps: #132

## 0.10.0 (Februar 2, 2015)

Changes

* Renamed all tasks with dashes in the name and removed the dash, e.g hockeykit-clean to hockeykitClean
* New package task to creates IPAs
 * supports Swift
 * supports App Extensions: Issue #96
* Codesign task was removed (is replaced by package task)
* archive task now creates a xcarchive: Issue #1
* distribution tasks (e.g. hockeykit) does not have a dependences to the build tasks anymore, if you want this define this in you build.gradle file
* distribution tasks uses the xcarchive as input now
* Support for the new Apple Testflight: Issue #98
 * Added new 'appstoreValidate' that validates the created ipa with the apple appstore:
 * Added new 'appstoreUpload' target that submits the ipa to apple
* Add custom keychain timeout: Issue #105 (Thanks to icecrystal23)
* Environment variables passing support: Issue #88 (Thanks to liliomk)

Bugfixes:

* Fixed parsing of Swift unit test output: Issue #124 (Thanks to tanob)
* Provision-cleanup should always remove bad links: Issue #123 (Thanks to icecrystal23)


## 0.9.15 (November 25, 2014)

Changes

* Pods can no be reinstalled with --refresh-dependencies: Issue #90
* If the provisioning profile has expired, the build fails early now, not after the build when signing: Issue #83
* Added that the value for workspace parameter is set automatically: Issue #86

Bugfixes

* Fixed that the provisioning profile is set: Issue #97
* Fixed test output parsing: Issue #106 (Thanks to icecrystal23)

## 0.9.14 (October 31, 2014)

Bugfixes

* Fixed codesign with Xcode 6.1: Issue #94
* Fixed unit test output, so that the number of test are shown with Xcode 6 #92
* Fixed that the additionalParameters are also used in test builds

## 0.9.13 (Sepember 23, 2014)

Changes

* Added the the xcode version can be selected: Issue #6

Bugfixes

* infoplist.version value is ignored

## 0.9.12 (August 27, 2014)

Changes

* Build in Cocoapods support. If a Podfile is found a pod install is perform automatically. If cocoapods is not install, it is installed for the current user. Issue #75
* Add support for modifing the info plist using plistbuddy commands. See Issue #74
* For unit testing destination are not optionals. I no destination is specified then the unit tests are executed on every available simulator.
  This works both for Xcode5 and Xcode6 (here the new simctl is used)
* The plugin should be more memory efficent now, because the output of the xcodebuild command that can be several megabytes is not hold in the mememory anymore, it is stored in an output file.

## 0.9.11 (July 22, 2014)

Changes

* Added appledoc target to generate documentation using the appledoc tool form gentlebytes: https://github.com/tomaz/appledoc
* Added code coverage target using gcovr: http://gcovr.com

Bugfixes

* Fixed error when no test is available: issue #72

## 0.9.10 (July 16, 2014)

Changes

* Added JUnit XML output for test results: Issue #56
* Compatibility for Xcode 6
 * The arch is not added to a simulator build, it is only added when specified using the xcodebuild.arch parameter

Bugfixes

* When unit test cases are compiled after a test was already executed, then no output was shown

## 0.9.9 (June 27, 2014)

Changes

* Codesign does not fail anymore when multiple keychain exists with the same key. (The PackageApplication script is copied and patched, and the proper keychain is passed as parameter to the codesign command )

## 0.9.8 (May 7, 2014)

Bugfixes

* Possible fix for Issue 55: Only add keychain options to the xcodebuild command when doing a device build

## 0.9.7 (May 5, 2014)

Changes

* Add Sparkle support (http://sparkle.andymatuschak.org). Thanks to gugmaster.
* Add DeployGate support (https://deploygate.com). Thanks to henteko.

Bugfixes

* When using asset catalogs for app icons then the hockey-image task did not find the app icons

## 0.9.6 (Mar 20, 2014)

Changes

* Remove _unitTestTarget_ parameter that is not needed with xcode 5

Bugfixes

* Fixed bundleDisplayName mapping: Issue #52

## 0.9.5 (Feb 11, 2014)

Bugfixes

* Fixed HockeyApp upload: Issue #48


## 0.9.4 (Feb 10, 2014)

Changes

* Reverted issue #36
* Added the the whole xcodebuild output is stored in an file: issue #46

Bugfixes

* Fixed warning that TaskContainer.add() is deprecated


## 0.9.3 (Jan 20, 2014)

Bugfixes

* Fixed issue #36


## 0.9.2 (Nov 26, 2013)

Changes

* Change that the project is also compiled using 'build' instead of 'xcodebuild'
* Added support for an architecture list: Issue #33

Bugfixes

* Fixed that the clean task is defined by the plugin, instead the clean asks are appended: Issue #38
* Build output for unit test did not show any failures if an exception occured

## 0.9.1 (Nov 12, 2013)

Changes

* Implemented much nice build output when compiling and also for the unit tests

## 0.9.0  (Sep 27, 2013)

Changes

* XCode5 support. This version only works with Xcode5
* Updated Keychain creation and cleanup for OS X Mavericks
* Updated unit test support

## 0.8.6 (Aug 19, 2013)

Changes:

* Merged pull request: https://github.com/openbakery/gradle-xcodePlugin/pull/31

## 0.8.5 (Aug 2, 2013)

Bugfixes:

* Merged pull request: https://github.com/openbakery/gradle-xcodePlugin/pull/29

## 0.8.4 (June 21, 2013)

Bugfixes:

* fixed hockeykit-image task that the image is created from the app icon as intended.

## 0.8.3 (June 18, 2013)

Bugfixes:

* fixed exception in hockeyapp-prepare task: issue #28

## 0.8.2 (June 18, 2013)

Bugfixes:

* fixed that on a simulator build the keychain is not created and provisioning profile is not installed

## 0.8.1 (June 6, 2013)

Bugfixes:

* fixed executing unit test. Where skipped because was TEST_HOST was set.
* fixed provisioning cleanup and create task that failed if the Provisioning Profiles directory does not exist

## 0.8.0 (June 3, 2013)

Features:

* new _xcodebuild.signing_ parameters, replaces provisioning.mobileprovisionUri, keychain.certificateUri,	keychain.certificatePassword,	keychain.keychainPassword, keychain.keychain
* added new parameter _hockeykit.notes_
* added new parameter _keychain.keychain_
* added hockeyapp target that was created by wfrank2509
* reworked keychain and provisioning file handling so that multiple parallel builds should work

Changes:

* removed _xcodebuild.buildRoot_: The gradle buildDir variable is used instead.
* removed _keychain.keychainName_

Bugfixes:

## 0.7.2 (Februar 8, 2013)

Bugfixes:

* hockeykit image does not fail anymore when no icon image was found

## 0.7.1

Bugfixes:

* fixed reading the UUID from the mobile provisioning file

## 0.7.0

Changes:

* added workspace supported

## 0.6.6

Bugfixes:

* fixed the archive task: The *.app and *.app.dSYM directory were not included recursivly

## 0.6.5

Changes:

* default _hockekit.displayName_ is now CFBundleDisplayName instead of CFBundleIdentifier

## 0.6.4 (September 5, 2012)

Changes:

* Added _xcodebuild.unitTestTarget_
* Added bundleIdentifierSuffix #12

## 0.6.3 (September 4, 2012)

Changes:

* Added inital TestFlight support with _testflight_ target that uploads the build to TestFlight. (see Readme for the testflight parameters)
* Added support for Scheme with the new configuration parameter _xcodebuild.scheme_. Issue #1
* Added that after codesign the keychain and provisioning profile gets cleaned up. Issue #9

## 0.6.2 (August 31, 2012)

Bugfixes:

* fixed that the hockeykit manifest xml file was not valid xml

## 0.6.1 (August 31, 2012)

Bugfixes:

* fixed cleanup: Issue #8
* fixed that if the short version entry is missing in the Info.plist the plist modify task does not fail


## 0.6.0 (August 30, 2012)

Changes:

* _xcodebuild.archiveVersion_ remame to _xcodebuild.bundleNameSuffix_
* _infolist.versionExtension_ remove an added the following keys instead: _version_, _versionSuffix_, _versionPrefix_
* Short Version String can now be also set using _shortVersionString_, _shortVersionStringSuffix_ and _shortVersionStringPrefix_
* Renamed _hockeykit.appName_ to _hockeykit.displayName_
* Renamed _hockeykit.version_ to _hockeykit.versionDirectoryName_
* _xcodebuild.additionalParameters_ now also accepts arrays

Bugfixes:

* changed to absolute file paths in build folders -> so cleanup with subprojects should now work

## 0.5.3 (August 29, 2012)

Features:

* Added new parameter for xcodebuild -> arch
* Corrected error in dependency version in consumer/build.gradle example

Documentation:

* Created documentation for new parameter -> arch
