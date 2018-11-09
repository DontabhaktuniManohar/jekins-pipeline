import jenkins
import os
import os.path
from six.moves.urllib.request import Request, install_opener, build_opener, urlopen

class RetryException(jenkins.JenkinsException):
    '''A special exception to abort after failed retries.'''
    pass

EMPTY_FOLDER_XML='''<?xml version='1.0' encoding='UTF-8'?>
<com.cloudbees.hudson.plugins.folder.Folder plugin="cloudbees-folder@6.0.4">
<actions/>
<description/>
<properties>
<com.cloudbees.jenkins.plugins.foldersplus.SecurityGrantsFolderProperty plugin="cloudbees-folders-plus@3.1">
<securityGrants/>
</com.cloudbees.jenkins.plugins.foldersplus.SecurityGrantsFolderProperty>
<com.cloudbees.hudson.plugins.folder.properties.EnvVarsFolderProperty plugin="cloudbees-folders-plus@3.1">
<properties/>
</com.cloudbees.hudson.plugins.folder.properties.EnvVarsFolderProperty>
<org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig plugin="pipeline-model-definition@1.0.2">
<dockerLabel/>
<registry plugin="docker-commons@1.6"/>
</org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig>
<com.cloudbees.hudson.plugins.folder.properties.SubItemFilterProperty plugin="cloudbees-folders-plus@3.1"/>
</properties>
<folderViews class="com.cloudbees.hudson.plugins.folder.views.DefaultFolderViewHolder">
<views>
<hudson.model.AllView>
<owner class="com.cloudbees.hudson.plugins.folder.Folder" reference="../../../.."/>
<name>All</name>
<filterExecutors>false</filterExecutors>
<filterQueue>false</filterQueue>
<properties class="hudson.model.View$PropertyList"/>
</hudson.model.AllView>
</views>
<tabBar class="hudson.views.DefaultViewsTabBar"/>
</folderViews>
<healthMetrics>
<com.cloudbees.hudson.plugins.folder.health.WorstChildHealthMetric>
<nonRecursive>false</nonRecursive>
</com.cloudbees.hudson.plugins.folder.health.WorstChildHealthMetric>
<com.cloudbees.hudson.plugins.folder.health.AverageChildHealthMetric plugin="cloudbees-folders-plus@3.1"/>
<com.cloudbees.hudson.plugins.folder.health.JobStatusHealthMetric plugin="cloudbees-folders-plus@3.1">
<success>true</success>
<failure>true</failure>
<unstable>true</unstable>
<unbuilt>true</unbuilt>
<countVirginJobs>false</countVirginJobs>
</com.cloudbees.hudson.plugins.folder.health.JobStatusHealthMetric>
<com.cloudbees.hudson.plugins.folder.health.ProjectEnabledHealthMetric plugin="cloudbees-folders-plus@3.1"/>
</healthMetrics>
<icon class="com.cloudbees.hudson.plugins.folder.icons.StockFolderIcon"/>
</com.cloudbees.hudson.plugins.folder.Folder>
'''

def list_running_jobs(server):
   builds = server.get_running_builds()
   print(builds)
def get_job_infos(server,folder):
   list=server.get_job_info_regex(folder)
   for l in list:
      print str(l['name'])

def do_retry(function):
   i=4
   ret_value=None
   while (i>0):
      try:
         # execute the anonymous function
         return function()
         break
      except jenkins.JenkinsException as err:
         print " > ERROR: %s" % (err)
         print " try #%d/%d failed" % (4-i,3)
         i=i-1
         pass
   if (i == 0):
      raise RetryException('failed after 3 retries')

def create_folder(server, folderName):
   global EMPTY_FOLDER_XML
   job_exists = None
   def folder_exists():
      return server.job_exists(folderName)
   job_exists=do_retry(folder_exists)
   if job_exists == False or job_exists is None:
      #print("creating folder: %s" % (folderName))
      folder_url, short_name = server._get_job_folder(folderName)
      #print folder_url + "--->" + str(short_name) + " --> " + str(folderName)
      fu=server._build_url(jenkins.CREATE_JOB, locals())
      print " < create folder: " + str(fu)
      def _create_folder():
         return server.create_job(folderName,EMPTY_FOLDER_XML)
      do_retry(_create_folder)
   else:
      print(" ? folder %s already exists" % (folderName))
      return folderName

def copy_jobs(server,folder,folderNew, jobname):
   job_exists=True
   newjobname=jobname.replace('_ALT','')
   def _job_exists():
      return server.job_exists( folderNew + newjobname)
   job_exists=do_retry(_job_exists)
   if job_exists == True:
      print(" ? job %s already exists" % (folderNew + newjobname))
   else:
      def _copy_job():
         print " < copying: %s" % (str(folder + jobname))
         my_job = server.get_job_config(str(folder + jobname))
         my_ret = server.create_job(folderNew + newjobname,my_job)
         print(" > copied=%s" % (folderNew + newjobname ))
      try:
         do_retry(_copy_job)
      except RetryException as err:
         # skipping record
         print " ! skipping job %s" % (jobname)


def recreate_jobs(server,folder,jobname):
    recreate_job(server, folder + jobname)

def recreate_job(server,jobpath):
   job_exists=False
   def _job_exists():
      return server.job_exists( jobpath)
   job_exists=do_retry(_job_exists)
   if job_exists == True:
      my_job = ''
      def _copy_job():
         print " < copying: %s" % (str(jobpath))
         _file = open('delete_tmp.txt', 'w')
         my_job = server.get_job_config(str(jobpath))
         _file.write(my_job)
      def _delete_job():
         print " > deleting: %s" % (str(jobpath))
         my_ret = server.delete_job(jobpath)
      def _create_job():
         _file = open('delete_tmp.txt', 'r')
         my_job = _file.read()
         my_ret = server.create_job(jobpath,my_job)
         print(" > copied=%s" % (jobpath ))
      stage = 'init'
      try:
         # do_retry(_copy_job)
         # stage = 'copy'
         # do_retry(_delete_job)
         # stage = 'delete'
         print " > initiate create"
         do_retry(_create_job)
         stage = 'create'
      except RetryException as err:
         # skipping record
         print " ! skipping job %s after %s stage." % (jobpath,stage)
   else:
      if os.path.isfile('job.txt'):
         print " < job file exists but no job in cloudbees"
         print " > creating job based on job file"
         try:
            def _create_job():
               _file = open('delete_tmp.txt', 'r')
               my_job = _file.read()
               my_ret = server.create_job(jobpath,my_job)
               print(" > created=%s from from file job.txt" % (jobpath ))
            do_retry(_create_job)
         except RetryException as err:
            print " ! skipping job %s after %s stage." % (jobname,stage)
         stage = 'create'


def create_job_from_xml(server, folderNew, jobname, job):
  if os.path.isfile(job):
     print " < job file exists but no job in cloudbees"
     print " > creating job based on job file"
     try:
        def _create_job():
           _file = open(job, 'r')
           my_job = _file.read()
           jobpath="%s/%s" % (folderNew, jobname)
           print(" > creating job '%s'" % (jobpath))
           my_ret = server.create_job(jobpath,my_job)
           print(" > created=%s from from file %s" % (folderNew + jobname, job ))
        do_retry(_create_job)
     except RetryException as err:
        print " ! skipping job %s after %s stage." % (jobname,stage)
     stage = 'create'


def build_jobs(server,folderNew, jobname):
   job_exists=True
   newjobname=jobname.replace('_ALT','')
   def _job_exists():
      return server.job_exists( folderNew + newjobname)
   job_exists=do_retry(_job_exists)
   if job_exists == True:
      def _build_job():
         print " > building: %s" % (str(folderNew + newjobname))
         my_ret = server.build_job(folderNew + newjobname)
      do_retry(_build_job)

def delete_jobs(server,folderNew, jobname):
   newjobname=jobname.replace('_ALT','')
   def _job_exists():
      return server.job_exists( folderNew + newjobname)
   job_exists=do_retry(_job_exists)
   if job_exists == False:
      print("%s=doesnt exist" % (folderNew + jobname))
   else:
      def _delete_job():
         print " > deleting: %s" % (str(folderNew + newjobname))
         my_ret = server.delete_job(folderNew + newjobname)
      do_retry(_delete_job)



#print EMPTY_FOLDER_XML
server = jenkins.Jenkins('https://jenkins-shipment.web.fedex.com:8443/jenkins', username='751818', password=os.environ['SC_PASSWORD'])
def do_whoami():
   return server.get_whoami()
def do_version():
   return server.get_version()
user = do_retry(do_whoami)
version = do_retry(do_version)
print('Hello %s from Jenkins %s' % (user['fullName'], version))

file = open('JOB_LIST.txt', 'r')
#list=file.read().split('\n')
#list=['/FXF_SEFS_CORE']
#list=['SEFS_FXG-6873/FXG_CORE/FXG_SEFS_CORE']
list=['SEFS_FXG-6873/FXG_SEFS_DOMAIN_VIEW/FXG_SEFS_BIZMATICS_MOCKSERVICE']
list=['SEFS_FXF-7499/FXF_SEFS_SC_PROPERTIES']
list=['SEFS_FXE-6269/FXE_SEFS_DOMAIN']
list=['SEFS_FXE-6269/FXE_SEFS_SC_PROPERTIES']
list=['SEFS_FXE-6269/FXE_CORE/BE/FXE_SEFS_CONVEYANCE']
list=['SEFS_COMMON-6270/JOBS/FXS_DASHBOARD_BY_JENKINSFILE']
list=['SEFS_COMMON-6270/TOOLS/FXS_SILVERCONTROL_TOOL']
list=['WAYPOINT-3534587/WAYPOINT_MATCHING_SERVICE2']

folderCurrent='ShipmentEFS/'
folderNew='ShipmentEFS/'
foldersCompleted=[]

#
recreate_job(server, list[0])
# for job in list:
#    job=job.strip()
#    if job.startswith('//') or job.startswith('#') or job == '':
#       continue
#    print "> %s" % (job)
#    jobname = str(job).split('/')
#    name = jobname.pop()
#    #path=[]
#    #for fName in jobname:
#    #   path.append(fName)
#    #   folderName = "/".join(path)
#    #   if not folderName in foldersCompleted:
#    #      create_folder(server,folderNew + folderName)
#    #      foldersCompleted.append(folderName)
#    # job action
#create_job_from_xml(server,'3534681-SEFS_COMMON/Java','RestoredJob-UUID2', '/Users/ak751818/Downloads/1.1.0.xml')
#    build_jobs(server,folderCurrent,job)

#   copy_jobs(server,'ShipmentEFS_old/',folderNew,job)
# SEFS_COMMON-6270/RELEASE_MANAGEMENT/FXE_SEFS_DEPLOYMENT
#for job in list:
#   job=job.strip()
#   if job.startswith('//') or job.startswith('#') or job == '':
#      continue
#   print "> %s" % (job)
#   build_jobs(server,folderNew,job)
#   delete_jobs(server,folderNew,job)

#for job in list:
#   job=job.strip()
#   if job.startswith('//') or job.startswith('#') or job == '':
#      continue
#   build_jobs(server,folderNew,job)
