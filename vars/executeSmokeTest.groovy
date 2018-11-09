import com.fedex.ci.*

//@NonCPS
//def getTestSuite(opco,level) {
//	xop=opco
//	if ( xop == 'FXE' ) {
//		xop=''
//	}
//	return "test/${level}/${xop}SmokeTest/Tests/Suites/${level}Suite.ste"
//}

def call(body) {
	// evaluate the body block, and collect configuration into the object
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	def sefs_level = config.level ?: 'L1'
	def sefs_opco = config.opco ?: 'FXS'
	def suite_url = Definitions.getTestSuite(sefs_opco,sefs_level) ?: "NONE"

	//node ('ShipmentEFS') {
		echo "\u2705 \u2705 \u2705 SMOKE TESTING \u2705 \u2705 \u2705"
		withCredentials([usernamePassword(credentialsId: 'ca-dev-tst',
			passwordVariable: 'CA_DEV_TEST_PASSWORD', usernameVariable: 'CA_DEV_TEST_USER')]) {
			withEnv([
				"TEST_SUITE_TO_RUN=${suite_url}"
			]) {
				echo "Executing Test Suite: ${suite_url}"
				stage("Execute Smoke Test") {
					sh '''#!/bin/bash -l
#!/bin/bash
set +xe

export CADEVTEST_HOME=/opt/fedex/tibco/CADevTest
export TIBCO_HOME=/opt/tibco/RA1.1
export AS_HOME=${TIBCO_HOME}/as/2.1

export JAVA_HOME=/opt/java/hotspot/7/current
export JAVA_VENDOR=Oracle
export M2_HOME=/opt/fedex/tibco/apache-maven-3.3.9

export PATH=${CADEVTEST_HOME}/lib:${CADEVTEST_HOME}/bin:${AS_HOME}/lib:${AS_HOME}/bin:${JAVA_HOME}/bin:${M2_HOME}/bin:${PATH}:/usr/local/bin
echo PATH=${PATH}

export LD_LIBRARY_PATH=${CADEVTEST_HOME}/lib:${AS_HOME}/lib:${AS_HOME}/bin
echo LD_LIBRARY_PATH=${LD_LIBRARY_PATH}

echo
which java
which mvn

echo
echo "========================================================================="
echo "Copy/setup test suite from Conexus-SVN checkout"
echo "https://conexus.prod.fedex.com:9443/subversion/sefs_test_automation/trunk"
echo "========================================================================="
echo
echo "TEST_SUITE_TO_RUN=${TEST_SUITE_TO_RUN}"
echo
echo
echo "========================================================================="
echo "Run CA Dev Test HERE"
echo "========================================================================="

if [[ "${TEST_SUITE_TO_RUN}" == "NONE" ]]; then
  echo; echo "No test suite selected to run.  Exiting 0."
  exit 0
fi

#cd /opt/fedex/tibco/CADevTest/bin

#Give cadevtst r/w/d permission on project root
find . -type d -exec chmod 777 {} \\;
find . -type f -exec chmod 777 {} \\;

#
# This was the original way to run CA Dev Test but it exposes the password on the command line to ps commands
#
#./TestRunner -u cadevtst -p ${CADEVTEST} -m ssl://srh00457.ute.fedex.com:2010/Registry

# TestRunner2 is a copy of the script that will read the PW from stdin so it's not showing on the ps command line
# The resultant java command executed by TestRunner2 is so long that it exceeds the max ps display size
# so the password is not revealed via the ps command.  This is not a perfect solution but will suffice for now.
umask 011
echo ${CA_DEV_TEST_PASSWORD} | ${CADEVTEST_HOME}/bin/TestRunner2 -u ${CA_DEV_TEST_USER} -m ssl://srh00457.ute.fedex.com:2010/Registry -s ${TEST_SUITE_TO_RUN}

'''
				} //stage
			} // withenv
		} // withcreds
	//} //node
}
