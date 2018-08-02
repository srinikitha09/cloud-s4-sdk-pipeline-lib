package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader

class ConfigUtil implements Serializable {
    static final long serialVersionUID = 1L

    @NonCPS
    static Map getContainersMap(script, stageName) {
        Map containers = [:]
        Map containerConfig = ConfigurationLoader.generalConfiguration(script)?.jenkinsKubernetes?.imageToContainerMap ?: [:]
        if (containerConfig.containsKey(stageName)) {
            containers = containerConfig[stageName]
        }
        return containers
    }
}
