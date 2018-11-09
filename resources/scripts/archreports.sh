### Initialize variable
SCRIPT_NAME=$(basename $0 .sh)
USEFILE=false
DEBUG=false
SCRIPT_HOME=/var/tmp/cicdreports
HSTNAME=""

CFG_DIR=/var/fedex/tibco/cicd_reports/reports_source

YYYYMMDDHHMMSS=`date +"%Y%m%d%H%M%S"`
YYYYMMDD=`date +"%Y%m%d"`
LOG_DIR=/var/fedex/tibco/cicd_reports/log
LOGFILE="${LOG_DIR}/${SCRIPT_NAME}_${YYYYMMDD}.log"
ARCH_DIR=${CFG_DIR}
MASK_LOGS="${SCRIPT_NAME}_20[0-9][0-9][0-1][0-9][0-3][0-9].log"
LOG_RETENTION_DAYS=2
RPT_RETENTION_DAYS=3
DF_KEY=""
DF_TIME=""


###############################################################################
#                            FUNCTION  Create_Path()                          #
###############################################################################
function CreateFolders
{

	write_log "Requesting the folder - ${1} Creation "

		if [[ ! -d "${1}" ]]
		then

			mkdir ${1}
        		if [[ $? == 0 ]]
        		then
                		write_log "${1} folder created"
        		else
                		write_error "${1} folder creation failure"
        		fi
		else
        		write_log "${1} folder already present"
		fi
}

###############################################################################
#                            FUNCTION  Usage()                               #
###############################################################################
function Usage
{
  printf "Syntax $0 -o <<FXE|FXF|FXG>> -a <<JAVA|BE|BW>> -r <<JACOCO|PMD|>> -key <<BUILD_NUMBER>> -d <<Report location>> -a <<Application Name>> [ -arch <<Archive Location>> ][ -s <<Subfolder in Archive Location>> ]"
  printf "\n"
  printf "\n   where"
  printf "\n      -v            Run verbose"
  printf "\n      -h            Help"
}




#####*****************************************************************#####
#     Function to write application Log                                   #
#####*****************************************************************#####
function write_log {

   echo "$(date '+%D %T') - $1" >> $LOGFILE
   echo "$1"

}
#####*****************************************************************#####
#     Function to write application error                                 #
#####*****************************************************************#####
function write_error {
   echo "$(date '+%D %T') - $1" >> $LOGFILE
   echo "$1"
}



#####*****************************************************************#####
#     Function to purge old logs                                          #
#####*****************************************************************#####
function purge_logs {

find ${LOG_DIR} -type f -name "*.log" -mtime +${LOG_RETENTION_DAYS} -exec rm {} \;
        if [[ $? == 0 ]]
        then
                write_log "Old log files purged"
        else
                write_error "Error purging old filed"
        fi

}

#####*****************************************************************#####
#     Function to archive the reports                                     #
#####*****************************************************************#####

function archive_reports {

if [[ "${DF_RPTDIR}" == "" ]]
then
	write_error "Required report field missing. Check the Usage"

else
	if [[ "${DF_TIME}" == "" ]]
	then
		DF_TIME=${YYYYMMDDHHMMSS}
	fi


		if [[ "${DF_APP}" == "" ]]
		then
			if [[ "${DF_KEY}" == "" ]]
			then
				RPT_TMP=${ARCH_DIR}/${DF_OPCO}/${DF_APP}/${DF_RPTTYPE}/${DF_OPCO}_${DF_RPTTYPE}_${DF_TIME}
			else
				RPT_TMP=${ARCH_DIR}/${DF_OPCO}/${DF_APP}/${DF_RPTTYPE}/${DF_OPCO}_${DF_RPTTYPE}_${DF_KEY}_${DF_TIME}
			fi
		else

			if [[ "${DF_KEY}" == "" ]]
			then
				RPT_TMP=${ARCH_DIR}/${DF_OPCO}/${DF_APP}/${DF_RPTTYPE}/${DF_OPCO}_${DF_APP}_${DF_RPTTYPE}_${DF_TIME}
			else
				RPT_TMP=${ARCH_DIR}/${DF_OPCO}/${DF_APP}/${DF_RPTTYPE}/${DF_OPCO}_${DF_APP}_${DF_RPTTYPE}_${DF_KEY}_${DF_TIME}
			fi
		fi

		CreateFolders ${RPT_TMP}

			if [[ "${DF_SUBF}" != "" ]]
			then
				CreateFolders ${RPT_TMP}/${DF_SUBF}
				RPT_TMP=${RPT_TMP}/${DF_SUBF}
			fi

	cp -r ${DF_RPTDIR} ${RPT_TMP}

	if [[ $? == 0 ]]
	then
		write_log "Reports from ${DF_RPTDIR} archived at ${RPT_TMP}"
	else
		write_error "Error archiving the reports at ${DF_RPTDIR}"
	fi
fi


#CreateFolders ${DF_ARCHDIR}/${DF_OPCO}_${DF_APP}_${DF_RPTTYPE}_${YYYYMMDDHHMMSS}
}

#####*****************************************************************#####
#     Function to view reports                                           #
#####*****************************************************************#####

function view_reports {
echo ""
}


#####*****************************************************************#####
#     Function to purge old reports                                       #
#####*****************************************************************#####
function purge_reports {

find ${ARCH_DIR} -maxdepth 1 -mindepth 1 -type d -mtime +${RPT_RETENTION_DAYS} -exec rm -r {} \;
        if [[ $? == 0 ]]
        then
                write_log "Old reports purged"
        else
                write_error "Error purging old reports"
        fi

}


###########################################################################
#                       Main Process                                      #
###########################################################################
# Check number of variable specified
#Usage

echo "Setting Environment Parameters"
until (($# == 0 ))
do
   case $1 in
        -a)
          shift
          DF_APP=$1
          echo "Passed -param - ${DF_APP}"
          shift
          ;;
	-key)
	  shift
	  DF_KEY=$1
	  shift
	  ;;
       -o)
          shift
          DF_OPCO=$1
          shift
          ;;
       -r)
          shift
          DF_RPTTYPE=$1
          shift
          ;;
       -d)
          shift
          DF_RPTDIR=$1
          shift
          ;;
       -arch)
          shift
          DF_ARCHDIR=$1
          shift
          ;;
		-t)
          shift
          DF_TIME=$1
          shift
		  ;;
		-s)
          shift
          DF_SUBF=$1
          shift
		  ;;
   esac
done



#Checking Necessary directories

if [ ! -d ${LOG_DIR} ]; then CreateFolders ${LOG_DIR}; fi

if [ ! -d ${ARCH_DIR} ]; then CreateFolders ${ARCH_DIR}; fi

#Arhive Reports
archive_reports

#Purge Reports
#purge_reports

#Purge Logs
purge_logs

exit 0
