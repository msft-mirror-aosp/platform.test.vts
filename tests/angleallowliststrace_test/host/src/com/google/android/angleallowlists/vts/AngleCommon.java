/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.android.angleallowlists.vts;

import com.android.tradefed.device.ITestDevice;

public class AngleCommon {
    // Settings.Global
    public static final String SETTINGS_GLOBAL_ALL_USE_ANGLE = "angle_gl_driver_all_angle";
    public static final String SETTINGS_GLOBAL_DRIVER_PKGS = "angle_gl_driver_selection_pkgs";
    public static final String SETTINGS_GLOBAL_DRIVER_VALUES = "angle_gl_driver_selection_values";
    public static final String SETTINGS_GLOBAL_ANGLE_DEBUG_PACKAGE = "angle_debug_package";

    // ANGLE
    public static final String ANGLE_TEST_PKG = "com.google.android.vts.angle.testapp";
    public static final String ANGLE_TEST_APP = "VtsAngleTestApp.apk";

    public static final String ANGLE_DRIVER_TEST_CLASS = "VtsAngleTestCase";
    public static final String ANGLE_DRIVER_TEST_LOCATION_METHOD = "testAngleLocation";

    static void setGlobalSetting(ITestDevice device, String globalSetting, String value)
            throws Exception {
        device.setSetting("global", globalSetting, value);
        device.executeShellCommand("am refresh-settings-cache");
    }

    /** Clear ANGLE-related settings */
    public static void clearSettings(ITestDevice device) throws Exception {
        // Cached Activity Manager settings
        setGlobalSetting(device, SETTINGS_GLOBAL_ALL_USE_ANGLE, "0");
        setGlobalSetting(device, SETTINGS_GLOBAL_DRIVER_PKGS, "\"\"");
        setGlobalSetting(device, SETTINGS_GLOBAL_DRIVER_VALUES, "\"\"");
        setGlobalSetting(device, SETTINGS_GLOBAL_ANGLE_DEBUG_PACKAGE, "\"\"");
    }
}
