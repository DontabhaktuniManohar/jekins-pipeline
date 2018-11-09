// vars/notify.groovy
def call(Map config = [:]) {
    def jobpath = "${env.JOB_NAME}".split("/")
    def configName = config.name ?: jobpath[jobpath.length-2] + "@${env.BRANCH_NAME}"
    echo "${config.name} / cn=${configName}/ bn=${env.BRANCH_NAME}"
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
    def subject = "${icon} ${context}:${configName} [${step} ${status}] : #(${env.BUILD_NUMBER})"
    //   if (config.simple) {
    //      subject = "${icon} ${context}:${message} [${step} ${status}] : #(${env.BUILD_NUMBER})"
    //   }
    echo "\u2705 \u2705 \u2705 Notifying \u2705 \u2705 \u2705 ${icon}"
    echo "change_author: ${env.CHANGE_AUTHOR_EMAIL}"
    echo "change_author: ${env.CHANGE_AUTHOR}"
    echo "email        : ${emailTo}"
    echo "failureEmailTo:${failureEmailTo}"
    mail mimeType: 'text/html', subject: "${subject}",
      body: "${icon} ${context}:${env.JOB_NAME}\n[${step} ${status} ${icon}] :<br><blockquote>Message: ${statusMessage}#</blockquote>\
      <blockquote>JOB:&nbsp;&nbsp;<a href=\"${env.JOB_URL}\">Job Configuration</a> #${env.BUILD_NUMBER}</blockquote>\
   <blockquote>Execution(${step}):&nbsp;&nbsp;<a href=\"${env.BUILD_URL}\">execution job details</a> executed on node '${env.NODE_NAME}'.</blockquote>\
   <blockquote>Changes(${step}):&nbsp;&nbsp;<a href=\"${env.BUILD_URL}changes\"> source changes </a>.</blockquote>\
   <blockquote>Activity (${step}):&nbsp;&nbsp;<a href=\"${env.JENKINS_URL}/blue/organizations/jenkins/${bluepath}/activity\">job activity details</a>(blue ocean).</blockquote>\
   <blockquote>Branches:&nbsp;&nbsp;<a href=\"${env.JENKINS_URL}/blue/organizations/jenkins/${bluepath}/branches\">listed job branches</a>.</blockquote>\
   <blockquote>CONSOLE:&nbsp;&nbsp;<a href=\"${env.BUILD_URL}console\">End of Console</a></blockquote>\
   <blockquote>FULL:&nbsp;&nbsp;<a href=\"${env.BUILD_URL}consoleFull\">Full Console Output</a> </blockquote>",
              to: "${emailTo}",
              replyTo: 'no-reply@jenkins-shipment.web.fedex.com',
              from: 'no-reply@jenkins-shipment.web.fedex.com'
    mattermostSend  channel: '#town-square', endpoint: ' https://mattermost.paas.fedex.com/hooks/brcuuinskidoxq5kx7fnfr9mpw', message: "${subject}", text: "${icon} ${context}:${env.JOB_NAME}\n[${step} ${status} ${icon}] :Message: ${statusMessage}#\n[SEE CONSOLE](${env.BUILD_URL}console)"

    try {
       sh(returnStdout: true, script: "curl -s -f -d '${build_history}'  -X POST 'http://c0009869.test.cloud.fedex.com:8090/sefs-dashboard/api/public/build/update' -H 'Content-Type: application/json'")
    } catch (error_x) {}
    unstash "build.prepare"
    try {
      //overwrite status for analytics
      status = "${currentBuild.result}"
      sh(returnStdout: true, script: "python segment.py -n '${configName}' -s '${status}' -b '${env.BUILD_NUMBER}' -m '${statusMessage}' -a '${env.CHANGE_AUTHOR}'")
    } catch (error_x) {}
}
