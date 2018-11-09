@NonCPS
def readProperties(atext,prop) {
   atext.split("\\r?\\n").each { param ->
      s=param.split('=')
      prop["${s[0]}"]="${s[1]}"
      //echo "Param: ${s[0]} ${s[1]}"
   }
   return prop
}

@NonCPS
def _property_(prop,key,_def_) {
   if (_def_) {
      if (prop[key]) {
         return prop[key]
      }
      else {
         return _def_
      }
   }
   return prop[key]
}
@NonCPS
def _property_bool_(prop,key,_def_) {
   def retValue = false
   if (_def_) {
      if (prop[key]) {
         return prop[key] == 'true'
      }
      else {
         return _def_
      }
   }
}

// vars/initialize.groovy
def call(body) {
   // evaluate the body block, and collect configuration into the object
   def config = [:]
   body.resolveStrategy = Closure.DELEGATE_FIRST
   body.delegate = config
   body()

   echo "\u2705 \u2705 \u2705 Initialize Job Properties \u2705 \u2705 \u2705"

   def default_email = 'no-reply@jenkins-shipment.web.fedex.com'
   def default_failure_email = 'no-reply@jenkins-shipment.web.fedex.com'
   def default_schedule = 'H/10 * * * *'
   def default_build_options = ''
   def default_goal = 'clean install'
   def default_app_type = 'java'
   def default_settings_xml = 'http://sefsmvn.ute.fedex.com/settings.xml'
   def project_token = 'a6eac20fae98e54b1a37c81d09276782'


   try { default_failure_email = "${env.BUILD_FAILURE_RECEPIENTS}" == 'null' ? 'no-reply@jenkins-shipment.web.fedex.com' : "${env.BUILD_FAILURE_RECEPIENTS}" } catch (exception) {}
   try { default_email = "${env.BUILD_RECEPIENTS}" == 'null' ? 'no-reply@jenkins-shipment.web.fedex.com' : "${env.BUILD_RECEPIENTS}" } catch (exception) {}
   try { default_goal = "${env.BUILD_GOAL}" == 'null' ? 'clean deploy' : "${env.BUILD_GOAL}" } catch (exception) {}
   try { default_settings_xml ="${env.BUILD_SETTINGS_XML}" == 'null' ? 'http://sefsmvn.ute.fedex.com/settings.xml' : "${env.BUILD_SETTINGS_XML}"  } catch (exception) { echo "${exception}: ${default_settings_xml}" }
   try { default_schedule = "${env.BUILD_SCHEDULE}" == 'null' ? 'H/10 * * * *' : "${env.BUILD_SCHEDULE}"   } catch (exception) { echo "${exception}: ${default_schedule}" }
   try { default_build_options = "${env.BUILD_OPTIONS}" == 'null' ? '' : "${env.BUILD_OPTIONS}" } catch (error1) {}
   try { default_concurrency = "${env.BUILD_PARALLEL}" == 'true'    } catch (error1) {}
   try { default_run_verbose = "${env.RUN_VERBOSE}" == 'true'    } catch (error1) {}
   try { default_run_test = "${env.RUN_TEST}" == 'true'    } catch (error1) {}
   try { default_run_static = "${env.RUN_STATIC}" == 'true'    } catch (error1) {}
   try { default_run_jacoco = "${env.RUN_JACOCO}" == 'true'    } catch (error1) {}
   try { default_run_pmd = "${env.RUN_PMD}" == 'true'          } catch (error2) {}
   try { default_run_findbugs = "${env.RUN_FINDBUGS}" == 'true'} catch (error3) {}
   try { default_run_checkstyle = "${env.RUN_CHECKSTYLE}" == 'true'} catch (error4) {}
   try { default_run_inttest = "${env.RUN_INTTEST}" == 'false'} catch (error5) {}
   try { default_run_sonar = "${env.RUN_SONAR}" == 'true'} catch (error6) {}
   try { default_app_type = "${env.APP_TYPE}" == 'null' ? 'java' : "${env.APP_TYPE}" } catch (error6) {}
   try { repo_type = "${env.GIT_URL}" == 'null' && "${env.GIT_URL}".contains("conexus") ? 'conexus' : "gitlab" } catch (error6) {}

   echo "default_app_type=\t\t = ${default_app_type}\n" +
        "default_build_options\t\t= ${default_build_options}\n" +
        "default_concurrency\t\t= ${default_concurrency}\n" +
        "default_settings_xml\t\t= ${default_settings_xml}\n" +
        "default_schedule\t\t= ${default_schedule}\n" +
        "default_email\t\t\t= ${default_email}\n" +
        "default_failure_email\t\t= ${default_failure_email}\n" +
        "default_goal\t\t\t= ${default_goal}\n" +
        "default_run_verbose\t\t= ${default_run_verbose}\n" +
        "default_run_test\t\t= ${default_run_test}\n" +
		"default_run_static\t\t= ${default_run_static}\n" +
        "default_run_jacoco\t\t= ${default_run_jacoco}\n" +
        "default_run_pmd\t\t\t= ${default_run_pmd}\n" +
        "default_run_findbugs\t\t= ${default_run_findbugs}\n" +
        "default_run_checkstyle\t\t= ${default_run_checkstyle}\n" +
        "default_run_sonar\t\t= ${default_run_sonar}\n" +
        "default_run_inttest\t\t= ${default_run_inttest}\n"
   def props = []

   props.add(
         [
          $class: 'ParametersDefinitionProperty', parameterDefinitions: [
            [$class: 'ChoiceParameterDefinition', defaultValue: 'CICD', choices: "CICD\nCI\nCD", description: 'Type of job', name: 'JOB_TYPE'],
            [$class: 'StringParameterDefinition', defaultValue:  _property_(config,'build_failure_recepients',default_failure_email), description: 'Build Failure Email Recepients', name: 'BUILD_FAILURE_RECEPIENTS'],
            [$class: 'StringParameterDefinition', defaultValue: _property_(config,'build_recepients',default_email), description: 'All build status email Recepients', name: 'BUILD_RECEPIENTS'],
            //[$class: 'StringParameterDefinition', defaultValue: _property_(config,'url',default_settings_xml), description: 'Build Settings XML', name : 'BUILD_SETTINGS_XML'],
            //[$class: 'StringParameterDefinition', defaultValue: default_build_options, description: 'Build Options -D', name : 'BUILD_OPTIONS'],
            [$class: 'StringParameterDefinition', defaultValue: _property_(config,'app_type',default_app_type), description: 'Application Type for testing', name : 'APP_TYPE'],
            //[$class: 'StringParameterDefinition', defaultValue: _property_(config,'goal',default_goal), description: 'Build Goal', name : 'BUILD_GOAL'],
            //[$class: 'StringParameterDefinition', defaultValue: _property_(config,'cron',default_schedule), description: 'Polling Schedule', name : 'BUILD_SCHEDULE'],
            //[$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'concurrency',default_concurrency), description: 'Build Execution in multi-thread/parallel mode (only works on thread-safe plugins)', name : 'BUILD_PARALLEL'],
            [$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_verbose',default_run_verbose), description: 'Run Commands Verbose', name : 'RUN_VERBOSE'],
            //[$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_test',default_run_test), description: 'Run Tests reports', name : 'RUN_TEST'],
            [$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_inttest',default_run_inttest), description: 'Run IT failsafe Integration Tests reports', name : 'RUN_INTTEST'],
            //[$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_static',default_run_static), description: 'Run Static Code reports', name : 'RUN_STATIC'],
			//[$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_jacoco',default_run_jacoco), description: 'Run JaCoCo reports', name : 'RUN_JACOCO'],
            [$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_pmd',default_run_pmd), description: 'Run PMD reports', name : 'RUN_PMD'],
            [$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_findbugs',default_run_findbugs), description: 'Run FindBugs reports', name : 'RUN_FINDBUGS'],
            [$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_checkstyle',default_run_checkstyle), description: 'Run CheckStyle reports', name : 'RUN_CHECKSTYLE'],
            //[$class: 'BooleanParameterDefinition', defaultValue: _property_bool_(config,'run_sonar',default_run_sonar), description: 'Run Sonar reports (not implemented yet)', name : 'RUN_SONAR'],
            [$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Perform release cut (1.0.0-SNAPSHOT to 1.0.0)', name : 'PERFORM_RELEASE_CUT'],
            [$class: 'ChoiceParameterDefinition', defaultValue: 'L1', choices: "L1\nL2\nL3", description: 'Deployment LEVEL', name: 'LEVEL']
            //[$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Perform incremental versioning from (1.0.0-SNAPSHOT to 1.1.0-SNAPSHOT) mostly done after a sprint completion or demo', name : 'INCREMENTAL_VERSIONING'],
            //[$class: 'StringParameterDefinition', defaultValue: '', description: 'Placeholder for explicit release tag for manual assignment', name: 'RELEASE_TAG'],
            //[$class: 'BooleanParameterDefinition', defaultValue: false, description: 'Electronic Compliance Check for Release Cuts', name: 'COMPLIANCE_CHECK']
         ]
      ]
   )

   props.add(
      buildDiscarder(
         logRotator(artifactDaysToKeepStr: '9', artifactNumToKeepStr: '10', daysToKeepStr: '9', numToKeepStr: '10')
      )
   )
   props.add(
       [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]
    )

    if (repo_type == "gitlab") {
        props.add(
           gitLabConnection('https://gitlab.prod.fedex.com')
           )
        props.add(
           pipelineTriggers([
            [
                 $class: 'GitLabPushTrigger',
                 branchFilterType: 'All',
                 triggerOnPush: true,
                 triggerOnMergeRequest: true,
                 triggerOpenMergeRequestOnPush: "never",
                 triggerOnNoteRequest: true,
                 noteRegex: "Jenkins please retry a build",
                 skipWorkInProgressMergeRequest: true,
                 ciSkip: false,
                 setBuildDescription: true,
                 addNoteOnMergeRequest: true,
                 addCiMessage: true,
                 addVoteOnMergeRequest: true,
                 acceptMergeRequestOnSuccess: false,
                 branchFilterType: "NameBasedFilter",
                 includeBranchesSpec: "release/qat",
                 excludeBranchesSpec: "",
             ]
              ])
        )
    } else {
        //conexus
         props.add(
                    pipelineTriggers([
                          triggers: [
                             [
                                $class: 'hudson.triggers.SCMTrigger',
                                scmpoll_spec : default_schedule
                             ]
                          ]
                       ])
                 )
    }


   def concurrency = config.concurrency ?: default_concurrency

   if (concurrency == false) {
      props.add(
         disableConcurrentBuilds()
      )
   }
   // install the properties
   properties( props  )
   return config
}
