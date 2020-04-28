#!/bin/bash

# Copyright (C) 2019 The Android Open Source Project
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

# A simple helper script that runs the VTS-Core harness unit tests

VTS_CORE_DIR=`dirname $0`/../etc

${VTS_CORE_DIR}/vts-tradefed run singleCommand host -n \
  --console-result-reporter:suppress-passed-tests \
  --class com.android.compatibility.common.tradefed.UnitTests \
  --class com.android.compatibility.common.util.HostUnitTests \
  --class com.android.compatibility.common.util.UnitTests \
  --class com.android.compatibility.tradefed.VtsUnitTests
  "$@"
