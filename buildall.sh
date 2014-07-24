#!/bin/bash

# Heavily hacked build/release script for MicroWorld
# Expects to be run from the parent directory of the directory which contains all
# the MicroWorld projects.

release=""

case $1 in
	build) 
		# 'build' is the expected normal case.
		;; 
	release) 
		# release is branch a release and upversion to new label
		release=$2;
		if [ "${release}" = "" ]
		then
			echo "Release flagged, but no release tag supplied" 1>&2;
			exit 1;
		fi;;
	*)
		echo "Usage:" 1>&2;
		echo "  ${0} build             Build all components and commit to master" 1>&2;
		echo "  ${0} release [LABEL]   Build all components, branch for release on " 1>&2;
		echo "        old label, then upversion to new LABEL and commit to master" 1>&2;
		exit 1;;
esac

for dir in mw-*
do
	pushd ${dir}

	if [ "${release}" != "" ]
	then
		old=`cat project.clj | grep 'defproject mw' | sed 's/(defproject mw-[a-z]* "\([A-Za-z0-9_.-]*\).*)"/\1/'`

		echo "foo ${old}"
		# Does the 'old' version tag end with the token "-SNAPSHOT"? it probably does!
		echo "${old}" | grep 'SNAPSHOT$' 
		if [ $? -eq 0 ]
		then
			# It does... 
			interim=`echo ${old} | sed 's/\([A-Za-z0-9_.-]*-SNAPSHOT/\1/'`
			cat project.clj > project.bak.1
			sed "s/${old}/${interim}/" project.bak.1 > project.clj
			echo "Upversioned from ${old} to ${interim}"
			old="${interim}"
		fi
	fi

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
	git commit -a
	# git push origin master
	if [ "${release}" != "" ]
	then
		branch="${old}_MAINTENANCE"
		git branch "${branch}"
		# git push origin "${branch}"
		cat project.clj > project.bak.2
		sed "s/${interim}/${release}-SNAPSHOT" project.bak.2 > project.clj
		echo "Upversioned from ${interim} to ${release}"
		lein compile
		if [ $? -ne 0 ]
		then
			echo "Sub-project ${dir} failed in compile after branch to ${release}!" 1>&2
			exit 1
		fi
		message="Upversioned from ${old} to ${release}-SNAPSHOT"
		git commit -a -m "${message}"
		echo ${message}
		# git push origin master
	fi

	# Finally, if we're in the UI project, build the uberwar - and should probably deploy it
	# to local Tomcat for test
	if [ "${dir}" = "mw-ui" ]
  then
    lein ring uberwar
		sudo cp target/microworld.war /var/lib/tomcat7/webapps
  fi
	popd
done


