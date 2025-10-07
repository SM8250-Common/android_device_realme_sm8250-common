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

package org.lineageos.settings.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.lineageos.settings.device.battery.BypassChargingUtils;
import org.lineageos.settings.device.display.AntiFlikerUtils;
import org.lineageos.settings.device.hbm.AutoHBMService;
import org.lineageos.settings.device.hbm.HBMUtils;

public class DeviceSettingsFragment extends PreferenceFragmentCompat
        implements OnPreferenceChangeListener {

    private static final String KEY_ANTI_FLICKER = "anti_flicker";
    private static final String KEY_BYPASS_CHARGING = "bypass_charging";
    private static final String KEY_AUTO_HBM = "auto_hbm_enabled";

    private SwitchPreferenceCompat mAntiFlikerPreference;
    private SwitchPreferenceCompat mBypassChargingPreference;
    private SwitchPreferenceCompat mAutoHBMPreference;

    private final BroadcastReceiver mPowerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateBypassChargingState();
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.device_settings);

        mAntiFlikerPreference = findPreference(KEY_ANTI_FLICKER);
        if (mAntiFlikerPreference != null) {
            if (AntiFlikerUtils.isSupported()) {
                mAntiFlikerPreference.setEnabled(true);
                mAntiFlikerPreference.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mAntiFlikerPreference);
            }
        }

        mBypassChargingPreference = findPreference(KEY_BYPASS_CHARGING);
        if (mBypassChargingPreference != null) {
            if (BypassChargingUtils.isSupported()) {
                mBypassChargingPreference.setOnPreferenceChangeListener(this);
                updateBypassChargingState();
            } else {
                getPreferenceScreen().removePreference(mBypassChargingPreference);
            }
        }

        mAutoHBMPreference = findPreference(KEY_AUTO_HBM);
        if (mAutoHBMPreference != null) {
            if (HBMUtils.isSupported()) {
                mAutoHBMPreference.setEnabled(true);
                mAutoHBMPreference.setOnPreferenceChangeListener(this);
            } else {
                getPreferenceScreen().removePreference(mAutoHBMPreference);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBypassChargingPreference != null && BypassChargingUtils.isSupported()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            getContext().registerReceiver(mPowerReceiver, filter);
            updateBypassChargingState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mBypassChargingPreference != null && BypassChargingUtils.isSupported()) {
            try {
                getContext().unregisterReceiver(mPowerReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered, ignore
            }
        }
    }

    private void updateBypassChargingState() {
        if (mBypassChargingPreference == null) {
            return;
        }
        boolean isCharging = BypassChargingUtils.isCharging(getContext());
        mBypassChargingPreference.setEnabled(isCharging);
        if (!isCharging) {
            mBypassChargingPreference.setSummary(R.string.bypass_charging_unavailable_summary);
        } else {
            mBypassChargingPreference.setSummary(R.string.bypass_charging_summary);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_ANTI_FLICKER.equals(preference.getKey())) {
            boolean enabled = (Boolean) newValue;
            return AntiFlikerUtils.setEnabled(enabled);
        } else if (KEY_BYPASS_CHARGING.equals(preference.getKey())) {
            boolean enabled = (Boolean) newValue;
            return BypassChargingUtils.setEnabled(enabled);
        } else if (KEY_AUTO_HBM.equals(preference.getKey())) {
            boolean enabled = (Boolean) newValue;
            // The service will pick up the preference change automatically
            // Just ensure the service is running
            if (enabled) {
                getContext().startService(new Intent(getContext(), AutoHBMService.class));
            }
            return true;
        }
        return false;
    }
}
