#!/usr/bin/env groovy
import com.fedex.ci.*

def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
	def scannerHome = tool 'SonarQube_Scanner';
	def splitJobName = env.JOB_NAME.split('/');
	def projectKey = (config.projectKey != null) ? config.projectKey : splitJobName[splitJobName.length - 2];
	def projectName = (config.projectName != null) ? config.projectName : splitJobName[splitJobName.length - 2];
	def src = (config.src != null) ? config.src : "src/main";
	def test = (config.test != null) ? config.test : "src/test";
	def binaries = (config.binaries != null) ? config.binaries : "target";
	def scmDisabled = (config.scmDisabled != null) ? "-Dsonar.scm.disabled=${config.scmDisabled}" : "";
	def repo = (config.repo != null) ? "-Dsonar.scm.provider=${config.repo}" : "";
	def java_home = tool name : 'JAVA_8'


		withEnv([
			"JAVA_HOME=${java_home}",
			"https_proxy=https://internet.proxy.fedex.com:3128",
			"http_proxy=https://internet.proxy.fedex.com:3128",
      "SONAR_SCANNER_OPTS=-Dhttps.proxyHost=internet.proxy.fedex.com -Dhttps.proxyPort=3128"
		]) {
			tool name: 'SonarQube_Scanner', type: 'hudson.plugins.sonar.SonarRunnerInstallation'
      model = readMavenPom file : 'pom.xml'
      withSonarQubeEnv('SonarQube') {
				sh "${scannerHome}/bin/sonar-scanner \
						-Dsonar.projectKey=SEFS_${projectKey}_${projectName} \
						-Dsonar.projectName=SEFS_${projectKey}_${projectName} \
            -Dsonar.projectVersion=${model.version} \
						-Dsonar.java.binaries=${binaries}  \
						-Dsonar.sources=${src} \
						-Dsonar.tests=${test} \
            -Dhttps.proxyHost=internet.proxy.fedex.com -Dhttps.proxyPort=3128 \
            -Dhttp.proxyHost=internet.proxy.fedex.com -Dhttp.proxyPort=3128 \
						${repo}  \
						${scmDisabled}"
			}
		}

}

def unstash_items(label) {
	try {
    echo "unstashing ${label}"
		unstash label
	} catch (exception) {
		echo "unstashing failure ${label}: ${exception}"
	}
}
