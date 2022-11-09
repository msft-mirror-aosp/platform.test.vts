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

package com.android.compatibility.tradefed;

import com.android.compatibility.common.tradefed.loading.CommonConfigLoadingTest;
import com.android.compatibility.tradefed.VtsCoreTradefedTest;
import com.android.compatibility.tradefed.util.TargetFileUtilsTest;
import com.android.tradefed.testtype.suite.module.KernelTestModuleControllerTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * A test suite for all VTS Trade Federation unit tests running under Junit4.
 *
 * <p>All tests listed here should be self-contained, and should not require any external
 * dependencies.
 */
@RunWith(Suite.class)
@SuiteClasses({
        KernelTestModuleControllerTest.class,
        TargetFileUtilsTest.class,
        VtsCoreTradefedTest.class,

        // Loading test
        CommonConfigLoadingTest.class,
})
public class VtsUnitTests {
    // empty on purpose
}
