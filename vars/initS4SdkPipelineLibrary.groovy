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

        loadS4sdkDefaultValues script: script
        echo "Values are ${ConfigurationLoader.stepConfiguration(script, 'executeNpm')}"

        script.commonPipelineEnvironment.configuration.k8sMapping = getContainers(script: script)
        echo "Values are ${script.commonPipelineEnvironment.configuration.k8sMapping}"

        convertLegacyConfiguration script: script
        setupDownloadCache script: script
    }
}

@NonCPS
Map getContainers(script) {
    Map containers = [:]
    def stageToStepMapping = ['buildBackend'        : ['mavenExecute': 'mavenExecute'],
                              'buildFrontend'       : ['executeNpm': 'executeNpm'],
                              'staticCodeChecks'    : ['mavenExecute': 'mavenExecute'],
                              'unitTests'           : ['mavenExecute': 'mavenExecute'],
                              'integrationTests'    : ['mavenExecute': 'mavenExecute'],
                              'frontendUnitTests'   : ['executeNpm': 'executeNpm'],
                              'nodeSecurityPlatform': ['executeNpm': 'checkNodeSecurityPlatform'],
                              'endToEndTests'       : ['mavenExecute': 'mavenExecute', 'executeNpm': 'executeNpm', 'deployToCfWithCli': 'deployToCfWithCli', 'deployToNeoWithCli': 'deployToNeoWithCli'],
                              'performanceTests'    : ['mavenExecute': 'mavenExecute', 'checkJMeter': 'checkJMeter', 'deployToCfWithCli': 'deployToCfWithCli', 'deployToNeoWithCli': 'deployToNeoWithCli'],
                              's4SdkQualityChecks'  : ['mavenExecute': 'mavenExecute'],
                              'artifactDeployment'  : ['mavenExecute': 'mavenExecute'],
                              'whitesourceScan'     : ['mavenExecute': 'mavenExecute', 'executeNpm': 'executeNpm'],
                              'sourceClearScan'     : ['executeSourceClearScan': 'executeSourceClearScan'],
                              'productionDeployment': ['mavenExecute': 'mavenExecute', 'executeNpm': 'executeNpm', 'deployToCfWithCli': 'deployToCfWithCli', 'deployToNeoWithCli': 'deployToNeoWithCli']

    ]
    stageToStepMapping.each { k, v -> containers[k] = getContainersList(script, v) }
    return containers
}

@NonCPS
def getContainersList(script, Map stepsMap) {
    def containers = [:]
    stepsMap.each { k, v -> containers[updateContainerForStep(script, v)] = k.toString().toLowerCase() }
    return containers
}

@NonCPS
def updateContainerForStep(script, stepName) {
    def parameters = [:]
    final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, stepName)

    final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)

    Set parameterKeys = ['dockerImage']
    Set stepConfigurationKeys = ['dockerImage']

    Map configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

    echo "Configuration is ${stepConfiguration} stepName ${stepName} configuration ${configuration}"
    return (configuration.dockerImage).toString()
}
