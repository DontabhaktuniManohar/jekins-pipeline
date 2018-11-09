#!/bin/sh

set -e

if [ $# -ne 2 ]
then
    echo "usage: ${0} \"binary location\" \"env\""
    exit 1
fi

BINARY=${1}

shift 1

. ./env.sh

function zeroDTPush {
    cf p ${APP_NM}-g -f manifest_${SPACE}.yml -p ${BINARY} --hostname ${APP_HOST}-g
    cf map-route ${APP_NM}-g ${DOMAIN} -n ${APP_HOST}
    cf unmap-route ${APP_NM} ${DOMAIN} -n ${APP_HOST}
    cf unmap-route ${APP_NM}-g ${DOMAIN} -n ${APP_HOST}-g
    cf delete-route -f ${DOMAIN} -n ${APP_HOST}-g
    cf delete -f ${APP_NM}
    cf rename ${APP_NM}-g ${APP_NM}
}

function initManifestFile {

    while read
    do
        eval "echo \"${REPLY}\""
    done < manifest.yml > manifest_${SPACE}.yml
}

cf target -s ${SPACE}
initManifestFile

set +e
cf app ${APP_NM} > /dev/null 2>&1
app_exists=${?}
set -e

echo "app exists: ${app_exists}"

if [ ${app_exists} -eq 0 ]
then
    echo "Doing zdt push"
    zeroDTPush
else
    echo "Doing std push"
    cf p ${APP_NM} -f manifest_${SPACE}.yml -p ${BINARY} --hostname ${APP_HOST}
fi

if [ "${SPACE}" == "prod" -o "${SPACE}" == "test" ]
then
    echo "Bind to additional services here"
fi

