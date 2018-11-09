#!/usr/bin/env groovy


def call(body) {
  // evaluate the body block, and collect configuration into the object
  def config = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = config
  body()

  if (config.eaiNumber == null) {
    error('no eaiNumber value supplied for fortify scan.')
  }
  unstash 'build.sources'
  unstash 'build.fortify'
  sh "cat ${WORKSPACE}/fortifyFunctions.sh"
  sh "cat ${WORKSPACE}/translate.sh"
  println "running Fortify Analysis"
  sh "${WORKSPACE}/translate.sh ${config.eaiNumber} RUN"

}
