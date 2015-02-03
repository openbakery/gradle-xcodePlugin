## 0.10.1 (Februar 3, 2015)

Bugfixes:
- Fixed archive for Swift apps: #132

## 0.10.0 (Februar 2, 2015)

Changes
- Renamed all tasks with dashes in the name and removed the dash, e.g hockeykit-clean to hockeykitClean
- New package task to creates IPAs
  - supports Swift
	- supports App Extensions: Issue #96
- Codesign task was removed (is replaced by package task)
- archive task now creates a xcarchive: Issue #1
- distribution tasks (e.g. hockeykit) does not have a dependences to the build tasks anymore, if you want this define this in you build.gradle file
- distribution tasks uses the xcarchive as input now
- Support for the new Apple Testflight: Issue #98
	- Added new 'appstoreValidate' that validates the created ipa with the apple appstore:
	- Added new 'appstoreUpload' target that submits the ipa to apple
- Add custom keychain timeout: Issue #105 (Thanks to icecrystal23)
- Environment variables passing support: Issue #88 (Thanks to liliomk)

Bugfixes:
- Fixed parsing of Swift unit test output: Issue #124 (Thanks to tanob)
- Provision-cleanup should always remove bad links: Issue #123 (Thanks to icecrystal23)


## 0.9.15 (November 25, 2014)

Changes
- Pods can no be reinstalled with --refresh-dependencies: Issue #90
- If the provisioning profile has expired, the build fails early now, not after the build when signing: Issue #83
- Added that the value for workspace parameter is set automatically: Issue #86

Bugfixes
- Fixed that the provisioning profile is set: Issue #97
- Fixed test output parsing: Issue #106 (Thanks to icecrystal23)

## 0.9.14 (October 31, 2014)

Bugfixes
- Fixed codesign with Xcode 6.1: Issue #94
- Fixed unit test output, so that the number of test are shown with Xcode 6 #92
- Fixed that the additionalParameters are also used in test builds

## 0.9.13 (Sepember 23, 2014)

Changes
- Added the the xcode version can be selected: Issue #6
Bugfixes
- infoplist.version value is ignored

## 0.9.12 (August 27, 2014)

Changes
- Build in Cocoapods support. If a Podfile is found a pod install is perform automatically. If cocoapods is not install, it is installed for the current user. Issue #75
- Add support for modifing the info plist using plistbuddy commands. See Issue #74 
- For unit testing destination are not optionals. I no destination is specified then the unit tests are executed on every available simulator.
  This works both for Xcode5 and Xcode6 (here the new simctl is used)
- The plugin should be more memory efficent now, because the output of the xcodebuild command that can be several megabytes is not hold in the mememory anymore, it is stored in an output file.	

## 0.9.11 (July 22, 2014)

Changes
- Added appledoc target to generate documentation using the appledoc tool form gentlebytes: https://github.com/tomaz/appledoc
- Added code coverage target using gcovr: http://gcovr.com

Bugfixes
- Fixed error when no test is available: issue #72

## 0.9.10 (July 16, 2014)

Changes
- Added JUnit XML output for test results: Issue #56
- Compatibility for Xcode 6
  - The arch is not added to a simulator build, it is only added when specified using the xcodebuild.arch parameter 

Bugfixes
- When unit test cases are compiled after a test was already executed, then no output was shown

## 0.9.9 (June 27, 2014)

Changes
- Codesign does not fail anymore when multiple keychain exists with the same key. (The PackageApplication script is copied and patched, and the proper keychain is passed as parameter to the codesign command ) 

## 0.9.8 (May 7, 2014)

Bugfixes
- Possible fix for Issue 55: Only add keychain options to the xcodebuild command when doing a device build

## 0.9.7 (May 5, 2014)

Changes
- Add Sparkle support (http://sparkle.andymatuschak.org). Thanks to gugmaster.
- Add DeployGate support (https://deploygate.com). Thanks to henteko.

Bugfixes
- When using asset catalogs for app icons then the hockey-image task did not find the app icons

## 0.9.6 (Mar 20, 2014)

Changes
- Remove _unitTestTarget_ parameter that is not needed with xcode 5

Bugfixes
- Fixed bundleDisplayName mapping: Issue #52

## 0.9.5 (Feb 11, 2014)

Bugfixes
- Fixed HockeyApp upload: Issue #48


## 0.9.4 (Feb 10, 2014)

Changes
- Reverted issue #36
- Added the the whole xcodebuild output is stored in an file: issue #46

Bugfixes
- Fixed warning that TaskContainer.add() is deprecated


## 0.9.3 (Jan 20, 2014)

Bugfixes
- Fixed issue #36


## 0.9.2 (Nov 26, 2013)

Changes
- Change that the project is also compiled using 'build' instead of 'xcodebuild'
- Added support for an architecture list: Issue #33

Bugfixes
- Fixed that the clean task is defined by the plugin, instead the clean asks are appended: Issue #38
- Build output for unit test did not show any failures if an exception occured

## 0.9.1 (Nov 12, 2013)

Changes
- Implemented much nice build output when compiling and also for the unit tests

## 0.9.0  (Sep 27, 2013)

Changes
- XCode5 support. This version only works with Xcode5
- Updated Keychain creation and cleanup for OS X Mavericks
- Updated unit test support

## 0.8.6 (Aug 19, 2013)

Changes:
- Merged pull request: https://github.com/openbakery/gradle-xcodePlugin/pull/31

## 0.8.5 (Aug 2, 2013)

Bugfixes:
- Merged pull request: https://github.com/openbakery/gradle-xcodePlugin/pull/29

## 0.8.4 (June 21, 2013)

Bugfixes:
- fixed hockeykit-image task that the image is created from the app icon as intended.

## 0.8.3 (June 18, 2013)

Bugfixes:
- fixed exception in hockeyapp-prepare task: issue #28

## 0.8.2 (June 18, 2013)

Bugfixes:
- fixed that on a simulator build the keychain is not created and provisioning profile is not installed

## 0.8.1 (June 6, 2013)

Bugfixes:
- fixed executing unit test. Where skipped because was TEST_HOST was set.
- fixed provisioning cleanup and create task that failed if the Provisioning Profiles directory does not exist 

## 0.8.0 (June 3, 2013)

Features:
  - new _xcodebuild.signing_ parameters, replaces provisioning.mobileprovisionUri, keychain.certificateUri,	keychain.certificatePassword,	keychain.keychainPassword, keychain.keychain
  - added new parameter _hockeykit.notes_
  - added new parameter _keychain.keychain_
  - added hockeyapp target that was created by wfrank2509
  - reworked keychain and provisioning file handling so that multiple parallel builds should work

Changes:
  - removed _xcodebuild.buildRoot_: The gradle buildDir variable is used instead.
  - removed _keychain.keychainName_

Bugfixes:
	
## 0.7.2 (Februar 8, 2013)

Bugfixes:
 - hockeykit image does not fail anymore when no icon image was found

## 0.7.1

Bugfixes:
  - fixed reading the UUID from the mobile provisioning file

## 0.7.0

Changes:
  - added workspace supported

## 0.6.6

Bugfixes:
  - fixed the archive task: The *.app and *.app.dSYM directory were not included recursivly

## 0.6.5

Changes:
  - default _hockekit.displayName_ is now CFBundleDisplayName instead of CFBundleIdentifier

## 0.6.4 (September 5, 2012)

Changes:
  - Added _xcodebuild.unitTestTarget_
  - Added bundleIdentifierSuffix #12

## 0.6.3 (September 4, 2012)

Changes:
  - Added inital TestFlight support with _testflight_ target that uploads the build to TestFlight. (see Readme for the testflight parameters)
  - Added support for Scheme with the new configuration parameter _xcodebuild.scheme_. Issue #1
  - Added that after codesign the keychain and provisioning profile gets cleaned up. Issue #9

## 0.6.2 (August 31, 2012)

Bugfixes:
  - fixed that the hockeykit manifest xml file was not valid xml

## 0.6.1 (August 31, 2012)

Bugfixes:
 - fixed cleanup: Issue #8
 - fixed that if the short version entry is missing in the Info.plist the plist modify task does not fail


## 0.6.0 (August 30, 2012)

Changes:
  - _xcodebuild.archiveVersion_ remame to _xcodebuild.bundleNameSuffix_
	- _infolist.versionExtension_ remove an added the following keys instead: _version_, _versionSuffix_, _versionPrefix_
	- Short Version String can now be also set using _shortVersionString_, _shortVersionStringSuffix_ and _shortVersionStringPrefix_
	- Renamed _hockeykit.appName_ to _hockeykit.displayName_
	- Renamed _hockeykit.version_ to _hockeykit.versionDirectoryName_
	- _xcodebuild.additionalParameters_ now also accepts arrays

Bugfixes: 
  - changed to absolute file paths in build folders -> so cleanup with subprojects should now work

## 0.5.3 (August 29, 2012)

Features:

  - Added new parameter for xcodebuild -> arch
  - Corrected error in dependency version in consumer/build.gradle example

Documentation:

  - Created documentation for new parameter -> arch 