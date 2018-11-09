
def call() {
  echo "\u25b6 \u25b6 \u25b6 \u25b6 \u25b6 \u25b6"
  releaseCutSuccess = sh returnStdout: true, script : '''#!/bin/bash
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
echo "${next_version}" > RELEASED
echo "${next_version}"
'''
    echo "propspective release ${releaseCutSuccess}"
    echo "\u25b6 \u25b6 \u25b6 \u25b6 \u25b6 \u25b6"
    return releaseCutSuccess
}
