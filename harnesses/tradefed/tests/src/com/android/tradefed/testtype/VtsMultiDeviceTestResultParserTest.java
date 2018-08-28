/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.tradefed.testtype;

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.tradefed.util.StreamUtil;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

/**
 * Unit tests for {@link VtsMultiDeviceTestResultParser}.
 */
public class VtsMultiDeviceTestResultParserTest extends TestCase {

    // for file path
    private static final String TEST_DIR = "tests";
    private static final String OUTPUT_FILE_1 = "vts_multi_device_test_parser_output.txt";
    private static final String OUTPUT_FILE_2 = "vts_multi_device_test_parser_output_timeout.txt";
    private static final String OUTPUT_FILE_3 = "vts_multi_device_test_parser_output_error.txt";
    private static final String SUMMARY_FILE_NORMAL = "/testtype/test_run_summary_normal.json";
    private static final String SUMMARY_FILE_CLASS_ERRORS =
            "/testtype/test_run_summary_class_errors.json";
    private static final String USER_DIR = "user.dir";
    private static final String RES = "res";
    private static final String TEST_TYPE = "testtype";

    //test results
    static final String PASS = "PASS";
    static final String FAIL = "FAIL";
    static final String TIME_OUT = "TIMEOUT";

    // test name
    private static final String RUN_NAME = "SampleLightFuzzTest";
    private static final String TEST_NAME_1 = "testTurnOnLightBlackBoxFuzzing";
    private static final String TEST_NAME_2 = "testTurnOnLightWhiteBoxFuzzing";

    // error messages in the summary files.
    private static final String FAILURE_MESSAGE = "unit test";
    private static final String CLASS_ERROR_MESSAGE = "class error";

    // enumeration to indicate the input file used for each run.
    private TestCase mTestCase = TestCase.NORMAL;
    private enum TestCase {
        NORMAL, ERROR, TIMEOUT;
    }

    /**
     * Test the run method with a normal input.
     * @throws IOException
     */
    public void testRunTimeoutInput() throws IOException{
        mTestCase = TestCase.TIMEOUT;
        String[] contents =  getOutput();
        long totalTime = getTotalTime(contents);

        // prepare the mock object
        ITestRunListener mockRunListener = EasyMock.createMock(ITestRunListener.class);
        mockRunListener.testRunStarted(TEST_NAME_1, 2);
        mockRunListener.testRunStarted(TEST_NAME_2, 2);
        mockRunListener.testStarted(new TestIdentifier(RUN_NAME, TEST_NAME_1));
        mockRunListener.testStarted(new TestIdentifier(RUN_NAME, TEST_NAME_2));
        mockRunListener.testEnded(new TestIdentifier(RUN_NAME, TEST_NAME_1),
                Collections.<String, String>emptyMap());
        mockRunListener.testFailed(new TestIdentifier(RUN_NAME, TEST_NAME_2), TIME_OUT);
        mockRunListener.testRunEnded(totalTime, Collections.<String, String>emptyMap());

        EasyMock.replay(mockRunListener);
        VtsMultiDeviceTestResultParser resultParser = new VtsMultiDeviceTestResultParser(
            mockRunListener, RUN_NAME);
        resultParser.processNewLines(contents);
        resultParser.done();
    }

     /**
      * Test the run method with a normal input.
      * @throws IOException
      */
     public void testRunNormalInput() throws IOException{
         mTestCase = TestCase.NORMAL;
         String[] contents =  getOutput();
         long totalTime = getTotalTime(contents);

         // prepare the mock object
         ITestRunListener mockRunListener = EasyMock.createMock(ITestRunListener.class);
         mockRunListener.testRunStarted(TEST_NAME_1, 2);
         mockRunListener.testRunStarted(TEST_NAME_2, 2);
         mockRunListener.testStarted(new TestIdentifier(RUN_NAME, TEST_NAME_1));
         mockRunListener.testStarted(new TestIdentifier(RUN_NAME, TEST_NAME_2));
         mockRunListener.testEnded(new TestIdentifier(RUN_NAME, TEST_NAME_1),
                 Collections.<String, String>emptyMap());
         mockRunListener.testEnded(new TestIdentifier(RUN_NAME, TEST_NAME_2),
                 Collections.<String, String>emptyMap());
         mockRunListener.testRunEnded(totalTime, Collections.<String, String>emptyMap());

         EasyMock.replay(mockRunListener);
         VtsMultiDeviceTestResultParser resultParser = new VtsMultiDeviceTestResultParser(
                 mockRunListener, RUN_NAME);
         resultParser.processNewLines(contents);
         resultParser.done();
     }

     /**
      * Test the run method with a erroneous input.
      * @throws IOException
      */
     public void testRunErrorInput() throws IOException{
       mTestCase = TestCase.ERROR;
       String[] contents =  getOutput();
       long totalTime = getTotalTime(contents);

       // prepare the mock object
       ITestRunListener mockRunListener = EasyMock.createMock(ITestRunListener.class);
       mockRunListener.testRunStarted(null, 0);
       mockRunListener.testRunEnded(totalTime, Collections.<String, String>emptyMap());

       EasyMock.replay(mockRunListener);
       VtsMultiDeviceTestResultParser resultParser = new VtsMultiDeviceTestResultParser(
               mockRunListener, RUN_NAME);
       resultParser.processNewLines(contents);
       resultParser.done();
     }
     /**
     * @param contents The logs that are used for a test case.
     * @return {long} total running time of the test.
     */
     private long getTotalTime(String[] contents) {
         Date startDate = getDate(contents, true);
         Date endDate = getDate(contents, false);

         if (startDate == null || endDate == null) {
             return 0;
         }
         return endDate.getTime() - startDate.getTime();
     }

    /**
     * Read a resource file as a string.
     *
     * @param resource the name of the resource.
     * @return {String} the contents of the resource.
     * @throws IOException if fails to read.
     */
    private String getResourceAsString(String resource) throws IOException {
        InputStream input = getClass().getResourceAsStream(resource);
        Assert.assertNotNull("Cannot load " + resource, input);
        try {
            return StreamUtil.getStringFromStream(input);
        } finally {
            input.close();
        }
    }

    /**
      * Returns the sample shell output for a test command.
      * @return {String} shell output
      * @throws IOException
      */
     private String[] getOutput() throws IOException{
         BufferedReader br = null;
         String output = null;
         try {
             br = new BufferedReader(new FileReader(getFileName()));
             StringBuilder sb = new StringBuilder();
             String line = br.readLine();

             while (line != null) {
                 sb.append(line);
                 sb.append(System.lineSeparator());
                 line = br.readLine();
             }
             output = sb.toString();
         } finally {
             br.close();
         }
         return output.split("\n");
     }

    /**
     * Returns the sample shell output for a test command.
     *
     * @return {String} shell output
     * @throws IOException
     */
    private String[] getOutput(String filePath) throws IOException {
        return getResourceAsString(filePath).split("\n");
    }

     /** Return the file path that contains sample shell output logs.
      *
      * @return {String} The file path.
      */
     private String getFileName(){
         String fileName = null;
         switch (mTestCase) {
             case NORMAL:
                 fileName = OUTPUT_FILE_1;
                 break;
             case TIMEOUT:
                 fileName = OUTPUT_FILE_2;
                 break;
             case ERROR:
                 fileName = OUTPUT_FILE_3;
                 break;
             default:
                 break;
         }
         StringBuilder path = new StringBuilder();
         path.append(System.getProperty(USER_DIR)).append(File.separator).append(TEST_DIR).
                 append(File.separator).append(RES).append(File.separator).
                 append(TEST_TYPE).append(File.separator).append(fileName);
         return path.toString();
      }

     /**
      * Return the time in milliseconds to calculate the time elapsed in a particular test.
      * 
      * @param lines The logs that need to be parsed.
      * @param calculateStartDate flag which is true if we need to calculate start date.
      * @return {Date} the start and end time corresponding to a test.
      */
     private Date getDate(String[] lines, boolean calculateStartDate) {
         Date date = null;
         int begin = calculateStartDate ? 0 : lines.length - 1;
         int diff = calculateStartDate ? 1 : -1;

         for (int index = begin; index >= 0 && index < lines.length; index += diff) {
             lines[index].trim();
             String[] toks = lines[index].split(" ");

             // set the start time from the first line
             // the loop should continue if exception occurs, else it can break
             if (toks.length < 3) {
                 continue;
             }
             String time = toks[2];
             SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
             try {
                 date = sdf.parse(time);
             } catch (ParseException e) {
                 continue;
             }
             break;
         }
         return date;
     }

    /*
     * Test parsing a summary containing passing and failing records.
     */
    public void testNormalSummary() throws IOException, JSONException {
        JSONObject object = new JSONObject(getResourceAsString(SUMMARY_FILE_NORMAL));

        ITestRunListener mockRunListener = EasyMock.createMock(ITestRunListener.class);
        mockRunListener.testRunStarted(RUN_NAME, 2);
        TestIdentifier test1 = new TestIdentifier(RUN_NAME, TEST_NAME_1);
        mockRunListener.testStarted(test1);
        mockRunListener.testEnded(test1, Collections.emptyMap());

        TestIdentifier test2 = new TestIdentifier(RUN_NAME, TEST_NAME_2);
        mockRunListener.testStarted(test2);
        mockRunListener.testFailed(test2, FAILURE_MESSAGE);
        mockRunListener.testEnded(test2, Collections.emptyMap());
        mockRunListener.testRunEnded(EasyMock.anyLong(), EasyMock.eq(Collections.emptyMap()));

        EasyMock.replay(mockRunListener);
        VtsMultiDeviceTestResultParser resultParser =
                new VtsMultiDeviceTestResultParser(mockRunListener, RUN_NAME);
        resultParser.processJsonFile(object);
    }

    /*
     * Test parsing a summary containing class error message.
     */
    public void testClassErrorSummary() throws IOException, JSONException {
        JSONObject object = new JSONObject(getResourceAsString(SUMMARY_FILE_CLASS_ERRORS));

        ITestRunListener mockRunListener = EasyMock.createMock(ITestRunListener.class);
        mockRunListener.testRunStarted(RUN_NAME, 1);
        TestIdentifier test1 = new TestIdentifier(RUN_NAME, TEST_NAME_1);
        mockRunListener.testStarted(test1);
        mockRunListener.testFailed(test1, FAILURE_MESSAGE);
        mockRunListener.testEnded(test1, Collections.emptyMap());
        mockRunListener.testRunFailed(CLASS_ERROR_MESSAGE);
        mockRunListener.testRunEnded(EasyMock.anyLong(), EasyMock.eq(Collections.emptyMap()));

        EasyMock.replay(mockRunListener);
        VtsMultiDeviceTestResultParser resultParser =
                new VtsMultiDeviceTestResultParser(mockRunListener, RUN_NAME);
        resultParser.processJsonFile(object);
    }
}
