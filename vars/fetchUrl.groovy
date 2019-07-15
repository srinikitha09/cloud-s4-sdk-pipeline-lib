import static com.sap.cloud.sdk.s4hana.pipeline.EnvironmentAssertionUtils.assertPluginIsActive

def call(String url) {
    assertPluginIsActive('http_request')
    String proxy = null
    if (Jenkins.instance.proxy) {
        proxy = "${Jenkins.instance.proxy.name}:${Jenkins.instance.proxy.port}"
    }
    echo "DEBUG $proxy"
    def response = httpRequest(url: url, httpProxy: proxy)
    return response.content.toString()
}
