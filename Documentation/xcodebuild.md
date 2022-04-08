# Xcodebuild

The `xcodebuild` parameters defines the basic build parameters. e.g

```
xcodebuild {
	scheme = "Example"
	target = "Example"
}
```

# Parameters

### arch

Use the architecture specified by architecture when building each target. e.g. `i386`, `armv6`, `armv7`

Also an array of parameters is possible e.g. `['armv7', 'arm64']``

default value: _empty_

### additionalParameters

additional parameters for the xcodebuild. Here you can for example pass preprocessor definitions:

`additionalParameters = "GCC_PREPROCESSOR_DEFINITIONS='TIME=" + System.currentTimeMillis() + "'"`.

Also an array of parameters is excepted e.g. 

`["-xcconfig", "/path/to/the/xconfig" ]`

default value: _empty_

### bitcode

If set to true the resulting binary will contain bitcode.

default value: false

Since version 0.15.0

### bundleNameSuffix

String that should be appended to the generated app bundle.
e.g. the default app bundle name is 'Demo.App'. When you set `bundleNameSuffix=-1.0.0` than the generated bundle is 'Demo-1.0.0.app'

default value: _empty_

### buildRoot

build root directory for the build output

default value: `'build'`

### configuration

the build configuration name that should be used (e.g. 'Debug', 'Release')

  default value: `'Debug'`

### derivedDataPath

the derived data path that should be used

default value: `'build/derivedData'`

### dstRoot

the distribution root directory

default value: `'build/dst'`

### destination

Destination configuration, that is used for the unit test execution

default value: _empty_ - When empty alls available simulators are used for the unit tests

Note when building using the iPhone simulator: The destinations are verified if they already exists. If not then the destination is ignored for the unit test. If no valid destination is specified, then all available simulator destinations are used for the unit tests.
Therefor you can specify destinations to limit on which simulator destinations the unit tests are performed.
You also can specify a regular expression for the value and all available simulator devices are included that match these value: e.g. name = 'iPhone.*'

#### Simple Syntax
	
```	destination = "iPhone 4s"```

or 

```destination = ['iPhone 4s', 'iPad Air']```
	
Here the simulator device of the most recent runtime is used for iPhone 4s and iPad Air

#### Full syntax:
	
```
destination {
	platform = 'iOS Simulator'
	name = 'iPad Air'
	os = '10.1'
}
```
	
This closure can be defined multiple times for multipe devices

#### id
device identifier

default value: _empty_

#### platform
Platform, e.g. 'iOS', 'OS X' or 'iOS Simulator'

default value: _empty_

#### name
Name of the destination device.

default value: _empty_

#### arch
Architecture of the destination target

The arch can a single value e.g. 'i386' or a list of values e.g. [ 'armv7', 'armv7s' ]

default value: _empty_
	
### environment

pass environment variable to xcodebuild

### infoPlist

override the Info.plist file that is configured in the xcode project file

  default value: empty

### ipaFileName

a custom name for the generated ipa file

  default value: the applicaiton name is used if no ipaFileName is given
	
### objRoot

the object root directory

default value: 'build/obj'

### projectFile

path to the `xcodeproj` file. You only need to set this if you have multiple xcodeproj in the same directory.

default value: _empty_ - This means that the first `xcodeproj` is automatically picked that is present in the project directory

### scheme

the xcode build scheme that should be used. If the scheme is set the _configuration_ is ignored. You need the to specify the _target_ because this is not yet read from the scheme.

default value: _empty_

### simulator

should perform a simulator build. Possible values are 'true' and 'false'

  default value: `true`

### sharedPrecompsDir

  default value: 'build/shared'

### symRoot

the sym directory. Here is where the app and ipa is generated

  default value: 'build/sym'

### type

the type of the build. Possible values are `iOS`, `macOS`, `tvOS` and `watchOS`. This parameter replaces the sdk parameter. The given values is not case sensitive therefor `ios`, `iOs`, `IOS` are all correct values. Also `OSX' as value is supported for backward compatibility that is mapped to `macOS`. 

default value: `'iOS'`	

Note: the `macOS` value was introduced with Version 0.14.6, before it was `OSX`

### target

the xcode build target that should be used

  default value: _empty_

### version

set the xcode version that should be used if multiple versions of Xcode are installed. Here you can set the version with '6.1' that selects 6.1 or 6.1.x if present. Or you can use the build number e.g. '5B1008' for Xcode 5.1.1.
	If this value is empty then the default version is used that is selected using 'xcode-select'

  defaul value: empty

### workspace

the workspace file that should be used for the build.

default value: `*.xcworkspace` that was found in the project directory or empty if not found


