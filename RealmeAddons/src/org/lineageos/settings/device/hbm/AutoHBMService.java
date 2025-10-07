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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class AutoHBMService extends Service {
    private static final String TAG = "AutoHBMService";

    private static final String AUTO_HBM_ENABLED_KEY = "auto_hbm_enabled";
    private static final String AUTO_HBM_THRESHOLD_KEY = "auto_hbm_threshold";
    private static final int DEFAULT_THRESHOLD = 10000; // 10000 lux (bright outdoor light)

    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private PowerManager mPowerManager;
    private SharedPreferences mSharedPrefs;

    private boolean mAutoHBMEnabled = false;
    private int mThreshold = DEFAULT_THRESHOLD;
    private boolean mHBMActive = false;
    private boolean mScreenOn = true;

    private final SensorEventListener mLightSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (!mAutoHBMEnabled || !mScreenOn) {
                return;
            }

            float lux = event.values[0];
            updateHBMState(lux);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used
        }
    };

    private final BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mScreenOn = false;
                // Disable HBM when screen is off
                if (mHBMActive) {
                    HBMUtils.setEnabled(false);
                    mHBMActive = false;
                }
                unregisterLightSensor();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                mScreenOn = true;
                if (mAutoHBMEnabled) {
                    registerLightSensor();
                }
            }
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener mPrefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (AUTO_HBM_ENABLED_KEY.equals(key)) {
                mAutoHBMEnabled = sharedPreferences.getBoolean(AUTO_HBM_ENABLED_KEY, false);
                if (mAutoHBMEnabled && mScreenOn) {
                    registerLightSensor();
                } else {
                    unregisterLightSensor();
                    if (mHBMActive) {
                        HBMUtils.setEnabled(false);
                        mHBMActive = false;
                    }
                }
            } else if (AUTO_HBM_THRESHOLD_KEY.equals(key)) {
                mThreshold = sharedPreferences.getInt(AUTO_HBM_THRESHOLD_KEY, DEFAULT_THRESHOLD);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AutoHBMService started");

        mSensorManager = getSystemService(SensorManager.class);
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mPowerManager = getSystemService(PowerManager.class);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Load preferences
        mAutoHBMEnabled = mSharedPrefs.getBoolean(AUTO_HBM_ENABLED_KEY, false);
        mThreshold = mSharedPrefs.getInt(AUTO_HBM_THRESHOLD_KEY, DEFAULT_THRESHOLD);

        // Register preference change listener
        mSharedPrefs.registerOnSharedPreferenceChangeListener(mPrefListener);

        // Register screen state receiver
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenFilter);

        // Start monitoring if enabled
        if (mAutoHBMEnabled && mPowerManager.isInteractive()) {
            registerLightSensor();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AutoHBMService stopped");

        unregisterLightSensor();
        if (mHBMActive) {
            HBMUtils.setEnabled(false);
            mHBMActive = false;
        }

        mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mPrefListener);
        unregisterReceiver(mScreenStateReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerLightSensor() {
        if (mLightSensor != null) {
            mSensorManager.registerListener(mLightSensorListener, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Light sensor registered");
        }
    }

    private void unregisterLightSensor() {
        mSensorManager.unregisterListener(mLightSensorListener);
        Log.d(TAG, "Light sensor unregistered");
    }

    private void updateHBMState(float lux) {
        // Add hysteresis to prevent rapid toggling
        // Enable HBM when lux exceeds threshold
        // Disable HBM when lux drops below 80% of threshold
        boolean shouldEnable = lux >= mThreshold;
        boolean shouldDisable = lux < (mThreshold * 0.8f);

        if (shouldEnable && !mHBMActive) {
            if (HBMUtils.setEnabled(true)) {
                mHBMActive = true;
                Log.d(TAG, "HBM enabled (lux: " + lux + ")");
            }
        } else if (shouldDisable && mHBMActive) {
            if (HBMUtils.setEnabled(false)) {
                mHBMActive = false;
                Log.d(TAG, "HBM disabled (lux: " + lux + ")");
            }
        }
    }
}
