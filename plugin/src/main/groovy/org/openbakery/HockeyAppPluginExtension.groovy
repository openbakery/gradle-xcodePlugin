package org.openbakery


class HockeyAppPluginExtension {
	def String outputDirectory = "build/hockeyapp"
	def String apiToken = null
	def String notes = "This build was uploaded using the gradle xcodePlugin"
    def String status = 2
    def String notify= 1
    def String notesType=1
}


/**

 #!/bin/sh

 #  hockey_upload.sh
 #
 #
 #  Created by Markus Kopf on 2/16/13.
 #

 # go to binary dir
 #cd "${CONFIGURATION_BUILD_DIR}" || exit 1

 XCODE_TARGET="Teleboy"
 if [ $1 ]; then
 XCODE_TARGET=$1
 fi

 HOCKEY_TOKEN="bam"
 if [ $2 ]; then
 HOCKEY_TOKEN=$2
 fi

 PROVISIONING_PROFILE_PATH="provisioningProfile/Teleboy_Hockey_Adhoc.mobileprovision"
 if [ $3 ]; then
 PROVISIONING_PROFILE_PATH=$3
 fi

 buildPath="build/sym/Debug-iphoneos"
 ipaFile="$buildPath/${XCODE_TARGET}.ipa"
 dsymFile="$buildPath/${XCODE_TARGET}.app.dSYM"
 dsymZipFile="$buildPath/${XCODE_TARGET}.app.dSYM.zip"
 releaseNotes=`git log -n1 --oneline`


 # zip dSYM file
 zip -q -r -9 "$dsymZipFile" "$dsymFile" || exit 1

 # submit ipa and dSYM to hockey
 CURL_APP_RESPONSE=`curl --show-error -F "status=2" -F "notify=1" -F "notes_type=1" -F "notes=$releaseNotes" -F "ipa=@$ipaFile" -F "dsym=@$dsymZipFile" -H "X-HockeyAppToken: $HOCKEY_TOKEN" https://rink.hockeyapp.net/api/2/apps || exit 1`
 if [ `echo $CURL_APP_RESPONSE | grep -o error` ]; then
 echo $CURL_APP_RESPONSE
 exit 1;
 fi

 # submit mobileprovision to hockey
 CURL_PROV_RESPONSE=`curl --show-error -F "mobileprovision=@$PROVISIONING_PROFILE_PATH"  -H "X-HockeyAppToken: $HOCKEY_TOKEN" https://rink.hockeyapp.net/api/2/apps/$HOCKEY_TOKEN/provisioning_profiles || exit 1`
 if [ `echo $CURL_PROV_RESPONSE | grep -o error` ]; then
 echo $CURL_PROV_RESPONSE
 exit 1;
 fi

 # remove old versions from hockey
 CURL_REMOVE_RESPONSE=`curl --show-error -F "keep=10" -H "X-HockeyAppToken: $HOCKEY_TOKEN" https://rink.hockeyapp.net/api/2/apps/$HOCKEY_TOKEN/app_versions/delete`
 if [ `echo $CURL_REMOVE_RESPONSE | grep -o error` ]; then
 echo $CURL_REMOVE_RESPONSE
 exit 1;
 fi

 exit 0

 fi

 echo "INFO => HOCKEY_APP_TOKEN missing. HOCKEY_APP_TOKEN will be set in Hudson-Environment"

 exit 0

 **/