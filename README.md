gradle-xcodePlugin
==================

gradle xcodePlugin to build Mac and iOS projects

**Current stable version is 0.9.14**

0.9.14 supports Xcode 5, Xcode 6 and Xcode 6.1


__Introduction:__ http://openbakery.org/gradle.php

__Example:__ [build.gradle](example/Example/build.gradle)

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

  
* _signing_ - signing configuration that should be used when building for the device

  * identity - the signing identity e.g. 'iPhone Developer: Rene Piringer (AASDF1234)'
	
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


* _destination_ * - destination configuration, that is used for the unit test execution

	default value: empty (When empty alls available simulators are used for the unit tests)
	
	Note when building using the iPhoneSimulator: The destinations are verified if they already exists. If not then the destination is ignored for the unit test. If no valid destination is specified, then all available simulator destinations are used for the unit tests.
	Therefor you can specify destinations to limit on which simulator destinations the unit tests are performed.
	You also can specify a regular expression for the value and all available simulator devices are included that match these value: e.g. name = 'iPhone.*'
	
	* _platform_ - Platform, e.g. 'iOS' or 'OS X'
	
	default value: empty
	
	* _name_ - Name of the destination device. 
	
	default value: empty
	
	* _arch_ - Architecture of the destination target
	
	The arch can a single value e.g. 'i386' or a list of values e.g. [ 'armv7', 'armv7s' ]
	
	default value: empty


* _additionalParameters_ - additional parameters for the xcodebuild. Here you can for example pass preprocessor definitions: 
  `additionalParameters = "GCC_PREPROCESSOR_DEFINITIONS='TIME=" + System.currentTimeMillis() + "'"`.

	Also an array of parameters is excepted e.g. _["-xcconfig", "/path/to/the/xconfig" ]_

  default value: empty
    
* _bundleNameSuffix_ - String that should be appended to the generated app bundle. 
  e.g. the default app bundle name is 'Demo.App'. When you set `bundleNameSuffix=-1.0.0` than the generated bundle is 'Demo-1.0.0.app'
  
  default value: empty
  
* _arch_ - Use the architecture specified by architecture when building each target.
  e.g. 'i386', 'armv6', 'armv7' 
	
	Also an array of parameters is possible e.g. ['armv7', 'arm64']
  
  default value: empty

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
	
* _version_ - set the xcode version that should be used if multiple versions of Xcode are installed. Here the build number must be specifed e.g. '5B1008' for Xcode 5.1.1.
	If this value is empty then the default version is used that is selected using 'xcode-select'

  defaul value: empty

* _enviroment_ - pass enviroment variable to xcodebuild    


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

* _commands_ - adds commands to modify the info plist that are excuted with the plistbuddy tool (see also 'man PlistBuddy' )

	default value: empty
	
	Example: This commands modify the URL scheme.
	
```
	commands = [
		'Delete CFBundleURLTypes:0:CFBundleURLSchemes',
		'Add CFBundleURLTypes:0:CFBundleURLSchemes array',
		'Add CFBundleURLTypes:0:CFBundleURLSchemes:0 string newscheme'
	]
```

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
	
* _distributionLists_ - Comma separated distribution list names which will receive access to the build

	default value: empty

* _notifyDistributionList_ - notify permitted teammates to install the build

  default value: false

* _replaceBuild_ - replace binary for an existing build if one is found with the same name/bundle version

  default value: false
	

Note: see also https://testflightapp.com/api/doc/	
	

HockeyApp Parameters:
----------------------

* _apiToken_ - The HockeyApp API Token (see https://rink.hockeyapp.net/manage/auth_tokens )

  default value: empty

* _appID_ - The HockeyApp App ID (see https://rink.hockeyapp.net/manage/dashboard and select the app)

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


DeployGate Parameters:
----------------------

* _apiToken_ - The DeployGate API Token (https://deploygate.com/settings)

  default value: empty

* _userName_ - The DeployGate User Name (https://deploygate.com/settings)

  default value: empty
  
* _message_ - Release notes for the build

  default value: "This build was uploaded using the gradle xcodePlugin"

* _outputDirectory_ - Output directory where the ipa

  default value: "build/deploygate"
	
	

Note: see also https://deploygate.com/docs/api	
	


sparkle Parameters:
--------------------

* _appName_ - You should specify this parameter when your appname does not match your target name. Default appname in XCode is $(TARGET_NAME). Do not specify .app here because it will be added automatically. If a different file ending than .app will be needed in future we will add a new parameter.

  default value: empty
  
* _outputDirectory_ - The directory to output a ZIP file and release notes of the app.

  default value: empty

coverage Parameters:
--------------------

Note: For coverage the the "Generate Test Coverage Files" in the project for the executed target must be set to Yes

* _exclude_ - Exclude data files that match this regular expression. e.g. '.*h$|.*UnitTests.*m$' excludes all headers and all *.m files from the UnitTests directory

	default value: empty


* _outputFormat_ -  Format of the generated output. Possible values are, xml or html. If not specified text file is generated

	default value: empty


* _outputDirectory_ - Output directory for the results

  default value: "build/coverage"


