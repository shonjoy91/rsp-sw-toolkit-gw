#!/bin/bash
#
# Copyright (c) 2019 Intel Corporation
# SPDX-License-Identifier: BSD-3-Clause 
#

MQTT_BROKER="127.0.0.1"
COMMAND_TOPIC="rfid/gw/command"
RESPONSE_TOPIC="rfid/gw/response"
ID="1"

ID=$(($ID + 1))
GET_DOWNSTREAM_SUMMARY='{"jsonrpc":"2.0","id":"'$ID'","method":"get_downstream_summary"}'

ID=$(($ID + 1))
GET_UPSTREAM_SUMMARY='{"jsonrpc":"2.0","id":"'$ID'","method":"get_upstream_summary"}'

ID=$(($ID + 1))
SENSOR_ID="RSP-150001"
DEVICE_ID="remote-gpio"
STATE="ASSERTED"
GPIO_INFO='{"index":2,"name":"gpio26","state":"'$STATE'","direction":"OUTPUT"}'
FUNCTION="SENSOR_TRANSMITTING"
MAPPING='{"sensor_id":"'$SENSOR_ID'","device_id":"'$DEVICE_ID'","gpio_info":'$GPIO_INFO',"function":"'$FUNCTION'"}'
SET_GPIO_MAPPING='{"jsonrpc":"2.0","id":"'$ID'","method":"set_gpio_mapping","params":'$MAPPING'}'

ID=$(($ID + 1))
CLEAR_GPIO_MAPPINGS='{"jsonrpc":"2.0","id":"'$ID'","method":"clear_gpio_mappings"}'

ID=$(($ID + 1))
REGX='"*BEEF*"'
#REGX=null
GET_TAGS='{"jsonrpc":"2.0","id":"'$ID'","method":"get_tags","params":'$REGX'}'

ID=$(($ID + 1))
RUN_STATE="ALL_SEQUENCED"
SCHEDULER_ACTIVATE='{"jsonrpc":"2.0","id":"'$ID'","method":"scheduler_activate","params":"'$RUN_STATE'"}'

ID=$(($ID + 1))
RUN_STATE="ALL_SEQUENCED"
SCHEDULER_DEACTIVATE='{"jsonrpc":"2.0","id":"'$ID'","method":"scheduler_deactivate"}'

ID=$(($ID + 1))
CLUSTER_1='{"id":"TwoPort","facility_id":"Facility2Ports","behavior_id":"ClusterDeepScan_PORTS_1","aliases":["DEVICE_ID","DEVICE_ID"],"sensor_groups":[["RSP-150002", "RSP-150003", "RSP-150004"]]}'
CLUSTER_2='{"id":"FourPort","facility_id":"Facility4Ports","behavior_id":"ClusterDeepScan_PORTS_1","aliases":["freezer","cooler"],"sensor_groups":[["RSP-150005"]]}'
CLUSTER_CONFIG='{"id":"SampleForJP","clusters":['$CLUSTER_1','$CLUSTER_2']}'
SET_CLUSTER_CONFIG='{"jsonrpc":"2.0","id":"'$ID'","method":"set_cluster_config","params":{"cluster_config":'$CLUSTER_CONFIG'}}'

ID=$(($ID + 1))
LED_STATE="Test"
SET_SENSOR_LED='{"jsonrpc":"2.0","id":"'$ID'","method":"set_sensor_led","params":{"led_state":"'$LED_STATE'"}}'

ID=$(($ID + 1))
GET_SENSOR_INFO='{"jsonrpc":"2.0","id":"'$ID'","method":"get_sensor_info"}'

ID=$(($ID + 1))
GET_SW_VERSION='{"jsonrpc":"2.0","id":"'$ID'","method":"get_sw_version"}'


echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_DOWNSTREAM_SUMMARY"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_DOWNSTREAM_SUMMARY"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_UPSTREAM_SUMMARY"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_UPSTREAM_SUMMARY"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SET_GPIO_MAPPING"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SET_GPIO_MAPPING"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$CLEAR_GPIO_MAPPINGS"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$CLEAR_GPIO_MAPPINGS"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_TAGS"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_TAGS"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SCHEDULER_ACTIVATE"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SCHEDULER_ACTIVATE"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SCHEDULER_DEACTIVATE"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SCHEDULER_DEACTIVATE"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SET_CLUSTER_CONFIG"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SET_CLUSTER_CONFIG"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SET_SENSOR_LED"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$SET_SENSOR_LED"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_SENSOR_INFO"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_SENSOR_INFO"
read -p 'Press [Enter] key to continue...'

echo
echo mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_SW_VERSION"
mosquitto_pub -h $MQTT_BROKER -t $COMMAND_TOPIC -m "$GET_SW_VERSION"
read -p 'Press [Enter] key to continue...'
