/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.android.compatibility.common.util.PropertyUtil;
import com.android.compatibility.common.util.VsrTest;
import com.android.tradefed.config.Option;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.device.ITestDevice;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test;
import com.android.tradefed.util.RunUtil;
import com.google.android.gts.angle.GtsAngleCommon;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(DeviceJUnit4ClassRunner.class)
public class AngleAllowlistTraceTest extends BaseHostJUnit4Test {
    // Object that invokes adb commands and interacts with test devices
    private Helper mTestHelper;

    // Multi-user system property
    private String mCurrentUser;

    // ANGLE trace app directory. The directory path is affected by the value of mCurrentUser
    private String mAngleTraceTestAppHomeDir;
    private String mAngleTraceTestBlobCacheDir;

    // Properties used for Vulkan feature checks
    private static final int VULKAN_1_1 = 0x00401000; // 1.1.0
    private static final String VULKAN_VERSION_FEATURE = "feature:android.hardware.vulkan.version";
    private static final String VULKAN_LEVEL_FEATURE = "feature:android.hardware.vulkan.level";

    // Package install attempts and intervals before install retries
    private static final int NUM_ATTEMPTS = 5;
    private static final int APP_INSTALL_REATTEMPT_SLEEP_MSEC = 5000;

    // Trace test max runs
    private static final int MAX_TRACE_RUN_COUNT = 5;

    // Properties used for ANGLE Trace test
    @Rule public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    @Rule
    public final DeviceJUnit4ClassRunner.TestMetrics mMetrics =
            new DeviceJUnit4ClassRunner.TestMetrics();

    @Rule
    public final DeviceJUnit4ClassRunner.TestLogData mLogData =
            new DeviceJUnit4ClassRunner.TestLogData();

    @Rule public final TestName mTestName = new TestName();

    @Option(name = "angle_trace_package_path", description = "path to angle trace package files")
    private String mANGLETracePackagePath = null;

    private static final String ANGLE_TRACE_TEST_PACKAGE_NAME = "com.android.angle.test";
    private static final String DEFAULT_ANGLE_TRACE_PACKAGE_FILE_DIR = "angle_30_traces";
    private static final String ANGLE_TRACE_DATA_ON_DEVICE_DIR =
            "/storage/emulated/0/chromium_tests_root";

    private static final int WAIT_RUN_TRACES_MILLIS = 5 * 60 * 1000;
    private HashMap<String, Double> mTracePerfFPS = new HashMap<>();

    private HashSet<String> mSkippedTrace = new HashSet<>();

    // Group 1: e.g. "wall_time"
    // Group 2: e.g. "1945_air_force".
    // Group 3: time in ms. e.g. "11.5506817933".
    private static final Pattern PATTERN_METRICS = Pattern.compile(
            "TracePerf_(?:vulkan|native)\\.(wall_time|gpu_time): ([^\\s=]*)= ([^\\s]*) ms");
    private static final Pattern PATTERN_TRACE_NAMES = Pattern.compile("TraceTest.(.*?)\n");

    private void setANGLETracePackagePath() {
        if (mANGLETracePackagePath == null) {
            mANGLETracePackagePath = DEFAULT_ANGLE_TRACE_PACKAGE_FILE_DIR;
        }
    }

    // Invokes BaseHostJUnit4Test installPackage() API, with NUM_ATTEMPTS of retries
    // Difference between this function and Helper.installApkFile() is this function can only
    // install apks that exist in the same test module (e.g. apks that are specified under
    // device_common_data or data field in Android.bp), while installApkFile() can install apks from
    // any directory.
    private void installTestApp(String appName) throws Exception {
        for (int i = 0; i < NUM_ATTEMPTS; i++) {
            try {
                installPackage(appName);
                return;
            } catch (Exception e) {
                LogUtil.CLog.e("Exception in installing the app: %s, error message: %s", appName,
                        e.getMessage());
                if (i < NUM_ATTEMPTS - 1) {
                    RunUtil.getDefault().sleep(APP_INSTALL_REATTEMPT_SLEEP_MSEC);
                } else {
                    throw e;
                }
            }
        }
    }

    private String getAngleInstrumentCommand(final String gtestArguments) {
        return String.format("am instrument -w -e"
                        + " org.chromium.native_test.NativeTestInstrumentationTestRunner.StdoutFile"
                        + " %s/files/out.txt -e"
                        + " org.chromium.native_test.NativeTest.CommandLineFlags \"%s\" -e"
                        + " org.chromium.native_test."
                        + "NativeTestInstrumentationTestRunner.ShardNanoTimeout"
                        + " 1000000000000000000 -e"
                        + " org.chromium.native_test."
                        + "NativeTestInstrumentationTestRunner.NativeTestActivity"
                        + "  com.android.angle.test.AngleUnitTestActivity "
                        + " com.android.angle.test/"
                        + "org.chromium.build.gtest_apk.NativeTestInstrumentationTestRunner",
                mAngleTraceTestAppHomeDir, gtestArguments);
    }

    private void runAndBlockAngleTestApp(final Helper helper, final String gtestArguments)
            throws CommandException, InstrumentationCrashException {
        helper.adbShellInstrumentationCommandCheck(
                WAIT_RUN_TRACES_MILLIS, getAngleInstrumentCommand(gtestArguments));

        // Cat the stdout file. This will be logged.
        helper.adbShellCommandCheck(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS,
                String.format("run-as %s cat %s/files/out.txt", ANGLE_TRACE_TEST_PACKAGE_NAME,
                        mAngleTraceTestAppHomeDir));
    }

    /** Run angle_trace_tests app to get the list of traces */
    private List<String> runAngleListTrace(final Helper helper, final File gtestStdoutFile)
            throws CommandException, InstrumentationCrashException, IOException,
                   DeviceNotAvailableException {
        // verify the device state
        helper.assertDeviceStateOk();

        // Remove previous stdout file on the device, if present.
        helper.adbShellCommandCheck(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS,
                String.format("run-as %s rm -f %s/files/out.txt", ANGLE_TRACE_TEST_PACKAGE_NAME,
                        mAngleTraceTestAppHomeDir));

        // Check file has gone but the directory exists.
        helper.adbShellCommandCheck(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS,
                String.format("run-as %s test ! -f %s/files/out.txt && run-as %s test -d %s",
                        ANGLE_TRACE_TEST_PACKAGE_NAME, mAngleTraceTestAppHomeDir,
                        ANGLE_TRACE_TEST_PACKAGE_NAME, mAngleTraceTestAppHomeDir));

        // run angle_trace_tests app with --list-tests arg
        runAndBlockAngleTestApp(helper, "--list-tests");

        // pull the test output file
        helper.adbShellCommandWithStdout(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS, gtestStdoutFile,
                String.format("run-as %s cat %s/files/out.txt", ANGLE_TRACE_TEST_PACKAGE_NAME,
                        mAngleTraceTestAppHomeDir));

        // Log it.
        helper.logTextFile("ListTraceOutput", gtestStdoutFile);

        // Read it.
        final String stdout = Files.asCharSource(gtestStdoutFile, StandardCharsets.UTF_8).read();

        // Find list of traces to run
        final Matcher traceNameMatcher = PATTERN_TRACE_NAMES.matcher(stdout);

        // Store the trace names in an ArrayList
        final ArrayList<String> traceNames = new ArrayList<>();

        while (traceNameMatcher.find()) {
            final String traceName = traceNameMatcher.group(1);
            traceNames.add(traceName);
        }
        return traceNames;
    }

    private void runAngleTracePerf(final String testName, final File gtestStdoutFile,
            final Helper helper) throws CommandException, InstrumentationCrashException,
                                        IOException, DeviceNotAvailableException {
        // verify device state
        helper.assertDeviceStateOk();

        // Remove previous stdout file on the device, if present.
        helper.adbShellCommandCheck(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS,
                String.format("run-as %s rm -f %s/files/out.txt", ANGLE_TRACE_TEST_PACKAGE_NAME,
                        mAngleTraceTestAppHomeDir));

        // Check file has gone but the directory exists.
        helper.adbShellCommandCheck(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS,
                String.format("run-as %s test ! -f %s/files/out.txt && run-as %s test -d %s",
                        ANGLE_TRACE_TEST_PACKAGE_NAME, mAngleTraceTestAppHomeDir,
                        ANGLE_TRACE_TEST_PACKAGE_NAME, mAngleTraceTestAppHomeDir));

        // Remove previous stdout file on the host, if present.
        // noinspection ResultOfMethodCallIgnored
        gtestStdoutFile.delete();

        // Check file has gone.
        assertFalse("Failed to delete " + gtestStdoutFile, gtestStdoutFile.exists());

        // Clear blob cache
        helper.adbShellCommandCheck(Helper.WAIT_ADB_LARGE_FILE_OP_MILLIS,
                String.format("run-as %s rm -rf %s", ANGLE_TRACE_TEST_PACKAGE_NAME,
                        mAngleTraceTestBlobCacheDir));

        // Run the trace.
        runAndBlockAngleTestApp(helper,
                String.format("--gtest_filter=TraceTest.%s "
                                + "--use-gl=native "
                                + "--verbose "
                                + "--verbose-logging "
                                + "--fps-limit=100 "
                                + "--fixed-test-time-with-warmup "
                                + "10",
                        testName));

        helper.assertDeviceStateOk();

        getAndLogAngleMetrics(testName, gtestStdoutFile, helper);
    }

    private void getAndLogAngleMetrics(final String testName, final File gtestStdoutFile,
            final Helper helper) throws CommandException, IOException {
        // cat the test output file
        helper.adbShellCommandWithStdout(Helper.WAIT_ADB_SHELL_FILE_OP_MILLIS, gtestStdoutFile,
                String.format("run-as %s cat %s/files/out.txt", ANGLE_TRACE_TEST_PACKAGE_NAME,
                        mAngleTraceTestAppHomeDir));

        // Log it.
        helper.logTextFile(String.format("%s_stdout", testName), gtestStdoutFile);

        // Read it.
        final String stdout = Files.asCharSource(gtestStdoutFile, StandardCharsets.UTF_8).read();

        boolean isTraceSkipped = false;

        if (stdout.contains("Test skipped due to missing extension")) {
            LogUtil.CLog.d("ANGLE trace test skipped: missing ext");
            isTraceSkipped = true;
        }

        if (stdout.contains("[  SKIPPED ] 1 test, listed below:")) {
            LogUtil.CLog.d("ANGLE trace test skipped");
            isTraceSkipped = true;
        }

        if (isTraceSkipped) {
            mSkippedTrace.add(testName);
            helper.logMetricString(testName, "skipped");
            return;
        }

        // Find all metrics of interest in the stdout file and store them into metricsMap.
        final Matcher metricsMatcher = PATTERN_METRICS.matcher(stdout);
        final HashMap<String, String> metricsMap = new HashMap<>();

        // Keep a list as well, so that we process the metrics deterministically, in order.
        final ArrayList<String> metricNames = new ArrayList<>();

        while (metricsMatcher.find()) {
            final String metricName = metricsMatcher.group(1);
            final String metricValue = metricsMatcher.group(3);

            if (!metricsMap.containsKey(metricName)) {
                metricNames.add(metricName);
            }
            metricsMap.put(metricName, metricValue);
        }

        assertTrue("We expect at least one metric.", metricNames.size() >= 1);

        // Add each time as a metric
        for (final String metricName : metricNames) {
            final String metricValue = metricsMap.get(metricName);

            // E.g. "1945_air_force.wall_time"
            String fullMetricName = String.format("%s.%s", testName, metricName);

            helper.logMetricDouble(String.format("%s_ms", fullMetricName), metricValue, "ms");

            if (metricName.equals("wall_time")) {
                // Calculate FPS
                double wallTime = Double.parseDouble(metricValue);
                double fps = 1000.0 / wallTime;
                mTracePerfFPS.put(testName, Double.valueOf(fps));

                // Log FPS in metrics, which will be saved as a tradefed result file later with
                // mTestHelper.saveMetricsAsArtifact()
                fullMetricName = String.format("%s_fps", testName);
                helper.logMetricDouble(fullMetricName, String.valueOf(fps), "fps");
            }
        }
    }

    private void uninstallTestApps() throws CommandException {
        // Remove the existing ANGLE allowlist trace test apk from the device, if present.
        mTestHelper.uninstallAppIgnoreErrors(ANGLE_TRACE_TEST_PACKAGE_NAME);

        // Remove previous ANGLE trace data directory, if present
        mTestHelper.adbShellCommandCheck(mTestHelper.WAIT_ADB_LARGE_FILE_OP_MILLIS,
                String.format("rm -rf %s", ANGLE_TRACE_DATA_ON_DEVICE_DIR));

        // Remove the existing ANGLE allowlist driver check apk from the device, if present
        mTestHelper.uninstallAppIgnoreErrors(GtsAngleCommon.ANGLE_TEST_PKG);
    }

    // Check if device supports vulkan 1.1.
    // If the device includes a Vulkan driver, feature list returned by
    // "adb shell pm list features" should contain
    // "feature:android.hardware.vulkan.level" (FEATURE_VULKAN_HARDWARE_LEVEL) and
    // "feature:android.hardware.vulkan.version" (FEATURE_VULKAN_HARDWARE_VERSION)
    // reference: https://source.android.com/docs/core/graphics/implement-vulkan
    private boolean isVulkan11Supported(ITestDevice device) throws Exception {
        final String features = device.executeShellCommand("pm list features");

        StringTokenizer featureToken = new StringTokenizer(features, "\n");

        boolean isVulkanLevelFeatureSupported = false;

        boolean isVulkanVersionFeatureSupported = false;

        boolean isVulkan_1_1_Supported = false;

        while (featureToken.hasMoreTokens()) {
            String currentFeature = featureToken.nextToken();

            // Check if currentFeature strings starts with "feature:android.hardware.vulkan.level"
            // Check that currentFeature string length is at least the length of
            // "feature:android.hardware.vulkan.level" before calling substring so that the endIndex
            // is not out of bound.
            if (currentFeature.length() >= VULKAN_LEVEL_FEATURE.length()
                    && currentFeature.substring(0, VULKAN_LEVEL_FEATURE.length())
                               .equals(VULKAN_LEVEL_FEATURE)) {
                isVulkanLevelFeatureSupported = true;
            }

            // Check if currentFeature strings starts with "feature:android.hardware.vulkan.version"
            // Check that currentFeature string length is at least the length of
            // "feature:android.hardware.vulkan.version" before calling substring so that the
            // endIndex is not out of bound.
            if (currentFeature.length() >= VULKAN_VERSION_FEATURE.length()
                    && currentFeature.substring(0, VULKAN_VERSION_FEATURE.length())
                               .equals(VULKAN_VERSION_FEATURE)) {
                isVulkanVersionFeatureSupported = true;

                // If android.hardware.vulkan.version feature is supported by the device,
                // check if the vulkan version supported is at least vulkan 1.1.
                // ANGLE is only intended to work properly with vulkan version >= vulkan 1.1
                String[] currentFeatureAndValue = currentFeature.split("=");
                if (currentFeatureAndValue.length > 1) {
                    int vulkanVersionLevelSupported = Integer.parseInt(currentFeatureAndValue[1]);
                    isVulkan_1_1_Supported = vulkanVersionLevelSupported >= VULKAN_1_1;
                }
            }

            if (isVulkanLevelFeatureSupported && isVulkanVersionFeatureSupported
                    && isVulkan_1_1_Supported) {
                return true;
            }
        }

        return false;
    }

    private boolean isChipSetMeetingA16Requirement(ITestDevice device) throws Exception {
        long boardFirstAPILevel = device.getIntProperty("ro.board.first_api_level", 0);
        long boardAPILevel = device.getIntProperty("ro.board.api_level", 0);
        return (boardFirstAPILevel >= 202504) || (boardAPILevel >= 202504);
    }

    private boolean isVendorAPILevelMeetingA16Requirement(ITestDevice device) throws Exception {
        final int vendorApiLevel = PropertyUtil.getVsrApiLevel(device);
        return vendorApiLevel >= 202504;
    }

    @Before
    public void setUp() throws Exception {
        // Instantiate a Helper object, which also calls Helper.preTestSetup()
        // that sets the device ready for tests
        mTestHelper = new Helper(getTestInformation(), mTemporaryFolder, mMetrics, mLogData,
                mTestName.getMethodName());

        // Query current_user
        final File cmdStdOutFile = new File(mTemporaryFolder.getRoot(), "cmdStdOut.txt");
        mCurrentUser = mTestHelper.adbShellCommandWithStdout(
                mTestHelper.WAIT_SET_GLOBAL_SETTING_MILLIS, cmdStdOutFile, "am get-current-user");

        LogUtil.CLog.d("mCurrentUser is: %s", mCurrentUser);

        mAngleTraceTestAppHomeDir =
                String.format("/data/user/%s/com.android.angle.test", mCurrentUser);
        mAngleTraceTestBlobCacheDir =
                String.format("/data/user_de/%s/com.android.angle.test/cache", mCurrentUser);

        setANGLETracePackagePath();

        uninstallTestApps();

        GtsAngleCommon.clearSettings(getDevice());
    }

    @After
    public void tearDown() throws Exception {
        uninstallTestApps();

        GtsAngleCommon.clearSettings(getDevice());
    }

    @VsrTest(requirements = {"VSR-5.1"})
    @Test
    public void testAngleTraces() throws Throwable {
        Assume.assumeTrue(isVulkan11Supported(getDevice()));
        Assume.assumeTrue(isChipSetMeetingA16Requirement(getDevice()));
        Assume.assumeTrue(isVendorAPILevelMeetingA16Requirement(getDevice()));
        // Firstly check ANGLE is available in System Partition
        // Install driver check app
        installTestApp(GtsAngleCommon.ANGLE_TEST_APP);
        // Verify ANGLE is available in system partition
        runDeviceTests(GtsAngleCommon.ANGLE_TEST_PKG,
                GtsAngleCommon.ANGLE_TEST_PKG + "." + GtsAngleCommon.ANGLE_DRIVER_TEST_CLASS,
                GtsAngleCommon.ANGLE_DRIVER_TEST_LOCATION_METHOD);

        // Secondly run trace tests with System ANGLE
        // We will copy the stdout file content from the device to here.
        final File gtestStdoutFile = new File(mTemporaryFolder.getRoot(), "out.txt");

        try {
            LogUtil.CLog.d("Installing angle trace app and pushing trace data to the device.");

            // Create trace data directory on the device.
            mTestHelper.deviceMkDirP(ANGLE_TRACE_DATA_ON_DEVICE_DIR);

            final File angleTraceTestPackage = new File(mANGLETracePackagePath);

            // Install the ANGLE APK.
            final File angleApkFile = mTestHelper.path(angleTraceTestPackage, "out",
                    "AndroidPerformance", "angle_trace_tests_apk", "angle_trace_tests-debug.apk");

            mTestHelper.installApkFile(angleApkFile);

            // grant test apk permissions
            mTestHelper.adbShellCommandCheck(mTestHelper.WAIT_ADB_SHELL_FILE_OP_MILLIS,
                    String.format("appops set %s MANAGE_EXTERNAL_STORAGE allow || true",
                            ANGLE_TRACE_TEST_PACKAGE_NAME));

            // Push trace_list.json
            final File angleTraceListJson = mTestHelper.path(
                    angleTraceTestPackage, "out", "AndroidPerformance", "gen", "trace_list.json");
            mTestHelper.adbCommandCheck(mTestHelper.WAIT_ADB_SHELL_FILE_OP_MILLIS, "push",
                    angleTraceListJson.toString(),
                    String.format("%s/gen/trace_list.json", ANGLE_TRACE_DATA_ON_DEVICE_DIR));

            // Create a src/tests/restricted_traces directory on test device, this is required in
            // order for angle_trace_tests app process to launch successfully
            mTestHelper.deviceMkDirP(String.format(
                    "%s/src/tests/restricted_traces", ANGLE_TRACE_DATA_ON_DEVICE_DIR));

            // Launch angle_trace_tests app with --list-test argument to get the list of trace names
            List<String> traceNames = runAngleListTrace(mTestHelper, gtestStdoutFile);

            assertFalse("trace list is not empty", traceNames.isEmpty());

            // Set trace to run with System ANGLE
            mTestHelper.adbShellCommandCheck(mTestHelper.WAIT_SET_GLOBAL_SETTING_MILLIS,
                    "settings put global angle_gl_driver_selection_pkgs com.android.angle.test");
            mTestHelper.adbShellCommandCheck(mTestHelper.WAIT_SET_GLOBAL_SETTING_MILLIS,
                    "settings put global angle_gl_driver_selection_values angle");
            mTestHelper.adbShellCommandCheck(mTestHelper.WAIT_SET_GLOBAL_SETTING_MILLIS,
                    "settings delete global angle_debug_package");

            // Run all the trace tests.
            for (final String traceName : traceNames) {
                // push the "<traceName>.json" onto the device
                String traceJsonFileName = String.format("%s.json", traceName);
                final File traceJsonFile = mTestHelper.path(angleTraceTestPackage, "src", "tests",
                        "restricted_traces", traceName, traceJsonFileName);
                mTestHelper.adbCommandCheck(mTestHelper.WAIT_ADB_LARGE_FILE_OP_MILLIS, "push",
                        traceJsonFile.toString(),
                        String.format("%s/src/tests/restricted_traces/%s/%s",
                                ANGLE_TRACE_DATA_ON_DEVICE_DIR, traceName, traceJsonFileName));

                // push the "<traceName>.angledata.gz" file onto the device
                String traceDataFileName = String.format("%s.angledata.gz", traceName);
                final File traceDataFile = mTestHelper.path(angleTraceTestPackage, "src", "tests",
                        "restricted_traces", traceName, traceDataFileName);
                mTestHelper.adbCommandCheck(mTestHelper.WAIT_ADB_LARGE_FILE_OP_MILLIS, "push",
                        traceDataFile.toString(),
                        String.format("%s/src/tests/restricted_traces/%s/%s",
                                ANGLE_TRACE_DATA_ON_DEVICE_DIR, traceName, traceDataFileName));

                // Run trace test until either of below conditions is met:
                // 1) trace reaches 60 fps
                // 2) trace is ran 5 times
                Double currentTraceFPS = null;
                int traceRun = 0;
                do {
                    // Launch trace test with 3 times of retry. On retry, the device is reboot.
                    // This allows test re-attempts in case the device lost connections during the
                    // test.
                    mTestHelper.runWithRetry(
                            () -> runAngleTracePerf(traceName, gtestStdoutFile, mTestHelper));
                    ++traceRun;
                    currentTraceFPS = mTracePerfFPS.get(traceName);
                } while ((currentTraceFPS != null
                                 && Double.compare(currentTraceFPS.doubleValue(), 60.0) < 0)
                        && traceRun < MAX_TRACE_RUN_COUNT);
            }

            assertTrue(String.format("Not all required traces are ran, traces that are skipped: %s",
                               mSkippedTrace.toString()),
                    mTracePerfFPS.size() == traceNames.size());

            // Check test result
            for (Map.Entry<String, Double> entry : mTracePerfFPS.entrySet()) {
                String testName = entry.getKey();
                Double fps = entry.getValue();
                assertTrue(String.format("fps of trace %s must be at least 60", testName),
                        Double.compare(fps.doubleValue(), 60.0) >= 0);
            }
        } finally {
            mTestHelper.saveMetricsAsArtifact();
        }
    }
}
