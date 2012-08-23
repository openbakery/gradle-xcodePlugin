package org.openbakery

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.xml.MarkupBuilder
import org.apache.commons.io.FilenameUtils

/**
 * Created with IntelliJ IDEA.
 * User: rene
 * Date: 23.08.12
 * Time: 09:51
 * To change this template use File | Settings | File Templates.
 */
class HockeyKitManifestTask extends AbstractXcodeTask {

    static final String XML_DEF_LINE = '<?xml version="1.0" encoding="UTF-8"?>';
    static final String DOCTYPE_LINE = '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">'


    HockeyKitManifestTask() {
        super();
        this.description = "Creates the manifest that is needed to deploy on a HockeyKit Server"
    }

    @TaskAction
    def createManifest() {

        def appName = getAppBundleName()
        def basename = FilenameUtils.getBaseName(appName);

        def infoPlist = getAppBundleInfoPlist()

        def bundleIdentifier = getValueFromPlist(infoPlist, "CFBundleIdentifier")
        def bundleVersion = getValueFromPlist(infoPlist, "CFBundleVersion")

        File outputDirectory = new File(project.hockeykit.outputDirectory + "/" + bundleIdentifier + "/" + project.hockeykit.version);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        def manifestFilename = outputDirectory.absolutePath + "/" +  basename + ".plist";


        def title = project.hockeykit.appName
        if (title == null) {
            title = bundleIdentifier
        }

        def subtitle = getValueFromPlist(infoPlist, "CFBundleShortVersionString")
        if (subtitle == null) {
            subtitle = title
        }



        def writer = new BufferedWriter(new FileWriter(manifestFilename))
        writer.write(XML_DEF_LINE); writer.newLine();
        writer.write(DOCTYPE_LINE); writer.newLine();
        def xml = new MarkupBuilder(writer)
        xml.plist(version: "1.0") {
            dict() {
                key('items')
                array() {
                    dict() {
                        key('assets')
                        array() {
                            dict() {
                                key('kind')
                                string('software-package')
                                key('url')
                                string('__URL__')
                            }
                        }
                        key('metadata')
                        dict() {
                            key('bundle-identifier')
                            string(bundleIdentifier)
                            key('bundle-version')
                            string(bundleVersion)
                            key('kind')
                            string('software')
                            key('title')
                            string(title)
                            key('subtitle')
                            string(subtitle)
                        }
                    }
                }
            }
        }
        writer.close()
    }

}
