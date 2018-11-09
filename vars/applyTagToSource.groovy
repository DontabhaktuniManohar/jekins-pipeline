
def call(String m2_home) {
  def releaseCutSuccess = 0
  withEnv(["M2_HOME=${m2_home}"]) {
    releaseCutSuccess = sh returnStatus: true, script : '''#!/bin/bash
set +xe
export PATH=${M2_HOME}/bin:$PATH
next_version="${release_tag}"
if [[ "${next_version}" == "" ]]; then
WORD_FLAG=""
if [[ "${RELEASE_ITEMS}" != "" ]]; then
WORD_FLAG="-w"
fi
next_version=$(python cloudbees.py "${WORD_FLAG}" 2> /dev/null )
fi
echo "next=${next_version}"
finalExit=0
[[ ! -e RELEASED ]] && echo "next=${next_version}" && ${m2_home}/bin/mvn -s ./settings.xml -B -U org.codehaus.mojo:build-helper-maven-plugin:3.0.0:parse-version versions:set -DnewVersion="${next_version}" versions:commit ;
echo "${next_version}" > RELEASED
'''
  }
  return releaseCutSuccess
}
