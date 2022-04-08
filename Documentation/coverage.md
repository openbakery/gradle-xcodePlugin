# Coverage 

The `coverage` configuration parameters when running the `coverage` task to determine the unit test code coverage. When the app is build using the newer llvm compiler then the report is generated using the [CoverageReport](https://github.com/openbakery/CoverageReport) tool. For old projects  [GCovr](http://gcovr.com) is used

Example configuration settings: 

```
coverage {
	outputFormat = 'html'
	exclude = '.*h$|.*UnitTests.*m$'
}
```

Note: The `xcodetest` or the `xcodetestrun` task must be executed prior to the `coverage` task, so that the necessary informations are available in the build output folder.

# Parameters

### outputFormat

The coverage output format: can be `text`, `xml`  or `html`

default value: _empty_ - Creates text summary

### exclude

Files to exclude for the coverage report as regular expresssion: e.g. '.*h$|.*UnitTests.*m$'
