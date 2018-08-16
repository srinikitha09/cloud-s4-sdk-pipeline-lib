package com.sap.cloud.sdk.s4hana.pipeline

import com.sap.piper.ConfigurationLoader
import com.sap.piper.ContainerMap

class ConfigUtil implements Serializable {
    static final long serialVersionUID = 1L

    static Map getContainersMap(script, stageName) {
        Map containers = [:]
        Map containerConfig = ContainerMap.instance.getMap()
        if (containerConfig.containsKey(stageName)) {
            containers = containerConfig[stageName]
        }
        return containers
    }
}
