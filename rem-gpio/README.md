# rem-gpio (Remote GPIO)
A tool that exercises the Intel® RSP Software Toolkit Remote GPIO functionality

This tool is intended to aid the developer by providing simulated messaging from a Remote GPIO Device in order to demonstrate the API for that functionality.  This script can be modified to operate on any Linux platform.  When running on a platform that supports real GPIO (i.e. Intel® UP, BeagleBone or RaspberryPi), this script can be configured to access and control GPIO pins through their file descriptors. 


## Install the Intel® RSP Software Toolkit - RSP Controller Application

Follow the instructions here (https://github.com/intel/rsp-sw-toolkit-gw/) to install and run the Intel&reg; RSP Controller Application.


## Configure the Remote GPIO script

Before running the script, configure the platform specific variables.  The example variable list shown below illustrates the values used for a Raspberry Pi.
```
# 
# PLATFORM SPECIFIC VARIABLES
#     DEVICE_ID -
#     IFACE -
#     GPIO_PATH
#     GPIO_COUNT,
#     GPIO_INPUT_COUNT - 
#     GPIO[x] - 
#
DEVICE_ID=$HOSTNAME
IFACE="eth0"
HWADDRESS=$(cat /sys/class/net/$IFACE/address)
GPIO_PATH_HW="/sys/class/gpio/"
GPIO_PATH_SIM="./gpio/"
GPIO_PATH=$GPIO_PATH_HW
GPIO_COUNT=5
GPIO_INPUT_COUNT=2
GPIO[0]="19"
GPIO[1]="16"
GPIO[2]="26"
GPIO[3]="20"
GPIO[4]="21"
```
DEVICE_ID          : The name used to identify the GPIO device to the Intel&reg; RSP Controller Application.
IFACE              : The name of the network interface used to connect to the Intel&reg; RSP Controller Application.
GPIO_PATH_SIM      : The path to the simulated GPIO file descriptors. 
GPIO_COUNT         : The number of GPIO pins enumerated to the Intel&reg; RSP Controller Application.
GPIO_INPUT_COUNT=2 : The number of GPIO input pins enumerated to the Intel&reg; RSP Controller Application.
GPIO[x]            : The GPIO number for the specific hardware platform.
                   : The GPIO number array must list the inputs first.


### Install Dependencies

Additional dependencies need to be installed in order to use the RSP simulation script.
```
:~$ sudo apt install mosquitto-clients curl
```


### Run the Simulator

To simulate the presence of remote GPIO devices, execute the "rem-gpio.sh" script.  For help, execute the script with no arguments to see usage information.

```
:~$ ./rem-gpio.sh 

This script is used to simulate the messaging of a remote GPIO device
that is used in conjunction with the Intel® RSP SW Toolkit - RSP Controller Application.
When using this script on a platform with actual GPIO pins, you must
edit the platform specific variables first to access the GPIO pins.

Usage: rsp-sim.sh <address>
where <address> is IP address or FQDN of the computer running the Intel® RSP Controller Application.

This script depends on the mosquitto-clients package being installed.
Run 'sudo apt install mosquitto-clients' to install.

NOTE: The Intel® RSP SW Toolkit - RSP Controller must be running BEFORE
      attempting to execute this script.

:~$ ./rem-gpio.sh 127.0.0.1
Creating simulated GPIO diretories...
requesting root certificate from http://127.0.0.1:8080/provision/root-ca-cert
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100  2078  100  2078    0     0   338k      0 --:--:-- --:--:-- --:--:--  338k
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100   255  100    94  100   161   1620   2775 --:--:-- --:--:-- --:--:--  4396
Press CTRL-C to disconnect.
```


## Use the Command Line Interface (CLI)

Use the CLI to map the GPIO device pins to a specific function on the sensor.  Input functionalities (i.e. START_READING, STOP_READING) can only be mapped to GPIO input pins and output functionalities (i.e. SENSOR_CONNECTED, SENSOR_DISCONNECTED, SENSOR_READING_TAGS, SENSOR_TRANSMITTING) can only be mapped to GPIO output pins.

```
:~$ ssh -p5222 console@localhost
Password authentication
Password: console

RSP Controller console session

<tab> to view available commands
'clear' to clear the screen/console
'quit' to end

cli> sensor show
--------------------------------------------------------------------------------------------
device     connect      reading    behavior                  facility           personality  aliases

RSP-150000 CONNECTED    STOPPED    Default                   DEFAULT_FACILITY                [RSP-150000-0, RSP-150000-1, RSP-150000-2, RSP-150000-3]
--------------------------------------------------------------------------------------------
cli> gpio show.devices
--------------------------------------------------------------------------------------------
device       state        inputs       outputs     

remote-gpio  CONNECTED    2            3           
--------------------------------------------------------------------------------------------
cli> gpio show.device.info remote-gpio 
--------------------------------------------------------------------------------------------
index        name         state        direction   

0            gpio19       DEASSERTED   INPUT       
1            gpio16       DEASSERTED   INPUT       
2            gpio26       DEASSERTED   OUTPUT      
3            gpio20       DEASSERTED   OUTPUT      
4            gpio21       DEASSERTED   OUTPUT      
--------------------------------------------------------------------------------------------
cli> gpio map.gpio RSP-150000 remote-gpio 0 START_READING 
--------------------------------------------------------------------------------------------
OK
--------------------------------------------------------------------------------------------
cli> gpio map.gpio RSP-150000 remote-gpio 2 SENSOR_TRANSMITTING 
--------------------------------------------------------------------------------------------
OK
--------------------------------------------------------------------------------------------
cli> gpio show.mapping 
--------------------------------------------------------------------------------------------
sensor id    gpio id      gpio index   pin function            

RSP-150000   remote-gpio  0            START_READING           
RSP-150000   remote-gpio  2            SENSOR_TRANSMITTING     
--------------------------------------------------------------------------------------------
```


## Monitor the Downstream MQTT Topics for GPIO

The mosquitto-clients utilities can be used to monitor the downstream MQTT topics associated with GPIO commands. responses and notifications. 

```
:~$ mosquitto_sub -t rfid/gpio/#

{"jsonrpc":"2.0","id":"27","method":"gpio_connect","params":{"device_id":"remote-gpio","hwaddress":"08:00:27:ee:b5:61","app_version":"1.0","gpio_info":[{"index":0,"name":"gpio19","state":"DEASSERTED","direction":"INPUT"},{"index":1,"name":"gpio16","state":"DEASSERTED","direction":"INPUT"},{"index":2,"name":"gpio26","state":"DEASSERTED","direction":"OUTPUT"},{"index":3,"name":"gpio20","state":"DEASSERTED","direction":"OUTPUT"},{"index":4,"name":"gpio21","state":"DEASSERTED","direction":"OUTPUT"}]}}

{"jsonrpc":"2.0","id":"27","result":{"sent_on":1557441260083}}
```
```
{"jsonrpc":"2.0","method":"heartbeat","params":{"sent_on":1557441290097,"device_id":"remote-gpio","facility_id":"UNKNOWN","location":null,"video_url":null}}
```
```
{"jsonrpc":"2.0","id":"28","method":"set_gpio","params":{"index":4,"name":"gpio21","state":"ASSERTED","direction":"OUTPUT"}}

{"jsonrpc":"2.0","result":true,"id":"28"}

```
```
{"jsonrpc":"2.0","method":"gpio_input","params":{"sent_on":1557441542417,"device_id":"remote-gpio","gpio_info":{"index":1,"name":"gpio16","state":"ASSERTED","direction":"INPUT"}}}
```


