import com.sap.cloud.sdk.s4hana.pipeline.CloudPlatform
import com.sap.cloud.sdk.s4hana.pipeline.DeploymentType

def call(Map parameters = [:]) {
    handleStepErrors(stepName: 'deployToCloudPlatform', stepParameters: parameters) {
        def index = 1
        def deployments = [:]
        def stageName = parameters.get('stage')
        def script = parameters.get('script')
        if (parameters.cfTargets) {
            for (int i = 0; i < parameters.cfTargets.size(); i++) {
                def target = parameters.cfTargets[i]
                deployments["Deployment ${index > 1 ? index : ''}"] = {
                    runAsStage(script: script, stageName: stageName, node: env.NODE_NAME) {
                        deployToCfWithCli script: parameters.script, appName: target.appName, org: target.org, space: target.space, apiEndpoint: target.apiEndpoint, manifest: target.manifest, credentialsId: target.credentialsId, deploymentType: DeploymentType.selectFor(CloudPlatform.CLOUD_FOUNDRY, parameters.isProduction.asBoolean())
                    }
                }
                index++
                echo "Increment index"
            }
            runClosures deployments, script
        } else if (parameters.neoTargets) {

            def pom = readMavenPom file: 'application/pom.xml'
            def source = "application/target/${pom.getArtifactId()}.${pom.getPackaging()}"
            for (int i = 0; i < parameters.neoTargets.size(); i++) {
                def target = parameters.neoTargets[i]
                deployments["Deployment ${index > 1 ? index : ''}"] = {
                    runAsStage(script: script, stageName: stageName, node: env.NODE_NAME) {
                        deployToNeoWithCli script: parameters.script, target: target, deploymentType: DeploymentType.selectFor(CloudPlatform.NEO, parameters.isProduction.asBoolean()), source: source
                    }
                }
                index++
            }
            runClosures deployments, script
        } else {
            currentBuild.result = 'FAILURE'
            error("Test Deployment skipped because no targets defined!")
        }
    }
}
