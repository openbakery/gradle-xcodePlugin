# Infoplist

The `infoplist` is a nested section within the `xcodebuild` parameters. e.g:

```
xcodebuild {
	target = "Example"
	scheme = "Example"
	
	infoplist {
		bundleIdentifier = "com.example.Example"
	}
}
```

# Parameters

### bundleIdentifier

If set it override the bundle identifier in the Info.plist (CFBundleIdentifier)

default value: empty

### bundleIdentifierSuffix

If set it adds a suffix to the bundle identifier in the Info.plist (CFBundleIdentifier)

default value: empty

### bundleName

If set it override the bundle name in the Info.plist (CFBundleName)

default value: empty

### bundleDisplayName

If set it override the bundle display name in the Info.plist (CFBundleDisplayName)

default value: empty

### bundleDisplayNameSuffix

If set it adds a suffix to the bundle display name in the Info.plist (CFBundleDisplayName)

default value: empty

### version

sets the CFBundleVersion to the given version
  Note: _version=2.3.4_ and _versionSuffix=-Suffix_ and _versionPrefix=Prefix-_ results in 'Prefix-2.3.4-Suffix'

default value: empty

### versionSuffix

adds the value to the CFBundleVersion e.g. 'CFBundleVersion=1.0.0' and 'versionSuffix=-Test' results in '1.0.0-Test'

default value: empty

### versionPrefix

adds the value in front of the CFBundleVersion e.g. 'CFBundleVersion=1.0.0' and 'versionPrefix=Test-' results in 'Test-1.0.0'

default value: empty

### shortVersionString

sets the CFBundleShortVersionString to the given shortVersionString

default value: empty

### shortVersionStringSuffix

adds the value to the CFBundleVersion e.g. 'CFBundleShortVersionString=1.0.0' and 'versionSuffix=-Test' results in '1.0.0-Test'

default value: empty

### shortVersionStringPrefix

adds the value in front of the CFBundleVersion e.g. 'CFBundleShortVersionString=1.0.0' and 'versionPrefix=Test-' results in 'Test-1.0.0'

default value: empty

### commands

adds commands to modify the info plist that are excuted with the plistbuddy tool (see also 'man PlistBuddy' )

default value: empty

	Example: This commands modify the URL scheme.

```
commands = [
	'Delete CFBundleURLTypes:0:CFBundleURLSchemes',
	'Add CFBundleURLTypes:0:CFBundleURLSchemes array',
	'Add CFBundleURLTypes:0:CFBundleURLSchemes:0 string newscheme'
]
```
