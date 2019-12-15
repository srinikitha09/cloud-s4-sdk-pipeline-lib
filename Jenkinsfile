#!/usr/bin/env groovy
import com.sap.piper.testutils.JenkinsController

//fixme replaced original jenkinsfile for testing


@Library(['jenkins-library-test']) _

timeout(time: 5, unit: 'HOURS') {

    node("master") {
        stage('Init') {
            deleteDir()
            checkout scm
            if (!env.HOST) {
                error "Violated assumption: Environment variable env.HOST is not set. This must be set as the hostname."
            }
        }

        JenkinsController jenkins

        stage('Start Jenkins') {
            jenkins = new JenkinsController(this, "http://${env.HOST}")
            jenkins.waitForJenkinsStarted()
        }

        stage('Trigger Job') {
            jenkins.buildJob('validation-job')
        }

        stage('Wait for successful build') {
            jenkins.waitForSuccess('validation-job', 'sdk-lib-test-mta')
        }
    }
}
