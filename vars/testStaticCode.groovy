@NonCPS
def getSCGAV(opco) {
              echo "getSCGAV: ${opco}"
              if ('FXE' == "${opco}") {
                             return [ groupId : 'com.fedex.sefs.core', version : '5.10.0.UGG-SNAPSHOT', artifactId : 'sefs_silverControl_FX']
              } else     if ('FXF' == "${opco}") {
                             return [ groupId : 'com.fedex.fxf.core', version : '2.0.0-SNAPSHOT', artifactId : 'sefs_silverControl_FXF']
              } else     if ('FXG' == "${opco}") {
                             return [ groupId : 'com.fedex.ground.sefs', version : '2.5.0.VBATTALION-SNAPSHOT', artifactId : 'sefs_SilverControl_FXG']
              } else     if ('FXS' == "${opco}") {
                             return [ groupId : 'com.fedex.sefs.common', version : '1.6.0-SNAPSHOT', artifactId : 'sefs_silverControl_FXS']
              } else {
                             throw new Exception("undefined opco value; ${opco}. Aborting!")
              }
}

def call(body) {


              //evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    //Get the Conciguration of Parameters passed for execution
    def SEFS_OPCO = config.SEFS_OPCO ?: 'FXE FXF FXG'
    def SEFS_LEVEL = config.SEFS_LEVEL ?: 'L1'
    def APP = config.APP ?: 'BW BE'
    def goal = config.goal ?: 'compile'
    def ManLoc = '/var/fedex/tibco/cicd_reports/reports_jenkin'
    //def mailrecp = config.mailrecp ?: ''
              def NEXUS_PATH='sefsmvn.ute.fedex.com:9999'
    //Set the Environment for execution of the process
                             withEnv([
                                           "JAVA_HOME=/opt/java/hotspot/7/current",
                                           "M2_HOME=/opt/fedex/tibco/apache-maven-3.3.9",
                                           "PATH+MAVEN=/opt/jenkins/tools/apache-maven-3.3.9/bin:${env.JAVA_HOME}/bin",
                                           "ANT_HOME=/opt/fedex/tibco/apache-ant-1.8.4",
                                           "AS_HOME=/opt/tibco/RA1.1/as/2.1",
                                           "LD_LIBRARY_PATH=/opt/tibco/RA1.1/as/2.1/lib/",
                                           "LD_LIBRARY_PATH=${env.LD_LIBRARY_PATH}:/home/f5231062/ems/8.2/lib",
                                           "MAVEN_OPTS=-Xmx2048m -Xms1024m -Dmaven.artifact.threads=10 -Dmaven.repo.local=/var/tmp/sefs/repository",
                                           "PATH=${env.JAVA_HOME}/bin:${env.M2_HOME}/bin:${env.ANT_HOME}/bin:${env.PATH}:/usr/local/bin:${env.AS_HOME}/bin:${env.AS_COMMON}/lib:home/f5231062/ems/8.2/lib:/opt/fedex/tibco/jenkins/.m2/repository/org/apache/commons/commons-lang3/3.1",
                                           "SEFS_OPCO=${SEFS_OPCO}",
                                           "SEFS_LEVEL=${SEFS_LEVEL}",
										   "APP=${APP}"
                             ]) {
                                           try {
                                                          def NEXUS_SVR = "sefsmvn.ute.fedex.com:9999"
                                                          def NEXUS_USER = "sefsdev"
                                                          def NEXUS_PASS = "sefsdev"
                                                          def NEXUS_REPO = "snapshots"
                                                          def PACKAGING = "zip"
                                                          def LOGFILE = "wget.log"
                                                          def GROUPID_SILVER = "com.fedex.sefs.common"
                                                          def ARTIFACT_SILVER = "sefs_silverControl"
                                                          def VERSION_SILVER = "5.5.0-SNAPSHOT"
                                                          def finalExit = 0
                                                          def slvrdir = "silvercontrol"
                                                          
                                 dir(WORKSPACE){
                                     //Clear the workspace - Caution - How if we use with other pipeline        
                                deleteDir()   
                                 
                                    checkout([$class: 'SubversionSCM', additionalCredentials: [], excludedCommitMessages: '', excludedRegions: '', excludedRevprop: '', excludedUsers: '', filterChangelog: false, ignoreDirPropChanges: false, includedRegions: '', locations: [[credentialsId: '9e863997-c0d9-4acb-8d54-99bd6c0b6ae1', depthOption: 'infinity', ignoreExternalsOption: true, local: '.', remote: 'https://conexus.prod.fedex.com:9443/subversion/sefs_common/jacoco/trunk/jacoco-jenkins']], workspaceUpdater: [$class: 'UpdateUpdater']])

                                     def wgetcm = "wget --content-disposition --append-output=${LOGFILE} --user=${NEXUS_USER} --password=${NEXUS_PASS} \"http://${NEXUS_SVR}/nexus/service/local/artifact/maven/redirect?r=${NEXUS_REPO}&g=${GROUPID_SILVER}&a=${ARTIFACT_SILVER}&v=${VERSION_SILVER}&p=${PACKAGING}\""
                                               
                                               sh '''
                                                          rm -rf silvercontrol
                                                          mkdir silvercontrol
                                                          cd silvercontrol
                                                          finalExit=0
                                                          ''' + wgetcm + '''
                                                          unzip -q -o *.zip'''
                                     
                                     //Loop Through the Apps
                                     def ArrList2 = APP.split(" ") as String[]
                for(int j = 0;j<ArrList2.size();j++)
                {
                                                          currapp = ArrList2[j]
                                                          
                                                          
                                         //Loop Through the OPCO's
                                         def ArrList = SEFS_OPCO.split(" ") as String[]
                    for(int i = 0;i<ArrList.size();i++){
                                                                        iter = ArrList[i]
                                                                        
                
                                                                 
                                                                  NEXUS_REPO = "snapshots"
                        GROUPID = getSCGAV(iter).groupId
                                                                  ARTIFACT = getSCGAV(iter).artifactId
                                                                  VERSION = getSCGAV(iter).version
                                                                  wgetcm = "wget --content-disposition --append-output=${LOGFILE} --user=${NEXUS_USER} --password=${NEXUS_PASS} \"http://${NEXUS_SVR}/nexus/service/local/artifact/maven/redirect?r=${NEXUS_REPO}&g=${GROUPID}&a=${ARTIFACT}&v=${VERSION}&p=${PACKAGING}\""
                        def bwcom = "./bw5cs.sh -s ${WORKSPACE}/sefs_" + iter.toLowerCase() + "_core/${iter}_${currapp}.ear.zip -l /home/tibco/ccl_fedex_bwcs5.license -t excel -o ${WORKSPACE}/sefs_" + iter.toLowerCase() + "_core/"
						def becom = "./becs.sh -s ${WORKSPACE}/sefs_" + iter.toLowerCase() + "_core/${iter}_${currapp}.ear.zip -l /home/tibco/ccl_fedex_becs5.license -t excel -o ${WORKSPACE}/sefs_" + iter.toLowerCase() + "_core/"
                        sh '''
                                                                  cd silvercontrol
                                                                  set xe
                                                                  ''' + wgetcm + '''
                                                                  unzip -q -o ''' + ARTIFACT + '''*.zip
                                                                  rm ''' + ARTIFACT + '''*.zip
                                                                  mv config config.''' + iter
                                                          
                                                                  def mvdir = WORKSPACE + "/" + slvrdir
                                                                  def opcodir = "sefs_" + iter.toLowerCase() +"_core"
                                                          
                                                                  dir(mvdir)
                                                                      {
                                                          
                                                                        def repl = WORKSPACE
                                                                        def location = WORKSPACE + "/sefs_" + iter.toLowerCase() +"_core/ears"
                                                                        sh "mkdir -p " + location
                                                               
                                                                        sh '''
                                                                        for file in `ls config.''' + iter + "/L1/*." + currapp.toLowerCase() +".*.properties config." + iter + "/L1/*broker*properties config." + iter + '''/L1/*stack*common*properties | cat | grep -v config.''' + iter + '''/L1/comp | uniq`
                                do
                                    test+=$file" "
                                done
                                python silverControl.py -k --versions `echo $test`'''
                                                                                                         
                             
                                                                                            def test = '''for f in ''' + WORKSPACE + '''/tmp/*.ear
                                                                                                        do
                                                                                                             var=`echo $f | cut -d "." -f 1 | rev | cut -d "/" -f 1 | rev` 
                                                                                                       
                                                                                                             echo ''' + "${iter},${currapp}," + '''${var}''' + ",SCA,${BUILD_NUMBER},${BUILD_URL} >> ${WORKSPACE}/${iter}_${currapp}_SCA_manifest_${BUILD_NUMBER}.txt" + '''
                                                                                                        done
                                          '''            
                                         sh test
                                         
                                          echo test
                                          
                                                                                                        def cpdir = "cp -avr " + WORKSPACE + "/tmp/*.ear" + " " + location + "/"
                                                                                                        sh cpdir
                                                                                                        
                                                                                                        def rmdir = "rm -rf " + WORKSPACE + "/tmp/*"
                                                                                                        sh rmdir
                                                                                                       
                              dir(WORKSPACE + "/" + opcodir){
                                           sh '''zip -r ''' + iter + '''_''' + currapp + '''.ear.zip ears
									    sh '''
                                       if ( "${currapp}" == 'BW' ) {
									   sh '''
									   cd /opt/fedex/tibco/bw5cs2.2.2/bin
                                        ''' + bwcom
									   } else if ( "${currapp}" == 'BE' ) {
									   sh '''
									   cd /opt/fedex/tibco/becs2.3.0/bin
									   ''' + becom
									   } else {
									   throw new Exception("undefined App value; ${currapp}. Aborting!")
									   }
									   
									    if ( "${iter}" == 'FXE' ) {
									  mailrecp='sefs_cicd@corp.ds.fedex.com,SEFS-FXE-DeploymentNotify@corp.ds.fedex.com' 
									   echo "mailrecp = ${mailrecp}"
									   } else if ( "${iter}" == 'FXF' ) {
									  mailrecp='sefs_cicd@corp.ds.fedex.com,SEFS-FXF-DeploymentNotify@corp.ds.fedex.com'
									   echo "mailrecp = ${mailrecp}"
									   }
									   else if ( "${iter}" == 'FXG' ) {
									  mailrecp='sefs_cicd@corp.ds.fedex.com,SEFS-FXG-DeploymentNotify@corp.ds.fedex.com' 
									   echo "mailrecp = ${mailrecp}"
									   }
									   
									   
                                    archiveArtifacts '**/*.xlsx'                                                                                 
                                    def OUT_HTML = "Hi All,"
                                    OUT_HTML += "<BR><BR>Please find the attachment for ${iter} ${currapp} Code Review report."
                                    OUT_HTML += "<BR><BR>Thanks,<BR>CI/CD Team<BR>"
                                    def dest = "**\\*.xlsx"
                                    def OUT_HTML_SUB  = "CICD Reports - ${SEFS_LEVEL} - ${iter.toUpperCase()} - ${currapp.toUpperCase()} Code Scanner Report - RunNumber : ${BUILD_NUMBER}"
                                    emailext attachmentsPattern: dest, body: OUT_HTML, subject: OUT_HTML_SUB, to: mailrecp
                                    
                                           
                                                              echo pwd()
                               
                                        def ArchSh = WORKSPACE + '/src/main/resources/archreports.sh -o ' + iter + " -a " + currapp.toUpperCase() + ' -key ' + BUILD_NUMBER + " -r STATIC_CODE_ANALYSIS -d " + WORKSPACE + "/sefs_"+ iter.toLowerCase() +"_core/*.xlsx "
                                        echo ArchSh
                                        try{
                                                                                      sh 'chmod 750 '+ WORKSPACE + '/src/main/resources/archreports.sh'
                                            sh returnStatus: true, script: ArchSh
                                            }
                                        catch (Exception)
                                                       {
                                                       echo "Caught: ${Exception}"
                                                       } 
                                          step([$class: 'Mailer', notifyEveryUnstableBuild: true, recipients: 'sefs_cicd@corp.ds.fedex.com', sendToIndividuals: false])                               
                                    }
									def cleanup = 'rm -rf ' + WORKSPACE + "/" + opcodir + "/*"
									sh cleanup
									def MovManFile = 'mv '+ "${WORKSPACE}/" + iter + "_" + currapp + "_SCA_manifest_${BUILD_NUMBER}.txt" + " " + ManLoc
                                               sh MovManFile
                                     }  
										
                    }
                }
                
                                                            
                                 }
                                 

                                           
                             }
                             catch (Exception) 
                             {
                                           echo "Caught: ${Exception}"
                             }
                             
              }
              
}
