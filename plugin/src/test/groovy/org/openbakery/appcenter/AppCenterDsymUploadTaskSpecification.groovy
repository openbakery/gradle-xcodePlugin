package org.openbakery.appcenter

import groovy.json.JsonBuilder
import org.gradle.api.Project
import org.apache.commons.io.FileUtils
import org.gradle.testfixtures.ProjectBuilder
import org.openbakery.XcodeBuildArchiveTask
import org.openbakery.CommandRunner
import org.openbakery.XcodePlugin
import org.openbakery.appcenter.models.CommitRequest
import org.openbakery.appcenter.models.CommitResponse
import org.openbakery.appcenter.models.InitDebugSymbolRequest
import org.openbakery.appcenter.models.InitDebugSymbolResponse
import org.openbakery.http.HttpUtil
import spock.lang.Specification

import java.util.zip.ZipFile

class AppCenterDsymUploadTaskSpecification extends Specification {

	Project project
	AppCenterDsymUploadTask appCenterDsymUploadTask

	CommandRunner commandRunner = Mock(CommandRunner)
	HttpUtil httpUtil = Mock(HttpUtil)

	File infoPlist
	File appDsym
	File frameworkDsym

	InitDebugSymbolRequest initRequest
	InitDebugSymbolResponse initResponse
	CommitRequest commitRequest
	CommitResponse commitResponse


	def setup() {
		File projectDir = new File(System.getProperty("java.io.tmpdir"), "gradle-xcodebuild")
		project = ProjectBuilder.builder().withProjectDir(projectDir).build()
		project.buildDir = new File(projectDir, 'build').absoluteFile
		project.apply plugin: org.openbakery.XcodePlugin
		project.xcodebuild.productType = 'app'
		project.xcodebuild.productName = 'Test'

		appCenterDsymUploadTask = project.getTasks().getByPath(XcodePlugin.APPCENTER_DSYM_UPLOAD_TASK_NAME)
		appCenterDsymUploadTask.commandRunner = commandRunner
		appCenterDsymUploadTask.httpUtil = httpUtil

		File archiveDirectory = new File(project.getBuildDir(), XcodeBuildArchiveTask.ARCHIVE_FOLDER + "/Test.xcarchive")
		archiveDirectory.mkdirs()

		infoPlist = new File(archiveDirectory, "Products/Applications/Test.app/Info.plist");
		infoPlist.parentFile.mkdirs();

		appDsym = new File(archiveDirectory, "dSYMs/Test.app.dSYM")
		FileUtils.writeStringToFile(appDsym, "dummy")

		frameworkDsym = new File(archiveDirectory, "dSYMs/framework.dSYM")
		FileUtils.writeStringToFile(frameworkDsym, "dummy")

		initRequest = new InitDebugSymbolRequest()
		initResponse = new InitDebugSymbolResponse()
		initResponse.symbol_upload_id = "1"
		initResponse.upload_url = "https://symbolupload.mock"

		commitRequest = new CommitRequest()
		commitResponse = new CommitResponse()
		commitResponse.release_id = "42"
		commitResponse.release_url = "https://release.mock"
	}

	def cleanup() {
		FileUtils.deleteDirectory(project.projectDir)
	}

	def jsonString(Object object) {
		new JsonBuilder(object).toPrettyString()
	}

	def "upload"() {
		given:
		project.appcenter.apiToken = "123"

		when:
		appCenterDsymUploadTask.upload()

		File expectedDSYMZip = new File(project.buildDir, "appcenter/Test.app.dSYM.zip")
		ZipFile dsymZipFile = new ZipFile(expectedDSYMZip)

		then:
		expectedDSYMZip.exists()
		dsymZipFile.entries().toList().size() == 2
		1 * httpUtil.sendJson(_, _, _, _, jsonString(initRequest)) >> jsonString(initResponse)
		1 * httpUtil.sendJson(_, { it.endsWith(initResponse.symbol_upload_id) }, _, _, jsonString(commitRequest)) >> jsonString(commitResponse)
		1 * httpUtil.sendForm(_, initResponse.upload_url, _, _)
	}

	def "bundleSuffix"() {
		given:
		project.xcodebuild.bundleNameSuffix = '-SUFFIX'
		project.appcenter.apiToken = "123"
		httpUtil.sendJson(_, _, _, _, jsonString(initRequest)) >> jsonString(initResponse)
		httpUtil.sendJson(_, _, _, _, jsonString(commitRequest)) >> jsonString(commitResponse)

		when:
		appCenterDsymUploadTask.upload()

		File expectedDSYMZip = new File(project.buildDir, "appcenter/Test-SUFFIX.app.dSYM.zip")
		ZipFile dsymZipFile = new ZipFile(expectedDSYMZip)

		then:
		expectedDSYMZip.exists()
		dsymZipFile.entries().toList().size() == 2
	}

	def "dsym upload task is executed when dysm folder exists"() {
		expect:
		appCenterDsymUploadTask.getOnlyIf().isSatisfiedBy(appCenterDsymUploadTask)
	}

	def "dsym upload task is skipped when dsym folder is missing"() {
		when:
		appDsym.delete()
		frameworkDsym.delete()
		appDsym.parentFile.delete()

		then:
		!appCenterDsymUploadTask.getOnlyIf().isSatisfiedBy(appCenterDsymUploadTask)
	}

	def "The task should not be executed if the dsym folder is missing"() {
		given:
		project.appcenter.apiToken = "123"

		when:
		appDsym.delete()
		frameworkDsym.delete()
		appDsym.parentFile.delete()

		appCenterDsymUploadTask.upload()

		then:
		0 * httpUtil.sendJson(_, _, _, _, jsonString(initRequest)) >> { }
		0 * httpUtil.sendJson(_, { it.endsWith(initResponse.symbol_upload_id) }, _, _, jsonString(commitRequest)) >> { }
		0 * httpUtil.sendForm(_, _, _, _) >> { }
	}
}
