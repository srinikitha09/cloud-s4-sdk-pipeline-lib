import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'initContainersMap', stepParameters: parameters) {
        def script = parameters.script
        echo "${script.commonPipelineEnvironment}"
        script.commonPipelineEnvironment.configuration.k8sMapping = getContainers(script: script)
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
    echo "Containers are ${containers}"
    return containers
}

@NonCPS
def getContainersList(script, Map stepsMap) {
    def containers = [:]
    stepsMap.each { k, v -> containers[updateContainerForStep(script, v)] = k.toString().toLowerCase() }
    return containers
}

@NonCPS
def updateContainerForStep(script, String stepName) {
    def parameters = [:]
    final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, stepName)
    final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)
    Set stepConfigurationKeys = ['dockerImage']
    Set parameterKeys = ['dockerImage']

    def configuration = ConfigurationMerger.merge(parameters, parameterKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)
    echo "Here it is ${(configuration.dockerImage).toString()} ${stepName}"
    return (configuration.dockerImage).toString()
}
