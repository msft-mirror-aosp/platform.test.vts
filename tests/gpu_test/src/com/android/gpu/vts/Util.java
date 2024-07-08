/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gpu.vts;

import com.android.compatibility.common.util.FeatureUtil;
import com.android.compatibility.common.util.PropertyUtil;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil;

public class Util {
    public static final String FEATURE_PC = "android.hardware.type.pc";

    public static int getVendorApiLevelOrFirstProductApiLevel(final ITestDevice device)
            throws DeviceNotAvailableException {
        // ro.vendor.api_level already has the minimum of the vendor api level
        // and the product first api level. It can be read from
        // PropertyUtil.getVsrApiLevel(device)
        final int vendorApiLevel = PropertyUtil.getVsrApiLevel(device);
        LogUtil.CLog.i("ro.vendor.api_level: %d", vendorApiLevel);
        return vendorApiLevel;
    }

    public static boolean isHandheld(final ITestDevice device) throws DeviceNotAvailableException {
        return !FeatureUtil.isTV(device) && !FeatureUtil.isWatch(device)
                && !FeatureUtil.isAutomotive(device)
                && !FeatureUtil.hasSystemFeature(device, FEATURE_PC);
    }
}
