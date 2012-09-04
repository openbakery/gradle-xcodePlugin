## 0.6.3 (TBA)

Changes:
  - Added TestFlight support with testflight-prepare target that puts the ipa and zipped dSYM to build/testflight
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

  - Created documentation for new paramter -> arch 