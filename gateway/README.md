# Gateway

The features and functionality included in this reference design are intended to showcase the capabilities of the Intel® RFID Sensor Platform (Intel® RSP) by demonstrating the use of the API to collect and process RFID tag read information. **_THIS SOFTWARE IS NOT INTENDED TO BE A COMPLETE END-TO-END INVENTORY MANAGEMENT SOLUTION._**

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. 

The Gateway is a Java application built with Gradle. As such, it can run on any OS that supports a Java Runtime Environment version 8 or greater. The development of the Gateway software has been done on an Ubuntu Linux platform and there are several bash scripts supporting installation and configuration so to enable out of the box (repository) operation, a Linux OS is recommended. These instructions assume an installation of Ubuntu 18.

#### Install and Build
``` 
sudo apt-get install openjdk-8-jdk git gradle
sudo apt-get install mosquitto avahi-daemon ntp ssh gradle

cd ${PROJECT_DIR}
# clone the project into this project directory

# build an archive suitable for deployment
cd ${PROJECT_DIR}/gateway
gradle buildTar
```

#### Deploy
```
cd $DEPLOY_DIR
tar -xf ${PROJECT_DIR}/build/distributions/gateway-1.0.tgz
cd ${DEPLOY_DIR}/gateway

# need to create self signed certificates for sensors to connect
mkdir -p ${DEPLOY_DIR}/gateway/cache
cd ${DEPLOY_DIR}/gateway/cache
${DEPLOY_DIR}/gateway/gen_keys.sh
```

#### Run
```
${DEPLOY_DIR}/gateway/run.sh
```

