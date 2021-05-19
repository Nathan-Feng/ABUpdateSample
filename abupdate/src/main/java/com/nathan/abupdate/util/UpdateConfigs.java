/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.nathan.abupdate.util;

import android.content.Context;
import android.util.Log;


import com.nathan.abupdate.UpdateConfig;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.nathan.abupdate.util.PackageFiles.OTA_PACKAGE_DIR;


/**
 * Utility class for working with json update configurations.
 */
public final class UpdateConfigs {

    public static final String UPDATE_CONFIGS_ROOT = "configs/";

    /**
     * @param configs update configs
     * @return list of names
     */
    public static String[] configsToNames(List<UpdateConfig> configs) {
        return configs.stream().map(UpdateConfig::getName).toArray(String[]::new);
    }

    /**
     * @param context app context
     * @return configs root directory
     */
    public static String getConfigsRootDir(Context context) {
//		Log.e("UpdateConfigs", "getConfigsRoot  "+Paths
//                .get(context.getFilesDir().toString(), UPDATE_CONFIGS_ROOT)
//                .toString());
        return Paths
                .get(context.getFilesDir().toString(), UPDATE_CONFIGS_ROOT)
                .toString();
    }

    /**
     * @param context no need
     * @return /data/ota_package   modify by zhaoyufeng3
     */
    public static String getConfigsRoot(Context context) {
        return OTA_PACKAGE_DIR;
    }

    /**
     * @param context application context
     * @return list of configs from directory {@link UpdateConfigs#getConfigsRoot}
     */
    public static List<UpdateConfig> getUpdateConfigs(Context context) {
        File root = new File(getConfigsRoot(context));
        ArrayList<UpdateConfig> configs = new ArrayList<>();
        if (!root.exists()) {
            return configs;
        }
        File[] list = root.listFiles();
        if (list == null) {
            return configs;
        }
        for (final File f : list) {
            if (!f.isDirectory() && f.getName().endsWith(".json")) {
                try {
                    String json = new String(Files.readAllBytes(f.toPath()),
                            StandardCharsets.UTF_8);
                    configs.add(UpdateConfig.fromJson(json));
                } catch (Exception e) {
                    Log.e("UpdateConfigs", "Can't read/parse config file " + f.getName(), e);
                    throw new RuntimeException(
                            "Can't read/parse config file " + f.getName(), e);
                }
            }
        }
        return configs;
    }

    /**
     * @param filePath for local dir
     * @return list of configs from directory {@link UpdateConfigs#getConfigsRoot}
     */
    public static UpdateConfig getUpdateConfigsFromLocal(String filePath) {
        File root = new File(filePath);
        UpdateConfig config = null;
        if (!root.exists()) {
            return null;
        }
        if (!root.isDirectory() && root.getName().endsWith(".json")) {
            try {
                String json = new String(Files.readAllBytes(root.toPath()),
                        StandardCharsets.UTF_8);
                config = UpdateConfig.fromJson(json);
                //parce payload_properties.txt and save to path
                UpdateConfig.parceJsonToFile(json);
                //end
            } catch (Exception e) {
                throw new RuntimeException(
                        "Can't read/parse config file " + root.getName(), e);
            }
        }
        return config;
    }

    /**
     * @param jsonString for json string
     * @return list of configs from directory {@link UpdateConfigs#getConfigsRoot}
     */
    public static UpdateConfig getUpdateConfigsFromString(String jsonString) {
        UpdateConfig config = null;
        if (jsonString.startsWith("{") && jsonString.endsWith("}")) {
            try {
                config = UpdateConfig.fromJson(jsonString);
                //parce payload_properties.txt and save to path
                UpdateConfig.parceJsonToFile(jsonString);
                //end
            } catch (Exception e) {
                throw new RuntimeException(
                        "Can't read/parse config " + jsonString, e);
            }
        }
        return config;
    }
    /**
     * @param filename searches by given filename
     * @param config searches in {@link UpdateConfig#getAbConfig()}
     * @return offset and size of {@code filename} in the package zip file
     *         stored as {@link UpdateConfig.PackageFile}.
     */
    public static Optional<UpdateConfig.PackageFile> getPropertyFile(
            final String filename,
            UpdateConfig config) {
        //Log.e("UpdateConfigs", "getPropertyFile filename"+filename);
        //Log.e("UpdateConfigs", "getPropertyFile UpdateConfig filename "+config.getAbConfig().getPropertyFiles()[0].getFilename());
        return Arrays
                .stream(config.getAbConfig().getPropertyFiles())
                .filter(file -> filename.equals(file.getFilename()))
                .findFirst();
    }

    private UpdateConfigs() {}

}
