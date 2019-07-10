Build 2019 Q3 01 (version 19.3.7.11)

    added about page to the web-admin to show the gateway software version etc.

    removed the periodic log INFO message of caching current_inventory.json

    changed schedule manager exception handling to not log as error when the backing 
    cache file does not exist

    changed gateway.cfg default ntp server to use the gateway host

    fixed sensors main page to request updated information when a previously unknown 
    sensor connects

    fixed scheduler page to use periodic summary updates as the device ids in the 
    scheduler clusters do not get populated until the ClusterRunner starts looping

    fixed bug - sensor that was removed and then reconnects was not being handled 
    properly as object references to the previous sensor platform were not removed 
    in DownstreamManager and ScheduleManager.

    added gateway heartbeat that includes sensor, inventory, and scheduler summary 
    information and is published as an upstream event

    fixed javascript date conversion to add 1 to the getUTCMonth value as it is 0 based 
    so the month was wrong

    fixed sensor disti page functional problem when sensors connect while 
    page was already open, connection state info name from current to connection_state

    updated user guide with instructions to enable NTP as a time service 
    for Windows installation

    fixed incorrect reference in config manager from mqtt.gpio back to mqtt.downstream
    
    changed inventory web admin to have the same fixed header as the sensors page

    added unload command to the inventory web admin page

    removed .docx versions of the documents

    added RSP Installation & User Guide and RSP Application Interface (API)


Version 19.2.6.7

    changed default power levels of the included behaviors from 30.5 to 28.5
    
    added web admin page for cluster configuration with simple capabilities of 
    upload, download, delete, and view.

    added ability to completely delete the cluster configuration 
    that has been loaded to the gateway

    updated upstream API to include additional cluster configuration message support
    
    changed the retail use case example behavior power_levels from 15.0 to 18.0
    
    changed default mobility profile from retail_garment to asset_tracking
    
    added sensor commands remove, reset, reboot, force disconnect all to the web admin
    
    updated behaviors copying instructions in the modal to describe 
    WITH_TID and PORTS_# special considerations

    added support for behavior configuration to the web admin

    added behavior_delete json API method to the JsonRpcController behaviors
     web page available supporting edit, copy, save, delete, download, and upload
    
    changed the json api for tag stats. renamed device_id to source_alias 
    to convey the correct meaning
   
    changed name of the upstream command classes from GPIO to Gpio for API consistency 
    when generating example json messages
    
    added GPIO Mapping Commands to Upstream API.

    created a common JsonRpcController for handling gateway configuration and commands

    ported the existing AdminWebSocket to use the JsonRpcController and ported 
    the UpstreamManager to use the JsonRpcController
    
    refactored web-admin to updated requests responses
    
    created example json API files for the upstream interface

    create example retail use case to go with the dev kit

    added RSSI thresholding for filtering tag reads from sensors 
    (CLI interface only at this time)

Version 19.2.5.18

    baseline CHANGELOG version


