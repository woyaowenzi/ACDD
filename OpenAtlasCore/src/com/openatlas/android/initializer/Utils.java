/**
 * OpenAtlasForAndroid Project
 * <p>
 * The MIT License (MIT)
 * Copyright (c) 2015 Bunny Blue
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * @author BunnyBlue
 * @author BunnyBlue
 * @author BunnyBlue
 */
/**
 * @author BunnyBlue
 */
package com.openatlas.android.initializer;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.openatlas.framework.AtlasConfig;
import com.openatlas.framework.OpenAtlasInternalConstant;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utils used for  initializer
 **/
public class Utils {

    /**get file name from entryName***/
    public static String getFileNameFromEntryName(String entryName) {
        String local = "lib/" + AtlasConfig.PRELOAD_DIR + "/";
        return entryName.substring(entryName.indexOf(local) + local.length());
    }

    /***get bundle name from archive file(hose app apk)
     * @param entryName   bundle entryName in archive file
     * ****/
    public static String getPackageNameFromEntryName(String entryName) {
        String local = "lib/" + AtlasConfig.PRELOAD_DIR + "/lib";
        return entryName.substring(entryName.indexOf(local) + local.length(), entryName.indexOf(".so")).replace("_", ".");
    }

    /**get bundle name from so file <br> such as libcom_myapp_app1.so,you will get com.myapp.app1******/
    public static String getPackageNameFromSoName(String soName) {
        return soName.substring(soName.indexOf("lib") + "lib".length(), soName.indexOf(".so")).replace("_", ".");
    }

    /****get file name ,exclude  extension***/
    public static String getBaseFileName(String fileName) {
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf > 0) {
            return fileName.substring(0, lastIndexOf);
        }
        return fileName;
    }

    public static PackageInfo getPackageInfo(Application application) {
        try {
            return application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
        } catch (Throwable e) {
            Log.e("Utils", "Error to get PackageInfo >>>", e);
            return new PackageInfo();
        }
    }

    /**
     * save OpenAtlas runtime info to sharedPreference
     **/
    public static void saveAtlasInfoBySharedPreferences(Application application) {
        Map<String, String> concurrentHashMap = new ConcurrentHashMap<String, String>();
        concurrentHashMap.put(getPackageInfo(application).versionName, "dexopt");
        SharedPreferences sharedPreferences = application.getSharedPreferences(OpenAtlasInternalConstant.OPENATLAS_CONFIGURE, Context.MODE_PRIVATE);
        if (sharedPreferences == null) {
            sharedPreferences = application.getSharedPreferences(OpenAtlasInternalConstant.OPENATLAS_CONFIGURE, Context.MODE_PRIVATE);
        }
        Editor edit = sharedPreferences.edit();
        for (String key : concurrentHashMap.keySet()) {
            edit.putString(key, concurrentHashMap.get(key));
        }
        edit.commit();
    }

    /****update version info***/
    public static void updatePackageVersion(Application application) {
        PackageInfo packageInfo = getPackageInfo(application);
        Editor edit = application.getSharedPreferences(OpenAtlasInternalConstant.OPENATLAS_CONFIGURE, Context.MODE_PRIVATE).edit();
        edit.putInt("last_version_code", packageInfo.versionCode);
        edit.putString("last_version_name", packageInfo.versionName);
        edit.putString(packageInfo.versionName, "dexopt");
        edit.commit();
    }

    /***nofity UI bundle installed***/
    public static void notifyBundleInstalled(Application application) {
        System.setProperty("BUNDLES_INSTALLED", "true");
        application.sendBroadcast(new Intent(OpenAtlasInternalConstant.ACTION_BROADCAST_BUNDLES_INSTALLED));
    }

    /*****find  file from specific directory
     * @param directoryPath directory
     * @param keyword keyword of file
     * *******/
    public static boolean searchFile(String directoryPath, String keyword) {
        if (directoryPath == null || keyword == null) {
            Log.e("Utils", "error in search File, direcoty or keyword is null");
            return false;
        }
        File direcotyFile = new File(directoryPath);
        if (direcotyFile == null || !direcotyFile.exists()) {
            Log.e("Utils", "error in search File, can not open directory " + directoryPath);
            return false;
        }
        File[] listFiles = new File(directoryPath).listFiles();
        if (listFiles == null || listFiles.length <= 0) {
            return false;
        }
        for (File file : listFiles) {
            if (file.getName().indexOf(keyword) >= 0) {
                Log.d("Util", "the file search success " + file.getName() + " keyword is " + keyword);
                return true;
            }
        }
        Log.e("Util", "the file search failed directory is " + directoryPath + " keyword is " + keyword);
        return false;
    }
}