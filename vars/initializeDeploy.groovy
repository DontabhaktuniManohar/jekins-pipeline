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

	echo "\u2705 \u2705 \u2705 Initialize Siver Fabric Job Properties \u2705 \u2705 \u2705"

	def opco_choices = "FXE\nFXF\nFXG\nFXS"
	def choice_execution = "-select-\nL1\nL2"
	if (config.test_team || "${env.TEAM}" == "TEST_TEAM") {
		choice_execution = "-select-\nL1\nL2\nL3"
	} else if (config.system_team || "${env.TEAM}" == "SYSTEM_TEAM") {
		choice_execution = "-select-\nL1\nL2\nL3\nL4\nL4B\nL4C\nL5"
	}
	try {
		bRUN_DEPLOYMENT = "${RUN_DEPLOYMENT}" == "true"
	} catch (exception) {
		bRUN_DEPLOYMENT = true
	}
	try {
		bRUN_SMOKETEST = "${RUN_SMOKETEST}" == "true"
	} catch (exception) {
		bRUN_SMOKETEST = true
	}
	try {
		sSEFS_LEVEL = "${SEFS_LEVEL}"
	} catch (exception) {
		sSEFS_LEVEL = '?'
	}
	try {
		sSEFS_OPCO = "${SEFS_OPCO}"
	} catch (exception) {
		sSEFS_OPCO = '?'
	}

	def props = []
	def definitions = [
         // Data-Entry for 'What project are we deploying?'
			[$class: 'StringParameterDefinition', defaultValue: _property_(config,'context','SEFS'), description: 'Project Context Abbreviation', name: 'CONTEXT'],
         // Data-Entry for 'Are we deploying?'
			[$class: 'BooleanParameterDefinition', defaultValue: bRUN_DEPLOYMENT, description: 'Running the deployment', name : 'RUN_DEPLOYMENT'],
			[$class: 'BooleanParameterDefinition', defaultValue: bRUN_SMOKETEST, description: 'Running the smoketest', name : 'RUN_SMOKETEST'],
         // Data-Entry for 'Are we smoke testing?'
			[$class: 'StringParameterDefinition', defaultValue: '', description: 'The projects Silver Control properties, if left blank we calculate the version based on the branch name.', name: 'OVERRIDE_BRANCH_NAME'],
         // Data-Entry for 'Where are we deploying to?'
         [$class: 'ChoiceParameterDefinition', choices: "${choice_execution}", description: 'ShipmentEFS Level', name: 'SEFS_LEVEL'],
   ]
	if (config.set_opco) {
      // Data-Entry for 'What are we deploying?'
		definitions.add(
			[$class: 'ChoiceParameterDefinition', choices: "${opco_choices}", description: 'ShipmentEFS Operating Company', name: 'SEFS_OPCO']
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
   
   echo "default settings: ${sSEFS_OPCO} | ${sSEFS_LEVEL} | deploy=${bRUN_DEPLOYMENT} | smoketest= ${bRUN_SMOKETEST}"
   
	return config
}
