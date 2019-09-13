![QSR H1000 Use Case](./QSR_H1000.png)

This use case demonstrates configuring the Intel&reg; RSP H1000 Devkit Sensor and Intel&reg; RSP 
Controller Application as deployed in a typical quick serve restaurant (qsr) environment.

## Goals
- Manage a deployment with two separate cold storage rooms, using one H1000 sensor and two antennas
  - This will be done by assigning a different alias to each antenna port
- Know when tagged items come into either cold room
- Determine the location of a tagged item (sensor and facility)
  - This will be done by setting a facility for the sensor and the aliases for the antenna ports
- Know when items potentially move between the cold rooms
  - Using different aliases for the different antennas will generate events when tags move between them
- Know when items leave either of the cold rooms
  - This will be done by setting the personality of the sensor to EXIT to determine tag departures
  
By the end of the example, you will be able to track tags as they move in and out of the different 
cold rooms.

## Prerequisites
1. You have an [H1000 DevKit](https://www.atlasrfidstore.com/intel-rsp-h1000-rfid-reader-development-kit/), 
or an equivalent setup.

2. You have completed the setup described in the Getting Started Guide.

3. The Intel&reg; RSP Controller application (hereafter referred to as RSP Controller) is running.

4. The H1000 sensor (with two antennas attached) is connected to the RSP Controller.

5. All RFID tags are hidden.  You can hide the tags by enclosing them in some metallic material, like a metal 
box or some aluminum foil.  You can also hide the tags under a laptop or computer.  Make sure no tags are 
visible to the sensor in order to see the complete use case scenario.

6. The antennas are positioned in an optimal setting.  Face them away from each other, point them in different 
directions, and space them at least 3-5 feet apart.
![H1000 Physical Setup](../../resources/H1000_Physical_Setup.png)

## Terminology and Concepts
- Sensor/Device ID: This is the unique identifier for each sensor.  The ID consists of "RSP-" followed by the 
last 6 characters of that sensor's MAC address.  Add picture.
- Personality: This is an optional attribute that can be assigned to the sensors. It is utilized by the RSP 
Controller to generate specific types of tag events.
- Alias: An alias can be used to identify a specific sensor/antenna-port combination.  This tuple is used to 
identify the location of tags in the inventory.
- Facility: This is used to define zones that consist of one or more sensors.  A typical deployment/location 
will consist of one facility.
- Behavior: A collection of low-level RFID settings that dictates how the sensor operates.
- Cluster: A grouping of one or more sensors that share the same set of configurations (facility, personality, 
alias, and behavior).
- Tag State: A particular condition that describes the tag's current status.  The most common states for tags 
are present, exiting, and departed.
- Tag Event: This is generated when a tag transitions between states.  The most common events are arrival, 
departed, and moved.

## Configure / Control the Intel&reg; RSP Controller Application
To configure and use the RSP Controller, one of the main components is the cluster file.  The cluster 
file specifies 
- How sensors should be grouped together
- The facility(ies) to be used
- What aliases should be assigned to the sensors' antenna ports (for unique/custom location reporting)
- Which personalities (if any) should be assigned to the sensors
- Which behavior settings should be used

__Note:__ In the following instructions, these two placeholders will be used:
- YOUR_PROJECT_DIRECTORY will refer to the directory where 
the cloned rsp-sw-toolkit-gw repo contents reside (the default location is ~/projects/)
- YOUR_DEPLOY_DIRECTORY will refer to the directory where the Intel&reg; RSP Controller Application was 
deployed (the default location is ~/deploy/)

### Cluster Configuration
1. Edit the [DevkitRetailCluster.json](./DevkitRetailCluster.json) file (located at 
YOUR_PROJECT_DIRECTORY/rsp-sw-toolkit-gw/examples/use-cases/qsr/h1000/), by replacing the sensor device id in the 
sensor_groups with the ID of the sensor included with the Devkit.  This cluster configuration file is an example 
that establishes:
    - A single facility (QSR_Store_8402)
    - Two different aliases for each of the antennas (Freezer and Refridgerator) in order to generate more 
      descriptive location names
    - An EXIT personality in order to detect when an item leaves either of the cold rooms
    - The appropriate behaviors for reading the RFID tags

2. Save the updated cluster file.

3. Choose one of the following methods to configure and control the RSP Controller. Each method will accomplish 
the same configuration tasks.

    - [METHOD 1: Using the Web Admin](#method-1-using-the-web-admin)
    - [METHOD 2: Using the MQTT Messaging API](#method-2-using-the-mqtt-messaging-api)

___

### METHOD 1: Using the Web Admin
1. Open the [web admin](http://localhost:8080/web-admin) page and confirm that the sensor included in the 
devkit is connected. This can be seen on the [dashboard](http://localhost:8080/web-admin/dashboard.html) 
page or the [sensors](http://localhost:8080/web-admin/sensors-main.html) page.  You can navigate between 
the different pages by using the menu button found at the top left of each page.

    ![Nav_Menu_Button](../../resources/Nav_Menu.png)

2. On the [scheduler](http://localhost:8080/web-admin/scheduler.html) page, stop the sensor from reading 
tags by pressing the INACTIVE button.

    ![Scheduler_Inactive_Button](../../resources/Scheduler_Inactive.png)

3. On the [inventory](http://localhost:8080/web-admin/inventory-main.html) page, press the Unload button 
to clear out all previous tag history to start a clean session.

    ![Inventory_Unload_Button](../../resources/Inventory_Unload.png)

4. On the [behaviors](http://localhost:8080/web-admin/behaviors.html) page, use the Upload From File
button to upload the use case behavior to the RSP Controller.

    ![Behaviors_Upload_Button](../../resources/Behaviors_Upload.png)

    The behavior file can be found at 
    YOUR_PROJECT_DIRECTORY/rsp-sw-toolkit-gw/examples/use-cases/qsr/h1000/DevkitQsrBehaviorExit_PORTS_2.json.  

    __NOTE:__  This file __MUST__ be loaded to the RSP Controller __BEFORE__ the cluster configuration 
    because the cluster file references that behavior id, and that behavior must already be known by the 
    RSP Controller. Otherwise the loading of the cluster configuration file will fail validation.

5. Upload the __EDITED__ cluster configuration file (see the [Cluster Configuration section](#cluster-configuration)) 
using the [cluster config](http://localhost:8080/web-admin/cluster-config.html) page.

    ![Cluster_Config_Upload_Button](../../resources/Cluster_Config_Upload.png)

6. On the [scheduler](http://localhost:8080/web-admin/scheduler.html) page, start the sensor reading 
according to the cluster configuration by pressing the FROM_CONFIG button.

    ![Scheduler_From_Config_Button](../../resources/Scheduler_From_Config.png)
    
    The clusters that the scheduler is using will be displayed on the page.

7. On the [sensors](http://localhost:8080/web-admin/sensors-main.html) page, confirm that the sensor has 
been configured as specified in the cluster configuration file (has the correct behavior, facility, personality, 
and aliases) and is reading tags.

8. Navigate to the [inventory](http://localhost:8080/web-admin/inventory-main.html) page which can be used 
to monitor tag reads and states.

9. Continue to the [Observe Tag Events section](#observe-tag-events).
___

### METHOD 2: Using the MQTT Messaging API
1. Edit [cluster_set_config_request_use_case_qsr.json](./cluster_set_config_request_use_case_qsr.json) 
replacing "CONTENTS_OF_CLUSTER_CONFIG_GO_HERE" with the contents of the edited DevkitQsrCluster.json file. 

2. Open a terminal window and subscribe to the RSP Controller's command response topic in order to monitor the 
command responses.
    ```bash
    #-- monitor the rpc command responses
    mosquitto_sub -t rfid/controller/response
    ```

3. Open another terminal to send JsonRPC commands over MQTT to configure and control the RSP Controller.
    ```bash
    #-- change directory to the examples folder 
    #-- so the example commands work correctly
    cd YOUR_PROJECT_DIRECTORY/rsp-sw-toolkit-gw/examples
    
    #-- stop the scheduler
    mosquitto_pub -t rfid/controller/command -f api/upstream/scheduler_set_run_state_request_INACTIVE.json
    
    #-- unload the current inventory
    mosquitto_pub -t rfid/controller/command -f api/upstream/inventory_unload_request.json
    
    #-- load the behavior specific to this exercise
    #-- (lowered power level as antennas are likely to be interfering)
    mosquitto_pub -t rfid/controller/command -f use-cases/qsr/h1000/behavior_put_request_Exit.json
    
    #-- load (set) the cluster configuration
    mosquitto_pub -t rfid/controller/command -f use-cases/qsr/h1000/cluster_set_config_request_use_case_qsr.json
    
    #-- activate the scheduler in custom configuration mode
    mosquitto_pub -t rfid/controller/command -f api/upstream/scheduler_set_run_state_request_FROM_CONFIG.json
    ```

4. Continue to the [Observe Tag Events section](#observe-tag-events).
___

## Observe Tag Events
Open a terminal window and subscribe to the RSP Controller events MQTT topic in order to monitor tag events 
as produced by the RSP Controller.

```bash
#-- monitor the upstream events topic
mosquitto_sub -t rfid/controller/events
```

1. ##### Tag arrival in the first cold room
    At this point, remove one tag from hiding and place it nearby one of the two antennas. When the tag is  
    read initially, an arrival event will be generated on the rfid/controller/events MQTT topic. 
    Verify from the Web Admin 
    [inventory](http://localhost:8080/web-admin/inventory-main.html) page that the tag is now in the 
    EXITING state and the location is at the first antenna's alias (either Freezer or Refridgerator).  
    Verify the receipt of the MQTT event message.
    ```json
    {
      "jsonrpc": "2.0",
      "method": "inventory_event",
      "params": {
        "sent_on": 1559867406651,
        "device_id": "intel-acetest",
        "data": [
          {
            "facility_id": "QSR_Store_8402",
            "epc_code": "303530C29C000000F0006B12",
            "tid": null,
            "epc_encode_format": "tbd",
            "event_type": "arrival",
            "timestamp": 1559867406524,
            "location": "Freezer"
          }
        ]
      }
    }
    ```

    If you do not see the expected event, please confirm that
    - The cluster file was edited properly with the correct sensor ID (see the [Cluster Configuration 
    section](#cluster-configuration))
    - The cluster file was uploaded correctly
    - The scheduler is using that cluster configuration

2. ##### Tag moved to the other cold room
    Now take the tag and move it to the other antenna. Since these antennas are in the same facility, a 
    moved event will be generated. It may take a few moments for the event to be generated as the 
    algorithm uses time-weighted RSSI averages to determine tag location. From the 
    [inventory](http://localhost:8080/web-admin/inventory-main.html) page, confirm that the tag's location 
    has changed to the other cold room.  
    Verify the receipt of the MQTT event message.
    ```json
    {
      "jsonrpc": "2.0",
      "method": "inventory_event",
      "params": {
        "sent_on": 1559867488229,
        "device_id": "intel-acetest",
        "data": [
          {
            "facility_id": "QSR_Store_8402",
            "epc_code": "303530C29C000000F0006B12",
            "tid": null,
            "epc_encode_format": "tbd",
            "event_type": "moved",
            "timestamp": 1559867487834,
            "location": "Refridgerator"
          }
        ]
      }
    }
    ```

3. ##### Tag departure from the second cold room
    Now take the tag and hide it such that it can't be seen by either antenna.  After the departure 
    threshold time limit has passed (default being 30 seconds), a departed event should be generated 
    for the tag that was removed.  From the [inventory](http://localhost:8080/web-admin/inventory-main.html) 
    page, confirm that the tag state of the removed tag has changed to DEPARTED_EXIT.  
    
    Verify the receipt of the MQTT event message.
    ```json  
    {
      "jsonrpc": "2.0",
      "method": "inventory_event",
      "params": {
        "sent_on": 1559867527713,
        "device_id": "intel-acetest",
        "data": [
          {
            "facility_id": "QSR_Store_8402",
            "epc_code": "303530C29C000000F0006B12",
            "tid": null,
            "epc_encode_format": "tbd",
            "event_type": "departed",
            "timestamp": 1559867494569,
            "location": "Refridgerator"
          }
        ]
      }
    }
    ```

## Starting a Clean Session
If you would like to start another use case or would like to run your own scenario, then you will 
want to start with a clean session for the RSP Controller so that old data and configurations do 
not pollute your new scenario.  In order to do this, follow these steps:

1. Stop the RSP Controller.  If you used the installer to install the RSP Controller, and you used 
the native installation (non-Docker method), then simply press Ctrl+C in the terminal window where 
you ran the installer script.

2. Run the following commands to clear out the old data and configurations
```bash
cd YOUR_DEPLOY_DIRECTORY/rsp-sw-toolkit-gw/cache/
rm -rf *.json
```
3. Start the RSP Controller by running the following commands
```bash
cd YOUR_DEPLOY_DIRECTORY/rsp-sw-toolkit-gw/
./run.sh
```

Now you should have a clean session from which you can run any new scenario without worry of data 
or configuration pollution.