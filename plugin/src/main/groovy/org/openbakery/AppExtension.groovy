package org.openbakery

/**
 * Created by chaitanyar on 2/27/15.
 */

import org.openbakery.InfoPlistExtension;

class AppExtension {
    String infoPlistPath
    String entitlementsPath
    def infoPlistConfig
    def entitlementsConfig
    String name

    public AppExtension(name,infoPlistConfig,entitlementsConfig) {
        super()
        this.infoPlistConfig = infoPlistConfig
        this.entitlementsConfig = entitlementsConfig
        this.name = name
    }
}
