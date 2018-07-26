import com.sap.piper.ConfigurationLoader
def call(Map parameters) {
    handleStepErrors(stepName: 'initS4SdkPipelineLibrary', stepParameters: parameters) {
        def script = parameters.script

        loadPiper script: script

        if (!parameters.configFile) {
            parameters.configFile = 'pipeline_config.yml'
        }

        setupCommonPipelineEnvironment(parameters)

        echo "Values are ${ConfigurationLoader.stepConfiguration(script, 'executeNpm')}"
        initContainersMap script:script

        loadS4sdkDefaultValues script: script

        echo "Values are ${ConfigurationLoader.stepConfiguration(script, 'executeNpm')}"

        convertLegacyConfiguration script: script

        echo "Values are ${ConfigurationLoader.stepConfiguration(script, 'executeNpm')}"

        setupDownloadCache script: script

        initContainersMap script:script

    }
}
