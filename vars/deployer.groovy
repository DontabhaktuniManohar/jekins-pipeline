
def call(Map config=[:] ) {
  if (!fileExists('.deploy')) {
    // skip deployment
    return
  }
  if ( currentBuild.result == 'SUCCESS' || currentBuild.result == null) {
    if ("${env.LEVEL}" != "null" && ("${currentBuild.result}" == "SUCCESS" || "${currentBuild.result}" == "null") && ("${config.JOB_TYPE}" == "CICD" || "${config.JOB_TYPE}" == "CD" ) ) {
        echo "CD-STEP-LEVEL-${env.LEVEL}"
        String[] myData = "${env.LEVEL}".split("/");
        if (myData.length == 0) {
          myData = ["${env.LEVEL}"]
        }
        for (String s: myData) {
          withEnv(["LEVEL=${s}", "FD_STAGE_NAME=Staging+${s}"]) {
            deployment {}
            integrationTesting {}
            smokeTesting() {}
          }
          notification(context : 'SEFS', status : "Success")
        }
    } else {
      echo "skipping CD, no LEVEL specified or build unstable. LEVEL=" + env.LEVEL + " CurrentBuild Status=" + currentBuild.result
      if (currentBuild.result == null) {
        currentBuild.result = 'SUCCESS'
      }
    } // if
  }
}
