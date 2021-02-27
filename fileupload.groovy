*******
// assuming you wish to upload a zip archive
def uploadedFile = 'uploaded.zip'

//file is uploaded to $JENKINS_HOME/$PATH_TO_THE_JOB/build/$BUILD_ID
def masterFilePath = input message: 'Upload your archive', parameters: [file(description: 'archive', name: uploadedFile)]

node('agent') { 
    stage('Copy From Master') {
        def localFile = getContext(hudson.FilePath).child(uploadedFile)
        localFile.copyFrom(masterFilePath)
        sh 'ls -al'
        archiveArtifacts uploadedFile
    }
