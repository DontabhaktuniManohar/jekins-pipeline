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

// vars/initialize.groovy
def call(body) {
	// evaluate the body block, and collect configuration into the object
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	echo "\u2705 \u2705 \u2705 Initialize Deployment Job Properties \u2705 \u2705 \u2705"

	def default_email = 'no-reply@jenkins.prod.fedex.com'
	def default_failure_email = 'no-reply@jenkins.prod.fedex.com'
	def opco_choices = "FXE\nFXF\nFXG\nFXS"
	def choice_execution = "-select-\nL0\nL1\nL2"
	if (config.test_team || "${env.TEAM}" == "TEST_TEAM") {
		choice_execution = "-select-\nL0\nL1\nL2\nL3"
	} else if (config.system_team || "${env.TEAM}" == "SYSTEM_TEAM") {
		choice_execution = "-select-\nL0\nL1\nL2\nL3\nL4\nL5\nPROD"
	} else if (config.system_team || "${env.TEAM}" == "RELY") {
		choice_execution = "-select-\nL4\nL5\nL6\nPROD"
	}
	try { default_failure_email = "${env.BUILD_FAILURE_RECEPIENTS}" == 'null' ? 'no-reply@jenkins.prod.fedex.com' : "${env.BUILD_FAILURE_RECEPIENTS}" } catch (exception) {}
	try { default_email = "${env.BUILD_RECEPIENTS}" == 'null' ? 'no-reply@jenkins.prod.fedex.com' : "${env.BUILD_RECEPIENTS}" } catch (exception) {}
	try { bRUN_DEPLOYMENT = "${RUN_DEPLOYMENT}" == "true" } catch (exception) { bRUN_DEPLOYMENT = true }
	try { bRUN_SMOKETEST = "${RUN_SMOKETEST}" == "true" } catch (exception) { bRUN_SMOKETEST = true }
	try { sLEVEL = "${LEVEL}" } catch (exception) { sLEVEL = '?' }
	try { sOPCO = "${OPCO}" } catch (exception) { sOPCO = '?' }
  try { sFD_JSON = "${FD_JSON}" } catch (exception) { sFD_JSON = '?' }
	try { sFD_CREDS = "${FD_CREDS}" } catch (exception) { sFD_CREDS = 'test_deploy_user' }

	def props = []
	def definitions = [
         // Data-Entry for 'What project are we deploying?'
			[$class: 'StringParameterDefinition', defaultValue:  default_failure_email, description: 'Build Failure Email Recepients', name: 'BUILD_FAILURE_RECEPIENTS'],
	 		[$class: 'StringParameterDefinition', defaultValue: default_email, description: 'All build status email Recepients', name: 'BUILD_RECEPIENTS'],
			[$class: 'StringParameterDefinition', defaultValue: 'FXG', description: 'Project Context Abbreviation', name: 'CONTEXT'],
         // Data-Entry for 'What project are we deploying?'
			[$class: 'StringParameterDefinition', defaultValue: sFD_JSON, description: 'Fdeploy Deployment Descriptor json filename', name: 'FD_JSON'],
         // Data-Entry for 'Are we deploying?'
			[$class: 'BooleanParameterDefinition', defaultValue: bRUN_DEPLOYMENT, description: 'Running the deployment', name : 'RUN_DEPLOYMENT'],
			[$class: 'BooleanParameterDefinition', defaultValue: bRUN_SMOKETEST, description: 'Running the smoketest', name : 'RUN_SMOKETEST'],
         // Data-Entry for 'Are we smoke testing?'
			[$class: 'StringParameterDefinition', defaultValue: '', description: 'The projects Silver Control properties, if left blank we calculate the version based on the branch name.', name: 'OVERRIDE_BRANCH_NAME'],
			// Data-Entry for 'Are we smoke testing?'
	 		[$class: 'StringParameterDefinition', defaultValue: sFD_CREDS, description: 'The credentials id for deployment.', name: 'FD_CREDS'],
         // Data-Entry for 'Where are we deploying to?'
      [$class: 'ChoiceParameterDefinition', choices: "${choice_execution}", description: 'Level', name: 'LEVEL']
   ]
	if (config.set_opco) {
      // Data-Entry for 'What are we deploying?'
		definitions.add(
			[$class: 'ChoiceParameterDefinition', choices: "${opco_choices}", description: 'Operating Company', name: 'OPCO']
		)
	}
	if (config.set_schedule) {
      // Data-Entry for 'When do we want to deploy? (used for AUTODEPLOY)'
		definitions.add([$class: 'StringParameterDefinition', defaultValue: 'H 3 * * *', description: 'The version of the Silver Control Properties package', name: 'DEPLOY_SCHEDULE'])
	}
	props.add(
			[
			 $class: 'ParametersDefinitionProperty', parameterDefinitions: definitions
		]
	)
   props.add(
      buildDiscarder(
         logRotator(artifactDaysToKeepStr: '9', artifactNumToKeepStr: '10', daysToKeepStr: '9', numToKeepStr: '10')
      )
   )
  def concurrency = config.concurrency ?: false
	if (concurrency == false) {
		props.add(
			disableConcurrentBuilds()
		)
	}
	def cron = config.cron
	if (cron) {
		props.add(
			pipelineTriggers([
					triggers: [
						[
							$class: 'hudson.triggers.SCMTrigger',
							scmpoll_spec : cron
						]
					]
				])
		)
	}

	// install the properties
	properties( props  )

   echo "default settings: ${sOPCO} | ${sLEVEL} | ${sFD_JSON} | deploy=${bRUN_DEPLOYMENT} | smoketest= ${bRUN_SMOKETEST}"

	return config
}
