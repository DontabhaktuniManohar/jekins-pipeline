import com.fedex.ci.*

// vars/buildPlugin.groovy
def call(Map config = [:]) {

    // EXTERNAL ENVIRONMENT VARIABLES THAT CAN BE SELECTED AND OVERWRITTEN
    def perform_release_cut = config.perform_release ?: false
    def incremental_versioning = false
    def verbose = false
    def release_tag = ''
    def extra_build_options = ''
    def module = '.'
    try { perform_release_cut = config.perform_release_cut ?: PERFORM_RELEASE_CUT } catch (error_x) {}
    try { incremental_versioning = config.incremental_versioning ?: INCREMENTAL_VERSIONING } catch (error_x) {}
    try { verbose = config.verbose  ?: RUN_VERBOSE } catch (error_x) { verbose = false }
    try { release_tag = config.release_tag ?: RELEASE_TAG } catch (error_x) {}
    try { extra_build_options = '' ?: BUILD_OPTIONS } catch (error_x) {}
    try { module = config.module ?: BUILD_MODULE } catch (error_y) {}

    // STEP VARIABLES
    def stageName = config.stageName ?: 'Build'
    def nodeName = config.nodeName ?: ''
    def resumeFrom = config.resumeFrom ?: ''
    def goal = config.goal ?: 'clean compile'
    def operationMode = config.operationMode ?: '-fae'
    def headless = config.headless ?: false
    def parallel = config.parallel ?: false
    def c_verbose = verbose == true ? '-X' : ''
    //def tibco_home = buildenv.getTibcoHome(nodeName)
    // command line assignments
    def skipTests = "-Dmaven.test.skip=${config.fortify}"
    def java_home = tool name : 'JAVA_8'
    def m2_home = tool name : Definitions.APACHE_MAVEN_VERSION
    def build_goals = "clean org.jacoco:jacoco-maven-plugin:0.8.1:prepare-agent pmd:pmd -Daggregate=true -Dpmd.typeResolution=false install org.jacoco:jacoco-maven-plugin:0.8.1:report"
    def XRUN = ''
    if (headless || Definitions.isHeadless(nodeName) && "${APP_TYPE}" == "BW" || "${APP_TYPE}" == "BE" ) {
        XRUN = '/usr/bin/xvfb-run -a -f ${HOME}/.Xauthority'
        headless = true
    }
    def threading = ''
    if (parallel) {
        threading = '-T 1C'
    }
    echo "\u2705 \u2705 \u2705 ${stageName} \u2705 \u2705 \u2705"
    echo "currentBuild=${currentBuild.result}"

    dir(module) {
      def a = fileExists('BUILT')
      def b = fileExists('RELEASED')
      def c = fileExists('RELEASE_CUT')
      def d = fileExists('UPLOADED')
      echo "config\t\t\t\t= ${config}\n" +
              "module\t\t\t\t= ${module}\n" +
              "extra_build_options\t\t= ${extra_build_options}\n" +
              "verbose\t\t\t\t= ${verbose}\n" +
              "java_home=\t\t\t\t= ${java_home}\n" +
              "m2_home\t\t\t\t= ${m2_home}\n"  +
              "perform_release_cut\t\t= ${perform_release_cut}\n" +
              "BUILT\t\t\t\t= ${a}\n" +
              "RELEASED\t\t\t\t= ${b}\n" +
              "QA_PASSED\t\t\t\t= ${c}\n" +
              "UPLOADED\t\t\t\t= ${d}\n"
        def fdeploy_home = tool name: 'fdeploy', type: 'maven'
        unstash "build.prepare"

        if (!fileExists('pom.xml')) {
          return
        }
        // check if this build has already been RELEASED
        // if so we just skip the release step.
        def buildSuccess = 1
        if (fileExists('BUILT') == false) {
            stage('Build') {
              def next_version = calculate_next_version()
              if ("${perform_release_cut}" == "true") {
                  echo "\u2705 \u2705 \u2705 PERFORMING RELEASE CUT"
                  // LIBRARIES GET LOADED in the PREPARE STEP
                  echo "cutting release for version ${next_version}"
                  our_build("${extra_build_options}","versions:set -DnewVersion=${next_version}", XRUN)
                  stash excludes: "**/*.zip,**/*.*ar,**/target/**,**/*.class,**/node**", useDefaultExcludes: true, name: 'build.sources'
              		stash includes: '**/pom.xml', useDefaultExcludes: true, name: 'build.poms'
              }
              // the first build based on the supplied goal,
              our_build("${extra_build_options} ${skipTests}","${build_goals}", XRUN)
              stash excludes: "**/*.zip,**/*.*ar,**/target/**,**/*.class,**/node**", useDefaultExcludes: true, name: 'build.sources'
          		stash includes: '**/pom.xml', useDefaultExcludes: true, name: 'build.poms'
              touch file: 'BUILT', timestamp: 0
              if ("${perform_release_cut}" == "true") {
                touch file: 'RELEASE_CUT', timestamp: 0
              }
            }
        } else {
            //unstash name: 'build.sources'
            // already passed BUILD and TEST phase
            if (fileExists('DEPLOYED_TO_NEXUS') == false ) {
              if (fileExists('RELEASE_CUT')) {
                stage("Tagging") {
                    // 4. TAG VERSION in source repository
                    echo "\u2705 \u2705 \u2705 PERFORMING TAGGING \u2705 \u2705 \u2705"
                    tagPlugin(module)
                }
              }
              else {
                stage('Tagging') {                  // overwrite the goal if user provided one
                  echo "skip tagging"
                }
              }
              our_deploy("${extra_build_options} ${skipTests} -DskipTests=true -DdeployAtEnd=false", XRUN)
              touch file: 'DEPLOYED_TO_NEXUS', timestamp: 0
            }
            else {
              echo "already in status DEPLOYED_TO_NEXUS"
            }
            touch file: 'RELEASED', timestamp: 0
            } // tagging stage
        try {
            stash includes: '**/target/classes/**', name: 'build.output'
            //stash includes: '**/target/*.zip,**/target/*.*ar', name: 'build.archive'
        } catch (error) {
            echo "Caughty: ${error}"
        }
    } //dir
}

def our_deploy(extra_build_options,xrun) {
    unstash  name : 'build.prepare'
    unstash  name : 'archives'
    our_execute(Definitions.EXCLUDE_ALL_BUT_DEPLOY ,'deploy',xrun)
}
def our_build(extra_build_options, goal,xrun) {
    our_execute( extra_build_options, goal,xrun)

}
def our_execute(extra_build_options, goal,xrun) {
    sh 'echo "Running on $(hostname)"'
    def javahome = tool name: 'JAVA_8', type: 'jdk'
    def m2home = tool name : Definitions.APACHE_MAVEN_VERSION
    withEnv(["JAVA_HOME=${javahome}", "M2_HOME=${m2home}", "PATH+MAVEN=${m2home}/bin:${javahome}/bin",
            "MAVEN_OPTS=-Xmx2048m -Xms2048m -Djava.security.egd=file:/dev/urandom -Dmaven.artifact.threads=10 ${extra_build_options}",
            "https_proxy=https://internet.proxy.fedex.com:3128",
            "http_proxy=https://internet.proxy.fedex.com:3128"
    ]) {
        def cmd = "set -xv;set | grep HOME; set | grep NODE; set | grep PATH ;${xrun} ${m2home}/bin/mvn -U -B -V -e -s ./settings.xml ${extra_build_options} ${goal}"
        def buildSuccess = sh returnStatus: true, script: cmd
        if (buildSuccess != 0) {
            throw new Exception("BUILD STEP: Error while uploading to Nexus (${buildSuccess})")
        }
    }
}
