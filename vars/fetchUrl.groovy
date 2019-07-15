import com.cloudbees.groovy.cps.NonCPS

@NonCPS
def call(url) {
    System.setProperty("http.proxyHost", Jenkins.instance.proxy.name);
    System.setProperty("http.proxyPort", "${Jenkins.instance.proxy.port}");
    return new URL(url).getText()
}
