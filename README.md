gradle-xcodePlugin
==================

gradle xcodePlugin to build iOS projects

**Current version is 0.6.3 which is not compatible with 0.5 because some parameters have changed.** See [Changelog](https://github.com/openbakery/gradle-xcodePlugin/blob/master/CHANGELOG.md) for details


xcodebuild Parameters:
----------------------

* _scheme_ - the xcode build scheme that should be used. If the scheme is set the _configuration_, _sdk_ and
  _target_ is ignored

  default value: empty

* _configuration_ - the build configuration name that should be used (e.g. 'Debug', 'Release')

  default value: 'Debug'

* _sdk_ - the SDK that should be used (e.g. 'iphonesimulator', 'iphoneos')

  default value: 'iphonesimulator'

* _target_ - the xcode build target that should be used

  default value: 'unknown'
  
* _signIdentity_ - sign identity that should be used when building for the device

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
    

provisioning Parameters:
----------------------

* _mobileprovisionUri_ - URI where the mobileprovision profile is located that should be used when signing the app

  default value: empty
    
    
keychain Parameters:
----------------------

* _certificateUri_ - Uri to the certificate that should be used to sign the app

	default value: empty

* _certificatePassword_ - Password for the certificate file

	default value: empty

* _keychainPassword_ - Password for the chain that is created

	default value: "This_is_the_default_keychain_password"

* _keychainName_ - name of the keychain that is created

	default value: 'gradle.keychain'


infoplist Parameters:
----------------------

* _bundleIdentifier_ - If set it override the bundle identifier in the Info.plist (CFBundleIdentifier)

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

	default value: empty
  

* _versionDirectoryName_ - subdirectory that should be used for the app.

	default value: "0"


* _outputDirectory_ - directory where to store the files that are generated for the hockeykit distribution

	default value "build/hockeykit";


    