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

package org.lineageos.settings.device.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtils {

    private static final String TAG = "RealmeAddons";

    /**
     * Write a string value to the specified file
     */
    public static boolean writeLine(String fileName, String value) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            writer.write(value);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Could not write to file " + fileName, e);
            return false;
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                // Ignored
            }
        }
    }

    /**
     * Read the first line from the specified file
     */
    public static String readLine(String fileName) {
        BufferedReader reader = null;
        String line = null;
        try {
            reader = new BufferedReader(new FileReader(fileName));
            line = reader.readLine();
        } catch (IOException e) {
            Log.e(TAG, "Could not read from file " + fileName, e);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // Ignored
            }
        }
        return line;
    }

    /**
     * Check if the specified file exists
     */
    public static boolean fileExists(String fileName) {
        return new File(fileName).exists();
    }
}
