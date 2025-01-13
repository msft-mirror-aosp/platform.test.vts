/*
 * Copyright (C) 2024 The Android Open Source Project
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
import static org.junit.Assert.fail;

import com.android.ddmlib.Log;
import com.android.tradefed.device.DeviceNotAvailableException;
import com.android.tradefed.invoker.TestInformation;
import com.android.tradefed.log.LogUtil;
import com.android.tradefed.metrics.proto.MetricMeasurement;
import com.android.tradefed.result.FileInputStreamSource;
import com.android.tradefed.result.LogDataType;
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner;
import com.android.tradefed.util.ArrayUtil;
import com.android.tradefed.util.CommandResult;
import com.android.tradefed.util.CommandStatus;
import com.android.tradefed.util.FileUtil;
import com.android.tradefed.util.IRunUtil;
import com.android.tradefed.util.RunUtil;
import com.google.common.io.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.rules.TemporaryFolder;

public class Helper {
    /** Runs commands, like adb. */
    private final IRunUtil mRunUtil;

    /** The serial number of the device, so we can execute adb commands on this device. */
    private final String mDeviceSerialNumber;

    /** TradeFed object for saving metrics (key-value pairs). */
    private final DeviceJUnit4ClassRunner.TestMetrics mMetrics;

    /**
     * TradeFed object for saving "artifacts" (i.e. files) that TradeFed saves for inspection when
     * investigating the results of tests.
     */
    private final DeviceJUnit4ClassRunner.TestLogData mLogData;

    /**
     * The test method name. Used so that logged metrics (see {@link
     * Helper#saveMetricsAsArtifact()}) include the test method name.
     */
    private final String mMethodName;

    /** The file to which metrics are output. See {@link Helper#mMetricsLog}. */
    private final File mMetricsTextLogFile;

    /**
     * Used to store all metrics (i.e. key-value pairs) in CSV format so that all metrics can be
     * saved as an artifact, which TradeFed saves for inspection when investigating the results of
     * tests. This is useful for local runs and may be useful in the future because the newer
     * Android test infrastructure does not ingest metrics.
     */
    private final BufferedWriter mMetricsLog;

    private final TestInformation mTestInformation;

    // These are timeout values used to wait for certain types of actions to complete.
    public static final int WAIT_INSTALL_APK_MILLIS = 120 * 1000;
    public static final int WAIT_SET_GLOBAL_SETTING_MILLIS = 5 * 1000;
    public static final int WAIT_ADB_SHELL_FILE_OP_MILLIS = 5 * 1000;
    public static final int WAIT_ADB_LARGE_FILE_OP_MILLIS = 5 * 60 * 1000;
    public static final int WAIT_APP_UNINSTALL_MILLIS = 5 * 1000;

    public static final int PAUSE_AFTER_REBOOT_MILLIS = 20 * 1000;
    public static final int PAUSE_AFTER_ADB_COMMAND_MILLIS = 2 * 1000;

    public static final int NUM_RETRIES = 3;

    public Helper(final TestInformation testInformation, final TemporaryFolder temporaryFolder,
            final DeviceJUnit4ClassRunner.TestMetrics metrics,
            final DeviceJUnit4ClassRunner.TestLogData logData, final String methodName)
            throws IOException, DeviceNotAvailableException, CommandException,
                   InterruptedException {
        mRunUtil = RunUtil.getDefault();
        mDeviceSerialNumber = testInformation.getDevice().getSerialNumber();
        mMetrics = metrics;
        mLogData = logData;

        mMethodName = methodName;
        mMetricsTextLogFile = temporaryFolder.newFile(String.format("metrics_%s.txt", mMethodName));
        mMetricsLog = new BufferedWriter(new FileWriter(mMetricsTextLogFile));

        mTestInformation = testInformation;

        preTestSetup();
        assertDeviceStateOk();
    }

    /** verify that device is ready to run tests */
    public void assertDeviceStateOk() throws CommandException, DeviceNotAvailableException {
        // Check if device is locked or screen is off.
        CommandResult result = adbShellCommandCheck(
                WAIT_ADB_SHELL_FILE_OP_MILLIS, /*logOutput*/ false, "dumpsys window");
        try {
            assertFalse(
                    "Screen was off", result.getStdout().contains("screenState=SCREEN_STATE_OFF"));

            assertFalse("Screen was locked",
                    result.getStdout().contains("KeyguardStateMonitor\n        mIsShowing=true"));

        } catch (final Throwable ex) {
            LogUtil.CLog.e(
                    "Details of dumpsys window: %s", Helper.getCommandResultAsString(result));

            throw ex;
        }
    }

    private void preTestSetup()
            throws DeviceNotAvailableException, CommandException, InterruptedException {
        // Keep screen on while plugged in.
        adbShellCommandCheck(WAIT_ADB_SHELL_FILE_OP_MILLIS, "svc power stayon true");
        RunUtil.getDefault().sleep(PAUSE_AFTER_ADB_COMMAND_MILLIS);

        // Turn screen on.
        adbShellCommandCheck(WAIT_ADB_SHELL_FILE_OP_MILLIS, "input keyevent KEYCODE_WAKEUP");
        RunUtil.getDefault().sleep(PAUSE_AFTER_ADB_COMMAND_MILLIS);

        // Skip lock screen.
        adbShellCommandCheck(WAIT_ADB_SHELL_FILE_OP_MILLIS, "wm dismiss-keyguard");
        RunUtil.getDefault().sleep(PAUSE_AFTER_ADB_COMMAND_MILLIS);

        // Disable notifications like "you're entering full screen mode", which may affect the
        // results.
        adbShellCommandCheck(WAIT_ADB_SHELL_FILE_OP_MILLIS,
                "settings put secure immersive_mode_confirmations confirmed");
        adbShellCommandCheck(WAIT_ADB_SHELL_FILE_OP_MILLIS,
                "settings put global heads_up_notifications_enabled 0");
    }

    /**
     * Saves a text file as an "artifact", which TradeFed saves for inspection when investigating
     * the results of tests.
     */
    public void saveTextFileAsArtifact(final String dataName, final File textFile) {
        try (FileInputStreamSource fiss = new FileInputStreamSource(textFile)) {
            mLogData.addTestLog(
                    String.format("%s_%s", mMethodName, dataName), LogDataType.TEXT, fiss);
        }
    }

    /**
     * Logs the contents of a text file. This goes to the "host log", which TradeFed saves for
     * inspection when investigating the results of tests.
     */
    public void logTextFile(final String tag, final File textFile) throws IOException {
        final String text = Files.asCharSource(textFile, StandardCharsets.UTF_8).read();
        Log.d(tag, text);
    }

    /**
     * Saves a metric (i.e. key-value pair). Depending on various TradeFed options, these get saved
     * to a database for querying, ingestion into dashboards, etc. They are also usually output to
     * the terminal when running a test using `atest`. This function also appends the metric ("key,
     * value") to {@link Helper#mMetricsLog}, which is saved as an artifact so that all metrics are
     * made available in a simple CSV format. This is useful for local runs and may be useful in the
     * future because the newer Android test infrastructure does not ingest metrics.
     */
    public void logMetricDouble(final String metricName, final String metricValue,
            final String unit) throws IOException {
        final double doubleValue = Double.parseDouble(metricValue);

        final MetricMeasurement.Metric metric =
                MetricMeasurement.Metric.newBuilder()
                        .setMeasurements(
                                MetricMeasurement.Measurements.newBuilder().setSingleDouble(
                                        doubleValue))
                        .setUnit(unit)
                        .build();

        mMetrics.addTestMetric(metricName, metric);

        mMetricsLog.write(String.format("%s#%s,%s", mMethodName, metricName, metricValue));
        mMetricsLog.newLine();
    }

    /**
     * Saves a metric (i.e. key-value pair). Depending on various TradeFed options, these get saved
     * to a database for querying, ingestion into dashboards, etc. They are also usually output to
     * the terminal when running a test using `atest`. This function also appends the metric ("key,
     * value") to {@link Helper#mMetricsLog}, which is saved as an artifact so that all metrics are
     * made available in a simple CSV format. This is useful for local runs and may be useful in the
     * future because the newer Android test infrastructure does not ingest metrics.
     */
    public void logMetricString(final String metricName, final String metricValue)
            throws IOException {
        final MetricMeasurement.Metric metric =
                MetricMeasurement.Metric.newBuilder()
                        .setMeasurements(
                                MetricMeasurement.Measurements.newBuilder().setSingleString(
                                        metricValue))
                        .build();

        mMetrics.addTestMetric(metricName, metric);

        mMetricsLog.write(String.format("%s#%s,%s", mMethodName, metricName, metricValue));
        mMetricsLog.newLine();
    }

    /**
     * Saves all metrics (i.e. key-value pairs) as an "artifact", which TradeFed saves for
     * inspection when investigating the results of tests. The metric functions in {@link Helper}
     * append every metric ("key, value") to {@link Helper#mMetricsLog}, which this function saves
     * as an artifact so that all metrics are made available in a simple CSV format. This is useful
     * for local runs and may be useful in the future because the newer Android test infrastructure
     * does not ingest metrics.
     */
    public void saveMetricsAsArtifact() throws IOException {
        mMetricsLog.close();

        saveTextFileAsArtifact(
                Files.getNameWithoutExtension(mMetricsTextLogFile.getName()), mMetricsTextLogFile);
    }

    /** Install the apkFile onto the device */
    public void installApkFile(final File apkFile) throws CommandException {
        adbCommandCheck(WAIT_INSTALL_APK_MILLIS, "install", "-r", "-d", "-g", apkFile.toString());
    }

    /**
     * Runs an "adb shell am instrument" command.
     *
     * <p>The command must start with "am instrument".
     *
     * @throws CommandException if the adb command fails
     * @throws InstrumentationCrashException if the stdout contains shortMsg=Process crashed
     */
    public CommandResult adbShellInstrumentationCommandCheck(final long timeout,
            final String command) throws CommandException, InstrumentationCrashException {
        if (!command.startsWith("am instrument")) {
            throw new IllegalArgumentException(String.format(
                    "Instrumentation command must start with 'am instrument': %s", command));
        }
        final String[] commandArray = new String[] {"shell", command};
        final CommandResult result = adbCommandCheck(timeout, commandArray);
        if (result.getStdout().contains("shortMsg=Process crashed")) {
            throw new InstrumentationCrashException(commandArray, result);
        }
        return result;
    }

    /** Runs adb shell command and returns the result code, also logs the result code */
    public CommandResult adbShellCommandCheck(final long timeout, final String command)
            throws CommandException {
        return adbShellCommandCheck(timeout, true, command);
    }

    private CommandResult adbShellCommandCheck(final long timeout, final boolean logOutput,
            final String command) throws CommandException {
        return adbCommandCheck(timeout, logOutput, "shell", command);
    }

    /** Runs adb command and returns the command result code, also logs the result code */
    public CommandResult adbCommandCheck(final long timeout, final String... command)
            throws CommandException {
        return adbCommandCheck(timeout, true, command);
    }

    private CommandResult adbCommandCheck(final long timeout, final boolean logOutput,
            final String... command) throws CommandException {
        final String[] newCommand =
                ArrayUtil.buildArray(new String[] {"adb", "-s", mDeviceSerialNumber}, command);
        return commandCheck(timeout, logOutput, newCommand);
    }

    /** Runs command and returns the result */
    private CommandResult commandCheck(final long timeout, final boolean logOutput,
            final String... command) throws CommandException {
        final CommandResult result = mRunUtil.runTimedCmd(timeout, command);
        if (!result.getStatus().equals(CommandStatus.SUCCESS)) {
            throw new CommandException(command, result);
        }
        if (logOutput) {
            LogUtil.CLog.d(Helper.getCommandResultAsString(result));
        }
        return result;
    }

    /** compile command result code into a full string */
    public static String getCommandResultAsString(final CommandResult commandResult) {
        if (commandResult == null) {
            return "No details of command result.";
        }
        return String.format("Exit code: %d\nStdout: %s\nStderr: %s\n", commandResult.getExitCode(),
                commandResult.getStdout(), commandResult.getStderr());
    }

    /** runs adb shell command and return the stdout if there is any */
    public String adbShellCommandWithStdout(final long timeout, final File stdOutFile,
            final String... command) throws CommandException, FileNotFoundException, IOException {
        OutputStream stdout = new FileOutputStream(stdOutFile);
        final String[] newCommand = ArrayUtil.buildArray(
                new String[] {"adb", "-s", mDeviceSerialNumber, "shell"}, command);
        mRunUtil.runTimedCmd(timeout, stdout, null, newCommand);
        return FileUtil.readStringFromFile(stdOutFile).trim();
    }

    /** create a directory on test device */
    public void deviceMkDirP(final String dir) throws CommandException {
        adbShellCommandCheck(WAIT_ADB_SHELL_FILE_OP_MILLIS, String.format("mkdir -p %s", dir));
    }

    /** uninstall app */
    public CommandResult uninstallAppIgnoreErrors(final String appPackageName) {
        try {
            return adbCommandCheck(WAIT_APP_UNINSTALL_MILLIS, "uninstall", appPackageName);
        } catch (final CommandException commandException) {
            LogUtil.CLog.w(commandException);
            return commandException.getCommandResult();
        }
    }

    /** construct the file full path from initialPath and pathSegments */
    public static File path(final File initialPath, final String... pathSegments) {
        return FileUtil.getFileForPath(initialPath, pathSegments);
    }

    /** Retries executing |runnable.run()| |NUM_RETRIES| times. On retry, reboots the device. */
    public void runWithRetry(final RunnableWithThrowable runnable) throws Throwable {
        // noinspection ConstantConditions
        for (int i = 1; i <= NUM_RETRIES; ++i) {
            try {
                runnable.run();
                return;
            } catch (final Throwable throwable) {
                // Log it.
                LogUtil.CLog.e(throwable);

                if (i >= NUM_RETRIES) {
                    // Give up.
                    LogUtil.CLog.e("Giving up after %d retries.", NUM_RETRIES);
                    throw throwable;
                } else {
                    // Reboot the device.
                    mTestInformation.getDevice().reboot();

                    preTestSetup();

                    LogUtil.CLog.i("Waiting for %s seconds after reboot.",
                            PAUSE_AFTER_REBOOT_MILLIS / 1000);
                    RunUtil.getDefault().sleep(PAUSE_AFTER_REBOOT_MILLIS);

                    LogUtil.CLog.w("Retry %d.", i);
                }
            }
        }
        fail("Unreachable");
    }
}
