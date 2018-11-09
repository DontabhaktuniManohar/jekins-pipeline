import com.fedex.ci.*

// vars/buildPrepare.groovy
def call(Map config=[:]) {

   def jobpath = "${env.JOB_NAME}".split("/")
   def configName = config.name ?: jobpath[jobpath.length-2] + "@${env.BRANCH_NAME}"
   def bluepath = ''
   if (!config.name && ("${env.BRANCH_NAME}" == "null" || env.BRANCH_NAME == null )) {
       // is a trunk
      configName = jobpath[jobpath.length-1]
   }
   // poorman's join
    for (i=0;i < jobpath.length-1; i++) {
       if (i == 0) {
          bluepath = jobpath[i]
       } else {
         bluepath = bluepath + '%2F' + jobpath[i]
       }
    }
   def statusMessage = config.message ?: 'no message'
   def status = config.status ?: ''
   def context = config.context ?: 'PRJ'
   def emailTo = config.recipients ?: env.BUILD_RECEPIENTS
   emailTo = emailTo ?: 'akaan@fedex.com'
   def failureEmailTo = config.failure_recipients ?: env.BUILD_FAILURE_RECEPIENTS
   failureEmailTo = failureEmailTo ?: 'akaan@fedex.com'
   def step = config.step ?: 'Build'
   def icon = ''
   if ( status =~ /Failed/ || status =~ /ABORTED/ ) {
      icon = '\u2614'
      emailTo = emailTo + ',' + failureEmailTo + ',SEFS-BuildFailures@corp.ds.fedex.com'
   } else if ( status =~ /Success/ ) {
      icon = '\u2600\ufe0f'
   } else {
      icon = '\u27BF'
   }
   int jobDuration = (System.currentTimeMillis() - currentBuild.startTimeInMillis)/1000;
   def nodeName = "${env.NODE_NAME}".replaceAll('\'','')
   def build_history =
      "{\"jobName\":\"${env.JOB_NAME}\"," +
      "\"nodeNm\":\"${nodeName}\",\"issuer\":null,\"lastBuild\":null," +
      "\"sourceLocation\":\"${configName}\",\"comments\":\"${statusMessage}\"," +
      "\"notification\":\"${env.BUILD_RECEPIENTS}\"," +
      "\"failedAt\":null,\"buildNumber\":\"${env.BUILD_NUMBER}\"," +
      "\"buildCause\":\"${env.BUILD_CAUSE}\"," +
      "\"duration\":\"${jobDuration}\"," +
      "\"releaseTag\":\"${env.RELEASE_TAG}\"," +
        "\"status\":\"${status}\"}";
  def abort_flag = sh returnStatus: true, returnStdout: false, script:'wget -nc -q http://sefsmvn.ute.fedex.com/abort_flag'
  if (abort_flag == 0) {
    echo "abort = ${abort_flag}"
    currentBuild.result = 'ABORTED'
    return
  }
  def url = 'http://sefsmvn.ute.fedex.com/settings.xml'
  try {
    url = config.url ?: BUILD_SETTINGS_XML
  } catch (error_x) {
    url = 'http://sefsmvn.ute.fedex.com/settings.xml'
  }

   // now build, based on the configuration provided in the closure
   echo "\u2705 \u2705 \u2705 Prepare \u2705 \u2705 \u2705\nJOB_NAME=${env.JOB_NAME}"
   stage('Prepare') {
      // reset state machine
      sh returnStatus: true, script: 'rm RELEASED RELEASE_CUT BUILT DEPLOYED_TO_NEXUS UPLOADED 2> /dev/null'
      //sh 'ls -1'
      sh "curl -O ${url}; curl -O http://sefsmvn.ute.fedex.com/install.py;"
      sh script: '''
echo $(curl -s http://sefsmvn.ute.fedex.com/fdeploy-install.sh | grep VERSION= | head -n 1 | awk -F '=' '{ print $2 }') > DEPLOYMENT_PACKAGE_VERSION
        '''
      // setup fortify
      try {
          node('Master-Build') {
            dir("${env.JENKINS_HOME}/userContent/fortify"){
              stash includes: '*.sh', name: 'build.fortify'
            }
          }
      } catch (Exception e) {
        echo "${e}"
      }
      try {
        unstash "build.fortify"
      } catch (Exception e) {
        echo "${e}"
      }
      sh '''#!/bin/bash
FD_VERSION="$(cat DEPLOYMENT_PACKAGE_VERSION)"
python install.py -v ${FD_VERSION}
'''
      stash "build.prepare"


      try {
         sh(returnStdout: true, script: "curl -s -f -d '${build_history}'  -X POST 'http://c0009869.test.cloud.fedex.com:8090/sefs-dashboard/api/public/build/create' -H 'Content-Type: application/json'")
      } catch (error_x) {}
   }

}
