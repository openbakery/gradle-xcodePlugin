gradle-xcodePlugin
==================

The gradle xcode plugin (gxp) makes it easier to build Xcode projects by specifying the build settings in a single configuration file. The goal is to keep the build file as simple as possible, but also enable a great flexibility for the build.

The gxp uses the Apple command line tools (like xcodebuild) to perform the build.

Here a brief overview of the features:

* Build iOS, watchOS, tvOS and Mac projects
* Override sign settings for builds
* Perform unit tests
* Support for multiple Xcodes (on one machine)
* [Cocoapods](https://cocoapods.org/) support
* [Carthage](https://github.com/Carthage/Carthage) support
* [Appledoc](http://gentlebytes.com/appledoc/) support
* Code coverage support (using [gcovr](http://gcovr.com) or using [CoverageReport](https://github.com/openbakery/CoverageReport) )
* OCLint



## Requirements

 Xcode 12 or greater
* [Gradle](http://gradle.org) 2.14 or greater
* Java 1.6 or greater


### Current stable version is 0.24.5


## Parameters Documentation

* [xcodebuild](Documentation/xcodebuild.md)
* [signing](Documentation/signing.md)
* [infoplist](Documentation/infolist.md)
* [appstore](Documentation/appstore.md)
* [deploygate](Documentation/deploygate.md)
* [crashlytics](Documentation/crashlytics.md)
* [coverage](Documentation/coverage.md)
* [oclint](Documentation/oclint.md)



## Usage

Create a build.gradle file and place it in the same directory where xcodeproj file lies.

Here the minimal content you need in your build.gradle file:

```
buildscript {
  repositories {
    maven {
      url('https://openbakery.org/repository/')
    }
  mavenCentral()
  }

  dependencies {
    classpath "org.openbakery:xcode-plugin:0.24.+"
  }
}

apply plugin: "org.openbakery.xcode-plugin"
```

### Current develop version

When using the [openbakery.org](https://openbakery.org) repository you can also get the latest develop version by including `develop` into the version pattern. e.g.: 
```
classpath "org.openbakery:xcode-plugin:0.24.5.develop.+"
```

The develop version contains all the changes from the develop branch, where all the fixes and feature are implemented. The development version is deployed automatically when all the projects unit tests are  successful, and also the if the example projects build. 


## Example

You find example projects in [example/](example/) with a working build.gradle file.
After you have fetched the example go to the `example/iOS/Example` directory and you build the project different targets:

* Build with `gradle xcodebuild`
* Run the unit tests with `gradle test`
* Perform a device build and upload it to App Center with `gradle integration`. Here you need to specify your sign settings first (see [Signing](Documentation/Parameters.md#sign-settings) ). Open the build.gradle file an follow the instructions.
* Perform an appstore build with `gradle appstore`. (Also the sign settings are needed).

# Need help?

If you need help you can create an issue here on GitHub, you can [send me a mail](mailto:rene@openbakery.org) or [ask on twitter](https://twitter.com/rpirringer).

# Collaborate

I'm always happy to receive pull requests with new features and if you send a pull request please consider the following things:

* Use the _develop_ branch for pull requests, because all the new stuff is implemented in the _develop_ branch and also pull requests are always merge into _develop_
* Use Tabs instead of spaces.
* Make sure that all unit tests are working before you send the pull request. Just run 'gradle test' 
* I urge you to write unit tests. 
* For unit test please use the [spock framework](http://spockframework.org) for mocking. I want to remove the old gmock framework and port the old tests to use spock.
* Pull requests that contains new features or fixes but without any unit tests will NOT be merged.


# License

This project is licensed under the terms of the Apache license. See the [LICENSE](LICENSE) file.
