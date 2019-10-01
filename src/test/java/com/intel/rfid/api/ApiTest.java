package com.intel.rfid.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.intel.rfid.api.data.ClusterConfig;
import com.intel.rfid.api.data.DeviceAlertType;
import com.intel.rfid.api.data.FilterPattern;
import com.intel.rfid.api.data.InventorySummary;
import com.intel.rfid.api.data.MqttStatus;
import com.intel.rfid.api.data.OemCfgUpdateInfo;
import com.intel.rfid.api.data.ReadState;
import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.data.SensorConnectionStateInfo;
import com.intel.rfid.api.data.SensorReadStateInfo;
import com.intel.rfid.api.data.SensorSoftwareRepoVersions;
import com.intel.rfid.api.data.TagInfo;
import com.intel.rfid.api.sensor.BISTResults;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.api.sensor.DeviceAlertNotification;
import com.intel.rfid.api.sensor.GeoRegion;
import com.intel.rfid.api.sensor.InventoryDataNotification;
import com.intel.rfid.api.sensor.LEDState;
import com.intel.rfid.api.sensor.OemCfgUpdateNotification;
import com.intel.rfid.api.sensor.Platform;
import com.intel.rfid.api.sensor.RspControllerVersions;
import com.intel.rfid.api.sensor.RspInfo;
import com.intel.rfid.api.sensor.RfPortStatus;
import com.intel.rfid.api.sensor.SensorSoftwareVersions;
import com.intel.rfid.api.sensor.TagRead;
import com.intel.rfid.api.upstream.BehaviorDeleteRequest;
import com.intel.rfid.api.upstream.BehaviorGetAllRequest;
import com.intel.rfid.api.upstream.BehaviorGetRequest;
import com.intel.rfid.api.upstream.BehaviorPutRequest;
import com.intel.rfid.api.upstream.BehaviorResponse;
import com.intel.rfid.api.upstream.ClusterConfigResponse;
import com.intel.rfid.api.upstream.ClusterDeleteConfigRequest;
import com.intel.rfid.api.upstream.ClusterGetConfigRequest;
import com.intel.rfid.api.upstream.ClusterGetTemplateRequest;
import com.intel.rfid.api.upstream.ClusterGetTemplateResponse;
import com.intel.rfid.api.upstream.ClusterSetConfigRequest;
import com.intel.rfid.api.upstream.DownstreamGetMqttStatusRequest;
import com.intel.rfid.api.upstream.DownstreamGetMqttStatusResponse;
import com.intel.rfid.api.upstream.DownstreamMqttStatusNotification;
import com.intel.rfid.api.upstream.GpioClearMappingsRequest;
import com.intel.rfid.api.upstream.GpioSetMappingRequest;
import com.intel.rfid.api.upstream.InventoryActivateMobilityProfileRequest;
import com.intel.rfid.api.upstream.InventoryEventNotification;
import com.intel.rfid.api.upstream.InventoryGetActiveMobilityProfileIdRequest;
import com.intel.rfid.api.upstream.InventoryGetActiveMobilityProfileIdResponse;
import com.intel.rfid.api.upstream.InventoryGetTagInfoRequest;
import com.intel.rfid.api.upstream.InventoryGetTagInfoResponse;
import com.intel.rfid.api.upstream.InventoryGetTagStatsInfoRequest;
import com.intel.rfid.api.upstream.InventoryGetTagStatsInfoResponse;
import com.intel.rfid.api.upstream.InventoryReadRateNotification;
import com.intel.rfid.api.upstream.InventorySummaryNotification;
import com.intel.rfid.api.upstream.InventoryUnloadRequest;
import com.intel.rfid.api.upstream.MobilityProfileDeleteRequest;
import com.intel.rfid.api.upstream.MobilityProfileGetAllRequest;
import com.intel.rfid.api.upstream.MobilityProfileGetRequest;
import com.intel.rfid.api.upstream.MobilityProfilePutRequest;
import com.intel.rfid.api.upstream.MobilityProfileResponse;
import com.intel.rfid.api.upstream.RspControllerDeviceAlertNotification;
import com.intel.rfid.api.upstream.RspControllerGetAllGeoRegionsRequest;
import com.intel.rfid.api.upstream.RspControllerGetAllGeoRegionsResponse;
import com.intel.rfid.api.upstream.RspControllerGetSensorSWRepoVersionsRequest;
import com.intel.rfid.api.upstream.RspControllerGetSensorSwRepoVersionsResponse;
import com.intel.rfid.api.upstream.RspControllerGetVersionsRequest;
import com.intel.rfid.api.upstream.RspControllerGetVersionsResponse;
import com.intel.rfid.api.upstream.RspControllerHeartbeatNotification;
import com.intel.rfid.api.upstream.RspControllerStatusUpdateNotification;
import com.intel.rfid.api.upstream.SchedulerGetRunStateRequest;
import com.intel.rfid.api.upstream.SchedulerRunStateNotification;
import com.intel.rfid.api.upstream.SchedulerRunStateResponse;
import com.intel.rfid.api.upstream.SchedulerSetRunStateRequest;
import com.intel.rfid.api.upstream.SensorConfigNotification;
import com.intel.rfid.api.upstream.SensorConnectionStateNotification;
import com.intel.rfid.api.upstream.SensorForceAllDisconnectRequest;
import com.intel.rfid.api.upstream.SensorGetBasicInfoRequest;
import com.intel.rfid.api.upstream.SensorGetBasicInfoResponse;
import com.intel.rfid.api.upstream.SensorGetBistResultsRequest;
import com.intel.rfid.api.upstream.SensorGetBistResultsResponse;
import com.intel.rfid.api.upstream.SensorGetDeviceIdsRequest;
import com.intel.rfid.api.upstream.SensorGetDeviceIdsResponse;
import com.intel.rfid.api.upstream.SensorGetGeoRegionRequest;
import com.intel.rfid.api.upstream.SensorGetGeoRegionResponse;
import com.intel.rfid.api.upstream.SensorGetStateRequest;
import com.intel.rfid.api.upstream.SensorGetStateResponse;
import com.intel.rfid.api.upstream.SensorGetVersionsRequest;
import com.intel.rfid.api.upstream.SensorGetVersionsResponse;
import com.intel.rfid.api.upstream.SensorReadStateNotification;
import com.intel.rfid.api.upstream.SensorRebootRequest;
import com.intel.rfid.api.upstream.SensorRemoveRequest;
import com.intel.rfid.api.upstream.SensorResetRequest;
import com.intel.rfid.api.upstream.SensorSetGeoRegionRequest;
import com.intel.rfid.api.upstream.SensorSetLedRequest;
import com.intel.rfid.api.upstream.SensorSetRssiThresholdRequest;
import com.intel.rfid.api.upstream.SensorStateSummaryNotification;
import com.intel.rfid.api.upstream.SensorUpdateSoftwareRequest;
import com.intel.rfid.api.upstream.UpstreamGetMqttStatusRequest;
import com.intel.rfid.api.upstream.UpstreamGetMqttStatusResponse;
import com.intel.rfid.api.upstream.UpstreamMqttStatusNotification;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.cluster.MockClusterManager;
import com.intel.rfid.controller.Env;
import com.intel.rfid.controller.MockRspController;
import com.intel.rfid.controller.RspControllerStatus;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.helpers.EnvHelper;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.TestStore;
import com.intel.rfid.inventory.InventoryManager;
import com.intel.rfid.inventory.MobilityProfile;
import com.intel.rfid.inventory.MobilityProfileConfig;
import com.intel.rfid.inventory.MockInventoryManager;
import com.intel.rfid.schedule.MockScheduleManager;
import com.intel.rfid.sensor.MockSensorManager;
import com.intel.rfid.sensor.MockSensorPlatform;
import com.intel.rfid.upstream.UpstreamInventoryEventInfo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ApiTest implements InventoryManager.UpstreamEventListener {

    @BeforeClass
    public static void beforeClass() throws IOException {
        EnvHelper.beforeTests();
    }

    @AfterClass
    public static void afterClass() {
        EnvHelper.afterTests();
    }

    private List<UpstreamInventoryEventInfo> upstreamEvents = new ArrayList<>();

    public void onUpstreamEvent(UpstreamInventoryEventInfo _uie) {
        upstreamEvents.add(_uie);
    }

    ObjectMapper mapper = Jackson.getMapper();
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    Path outDir;

    TestStore testStore;
    MockRspController rspController;
    MockInventoryManager invMgr;
    MockScheduleManager schedMgr;
    MockClusterManager clusterMgr;
    MockSensorManager sensorMgr;

    MockSensorPlatform theSensor;
    ClusterConfig retailUseCaseClusterCfg;

    public ApiTest() {
        testStore = new TestStore();
        theSensor = testStore.sensorFront01;
        retailUseCaseClusterCfg = testStore.getRetailUseCaseClusterConfig();

        rspController = new MockRspController();
        invMgr = rspController.getMockInventoryManager();
        invMgr.addUpstreamEventListener(this);
        invMgr.unload();

        schedMgr = rspController.getMockScheduleManager();
        clusterMgr = rspController.getMockClusterManager();
        sensorMgr = rspController.getMockSensorManager();
    }
    
    protected String formatClasNameToJson(String _input) {
        return _input.replaceAll("(.)(\\p{Upper})", "$1_$2").toLowerCase();
    }

    protected void persistJsonApi(String _suffix, Object _obj) throws IOException {
        // convert CamelCase to snake_case and add the (optional) suffix
        //String fileName = _obj.getClass()
        //                      .getSimpleName()
        //                      .replaceAll("(.)(\\p{Upper})", "$1_$2")
        //                      .toLowerCase() + _suffix;
        String fileName = formatClasNameToJson(_obj.getClass().getSimpleName()) + _suffix;
        writer.writeValue(new File(outDir.toFile(), fileName + ".json"), _obj);
    }
    
    protected TreeSet<String> getExpectedNames(String _package) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        assert classLoader != null;
        String path = _package.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        TreeSet<String> names = new TreeSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File pkgDir = new File(resource.getFile());
            File[] files = pkgDir.listFiles();
            if(files == null) { continue; }
            for (File file : files) {
                String fname = file.getName();
                // filter for only main classes (no inner classes)
                if (fname.endsWith(".class") && !fname.contains("$")) {
                    fname = formatClasNameToJson(fname.substring(0, fname.length() - 6));
                    names.add(fname);
                }
            }
        }
        return names;
    }

    protected TreeSet<String>  getExampleNames() {
        TreeSet<String> names = new TreeSet<>();
        File[] files = outDir.toFile().listFiles();
        if(files == null) { return names; }
        for(File file : files) {
            String fname = file.getName();
            // filter for only main classes (no inner classes)
            if (fname.endsWith(".json")) {
                names.add(file.getName().substring(0, file.getName().length() - 5));
            }
        }
        return names;
    }

    protected void persistJsonApi(Object _obj) throws IOException {
        persistJsonApi("", _obj);
    }

    @Test
    public void generateSensorApiExamples() throws IOException {
        outDir = Paths.get("/tmp/api/sensor");
        Env.ensurePath(outDir);
    }

    @Test
    public void testGetFiles() throws IOException {
        Set<String> filenames = getExpectedNames("com.intel.rfid.api.upstream");
        for(String f : filenames) {
            System.out.println(f);
        }
    }

    @Test
    public void generateUpstreamApiExamples() throws IOException, ConfigException {

        
        outDir = Paths.get("/tmp/api/upstream");
        Env.ensurePath(outDir);

        JsonRequest req;
        JsonResponse rsp;
        JsonNotification not;

        // Behavior
        persistJsonApi(new BehaviorDeleteRequest("ExampleBehaviorID"));
        persistJsonApi(new BehaviorGetRequest("ExampleBehaviorID"));
        req = new BehaviorGetAllRequest();
        persistJsonApi(req);
        rsp = new BehaviorResponse(req.id, new ArrayList<>(BehaviorConfig.available().values()));
        persistJsonApi(rsp);
        persistJsonApi(new BehaviorPutRequest(new Behavior()));

        // Cluster
        persistJsonApi(new ClusterDeleteConfigRequest());
        persistJsonApi(new ClusterGetConfigRequest());
        req = new ClusterGetTemplateRequest();
        persistJsonApi(req);
        persistJsonApi(new ClusterGetTemplateResponse(req.id, clusterMgr.getTemplate()));

        req = new ClusterSetConfigRequest(retailUseCaseClusterCfg);
        persistJsonApi(req);

        ClusterConfigResponse clusterCfgRsp = new ClusterConfigResponse(req.id, retailUseCaseClusterCfg);
        persistJsonApi(clusterCfgRsp);

        // Downstream
        req = new DownstreamGetMqttStatusRequest();
        persistJsonApi(req);
        MqttStatus mqttStatus = rspController.getMockDownstreamManager().getMqttStatus();
        rsp = new DownstreamGetMqttStatusResponse(
                req.id,
                mqttStatus);
        persistJsonApi(rsp);

        not = new DownstreamMqttStatusNotification(mqttStatus);
        persistJsonApi(not);

        // RspController
        DeviceAlertNotification alertNot = new DeviceAlertNotification();
        alertNot.params.device_id = "some.controller.deviceid";
        alertNot.params.alert_description = DeviceAlertType.HighMemoryUsage.toString();
        alertNot.params.alert_number = DeviceAlertType.HighMemoryUsage.id;
        alertNot.params.sent_on = System.currentTimeMillis();
        persistJsonApi(new RspControllerDeviceAlertNotification(alertNot));

        req = new RspControllerGetAllGeoRegionsRequest();
        persistJsonApi(req);
        persistJsonApi(new RspControllerGetAllGeoRegionsResponse(req.id, GeoRegion.asStrings()));

        req = new RspControllerGetSensorSWRepoVersionsRequest();
        persistJsonApi(req);
        SensorSoftwareRepoVersions ssrv = new SensorSoftwareRepoVersions();
        ssrv.app_version = "19.3.7.11";
        ssrv.platform_support_version = "19.2.3.5";
        ssrv.pkg_manifest_version = "19.3.8.11";
        ssrv.uboot_version = "2018.11.3";
        ssrv.linux_version = "linux-5.1";
        persistJsonApi(new RspControllerGetSensorSwRepoVersionsResponse(req.id, ssrv));

        req = new RspControllerGetVersionsRequest();
        persistJsonApi(req);
        RspControllerVersions controllerVersions = new RspControllerVersions();
        controllerVersions.software_version = "19.3.7.14";
        persistJsonApi(new RspControllerGetVersionsResponse(req.id, controllerVersions));

        persistJsonApi(new RspControllerHeartbeatNotification("device.host.name"));
        persistJsonApi(new RspControllerStatusUpdateNotification("device.host.name",
                                                                 RspControllerStatus.RSP_CONTROLLER_STARTED));

        // Gpio
        persistJsonApi(new GpioClearMappingsRequest());
        persistJsonApi(new GpioSetMappingRequest());


        // Inventory
        long readTimeOrig = System.currentTimeMillis();
        TagRead tagRead01 = testStore.generateReadData(readTimeOrig);
        TagRead tagRead02 = testStore.generateReadData(readTimeOrig);
        TagRead tagRead03 = testStore.generateReadData(readTimeOrig);

        InventoryDataNotification invDataNot = new InventoryDataNotification();

        // uie extends InventoryEvent
        upstreamEvents.clear();

        invDataNot.params.device_id = testStore.sensorBack01.getDeviceId();
        invDataNot.params.data.add(tagRead01);
        invDataNot.params.data.add(tagRead02);
        invDataNot.params.data.add(tagRead03);
        invMgr.onInventoryData(invDataNot, testStore.sensorBack01);

        assertThat(upstreamEvents.size()).isGreaterThan(0);
        persistJsonApi(new InventoryEventNotification(upstreamEvents.get(0)));

        readTimeOrig += 2000;
        tagRead01.last_read_on = readTimeOrig;
        tagRead02.last_read_on = readTimeOrig;
        tagRead03.last_read_on = readTimeOrig;

        invDataNot.params.device_id = testStore.sensorFront01.getDeviceId();
        invDataNot.params.data.add(tagRead01);
        invDataNot.params.data.add(tagRead02);
        invDataNot.params.data.add(tagRead03);
        invMgr.onInventoryData(invDataNot, testStore.sensorFront01);

        FilterPattern wildcard = new FilterPattern("*");
        InventoryGetTagInfoRequest invGetTagInfoReq = new InventoryGetTagInfoRequest(wildcard);
        persistJsonApi(invGetTagInfoReq);

        List<TagInfo> tagInfoList = new ArrayList<>();
        invMgr.getTagInfo(invGetTagInfoReq.params.filter_pattern, tagInfoList);
        rsp = new InventoryGetTagInfoResponse(invGetTagInfoReq.id, tagInfoList);
        persistJsonApi(rsp);

        InventoryGetTagStatsInfoRequest invGetTagStatsInfoReq = new InventoryGetTagStatsInfoRequest(wildcard);
        persistJsonApi(invGetTagStatsInfoReq);

        invMgr.getStatsInfo(invGetTagInfoReq.params.filter_pattern);
        InventoryGetTagStatsInfoResponse invGetTagStatsInfoRsp = new InventoryGetTagStatsInfoResponse(
                invGetTagStatsInfoReq.id,
                invMgr.getStatsInfo(invGetTagStatsInfoReq.params.filter_pattern));
        persistJsonApi(invGetTagStatsInfoRsp);

        persistJsonApi(new InventoryReadRateNotification(154));

        InventorySummary invSummary = new InventorySummary();
        invMgr.getSummary(invSummary);
        persistJsonApi(new InventorySummaryNotification(invSummary));

        persistJsonApi(new InventoryUnloadRequest());

        persistJsonApi(new InventoryActivateMobilityProfileRequest());
        req = new InventoryGetActiveMobilityProfileIdRequest();
        persistJsonApi(req);
        persistJsonApi(new InventoryGetActiveMobilityProfileIdResponse(req.id, MobilityProfile.DEFAULT_ID));
        
        // Mobility Profile
        // Behavior
        persistJsonApi(new MobilityProfileDeleteRequest("ExampleMobilityProfileID"));
        persistJsonApi(new MobilityProfileGetRequest("ExampleMobilityProfileID"));
        req = new MobilityProfileGetAllRequest();
        persistJsonApi(req);
        rsp = new MobilityProfileResponse(req.id, new ArrayList<>(MobilityProfileConfig.available().values()));
        persistJsonApi(rsp);
        persistJsonApi(new MobilityProfilePutRequest(new MobilityProfile()));
        

        // Scheduler
        persistJsonApi(new SchedulerGetRunStateRequest());
        req = new SchedulerSetRunStateRequest(ScheduleRunState.FROM_CONFIG);
        persistJsonApi(req);
        clusterMgr.loadConfig(retailUseCaseClusterCfg);
        schedMgr.setRunState(ScheduleRunState.FROM_CONFIG);
        persistJsonApi(new SchedulerRunStateResponse(req.id, schedMgr.getSummary()));
        persistJsonApi(new SchedulerRunStateNotification(schedMgr.getSummary()));

        // Sensor
        persistJsonApi(new SensorConfigNotification(theSensor.getConfigInfo()));
        persistJsonApi(new SensorConnectionStateNotification(new SensorConnectionStateInfo(theSensor.getDeviceId(),
                                                                                           theSensor.getConnectionState())));
        persistJsonApi(new SensorForceAllDisconnectRequest());
        req = new SensorGetBasicInfoRequest(theSensor.getDeviceId());
        persistJsonApi(req);
        persistJsonApi(new SensorGetBasicInfoResponse(req.id, theSensor.getBasicInfo()));

        req = new SensorGetBistResultsRequest(theSensor.getDeviceId());
        persistJsonApi(req);

        BISTResults bistResults = new BISTResults();
        bistResults.region = GeoRegion.USA;
        bistResults.rf_port_statuses = new ArrayList<>();
        RfPortStatus rfPortStatus1 = new RfPortStatus();
        rfPortStatus1.port = 0;
        rfPortStatus1.forward_power_dbm10 = 249;
        rfPortStatus1.reverse_power_dbm10 = 54;
        rfPortStatus1.connected = true;
        bistResults.rf_port_statuses.add(rfPortStatus1);

        RfPortStatus rfPortStatus2 = new RfPortStatus();
        rfPortStatus2.port = 1;
        rfPortStatus2.forward_power_dbm10 = 249;
        rfPortStatus2.reverse_power_dbm10 = 197;
        rfPortStatus2.connected = false;
        bistResults.rf_port_statuses.add(rfPortStatus2);

        persistJsonApi(new SensorGetBistResultsResponse(req.id, bistResults));

        req = new SensorGetDeviceIdsRequest();
        persistJsonApi(req);
        List<String> deviceIdList = new ArrayList<>();
        sensorMgr.getDeviceIds(deviceIdList);
        persistJsonApi(new SensorGetDeviceIdsResponse(req.id, deviceIdList));

        req = new SensorGetGeoRegionRequest();
        persistJsonApi(req);
        persistJsonApi(new SensorGetGeoRegionResponse(req.id, GeoRegion.USA));


        SensorGetStateRequest sensorGetStateReq = new SensorGetStateRequest();
        persistJsonApi(sensorGetStateReq);
        RspInfo rspInfo = new RspInfo();
        rspInfo.hostname = theSensor.getDeviceId();
        rspInfo.app_version = "19.2.5.14";
        rspInfo.module_version = "3.9";
        rspInfo.motion_sensor = true;
        rspInfo.num_physical_ports = 2;
        rspInfo.configuration_state = "DISCONNECTED";
        rspInfo.hwaddress = "98:4f:ee:15:04:17";
        rspInfo.operational_state = "Idle";
        rspInfo.platform = Platform.H1000;
        persistJsonApi(new SensorGetStateResponse(sensorGetStateReq.id, rspInfo));

        SensorGetVersionsRequest sensorGetVersionsReq = new SensorGetVersionsRequest();
        persistJsonApi(sensorGetVersionsReq);
        SensorSoftwareVersions sensorSWVersions = new SensorSoftwareVersions();
        sensorSWVersions.app_version = "19.2.5.14";
        sensorSWVersions.linux_version = "4.19.34 #1 SMP PREEMPT Fri Apr 26 23:33:39 UTC 2019";
        sensorSWVersions.module_version = "3.9";
        sensorSWVersions.platform_id = "H3000";
        sensorSWVersions.pkg_manifest_version = "19.2.5.14";
        sensorSWVersions.platform_support_version = "19.1.3.26-r0";
        sensorSWVersions.uboot_version = "2019.04.20190426225448";
        persistJsonApi(new SensorGetVersionsResponse(sensorGetVersionsReq.id, sensorSWVersions));

        persistJsonApi(new SensorReadStateNotification(new SensorReadStateInfo(theSensor.getDeviceId(),
                                                                               ReadState.PEND_START,
                                                                               ReadState.STARTED,
                                                                               "BehaviorId")));
        persistJsonApi(new SensorRebootRequest());
        persistJsonApi(new SensorRemoveRequest());
        persistJsonApi(new SensorResetRequest());
        persistJsonApi(new SensorSetGeoRegionRequest(theSensor.getDeviceId(), GeoRegion.USA));
        persistJsonApi(new SensorSetLedRequest(theSensor.getDeviceId(), LEDState.Disabled));
        persistJsonApi(new SensorSetRssiThresholdRequest(theSensor.getDeviceId(), 645));
        persistJsonApi(new SensorStateSummaryNotification(sensorMgr.getSummary()));
        persistJsonApi(new SensorUpdateSoftwareRequest());

        // This notification "goes" upstream but it is a pass through from the sensor API
        OemCfgUpdateInfo updateInfo = new OemCfgUpdateInfo();
        updateInfo.current_line_num = 120;
        updateInfo.total_lines = 137;
        updateInfo.device_id = theSensor.getDeviceId();
        updateInfo.file = "USA.freq.plan.txt";
        updateInfo.region = GeoRegion.USA;
        updateInfo.sent_on = System.currentTimeMillis();
        updateInfo.status = OemCfgUpdateInfo.Status.IN_PROGRESS;
        persistJsonApi(new OemCfgUpdateNotification(updateInfo));

        // Upstream
        req = new UpstreamGetMqttStatusRequest();
        persistJsonApi(req);

        mqttStatus = rspController.getMockUpstreamManager().getMqttStatus();
        rsp = new UpstreamGetMqttStatusResponse(
                req.id,
                mqttStatus);
        persistJsonApi(rsp);
        persistJsonApi(new UpstreamMqttStatusNotification(mqttStatus));

        
        // check that all the .java files in the package have corresponding examples
        Set<String> expectedNames = getExpectedNames("com.intel.rfid.api.upstream");
        Set<String> exampleNames = getExampleNames();
        expectedNames.removeAll(exampleNames);
        assertThat(expectedNames).isEmpty();

        

    }
}
