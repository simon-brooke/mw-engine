#!/bin/bash

# Heavily hacked build/release script for MicroWorld
# Expects to be run from the parent directory of the directory which contains all
# the MicroWorld projects.

# WARNING: The regexps in this are fair awfy bruckle. Edit with care.

# Simon Broooke <simon@journeyman.cc>

# Variable and glag initialisation
archive=FALSE
email=`grep ${USER} /etc/passwd | awk -F\: '{print $5}' | awk -F\, '{print $4}'`
fullname=`grep ${USER} /etc/passwd | awk -F\: '{print $5}' | awk -F\, '{print $1}'`
old="unset"
release=""
tmp=buildall.tmp.$$
trial="FALSE"
webappsdir="/var/lib/tomcat8/webapps"

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
		i="unset"
	else
		t=`date --rfc-3339 seconds`
		i="${v} built by ${u} on ${t}"
	fi

cat <<-EOF > ${tmp}/manifest.sed
s/${o}/${v}/g
s/"build-signature-user" *".*"/"build-signature-user" "${u}"/
s/"build-signature-email" *".*"/"build-signature-email" "${e}"/
s/"build-signature-timestamp" *".*"/"build-signature-timestamp" "${t}"/
s/"build-signature-version" *".*"/"build-signature-version" "${v}"/
s/"Implementation-Version" *".*"/"Implementation-Version" "${i}"/
EOF
}

if [ $# -lt 1 ]
then
	cat <<-EOF 1>&2
	Usage:
    -archive           Create a tar archive of the current state of the source.
	  -build             Build all components, commit and push to origin.
    -docker            Build and push a Docker image.
	  -email [ADDRESS]   Your email address, to be recorded in the build signature.
	  -fullname [NAME]   Your full name, to be recorded in the build signature.
	  -pull				 Pull from remote git repository
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
    -a|-archive)
      	archive="TRUE";;
		-b|-build)
			# 'build' is the expected normal case.
			trial="FALSE";
			;;
    -d|-docker)
      docker="TRUE";;
		-e|-email)
			shift;
			email=$1;;
		-f|-fullname)
			shift;
			fullname=$1;;
	    -p|-pull)
	      	# pull from remote Git origin
	      	git pull origin master;;
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

echo "Trial: ${trial}; docker: ${docker}; email: ${email}; fullname ${fullname}; release: ${release}; webapps: $webappsdir"

ls mw-* > /dev/null 2>&1
if [ $? -ne 0 ]
then
	echo "No subdirectories matching 'mw-*' found, exiting." 1>&2;
	exit 1;
fi

for dir in mw-*
do
  if [ "${dir}" != "mw-explore" ]
  then
    pushd ${dir}

    # Make a temporary directory to keep the work-in-progress files.
    if [ ! -d "${tmp}" ]
    then
      rm -f "${tmp}"
      mkdir "${tmp}"
    fi

    cat project.clj > ${tmp}/project.bak.1
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
        setup-build-sig "unset" "${old}" "${fullname}" "${email}"
      fi
    else
      setup-build-sig "unset" "${old}" "${fullname}" "${email}"
    fi

    sed -f ${tmp}/manifest.sed ${tmp}/project.bak.1 > project.clj

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

    # If we're in the UI project, build the uberwar - and should
    # probably deploy it to local Tomcat for test
    if [ "${dir}" = "mw-ui" -a "${webappsdir}" != "" ]
    then
      lein ring uberwar
      sudo cp target/microworld.war "${webappsdir}"
      echo "Deployed new WAR file to local Tomcat at ${webappsdir}"
    fi

    if [ "${dir}" = "mw-ui" -a "${docker}" = "TRUE" ]
    then
      lein docker build
      lein docker push
    fi

    # Then unset manifest properties prior to committing.
    cat project.clj > ${tmp}/project.bak.2
    setup-build-sig
    sed -f ${tmp}/manifest.sed ${tmp}/project.bak.2 > project.clj

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

      cat project.clj > ${tmp}/project.bak.3
      setup-build-sig "${old}" "${release}-SNAPSHOT" "${fullname}" "${email}"
      sed -f ${tmp}/manifest.sed ${tmp}/project.bak.3 > project.clj
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

      # Then unset manifest properties prior to committing.
      cat project.clj > ${tmp}/project.bak.4
      setup-build-sig
      sed -f ${tmp}/manifest.sed ${tmp}/project.bak.4 > project.clj

      if [ "${trial}" = "FALSE" ]
      then
        git commit -a -m "${message}"
        echo ${message}
        git push origin master
      fi
    fi

    # if nothing broke so far, clean up...
    rm -rf "${tmp}"
    popd
  fi
done


if [ "${archive}" ]
then
  for dir in mw-*
  do
    pushd ${dir}
    version=`cat project.clj | grep 'defproject mw' | sed 's/.*defproject mw-[a-z]* "\([A-Za-z0-9_.-]*\)".*/\1/'`
    lein clean
    popd
  done

  tmp=microworld-${version}
  mkdir ${tmp}
  pushd ${tmp}
  for dir in ../mw-*
  do
    cp -r $dir .
  done
  popd
  tar czvf ${tmp}.orig.tar.gz ${tmp}
  rm -rf ${tmp}
fi


