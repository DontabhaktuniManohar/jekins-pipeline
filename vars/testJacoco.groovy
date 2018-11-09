def call(body) {
	// evaluate the body block, and collect configuration into the object
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()
    //Get the Conciguration of Parameters passed for execution
    def SEFS_OPCO = config.SEFS_OPCO ?: 'FXF FXG FXE'
    def SEFS_LEVEL = config.SEFS_LEVEL ?: 'L1'
    def sefs_level = config.sefs_level 
	def sefs_opco = config.sefs_opco 
    def suite_url = config.suite_url
    def APP = config.APP ?: 'BE BW'
    def goal = config.goal ?: 'compile'
    def ManLoc = '/var/fedex/tibco/cicd_reports/reports_jenkin'
    def mailrecp = config.mailrecp ?: 'sefs_cicd@corp.ds.fedex.com'
    def opco_inj = ""

    //Set the Environment for execution of the process
		 withEnv([
			 "JAVA_HOME=/opt/java/hotspot/7/current",
			 "PATH+MAVEN=/opt/jenkins/tools/apache-maven-3.3.9/bin:${env.JAVA_HOME}/bin",
			 "MAVEN_OPTS=-Xmx2048m -Xms1024m -Dmaven.artifact.threads=10 -Dmaven.repo.local=/opt/fedex/jenkins/.m2/repository",
			 "https_proxy=https://internet.proxy.fedex.com:3128",
			 "http_proxy=https://internet.proxy.fedex.com:3128",
			 "SEFS_OPCO=${SEFS_OPCO}",
			 "SEFS_LEVEL=${SEFS_LEVEL}"
		 ]) {
				
			try {

			//	unstash "build.prepare"
			//	unstash "build.output"
			
	checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: '9e863997-c0d9-4acb-8d54-99bd6c0b6ae1', depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: 'https://conexus.prod.fedex.com:9443/subversion/sefs_common/jacoco/trunk']], workspaceUpdater: [$class: 'UpdateUpdater']])
								    
			    def AppConfigURL = "./jacoco-jenkins/src/main/resources/Appconfig.csv"
			    def TrigMail = 0
                def ArrList = SEFS_OPCO.split(" ") as String[]
                for(int i = 0;i<ArrList.size();i++){
                echo "--------Executing OPCO : " + ArrList[i]
                iter = ArrList[i]
                def ArrList2 = APP.split(" ") as String[]
               
                for(int j = 0;j<ArrList2.size();j++)
                    {
                    currapp = ArrList2[j]

                    def AppConfigSh =  "cat " + AppConfigURL + ' | grep "' + iter + '," | grep "' + currapp + '," | ' + "cut -d ',' -f 3-"  
                    def AppConfig =  sh returnStdout: true, script: AppConfigSh
                    echo "--------Executing Application : " + AppConfig.toString()
                    def AppList = AppConfig.split("\\r?\\n") as String[]

                    def ws = pwd()
                    def BID = BUILD_NUMBER
                    def CURR_BLD = BUILD_NUMBER
                    def WRK_PRE = pwd() + "/target/" + currapp.toLowerCase() + "/"
                    def URL_PRE = env.BUILD_URL
                    def ReportXML = "index.html"
                    def VAR = 0
                    def Thresh = 0
                    def VARNO = "#e6ccff"
                    def VARFO = "#F4ECF7"
                    def CURR = VARNO
                    
                    def OUT_HTML = "<table><tr bgcolor='#99ccff'><td width='30%'><b>App Name</b></td><td><b>Coverage required %</b></td><td><b>Actual Coverage %</b></td><td width='30%'><b>Report Link</b></td></tr>"
                    def IN_HTML="<html><body><BR>HI All,<BR>Please find the Jacoco report details below : <BR><BR><BR>"
                    IN_HTML += "<table><tr bgcolor='#99ccff'><td width='8%'><b>Number</b></td><td><b>Coverage Report</b></td></tr>"
                    
                    withEnv([
                        "SEFS_OPCO=${iter}"
                    ]) {
                        
                    def CovSh = 'mvn -X -U -B clean compile -Ddo.' + currapp.toLowerCase() + '.codecoverage=true -Dcoverage.app=' + currapp.toLowerCase() + ' -P' + iter + ' -P' + env.BUILD_NUMBER
                    echo "Executing Maven for Code Coverage as : " + CovSh
                    
                    
                    try{
                       sh returnStatus: true, script: CovSh
                        
                        if (currapp.toLowerCase() == 'bw')
                            {
                                sh returnStatus: true, script: CovSh
                                checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: '9e863997-c0d9-4acb-8d54-99bd6c0b6ae1', depthOption: 'infinity', ignoreExternalsOption: true, local: 'SmokeTest', remote: 'https://conexus.prod.fedex.com:9443/subversion/sefs_test_automation/trunk']], workspaceUpdater: [$class: 'UpdateUpdater']])
                                
                                if ( iter == 'FXE' ) {
                                    opco_inj = ""
                                    } 
								else {
								    opco_inj = iter
									 }
									sefs_level = "${SEFS_LEVEL}"
                                    sefs_opco = "${SEFS_OPCO}"
									suite_url = "SmokeTest/test/${SEFS_LEVEL}/${opco_inj}SmokeTest/Tests/Suites/${SEFS_LEVEL}Suite.ste"
									echo "suite_url = ${suite_url}"
							    
                                
                               
                                withCredentials([usernamePassword(credentialsId: 'ca-dev-tst',
			passwordVariable: 'CA_DEV_TEST_PASSWORD', usernameVariable: 'CA_DEV_TEST_USER')]) {
			withEnv([
				"TEST_SUITE_TO_RUN=${suite_url}"
			]) {
			    echo "Executing Test Suite: ${suite_url}"
                                stage("SmokeTest"){
                                    sh '''#!/bin/bash -l
#!/bin/bash
set +xe
     
export CADEVTEST_HOME=/opt/fedex/tibco/CADevTest
export TIBCO_HOME=/opt/tibco/RA1.1
export AS_HOME=${TIBCO_HOME}/as/2.1

export JAVA_HOME=/opt/java/hotspot/7/current
export JAVA_VENDOR=Oracle
export M2_HOME=/opt/fedex/tibco/apache-maven-3.3.9

export PATH=${CADEVTEST_HOME}/lib:${CADEVTEST_HOME}/bin:${AS_HOME}/lib:${AS_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}:/usr/local/bin
echo PATH=${PATH}

export LD_LIBRARY_PATH=${CADEVTEST_HOME}/lib:${AS_HOME}/lib:${AS_HOME}/bin
echo LD_LIBRARY_PATH=${LD_LIBRARY_PATH}

echo
which java
which mvn

echo
echo "========================================================================="
echo "Copy/setup test suite from Conexus-SVN checkout"
echo "https://conexus.prod.fedex.com:9443/subversion/sefs_test_automation/trunk"
echo "========================================================================="
echo
echo "TEST_SUITE_TO_RUN=${TEST_SUITE_TO_RUN}"
echo
echo
echo "========================================================================="
echo "Run CA Dev Test HERE"
echo "========================================================================="

if [[ "${TEST_SUITE_TO_RUN}" == "NONE" ]]; then
  echo; echo "No test suite selected to run.  Exiting 0."
  exit 0
fi

#cd /opt/fedex/tibco/CADevTest/bin

#Give cadevtst r/w/d permission on project root
find . -type d -exec chmod 777 {} \\;
find . -type f -exec chmod 777 {} \\;

#
# This was the original way to run CA Dev Test but it exposes the password on the command line to ps commands
#
#./TestRunner -u cadevtst -p ${CADEVTEST} -m ssl://trh00200.ve.fedex.com:2010/Registry

# TestRunner2 is a copy of the script that will read the PW from stdin so it's not showing on the ps command line
# The resultant java command executed by TestRunner2 is so long that it exceeds the max ps display size
# so the password is not revealed via the ps command.  This is not a perfect solution but will suffice for now.
umask 011

echo ${CA_DEV_TEST_PASSWORD} | ${CADEVTEST_HOME}/bin/TestRunner2 -u ${CA_DEV_TEST_USER} -m ssl://trh00200.ve.fedex.com:2010/Registry -s ${TEST_SUITE_TO_RUN}

'''
                                }
                            
                            dir(workspace+"/jacoco-jenkins"){
                                sh returnStatus: true, script: CovSh
                            }
			}
			}
                            }
                        
                        }
                    catch (Exception) 
			            {
			            echo "Caught: ${Exception}"
			            }
                    
                    	 for(int k = 0;k<AppList.size();k++)
                    	 {
                    	    CURR = (VARNO == CURR) ? VARFO : VARNO
                            VAR += 1 

                    	    def currAppName = AppList[k].split(",")[0]
                    	    def currAppTh = AppList[k].split(",")[1]
                            Thresh = currAppTh
                            def dirtest = ws + "/jacoco-jenkins/target/" + currapp.toLowerCase() + "/" + currAppName + "/reports/${BID}"
                            def PubLink = "jacoco-jenkins/target/" + currapp.toLowerCase() + "/" + currAppName + "/reports/${BID}"
                            
                            IN_HTML += "<tr bgcolor=" + CURR +"><td><center>" + VAR + "</center></td>"

                    	  	if (fileExists(dirtest + "/" + ReportXML)) 
			                {
			                    echo "FileExists"
			                    publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true, reportDir: PubLink, reportFiles: ReportXML, reportName: currAppName ])
		
		                        def scanReportSh = ""
			                    if ("bw" == currapp.toLowerCase()) {
			                        scanReportSh = '''grep -oP \'(?<=<tfoot>).*(?=</tfoot>)\' ''' + dirtest + "/" + ReportXML +''' | grep -oP \'(?<=<td class="ctr2">).*(?=%)\''''
			                    }
			                    else if ("be" == currapp.toLowerCase()) {
			                        scanReportSh = '''grep -oP \'(?<=<tfoot>).*(?=</tfoot>)\' ''' + dirtest + "/" + ReportXML +''' | grep -oP \'(?<=<td class="ctr2">).*(?=%)\' | cut -d '%' -f1'''
			                    }
			                    else {}
			                        
			                    def scanReport =  sh returnStdout: true, script: scanReportSh
			                        
			                        if ( scanReport.toInteger() < currAppTh.toInteger() )
			                        { 
			                            TrigMail += 1
			                        }
			                      
			                        OUT_HTML += "<td>" + currAppName + "</td>"
                                    OUT_HTML += "<td>" + currAppTh + "</td>"
                                    OUT_HTML += "<td>" + scanReport + "</td>"
                                    OUT_HTML += "<td><A href='${URL_PRE}/${currAppName}/'>${currapp.toUpperCase()} ${currAppName}</A></td><tr>"
	
			                        def testurl = URL_PRE + "/" + currAppName + "/"
			                        def js1 = "echo `grep -oP '.*(?=<div class=\"footer\">)' " + dirtest + "/" + ReportXML + " | cat ` | sed 's#src=\"#src=\"" + testurl + "#g' | sed 's#href=\"#href=\"" + testurl + "#g'"  
                                    def incat =  sh returnStdout: true, script: js1
    
                                    IN_HTML += "<td>" + incat + "<BR><BR><BR></td></tr>"
			                    
			                    
			                }
			                else
			                {
                                    IN_HTML += "<td>Missing Report for <B>" + currapp.toUpperCase() + " " + currAppName + "</B> - Check if the services/Jacoco agent are running<br><br></td></tr>"			                    
			                }
                    
	                    def Manifest = "echo " + iter + "," + currapp.toUpperCase() + "," + currAppName + ",CC," + BUILD_NUMBER + "," + URL_PRE  + currAppName + '/,' + dirtest + '/ >> ' + iter + "_" + currapp.toUpperCase() + "_CC_manifest_" + BUILD_NUMBER + ".txt"  
	                    sh returnStatus: true, script: Manifest
	                    
                    	  
                    	def ArchSh = './jacoco-jenkins/src/main/resources/archreports.sh -o ' + iter + " -a " + currapp.toUpperCase() + ' -key ' + BUILD_NUMBER + " -s " + currAppName + " -r CODE_COVERAGE -d \"./jacoco-jenkins/target/" + currapp.toLowerCase() + "/" + currAppName + "/reports/" + BUILD_NUMBER + "/*\" " + "-t 20170101010101" 
	                    try{
							sh 'chmod 750 ./jacoco-jenkins/src/main/resources/archreports.sh'
	                        sh returnStatus: true, script: ArchSh
	                        }
	                    catch (Exception)
				            {
				            echo "Caught: ${Exception}"
				            }
                    	  
                    	  
                    	     
                    	 }
                    	 
                    	 
                    	}

		                            IN_HTML += "</table><br>Thanks<br>CI/CD Team</body></html>"
		            			    IN_HMTL_SUB = "CICD Reports - ${SEFS_LEVEL}  - ${iter.toUpperCase()}  - ${currapp.toUpperCase()} Application Code Coverage Analysis - RunNumber : ${BUILD_NUMBER}"

		            			    emailext body: IN_HTML, subject: IN_HMTL_SUB, to: mailrecp
		            			                        
			                        if ( TrigMail > 0 )
			                        {
                                    OUT_HTML += "</table>"
                                    OUT_HTML = "Please click the link below to view Code Coverage of ${iter.toUpperCase()} ${currapp.toUpperCase()}  Applications in ${SEFS_LEVEL}<br><br>Coverage percentage is less than required threshold of ${Thresh}%. <br><br>" + OUT_HTML
                                    OUT_HMTL_SUB  = "CICD Reports - ${SEFS_LEVEL}  - ${iter.toUpperCase()}  - ${currapp.toUpperCase()} Application Code Coverage Analysis below threshold requirement of ${Thresh}% "
                            
                                    emailext body: OUT_HTML, subject: OUT_HMTL_SUB, to: mailrecp
			                        }
                             TrigMail = 0
                    }
                }
 
			    def MovManFile = 'mv *manifest*' + BUILD_NUMBER + "*.txt " + ManLoc
			    sh MovManFile
			}
				catch (Exception) 
			    {
			        echo "Caught: ${Exception}"
			    }
	 }
}