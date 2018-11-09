// vars/buildCheckout.groovy
def call(boolean inlineCheckout=true) {
	echo "\u2705 \u2705 \u2705 Checkout \u2705 \u2705 \u2705"
	def title = inlineCheckout ? "Checkout" : "Job Scan"
	stage("${title}") {
		if (inlineCheckout) {
			checkout scm
		}
		try {
			if (fileExists(".build")) {
				stash includes: '.build', name: 'build.flag'
			}
			if (fileExists(".deploy")) {
				stash includes: '.deploy', name: 'deploy.flag'
			}
			if (fileExists(".ittest")) {
				stash includes: '.ittest', name: 'ittest.flag'
			}
			if (fileExists(".smoketest")) {
				stash includes: '.smoketest', name: 'smoketest.flag'
			}
			if (fileExists(".git")) {
				try {
					sh 'git remote -v > VCS_SOURCE'
				} catch (exception) {
					echo "${exception}"
				}
				stash includes: 'VCS_SOURCE', name: 'vcs.source'
			} else if (fileExists(".svn")) {
				sh 'echo "SVN" > VCS_SOURCE'
				stash includes: 'VCS_SOURCE', name: 'vcs.source'
			}
		} catch (error) {
			echo "Caught: ${error}"
		}
		stash excludes: "**/*.zip,**/*.*ar,**/target/**,**/*.class,**/node**", useDefaultExcludes: true, name: 'build.sources'
		stash includes: '**/pom.xml', useDefaultExcludes: true, name: 'build.poms'

		if (fileExists('pom.xml')) {
			//sayHello { step = 'checkout' }
		}
	}
}
