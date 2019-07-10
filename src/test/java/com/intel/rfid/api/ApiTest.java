package com.intel.rfid.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.intel.rfid.api.data.ClusterConfig;
import com.intel.rfid.api.data.FilterPattern;
import com.intel.rfid.api.data.InventorySummary;
import com.intel.rfid.api.data.MqttStatus;
import com.intel.rfid.api.data.OemCfgUpdateInfo;
import com.intel.rfid.api.data.ScheduleRunState;
import com.intel.rfid.api.data.SensorConnectionStateInfo;
import com.intel.rfid.api.data.TagInfo;
import com.intel.rfid.api.sensor.BISTResults;
import com.intel.rfid.api.sensor.Behavior;
import com.intel.rfid.api.sensor.GeoRegion;
import com.intel.rfid.api.sensor.InventoryDataNotification;
import com.intel.rfid.api.sensor.LEDState;
import com.intel.rfid.api.sensor.OemCfgUpdateNotification;
import com.intel.rfid.api.sensor.RSPInfo;
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
import com.intel.rfid.api.upstream.InventoryGetTagInfoRequest;
import com.intel.rfid.api.upstream.InventoryGetTagInfoResponse;
import com.intel.rfid.api.upstream.InventoryGetTagStatsInfoRequest;
import com.intel.rfid.api.upstream.InventoryGetTagStatsInfoResponse;
import com.intel.rfid.api.upstream.InventoryReadRateNotification;
import com.intel.rfid.api.upstream.InventorySummaryNotification;
import com.intel.rfid.api.upstream.InventoryUnloadRequest;
import com.intel.rfid.api.upstream.RemoveDeviceRequest;
import com.intel.rfid.api.upstream.RemoveDeviceResponse;
import com.intel.rfid.api.upstream.SchedulerGetRunStateRequest;
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
import com.intel.rfid.api.upstream.SensorRebootRequest;
import com.intel.rfid.api.upstream.SensorRemoveRequest;
import com.intel.rfid.api.upstream.SensorResetRequest;
import com.intel.rfid.api.upstream.SensorSetGeoRegionRequest;
import com.intel.rfid.api.upstream.SensorSetLedRequest;
import com.intel.rfid.api.upstream.SensorStateSummaryNotification;
import com.intel.rfid.api.upstream.SensorUpdateSoftwareRequest;
import com.intel.rfid.api.upstream.UpstreamGetMqttStatusRequest;
import com.intel.rfid.api.upstream.UpstreamGetMqttStatusResponse;
import com.intel.rfid.api.upstream.UpstreamMqttStatusNotification;
import com.intel.rfid.behavior.BehaviorConfig;
import com.intel.rfid.cluster.MockClusterManager;
import com.intel.rfid.exception.ConfigException;
import com.intel.rfid.gateway.Env;
import com.intel.rfid.gateway.MockGateway;
import com.intel.rfid.helpers.EnvHelper;
import com.intel.rfid.helpers.Jackson;
import com.intel.rfid.helpers.TestStore;
import com.intel.rfid.inventory.MockInventoryManager;
import com.intel.rfid.schedule.MockScheduleManager;
import com.intel.rfid.sensor.MockSensorManager;
import com.intel.rfid.sensor.MockSensorPlatform;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ApiTest {

    @BeforeClass
    public static void beforeClass() throws IOException {
        EnvHelper.beforeTests();
    }

    @AfterClass
    public static void afterClass() {
        EnvHelper.afterTests();
    }

    ObjectMapper mapper = Jackson.getMapper();
    ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
    Path outDir;

    TestStore testStore;
    MockGateway gateway;
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

        gateway = new MockGateway();
        invMgr = gateway.getMockInventoryManager();
        invMgr.unload();
        schedMgr = gateway.getMockScheduleManager();
        clusterMgr = gateway.getMockClusterManager();
        sensorMgr = gateway.getMockSensorManager();
    }

    protected void persistJsonApi(String _suffix, Object _obj) throws IOException {
        // convert CamelCase to snake_case and add the (optional) suffix
        String fileName = _obj.getClass()
                              .getSimpleName()
                              .replaceAll("(.)(\\p{Upper})", "$1_$2")
                              .toLowerCase() + _suffix;
        writer.writeValue(new File(outDir.toFile(), fileName + ".json"), _obj);
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
    public void generateUpstreamApiExamples() throws IOException, ConfigException {

        outDir = Paths.get("/tmp/api/upstream");
        Env.ensurePath(outDir);

        persistJsonApi("_ClusterAllSeq_Ports_1", new BehaviorGetRequest("ClusterAllSeq_Ports_1"));
        persistJsonApi("_ClusterAllSeq_Ports_1", new BehaviorDeleteRequest("ClusterAllSeq_Ports_1"));

        BehaviorPutRequest behPutReq = new BehaviorPutRequest(new Behavior());
        persistJsonApi("_DEFAULT", behPutReq);

        BehaviorGetAllRequest behGetAllReq = new BehaviorGetAllRequest();
        persistJsonApi(behGetAllReq);
        BehaviorResponse behRsp = new BehaviorResponse(behGetAllReq.id,
                                                       new ArrayList<>(BehaviorConfig.available().values()));
        persistJsonApi(behRsp);

        persistJsonApi(new ClusterGetConfigRequest());

        persistJsonApi(new ClusterDeleteConfigRequest());

        ClusterGetTemplateRequest clusterGetTemplateReq = new ClusterGetTemplateRequest();
        persistJsonApi(clusterGetTemplateReq);
        persistJsonApi(new ClusterGetTemplateResponse(clusterGetTemplateReq.id, clusterMgr.getTemplate()));

        ClusterSetConfigRequest clusterSetCfgReqRetailUseCase = new ClusterSetConfigRequest(retailUseCaseClusterCfg);
        persistJsonApi("_use_case_retail", clusterSetCfgReqRetailUseCase);

        ClusterConfigResponse clusterCfgRspRetailUseCase = new ClusterConfigResponse(clusterSetCfgReqRetailUseCase.id,
                                                                                     retailUseCaseClusterCfg);
        persistJsonApi("_use_case_retail", clusterCfgRspRetailUseCase);

        DownstreamGetMqttStatusRequest downstreamGetMqttStatusReq = new DownstreamGetMqttStatusRequest();
        persistJsonApi(downstreamGetMqttStatusReq);

        GpioClearMappingsRequest gpioClearMappingsRequest = new GpioClearMappingsRequest();
        persistJsonApi(gpioClearMappingsRequest);

        GpioSetMappingRequest gpioSetMappingRequest = new GpioSetMappingRequest();
        persistJsonApi(gpioSetMappingRequest);

        MqttStatus mqttStatus = gateway.getMockDownstreamManager().getMqttStatus();
        DownstreamGetMqttStatusResponse downstreamGetMqttStatusRsp = new DownstreamGetMqttStatusResponse(
                downstreamGetMqttStatusReq.id,
                mqttStatus);
        persistJsonApi(downstreamGetMqttStatusRsp);

        DownstreamMqttStatusNotification downstreamGetMqttStatusNot = new DownstreamMqttStatusNotification(mqttStatus);
        persistJsonApi(downstreamGetMqttStatusNot);

        UpstreamGetMqttStatusRequest upstreamGetMqttStatusReq = new UpstreamGetMqttStatusRequest();
        persistJsonApi(upstreamGetMqttStatusReq);

        mqttStatus = gateway.getMockUpstreamManager().getMqttStatus();
        UpstreamGetMqttStatusResponse upstreamGetMqttStatusRsp = new UpstreamGetMqttStatusResponse(
                upstreamGetMqttStatusReq.id,
                mqttStatus);
        persistJsonApi(upstreamGetMqttStatusRsp);

        UpstreamMqttStatusNotification upstreamGetMqttStatusNot = new UpstreamMqttStatusNotification(mqttStatus);
        persistJsonApi(upstreamGetMqttStatusNot);


        FilterPattern wildcard = new FilterPattern("*");
        InventoryGetTagInfoRequest invGetTagInfoReq = new InventoryGetTagInfoRequest(wildcard);
        persistJsonApi(invGetTagInfoReq);

        InventoryDataNotification invDataNot = new InventoryDataNotification();

        long readTimeOrig = System.currentTimeMillis();
        TagRead tagRead01 = testStore.generateReadData(readTimeOrig);
        TagRead tagRead02 = testStore.generateReadData(readTimeOrig);
        TagRead tagRead03 = testStore.generateReadData(readTimeOrig);


        invDataNot.params.device_id = testStore.sensorBack01.getDeviceId();
        invDataNot.params.data.add(tagRead01);
        invDataNot.params.data.add(tagRead02);
        invDataNot.params.data.add(tagRead03);
        invMgr.onInventoryData(invDataNot, testStore.sensorBack01);

        readTimeOrig += 2000;
        tagRead01.last_read_on = readTimeOrig;
        tagRead02.last_read_on = readTimeOrig;
        tagRead03.last_read_on = readTimeOrig;

        invDataNot.params.device_id = testStore.sensorFront01.getDeviceId();
        invDataNot.params.data.add(tagRead01);
        invDataNot.params.data.add(tagRead02);
        invDataNot.params.data.add(tagRead03);
        invMgr.onInventoryData(invDataNot, testStore.sensorFront01);


        List<TagInfo> tagInfoList = new ArrayList<>();
        invMgr.getTagInfo(invGetTagInfoReq.params.filter_pattern, tagInfoList);
        InventoryGetTagInfoResponse invGetTagInfoRsp = new InventoryGetTagInfoResponse(invGetTagInfoReq.id,
                                                                                       tagInfoList);
        persistJsonApi(invGetTagInfoRsp);

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

        InventoryUnloadRequest invUnloadReq = new InventoryUnloadRequest();
        persistJsonApi(invUnloadReq);

        RemoveDeviceRequest remDeviceReq = new RemoveDeviceRequest(theSensor.getDeviceId());
        persistJsonApi(remDeviceReq);
        persistJsonApi(new RemoveDeviceResponse(remDeviceReq.id, theSensor.getDeviceId()));


        persistJsonApi(new SchedulerGetRunStateRequest());

        SchedulerSetRunStateRequest schedSetRunStateReq = new SchedulerSetRunStateRequest(ScheduleRunState.INACTIVE);
        persistJsonApi("_INACTIVE", schedSetRunStateReq);

        schedSetRunStateReq.generateId();
        schedSetRunStateReq.params.run_state = ScheduleRunState.ALL_ON;
        persistJsonApi("_ALL_ON", schedSetRunStateReq);

        schedSetRunStateReq.generateId();
        schedSetRunStateReq.params.run_state = ScheduleRunState.ALL_SEQUENCED;
        persistJsonApi("_ALL_SEQUENCED", schedSetRunStateReq);

        schedSetRunStateReq.generateId();
        schedSetRunStateReq.params.run_state = ScheduleRunState.FROM_CONFIG;
        persistJsonApi("_FROM_CONFIG", schedSetRunStateReq);

        clusterMgr.loadConfig(retailUseCaseClusterCfg);
        schedMgr.setRunState(ScheduleRunState.FROM_CONFIG);
        persistJsonApi(new SchedulerRunStateResponse(schedSetRunStateReq.id, schedMgr.getSummary()));


        persistJsonApi(new SensorConfigNotification(theSensor.getConfigInfo()));

        persistJsonApi(new SensorConnectionStateNotification(new SensorConnectionStateInfo(theSensor.getDeviceId(),
                                                                                           theSensor.getConnectionState())));

        SensorGetBasicInfoRequest sensorGetBasicInfoReq = new SensorGetBasicInfoRequest(theSensor.getDeviceId());
        persistJsonApi(sensorGetBasicInfoReq);
        persistJsonApi(new SensorGetBasicInfoResponse(sensorGetBasicInfoReq.id, theSensor.getBasicInfo()));

        SensorGetBistResultsRequest sensorGetBistResultsReq = new SensorGetBistResultsRequest(theSensor.getDeviceId());
        persistJsonApi(sensorGetBistResultsReq);

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

        persistJsonApi(new SensorGetBistResultsResponse(sensorGetBistResultsReq.id,
                                                        bistResults));

        SensorGetDeviceIdsRequest sensorGetDeviceIdsReq = new SensorGetDeviceIdsRequest();
        persistJsonApi(sensorGetDeviceIdsReq);
        List<String> deviceIdList = new ArrayList<>();
        sensorMgr.getDeviceIds(deviceIdList);
        persistJsonApi(new SensorGetDeviceIdsResponse(sensorGetDeviceIdsReq.id, deviceIdList));

        SensorGetGeoRegionRequest sensorGetGeoRegionReq = new SensorGetGeoRegionRequest();
        persistJsonApi(sensorGetGeoRegionReq);
        persistJsonApi(new SensorGetGeoRegionResponse(sensorGetGeoRegionReq.id, GeoRegion.USA));

        SensorSetGeoRegionRequest sensorSetGeoRegionReq = new SensorSetGeoRegionRequest(theSensor.getDeviceId(),
                                                                                        GeoRegion.USA);
        persistJsonApi(sensorSetGeoRegionReq);

        persistJsonApi(new SensorForceAllDisconnectRequest());
        persistJsonApi(new SensorRebootRequest());
        persistJsonApi(new SensorRemoveRequest());
        persistJsonApi(new SensorResetRequest());

        OemCfgUpdateInfo updateInfo = new OemCfgUpdateInfo();
        updateInfo.current_line_num = 120;
        updateInfo.total_lines = 137;
        updateInfo.device_id = theSensor.getDeviceId();
        updateInfo.file = "USA.freq.plan.txt";
        updateInfo.region = GeoRegion.USA;
        updateInfo.sent_on = System.currentTimeMillis();
        updateInfo.status = OemCfgUpdateInfo.Status.IN_PROGRESS;
        persistJsonApi(new OemCfgUpdateNotification(updateInfo));

        SensorGetStateRequest sensorGetStateReq = new SensorGetStateRequest();
        persistJsonApi(sensorGetStateReq);
        RSPInfo rspInfo = new RSPInfo();
        rspInfo.hostname = theSensor.getDeviceId();
        rspInfo.app_version = "19.2.5.14";
        rspInfo.module_version = "3.9";
        rspInfo.motion_sensor = true;
        rspInfo.num_physical_ports = 2;
        rspInfo.configuration_state = "DISCONNECTED";
        rspInfo.hwaddress = "98:4f:ee:15:04:17";
        rspInfo.operational_state = "Idle";
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

        SensorSetLedRequest sensorSetLedReq = new SensorSetLedRequest(theSensor.getDeviceId(), LEDState.Disabled);
        persistJsonApi("_" + LEDState.Disabled.toString(), sensorSetLedReq);

        persistJsonApi(new SensorStateSummaryNotification(sensorMgr.getSummary()));

        persistJsonApi(new SensorUpdateSoftwareRequest());

    }
}
