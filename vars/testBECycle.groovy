// vars/testBECycle.groovy
import com.fedex.ci.*

@NonCPS
def resolveEnvironmentVar(key,value) {
   if (value == 'true') {
      return key
   }
   return null;
}



def jacocoReport(opco,app_name) {

    def APP = 'BE'
    def SEFS_OPCO = opco
    def APP_NAME = app_name
    def SEFS_LEVEL = 'L0'
    def ws = WORKSPACE
    def BID = BUILD_NUMBER
    def CURR_BLD = BUILD_NUMBER
    def WRK_PRE = ws + "/target/Coveragereports"
    def URL_PRE = env.JOB_URL
    def ReportXML = "index.html"
    def VAR = 1

    def Thresh = 0
    def VARNO = "#e6ccff"
    def VARFO = "#F4ECF7"
    def CURR = VARNO

    def OUT_HTML = "<table><tr bgcolor='#99ccff'><td width='30%'><b>App Name</b></td><td><b>Coverage required %</b></td><td><b>Actual Coverage %</b></td><td width='30%'><b>Report Link</b></td></tr>"

    def IN_HTML="<html><body><BR><BR>"
    IN_HTML += "<table><tr bgcolor='#99ccff'><td width='8%'><b>Number</b></td><td><b>Coverage Report</b></td></tr>"

    dir(WORKSPACE+"/target/Coveragereports"){
            def dirtest = ws + "/target/Coveragereports"
            def PubLink = "target/Coveragereports"
            IN_HTML += "<tr bgcolor=" + CURR +"><td><center>" + VAR + "</center></td>"

            if (fileExists(ReportXML))
                                           {
                                               def scanReportSh = '''grep -oP \'(?<=<tfoot>).*(?=</tfoot>)\' ''' + dirtest + "/" + ReportXML +''' | grep -oP \'(?<=<td class="ctr2">).*(?=%)\''''
                                               def scanReport =  sh returnStdout: true, script: scanReportSh
                                               OUT_HTML += "<td>" + APP_NAME + "</td>"
                OUT_HTML += "<td>" + Thresh + "</td>"
                OUT_HTML += "<td>" + scanReport + "</td>"
                OUT_HTML += "<td><A href='${URL_PRE}/JacocoReport/'>BW ${APP_NAME}</A></td><tr>"
                def testurl = URL_PRE + "/JacocoReport/"
                def js1 = "echo `grep -oP '.*(?=<div class=\"footer\">)' " + dirtest + "/" + ReportXML + " | cat ` | sed 's#src=\"#src=\"" + testurl + "#g' | sed 's#href=\"#href=\"" + testurl + "#g'"
                def incat =  sh returnStdout: true, script: js1
                IN_HTML += "<td>" + incat + "<BR><BR><BR></td></tr>"

                }
                else
                {
				IN_HTML += "<td>Missing Report for <B>" + APP + " " + APP_NAME + "</B> - Check if the services/Jacoco agent are running<br><br></td></tr>"
                }
    }
            IN_HTML += "</table></body></html>"
            IN_HMTL_SUB = "CICD Reports - ${SEFS_LEVEL}  - ${APP}  - ${APP_NAME} Application Code Coverage Analysis - RunNumber : ${BUILD_NUMBER}"
			return IN_HTML

}


def call(body) {
   // evaluate the body block, and collect configuration into the object
   def config = [:]
   body.resolveStrategy = Closure.DELEGATE_FIRST
   body.delegate = config
   body()
    def jobpath = "${env.JOB_NAME}".split("/")
    def SEFS_OPCO = jobpath[jobpath.length-2].take(3)
    def branch = "${env.BRANCH_NAME}".split("/")
	def VERSION = branch[branch.length-1]
	def mailrecp = env.BUILD_RECEPIENTS ? env.BUILD_RECEPIENTS : "ddptaszynski@fedex.com,SEFS-${SEFS_OPCO}-DeploymentNotify@corp.ds.fedex.com"
    def apppath = jobpath[jobpath.length-2].split("_")
    //def APP_NAME = apppath[apppath.length-1]
    def APP_NAME = env.APP_NAME
    def APP_TYPE = env.APP_TYPE
	def t =  readMavenPom file: ''
    def ARTIFACT_ID =  t.getArtifactId()
    def releaseName = config.releaseName ?: ""
    def repoType    = config.repoType ?: ""
    def svnPath     = config.svnPath ?: ""

    def APP_LEVEL = 'L0'
    def finalStatus = 0
    def scanReport = 0
    def abortOut = ''
	def abortFlag = 0
    def errOut = ''

    def APP_EAR_LOC = "${WORKSPACE}/target/archive/bin"

   echo "config=" + config.toString()
   def goalresultss = []
	def cnt = -1
   def goals = []
   def value = ''
   goals[0] = [ 'name' : 'Test', 'goal' : 'test -DskipTests=false']
   try {
      value = "${RUN_TEST}"
      goals[0] = [ 'name' : 'Test', 'goal' : resolveEnvironmentVar('install -P unittest -Dmaven.test.failure.ignore=true', value), 'operation' : 'mvn']
   } catch (error) {
      goals[0]=null
   }


   try {
        value = "${RUN_STATIC}"
        def becom = "./becs.sh -s ${APP_EAR_LOC}/*.ear -t excel -o ${WORKSPACE}"
        becom = '''cd /opt/fedex/tibco/becs2.3.0/bin
        ''' + becom

        goals[1] = [ 'name' : 'Static', 'goal' : resolveEnvironmentVar(becom,value), 'operation' : 'sh' ]

   } catch (error) {
      goals[1]=null
   }
        try {
                 def hostnm = '''sed -i "s|tibco.be.host=[^ ]*|tibco.be.host=$(hostname)|g" ''' + "${WORKSPACE}/src/test/filters/Linux/build.properties"
                 def hostnm1 = '''sed -i "s|metaspace.host=[^ ]*|metaspace.host=$(hostname)|g" ''' + "${WORKSPACE}/src/test/filters/Linux/build.properties"
                 def retVal = sh returnStatus: true, script: hostnm
                 def retVal1 = sh returnStatus: true, script: hostnm1
            } catch (error) {
               echo "Caught1: ${error}"
                             }

   def stageName = config.stageName ?: 'Test'
   def nodeName = config.nodeName ?: 'Java7'
   def resumeFrom = config.resumeFrom ?: ''

   def module = '.'
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
         // sonar > 6
         for (i=0;i<2;i++) {
            if (goals[i] != null && goals[i].goal != null && goals[i].goal != 'null' ) {
               def field = goals[i]
               echo "goals= ${field}"
               parallelExecutors["Goals - ${field.name}"] = build(field.name,field.goal,field.operation,operationMode)
            }
         }


		try {
             def hostnm = '''sed -i "s|tibco.be.host=[^ ]*|tibco.be.host=$(hostname)|g" ''' + "${WORKSPACE}/src/test/filters/Linux/build.properties"
             def retVal = sh returnStatus: true, script: hostnm
        } catch (error) {
               echo "Caught1: ${error}"
        }


         if (parallelExecutors.size() > 0) {
            parallel parallelExecutors

            try {
                def scanReportSh = '''sed -n \'2p\' ''' + WORKSPACE + '''/target/surefire-reports/testng-results.xml | cut -d " " -f3 | cut -d \'"\' -f2'''
                scanReport =  sh returnStdout: true, script: scanReportSh
                stash includes: '**/target/surefire-reports/*.xml', name: 'test.reports'
                echo " -> stashing test data"

            } catch (error) {
               echo "Caught1: ${error}"
			   goalresultss[++cnt] = [ 'name' : 'UnitTest', 'message' : 'Unit Test creation/publishing failed', 'errcode' : '20' , 'continue' : 'true' , 'actualerr' : error]
            }
            try {
                stash includes: '**/*.xlsx', name: 'excel.reports'
                archiveArtifacts '**/*.xlsx'
                echo " -> stashing static data"
            } catch (error) {
               echo "Caught2: ${error}"
			   goalresultss[++cnt] = [ 'name' : 'StaticCode', 'message' : 'Static Code creation/publishing failed', 'errcode' : '30' , 'continue' : 'true' , 'actualerr' : error]

            }
            try {
                stash includes: '**/target/Coveragereports/*.xml,**/target/Coveragereports/*.csv,**/target/Coveragereports/*.html', name: 'jacoco.reports'
                echo " -> stashing jacoco data"
            } catch (error) {
               echo "Caught3: ${error}"
			   goalresultss[++cnt] = [ 'name' : 'CodeCoverage', 'message' : 'Code Coverage creation/publishing failed', 'errcode' : '40' , 'continue' : 'true' , 'actualerr' : error]

            }
         }
      }
   }



            try {
                def archsh =  '''
				./target/test/archreports.sh -o '''+ SEFS_OPCO + ''' -a  '''+ APP_TYPE + ''' -r CODE_COVERAGE -key '''+ BUILD_NUMBER + ''' -d ''' + WORKSPACE + '''/target/Coveragereports -s ''' + APP_NAME + '''_Ver''' + VERSION +
				'''
				./target/test/archreports.sh -o '''+ SEFS_OPCO + ''' -a  '''+ APP_TYPE + ''' -r UNIT_TEST -key '''+ BUILD_NUMBER + ''' -d ''' + WORKSPACE + '''/target/surefire-reports -s ''' + APP_NAME + '''_Ver''' + VERSION +
				'''
				./target/test/archreports.sh -o '''+ SEFS_OPCO + ''' -a  '''+ APP_TYPE + ''' -r STATIC_CODE_ANALYSIS -key '''+ BUILD_NUMBER + ''' -d ''' + WORKSPACE + '''/*.xlsx -s ''' + APP_NAME + '''_Ver''' + VERSION
				def retCd = sh returnStatus: true, script: archsh

				if ( retCd > 0 )
				{
					goalresultss[++cnt] = [ 'name' : 'ReportArchive', 'message' : 'ReportArchive failed', 'errcode' : '50' , 'continue' : 'true' , 'actualerr' : 'Report Archiving issue']
				}

            }
            catch (error) {
               echo "Caught3: ${error}"
			   goalresultss[++cnt] = [ 'name' : 'ReportArchive', 'message' : 'ReportArchive Script failed', 'errcode' : '50' , 'continue' : 'true' , 'actualerr' : 'Report Archiving issue']

            }

			try {
				def ManifestFile =  "/var/fedex/tibco/cicd_reports/reports_jenkin/${SEFS_OPCO}_${APP_TYPE}_${APP_NAME}_manifest_${BUILD_NUMBER}.txt"
				def Manifest = "${SEFS_OPCO},${APP_TYPE},${APP_NAME},SCA,${BUILD_NUMBER}," + BUILD_URL + ",${VERSION},${releaseName},${repoType},${svnPath}"
				Manifest += "\n${SEFS_OPCO},${APP_TYPE},${APP_NAME},CC,${BUILD_NUMBER}," + BUILD_URL + "JacocoReport" + '/' + ",${VERSION},${releaseName},${repoType},${svnPath}"
				Manifest += "\n${SEFS_OPCO},${APP_TYPE},${APP_NAME},UT,${BUILD_NUMBER}," + BUILD_URL + "testReport" + '/' + ",${VERSION},${releaseName},${repoType},${svnPath}"
				writeFile file: ManifestFile, text: Manifest
				}
            catch (error) {
               echo "Caught3: ${error}"
			   goalresultss[++cnt] = [ 'name' : 'ReportManifest', 'message' : 'Manifest File creation failed', 'errcode' : '60' , 'continue' : 'true' , 'actualerr' : 'Report Archiving issue']
            }

            if ( scanReport.toInteger() > 0 )
            {
				goalresultss[++cnt] = [ 'name' : 'UnitTest', 'message' : "Unit Test execution for ${SEFS_OPCO}_${APP_TYPE}_${APP_NAME} FAILED. Aborting the build and your application will not be deployed to any test levels until this issue is fixed.", 'errcode' : '60' , 'continue' : 'false' , 'actualerr' : 'Unit Test execution for ${SEFS_OPCO}_${APP_TYPE}_${APP_NAME} FAILED. Aborting the build and your application will not be deployed to any test levels until this issue is fixed.']
			}


			for (j=0;j<=cnt;j++) {
			   errOut += "<BR><font color=red>" + goalresultss[j].message + "</font><BR>"
			   abortFlag = (goalresultss[j].continue == 'false') ? ++abortFlag : abortFlag
			   abortOut += (goalresultss[j].continue == 'false') ? goalresultss[j].name + " - " + goalresultss[j].message + "\n" : ""
			   mailrecp = env.BUILD_FAILURE_RECEPIENTS ? env.BUILD_FAILURE_RECEPIENTS : "SEFS-${SEFS_OPCO}-DeploymentNotify@corp.ds.fedex.com,SEFS_CICD@corp.ds.fedex.com"
			}


			try {
			def jac = jacocoReport(SEFS_OPCO,APP_NAME)
			def OUT_HTML = errOut + "<BR><BR>Please find the attachment for ${APP_NAME}/${VERSION} test reports."
			OUT_HTML += "<BR><BR><B> OPCO          : </B>${SEFS_OPCO}"
			OUT_HTML += "<BR><B> Component Type    : </B>${APP_TYPE}"
			OUT_HTML += "<BR><B> ApplicationName   : </B>${APP_NAME}"
			OUT_HTML += "<BR><B> ArtifactID        : </B>${ARTIFACT_ID}"
			OUT_HTML += "<BR><B> Branch            : </B>${VERSION}"
			OUT_HTML = repoType == "" ? OUT_HTML : OUT_HTML + "<BR><B> Repo Type         : </B>${repoType}"
			OUT_HTML = releaseName == "" ? OUT_HTML : OUT_HTML + "<BR><B> Release           : </B>${releaseName}"
			OUT_HTML += "<BR><BR><B>JOB URL<B><BR>${JOB_URL}"

			OUT_HTML +=  (abortFlag > 0) ? "" : jacocoReport(SEFS_OPCO,APP_NAME) + "<BR><BR>Thanks,<BR>CI/CD Team<BR>"
            def BUILD_FAIL = (abortFlag > 0) ? "BuildFailed" : "Reports"
            def dest = "**/target/surefire-reports/testng-results.xml"
            dest += (abortFlag > 0) ? "" : ",**\\*.xlsx,"
            def OUT_HTML_SUB  = "CICD " + BUILD_FAIL + " for - ${SEFS_OPCO} - ${APP_TYPE} - ${APP_NAME}/${VERSION}"
            OUT_HTML_SUB = releaseName == "" ? OUT_HTML_SUB : OUT_HTML_SUB + "/Release:" + releaseName + " "
            OUT_HTML_SUB += "- ${APP_LEVEL} - BuildNumber : ${BUILD_NUMBER}"

            publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'target/Coveragereports', reportFiles: 'index.html', reportName: 'JacocoReport', reportTitles: ''])
			emailext attachmentsPattern: dest, body: OUT_HTML, subject: OUT_HTML_SUB, to: mailrecp

			} catch (error) {
               echo "Caught1: ${error}"

            }

            if (abortFlag > 0) {
                throw new Exception(abortOut)
            }

}
def build(_name,_goal,_operation,_operationMode) {
   return {
      stage(_name) {
        if ("mvn" == _operation) {
        sh "mvn -Dpipeline.run=true -s ./settings.xml -B -U ${_goal} ${_operationMode}"}
        if ("sh" == _operation) {
        sh _goal}
      }
   }
}
