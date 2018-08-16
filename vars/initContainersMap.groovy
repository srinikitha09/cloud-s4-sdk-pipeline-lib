import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.ConfigurationLoader
import com.sap.piper.ConfigurationMerger
import com.sap.piper.ContainerMap

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'initContainersMap', stepParameters: parameters) {
        def script = parameters.script
        ContainerMap.getInstance().setMap(getContainers(script))
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
    stageToStepMapping.each { stageName, stepsMap -> containers[stageName] = getContainersList(script, stageName, stepsMap) }
    return containers
}

@NonCPS
def getContainersList(script, stageName, Map stepsMap) {
    def containers = [:]
    stepsMap.each { containerName, stepName ->
        def imageName = updateContainerForStep(script, stageName, stepName)
        if (imageName) {
            containers[imageName] = containerName.toString().toLowerCase()
        }
    }
    return containers
}

@NonCPS
def updateContainerForStep(script, stageName, stepName) {
    def stageConfiguration = ConfigurationLoader.stageConfiguration(script, stageName)
    final Map stepDefaults = ConfigurationLoader.defaultStepConfiguration(script, stepName)

    final Map stepConfiguration = ConfigurationLoader.stepConfiguration(script, stepName)

    Set stageConfigurationKeys = ['dockerImage']
    Set stepConfigurationKeys = ['dockerImage']

    Map configuration = ConfigurationMerger.merge(stageConfiguration, stageConfigurationKeys, stepConfiguration, stepConfigurationKeys, stepDefaults)

    return configuration.dockerImage ?: ''
}
