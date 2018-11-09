import com.fedex.ci.*

@NonCPS
def resolveEnvironmentVar(key,value) {
   if (value == 'true') {
      return key
   }
   return null;
}
@NonCPS
def isDefined(value) {
  try {
    if (value == 'true') {
       return true
    }
  } catch (errror) {
    return false
  }
}
@NonCPS
def append(value,base, addition) {
   if (value == 'true') {
      return base + ' ' + addition
   }
   return base;
}


// vars/buildPlugin.groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  echo "config=" + config.toString()
  // now build, based on the configuration provided
  def goals = []
  def value = ''
  def goal = ''
  def i = 0
  def failed_meet_test_flag = false
  goals[i] = [ 'name' : 'Test', 'goal' : 'test -DskipTests=false']
  try {
    //if (isDefined("true")) {
      try {
        goal = append("true", goal, 'org.jacoco:jacoco-maven-plugin:0.8.1:prepare-agent')
      } catch (errorjacoco) {}
      try {
        goal = append("true", goal, 'test -DskipTests=false')
      } catch (error) {
        failed_meet_test_flag = true
      }
      try {
      goal = append("${RUN_INTTEST}", goal, 'org.apache.maven.plugins:maven-failsafe-plugin:integration-test -DskipITs=false')
      } catch (errorinttest) {}
      try {
        goal = append("true", goal,' package org.jacoco:jacoco-maven-plugin:0.8.1:report')
      } catch (errorjacoco2) {}
      goals[i] = [ 'name' : 'Test', 'goal' : goal ]
      if (goal != '') {
        i++
      }
    // }
    // else {
    //   goal = ''
    // }
  } catch (exxor) {
    error(exxor)
  }
   try {
      value = "${RUN_PMD}"
      goals[i] = [ 'name' : 'PMD', 'goal' : resolveEnvironmentVar('findbugs:check findbugs:findbugs -Dfindbugs.failOnError=false -Dfindbugs.fork=true pmd:pmd -Daggregate=true', value) ]
      //goals[1] = resolveEnvironmentVar('org.apache.maven.plugins:maven-pmd-plugin:3.7:pmd -Daggregate=true', "${RUN_PMD}")
      i++
   } catch (error) {
   }
   try {
      value = "${RUN_FINDBUGS}"
      goals[i] = [ 'name' : 'FindBugs', 'goal' : resolveEnvironmentVar('findbugs:check findbugs:findbugs -Dfindbugs.failOnError=false -Dfindbugs.fork=true -Dfindbugs.', value) ]
      i++
   } catch (error) {
   }

   try {
      value = "${RUN_CHECKSTYLE}"
      goals[i] = [ 'name' : 'CheckStyle', 'goal' : resolveEnvironmentVar('checkstyle:checkstyle', value) ]
      i++
   } catch (error) {
      goals[i]=null
   }
   try {
      value = "${RUN_INTTEST}"
      goals[i] = [ 'name' : 'Integration Test / Failsafe', 'goal' :
                  resolveEnvironmentVar('org.apache.maven.plugins:maven-failsafe-plugin:integration-test -DskipITs=false', value) ]
      i++
   } catch (error) {
   }
   echo "goals[${i}]=${failed_meet_test_flag}=${goals}"

   def stageName = config.stageName ?: 'Test'
   def nodeName = config.nodeName ?: 'Java7'
   def resumeFrom = config.resumeFrom ?: ''
   def extra_build_options = ''
   def module = '.'
   try { extra_build_options = '' ?: BUILD_OPTIONS } catch (error_x) {}
   try { module = config.module ?: BUILD_MODULE } catch (error_y) {}

   def operationMode = config.operationMode ?: '-fae'
   def headless = config.headless ?: false
   // command line assignments
   def java_home = tool name : 'JAVA_8'
   def m2_home = tool name : 'apache-maven-3.3.9'
   echo "java_home\t=${java_home} on node ${nodeName}\nm2_home\t=${m2_home}"
   def XRUN = ''
   if (headless || Definitions.isHeadless(nodeName)) {
      XRUN = '/usr/bin/xvfb-run -a -f ${HOME}/.Xauthority'
   }
   echo "\u2705 \u2705 \u2705 ${stageName} \u2705 \u2705 \u2705"
   withEnv([
      "JAVA_HOME=${java_home}",
      "M2_HOME=${m2_home}",
      "PATH+MAVEN=${m2_home}/bin:${java_home}/bin",
      "MAVEN_OPTS=-Xmx2048m -Xms1024m -Dmaven.artifact.threads=10 -Djava.security.egd=file:/dev/./urandom",
      "https_proxy=https://internet.proxy.fedex.com:3128",
      "http_proxy=https://internet.proxy.fedex.com:3128"
   ]) {
    dir(module) {
      unstash "build.prepare"
      parallelExecutors=[:]
      for (j=0;j<i;j++) {
        def field = goals[j]
        echo "goal[${j}]= ${field}"
        if (goals[j] != null && goals[j].goal != null && goals[j].goal != 'null' ) {
           echo "goals= ${field}"
           parallelExecutors["Goals - ${field.name}"] = build(field.name,field.goal,operationMode,extra_build_options)
        }
      }

      if (parallelExecutors.size() > 0) {
        parallel parallelExecutors
        parallel(
          unittest : {
            try {
                echo " -> stashing unittest data"
                stash includes: '**/target/surefire-reports/*.xml', name: 'test.reports'
                echo " -> stashed unittest data"
            } catch (error) {
               echo "Caught1: ${error}"
            }
          },
          failsafe : {
            try {
                echo " -> stashing failsafe data"
                stash includes: '**/target/failsafe-reports/TEST-*.xml', name: 'failsafe.reports'
                echo " -> stashing failsafe data"
            } catch (error) {
               echo "Caught2: ${error}"
            }
          },
          pmd : {
            try {
                echo " -> stashing xml data"
                stash includes: '**/target/*.xml', name: 'xml.reports'
                echo " -> stashed xml data"
            } catch (error) {
               echo "Caught3: ${error}"
            }
          },
          jacoco : {
            try {
                echo " -> stashing jacoco data"
                stash includes: '**/target/*.exec,**/target/**/*.class,**/src/main/java/**', name: 'jacoco.reports'
                echo " -> stashed jacoco data"
                sh 'find . -type f -name "*.exec" '
            } catch (error) {
               echo "Caught4: ${error}"
            }
          }
        ) // parallel
        try {
           value = "${RUN_SONAR}"
           def jobpath = "${env.JOB_NAME}".split("/")
           def project_name = jobpath[jobpath.length-2] + "-" +jobpath[jobpath.length-2]
           def project_key = jobpath[jobpath.length-2]
           sonarScan {
             projectKey = project_key
             projectName = project_name
           }
           i++
        } catch (error) {
        }

      } //if
    } //dir
  } //withEnv
 } // call

def build(_name,_goal,_operationMode, extra_build_options) {
   return {
      stage(_name) {
         sh "mvn -Dpipeline.run=true -s ./settings.xml -B -U ${extra_build_options} ${_goal} ${_operationMode}"
      }
   }
}
