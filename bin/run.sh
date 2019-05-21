#!/bin/bash
#----------------------------------------------------------------
#- Copyright (C) 2018 Intel Corporation
#- SPDX-License-Identifier: BSD-3-Clause
#----------------------------------------------------------------

home_dir="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

keep_going="true"

while [ $keep_going = "true" ]; do
    if [[ -d ${home_dir}/lib && -d ${home_dir}/config ]]; then
        keep_going="false"
    else
        home_dir=${home_dir%/*}
    fi

    if [[ $home_dir = "/" ]]; then
        echo "walked up the entire path without finding home directory"
        exit
    fi
done;

echo "home_dir: $home_dir"

# JAVA_OPTS is a fairly standard way of passing options to the jvm.
# For example:
# export JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,address=5005,suspend=n"
# will result in the java process getting started with remote debugging enabled.
#

# Available for additional options specified in this script
ADDITIONAL_OPTS="-server"

# all the jars in the lib diretory go into the classpath
CLASSPATH="-classpath ${home_dir}/lib/*:${home_dir}/config"
MAIN_CLASS="com.intel.rfid.gateway.Main"

pushd ${home_dir}

exec java ${JAVA_OPTS} ${ADDITIONAL_OPTS} ${CLASSPATH} ${MAIN_CLASS} "$@"
