# Deploygate 

The `deploygate` parameters are used to prepare a IPA for ad hoc deployment using [DeployGate](https://deploygate.com/)

e.g.

```
deploygate {
	apiToken='12345678901234567890123456789012'
	apiToken='abcdefghijklmnopqrstuvwxyzabcdef'
}
```


## DeployGate Parameters

### apiToken

The DeployGate API Token (https://deploygate.com/settings)

default value: _empty_

### userName

The DeployGate User Name (https://deploygate.com/settings)

default value: _empty_

### message

Release notes for the build

default value: `'This build was uploaded using the gradle xcodePlugin'`

### outputDirectory

Output directory where the ipa

default value: `'build/deploygate'`



Note: see also https://deploygate.com/docs/api

