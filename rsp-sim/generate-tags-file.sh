#!/bin/bash
#
# Copyright (c) 2019 Intel Corporation
# SPDX-License-Identifier: BSD-3-Clause 
#

# Set the number of sensors to simulate
if [ "$#" -ne 3 ]; then
    echo
    echo "This script generates the tags_in_view_of_rsp_ files used with"
    echo "the Intel RFID Sensor Platform simulation script (rsp-sim.sh)."
    echo
    echo "Usage: generate-tags-file.sh <index> <start> <count>"
    echo "where..."
    echo "<index> is the RSP index between 0 and 99 (0-based index)"
    echo "<start> is the starting point between 0 and (9999 - <count>)"
    echo "<count> is the number of unique EPC's between 0 and (9999 - <base>)"
    echo
    echo "The script produces the output file - tags_in_view_of_rsp_<index>"
    echo
    exit 1
fi

MAX_INDEX=99
MAX_EPCS=9999
INDEX=$1
START=$2
COUNT=$3

if [ $INDEX -gt $MAX_INDEX ]; then
    echo "$INDEX exceeds the max allowed."
    echo "Setting sensor index to zero."
    echo
    INDEX=1
fi

if [ $COUNT -gt $(($MAX_EPCS-$START)) ]; then
    echo "base and count will exceed max allowed."
    echo "Setting <base> to zero and count to ten."
    echo
    START=0
    COUNT=10
fi

EPC_BASE="30143639F8419145BEEF"
FILE="tags_in_view_of_rsp_$INDEX"

idx=0
for i in $(seq -f "%04g" $START $(($START+$COUNT-1)))
do
  tag[$idx]="$EPC_BASE""$i"
  let idx=idx+1
done

echo "Storing output in $FILE"
echo "${tag[@]}" > $FILE


