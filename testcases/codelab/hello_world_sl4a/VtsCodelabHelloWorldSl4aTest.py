#!/usr/bin/env python
#
# Copyright (C) 2016 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import logging

from vts.runners.host import asserts
from vts.runners.host import base_test
from vts.runners.host import test_runner
from vts.utils.python.controllers import sl4a_client


class SampleSl4aTest(base_test.BaseTestClass):
    """An example showing making SL4A calls in VTS."""

    def setUpClass(self):
        self.dut = self.android_devices[0]

    def testToast(self):
        """A sample test controlling Android device with sl4a.

        Shows a toast message on the device screen.
        """
        logging.info("A toast message should show up on the devce's screen.")
        try:
            self.dut.sl4a.makeToast("Hello World!")
        except sl4a_client.ProtocolError as e:
            asserts.fail("Protocol error in an SL4A operation")
        except sl4a_client.ApiError as e:
            asserts.fail("API error during an SL4A API call")


if __name__ == "__main__":
    test_runner.main()
