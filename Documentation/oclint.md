# OCLint

Parameters for the `oclint` analytics task

Example: 
```
oclint {
	maxPriority2 = 999
	maxPriority3 = 999			
	excludes = [ 'Pods' ]
	rules = [
		"SHORT_VARIABLE_NAME=1",
		"LONG_LINE=250",
		"LONG_VARIABLE_NAME=64",
		"LONG_METHOD=150",
	]
	disableRules = [
		"IvarAssignmentOutsideAccessorsOrInit",
	]
}
```

# Parameters


### reportType

The report type that should be generated. Must be one of text, html, xml, json and pmd

default value: `'html`'

### rules

the line rules as array (see also: http://docs.oclint.org/en/dev/rules/index.html) e.g

```
oclint {
	rules = [
		"LINT_LONG_LINE=300",
		"LINT_LONG_VARIABLE_NAME=64"]
}
```

### disableRules

the rules that should be disabled as array (see also: http://docs.oclint.org/en/dev/rules/index.html) e.g

```
oclint {
	disableRules = [
		"UnusedMethodParameter",
		"UselessParentheses",
	"IvarAssignmentOutsideAccessorsOrInit"
	]
}
```


### excludes

array of elements that should be excluded. e.g.

```
oclint {
	excludes = [ "Pods" ]
}
```

### maxPriority1

maximum number of violations: see http://docs.oclint.org/en/dev/manual/oclint.html#exit-status-options

default value: `0`

### maxPriority2

maximum number of violations: see http://docs.oclint.org/en/dev/manual/oclint.html#exit-status-options

default value: `10`

### maxPriority3

maximum number of violations: see http://docs.oclint.org/en/dev/manual/oclint.html#exit-status-options

default value: `20`
