gradle-xcodePlugin
==================

gradle xcodePlugin to build iOS projects

**Current stable version is 0.8.5** 

**The lastest version is not compatible with 0.7.x, the signing configuration has changed. See [Changelog.md](https://github.com/openbakery/gradle-xcodePlugin/blob/master/CHANGELOG.md)**


__Introduction:__ http://openbakery.org/gradle.php

__Example:__ [build.gradle](consumer/build.gradle)

xcodebuild Parameters:
----------------------

* _scheme_ - the xcode build scheme that should be used. If the scheme is set the _configuration_ and _target_ is ignored

  default value: empty

* _workspace_ - the workspace file that should be used for the build.

	default value: empty
	
* _configuration_ - the build configuration name that should be used (e.g. 'Debug', 'Release')

  default value: 'Debug'

* _sdk_ - the SDK that should be used (e.g. 'iphonesimulator', 'iphoneos')

  default value: 'iphonesimulator'

* _target_ - the xcode build target that should be used

  default value: empty

* _unitTestTarget_ - Unit Test target that should be executed when running xcodebuild. Here first the _target_ is build and afterwards the _unitTestTarget_ is build and executed. This target only works when the iphonesimulator _sdk_ is used.

  default value: empty

  
* _signing_ - signing configuration that should be used when building for the device

  default value: empty

	* _mobileProvisionURI_ - URI where the mobile provision profile is located that should be used when signing the app

		default value: empty

	* _certificateURI_ - Uri to the certificate that should be used to sign the app

		default value: empty

	* _certificatePassword_ - Password for the certificate file

		default value: empty

	* _keychainPassword_ - Password for the chain that is created

		default value: "This_is_the_default_keychain_password"

	* _keychain_ - Parameter to specify an existing keychain. If this parameters is set _no_ keychain is created and also the certificate is _not_ imported.

		default value: empty

		Note: Make sure that _/usr/bin/codesign_ is allowed to access the certificate in the keychain that is needed to sign.



* _additionalParameters_ - additional parameters for the xcodebuild. Here you can for example pass preprocessor definitions: 
  `additionalParameters = "GCC_PREPROCESSOR_DEFINITIONS='TIME=" + System.currentTimeMillis() + "'"`.

	Also an array of parameters is excepted e.g. _["-xcconfig", "/path/to/the/xconfig" ]_

  default value: empty
    
* _bundleNameSuffix_ - String that should be appended to the generated app bundle. 
  e.g. the default app bundle name is 'Demo.App'. When you set `bundleNameSuffix=-1.0.0` than the generated bundle is 'Demo-1.0.0.app'
  
  default value: empty
  
* _arch_ - Use the architecture specified by architecture when building each target.
  e.g. 'i386', 'armv6', 'armv7'
  
  default value: empty

	_NOTE: when scheme and workspace is set and also the sdk value is 'iphonesimulator' that the arch is set per default to the value 'i386', otherwise a simulator build fails_


* _buildRoot_ - build root directory for the build output

  default value: build

* _dstRoot_ - the distribution root directory

  default value: 'build/dst'
  
* _objRoot_ - the object root directory

  default value: 'build/obj'
  
* _symRoot_ - the sym directory. Here is where the app and ipa is generated

  default value: 'build/sym'

* _sharedPrecompsDir_

  default value: 'build/shared'

* _infoPlist_ - override the Info.plist file that is configured in the xcode project file

  default value: empty
    


infoplist Parameters:
----------------------

* _bundleIdentifier_ - If set it override the bundle identifier in the Info.plist (CFBundleIdentifier)

	default value: empty
	
* _bundleIdentifierSuffix_ - If set it adds a suffix to the bundle identifier in the Info.plist (CFBundleIdentifier)

	default value: empty


* _bundleDisplayName_ - If set it override the bundle display name in the Info.plist (CFBundleDisplayName)

	default value: empty
	
* _bundleDisplayNameSuffix_ - If set it adds a suffix to the bundle display name in the Info.plist (CFBundleDisplayName)

	default value: empty

* _version_ - sets the CFBundleVersion to the given version
  Note: _version=2.3.4_ and _versionSuffix=-Suffix_ and _versionPrefix=Prefix-_ results in 'Prefix-2.3.4-Suffix'

	default value: empty

* _versionSuffix_ - adds the value to the CFBundleVersion e.g. 'CFBundleVersion=1.0.0' and 'versionSuffix=-Test' results in '1.0.0-Test'

	default value: empty

* _versionPrefix_ - adds the value in front of the CFBundleVersion e.g. 'CFBundleVersion=1.0.0' and 'versionPrefix=Test-' results in 'Test-1.0.0'

	default value: empty

* _shortVersionString_ - sets the CFBundleShortVersionString to the given shortVersionString

	default value: empty

* _shortVersionStringSuffix_ - adds the value to the CFBundleVersion e.g. 'CFBundleShortVersionString=1.0.0' and 'versionSuffix=-Test' results in '1.0.0-Test'

	default value: empty

* _shortVersionStringPrefix_ - adds the value in front of the CFBundleVersion e.g. 'CFBundleShortVersionString=1.0.0' and 'versionPrefix=Test-' results in 'Test-1.0.0'

	default value: empty


hockeykit Parameters:
----------------------


* _displayName_ - Title that should be used that is shown on the hockeykit site for the app. 
  If the value is not set then the bundle identifier is used

	default value: the CFBundleDisplayName from the Info.plist file is used
  

* _versionDirectoryName_ - subdirectory that should be used for the app.

	default value: "0"


* _outputDirectory_ - directory where to store the files that are generated for the hockeykit distribution

	default value "build/hockeykit";
	

* _notes_ - Release notes as HTML or Markdown for the build that is stored in a releasenotes.html.

	default value: empty
	
TestFlight Parameters:
----------------------

* _apiToken_ - The TestFlight API Token (https://testflightapp.com/account/#api-token)

	default value: empty
	
* _teamToken_ - The TestFlight Team Token (https://testflightapp.com/dashboard/team/edit/?next=/api/doc/)

  default value: empty
  
* _notes_ - Release notes for the build

  default value: "This build was uploaded using the gradle xcodePlugin"

* _outputDirectory_ - Output directory where the ipa an dSYM.zip is created

  default value: "build/testflight"

HockeyApp Parameters:
----------------------

* _apiToken_ - The HockeyApp API Token (http://support.hockeyapp.net/kb/api)

  default value: empty

* _outputDirectory_ - Optional, output directory where the ipa an dSYM.zip is created

  default value: "build/hockeyapp"

* _notes_ - Release notes for the build

  default value: "This build was uploaded using the gradle xcodePlugin"
    
* _status_ - Optional, download status (can only be set with full-access tokens):

  default value: 2

* _notify_ - Optional, notify testers

  default value: 1

* _notesType_ - Optional, type of release notes

  default value: 1
