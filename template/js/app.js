
function getSummary(data) {
	var success = 0;
	var failure = 0;
	$.each(data, function(index, test) {
		$.each(test.results, function(index, results) {
			$.each(results.result, function(index, result) {
				if (result.success) {
					success++;
				} else {
					failure++;
				}
			});
		});
	});
	return {success: success, failure: failure};
}


function getTestResults(data) {

	var allTests = [];
	var testNames = [];
	var destination = 0;
	
	$.each(data, function(index, test) {
		
		
		$.each(test.results, function(index, results) {
			$.each(results.result, function(index, result) {
				var name = "[" + results.name + " " + result.method + "]";
				allTests.push({ name: name, result: result, destination: destination, id: results.name + "-" + result.method + "-" + destination});
				testNames.push(name);
			});
		});
		destination++;
		
	});

	testNames =	_.union(testNames) 
	
	var result = {}
	
	$.each(testNames, function(index, testName) {
		
		var resultValues = []
		
		for (i=0; i<destination; i++) {
			var item = _.find(allTests, 
				function(test){ 
					return (test.destination == i && test.name == testName);
				}
			);
			if (item == undefined) {
				item = { name: testName, result: null}
			}
			resultValues.push(item);
		}
		result[testName] = resultValues;
	});
	return result;
}

function getTestsResults(data) {
	tests = getTests(data);
	
	$.each(tests, function(index, test) {
		getTestResultForTestCase(test, data)
	});

	
}

function getURLParameter(name) {
    return decodeURI(
        (RegExp(name + '=' + '(.+?)(&|$)').exec(location.search)||[,null])[1]
    );
}


console.log(getURLParameter("result"));

var source   = $("#index").html();
var template = Handlebars.compile(source);
var context = {title: "Test Results"}
$('.container').append(template(context));


Handlebars.registerHelper('if', function(conditional, options) {
	if(conditional) {
		return options.fn(this);
	} else {
		return options.inverse(this);
	}
});

Handlebars.registerHelper('showResult', function(item, options) {
	if (item.result != null) {
		if (item.result.success) {
			return '<a data-toggle="modal" href="#'+ item.id + '" style="color: green"><span class="glyphicon glyphicon-ok glyphicon-black"/></a>'
		}
		return '<a data-toggle="modal" href="#'+ item.id + '" style="color:red; font-size: 150%"><span class="glyphicon glyphicon-remove-circle"/></a>'
	}
	return "";
});




$.get(getURLParameter("result"), function(data) {
	var tests = jQuery.parseJSON(data);

	var templateSummary = Handlebars.compile($("#summary").html());
	$('.container').append(templateSummary(getSummary(tests)));

//	getTestsResults(tests);

	var templateTest = Handlebars.compile($("#testResults").html());
	var destinations = _.pluck(tests, 'destination' );
	console.log(destinations);
	var data = {testClasses: getTestResults(tests), destinations: destinations}
	$('.container').append(templateTest(data));

	
}, 'text');



//file:///Volumes/Space/Users/rene/workspace/openbakery/xcodePlugin/test-results/unittest-result.json
       ///Volumes/Space/Users/rene/workspace/openbakery/xcodePlugin/test-results/unittest-result.json. Origin null is not allowed by Access-Control-Allow-Origin.

