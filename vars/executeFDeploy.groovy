import com.fedex.ci.*
import com.fedex.jenkins.*

// vars/executeFDeploy.groovy
def call(body) {
   // evaluate the body block, and collect configuration into the object
   def config = [:]
   body.resolveStrategy = Closure.DELEGATE_FIRST
   body.delegate = config
   body()





   def fd_project_info = []
   def fd_project_id = 'default'
   def fd_release_tag = 'master'
   def pcf_user = ''
   def pcf_password = ''
   def fd_url = config.fdurl ?: 'http://sefsmvn.ute.fedex.com/install.py'
   def fd_project_url = config.fdinstall
   def fd_stage_name = config.stagename ?: "Deploying to ${config.fdlevel}"
   def fd_credentials = config.credentials ?: "test_deploy_user"
   def fd_deploy_worker = config.deploy_worker ?: "!windows"
   def fd_svr = config.fdsvr ?: "https://nexus.prod.cloud.fedex.com:8443"
   def fd_version = config.fd_version ?:  Definitions.DEPLOYMENT_PACKAGE_VERSION

   if (config.fdinstall != null && config.fdinstall.contains('.git')) {
    if (config.fdinstall.contains('->')) {
      fd_project_url = config.fdinstall.split('->')[0]
      fd_release_tag = config.fdinstall.split('->')[1]
    }
    else {
      fd_project_url = config.fdinstall
    }
    __fd_project_url = fd_project_url.split('/')
    fd_project_id = __fd_project_url[__fd_project_url.length-1]
    fd_project_id = fd_project_id.replaceAll('.git','')
    // use a different default when dealing with git.
    fd_credentials = config.credentials ?: 'gitlab-user-for-master'
   }
   else {
     if (config.fdinstall == null) {
       error("FD_INSTALL_INFO format null was detected 'projectid:groupid:artifactid:version' or git repository reference")
     }
     try { fd_project_info = config.fdinstall.trim().split(":") } catch (error_x) {}
     if (fd_project_info.length != 4 || config.fdinstall.contains(".git")) {
       error("FD_INSTALL_INFO format ${fd_project_info} 'projectid:groupid:artifactid:version' or git repository reference")
     }
     fd_release_tag = fd_project_info[3]
     fd_project_id = fd_project_info[0]
   }

   if ("${fd_version}" == "null") {
     if (!fileExists('DEPLOYMENT_PACKAGE_VERSION')) {
       sh script: '''
echo $(curl -s http://sefsmvn.ute.fedex.com/fdeploy-install.sh | grep VERSION= | head -n 1 | awk -F '=' '{ print $2 }') > DEPLOYMENT_PACKAGE_VERSION
'''
     }
     _fdversion = sh returnStdout: true, script: 'cat DEPLOYMENT_PACKAGE_VERSION)'
   }

   node ('python2.7') {
     try {


         if (env.PAMID)
            {
                def appStract = new com.fedex.jenkins.APPIDextractor();
        	    def k = new com.fedex.jenkins.PamConnection();
        	    pcf_user = env.PCF_USER;
                pcf_password = k.pcfDeploy(env.PAMID)
           }

       echo "executeFDeploy(config)=${config} on ${fd_deploy_worker}"
       // if no branch name was passed calculate it from the release_name

       echo " RN=${fd_project_info} | rn=${fd_release_tag} | ${fd_url} "
       echo ""+
        "fd_project_id\t\t= ${fd_project_id}\n"+
        "fd_project_url\t\t= ${fd_project_url}\n"+
        "fd_install_info\t= ${config.fdinstall}\n"+
        "fd_release_tag\t\t= ${fd_release_tag}\n" +
        "fd_level\t\t= ${config.fdlevel}\n"+
        "fd_json\t\t= ${config.fdjson}\n" +
        "fd_credentials\t\t= ${config.credentials}\n" +
        "fd_url\t\t\t= ${fd_url}\n" +
        "fd_action\t\t= ${config.fdaction}\n" +
        "fd_deploy_version\t= ${fd_version}\n"
       echo "\u2705 \u2705 \u2705 DEPLOYING \u2705 \u2705 \u2705"
       def status = 0
       withEnv([
          "FD_URL=${fd_url}",
          "FD_VERSION=${fd_version}",
          "FD_PROJECT_ID=${fd_project_id}",
          "FD_PROJECT_INFO=${fd_project_info}",
          "FD_PROJECT_URL=${fd_project_url}",
          "FD_SVR=${fd_svr}",
          "FD_JSON=${config.fdjson}",
          "FD_ACTION=${config.fdaction}",
          "PATH+CF_CLI_PATH=${env.CF_CLI_PATH}",
          "PROJECT_VERSION=${fd_version}",
          "FD_CREDS=${fd_credentials}",
          "PCF_USER=${pcf_user}",
          "PCF_PASSWORD=${pcf_password}"

       ]) {
        // install fdeploy engine
        status = sh returnStatus: true, script: '''#!/bin/bash
          find . -type f -name '*.zip' -exec rm {} \\;
          #find `pwd`/config* -type f 2> /dev/null
          curl -L -o install.py "${FD_URL}"
          unset FD_SVR
          python install.py -v "${FD_VERSION}"
  '''
        if ("${fd_project_url}".contains(".git")) {
          sshagent(['gitlab-user-for-master']) {
            // checkout the deployment items
            echo "checkout ${fd_release_tag}@${fd_project_url} to config.${fd_project_id}"
            checkout([$class: 'GitSCM',
              branches: [[name: "*/${fd_release_tag}"]],
              doGenerateSubmoduleConfigurations: false,
              poll: false,
              extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "config.${fd_project_id}"]],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: 'gitlab-user-for-master', url: "${fd_project_url}"]]]
            )
          }
        } else {
          // if we have a INSTALL INFO Nexus Style
          status = sh returnStatus: true, script: '''#!/bin/bash -l
find . -type f -name '*.zip' -exec rm {} \\;
set | grep FD_
export FD_SVR
export FD_REPO
set -xv
curl -L -o install.py "${FD_URL}"
FD_REPO=${FD_REPO} FD_SVR=${FD_SVR} python install.py -n "${FD_INSTALL_INFO}"
set +xv
finalExit=0
find `pwd`/config* -type f 2> /dev/null
echo "installed fdeploy version:"
python fdeploy.py --version
'''
      echo "status of nexus fdeploy install = ${status}"
        }
        // install fdeploy engine
        def selectLevel = "${config.fdlevel}".split(",");
        if (selectLevel.length == 0) {
           selectLevel = ["${config.fdlevel}"]
        }
        def credentials = [file(credentialsId: "${env.FD_CREDS}", variable: 'DEPLOY_USER_KEY')]
        withCredentials(credentials) {

            echo "levelArr=${selectLevel}"
            for (int i=0;i<selectLevel.length;i++) {
              stage("Level\n" + selectLevel[i]) {
                withEnv([        "FD_LEVEL=${selectLevel[i]}"]) {

                      status = sh returnStatus: true, script: '''#!/bin/bash
IFS='-' read -ra COMMAND <<< "${FD_ACTION}"
_section="${COMMAND[0]}"
_action="${COMMAND[1]}"
set -xv
RC=""
[[ -e "config.${FD_PROJECT_ID}/.fdeployrc" ]] && export RC="-rc config.${FD_PROJECT_ID}/.fdeployrc"
python fdeploy.py --version
python fdeploy.py --analytics ${RC} -i ${DEPLOY_USER_KEY} ${LOGGING} "${_section}" -a "${_action}" -f ./config.${FD_PROJECT_ID}/${FD_JSON} -l "${FD_LEVEL}"
finalExit=$(($finalExit + $?))
set +xv
exit ${finalExit}
            '''
          }
                if (status != 0) {
                   error("\u274C\u274C\u274C\u274C FDeploy Deployment Descriptor JSON load failed for ${config.fdinstall} (errcd=${status}).\u274C\u274C\u274C\u274C")
                }
              } // stage

            } // selectLevel
          } // with credentis
      }
    } catch (Exception e) {
      echo "${e}"
      error(e.getMessage())
    }
  } // with node
}
