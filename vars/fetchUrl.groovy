import com.cloudbees.groovy.cps.NonCPS

import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

@NonCPS
def call(url) {
    assertPluginIsActive('http_request')
    def response = httpRequest url
    return response.content
}
