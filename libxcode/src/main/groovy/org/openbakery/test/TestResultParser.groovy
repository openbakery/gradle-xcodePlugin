package org.openbakery.test

import org.openbakery.xcode.Destination
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public class TestResultParser extends AbstractTestResultParser {

	private static Logger logger = LoggerFactory.getLogger(TestResultParser.class)

	File testSummariesDirectory
	List<Destination> destinations
	String xcresulttoolPath

	XCResultTool xcResultTool

	public TestResultParser(File testSummariesDirectory, String xcresulttoolPath, List<Destination> destinations) {
		super()
		this.testSummariesDirectory = testSummariesDirectory
		this.destinations = destinations
		this.xcresulttoolPath = xcresulttoolPath
		this.xcResultTool = new XCResultTool(xcresulttoolPath)
	}

	public void parseAndStore(File outputDirectory) {
		parse(outputDirectory)
		store(outputDirectory)
	}

	// Output directory is necessary for exporting test attachments
	private void parse(File outputDirectory) {
		if (!testSummariesDirectory.exists()) {
			return
		}

		if (isTestSummaryPlistAvailable()) {
			parseTestSummaryFile()
		} else {
			parseXcResult(outputDirectory)
		}
	}

	private void parseXcResult(File outputDirectory) {
		logger.debug("Using new xcresult scheme version")
		def files = testSummariesDirectory.listFiles({ d, f -> f.endsWith(".xcresult") } as FilenameFilter)
		files.each {
			if (xcresulttoolPath == null) {
				logger.debug("No xcresulttool found.")
				return
			}
			def xcResult = xcResultTool.getObject(it)
			def identifier = xcResult.actions._values.first().runDestination.targetDeviceRecord.identifier._value
			logger.debug("identifier {}", identifier)
			Destination destination = findDestinationForIdentifier(destinations, identifier)
			if (destination) {
				def resultList = processTestSummary(xcResult, it)
				testResults.put(destination, resultList)
				exportAttachments(it, outputDirectory)
			} else {
				logger.debug("destination not found for identifier {}", identifier)
			}

		}
	}

	def exportAttachments(File file, File outputDirectory) {
		for(results in testResults.values()) {
			for(TestClass testClass in results) {
				for(TestResult testResult in testClass.results) {
					for(TestResultAttachment attachment in testResult.attachments) {
						xcResultTool.exportAttachment(file, outputDirectory, attachment.id, attachment.name)
					}
				}
			}
		}
	}

	private void parseTestSummaryFile() {
		logger.info("parsing test summary files is deprecated, in order to get a test-results.xml transition to new .xcresult format or use older plugin version")
	}

	private Boolean isTestSummaryPlistAvailable() {
		def testSummaries = new FileNameFinder()
			.getFileNames(testSummariesDirectory.path, '*TestSummaries.plist *.xcresult/*_Test/action_TestSummaries.plist')

		return !testSummaries.isEmpty()
	}

	ArrayList<TestClass> processTestSummary(Map<String, Object> xcResult, File file) {
		logger.debug("processTestSummary")

		String testsRef = xcResult.actions._values.actionResult.testsRef.id._value[0]
		if (testsRef == null) {
			logger.debug("No tests reference found, skipping test result parsing")
			return []
		}

		def results = xcResultTool.getObject(file, testsRef)
		def testStatus = new ArrayList<TestClass>()

		if (results.summaries != null && results.summaries._values == null) {
			logger.debug("No test result summaries present")
			return []
		}

		results.summaries._values.each { summaryItem ->
			if (summaryItem.testableSummaries != null && summaryItem.testableSummaries._values != null) {
				summaryItem.testableSummaries._values.each { testableSummaryItem ->
					if (testableSummaryItem.tests != null && testableSummaryItem.tests._values != null) {
						testableSummaryItem.tests._values.each { testItem ->
							addTestResultWithStatusToTestClass(testItem, testStatus, null, file)
						}
					} else {
						logger.debug("tests are empty")
					}
				}
			} else {
				logger.debug.debug("testable summaries are empty")
			}
		}

		return testStatus
	}

	private void addTestResultWithStatusToTestClass(Map<String, Object> testData,
																									ArrayList<TestClass> testsStatus,
																									TestClass testClass,
																									File file) {
		TestResultAdditionalInfo additionalInfo = getAdditionalTestInfo(testData, file)
		if (testData.testStatus) {
			TestResult.State state
			def value = testData.testStatus._value
			if (value == "Success") {
				state = TestResult.State.Passed
			} else if (value == "Skipped") {
				state = TestResult.State.Skipped
			} else {
				state = TestResult.State.Failed
			}
			def testResult = new TestResult(
				method: testData.identifier._value,
				output: additionalInfo?.name,
				state: state,
				attachments: additionalInfo?.testResultAttachments,
				duration: (Float.parseFloat(testData.duration._value))
			)
			testClass.results.add(testResult)
		}

		if (testData.subtests && testData.subtests._values && testData._type._name == "ActionTestSummaryGroup") {
			testClass = new TestClass(name: testData.name._value)
			testData.subtests._values.each {
				addTestResultWithStatusToTestClass(it, testsStatus, testClass, file)
			}
			if (!testClass.results.empty) {
				testsStatus << testClass
			}
		}
	}

	TestResultAdditionalInfo getAdditionalTestInfo(Map<String, Object> testData, File file) {
		if(!testData.summaryRef) { return null }

		String id = testData.summaryRef.id._value
		def xcResultObject = xcResultTool.getObject(file, id)
		def title = ""
		def testAttachments = []

		if(!xcResultObject.activitySummaries) { return null }

		xcResultObject.activitySummaries._values.each { activity ->
			if((activity.activityType._value == "com.apple.dt.xctest.activity-type.userCreated"
			|| activity.activityType._value == "com.apple.dt.xctest.activity-type.attachmentContainer") && activity.attachments) {
				activity.attachments._values.each { attachment ->
					def attachmentTitle = attachment.filename._value
					def attachmentId = attachment.payloadRef.id._value
					logger.info("###### absolute path {}", file.absolutePath)
					TestResultAttachment tra = new TestResultAttachment(name: attachmentTitle, id: attachmentId)
					logger.info(tra.toString())
					testAttachments << tra
				}
			} else if(activity.activityType._value == "com.apple.dt.xctest.activity-type.testAssertionFailure") {
				title = activity.title._value
			}
		}

		return new TestResultAdditionalInfo(name:  title, testResultAttachments: testAttachments)
	}
}
