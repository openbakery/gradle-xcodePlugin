
# Parameter Description

## xcodebuild Parameters

* _scheme_ - the xcode build scheme that should be used. If the scheme is set the _configuration_ and _target_ is ignored

  default value: empty

* _workspace_ - the workspace file that should be used for the build.

	default value: '*.xcworkspace' that was found in the project directory or empty if not found

* _configuration_ - the build configuration name that should be used (e.g. 'Debug', 'Release')

  default value: 'Debug'

* _type_ - the type of the build. Possible values are iOS, OSX (tvOS will be added in the future). This parameter replaces the sdk parameter. The given values is not case sensitive therefor 'ios', 'iOs', 'IOS' are all correct values.

  default value: iOS
	
* _simulator_ - should perform a simulator build. Possible values are 'true' and 'false'

  default value: true

* _target_ - the xcode build target that should be used

  default value: empty

* _ipaFileName_ - a custom name for the generated ipa file

  default value: the applicaiton name is used if no ipaFileName is given

### Sign Settings

* _signing_ - signing configuration that should be used when building for the device

* identity - the signing identity e.g. 'iPhone Developer: Rene Piringer (AASDF1234)'. This parameter is **optional** and only needed if you have more then one identity in the keychain. This is only the case if the _keychain_ parameter is set, and the keychain is not created during the build process.

	default value: empty

* _mobileProvisionURI_ - URI where the mobile provision profile is located that should be used when signing the app. You can also specify multiple provisining profiles as array when you have an app exenstion e.g. `[ 'file:///first.mobileprovision', 'file:///second.mobileprovision']`

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

* _timeout_ - A custom timeout in seconds before the keychain automatically locks.

	default value: 3600 seconds (1 hour)

* _customCodesign_ - A custom code signing method represented by a closure.

    default value: empty (default /usr/bin/codesign usage)

### Unit Test Settings

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

### Other Settings

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

* _derivedDataPath_ - the derived data path that should be used

  default value: 'build/derivedData'

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

* _version_ - set the xcode version that should be used if multiple versions of Xcode are installed. Here you can set the version with '6.1' that selects 6.1 or 6.1.x if present. Or you can use the build number e.g. '5B1008' for Xcode 5.1.1.
	If this value is empty then the default version is used that is selected using 'xcode-select'

  defaul value: empty

* _environment_ - pass environment variable to xcodebuild


## Info plist Parameters:

* _bundleIdentifier_ - If set it override the bundle identifier in the Info.plist (CFBundleIdentifier)

	default value: empty

* _bundleIdentifierSuffix_ - If set it adds a suffix to the bundle identifier in the Info.plist (CFBundleIdentifier)

	default value: empty

* _bundleName_ - If set it override the bundle name in the Info.plist (CFBundleName)

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

## Appstore Parameters:

* _username_ - Your Apple ID

	default value: empty

* _passoword_ - The password for your Apple ID

	default value: empty


## HockeyKit Parameters:


* _displayName_ - Title that should be used that is shown on the hockeykit site for the app.
  If the value is not set then the bundle identifier is used

	default value: the CFBundleDisplayName from the Info.plist file is used


* _versionDirectoryName_ - subdirectory that should be used for the app.

	default value: "0"


* _outputDirectory_ - directory where to store the files that are generated for the hockeykit distribution

	default value "build/hockeykit";


* _notes_ - Release notes as HTML or Markdown for the build that is stored in a releasenotes.html.

	default value: empty



## HockeyApp Parameters

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

* _teams_ - Optional, corresponds to `teams` (http://support.hockeyapp.net/kb/api/api-apps)

  default value: empty  
  example value: `[1231, 123]`

* _users_ - Optional, corresponds to `users` (http://support.hockeyapp.net/kb/api/api-apps)

  default value: empty  
  example value: `[1231, 123]`

* _tags_ - Optional, corresponds to `tags` (http://support.hockeyapp.net/kb/api/api-apps)

  default value: empty  
  example value: `['earlytesters', 'mytag2']`

* _mandatory_ - Optional, set 1 to make version as mandatory (http://support.hockeyapp.net/kb/api/api-apps)

  default value: 0

* _releaseType_ - Optional, set the release type as in `release_type` (http://support.hockeyapp.net/kb/api/api-apps)

  default value: 1

* _privatePage_ - Optional, set true for a private download page as in `private` (http://support.hockeyapp.net/kb/api/api-apps)

  default value: false

* _commitSha_ - Optional, corresponds to `commit_sha` (http://support.hockeyapp.net/kb/api/api-apps)

  default value: empty

* _buildServerUrl_ - Optional, corresponds to `build_server_url`

  default value: empty

* _repositoryUrl_ - Optional, corresponds to `repository_url`

  default value: empty


## DeployGate Parameters

* _apiToken_ - The DeployGate API Token (https://deploygate.com/settings)

  default value: empty

* _userName_ - The DeployGate User Name (https://deploygate.com/settings)

  default value: empty

* _message_ - Release notes for the build

  default value: "This build was uploaded using the gradle xcodePlugin"

* _outputDirectory_ - Output directory where the ipa

  default value: "build/deploygate"



Note: see also https://deploygate.com/docs/api


## Crashlytics Parameters

* _apiKey_ - The Crashlytics API Key (https://www.crashlytics.com/settings/organizations)

  default value: empty

* _buildSecret_ - The Crashlytics Build Secret (https://www.crashlytics.com/settings/organizations)

  default value: empty

* _submitPath_ - Path to the crashlytics submit command (relative to the project dir)

  default value: "Crashlytics.framework/submit"

## Coverage

[GCovr](http://gcovr.com) is used for coverage. Make your you have enabled the code coverage support in xcode (See https://developer.apple.com/library/ios/qa/qa1514/_index.html)

* _outputFormat_ - The coverage output format: can be text, xml or html

  default value: empty - Creates text summary
	
* _exclude_ - Files to exclude for the coverage report as regular expresssion: e.g. '.*h$|.*UnitTests.*m$'


	
## OCLint Parameters


* _reportType_ - The report type that should be generated. Must be one of text, html, xml, json and pmd

  default value: html
	
* _rules_ - the line rules as array (see also: http://docs.oclint.org/en/dev/rules/index.html) e.g

```
oclint {
	rules = [
		"LINT_LONG_LINE=300",
		"LINT_LONG_VARIABLE_NAME=64"]
}
```

* _disableRules_ - the rules that should be disabled as array (see also: http://docs.oclint.org/en/dev/rules/index.html) e.g

```
oclint {
	disableRules = [
		"UnusedMethodParameter",
		"UselessParentheses",
	"IvarAssignmentOutsideAccessorsOrInit"
	]
}
```


* _excludes_ - array of elements that should be excluded. e.g.

```
oclint {
	excludes = [ "Pods" ]
}
```

* _maxPriority1_, _maxPriority2_, _maxPriority3_ - maximum number of violations: see http://docs.oclint.org/en/dev/manual/oclint.html#exit-status-options

	default values: maxPriority1=0, maxPriority2=10, maxPriority3=20


 
