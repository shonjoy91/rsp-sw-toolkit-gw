/*
 * INTEL CONFIDENTIAL
 * Copyright (2015, 2016, 2017) Intel Corporation.
 *
 * The source code contained or described herein and all documents related to the source code ("Material")
 * are owned by Intel Corporation or its suppliers or licensors. Title to the Material remains with
 * Intel Corporation or its suppliers and licensors. The Material may contain trade secrets and proprietary
 * and confidential information of Intel Corporation and its suppliers and licensors, and is protected by
 * worldwide copyright and trade secret laws and treaty provisions. No part of the Material may be used,
 * copied, reproduced, modified, published, uploaded, posted, transmitted, distributed, or disclosed in
 * any way without Intel/'s prior express written permission.
 * No license under any patent, copyright, trade secret or other intellectual property right is granted
 * to or conferred upon you by disclosure or delivery of the Materials, either expressly, by implication,
 * inducement, estoppel or otherwise. Any license under such intellectual property rights must be express
 * and approved by Intel in writing.
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this notice or any other
 * notice embedded in Materials by Intel or Intel's suppliers or licensors in any way.
 */
package com.intel.rfid.helpers;

import com.intel.rfid.api.sensor.InventoryDataNotification;
import com.intel.rfid.api.sensor.TagRead;

import java.util.concurrent.TimeUnit;

public class EpcHelper {

  public static final String EPC_DAY_BEFORE = "day-before";
  public static final String EPC_HOUR_BEFORE = "hour-before";
  public static final String EPC_HOUR_AFTER = "hour_after";
  public static final String EPC_DAY_AFTER = "day_after";

  public static InventoryDataNotification generateBeforeAfterTime(long _timeMillis) {

    long hour = TimeUnit.HOURS.toMillis(1);
    long day = TimeUnit.DAYS.toMillis(1);

    InventoryDataNotification id = new InventoryDataNotification();
    TagRead data;

    data = new TagRead();
    data.epc = EPC_DAY_BEFORE;
    data.last_read_on = _timeMillis - day;
    id.params.data.add(data);

    data = new TagRead();
    data.epc = EPC_HOUR_BEFORE;
    data.last_read_on = _timeMillis - hour;
    id.params.data.add(data);

    data = new TagRead();
    data.epc = EPC_HOUR_AFTER;
    data.last_read_on = _timeMillis + hour;
    id.params.data.add(data);

    data = new TagRead();
    data.epc = EPC_DAY_AFTER;
    data.last_read_on = _timeMillis + day;
    id.params.data.add(data);

    return id;
  }

  public static final String EPC_10_SEC_SEQ = "sequential-10-seconds";

  public static InventoryDataNotification generate10SecondSequential(long _timeMillis) {

    long interval = 10 * 1000;
    InventoryDataNotification id = new InventoryDataNotification();
    TagRead data;

    for (int i = 0; i < 5; i++) {
      data = new TagRead();
      data.epc = EPC_10_SEC_SEQ;
      data.last_read_on = _timeMillis - (i * interval);
      id.params.data.add(data);
    }

    return id;
  }

  public static InventoryDataNotification generateEpcs(int _start, int _size) {
    InventoryDataNotification id = new InventoryDataNotification();
    TagRead data;

    for (int i = _start; i < _start + _size; i++) {
      data = new TagRead();
      data.epc = String.valueOf(i);
      data.last_read_on = 1;
      id.params.data.add(data);
    }

    return id;
  }
}
