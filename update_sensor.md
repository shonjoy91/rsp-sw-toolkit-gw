# Update Sensor Software

Software on Intel® RSP sensors allows you to post an update package on the edge computer and have the Intel® RSP Controller application automatically install the package on all of the sensors connected to that edge computer.

1. On the edge computer, get the sensor software update from: URL

2. In a terminal window, expand the update package file and move the contents to the sensor software directory with these commands:
```
#-- Start in directory where you downloaded the sensor update
tar -xf <filename_of_sensor_update>
cd ~/deploy/rsp-sw-toolkit-gw/sensor-sw-repo
```
Sensors check this location for updates every 5 minutes. In a few minutes the sensors will automatically find and install the update.
<!--stackedit_data:
eyJoaXN0b3J5IjpbLTE5MTY5NDc2NDBdfQ==
-->
