import com.fedex.ci.*

// vars/buildPlugin.groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()
  echo "\u2705 \u2705 \u2705 Collect Test Results \u2705 \u2705 \u2705"
  def module = "."
  try { module = '.' ?: BUILD_MODULE } catch (error_y) {}
    dir(module) {
        parallel(
          unittest : {
            try {
                echo " -> stashing unittest data"
                stash includes: '**/target/surefire-reports/*.xml', name: 'test.reports'
                echo " -> stashed unittest data"
            } catch (error) {
               echo "Junit Caught: ${error}"
            }
          },
          failsafe : {
            try {
                echo " -> stashing failsafe data"
                stash includes: '**/target/failsafe-reports/TEST-*.xml', name: 'failsafe.reports'
                echo " -> stashing failsafe data"
            } catch (error) {
               echo "FailSafe Caught: ${error}"
            }
          },
          pmd : {
            try {
                echo " -> stashing xml data"
                stash includes: '**/target/*.xml', name: 'xml.reports'
                echo " -> stashed xml data"
            } catch (error) {
               echo "Pmd Caught: ${error}"
            }
          },
          jacoco : {
            try {
                echo " -> stashing jacoco data"
                stash includes: '**/target/*.exec,**/target/**/*.class,**/src/main/java/**', name: 'jacoco.reports'
                echo " -> stashed jacoco data"
                sh 'find . -type f -name "*.exec" '
            } catch (error) {
               echo "Jacoco Caught: ${error}"
            }
          },
          sonar : {
            try {
              def jobpath = "${env.JOB_NAME}".split("/")
              def project_name = jobpath[jobpath.length-2] + "-" +jobpath[jobpath.length-2]
              def project_key = jobpath[jobpath.length-2]
              sonarScan {
                projectKey = project_key
                projectName = project_name
              }
              i++
           } catch (error) {
             echo "Sonar Caught: ${error}"
           }
         }
        ) // parallel

      } //dir
    } //call
