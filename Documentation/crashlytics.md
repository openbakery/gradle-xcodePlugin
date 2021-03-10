# Crashlytics


The `crashlytics` parameters are used to prepare a IPA for ad hoc deployment using [Crashlytics](https://crashlytics.com/)

e.g.

```
deploygate {
	apiKey='12345678901234567890123456789012'
}
```



# Parameters

### apiKey

The Crashlytics API Key (https://www.crashlytics.com/settings/organizations)

default value: _empty_

### buildSecret

The Crashlytics Build Secret (https://www.crashlytics.com/settings/organizations)

default value: _empty_

### submitPath

Path to the crashlytics submit command (relative to the project dir)

  default value: "Crashlytics.framework/submit"

### emails

List of email addresses of users that get added as testers to the new build

default value: _empty_

### groupAliases

List of group aliases from the web dashboard

default value: _empty_

### notesPath

Path to a .txt file containing notes for the beta (relative to the project dir)

default value: _empty_

### notifications

Boolean value that enables/disables email notification to testers

default value: `true`
