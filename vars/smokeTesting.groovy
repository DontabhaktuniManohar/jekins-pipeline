// vars/smokeTesting.groovy
def call(body) {
	// evaluate the body block, and collect configuration into the object
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()


	node('!windows') {
		try {
			unstash name : 'smoketest.flag'
			if (fileExists(".smoketest")) {
				// now build, based on the configuration provided
				// ,deploy will contain all the default settings
				// and can be overwritten in the console.
				echo "\u2705 \u2705 \u2705 Smoke Testing \u2705 \u2705 \u2705"
				def props = readProperties file: '.smoketest'
				echo "${props}"
				if (props['smoketest.library.repository'] != null) {
					 stage("Smoke Testing") {
						 library identifier: props['smoketest.library.identifier'], retriever: modernSCM(
		  			 		[$class: 'GitSCMSource', remote: props['smoketest.library.repository'], credentialsId: props['smoketest.library.creds']])
							smokeTestProxy(props)
					 }
				 }
			 }
		 } catch (exception) {
			 echo "${exception}"
		 }
	}
}
