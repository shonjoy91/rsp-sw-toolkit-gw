#!/bin/bash
#
# Copyright (c) 2019 Intel Corporation
# SPDX-License-Identifier: BSD-3-Clause 
#

# Set the number of RSPs to simulate
if [ "$#" -ne 1 ]; then
    echo
    echo "This script simulates the API messages between the specified numbers of"
    echo "Intel RFID Sensor Platforms (RSP) and the Intel RSP SW Toolkit - Gateway."
    echo
    echo "Usage: rsp-sim.sh <count>"
    echo "where <count> is the number of RSP's to simulate."
    echo
    echo "This script depends on the mosquitto-clients package being installed."
    echo "Run 'sudo apt install mosquitto-clients' to install."
    echo
    echo "NOTE: The Intel RSP SW Toolkit - Gateway must be running BEFORE"
    echo "      attempting to execute this script."
    echo
    exit 1
fi

MAX_RSPS=99
RSPS=$1
if [ $RSPS -gt $MAX_RSPS ]; then
    echo "$RSPS exceeds max allowed."
    echo "Setting RSP count to five."
    RSPS=5
fi

GATEWAY_IP="127.0.0.1"
DEVICE_ID_INDEX=0
FACILITY_ID_INDEX=1
READ_STATE_INDEX=2
TOKEN_INDEX=3
DEFAULT_TOKEN="D544DF3F42EA86BED3C3D15FC321B8E949D666C06B008C6357580BC3816E00DE"
ROOT_CERT_URL="http://$GATEWAY_IP:8080/config/root-ca-cert"
MQTT_CRED_URL="https://$GATEWAY_IP:8443/config/sensor-credentials"
MQTT_BROKER=$GATEWAY_IP
HOST_BASE=150000
RSP_FILE_BASE="rsp_"
TAG_FILE_BASE="tags_in_view_of_rsp_"
START_TIME=$(($(date +%s%N)/1000000))
AMBI_TEMP=30
RFID_TEMP=70
HCPU_TEMP=100
CPU_USAGE=25
MEM_USAGE=25
MEM_TOTAL=1017576
RfModuleError=100
HighAmbientTemp=101
HighCpuTemp=102
HighCpuUsage=103
HighMemoryUsage=104
DeviceMoved=151


#
# Define all the functions up front
#

# Function takes device_id and token as arguments
get_mqtt_credentials () {
    TIME=$(($(date +%s%N)/1000000))
    RAW=$(curl --insecure --header "Content-type: application/json" --request POST --data '{"username":"'$1'","token":"'$2'","generatedTimestamp":'$TIME',"expirationTimestamp":-1}' $MQTT_CRED_URL)
    if [ "$RAW" = "" ]; then
        echo "Cannot access MQTT Credentials REST endpoint!"
        exit 1
    fi
    IFS=’/’ read -ra ARRAY <<< "$RAW"
    RAW=${ARRAY[2]}
    IFS=’:’ read -ra ARRAY <<< "$RAW"
    MQTT_BROKER=${ARRAY[0]}
}

# Function takes device_id and rsp index as arguments
send_connect_request () {
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/connect -m '{"jsonrpc":"2.0","id":"1","method":"connect","params":{"hostname":"'$1'","hwaddress":"98:4f:ee:15:00:'$2'","app_version":"19.1.sim","module_version":"none","num_physical_ports":2,"motion_sensor":true,"camera":false,"wireless":false,"configuration_state":"unknown","operational_state":"unknown"}}'
}

# Function takes device_id and facility_id as arguments
send_status_indication () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/rsp_status/$1 -m '{"jsonrpc":"2.0","method":"status_update","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'","status":"ready"}}'
}

# Function takes device_id and facility_id as arguments
send_heartbeat_indication () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/rsp_status/$1 -m '{"jsonrpc":"2.0","method":"heartbeat","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'","location":null,"video_url":null}}'
}

# Function takes device_id, facility_id, alert_number, alert_description, serverity and optional as arguments
send_device_alert_indication () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/rsp_status/$1 -m '{"jsonrpc":"2.0","method":"device_alert","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'","alert_number":'$3',"alert_description":"'$4'","severity":"'$5'","optional":'$6'}}'
}

# Function takes device_id and facility_id as arguments
send_inventory_complete_indication () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/rsp_status/$1 -m '{"jsonrpc":"2.0","method":"inventory_complete","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'"}}'
}

# Function takes device_id, facility_id and EPC as arguments
send_inventory_data_indication () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/data/$1 -m '{"jsonrpc":"2.0","method":"inventory_data","params":{"sent_on":'$TIME',"period":500,"device_id":"'$1'","facility_id":"'$2'","location":null,"motion_detected":true,"data":[{"epc":"'$3'","tid":null,"antenna_id":0,"last_read_on":'$TIME',"rssi":-654,"phase":32,"frequency":915250}]}}'
}

# Function takes device_id and "id":id as arguments
send_command_response () {
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/response/$1 -m '{"jsonrpc":"2.0","result":true,'$2'}'
}

# Function takes device_id, uptime and "id":id as arguments
send_bist_results_response () {
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/response/$1 -m '{"jsonrpc":"2.0","result":{"rf_module_error":false,"rf_status_code":0,"rf_module_temp":'$RFID_TEMP',"ambient_temp":'$AMBI_TEMP',"time_alive":'$2',"cpu_usage":'$CPU_USAGE',"mem_used_percent":'$MEM_USAGE',"mem_total_bytes":'$MEM_TOTAL',"camera_installed":false,"temp_sensor_installed":true,"accelerometer_installed":true,"region":"USA","rf_port_statuses":[{"port":0,"forward_power_dbm10":280,"reverse_power_dbm10":0,"connected":true},{"port":0,"forward_power_dbm10":280,"reverse_power_dbm10":0,"connected":true}],"device_moved":false},'$3'}'
}

# Function takes device_id, index and "id":id as arguments
send_state_response () {
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/response/$1 -m '{"jsonrpc":"2.0","result":{"device_id":"'$1'","hwaddress":"98:4f:ee:15:00:'$2'","app_version":"19.1.sim","module_version":"none","num_physical_ports":2,"motion_sensor":true,"camera":false,"wireless":false,"configuration_state":"unknown","operational_state":"unknown"},'$3'}'
}

# Function takes device_id and "id":id as arguments
send_sw_version_response () {
    mosquitto_pub -h $MQTT_BROKER -t rfid/rsp/response/$1 -m '{"jsonrpc":"2.0","result":{"app_version":"19.1.sim","module_version":"none"},'$2'}'
}

# Function takes rsp index as an argument
wait_for_connect_response () {
    index=$1
    rsp_file="$RSP_FILE_BASE""$index"
    rsp=($(cat $rsp_file))

    MSG=$(mosquitto_sub -h $MQTT_BROKER -t rfid/rsp/connect/${rsp[$DEVICE_ID_INDEX]} -C 1)
    # Split the message into individual parameters
    IFS=’,’ read -ra SP1 <<< "$MSG"
    # Parse out the facility_id
    IFS=’:’ read -ra SP2 <<< "${SP1[2]}"
    FACID="${SP2[2]}"
    FACID="${FACID%\"}"
    FACID="${FACID#\"}"
    # and store it in the rsp array
    rsp[$FACILITY_ID_INDEX]=$FACID
    echo "${rsp[@]}" > $rsp_file
}

# Function takes rsp index as an argument
wait_for_command () {
    index=$1
    rsp_file="$RSP_FILE_BASE""$index"
    rsp=($(cat $rsp_file))

    # Loop forever
    while [ 0 -lt 1 ]; do
        # Wait for the MQTT command (block)
        CMD=$(mosquitto_sub -h $MQTT_BROKER -t rfid/rsp/command/${rsp[$DEVICE_ID_INDEX]} -C 1)
        process_command $index $CMD &
    done
}

# Function takes rsp index and command as arguments
process_command () {
    index=$1
    rsp_file="$RSP_FILE_BASE""$index"
    rsp=($(cat $rsp_file))

    # Split the command into individual parameters
    IFS=’,’ read -ra CMD <<< "$2"
#    printf '%s\n' "${CMD[@]}"

    # Extract the id
    ID=${CMD[1]}
    # Extract the method
    METHOD=${CMD[2]}

    if [ "$METHOD" = '"method":"apply_behavior"' ]; then
        ACTION=${CMD[3]}
        REPEAT=${CMD[26]}
        if [ "$ACTION" = '"params":{"action":"STOP"' ]; then
            send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID
            rsp[$READ_STATE_INDEX]="STOPPED"
            echo "${rsp[@]}" > $rsp_file
            send_inventory_complete_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
        elif [ "$REPEAT" = '"auto_repeat":false' ]; then
            send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID
            rsp[$READ_STATE_INDEX]="STARTED"
            echo "${rsp[@]}" > $rsp_file
            DUR=${CMD[19]}
            IFS=’:’ read -ra MILLIS <<< "$DUR"
            sleep $((${MILLIS[1]} / 1000))
            rsp[$READ_STATE_INDEX]="STOPPED"
            echo "${rsp[@]}" > $rsp_file
            send_inventory_complete_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
        else
            send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID
            rsp[$READ_STATE_INDEX]="STARTED"
            echo "${rsp[@]}" > $rsp_file
        fi
    elif [ "$METHOD" = '"method":"get_sw_version"}' ]; then
        send_sw_version_response ${rsp[$DEVICE_ID_INDEX]} $ID

    elif [ "$METHOD" = '"method":"get_state"}' ]; then
        send_state_response ${rsp[$DEVICE_ID_INDEX]} $index $ID

    elif [ "$METHOD" = '"method":"get_bist_results"}' ]; then
        TIME=$(($(date +%s%N)/1000000))
        uptime=$(($TIME-$START_TIME))
        send_bist_results_response ${rsp[$DEVICE_ID_INDEX]} $uptime $ID

    elif [ "$METHOD" = '"method":"set_led"' ]; then
        # Do nothing
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID

    elif [ "$METHOD" = '"method":"set_motion_event"' ]; then
        # Do nothing
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID

    elif [ "$METHOD" = '"method":"set_device_alert"' ]; then
        # Extract the alert number
        IFS=’:’ read -ra ARRAY <<< "${CMD[3]}"
        number=${ARRAY[2]}
        if [ $number -eq $RfModuleError ]; then
            description="RfModuleError"
            option='{"string":"code","number":769}'
        elif [ $number -eq $HighAmbientTemp ]; then
            description="HighAmbientTemp"
            option='{"string":"degrees","number":'$AMBI_TEMP'}'
        elif [ $number -eq $HighCpuTemp ]; then
            description="HighCpuTemp"
            option='{"string":"degrees","number":'$HCPU_TEMP'}'
        elif [ $number -eq $HighCpuUsage ]; then
            description="HighCpuUsage"
            option='{"string":"percent","number":'$CPU_USAGE'}'
        elif [ $number -eq $HighMemoryUsage ]; then
            description="HighMemoryUsage"
            option='{"string":"percent","number":'$MEM_USAGE'}'
        elif [ $number -eq $DeviceMoved ]; then
            description="DeviceMoved"
            option="{ }"
        else
            description="UnknownError"
            option="{ }"
        fi    
        # Extract the alert severity
        IFS=’:’ read -ra ARRAY <<< "${CMD[4]}"
        severity=${ARRAY[1]}
        severity="${severity%\"}"
        severity="${severity#\"}"
        # Extract the alert threshold
        IFS=’:’ read -ra ARRAY <<< "${CMD[5]}"
        threshold=${ARRAY[1]}
        IFS=’}’ read -ra ARRAY <<< "$threshold"
        threshold=${ARRAY[0]}
        # Send the command response
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID
        sleep 1
        send_device_alert_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} $number $description $severity $option

    elif [ "$METHOD" = '"method":"set_facility_id"' ]; then
        # Extract the new facility_id
        FACILITY=${CMD[3]}
        IFS=’\"’ read -ra FAC <<< "$FACILITY"
        FACID=${FAC[3]}
        rsp[$FACILITY_ID_INDEX]=$FACID
        echo "${rsp[@]}" > $rsp_file
        # Send the command response
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID
    fi
}

# Function takes rsp index as an argument
generate_tag_reads_from_file () {
    index=$1
    rsp_file="$RSP_FILE_BASE""$index"
    tag_file="$TAG_FILE_BASE""$index"

    # Check to make sure these files exist
    if [[ -f $rsp_file && -f $tag_file ]]; then
        # Loop forever
        while [ 0 -lt 1 ]; do
            # Get the state of this rsp
            rsp=($(cat $rsp_file))
            if [ "${rsp[$READ_STATE_INDEX]}" == "STARTED" ]; then
                tags=($(cat $tag_file))
                for tag in "${tags[@]}"
                do
                    send_inventory_data_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} $tag
                done
            fi
            sleep 1
        done
    fi
}


#
# Here is where the fun begins
#

# Get the root certificate
RAW=$(curl --request GET $ROOT_CERT_URL)
IFS=’\"’ read -ra ARRAY <<< "$RAW"
ROOT_CERT=${ARRAY[3]}
if [ "$ROOT_CERT" = "" ]; then
    echo "Cannot access Root Cert REST endpoint!"
    exit 1
fi

echo "Creating default rsp data..." 
index=0
while [ $index -lt $RSPS ]; do
    rsp_file="$RSP_FILE_BASE""$index"
    if [ ! -f $rsp_file ]; then
        rsp[$DEVICE_ID_INDEX]="RSP-$(($HOST_BASE+$index))"
        rsp[$FACILITY_ID_INDEX]="UNKNOWN"
        rsp[$READ_STATE_INDEX]="STOPPED"
        rsp[$TOKEN_INDEX]=$DEFAULT_TOKEN
        echo "${rsp[@]}" > $rsp_file
    fi
    let index=index+1
done

# Create a tags_in_view file for each rsp
index=0
while [ $index -lt $RSPS ]; do
    FILE="tags_in_view_of_rsp_$index"
    touch $FILE
    let index=index+1
done


# This loop connects the RSPs to the Gateway
# assuming the Gateway is already running.
index=0
while [ $index -lt $RSPS ]; do
    rsp_file="$RSP_FILE_BASE""$index"
    rsp=($(cat $rsp_file))
    get_mqtt_credentials ${rsp[$DEVICE_ID_INDEX]} ${rsp[$TOKEN_INDEX]}
    if [ "$MQTT_BROKER" = "" ]; then
        echo "Invalid MQTT Broker address!"
        exit 1
    fi
    wait_for_connect_response $index &
    send_connect_request ${rsp[$DEVICE_ID_INDEX]} $index
    send_status_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
    let index=index+1
done


# This loop starts a command listener
# in a separate process for each rsp.
index=0
while [ $index -lt $RSPS ]; do
    wait_for_command $index &
    let index=index+1
done


# This loop starts a tag read generator
# in a separate process for each rsp.
index=0
while [ $index -lt $RSPS ]; do
    generate_tag_reads_from_file $index &
    let index=index+1
done
echo "Press CTRL-C to disconnect."


# This loop sends heartbeat_indications
# to the Gateway every 30 seconds, forever.
while [ 0 -lt 1 ]; do
    index=0
    while [ $index -lt $RSPS ]; do
        rsp_file="$RSP_FILE_BASE""$index"
        rsp=($(cat $rsp_file))
        send_heartbeat_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
        let index=index+1
    done
    sleep 30
done
