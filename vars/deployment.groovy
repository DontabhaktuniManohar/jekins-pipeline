// vars/deployment.groovy
def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  def deploy_worker = "ShipmentEFS-Java8"
  try { deploy_worker = "${env.NODE_NAME}" } catch (err) {}
  try { deploy_worker = "${env.DEPLOY_NODE}"  } catch (errors) {}
  if (deploy_worker == "null") {
      deploy_worker = ''
  }
  echo "agent=${deploy_worker}"
  // 1. fetch deployment properties and load them fi exist on anynode
  def props=[:]
  node(deploy_worker) {
  	try {
      unstash name: 'deploy.flag'
      sh 'ls -alrt && cat .deploy'
      if (fileExists(".deploy")) {
          // now build, based on the configuration provided
          // ,deploy will contain all the default settings
          // and can be overwritten in the console.
          props = readProperties file: '.deploy'
          echo "${props}"
      }
  	} catch (exception) { echo "${exception}"  }
  }
  // 2. if there was an existing .deploy check fi internal or EXTERNAL
  if (props.size()>0) {
    echo "\u2705 \u2705 \u2705 Deployment \u2705 \u2705 \u2705"
    // 3. if we have an externalized deployment defined.
    if (props['deploy.library.repository'] != null) {
      stage("Deployment") {
        library identifier: props['deploy.library.identifier'] , retriever: modernSCM(
            [$class: 'GitSCMSource', remote: props['deploy.library.repository'], credentialsId: props['deploy.library.creds']])
        echo "library loaded invoking deployProxy"
        deployProxy(props)
      }
    } else {
      // 4. internal deployment mechanism
      echo "Deployment Worker ${deploy_worker}"
      node(deploy_worker) {
        executeCICDeploy {
          deploy_worker = deploy_worker
        }
      }
    }
  }

}


//   node("pje22698") {
// 	try { //unstash name: 'deploy.flag'
//     sh 'ls -alrt && cat .deploy'
//     if (fileExists(".deploy")) {
//         // now build, based on the configuration provided
//         // ,deploy will contain all the default settings
//         // and can be overwritten in the console.
//         def props = readProperties file: '.deploy'
//         echo "${props}"
//     }
// 	} catch (exception) { echo "${exception}" }
//   }
// //   // 2. if there was an existing .deploy check fi internal or EXTERNAL
// //   if (len(props)>0) {
// //     echo "\u2705 \u2705 \u2705 Deployment \u2705 \u2705 \u2705"
// //     // 3. if we have an externalized deployment defined.
// //     if (props['deploy.library.repository'] != null) {
// //       stage("Deployment") {
// //         library identifier: props['deploy.library.identifier'] , retriever: modernSCM(
// //             [$class: 'GitSCMSource', remote: props['deploy.library.repository'], credentialsId: props['deploy.library.creds']])
// //         echo "library loaded invoking deployProxy"
// //         deployProxy(props)
// //       }
// //     } else {
// //       // 4. internal deployment mechanism
// //       echo "Deployment Worker ${deploy_worker}"
// //       node("pje22698") {
// //         executeCICDeploy {
// //         }
// //       }
// //     }
// //   }
// }
