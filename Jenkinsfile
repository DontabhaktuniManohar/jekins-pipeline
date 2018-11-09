#!/usr/bin/groovy
import com.fedex.ci.*

library 'libz'
library 'AppServiceAccount'

pipeline {
  options {
    timeout(time: 1, unit: 'HOURS')
    timestamps()
    disableConcurrentBuilds()
    retry(1)
  }
  agent { label env.NODE_NAME }
  tools {
    jdk 'JAVA_8'
    maven Definitions.APACHE_MAVEN_VERSION
  }

  parameters {
    booleanParam(name: 'PERFORM_RELEASE_CUT', defaultValue: false, description: '')
    booleanParam(name: 'FORTIFY_SCAN', defaultValue: false, description: '')
    choice(name: 'JOB_TYPE', choices: 'CICD\nCD\nCI' , description: '')
    choice(name: 'LEVEL', choices: 'L1\nL2\nL3' , description: '')
  }
  environment {
    https_proxy = 'http://internet.proxy.fedex.com:3128'
    http_proxy = 'http://internet.proxy.fedex.com:3128'
  }
  stages{
      stage('Build & Test') {
          steps {
            script {
              buildSetup()
              if (params.JOB_TYPE.contains('CI')) {
                builder(perform_release_cut : params.PERFORM_RELEASE_CUT, fortify : params.FORTIFY_SCAN)
              }
            }
          }
        }
        stage('Publishing') {
            steps {
              script {
                if (params.JOB_TYPE.contains('CI')) {
                  collectTestResults {
                  }
                  publishTestResults('')
                  builder(upload : true, perform_release_cut : params.PERFORM_RELEASE_CUT, fortify : params.FORTIFY_SCAN)
              }
            }
          }
        }
        stage('Deployment') {
          agent { label 'ShipmentEFS' }
          steps {
            script {
              if (params.JOB_TYPE.contains('CD')) {
                deployer( JOB_TYPE : params.JOB_TYPE, LEVEL : params.LEVEL )
              }
            }

          }
        }
      } // stages
      post {

             success {
               step([$class: 'WsCleanup', cleanWhenFailure: false, cleanWhenNotBuilt: false, cleanWhenUnstable: false, notFailBuild: true])
               notification(status : 'Success')
             }
             unstable {
               notification(status : 'Unstable')
             }
             failure {
                 notification(status : 'Failed')
             }
             aborted {
                 notification(status : 'ABORTED')
             }
             changed {
                step([$class: 'WsCleanup'])
                 echo 'Things were different before...'
             }
           }

} // pipeline
