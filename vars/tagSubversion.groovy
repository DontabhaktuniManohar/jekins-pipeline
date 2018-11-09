def call(String module=".") {
   dir(module) {
     withCredentials([
      usernamePassword(credentialsId: '9e863997-c0d9-4acb-8d54-99bd6c0b6ae1',
      passwordVariable: 'SVN_PASSWORD', usernameVariable: 'SVN_USERNAME')
   ]) {
      if (fileExists('RELEASED')) {
         def taggingSuccess = sh returnStatus: true, script: '''#!/bin/bash
export PATH=/opt/fedex/tibco/subversion-1.8.19/bin:/opt/svn/bin:${PATH}
next_version=`cat RELEASED`
SVN_EXE=`which svn`
SVN_URL=`${SVN_EXE} info | perl -ne 'if (/^URL/) {(undef,$x)=split;$x=~s/\\/(?:trunk|branches\\/\\S+)$//g;print $x;}'`
echo "using ${SVN_EXE} with version ${next_version} => ${SVN_URL}"

svn_opts="--non-interactive --trust-server-cert --username ${SVN_USERNAME} --password ${SVN_PASSWORD}"
svn $svnopts update

# if no tags exist create one
${SVN_EXE} ls ${SVN_URL}/tags $svn_opts --depth empty
if [[ "$?" == "1" ]]; then
    echo "creating the directory ${SVN_URL}/tags"
    ${SVN_EXE} mkdir ${SVN_URL}/tags $svn_opts -m "SEFS shared-library: creating /tags directory."
fi
echo
echo Copy current branch
echo
${SVN_EXE} copy . ${SVN_URL}/tags/${next_version} $svn_opts -m "Tagged by SEFS shared-library. Release ${next_version}"
echo
echo Checkout current branch
echo
${SVN_EXE} $svn_opts co ${SVN_URL}/tags/${next_version}"
echo "Committing ${next_version}"
${SVN_EXE} --non-interactive upgrade .
${SVN_EXE} commit $svn_opts ${SVN_URL}/tags/${next_version} -m "Tagged by SEFS shared-library. Release ${next_version}"
'''
      }
   }
}
}
