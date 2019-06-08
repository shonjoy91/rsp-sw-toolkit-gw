#!/bin/bash
#
# Copyright (c) 2019 Intel Corporation
# SPDX-License-Identifier: BSD-3-Clause 
#

if [ "$#" -ne 1 ]; then
    echo
    echo "This script is used to simulate the messaging of a remote GPIO device"
    echo "that is used in conjunction with the Intel RSP SW Toolkit - Gateway."
    echo "When using this script on a platform with actual GPIO pins, you must"
    echo "edit the platform specific variables first to access the GPIO pins."
    echo
    echo "Usage: rem-gpio.sh <address>"
    echo "where <address> is IP address or FQDN of the Gateway."
    echo
    echo "This script depends on the mosquitto-clients package being installed."
    echo "Run 'sudo apt install mosquitto-clients' to install."
    echo
    echo "NOTE: The Intel RSP SW Toolkit - Gateway must be running BEFORE"
    echo "      attempting to execute this script."
    echo
    exit 1
fi

GATEWAY_IP=$1
HOSTNAME=$(cat /etc/hostname)
VERSION="1.0"

# 
# PLATFORM SPECIFIC VARIABLES
#     DEVICE_ID -
#     IFACE -
#     GPIO_PATH
#     GPIO_COUNT,
#     GPIO_INPUT_COUNT
#     GPIO[x] - 
#
DEVICE_ID="remote-gpio"
IFACE="enp0s3"
HWADDRESS=$(cat /sys/class/net/$IFACE/address)
GPIO_PATH_HW="/sys/class/gpio/"
GPIO_PATH_SIM="./gpio/"
GPIO_PATH=$GPIO_PATH_SIM
GPIO_COUNT=5
GPIO_INPUT_COUNT=2
GPIO[0]="19"
GPIO[1]="16"
GPIO[2]="26"
GPIO[3]="20"
GPIO[4]="21"
OUTPUT_COUNT=$(($GPIO_COUNT - $GPIO_INPUT_COUNT))

# 
# SYSTEM SPECIFIC SETTINGS
#
DEFAULT_TOKEN="D544DF3F42EA86BED3C3D15FC321B8E949D666C06B008C6357580BC3816E00DE"
ROOT_CERT_URL="http://$GATEWAY_IP:8080/provision/root-ca-cert"
MQTT_CRED_URL="https://$GATEWAY_IP:8443/provision/sensor-credentials"
MQTT_BROKER=$GATEWAY_IP
COMMAND_TOPIC="rfid/gpio/command/$DEVICE_ID"
CONNECT_TOPIC="rfid/gpio/connect"
RESPONSE_TOPIC="rfid/gpio/response/$DEVICE_ID"
STATUS_TOPIC="rfid/gpio/status/$DEVICE_ID"
START_TIME=$(($(date +%s%N)/1000000))
ASSERTED=0
DEASSERTED=1


#
# Create the GPIO_INFO JSON
#
INDEX=0
GPIO_INFO="["
while [ $INDEX -lt $GPIO_COUNT ]; do

    if [ $INDEX -lt $GPIO_INPUT_COUNT ]; then
        DIRECTION="INPUT"
    else
        DIRECTION="OUTPUT"
    fi

    GPIO_INFO=$GPIO_INFO'{"index":'$INDEX',"name":"gpio'${GPIO[$INDEX]}'","state":"DEASSERTED","direction":"'$DIRECTION'"}'

    let INDEX=INDEX+1
    if [ $INDEX -lt $GPIO_COUNT ]; then
        GPIO_INFO=$GPIO_INFO","
    else
        GPIO_INFO=$GPIO_INFO"]"
    fi

done

#
# Define all the functions up front
#

get_mqtt_credentials () {
    TIME=$(($(date +%s%N)/1000000))
    RAW=$(curl --insecure --header "Content-type: application/json" --request POST --data '{"username":"'$DEVICE_ID'","token":"'$DEFAULT_TOKEN'","generatedTimestamp":'$TIME',"expirationTimestamp":-1}' $MQTT_CRED_URL)
    if [ "$RAW" = "" ]; then
        echo "Cannot access MQTT Credentials REST endpoint!"
        exit 1
    fi
    IFS=’/’ read -ra ARRAY <<< "$RAW"
    RAW=${ARRAY[2]}
    IFS=’:’ read -ra ARRAY <<< "$RAW"
    MQTT_BROKER=${ARRAY[0]}
}

send_connect_request () {
    mosquitto_pub -h $MQTT_BROKER -t $CONNECT_TOPIC -m '{"jsonrpc":"2.0","id":"1","method":"gpio_connect","params":{"device_id":"'$DEVICE_ID'","hwaddress":"'$HWADDRESS'","app_version":"'$VERSION'","gpio_info":'$GPIO_INFO'}}'
}

# Function takes GPIOInfo as an argument
send_input_notification () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t $STATUS_TOPIC -m '{"jsonrpc":"2.0","method":"gpio_input","params":{"sent_on":'$TIME',"device_id":"'$DEVICE_ID'","gpio_info":'$1'}}'
}

send_heartbeat_indication () {
    TIME=$(($(date +%s%N)/1000000))
    mosquitto_pub -h $MQTT_BROKER -t $STATUS_TOPIC -m '{"jsonrpc":"2.0","method":"heartbeat","params":{"sent_on":'$TIME',"device_id":"'$DEVICE_ID'","facility_id":"UNKNOWN","location":null,"video_url":null}}'
}

# Function takes "id":id as arguments
send_command_response () {
    mosquitto_pub -h $MQTT_BROKER -t $RESPONSE_TOPIC -m '{"jsonrpc":"2.0","result":true,'$1'}'
}

wait_for_connect_response () {

    MSG=$(mosquitto_sub -h $MQTT_BROKER -t $CONNECT_TOPIC/$DEVICE_ID -C 1)
    # Nothing to do with this message
}

wait_for_command () {
    # Loop forever
    while [ 0 -lt 1 ]; do
        # Wait for the MQTT command (block)
        CMD=$(mosquitto_sub -h $MQTT_BROKER -t $COMMAND_TOPIC -C 1)
        process_command $CMD &
    done
}

# Function takes a command as an argument
process_command () {
    # Split the command into individual parameters
    IFS=’,’ read -ra CMD <<< "$1"
#    printf '%s\n' "${CMD[@]}"

    # Extract the id
    ID=${CMD[1]}
    # Extract the method
    METHOD=${CMD[2]}

    if [ "$METHOD" = '"method":"gpio_set_state"' ]; then
        # Extract the index
        IFS=’:’ read -ra ARRAY <<< "${CMD[3]}"
        index=${ARRAY[2]}
        # Extract the state
        IFS=’:’ read -ra ARRAY <<< "${CMD[5]}"
        state=${ARRAY[1]}
        if [ "$state" = '"ASSERTED"' ]; then
            echo $ASSERTED > $GPIO_PATH"gpio"${GPIO[$index]}"/value"
        fi
        if [ "$state" = '"DEASSERTED"' ]; then
            echo $DEASSERTED > $GPIO_PATH"gpio"${GPIO[$index]}"/value"
        fi
        # Send the command response
        send_command_response $ID
    fi
}

init_gpio () {
    if [ "$GPIO_PATH" = "$GPIO_PATH_SIM" ]; then
        create_sim_directories
    fi
    INDEX=0
    while [ $INDEX -lt $GPIO_COUNT ]; do
        echo ${GPIO[$INDEX]} > $GPIO_PATH"export"
        if [ $INDEX -lt $GPIO_INPUT_COUNT ]; then
            echo "in" > $GPIO_PATH"gpio"${GPIO[$INDEX]}"/direction"
        else
            echo "out" > $GPIO_PATH"gpio"${GPIO[$INDEX]}"/direction"
            echo $DEASSERTED > $GPIO_PATH"gpio"${GPIO[$INDEX]}"/value"
        fi
        let INDEX=INDEX+1
    done
}

close_gpio () {
    INDEX=0
    while [ $INDEX -lt $GPIO_COUNT ]; do
        echo ${GPIO[$INDEX]} > $GPIO_PATH"unexport"
        let INDEX=INDEX+1 
    done
}

create_sim_directories () {
    echo "Creating simulated GPIO diretories..." 
    mkdir $GPIO_PATH_SIM
    index=0
    while [ $index -lt $GPIO_COUNT ]; do
        mkdir $GPIO_PATH_SIM"gpio"${GPIO[$index]}
        echo $DEASSERTED > $GPIO_PATH_SIM"gpio"${GPIO[$index]}"/value"
        let index=index+1
    done
}

get_state () {
    if [ "$1" = "$ASSERTED" ]; then
        STATE="ASSERTED"
    else
        STATE="DEASSERTED"
    fi
    while [ $INDEX -lt $GPIO_COUNT ]; do
        echo ${GPIO[$INDEX]} > $GPIO_PATH"unexport"
        let INDEX=INDEX+1
    done
}

poll_gpio_inputs () {

    INDEX=0
    while [ $INDEX -lt $GPIO_INPUT_COUNT ]; do
        CURRENT_STATE[$INDEX]=$(cat $GPIO_PATH"gpio"${GPIO[$INDEX]}"/value")
        let INDEX=INDEX+1
    done

    while [ 0 -lt 1 ]; do
        INDEX=0
        while [ $INDEX -lt $GPIO_INPUT_COUNT ]; do
            NEXT_STATE[$INDEX]=$(cat $GPIO_PATH"gpio"${GPIO[$INDEX]}"/value")
            if [ "${NEXT_STATE[$INDEX]}" != "${CURRENT_STATE[$INDEX]}" ]; then
            
                if [ "${NEXT_STATE[$INDEX]}" = "$ASSERTED" ]; then
                    STATE="ASSERTED"
                else
                    STATE="DEASSERTED"
                fi
                INFO='{"index":'$INDEX',"name":"gpio'${GPIO[$INDEX]}'","state":"'$STATE'","direction":"INPUT"}'
                send_input_notification $INFO

                CURRENT_STATE[$INDEX]=${NEXT_STATE[$INDEX]}
            fi
            let INDEX=INDEX+1
        done
        sleep 1
    done
}


#
# Here is where the fun begins
#

init_gpio

# Get the root certificate
echo "requesting root certificate from $ROOT_CERT_URL"
RAW=$(curl --request GET $ROOT_CERT_URL)
IFS=’\"’ read -ra ARRAY <<< "$RAW"
ROOT_CERT=${ARRAY[3]}
if [ "$ROOT_CERT" = "" ]; then
    echo "Cannot access Root Cert REST endpoint!"
    exit 1
fi

# Connect to the Gateway
# assuming the Gateway is already running.
get_mqtt_credentials
if [ "$MQTT_BROKER" = "" ]; then
    echo "Invalid MQTT Broker address!"
    exit 1
fi
wait_for_connect_response &
send_connect_request


# This loop starts a command listener
# in a separate process.
wait_for_command &


# This loop polls the input gpios
# in a separate process.
poll_gpio_inputs &


echo "Press CTRL-C to disconnect."

# This loop sends heartbeat_indications
# to the Gateway every 30 seconds, forever.
while [ 0 -lt 1 ]; do
    sleep 30
    send_heartbeat_indication
done
