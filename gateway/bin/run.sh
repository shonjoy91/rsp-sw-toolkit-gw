#!/bin/bash
#
# INTEL CONFIDENTIAL
# Copyright (2015, 2016, 2017) Intel Corporation.
#
# The source code contained or described herein and all documents related to the source code ("Material")
# are owned by Intel Corporation or its suppliers or licensors. Title to the Material remains with
# Intel Corporation or its suppliers and licensors. The Material may contain trade secrets and proprietary
# and confidential information of Intel Corporation and its suppliers and licensors, and is protected by
# worldwide copyright and trade secret laws and treaty provisions. No part of the Material may be used,
# copied, reproduced, modified, published, uploaded, posted, transmitted, distributed, or disclosed in
# any way without Intel/'s prior express written permission.
# No license under any patent, copyright, trade secret or other intellectual property right is granted
# to or conferred upon you by disclosure or delivery of the Materials, either expressly, by implication,
# inducement, estoppel or otherwise. Any license under such intellectual property rights must be express
# and approved by Intel in writing.
# Unless otherwise agreed by Intel in writing, you may not remove or alter this notice or any other
# notice embedded in Materials by Intel or Intel's suppliers or licensors in any way.
#


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
