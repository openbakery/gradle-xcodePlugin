gradle-xcodePlugin
==================

gradle xcodePlugin to build iOS projects


xcodebuild Parameters:
----------------------

* _configuration_ - the build configuration name that should be used (e.g. 'Debug', 'Release')

  default value: 'Debug'

* _sdk_ - the SDK that should be used (e.g. 'iphonesimulator', 'iphoneos')

  default value: 'iphonesimulator'

* _target_ - the xcode build target that should be used

  default value: 'unknown'
  
* _signIdentity_ - sign identity that should be used when building for the device

  default value: 'empty'


* _additionalParameters_ - additional parameters for the xcodebuild. Here you can for example pass preprocessor definitions: 
  `additionalParameters = "GCC_PREPROCESSOR_DEFINITIONS='TIME=" + System.currentTimeMillis() + "'"`

  default value: 'empty'
    
* _archiveVersion_ - String that should be appended to the generated app bundle. 
  e.g. the default app bundle name is 'Demo.App'. When you set `archiveVersion=-1.0.0` than the generated bundle is 'Demo-1.0.0.app'
  
  default value: 'empty'
  
* _arch_ - Use the architecture specified by architecture when building each target.
  e.g. 'i386', 'armv6', 'armv7'
  
  default value: 'empty'


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

  default value: 'empty'
    

provisioning Parameters:
----------------------

* _mobileprovisionUri_ - URI where the mobileprovision profile is located that should be used when signing the app

  default value: 'empty'
    
    
keychain Parameters:
----------------------

* _certificateUri_ - Uri to the certificate that should be used to sign the app

default value: 'empty'

* _certificatePassword_ - Password for the certificate file

default value: 'empty'

* _keychainPassword_ - Password for the chain that is created

default value: "This_is_the_default_keychain_password"

* _keychainName_ - name of the keychain that is created

default value: 'gradle.keychain'


infoplist Parameters:
----------------------

* _bundleIdentifier_ - If set it override the bundle identifier in the Info.plist (CFBundleIdentifier)

default value: 'empty'

* _versionExtension_ - If set this value is appended to the version in the bundle identifier (CFBundleVersion)

default value: 'empty'


hockeykit Parameters:
----------------------

* _appName_ - Title that should be used that is shown on the hockeykit site for the app. 
  If the value is not set then the bundle identifier is used

default value: 'empty'
  
* _version_ - subdirectory that should be used for the app.

default value: "0"

* _outputDirectory_ - directory where to store the files that are generated for the hockeykit distribution

default value "build/hockeykit";


    