// vars/testCycle.groovy
def call(body) {
   // evaluate the body block, and collect configuration into the object
   def config = [:]
   body.resolveStrategy = Closure.DELEGATE_FIRST
   body.delegate = config
   body()

   def app_type = 'java'
   try { app_type = config.app_type ?: APP_TYPE
   } catch (error_x) {}

    def jobpath = "${env.JOB_NAME}".split("/")
    def SEFS_OPCO = jobpath[jobpath.length-2].take(3)
    def branch = "${env.BRANCH_NAME}".split("/")
    def VERSION = branch[branch.length-1]
    def APPTYPE = app_type.toLowerCase()
    def module = env.BUILD_MODULE ? env.BUILD_MODULE : '.'
    def t =  null
    echo "module = ${module}"
    dir(module) {
      t =  readMavenPom file: ''
    }
    def ARTIFACT_ID =  t.getArtifactId()
    def activeRlsName = ""
    def repType = ""
    def isActive = ""
    def repPath = ""

    try {
        httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: false, name: '', value: '']], outputFile: 'jsonResponse', responseHandle: 'NONE', url: 'http://irh00601.ute.fedex.com:8090/sefs-dashboard/api/public/cr/cicdBranchRls/' + SEFS_OPCO + '/' + APPTYPE + '/' + ARTIFACT_ID + '/' + VERSION, validResponseCodes: '200'
        def retstr = readJSON file: 'jsonResponse', text: ''
        for (item in retstr)
        {
            activeRlsName = (activeRlsName == "") ? item.activeRlsName : activeRlsName
            repType  = (repType == "") ? item.repType : repType
            isActive = (isActive == "") ? item.isActive : isActive
            repPath = (repPath == "") ? item.repPath : repPath
        }
       }
       catch(excep)
        {
            def excep_status = "${currentBuild}"
            if ("${excep}".contains('InterruptedException'))
            {
                currentBuild.result = 'ABORTED'
                //sayTest(failed=3)
                return
            }
        }

        if (isActive == 'N')
            {
              currentBuild.result = 'ABORTED'
              //sayTest(failed=4)
              throw new Exception("Component is inactive. Aborting the build")
            }
        try{
            if (item.statusCode == '1')
            {
                echo "Java App. error!!"
                ////sayTest(failed=item.statusCode)
                return
            }
        }catch(ex){
            }

    if ('BW' == app_type && env.RUN_TEST == 'true') {
      testBWCycle {
         nodeName = config.nodeName
         releaseName = activeRlsName
         repoType = repType
         svnPath = repPath
      }
      //sayTest()
    } else if ('BE' == app_type && env.RUN_TEST == 'true') {
      testBECycle {
         nodeName = config.nodeName
         releaseName = activeRlsName
         repoType = repType
         svnPath = repPath
      }
      //sayTest()
    } else if ('PCF-JAVA' == app_type || 'JAVA' == app_type) {
      testPCFJavaCycle {
         nodeName = config.nodeName
         releaseName = activeRlsName
         repoType = repType
         svnPath = repPath
      }
      //sayTest()

    } else if ('POM' == app_type) {
        // POM does not have tests associated.
    } else {
      testJavaCycle {
         nodeName = config.nodeName
      }
      //sayTest()
   }
}

def sayTest() {
  if (fileExists('pom.xml')) {
    // { step = 'test' }
  }
}
