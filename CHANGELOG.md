## 0.8.5 (Aug 2, 2013)

Bugfixes:
- Merged Pull Request: https://github.com/openbakery/gradle-xcodePlugin/pull/29

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