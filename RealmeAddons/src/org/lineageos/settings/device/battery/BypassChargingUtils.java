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

package org.lineageos.settings.device.battery;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import androidx.preference.PreferenceManager;

import org.lineageos.settings.device.utils.FileUtils;

public class BypassChargingUtils {

    private static final String BYPASS_CHARGING_NODE = "/sys/devices/virtual/oplus_chg/battery/mmi_charging_enable";

    private static final String BYPASS_CHARGING_KEY = "bypass_charging";

    /**
     * Check if bypass charging is supported on this device
     */
    public static boolean isSupported() {
        return FileUtils.fileExists(BYPASS_CHARGING_NODE);
    }

    /**
     * Enable or disable bypass charging
     * When enabled (true), normal charging is active
     * When disabled (false), device runs on direct current, battery bypassed
     */
    public static boolean setEnabled(boolean enabled) {
        // Note: The node works inverted - 1 = charging ON, 0 = bypass (charging OFF)
        // So we write the opposite of what user expects
        return FileUtils.writeLine(BYPASS_CHARGING_NODE, enabled ? "0" : "1");
    }

    /**
     * Get current bypass charging state from sysfs
     */
    public static boolean isCurrentlyEnabled() {
        String value = FileUtils.readOneLine(BYPASS_CHARGING_NODE);
        // 0 = bypass active (charging disabled), 1 = normal charging
        return "0".equals(value);
    }

    /**
     * Restore bypass charging state from shared preferences (for boot)
     */
    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean enabled = prefs.getBoolean(BYPASS_CHARGING_KEY, false);
        setEnabled(enabled);
    }

    /**
     * Check if device is currently charging
     */
    public static boolean isCharging(Context context) {
        if (context == null) {
            return false;
        }

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus == null) {
            return false;
        }

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
               status == BatteryManager.BATTERY_STATUS_FULL;
    }
}
