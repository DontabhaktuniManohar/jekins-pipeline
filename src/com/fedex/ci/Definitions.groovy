package com.fedex.ci

import java.io.Serializable

class Definitions implements Serializable {

   // no longer used but using the fdeploy-install.sh VERSION declaration
   // thus not forcing pipeline upgrades for a fdeploy install
   //public static String DEPLOYMENT_PACKAGE_VERSION = '7.0.7'
   public static String DEPLOYMENT_INSTALL_URL = 'http://sefsmvn.ute.fedex.com/fdeploy-install.sh'
   public static String APACHE_MAVEN_VERSION = 'apache-maven-3.3.9'
   public static String EXCLUDE_ALL_BUT_DEPLOY = '-Dexec.skip=true -Dmaven.main.skip=true -Dassembly.skipAssembly=true \
-Dmaven.install.skip=true -Dskip.npm=true -Dmdep.skip=true -Dmaven.resources.skip=true -Dmaven.test.skip=true \
-Dskip=true -Dspring-boot.repackage.skip=true -Dmaven.javadoc.skip=true -Dskip.npm=true \
-Dmaven.resources.skip=true -Dskip.yarn=true -Dskip.bower=true -Dskip.grunt=true \
-Dskip.gulp=true -Dskip.karma=true -Dskip.webpack=true \
-DaltReleaseDeploymentRepository=releases::default::https://nexus.prod.cloud.fedex.com:8443/nexus/content/repositories/SEFS-6270-releases \
-DaltSnapshotRepository=snapshots::default::https://nexus.prod.cloud.fedex.com:8443/nexus/content/repositories/SEFS-6270-snapshots'

   public static def getJDK(key) {
      def jdk = '/opt/jenkins/tools/jdk1.7.0_191'
      if ("ShipmentEFS-Java8" == key) {
         jdk='/opt/java/hotspot/8/64_bit/jdk1.8.0_181'
      }
      else if (key.indexOf("Java8") != -1) {
         // echo "---->jdk='${key}=jdk1.8"
         //jdk='/opt/jenkins/tools/jdk1.8.0_181'
         jdk='/opt/java/hotspot/8/current'
      }
      else if (key.indexOf('Java7') != -1) {
         // echo "---->jdk='${key}=jdk1.7"
         jdk='/opt/jenkins/tools/jdk1.7.0_191'
      }
      else if ("pje22698" == key || "Weblogic" == key) {
         jdk='/opt/java/hotspot/8/latest'
      }
      else if ("ShipmentEFS" == key || "uwb00078" == key ) {
         //echo "---->defaulting='${key}=jdk1.7"
         //jdk='/opt/jenkins/tools/jdk1.7.0_95'
         jdk='/opt/java/hotspot/7/64_bit/jdk1.7.0_191'
      }
      else if ("ShipmentEFS2" == key || "urh00613" == key ) {
         jdk='/opt/java/hotspot/8/64_bit/jdk1.8.0_181'
      }
      else if ("ShipmentEFS3" == key || "urh00614" == key ) {
         jdk='/opt/java/hotspot/8/64_bit/jdk1.8.0_181'
      }
      else if ("ShipmentEFSPlus" == key) {
         jdk="/opt/java/hotspot/8/current"
      }
      else if ("drh57000" == key ) {
         jdk='/opt/tibco/tibcojre64/1.7.0/'
      }
      return jdk
   }

   public static def getM2(key) {
      def mvn = '/opt/jenkins/tools/apache-maven-3.3.9'
      if (key == "ShipmentEFS" || key == "uwb00078" || key == "drh57000" ||
            key == "ShipmentEFS-Java8" || key == "ShipmentEFS2" || key == "ShipmentEFS3" ||
            key == "urh00613" || key == "urh00614" || key == "ShipmentEFSPlus" ) {
         mvn = '/opt/fedex/tibco/apache-maven-3.3.9'
      }
      else if (key == "pje22698") {
        mvn = '/home/cmbuild3/CMUTILITIES/apache-maven-3.3.9'
      }
      return mvn
   }
   public static def getTibcoHome(key) {
      if (key == "ShipmentEFS" || key == "ShipmentEFS2" || key == "ShipmentEFS3" || key == "ShipmentEFSPlus") {
         return '/opt/tibco/RA1.1'
      }
      else {
         return ''
      }
   }

   public static def isHeadless(key) {
      if ("ShipmentEFS" == key || "drh57000" == key || key.startsWith('ShipmentEFS' )) {
         return true
      }
      return false
   }

   public static def getTestSuite(opco,level) {
      def xop=opco
      if ( xop == 'FXE' ) {
         xop=''
      }
      return "test/${level}/${xop}SmokeTest/Tests/Suites/${level}Suite.ste"
   }

   public static def getSCGAV(opco) {
      if ('FXE' == "${opco}") {
         return [ groupId : 'com.fedex.sefs.core', artifactId : 'sefs_silverControl_FX' ]
      } else    if ('FXF' == "${opco}") {
         return [ groupId : 'com.fedex.fxf.core', artifactId : 'sefs_silverControl_FXF']
      } else    if ('FXG' == "${opco}") {
         return [ groupId : 'com.fedex.ground.sefs', artifactId : 'sefs_SilverControl_FXG']
      } else    if ('FXS' == "${opco}") {
         return [ groupId : 'com.fedex.sefs.common', artifactId : 'sefs_silverControl_FXS']
      } else {
         throw new Exception("undefined opco value; ${opco}. Aborting!")
      }
   }
}
