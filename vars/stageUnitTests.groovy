import com.sap.cloud.sdk.s4hana.pipeline.ConfigurationLoader

def call(Map parameters = [:]) {
    def stageName = 'unitTests'
    def script = parameters.script
    runAsStage(stageName: stageName, script: script) {
        Map configuration = ConfigurationLoader.stageConfiguration(script, stageName)

        try {
            executeMaven(
                script: script,
                flags: '-B',
                pomPath: 'unit-tests/pom.xml',
                m2Path: s4SdkGlobals.m2Directory,
                goals: 'org.jacoco:jacoco-maven-plugin:0.7.9:prepare-agent test',
                dockerImage: configuration.dockerImage,
                defines: '-Dsurefire.forkCount=1C'
            )
        } catch(Exception e) {
            executeWithLockedCurrentBuildResult(script: script, errorStatus: 'FAILURE', errorHandler: script.buildFailureReason.setFailureReason, errorHandlerParameter: 'Backend Unit Tests', errorMessage: "Please examine Backend Unit Tests report.") {
                script.currentBuild.result = 'FAILURE'
            }
            throw e
        }
        finally {
            junit allowEmptyResults: true, testResults: 'unit-tests/target/surefire-reports/TEST-*.xml'
        }

        copyExecFile execFiles: [
            'unit-tests/target/jacoco.exec',
            'unit-tests/target/coverage-reports/jacoco.exec',
            'unit-tests/target/coverage-reports/jacoco-ut.exec'
        ], target: 'unit-tests.exec'
    }
}




