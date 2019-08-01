#!/bin/bash
#
# Copyright (c) 2019 Intel Corporation
# SPDX-License-Identifier: BSD-3-Clause 
#

# Set the number of RSPs to simulate
if [ "$#" -lt 1 ]; then
    echo
    echo "This script simulates the API messages between the specified number of"
    echo "Intel RFID Sensor Platforms (RSP) and the Intel RSP SW Toolkit - RSP Controller."
    echo
    echo "Usage: rsp-sim.sh <count> [read_percent]"
    echo "where <count> is the number of RSP's to simulate."
    echo "where [read_percent] is the optional percent (1-100) of tag population to be read per inventory_data packet (500 ms). default: 100"
    echo
    echo "This script depends on the mosquitto-clients package being installed."
    echo "Run 'sudo apt install mosquitto-clients' to install."
    echo
    echo "NOTE: The Intel RSP SW Toolkit - RSP Controller must be running BEFORE"
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

READ_PERCENT=$2
if [ -z $READ_PERCENT ] || [ $READ_PERCENT -gt 100 ]; then
    echo "Setting READ_PERCENT to 100."
    READ_PERCENT=100
fi

# Customizable options
QUIET=${QUIET:-0}
DEBUG=${DEBUG:-0}
QOS=${QOS:-1}
COLOR=${COLOR:-1}
STYLE=${STYLE:-1}
RSP_CONTROLLER_IP="${RSP_CONTROLLER_IP:-127.0.0.1}"

DEVICE_ID_INDEX=0
FACILITY_ID_INDEX=1
READ_STATE_INDEX=2
TOKEN_INDEX=3
DEFAULT_TOKEN="D544DF3F42EA86BED3C3D15FC321B8E949D666C06B008C6357580BC3816E00DE"
ROOT_CERT_URL="http://$RSP_CONTROLLER_IP:8080/provision/root-ca-cert"
MQTT_CRED_URL="https://$RSP_CONTROLLER_IP:8443/provision/sensor-credentials"
MQTT_BROKER=$RSP_CONTROLLER_IP
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

# Create a lookup map of RSP colors for logging: map[device_id] = color
declare -A rsp_colors

# Only define colors if COLOR is enabled (1)
if [[ $COLOR -eq 1 ]]; then
    # clear resets color and style back to defaults
    clear="\e[0m"
    # no_color clears the foreground color
    no_color="\e[39m"
    # normal colors
    red="\e[31m"; green="\e[32m"; yellow="\e[33m"; blue="\e[34m"; magenta="\e[35m"; cyan="\e[36m"
    # light colors
    lt_red="\e[91m"; lt_green="\e[92m"; lt_yellow="\e[93m"; lt_blue="\e[94m"; lt_magenta="\e[95m"; lt_cyan="\e[96m"
fi

# Only define styles if STYLE is enabled (1):wq
if [[ $STYLE -eq 1 ]]; then
    # clear resets color and style back to defaults
    clear="\e[0m"
    # normal clears dim, bold, and underline
    normal="\e[22;24m"
    # styles
    dim="\e[2m"; bold="\e[1m"; uline="\e[4m"
fi

#
# Define all the functions up front
#

# Function takes device_id and token as arguments
get_mqtt_credentials () {
    TIME=$(($(date +%s%N)/1000000))
    echo "$1 requesting mqtt credentials from $MQTT_CRED_URL"
    RAW=$(curl --insecure --progress-bar --header "Content-type: application/json" --request POST --data '{"username":"'$1'","token":"'$2'","generatedTimestamp":'$TIME',"expirationTimestamp":-1}' $MQTT_CRED_URL)
    if [ "$RAW" = "" ]; then
        echo "Cannot access MQTT Credentials REST endpoint!"
        exit 1
    fi
    IFS='/' read -ra ARRAY <<< "$RAW"
    RAW=${ARRAY[2]}
    IFS=':' read -ra ARRAY <<< "$RAW"
    MQTT_BROKER=${ARRAY[0]}
}

# Function takes topic and message string as arguments
publish () {
    log_debug "[publish] topic: $1, msg: $2"
    mosquitto_pub -q $QOS -h $MQTT_BROKER -t "$1" -m "$2"
}

# Function takes topic and filename as arguments
publish_file () {
    log_debug "[publish] topic: $1, len: $(wc --bytes < "$2") bytes"
    mosquitto_pub -q $QOS -h $MQTT_BROKER -t "$1" -f "$2"
}

# Function takes debug message as argument
log_debug () {
    if [ $DEBUG -eq 1 ]; then
        printf "${dim}[$(date '+%x %X')] [DEBUG] %s${clear}\n" "$1"
    fi
}

# Function takes device_id and message as argument
log_warning () {
    if [ $QUIET -ne 1 ]; then
        printf "${dim}[$(date '+%x %X')]${normal} ${bold}${rsp_colors[$1]}%s${clear} ${yellow}[WARNING] %s${clear}\n" "$1" "$2"
    fi
}

# Function takes device_id, jsonrpc id, jsonrpc method and optional extra message as arguments
log_send_response () {
    if [ $QUIET -ne 1 ]; then
        printf "${dim}[$(date '+%x %X')]${normal} ${bold}${rsp_colors[$1]}%s${clear} %-10.10s ${bold}--->>${normal} ${uline}%-20s${normal} ${dim}%s${clear}\n" "$1" "id: $2" "$3" "${4:-// response}"
    fi
}

# Function takes device_id, jsonrpc method and optional extra message as arguments
log_send_msg () {
    if [ $QUIET -ne 1 ]; then
        printf "${dim}[$(date '+%x %X')]${normal} ${bold}${rsp_colors[$1]}%s${clear} %-10.10s ${dim}--->>${normal} ${uline}%-20s${normal} ${dim}%s${clear}\n" "$1" " " "$2" "$3"
    fi
}

# Function takes device_id, jsonrpc id, jsonrpc method and optional extra message as arguments
log_receive_msg () {
    if [ $QUIET -ne 1 ]; then
        printf "${dim}[$(date '+%x %X')]${normal} ${bold}${rsp_colors[$1]}%s${clear} ${bold}<<---${normal} %10.10s ${uline}%-20s${normal} ${dim}%s${clear}\n" "$1" "id: $2" "$3" "${4:-// request}"
    fi
}

# Function takes device_id and rsp index as arguments
send_connect_request () {
    log_send_msg "$1" "connect"
    publish "rfid/rsp/connect" '{"jsonrpc":"2.0","id":"1","method":"connect","params":{"hostname":"'$1'","hwaddress":"98:4f:ee:15:00:'$2'","app_version":"19.7.sim","module_version":"none","num_physical_ports":2,"motion_sensor":true,"camera":false,"wireless":false,"configuration_state":"unknown","operational_state":"unknown"}}'
}

# Function takes device_id, facility_id and status as arguments
send_status_indication () {
    TIME=$(($(date +%s%N)/1000000))
    log_send_msg "$1" "status_update" "// $3"
    publish "rfid/rsp/rsp_status/$1" '{"jsonrpc":"2.0","method":"status_update","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'","status":"'$3'"}}'
}

# Function takes device_id and facility_id as arguments
send_heartbeat_indication () {
    TIME=$(($(date +%s%N)/1000000))
    log_send_msg "$1" "heartbeat"
    publish "rfid/rsp/rsp_status/$1" '{"jsonrpc":"2.0","method":"heartbeat","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'","location":null,"video_url":null}}'
}

# Function takes device_id, facility_id, alert_number, alert_description, serverity and optional as arguments
send_device_alert_indication () {
    TIME=$(($(date +%s%N)/1000000))
    log_send_msg "$1" "device_alert"
    publish "rfid/rsp/rsp_status/$1" '{"jsonrpc":"2.0","method":"device_alert","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'","alert_number":'$3',"alert_description":"'$4'","severity":"'$5'","optional":'$6'}}'
}

# Function takes device_id and facility_id as arguments
send_inventory_complete_indication () {
    TIME=$(($(date +%s%N)/1000000))
    log_send_msg "$1" "inventory_complete"
    publish "rfid/rsp/rsp_status/$1" '{"jsonrpc":"2.0","method":"inventory_complete","params":{"sent_on":'$TIME',"device_id":"'$1'","facility_id":"'$2'"}}'
}

# Function takes device_id, facility_id, tag count and tagdata as arguments
# tagdata is an array of json epc read objects without the beginning and ending [ ]
# example: {"epc":"1234ABC","tid":null,"antenna_id":0,"last_read_on":123456,"rssi":-654,"phase":32,"frequency":915250},{...},{...}
send_inventory_data_indication () {
    TIME=$(($(date +%s%N)/1000000))
    log_send_msg "$1" "inventory_data" "// $3 tags | $2"
    # tag data may be too large to pass over command line and should be stored in a temp file
    tmpfile=`mktemp`
    echo -n '{"jsonrpc":"2.0","method":"inventory_data","params":{"sent_on":'$TIME',"period":500,"device_id":"'$1'","facility_id":"'$2'","location":null,"motion_detected":true,"data":['$4']}}' > $tmpfile
    publish_file "rfid/rsp/data/$1" "$tmpfile"
    rm -f $tmpfile
}

# Function takes device_id, request id, request method, and optional log string as arguments
send_command_response () {
    log_send_response "$1" "$2" "$3" "$4"
    publish "rfid/rsp/response/$1" '{"jsonrpc":"2.0","result":true,"id":"'$2'"}'
}

# Function takes device_id, request id and request method as arguments
send_bist_results_response () {
    log_send_response "$1" "$3" "$4"
    publish "rfid/rsp/response/$1" '{"jsonrpc":"2.0","result":{"rf_module_error":false,"rf_status_code":0,"rf_module_temp":'$RFID_TEMP',"ambient_temp":'$AMBI_TEMP',"time_alive":'$2',"cpu_usage":'$CPU_USAGE',"mem_used_percent":'$MEM_USAGE',"mem_total_bytes":'$MEM_TOTAL',"camera_installed":false,"temp_sensor_installed":true,"accelerometer_installed":true,"region":"USA","rf_port_statuses":[{"port":0,"forward_power_dbm10":280,"reverse_power_dbm10":0,"connected":true},{"port":0,"forward_power_dbm10":280,"reverse_power_dbm10":0,"connected":true}],"device_moved":false},"id":"'$3'"}'
}

# Function takes device_id, request id and request method as arguments
send_state_response () {
    log_send_response "$1" "$3" "$4"
    publish "rfid/rsp/response/$1" '{"jsonrpc":"2.0","result":{"device_id":"'$1'","hwaddress":"98:4f:ee:15:00:'$2'","app_version":"19.1.sim","module_version":"none","num_physical_ports":2,"motion_sensor":true,"camera":false,"wireless":false,"configuration_state":"unknown","operational_state":"unknown"},"id":"'$3'"}'
}

# Function takes device_id, request id and request method as arguments
send_sw_version_response () {
    log_send_response "$1" "$2" "$3"
    publish "rfid/rsp/response/$1" '{"jsonrpc":"2.0","result":{"app_version":"19.7.sim","module_version":"none"},"id":"'$2'"}'
}

# Function takes rsp index as an argument
wait_for_connect_response () {
    index=$1
    eval "$(rsp_array ${index})"

    MSG=$(mosquitto_sub -h $MQTT_BROKER -t rfid/rsp/connect/${rsp[$DEVICE_ID_INDEX]} -C 1 -q $QOS)
    log_debug "[receive] topic: rfid/rsp/connect/${rsp[$DEVICE_ID_INDEX]}, msg: $MSG"
    # Split the message into individual parameters
    IFS=',' read -ra SP1 <<< "$MSG"
    # Parse out the facility_id
    IFS=':' read -ra SP2 <<< "${SP1[2]}"
    FACID="${SP2[2]}"
    FACID="${FACID%\"}"
    FACID="${FACID#\"}"
    # and store it in the rsp array
    set_rsp_field $index $FACILITY_ID_INDEX $FACID
}

# Function takes rsp index as an argument
wait_for_command () {
    index=$1

    # Loop forever
    while true; do
        eval "$(rsp_array ${index})"
        # Wait for the MQTT command (block)
        CMD=$(mosquitto_sub -h $MQTT_BROKER -t rfid/rsp/command/${rsp[$DEVICE_ID_INDEX]} -C 1 -q $QOS)
        log_debug "[receive] topic: rfid/rsp/command/${rsp[$DEVICE_ID_INDEX]}, msg: $CMD"
        process_command $index $CMD &
    done
}

# Function takes rsp index and command as arguments
process_command () {
    index=$1
    eval "$(rsp_array ${index})"

    # Split the command into individual parameters
    IFS=',' read -ra CMD <<< "$2"
#    printf '%s\n' "${CMD[@]}"

    # Extract the id
    ID=${CMD[1]}
    # parse out the actual id value
    ID=`sed -r 's/"id":\s*"([^"]+)"\s*}*/\1/g' <<< $ID`
    # Extract the method
    METHOD=${CMD[2]}
    # parse out the actual method value
    METHOD=`sed -r 's/"method":\s*"([^"]+)"\s*}*/\1/g' <<< $METHOD`

    log_receive_msg ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD

    if [ "$METHOD" = "apply_behavior" ]; then
        ACTION=${CMD[3]}
        REPEAT=${CMD[26]}
        if [ "$ACTION" = '"params":{"action":"STOP"' ]; then
            send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD
            set_rsp_field $index $READ_STATE_INDEX "STOPPED"
            send_inventory_complete_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
        elif [ "$REPEAT" = '"auto_repeat":false' ]; then
            send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD
            set_rsp_field $index $READ_STATE_INDEX "STARTED"
            DUR=${CMD[19]}
            IFS=':' read -ra MILLIS <<< "$DUR"
            sleep $((${MILLIS[1]} / 1000))
            set_rsp_field $index $READ_STATE_INDEX "STOPPED"
            send_inventory_complete_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
        else
            send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD
            set_rsp_field $index $READ_STATE_INDEX "STARTED"
        fi
    elif [ "$METHOD" = "get_sw_version" ]; then
        send_sw_version_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD

    elif [ "$METHOD" = "get_state" ]; then
        send_state_response ${rsp[$DEVICE_ID_INDEX]} $index $ID $METHOD

    elif [ "$METHOD" = "get_bist_results" ]; then
        TIME=$(($(date +%s%N)/1000000))
        uptime=$(($TIME-$START_TIME))
        send_bist_results_response ${rsp[$DEVICE_ID_INDEX]} $uptime $ID $METHOD

    elif [ "$METHOD" = "set_led" ]; then
        # Do nothing
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD

    elif [ "$METHOD" = "set_motion_event" ]; then
        # Do nothing
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD

    elif [ "$METHOD" = "set_device_alert" ]; then
        # Extract the alert number
        IFS=':' read -ra ARRAY <<< "${CMD[3]}"
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
        IFS=':' read -ra ARRAY <<< "${CMD[4]}"
        severity=${ARRAY[1]}
        severity="${severity%\"}"
        severity="${severity#\"}"
        # Extract the alert threshold
        IFS=':' read -ra ARRAY <<< "${CMD[5]}"
        threshold=${ARRAY[1]}
        IFS='}' read -ra ARRAY <<< "$threshold"
        threshold=${ARRAY[0]}
        # Send the command response
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD
        sleep 1
        send_device_alert_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} $number $description $severity $option

    elif [ "$METHOD" = "set_facility_id" ]; then
        # Extract the new facility_id
        FACILITY=${CMD[3]}
        IFS='\"' read -ra FAC <<< "$FACILITY"
        FACID=${FAC[3]}
        set_rsp_field $index $FACILITY_ID_INDEX $FACID
        # Send the command response
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD

    elif [ "$METHOD" = "reset" ]; then
        # Send the command response
        send_command_response ${rsp[$DEVICE_ID_INDEX]} $ID $METHOD
        # Alert of the in_reset state
        send_status_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} "in_reset"
        # wait some time, pretend we are resetting
        sleep 15
        # send status of ready
        send_status_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} "ready"

    else
        log_warning "${rsp[$DEVICE_ID_INDEX]}" "Received request with unhandled method: ${METHOD}"
    fi
}

# Function takes rsp index, field index and field value as arguments
set_rsp_field() {
    rsp_file="${RSP_FILE_BASE}${1}"

    if [[ `type -P flock` ]]; then
        # write the changes using an exclusive lock
        flock --exclusive --no-fork ${rsp_file} -c "rsp=(\$(cat ${rsp_file})); rsp[${2}]=${3}; echo \"\${rsp[@]}\" > ${rsp_file}"
    else
        # write the changes un-synchronized
       local rsp=($(cat ${rsp_file}))
       rsp[${2}]=${3}
       echo "${rsp[@]}" > ${rsp_file}
    fi
}

# Function takes rsp_file as argument
# Returns array declaration for use with eval which when called will define an array called `rsp`
# example: eval "$(rsp_array ${index})"
rsp_array () {
    rsp_file="${RSP_FILE_BASE}${1}"

    if [[ `type -P flock` ]]; then
        # read the contents using a shared lock
        local rsp=($(flock --shared --no-fork ${rsp_file} -c "cat ${rsp_file}"))
    else
        # read the contents un-synchronized
        local rsp=($(cat ${rsp_file}))
    fi
    
    # print it out to be consumed by eval
    declare -p rsp
}

# Function takes rsp index as an argument
generate_tag_reads_from_file () {
    index=$1
    tag_file="$TAG_FILE_BASE""$index"

    # Check to make sure these files exist
    if [[ -f $rsp_file && -f $tag_file ]]; then
        # Loop forever
        while true; do
            # Get the state of this rsp
            eval "$(rsp_array ${index})"
            if [ "${rsp[$READ_STATE_INDEX]}" == "STARTED" ]; then
                TIME=$(($(date +%s%N)/1000000))
                tag_count=`wc -w < $tag_file`
                reads=$(($tag_count * $READ_PERCENT / 100))

                if [[ $reads -eq 0 ]]; then
                    log_warning "${rsp[$DEVICE_ID_INDEX]}" "No tags available to read for sensor"
                else
                    tagdata=`shuf -n $reads -e $(cat $tag_file) \
                          | paste -s -d ',' \
                          | sed -r 's/([0-9a-fA-F]+)/{"epc":"\1","tid":null,"antenna_id":0,"last_read_on":'$TIME',"rssi":-654,"phase":32,"frequency":915250}/g'`
                    send_inventory_data_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} $reads $tagdata &
                fi
            fi
            sleep 0.5
        done
    fi
}


#
# Here is where the fun begins
#

# Get the root certificate
echo "requesting root certificate from $ROOT_CERT_URL"
RAW=$(curl --progress-bar --request GET $ROOT_CERT_URL)
IFS='\"' read -ra ARRAY <<< "$RAW"
ROOT_CERT=${ARRAY[3]}
if [ "$ROOT_CERT" = "" ]; then
    echo "Cannot access Root Cert REST endpoint!"
    exit 1
fi

echo "Creating default rsp data..."
index=0
while [ $index -lt $RSPS ]; do
    rsp_file="${RSP_FILE_BASE}${index}"
    device_id="RSP-$(($HOST_BASE+$index))"

    if [ $COLOR -ne 1 ]; then
        rsp_colors[$device_id]=""
    else
        # compute a color for each rsp
        # starting with light colors (91-96)
        # and then use regular colors (31-36)  // 91-60 = 31 :)
        # once all available colors used, start over
        rsp_colors[$device_id]="\e[$(( 91 + ($index % 6) - (60 * (($index / 6) % 2)) ))m"
    fi

    if [ ! -f $rsp_file ]; then
        # the file does not exist yet, so do not care about using flock
        rsp[$DEVICE_ID_INDEX]=$device_id
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


# This loop connects the RSPs to the RSP Controller
# assuming the RSP Controller is already running.
index=0
while [ $index -lt $RSPS ]; do
    eval "$(rsp_array ${index})"
    get_mqtt_credentials ${rsp[$DEVICE_ID_INDEX]} ${rsp[$TOKEN_INDEX]}
    if [ "$MQTT_BROKER" = "" ]; then
        echo "Invalid MQTT Broker address!"
        exit 1
    fi
    wait_for_connect_response $index &
    send_connect_request ${rsp[$DEVICE_ID_INDEX]} $index
    send_status_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} "ready"
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

if [ $QUIET -ne 1 ]; then
    printf "\n\n${bold}"
    printf "******************************************\n"
    printf "*     ${lt_green}Connected to RSP Controller${no_color}        *\n"
    printf "******************************************\n"
    printf "*                                        *\n"
    printf "*     ${lt_red}Press CTRL-C to disconnect${no_color}         *\n"
    printf "*                                        *\n"
    printf "******************************************\n"
    printf "${clear}\n\n"
fi

# This will cleanly shutdown the simulators and send rsp_status of shutting_down
clean_shutdown() {
    trap - INT
    printf "\n${lt_red}${bold}Shutting down sensors...${clear}\n"

    # This loop sends shutting_down rsp_status for each rsp and if the sensor
    # is in the STARTED state it will send an inventory_complete and set the state to STOPPED
    index=0
    while [ $index -lt $RSPS ]; do
        eval "$(rsp_array ${index})"

        # Stop any currently running read cycles
        if [ "${rsp[$READ_STATE_INDEX]}" == "STARTED" ]; then
            send_inventory_complete_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
            set_rsp_field $index $READ_STATE_INDEX "STOPPED"
        fi

        # Let RSP Controller know we are shutting down
        send_status_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]} "shutting_down"

        let index=index+1
    done

    exit 0
}

# close connections when Ctrl-C is pressed
trap clean_shutdown INT

# This loop sends heartbeat_indications
# to the RSP Controller every 30 seconds, forever.
while true; do
    index=0
    while [ $index -lt $RSPS ]; do
        eval "$(rsp_array ${index})"
        send_heartbeat_indication ${rsp[$DEVICE_ID_INDEX]} ${rsp[$FACILITY_ID_INDEX]}
        let index=index+1
    done
    sleep 30
done
