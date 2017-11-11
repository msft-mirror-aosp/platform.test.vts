#
# Copyright (C) 2017 The Android Open Source Project
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

import json
import logging

from vts.runners.host import const
from vts.runners.host import errors
from vts.runners.host import keys
from vts.utils.python.file import target_file_utils
from vts.utils.python.hal import hal_service_name_utils


def CanRunHidlHalTest(test_instance,
                      dut,
                      shell=None,
                      run_as_compliance_test=False):
    """Checks HAL precondition of a test instance.

    Args:
        test_instance: the test instance which inherits BaseTestClass.
        dut: the AndroidDevice under test.
        shell: the ShellMirrorObject to execute command on the device.
               If not specified, the function creates one from dut.
        run_as_compliance_test: boolean, whether it is a compliance test.

    Returns:
        True if the precondition is satisfied; False otherwise.
    """
    if shell is None:
        dut.shell.InvokeTerminal("check_hal_preconditions")
        shell = dut.shell.check_hal_preconditions

    opt_params = [
        keys.ConfigKeys.IKEY_ABI_BITNESS,
        keys.ConfigKeys.IKEY_PRECONDITION_HWBINDER_SERVICE,
        keys.ConfigKeys.IKEY_PRECONDITION_FEATURE,
        keys.ConfigKeys.IKEY_PRECONDITION_FILE_PATH_PREFIX,
        keys.ConfigKeys.IKEY_PRECONDITION_LSHAL,
    ]
    test_instance.getUserParams(opt_param_names=opt_params)

    bitness = str(getattr(test_instance, keys.ConfigKeys.IKEY_ABI_BITNESS, ""))

    hwbinder_service_name = str(
        getattr(test_instance,
                keys.ConfigKeys.IKEY_PRECONDITION_HWBINDER_SERVICE, ""))
    if hwbinder_service_name:
        if not hwbinder_service_name.startswith("android.hardware."):
            logging.error("The given hwbinder service name %s is invalid.",
                          hwbinder_service_name)
        else:
            cmd_results = shell.Execute("ps -A")
            hwbinder_service_name += "@"
            if (any(cmd_results[const.EXIT_CODE]) or
                    hwbinder_service_name not in cmd_results[const.STDOUT][0]):
                logging.warn("The required hwbinder service %s not found.",
                             hwbinder_service_name)
                return False

    feature = str(
        getattr(test_instance, keys.ConfigKeys.IKEY_PRECONDITION_FEATURE, ""))
    if feature:
        if not feature.startswith("android.hardware."):
            logging.error("The given feature name %s is invalid for HIDL HAL.",
                          feature)
        else:
            cmd_results = shell.Execute("pm list features")
            if (any(cmd_results[const.EXIT_CODE]) or
                    feature not in cmd_results[const.STDOUT][0]):
                logging.warn("The required feature %s not found.", feature)
                return False

    file_path_prefix = getattr(test_instance, "file_path_prefix", "")
    if file_path_prefix and bitness:
        logging.info("FILE_PATH_PREFIX: %s", file_path_prefix)
        logging.info("Test bitness: %s", bitness)
        tag = "_" + bitness + "bit"
        if tag in file_path_prefix:
            for path_prefix in file_path_prefix[tag]:
                if not target_file_utils.Exists(path_prefix, shell):
                    msg = (
                        "The required file (prefix: {}) for {}-bit testcase "
                        "not found.").format(path_prefix, bitness)
                    logging.warn(msg)
                    return False

    hal = str(
        getattr(test_instance, keys.ConfigKeys.IKEY_PRECONDITION_LSHAL, ""))
    if hal:
        testable, _ = hal_service_name_utils.GetHalServiceName(
            shell, hal, bitness, run_as_compliance_test)
        if not testable:
            logging.warn("The required hal %s is not testable.", hal)
            return False

    logging.info("Precondition check pass.")
    return True
