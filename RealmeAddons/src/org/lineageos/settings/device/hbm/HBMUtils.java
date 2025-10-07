/*
 * Copyright (C) 2025 The LineageOS Project
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

package org.lineageos.settings.device.hbm;

import org.lineageos.settings.device.utils.FileUtils;

public class HBMUtils {

    private static final String HBM_NODE = "/sys/kernel/oplus_display/hbm";

    /**
     * Check if HBM is supported on this device
     */
    public static boolean isSupported() {
        return FileUtils.fileExists(HBM_NODE);
    }

    /**
     * Enable or disable HBM
     */
    public static boolean setEnabled(boolean enabled) {
        return FileUtils.writeLine(HBM_NODE, enabled ? "1" : "0");
    }

    /**
     * Get current HBM state from sysfs
     */
    public static boolean isCurrentlyEnabled() {
        String value = FileUtils.readOneLine(HBM_NODE);
        return "1".equals(value);
    }
}
