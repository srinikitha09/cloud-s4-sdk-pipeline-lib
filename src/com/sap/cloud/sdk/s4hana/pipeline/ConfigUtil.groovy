package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader

class ConfigUtil implements Serializable {
    static final long serialVersionUID = 1L

    @NonCPS
    static Map getContainersMap(script, stageName) {
        Map containers = [:]
        def generalConfiguration = ConfigurationLoader.generalConfiguration(script)
        Map containerConfig = generalConfiguration?.k8sMapping ?: [:]
        if (!containerConfig.containsKey(stageName)) {
            return containers
        }
        containers = containerConfig[stageName]
        return containers
    }
}
