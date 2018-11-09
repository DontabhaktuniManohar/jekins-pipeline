import com.fedex.ci.*

def call(body) {
   def config = [:]
   body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

   try { unstash name : 'deploy.flag' } catch (exception) {}
    if (fileExists(".deploy")) {

       try {
          // ,deploy will contain all the default settings
          // and can be overwritten in the console.
          def props = readProperties file: '.deploy'

          def _level, _action, _install_project_info, _json, _fdurl, _fdstagename, _fdversion, _fdsvr, _fdrepo, _fdcreds, _fddeploy_worker
          try { _level = "${env.LEVEL}" == 'null' ? props['LEVEL'] : "${env.LEVEL}" } catch (exception) {}
          try { _action = "${env.FD_ACTION}" == 'null' ? props['FD_ACTION'] : "${env.FD_ACTION}" } catch (exception) {}
          try { _install_project_info =  "${env.FD_INSTALL_INFO}" == 'null' || "${env.FD_INSTALL_INFO}" == '' ? props['FD_INSTALL_INFO'] : "${env.FD_INSTALL_INFO}" } catch (exception) { }
          //
          // FD_VERSION can be defined:
          // .deploy File
          // environment file FD_VERSION
          // if all fails default to the hardcoded package version
          try { _fdversion =  "${env.FD_VERSION}" == 'null' || "${env.FD_VERSION}" == '' ? props['FD_VERSION'] : "${env.FD_VERSION}" } catch (exception) { }
          if (!fileExists('DEPLOYMENT_PACKAGE_VERSION')) {
            sh script: '''
echo $(curl -s http://sefsmvn.ute.fedex.com/fdeploy-install.sh | grep VERSION= | head -n 1 | awk -F '=' '{ print $2 }') > DEPLOYMENT_PACKAGE_VERSION
'''
          }
          _fdversion = sh returnStdout: true, script: 'cat DEPLOYMENT_PACKAGE_VERSION)'
          try { _fdurl = "${env.FD_URL}" == 'null' ? props['FD_URL'] : "${env.FD_URL}" } catch (exception) {}
          try { _fdstagename = "${env.FD_STAGE_NAME}" == 'null' ? props['FD_STAGE_NAME'] : "${env.FD_STAGE_NAME}" } catch (exception) {}
          try { _fdsvr = "${env.FD_SVR}" == 'null' ? props['FD_SVR'] : "${env.FD_SVR}" } catch (exception) {}
          try { _fdrepo = "${env.FD_REPO}" == 'null' ? props['FD_REPO'] : "${env.FD_REPO}" } catch (exception) {}
          try { _fdcreds = "${env.FD_CREDS}" == 'null' ? props['FD_CREDS'] : "${env.FD_CREDS}" } catch (exception) {}
          try { _fddeploy_worker = "${env.DEPLOY_NODE}" == 'null' ? props['DEPLOY_NODE'] : "${env.DEPLOY_NODE}" } catch (exception) {}
          _fdsvr = "${_fdsvr}" == 'null' ? 'http://sefsmvn.ute.fedex.com:9999' : "${_fdsvr}"
          _fdrepo = "${_fdrepo}" == 'null' ? 'public' : "${_fdrepo}"

          if ("${_fdcreds}" == 'null') {
            _fdcreds = 'test_deploy_user'
          }

          if ("${_fddeploy_worker}" == 'null') {
            _fddeploy_worker = '!windows'
          }

          // no level specified abort
          if ("${_level}" == 'null') {
            currentBuild.result = 'UNSTABLE'
            return
          }

          if (props.containsKey('FD_JSON')) {
             _json = props['FD_JSON']
             try { _json = "${env.FD_JSON}" == 'null' ? _json : "${env.FD_JSON}" } catch (exception) {  }
             echo "deployment:\nlevel\t\t= ${_level}\njson\t\t= ${_json}\naction\t\t= ${_action}\ninstall_info\t= ${_install_project_info}\nurl\t\t= ${_fdurl}\n" +
              "creds\t\t= ${_fdcreds}\nworker\t\t= ${_fddeploy_worker}\nnexus_svr\t= ${_fdsvr}\nfd_version\t= ${_fdversion}"
             if ( _json != null && _install_project_info != null) {
               withEnv([
                 "FD_SVR=${_fdsvr}",
                 "FD_REPO=${_fdrepo}",
                 "FD_VERSION=${_fdversion}"
                 ]) {
                  executeFDeploy {
                     fdlevel = _level
                     fdjson = _json
                     fdaction = _action
                     fdinstall = _install_project_info
                     fdurl = _fdurl
                     fdsvr = _fdsvr
                     stagename = _fdstagename
                     credentials = _fdcreds
                     deploy_worker = _fddeploy_worker
                     fd_version = _fdversion
                  }
              }
             }
             else {
                echo "\u274C\u274C\u274C\u274C No JSON or RELEASE_BRANCH defined. Skipping Deployment \u274C \u274C \u274C"
                error(" No JSON or RELEASE_BRANCH defined. Skipping Deployment")
             }
          } else {
            echo "\u274C\u274C\u274C\u274C No DEPLOYMENT METHOD DEFINED - FD_JSON. \u274C \u274C \u274C"
            error("No DEPLOYMENT METHOD DEFINED")
          }
       } catch (exception) {
             print ">>>> ${exception}"
       }
    } //if

}
