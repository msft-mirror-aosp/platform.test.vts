#
# Copyright (C) 2018 The Android Open Source Project
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

from vts.utils.python.instrumentation import test_framework_instrumentation as tfi

class TestFrameworkInstrumentationTestSubmodule(object):
    """Unit test submodule for test_framework_instrumentation module"""

    def End(self, category, name):
        """Use End command on an event started from other module given category and name ."""
        tfi.End(category, name)