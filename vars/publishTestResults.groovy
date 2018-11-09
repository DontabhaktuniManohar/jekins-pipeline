// vars/publishTestResults.groovy
def call(String app_type='java') {

  if (app_type == 'POM') {
    return
  }
    // now build, based on the configuration provided
    echo "\u2705 \u2705 \u2705 Publish Test Results \u2705 \u2705 \u2705"
    def default_report_test = true
    def default_report_pmd = true
    def default_report_jacoco = true
    def default_report_findbugs = false
    def default_report_checkstyle = false
    def default_report_inttest = false
    try { default_report_test = "${env.RUN_TEST}" == 'true'    } catch (error1) {}
    try { default_report_jacoco = "${env.RUN_JACOCO}" == 'true'    } catch (error1) {}
    try { default_report_pmd = "${env.RUN_PMD}" == 'true'          } catch (error2) {}

     echo "default_report_test\t\t= ${default_report_test}\n" +
        "default_report_inttest\t= ${default_report_inttest}\n" +
        "default_report_pmd\t\t= ${default_report_pmd}\n" +
        "default_report_checkstyle\t= ${default_report_checkstyle}\n" +
        "default_report_findbugs\t= ${default_report_findbugs}\n" +
        "default_report_jacoco\t\t= ${default_report_jacoco}\n"

    parallel(
      unittest : {
        if (default_report_test) {
          try {
           echo " -> unstashing unittest report data"
           unstash name : 'test.reports'
           step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml'])
            if(currentBuild.result == 'UNSTABLE') {
             echo " -> Build UNSTABLE for unittest report data"
            }

          } catch (errory) {
             echo " Error publishing unittest report data"
             print "${errory}"
             currentBuild.result = 'FAILURE'
           }
        }
      },
      failsafe : {
        if (default_report_inttest) {
          try {
          echo " -> unstashing failsafe data"
           unstash name : 'failsafe.reports'
           step([$class: 'JUnitResultArchiver', testResults: '**/target/failsafe-reports/TEST-*.xml'])
            if(currentBuild.result == 'UNSTABLE') {
             echo " -> Build UNSTABLE for inttest report data"
            }


          } catch (errory) {
              echo " Error publishing intttest report data"
               currentBuild.result = 'UNSTABLE'
               print "${errory}"
           }
        }
      },
      pmd : {
        if (default_report_pmd) {
           try {
             echo " -> unstashing pmd data"
             unstash name : 'xml.reports'
             step([$class: 'PmdPublisher', canComputeNew: false, canRunOnFailed: true, defaultEncoding: '', healthy: '', pattern: '**/target/pmd.xml', shouldDetectModules: true, unHealthy: ''])
            if(currentBuild.result == 'UNSTABLE') {
                    echo " -> Build UNSTABLE for PMD report data"
      }

           } catch (errory) {
             echo " Error publishing PMD report data"
             currentBuild.result = 'UNSTABLE'
             print "${errory}"
           }
        }
      },
      jacoco : {
        if (default_report_jacoco) {
          try {
             echo " -> unstashing jacoco data"
             unstash name : 'jacoco.reports'
             sh 'find . -type f -name "*.exec" '

             def execpat = ''
              try{
                  def excludes = '(echo "import xml.etree.ElementTree as ET" ; echo "root = ET.parse(\'pom.xml\').getroot()" ; echo \'a = ".//{" + root.tag[root.tag.find("{")+1:root.tag.find("}")]  + "}exclude"\' ; echo \'check = []\' ; echo \'for child in root.findall("%s" % a):\' ; echo "   check.append(child.text)" ; echo "print(\',\'.join(check).strip())") | python'
                  execpat = sh returnStdout: true, script: excludes
                  echo "Jacoco Exclusion list --> " + execpat
              }
              catch(err)
              {
                  echo "--> Error while extracting Jacoco Exclusion list"
              }

             step([$class: 'JacocoPublisher', classPattern: '**/target/classes', exclusionPattern: execpat, execPattern: '**/**/**.exec', maximumBranchCoverage: '45', maximumClassCoverage: '100', maximumComplexityCoverage: '45', maximumInstructionCoverage: '45', maximumLineCoverage: '45', maximumMethodCoverage: '45'])
           if(currentBuild.result == 'UNSTABLE') {
                echo " -> Build UNSTABLE for Jacoco report data"
            }

          } catch (errory) {
            echo " Error publishing Jacoco report data"
            currentBuild.result = 'UNSTABLE'
            print "${errory}"
          }
        }
      },
      checkstyle : {
        if (default_report_checkstyle) {
          try {
             echo " -> unstashing checkstyle data"
             unstash name : 'xml.reports'
             step([$class: 'hudson.plugins.checkstyle.CheckStylePublisher', pattern: '**/target/checkstyle-result.xml', unstableTotalAll:'0'])
             if(currentBuild.result == 'UNSTABLE') {
        echo " -> Build UNSTABLE for Checkstyle report data"
      }

          } catch (errorx) {
            echo " Error publishing checkstyle report data"
            print "${errorx}"
          }
        }
      },
      findbugs : {
        if (default_report_findbugs) {
         try {
           echo " -> unstashing findbugs data"
          unstash name : 'xml.reports'
           step([$class: 'FindBugsPublisher', pattern: '**/findbugsXml.xml', unstableTotalAll:'0'])
          if(currentBuild.result == 'UNSTABLE') {
        echo " -> Build UNSTABLE for findbugs report data"
      }


         } catch (error_x) {
             echo " Error publishing findbugs report data"
           print "${error_x}"
         }
        }
      }
    )
}
