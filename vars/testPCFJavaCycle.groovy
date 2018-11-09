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

  echo "config=${config}"

  //def jobpath = "${env.JOB_NAME}".split("/")
  //def SEFS_OPCO = jobpath[jobpath.length-2].take(3)
  //def branch = "${env.BRANCH_NAME}".split("/")
  //def VERSION = branch[branch.length-1]
  def mailrecp = env.BUILD_RECEPIENTS ? env.BUILD_RECEPIENTS : "SEFS-${SEFS_OPCO}-DeploymentNotify@corp.ds.fedex.com,SEFS_CICD@corp.ds.fedex.com"
  //def apppath = jobpath[jobpath.length-2].split("_")
  //def APP_NAME = apppath[apppath.length-1]
  def APP_NAME = env.APP_NAME
  def APP_TYPE = env.APP_TYPE
  def t = null
  def module = '.'
  try { module = config.module ?: BUILD_MODULE } catch (error_y) {}
  dir(module) {
    t =  readMavenPom file: ''
  }
  def ARTIFACT_ID =  t.getArtifactId()
  def branch = t.getVersion()
  def VERSION = branch.tokenize("-")[0]
  def releaseName = config.releaseName ?: ""
  def repoType    = config.repoType ?: ""
  def svnPath     = config.svnPath ?: ""
  def APP_LEVEL = 'L0'
  def scanReport = 0
  def abortOut = ''
  def abortFlag = 0
  def errOut = ''
  def archnode = env.ARCH_NODE ?: 'ShipmentEFS'
  def goalresultss = []
	def cnt = -1
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
      /*try {
      goal = append("${RUN_INTTEST}", goal, 'org.apache.maven.plugins:maven-failsafe-plugin:integration-test -DskipITs=false')
      } catch (errorinttest) {}*/
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
      goals[i] = [ 'name' : 'PMD', 'goal' : resolveEnvironmentVar('pmd:pmd -Daggregate=true', value) ]
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
   // try {
   //    value = "${RUN_JACOCO}"
   //    goals[4] = [ 'name' : 'JaCoCo', 'goal' :
   //                resolveEnvironmentVar('org.jacoco:jacoco-maven-plugin:prepare-agent package test org.jacoco:jacoco-maven-plugin:report', value) ]
   // } catch (error) {
   //    goals[4]=null
   // }
   try {
      value = "${RUN_INTTEST}"
      goals[i] = [ 'name' : 'Integration Test / Failsafe', 'goal' :
                  resolveEnvironmentVar('org.apache.maven.plugins:maven-failsafe-plugin:integration-test -DskipITs=false', value) ]
      i++
   } catch (error) {
   }
   try {
      value = "${RUN_SONAR}"
      goals[i] = [ 'name' : 'Sonar', 'goal' :
                  resolveEnvironmentVar('sonar:sonar', value) ]
      i++
   } catch (error) {
   }
   echo "goals=${goals}"

   try {
      if (goals[5] != null && goals[0] != null) {
         if (goals[5].goal != null && goals[0].goal != null ) {
            goals[0].goal = goals[0].goal + " " + goals[5].goal
            goals[5]=null
         }
      }
   } catch (error) { }
   def stageName = config.stageName ?: 'Test'
   def nodeName = config.nodeName ?: 'Java7'
   def resumeFrom = config.resumeFrom ?: ''
   def extra_build_options = ''
   try { extra_build_options = '' ?: BUILD_OPTIONS } catch (error_x) {}

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
           try {
             parallel parallelExecutors
           } catch (error) {
             echo "parallel error: ${error}"
           }

            // reaping the results of the test cycle
            parallel(
              pom : {
                try {
                  echo " -> stashing pom"
                  stash includes: 'pom.xml', name: 'pom'
                  echo " -> stashed pom.xml"
                } catch (error) {
                   echo "Caught: ${error}"
                }
              },
              unittest : {
                try {
                  echo " -> stashing unittest data"
                  stash includes: '**/target/surefire-reports/*.xml', name: 'test.reports'
                  echo " -> stashed unittest data"
                    def scanReportSh = '''find . -name "TEST*.xml" -print0 | xargs -0 grep '<testsuite ' | sed -e 's/.errors=\"//' -e 's/\".//' | awk '{ SUM += $1} END { print SUM }' '''
                    scanReport =  sh returnStdout: true, script: scanReportSh
                    echo "Scan Report : " + scanReport.toString()
                } catch (error) {
                   echo "Caught1: ${error}"
                   goalresultss[++cnt] = [ 'name' : 'UnitTest', 'message' : 'Unit Test creation/publishing failed', 'errcode' : '20' , 'continue' : 'true' , 'actualerr' : error]
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
                    stash includes: '**/target/site/*.html,**/target/*.xml', name: 'xml.reports'
                    echo " -> stashed xml data"
                } catch (error) {
                   echo "Caught3: ${error}"
                }
              },
              jacoco : {
                try {
                    echo " -> stashing jacoco data"
                    stash includes: '**/target/site/jac*/*.*,**/target/*.exec,**/target/**/*.class,**/src/main/java/**', name: 'jacoco.reports'
                    echo " -> stashed jacoco data"
                } catch (error) {
                   echo "Caught4: ${error}"
                }
              }
            ) // parallel

            node(archnode){
              parallel(
                'arch-pom' : {
                  try{
                    unstash name : 'pom'
                  } catch (error){
                  echo "Unstashing pom.xml"
                  }
                },
                'arch-pmd' : {
                  try{
                    unstash name : 'xml.reports'
                  } catch (error){
                  echo "Unstashing error for PMD"
                  }
                },
                'arch-jacoco' : {
                  try{
                    unstash name : 'jacoco.reports'
                  } catch (error){
                  echo "Unstashing error for Jacoco"
                  }
                },
                'arch-test' : {
                  try{
                  unstash name : 'test.reports'
                  } catch (error){
                  echo "Unstashing error for Test Report"
                  }
                }
              )

              try {
                def cpdir = "cp -avr /opt/fedex/tibco/archreports.sh" + " " + WORKSPACE + "/target"
                sh cpdir
                sh '''
                if [ ! -d "${WORKSPACE}/target/surefire-reports" ]; then
                echo "Directory doesnot exits...Creating"
                mkdir "${WORKSPACE}/target/surefire-reports"
                find . -type f -name 'TEST*.xml' | xargs cp --parents -t ${WORKSPACE}/target/surefire-reports/
                find . -type f -name 'jacoco.csv' | xargs cp --parents -t ${WORKSPACE}/target/site/
                else
                echo "Directory exists!!"
                fi
                '''
                def archsh =  '''
                ./target/archreports.sh -o '''+ SEFS_OPCO + ''' -a  '''+ APP_TYPE + ''' -r CODE_COVERAGE -key '''+ BUILD_NUMBER + ''' -d ''' + WORKSPACE + '''/target/site -s ''' + APP_NAME + '''_Ver''' + VERSION +
                '''
                ./target/archreports.sh -o '''+ SEFS_OPCO + ''' -a  '''+ APP_TYPE + ''' -r UNIT_TEST -key '''+ BUILD_NUMBER + ''' -d ''' + WORKSPACE + '''/target/surefire-reports -s ''' + APP_NAME + '''_Ver''' + VERSION +
                '''
                ./target/archreports.sh -o '''+ SEFS_OPCO + ''' -a  '''+ APP_TYPE + ''' -r STATIC_CODE_ANALYSIS -key '''+ BUILD_NUMBER + ''' -d ''' + WORKSPACE + '''/target/pmd.xml -s ''' + APP_NAME + '''_Ver''' + VERSION
                def retCd = sh returnStatus: true, script: archsh

                if ( retCd > 0 ) {
              //    goalresultss[++cnt] = [ 'name' : 'ReportArchive', 'message' : 'ReportArchive failed', 'errcode' : '50' , 'continue' : 'true' , 'actualerr' : 'Report Archiving issue']
                echo "Placeholder"

                }
              } catch (error) {
                echo "Caught3: ${error}"
            //    goalresultss[++cnt] = [ 'name' : 'ReportArchive', 'message' : 'ReportArchive Script failed', 'errcode' : '50' , 'continue' : 'true' , 'actualerr' : 'Report Archiving issue']
              }
              try {
                def ManifestFile =  "/var/fedex/tibco/cicd_reports/reports_jenkin/${SEFS_OPCO}_${APP_TYPE}_${APP_NAME}_manifest_${BUILD_NUMBER}.txt"
                def Manifest = "${SEFS_OPCO},${APP_TYPE},${APP_NAME},SCA,${BUILD_NUMBER}," + BUILD_URL + "pmdResult" + '/' + ",${VERSION},${releaseName},${repoType},${svnPath}"
                Manifest += "\n${SEFS_OPCO},${APP_TYPE},${APP_NAME},CC,${BUILD_NUMBER}," + BUILD_URL + "jacoco" + '/' + ",${VERSION},${releaseName},${repoType},${svnPath}"
                Manifest += "\n${SEFS_OPCO},${APP_TYPE},${APP_NAME},UT,${BUILD_NUMBER}," + BUILD_URL + "testReport" + '/' + ",${VERSION},${releaseName},${repoType},${svnPath}"
                writeFile file: ManifestFile, text: Manifest
              } catch (error) {
                echo "Caught3: ${error}"
               // goalresultss[++cnt] = [ 'name' : 'ReportManifest', 'message' : 'Manifest File creation failed', 'errcode' : '60' , 'continue' : 'true' , 'actualerr' : 'Report Archiving issue']
              }
			  try {
              if ( scanReport.toInteger() != 0 ) {
                goalresultss[++cnt] = [ 'name' : 'UnitTest', 'message' : "Unit Test execution for ${SEFS_OPCO}_${APP_TYPE}_${APP_NAME} FAILED. Aborting the build and your application will not be deployed to any test levels until this issue is fixed.", 'errcode' : '60' , 'continue' : 'false' , 'actualerr' : 'Unit Test execution for ${SEFS_OPCO}_${APP_TYPE}_${APP_NAME} FAILED. Aborting the build and your application will not be deployed to any test levels until this issue is fixed.']
              }
			  } catch (error) {
                echo "Caught5: ${error}"
			  }
              if(cnt>-1){
              for (j=0;j<=cnt;j++) {
                 errOut += "<BR><font color=red>" + goalresultss[j].message + "</font><BR>"
                 abortFlag = (goalresultss[j].continue == 'false') ? ++abortFlag : abortFlag
                 abortOut += (goalresultss[j].continue == 'false') ? goalresultss[j].name + " - " + goalresultss[j].message + "\n" : ""
                 mailrecp = env.BUILD_FAILURE_RECEPIENTS ? env.BUILD_FAILURE_RECEPIENTS : "SEFS-${SEFS_OPCO}-DeploymentNotify@corp.ds.fedex.com,SEFS_CICD@corp.ds.fedex.com"
              }}

            	try {
              	def OUT_HTML = errOut + "<BR><BR>Please find the attachment for ${APP_NAME}/${VERSION} test reports."
                OUT_HTML += "<BR><BR><B> OPCO          : </B>${SEFS_OPCO}"
                OUT_HTML += "<BR><B> Component Type    : </B>${APP_TYPE}"
                OUT_HTML += "<BR><B> ApplicationName   : </B>${APP_NAME}"
                OUT_HTML += "<BR><B> ArtifactID        : </B>${ARTIFACT_ID}"
                OUT_HTML += "<BR><B> Branch            : </B>${VERSION}"
                OUT_HTML = repoType == "" ? OUT_HTML : OUT_HTML + "<BR><B> Repo Type         : </B>${repoType}"
                OUT_HTML = releaseName == "" ? OUT_HTML : OUT_HTML + "<BR><B> Release           : </B>${releaseName}"
                OUT_HTML += "<BR><BR><B>JOB URL<B><BR>${JOB_URL}"
                def IN_HTML = "<BR><BR>Code Coverage Link:</P>"
              	IN_HTML += "<BR><a href='${BUILD_URL}/jacoco/'>Code Coverage Analysis for ${APP_NAME}/${VERSION} </a>"
              	IN_HTML += "<BR><BR>Static Code Link:</P>"
              	IN_HTML += "<BR><a href='${BUILD_URL}/pmdResult/'>Static Code Analysis for ${APP_NAME}/${VERSION} </a>"
              	IN_HTML += "<BR><BR>Unit Test Link:</P>"
              	IN_HTML += "<BR><a href='${BUILD_URL}/testReport/'>Unit Test Analysis for ${APP_NAME}/${VERSION} </a>"
                def dest = "**/target/pmd.xml"
                dest += (abortFlag > 0) ? "" : ",**/target/site/jacoco/jacoco.csv"
                dest += (abortFlag > 0) ? "" : ",**/target/surefire-reports/TEST-*.xml"

                OUT_HTML +=  (abortFlag > 0) ? "" : IN_HTML + "<BR><BR>Thanks,<BR>CI/CD Team<BR>"
                def BUILD_FAIL = (abortFlag > 0) ? "BuildFailed" : "Reports"
                def OUT_HTML_SUB  = "CICD " + BUILD_FAIL + " for - ${SEFS_OPCO} - ${APP_TYPE} - ${APP_NAME}/${VERSION}"
                OUT_HTML_SUB = releaseName == "" ? OUT_HTML_SUB : OUT_HTML_SUB + "/Release: " + releaseName + " "
                OUT_HTML_SUB += "- ${APP_LEVEL} - BuildNumber : ${BUILD_NUMBER}"
                emailext attachmentsPattern: dest, body: OUT_HTML, subject: OUT_HTML_SUB, to: mailrecp
              } catch (error) {
                       echo "Caught1: ${error}"
              }
              if (abortFlag > 0) {
                echo "currrentBuild-${currentBuild.result}"
                currentBuild.result='UNSTABLE'
              }
            }
         }
      }
   }
}

def build(_name,_goal,_operationMode, extra_build_options) {
   return {
      stage(_name) {
        try {
          sh "mvn -Dpipeline.run=true -s ./settings.xml -B -U ${extra_build_options} ${_goal} ${_operationMode}"
        } catch (error) {
          echo "build: ${error}"
        }
      }
   }
}
