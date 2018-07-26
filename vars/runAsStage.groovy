import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.SysEnv

import java.util.UUID

def call(Map parameters = [:], body) {
    ConfigurationHelper configurationHelper = new ConfigurationHelper(parameters)
    def stageName = configurationHelper.getMandatoryProperty('stageName')
    def script = configurationHelper.getMandatoryProperty('script')
    Map defaultGeneralConfiguration = ConfigurationLoader.defaultGeneralConfiguration(script)
    Map projectGeneralConfiguration = ConfigurationLoader.generalConfiguration(script)

    Map generalConfiguration = ConfigurationMerger.merge(
        projectGeneralConfiguration,
        projectGeneralConfiguration.keySet(),
        defaultGeneralConfiguration
    )

    Map stageDefaultConfiguration = ConfigurationLoader.defaultStageConfiguration(script, stageName)
    Map stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)


    Set parameterKeys = ['node']
    Map mergedStageConfiguration = ConfigurationMerger.merge(
        parameters,
        parameterKeys,
        stageConfiguration,
        stageConfiguration.keySet(),
        stageDefaultConfiguration
    )
    mergedStageConfiguration.uniqueId = UUID.randomUUID().toString()
    def containers = getContainerList(script, mergedStageConfiguration, stageName)
    String nodeLabel = generalConfiguration.defaultNode


    if (mergedStageConfiguration.node) {
        nodeLabel = mergedStageConfiguration.node
    }
    handleStepErrors(stepName: stageName, stepParameters: [:]) {
        def options = [name      : 'dynamic-agent-' + mergedStageConfiguration.uniqueId,
                       label     : nodeLabel ?: mergedStageConfiguration.uniqueId,
                       containers: containers]
        if (env.jaas_owner && containers.size() > 1) {
            withEnv(["STAGE_NAME=${stageName}"]) {
                podTemplate(options) {
                    node(nodeLabel ?: mergedStageConfiguration.uniqueId) {
                        unstashFiles script: script, stage: stageName
                        executeStage(body, stageName, mergedStageConfiguration, generalConfiguration)
                        stashFiles script: script, stage: stageName
                        echo "Current build result in stage $stageName is ${script.currentBuild.result}."
                    }
                }
            }
        } else {
            node(nodeLabel) {
                try {
                    unstashFiles script: script, stage: stageName
                    executeStage(body, stageName, mergedStageConfiguration, generalConfiguration)
                    stashFiles script: script, stage: stageName
                    echo "Current build result in stage $stageName is ${script.currentBuild.result}."
                } finally {
                    deleteDir()
                }
            }
        }
    }
}

private executeStage(Closure originalStage, String stageName, Map stageConfiguration, Map generalConfiguration) {
    def stageInterceptor = "pipeline/extensions/${stageName}.groovy"
    if (fileExists(stageInterceptor)) {
        Script interceptor = load(stageInterceptor)
        echo "Running interceptor for ${stageName}."
        interceptor(originalStage, stageName, stageConfiguration, generalConfiguration)
    } else {
        originalStage()
    }
}

private getContainerList(script, config, stageName) {
    def envVars
    def jnlpAgent = ConfigurationLoader.generalConfiguration(script).jnlpAgent ?: 's4sdk/jenkins-agent-k8s:latest'
    Map containerConfig = (script?.commonPipelineEnvironment?.configuration?.k8sMapping) ?: [:]
    if(!containerConfig.containsKey(stageName)){
       return [:]
    }
    Map containers = containerConfig[stageName]

    envVars = getContainerEnvs()
    result = []
    result.push(containerTemplate(name: 'jnlp',
        image: jnlpAgent,
        args: '${computer.jnlpmac} ${computer.name}'))

    containers.each { k, v ->
        result.push(containerTemplate(name: v,
            image: k,
            alwaysPullImage: true,
            command: '/usr/bin/tail -f /dev/null',
            envVars: envVars))
    }
    return result
}

private getContainerEnvs() {
    def containerEnv = []

    // Inherit the proxy information from the master to the container
    def systemEnv = new SysEnv()
    def envList = systemEnv.getEnv().keySet()
    for (String env : envList) {
        containerEnv << envVar(key: env, value: systemEnv.get(env))
    }

    // ContainerEnv array can't be empty. Using a stub to avoid failure.
    if (!containerEnv) containerEnv << envVar(key: "EMPTY_VAR", value: " ")

    return containerEnv
}

