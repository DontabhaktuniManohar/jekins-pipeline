#!/bin/sh

set -e

if [ $# -ne 1 ]
then
    echo "usage: ${0} \"env\""
    exit 1
fi

. ./env.sh

cf target -s ${SPACE}

cf delete -f -r ${APP_NM}

