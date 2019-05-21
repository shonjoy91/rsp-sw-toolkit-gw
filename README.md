# Gateway

The features and functionality included in this reference design are intended to showcase the capabilities of the Intel® RFID Sensor Platform (Intel® RSP) by demonstrating the use of the API to collect and process RFID tag read information. **_THIS SOFTWARE IS NOT INTENDED TO BE A COMPLETE END-TO-END INVENTORY MANAGEMENT SOLUTION._**

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

The Gateway is a Java application built with Gradle. As such, it can run on any OS that supports a Java Runtime Environment version 8 or greater. The development of the Gateway software has been done on an Ubuntu Linux platform and there are several bash scripts supporting installation and configuration so to enable out of the box (repository) operation, a Linux OS is recommended.  The following instructions assume an Ubuntu 18.04 installation.  Instructions for build and installation in a Windows environment can be found in the Installation & User's Guide (see 338443-002_Intel-RSP-SW-Toolkit-Gateway.pdf in the docs directory).

### Download and Build (Linux)
``` 
# development dependencies
sudo apt-get install default-jdk git gradle

# runtime dependencies
sudo apt-get install mosquitto avahi-daemon ntp ssh

# the cloned repo can be anywhere but as a suggestion, 
# create a projects directory
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/intel/rsp-sw-toolkit-gw.git

# build an archive suitable for deployment
cd ~/projects/rsp-sw-toolkit-gw
gradle clean deploy
```

### Certificate Generation (Linux)
```
# need to create self signed certificates for sensors to make
# secured connections
mkdir -p ~/deploy/rsp-sw-toolkit-gw/cache
cd ~/deploy/rsp-sw-toolkit-gw/cache
~/deploy/rsp-sw-toolkit-gw/gen_keys.sh
```

### Run (Linux)
A shell script is provided to start the application in the foreground. 
```
cd ~/deploy/rsp-sw-toolkit-gw
~/deploy/rsp-sw-toolkit-gw/run.sh
```

### Web Administration
A web based administration interface is enabled on the gateway. If running the gateway
locally, the following URL can be used.    http://127.0.0.1:8080/web-admin/ 
The port is specified in the gateway configuration file.


#### Read RFID Tags
Connect an Intel&reg; RFID Sensor Platform on the same network segment as the gateway. 
The sensor will listen for a gateway announcement and initiate a connection. 
From a separate console window, connect to the gateway command line interface (CLI).
```
ssh -p5222 gwconsole@localhost
```
and enter the default password **_gwconsole_** when prompted. 
The CLI features tab completion for commands and parameters.  

Sensor status can be seen as follows.
```
rfid-gw> sensor show
--------------------------------------------------------------------------------------------
| device | connect | reading | behavior | facility | personality
--------------------------------------------------------------------------------------------
| RSP-150000 | CONNECTED | STARTED | ClusterAllSeq_PORTS_1 | UNKNOWN | 
| RSP-150001 | CONNECTED | STOPPED | ClusterAllSeq_PORTS_1 | UNKNOWN | 
| RSP-150002 | CONNECTED | STOPPED | ClusterAllSeq_PORTS_1 | UNKNOWN | 
--------------------------------------------------------------------------------------------
```

Tag reads are incorporated into the gateway inventory.
```
rfid-gw> inventory detail
--------------------------------------------------------------------------------------------
3014369F84191AD66100001, P, RSP-150000, 00:00:15:510
3014369F84191AD66100002, P, RSP-150000, 00:00:15:525
3014369F84191AD66100003, P, RSP-150000, 00:00:08:430
3014369F84191AD66100004, P, RSP-150000, 00:00:08:299
3014369F84191AD66100005, P, RSP-150000, 00:00:42:794
--------------------------------------------------------------------------------------------
```
(columns are EPC, Tag State, Location, Elapsed time since last read)
