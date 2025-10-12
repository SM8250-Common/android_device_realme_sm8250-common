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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class BypassChargingService extends Service {
    private static final String TAG = "BypassChargingService";

    private SharedPreferences mSharedPrefs;
    private boolean mBypassActive = false;
    private boolean mServiceEnabled = false;

    private final BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action) ||
                Intent.ACTION_POWER_CONNECTED.equals(action) ||
                Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                updateBypassState();
            }
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("bypass_charging".equals(key) || "bypass_charging_threshold".equals(key)) {
                mServiceEnabled = BypassChargingUtils.isBypassEnabled(BypassChargingService.this);
                if (!mServiceEnabled && mBypassActive) {
                    // Feature disabled, restore normal charging
                    BypassChargingUtils.setEnabled(false);
                    mBypassActive = false;
                    Log.d(TAG, "Feature disabled, restoring normal charging");
                } else {
                    updateBypassState();
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BypassChargingService started");

        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mServiceEnabled = BypassChargingUtils.isBypassEnabled(this);

        // Register battery state receiver
        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mBatteryReceiver, batteryFilter);

        // Register preference change listener
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        // Initialize state
        updateBypassState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BypassChargingService stopped");

        // Restore normal charging when service stops
        if (mBypassActive) {
            BypassChargingUtils.setEnabled(false);
            mBypassActive = false;
        }

        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        unregisterReceiver(mBatteryReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateBypassState() {
        if (!mServiceEnabled) {
            return;
        }

        boolean shouldBypass = BypassChargingUtils.shouldBypassBeActive(this, mBypassActive);

        if (shouldBypass && !mBypassActive) {
            // Enable bypass charging
            if (BypassChargingUtils.setEnabled(true)) {
                mBypassActive = true;
                int batteryLevel = BypassChargingUtils.getBatteryLevel(this);
                int threshold = BypassChargingUtils.getThreshold(this);
                Log.d(TAG, "Bypass charging enabled (Battery: " + batteryLevel + "%, Threshold: " + threshold + "%)");
            }
        } else if (!shouldBypass && mBypassActive) {
            // Disable bypass charging (restore normal charging)
            if (BypassChargingUtils.setEnabled(false)) {
                mBypassActive = false;
                int batteryLevel = BypassChargingUtils.getBatteryLevel(this);
                Log.d(TAG, "Bypass charging disabled (Battery: " + batteryLevel + "%)");
            }
        }
    }
}
