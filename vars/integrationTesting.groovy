// vars/integrationTesting.groovy
def call(body) {
	// evaluate the body block, and collect configuration into the object
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()


	node {
		try {
			unstash name : 'ittest.flag'
			if (fileExists(".ittest")) {
				echo "\u2705 \u2705 \u2705 Integration Testing \u2705 \u2705 \u2705"
				 // ,deploy will contain all the default settings
				 // and can be overwritten in the console.
				 // now build, based on the configuration provided
				def props = readProperties file: '.ittest'
				echo "${props}"
				if (props['ittest.library.repository'] != null) {
					stage("IT Testing") {
						library identifier: props['ittest.library.identifier'], retriever: modernSCM(
 	  			 		[$class: 'GitSCMSource', remote: props['ittest.library.repository'], credentialsId: props['ittest.library.creds']])
 						itTestProxy(props)
					}
				}
		 }
		} catch (exception) {
			echo "${exception}"
		}
	}
}
