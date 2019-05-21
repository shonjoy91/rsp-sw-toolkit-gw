package com.intel.rfid.cluster;

import com.intel.rfid.exception.GatewayException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ClusterTestHelper {

    public File getTestResourceFile(String _resourceFileName) {
        URL url = getClass().getClassLoader().getResource(_resourceFileName);
        assertNotNull(url);
        return new File(url.getFile());
    }

    public void loadConfig(ClusterManager _clusterMgr, String _testConfig) {
        try {
            Path p = Paths.get(getTestResourceFile(_testConfig).getAbsolutePath());
            _clusterMgr.loadConfig(p);
        } catch(IOException | GatewayException _e) {
            fail(_e.getMessage());
        }
    }

}
