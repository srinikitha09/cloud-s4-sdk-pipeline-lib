import com.sap.cloud.sdk.s4hana.pipeline.ConfigUtil
import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

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
    String nodeLabel = generalConfiguration.defaultNode

    def containersMap = ConfigUtil.getContainersMap(script,stageName)
    if (mergedStageConfiguration.node) {
        nodeLabel = mergedStageConfiguration.node
    }
    handleStepErrors(stepName: stageName, stepParameters: [:]) {
        if (env.ON_K8S == 'true' && containersMap.size() > 0) {
            withEnv(["POD_NAME=${stageName}"]) {
                containerExecuteInsidePod(script: script, containersMap: containersMap) {
                        unstashFiles script: script, stage: stageName
                        executeStage(body, stageName, mergedStageConfiguration, generalConfiguration)
                        stashFiles script: script, stage: stageName
                        echo "Current build result in stage $stageName is ${script.currentBuild.result}."
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

