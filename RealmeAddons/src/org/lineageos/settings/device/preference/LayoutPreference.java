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

package org.lineageos.settings.device.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import org.lineageos.settings.device.R;

public class LayoutPreference extends Preference {

    private View mRootView;
    private int mLayoutResId;

    public LayoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LayoutPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                new int[]{android.R.attr.layout});
        mLayoutResId = a.getResourceId(0, 0);
        a.recycle();

        if (mLayoutResId != 0) {
            setLayoutResource(mLayoutResId);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final boolean selectable = isSelectable();
        holder.itemView.setFocusable(selectable);
        holder.itemView.setClickable(selectable);

        mRootView = holder.itemView;
    }

    public View findViewById(int id) {
        return mRootView != null ? mRootView.findViewById(id) : null;
    }

    public View getRootView() {
        return mRootView;
    }
}
