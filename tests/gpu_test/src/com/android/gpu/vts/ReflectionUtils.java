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
package com.android.gpu.vts;

import java.lang.reflect.Field;

public class ReflectionUtils {
    /**
     * Returns a string representation pf static int member, e.g. for a static class-style enum.
     * @param clz The target class.
     * @param value The static value to match.
     * @return The name of the field if one matches the value, otherwise the integer string.
     */
    static <T> String valueName(Class<T> clz, int value) {
        for (Field f : clz.getDeclaredFields()) {
            try {
                if (f.getInt(null) == value) {
                    return f.getName();
                }
            } catch (IllegalAccessException e) {
                // Ignore by design.
            }
        }
        return Integer.toString(value);
    }
}
