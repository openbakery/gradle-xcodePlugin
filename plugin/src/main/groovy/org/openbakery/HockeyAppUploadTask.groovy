package org.openbakery

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.FileBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class HockeyAppUploadTask extends DefaultTask {

	HockeyAppUploadTask() {
		super()
		dependsOn("hockeyapp-prepare")
		this.description = "Uploades the app (.ipa, .dsym) to HockeyApp"
	}

	def getFile(String extension) {
		def buildOutputDirectory = new File(project.hockeyapp.outputDirectory)
		def pattern = Pattern.compile(".*" + extension)
		def fileList = buildOutputDirectory.list(
						[accept: {d, f -> f ==~ pattern }] as FilenameFilter
		).toList()
		if (fileList == null || fileList.size() == 0) {
			throw new IllegalStateException("No *" + extension + " file found in directory " + buildOutputDirectory.absolutePath)
		}
		return new File(buildOutputDirectory, fileList[0])
	}

	@TaskAction
	def upload() throws IOException {

		if (project.hockeyapp.apiToken == null) {
			throw new IllegalArgumentException("Cannot upload to HockeyApp because API Token is missing")
		}

		def ipaFile = getFile("ipa");
		def dSYMFile = getFile("dSYM.zip");

		println ipaFile.absolutePath
		println dSYMFile.absolutePath

		println "api_token " + project.hockeyapp.apiToken
		println "notes " + project.hockeyapp.notes
		println "file " + ipaFile
		println "dsym " + dSYMFile
        println "status " + project.hockeyapp.status
        println "notify "  + project.hockeyapp.notify
        println "notes_type " + project.hockeyapp.notesType


        uploadIPAandDSYM(ipaFile, dSYMFile)
        uploadProvisioningProfile()

	}

    def void uploadIPAandDSYM(File ipaFile, File dSYMFile) {
        HttpClient httpClient = new DefaultHttpClient()

        // for testing only
        //HttpHost proxy = new HttpHost("localhost", 8888);
        //httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

        HttpPost httpPost = new HttpPost("https://rink.hockeyapp.net/api/2/apps")

        MultipartEntity entity = new MultipartEntity();

        entity.addPart("status", new StringBody(project.hockeyapp.status))
        entity.addPart("notify", new StringBody(project.hockeyapp.notify))
        entity.addPart("notes_type", new StringBody(project.hockeyapp.notesType))

        entity.addPart("notes", new StringBody(project.hockeyapp.notes))
        entity.addPart("ipa", new FileBody(ipaFile))
        entity.addPart("dsym", new FileBody(dSYMFile))

        httpPost.addHeader("X-HockeyAppToken", project.hockeyapp.apiToken)

        httpPost.setEntity(entity);

        println "request " + httpPost.getRequestLine().toString()

        HttpResponse response = httpClient.execute(httpPost)

        if (response.getStatusLine().getStatusCode() != 201) {
            throw new IllegalStateException("file upload failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
        }
    }

    def void uploadProvisioningProfile() {
        HttpClient httpClient = new DefaultHttpClient()

        // for testing only
        //HttpHost proxy = new HttpHost("localhost", 8888);
        //httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);

        HttpPost httpPost = new HttpPost("https://rink.hockeyapp.net/api/2/apps/" + project.hockeyapp.apiToken + "/provisioning_profiles")

        MultipartEntity entity = new MultipartEntity();

        entity.addPart("mobileprovision", new FileBody(new File(project.provisioning.mobileprovisionFile)))

        httpPost.addHeader("X-HockeyAppToken", project.hockeyapp.apiToken)

        httpPost.setEntity(entity);

        println "request " + httpPost.getRequestLine().toString()

        HttpResponse response = httpClient.execute(httpPost)

        if (response.getStatusLine().getStatusCode() != 201) {
            throw new IllegalStateException("file upload failed: " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
        }
    }

}
