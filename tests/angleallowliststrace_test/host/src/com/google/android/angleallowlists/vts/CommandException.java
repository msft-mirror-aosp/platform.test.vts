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

import com.android.tradefed.util.ArrayUtil;
import com.android.tradefed.util.CommandResult;

public class CommandException extends Exception {
    private final CommandResult mCommandResult;

    private static String getCommandAsString(final String... command) {
        return ArrayUtil.join(" ", (Object[]) command);
    }

    public CommandException(final String[] command, final CommandResult commandResult) {
        super(String.format("Command failed: %s\n%s", getCommandAsString(command),
                Helper.getCommandResultAsString(commandResult)));

        mCommandResult = commandResult;
    }

    public CommandResult getCommandResult() {
        return mCommandResult;
    }
}
