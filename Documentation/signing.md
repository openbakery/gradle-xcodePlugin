# Codesigning

Signing configuration that should be used when building for the device.

The `signing` is a nested section within the `xcodebuild` parameters. e.g:

```
xcodebuild {
	target = "Example"
	scheme = "Example"
	
	signing {
		certificateURI = 'file:///Users/me/codesign/development.p12'
		certificatePassword = 'my_secret_password'
		mobileProvisionURI = [
			file:///Users/me/codesign/com.example.Example.mobileprovision',
			file:///Users/me/codesign/com.example.Example.Widget.mobileprovision'
		]
	}
}
```

# Parameters

### mobileProvisionURI

URI where the mobile provision profile is located that should be used when signing the app. You can also specify multiple provisining profiles as array when you have an app exenstion e.g. 

`[ 'file:///first.mobileprovision', 'file:///second.mobileprovision']`

default value: _empty_

### certificateURI

Uri to the certificate that should be used to sign the app

default value: _empty_

### certificatePassword

Password for the certificate file

default value: _empty_

### entitlements

Here the entitlements settings can be specifed that should be merged with the settings from the provisioning profile. The value can be specified as map. The values specified here overrides the values from the provisioning profile.
Here is an example: 
```
entitlements = [
	"com.apple.security.application-groups": [
		"group.com.example.MyApp"
	]
]
```

since 0.15.1:
If you want to delete a value from the entitlements than you can set it to null and it will be deleted: e.g.
```
entitlements = [
 'com.apple.developer.icloud-container-environment' : null
]
```

If the parameter is empty, then the build process looks for an entitlements that is embedded in the archive (*.xcent file) and uses this for merging the the entitlements from the provisioning profile.


default value: _empty_

(since 0.14.6)

### entitlementsFile

With this parameter a entitlements file can be specified that is used for codesigning. If you specify a file here only this settings are used and nothing is merged, that means that the `entitlements` parameter from above is ignored.
If empty then the entitlements that is embedded in the provisioning file is extracted and used.

default value: _empty_

### identity

The signing identity e.g. 'iPhone Developer: Rene Piringer (AASDF1234)' or the SHA. With macOS Sierra only the SHA works. This parameter is **optional** and only needed if you have more then one identity in the keychain. This is only the case if the _keychain_ parameter is set, and the keychain is not created during the build process.

default value: _empty_

### keychainPassword

Password for the chain that is created

default value: `'This_is_the_default_keychain_password'`

### keychain

_It is highly recommended that you do not use this parameters!_ Gradle creates a keychain automatically, therefor you do not need to specify the keychain. You only need the parameter if you want to use an existing keychain. For this you have to make sure that `/usr/bin/codesign` is allowed to access the certificates in this keychain. If you use this parameter then the parameters `certificateURI` and `certificatePassword` are ignored and nothing is imported!

### timeout

A custom timeout in seconds before the keychain automatically locks.

default value: _empty_ - This means the default timeout is used that is 5 minutes
