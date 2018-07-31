package com.sap.cloud.sdk.s4hana.pipeline

import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader

class ConfigUtil implements Serializable {
    static final long serialVersionUID = 1L

    @NonCPS
    static Map getContainersMap(script, stageName) {
        Map containers = [:]
        Map stepConfig = ConfigurationLoader.stepConfiguration(script, 'kubernetes')
        Map containerConfig = stepConfig?.k8sMapping ?: [:]
        if (!containerConfig.containsKey(stageName)) {
            return containers
        }
        containers = containerConfig[stageName]
        return containers
    }
}
