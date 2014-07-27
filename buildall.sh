#!/bin/bash

# Heavily hacked build/release script for MicroWorld
# Expects to be run from the parent directory of the directory which contains all
# the MicroWorld projects.

# WARNING: The regexps in this are fair awfy bruckle. Edit with care.

# Simon Broooke <simon@jasmine.org.uk>

email=`grep ${USER} /etc/passwd | awk -F\: '{print $5}' | awk -F\, '{print $4}'`
fullname=`grep ${USER} /etc/passwd | awk -F\: '{print $5}' | awk -F\, '{print $1}'`
webappsdir="/var/lib/tomcat7/webapps"
release=""
trial="FALSE"

# Builds the build signature properties in the manifest map file
# expected arguments: old version tag, version tag, full name of user, 
# email of user; if not passed, all these will be set to "unset".
# The objective I'm trying to achieve is that when committed to version
# control, these are all always unset; but they're all valid in a build.
function setup-build-sig {
	if [ "${1}" = "" ]
	then
		o="unset"
	else
		o="${1}" 
	fi
	if [ "${2}" = "" ]
	then
		v="unset"
	else
		v="${2}" 
	fi
	if [ "${3}" = "" ]
	then
		u="unset"
	else
		u="${3}" 
	fi
	if [ "${4}" = "" ]
	then
		e="unset"
	else
		e="${4}" 
	fi

	if [ "${2}${3}${4}" = "" ]
	then
		t="unset"
	else
		t=`date --rfc-3339 seconds`
	fi

cat <<-EOF > buildall.tmp/manifest.sed
s/${o}/${v}/g
s/^ *"build-signature-version" ".*" *\$/\t\t"build-signature-version" "${v}"/
s/^ *"build-signature-user" ".*" *\$/\t\t"build-signature-user" "${u}"/
s/^ *"build-signature-email" ".*" *\$/\t\t"build-signature-email" "${e}"/
s/^ *"build-signature-timestamp" ".*" *\$/\t\t"build-signature-timestamp" "${t}"/
EOF
}

if [ $# -lt 1 ]
then
	cat <<-EOF 1>&2
	Usage:
	  -build             Build all components and commit to master.
	  -email [ADDRESS]   Your email address, to be recorded in the build signature.
	  -fullname [NAME]   Your full name, to be recorded in the build signature.
	  -release [LABEL]   Build all components, branch for release on old label, then 
	                     upversion to new LABEL and commit to master.
	  -trial             Trial build only, do not commit.
	  -webapps [PATH]    Set the path to the local tomcat webapps directory
EOF
	exit 1
fi
while (( "$#" ))
do
	case $1 in
		-b|-build) 
			# 'build' is the expected normal case.
			trial="FALSE";
			;; 
		-e|-email)
			shift;
			email=$1;;
		-f|-fullname)
			shift;
			fullname=$1;;
		-r|-release) 
			# release is branch a release and upversion to new label
			shift;
			release=$1;
			trial="FALSE";
			if [ "${release}" = "" ]
			then
				echo "Release flagged, but no release tag supplied" 1>&2;
				exit 1;
			fi;;
		-t|-trial)
			trial="TRUE";;
		-w|-webapps)
			# Set the tomcat webapps directory to release to
			shift;
			webappsdir=$1;;
		*)
			echo "Unrecognised option '${1}', exiting." 1>&2;
			exit 1;;
	esac

	shift
done

echo "Trial: ${trial}; email: ${email}; fullname ${fullname}; release: ${release}; webapps: $webappsdir"

ls mw-* > /dev/null 2>&1
if [ $? -ne 0 ]
then
	echo "No subdirectories matching 'mw-*' found, exiting." 1>&2;
	exit 1;
fi

for dir in mw-*
do
	pushd ${dir}
	
	if [ ! -d "buildall.tmp" ]
	then
		rm -f "buildall.tmp"
		mkdir "buildall.tmp"
	fi

	cat project.clj > buildall.tmp/project.bak.1
	old=`cat project.clj | grep 'defproject mw' | sed 's/.*defproject mw-[a-z]* "\([A-Za-z0-9_.-]*\)".*/\1/'`

	if [ "${release}" != "" ]
	then
		message="Preparing ${old} for release"

		# Does the 'old' version tag end with the token "-SNAPSHOT"? it probably does!
		echo "${old}" | grep 'SNAPSHOT' 
		if [ $? -eq 0 ]
		then
			# It does... 
			interim=`echo ${old} | sed 's/\([A-Za-z0-9_.-]*\)-SNAPSHOT.*/\1/'`
			if [ "${interim}" = "" ]
			then
				echo "Failed to compute interim version tag from '${old}'" 1>&2
				exit 1;
			fi
			setup-build-sig "${old}" "${interim}" "${fullname}" "${email}"
			message="Upversioned from ${old} to ${interim} for release"
			old=${interim}
		else
			setup-build-sig "${old}" "${old}" "${fullname}" "${email}"
		fi
	else
		setup-build-sig "${old}" "${old}" "${fullname}" "${email}"	
	fi
			
	sed -f buildall.tmp/manifest.sed buildall.tmp/project.bak.1 > project.clj

	echo $message

	lein clean
	lein compile
	if [ $? -ne 0 ]
	then
		echo "Sub-project ${dir} failed in compile" 1>&2
		exit 1
	fi

  	lein test
	if [ $? -ne 0 ]
	then
		echo "Sub-project ${dir} failed in test" 1>&2
		exit 1
	fi

	lein marg
	lein install
	
	cat project.clj > buildall.tmp/project.bak.2
	setup-build-sig "${old}"
	sed -f buildall.tmp/manifest.sed buildall.tmp/project.bak.2 > project.clj

	if [ "${trial}" = "FALSE" ]
	then
		if [ "${message}" = "" ]
		then
			git commit -a
		else
			git commit -a -m "$message"
		fi
		git push origin master
	fi

	if [ "${release}" != "" ]
	then
		branch="${old}_MAINTENANCE"
		if [ "${trial}" = "FALSE" ]
		then
			git branch "${branch}"
			git push origin "${branch}"
		fi
		
		cat project.clj > buildall.tmp/project.bak.3
		setup-build-sig "${old}" "${release}-SNAPSHOT" "${fullname}" "${email}"
		sed -f buildall.tmp/manifest.sed buildall.tmp/project.bak.3 > project.clj
		message="Upversioned from ${interim} to ${release}-SNAPSHOT"

		echo $message

		lein clean
		lein compile
		if [ $? -ne 0 ]
		then
			echo "Sub-project ${dir} failed in compile after branch to ${release}!" 1>&2
			exit 1
		fi
		lein marg
		lein install
		
		cat project.clj > buildall.tmp/project.bak.4
		setup-build-sig "${release}-SNAPSHOT"
		sed -f buildall.tmp/manifest.sed buildall.tmp/project.bak.4 > project.clj
		
		if [ "${trial}" = "FALSE" ]
		then
			git commit -a -m "${message}"
			echo ${message}
			git push origin master
		fi
	fi

	# Finally, if we're in the UI project, build the uberwar - and should 
	# probably deploy it to local Tomcat for test
	if [ "${dir}" = "mw-ui" ]
	then
    	lein ring uberwar
		sudo cp buildall.tmp/microworld.war /var/lib/tomcat7/webapps
		echo "Deployed new WAR file to local Tomcat"
	fi
	popd
done


