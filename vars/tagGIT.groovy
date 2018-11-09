def call(String module=".") {
	sshagent(['gitlab-user-for-master']) {
		withEnv([
			"GIT_SSH_COMMAND=ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
			]) {
			withCredentials([
				usernamePassword(credentialsId: '9e863997-c0d9-4acb-8d54-99bd6c0b6ae1',
				passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME'),



			]) {
				if (fileExists('RELEASED')) {
					def taggingSuccess = sh returnStatus: true, script: '''#!/bin/bash
#set | grep GIT
RELEASE_TAG=`cat RELEASED`
# fetch all_remote objects
echo "[tagGIT]\t$(which git) $(git --version)"
echo "[tagGIT]\tcurrent status $(git status)"
echo "[tagGIT]\tURL git $(git remote -v 2>&1 | grep push)"
GIT_URL=$(git remote -v 2>&1 | grep push | perl -ne '($y,$x)=split;$x=~s/https:\\/\\//https:\\/\\/$ENV{GIT_USERNAME}:$ENV{GIT_PASSWORD}\\@/g;print $x;')
echo "[tagGIT]\tGIT_URL=$GIT_URL"
git fetch --all
if [[ "$?" -ne 0 ]]; then
	echo "[tagGIT]\t[ERROR] remote fetch failed from ${GIT_URL}"
	exit 6
fi
# list branches
echo "[tagGIT]\tlisting local branches"
git branch
echo "[tagGIT]\tshow local changes -> $GIT_URL"
git status
echo "[tagGIT]\tall local branches"
git branch -a
if [[ "$?" -eq 0 ]]; then
	# delete the local RELEASE_CUT branch
	echo "[tagGIT]\tdelete RELEASE_CUT branch if it exists"
	git branch -D RELEASE_CUT
	# create a new local branch
	echo "[tagGIT]\tcreate RELEASE_CUT branch"
	git branch RELEASE_CUT
	echo "[tagGIT]\tcheckout RELEASE_CUT branch"
	git checkout RELEASE_CUT
	if [[ "$?" -eq 0 ]]; then
			echo "[tagGIT]\tcommit new release cut work for ${RELEASE_TAG} branch locally"
			echo "[tagGIT]\tlocal pom.xml change in RELEASE_CUT branch"
			git commit -am "[jenkins] Created Tag ${RELEASE_TAG}"
			if [[ "$?" -eq 0 ]]; then
				echo "[tagGIT]\tlocal tag the RELEASE_CUT branch as a release"
				git tag -a "${RELEASE_TAG}" -m "[jenkins] Created Tag: ${RELEASE_TAG}"
				if [[ "$?" -eq 0 ]]; then
					#git gc --prune=now
					echo "[tagGIT]\tremote push the local release to remote ${GIT_URL}"
					git remote set-url origin ${GIT_URL}
					git push --set-upstream origin "${RELEASE_TAG}"
					if [[ "$?" -ne 0 ]]; then
					    echo "[tagGIT]\t[ERROR] push of ${RELEASE_TAG} to ${GIT_URL} has failed"
					    exit 5
					fi
				else
					echo "[tagGIT]\t[ERROR] pushing tagged new release remote, failed."
					exit 4
				fi
			else
				echo "[tagGIT]\t[ERROR] committing new new release cut work locally, failed."
				exit 3
			fi
	else
			echo "[tagGIT]\t[ERROR] creating new branch failed."
			exit 2
	fi
fi
'''
				} else {
					echo "[tagGIT]\t[WARN] released flag has not been set, skipping tagging for GIT"
				}
			}
		}
	}
}
