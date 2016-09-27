# Cocoapods Support

Here a description how the cocoapods support works.

If your project contains a Podfile the plugins automatically adds cocoapods support to your project. This means that the `pod install` is executed during the build process. This is only done when the `Pods/Manifest.lock` does not match the `Podfile.lock`.
Therfor the `pod install` is only performed when the pods are not up to date.

If you want to force `pod install` than you can run the gradle with `--refresh-dependencies`.

Cocoapods is bootstraped if it is not installed.
This means that cocoapods is installed for the current user and can be found you gem userdir (see `ruby -rubygems -e "puts Gem.user_dir"`)

