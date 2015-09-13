/**
 * OpenAtlasForAndroid Project
 * The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies
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
 **/
package com.openatlas.android.compat;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteException;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import com.openatlas.android.initializer.BundleParser;
import com.openatlas.android.initializer.OpenAtlasInitializer;
import com.openatlas.framework.OpenAtlasInternalConstant;
import com.openatlas.runtime.Globals;
import com.openatlas.runtime.ContextImplHook;
import com.openatlas.util.OpenAtlasUtils;

import java.lang.reflect.Field;


/****OpenAtlasApplication, you can  extend  this class direct****/
public class OpenAtlasApp extends Application {
    private static final Handler mAppHandler;

    private Context mBaseContext;
    OpenAtlasInitializer mAtlasInitializer;


    public OpenAtlasApp() {

    }


    public static void runOnUiThread(Runnable runnable) {
        mAppHandler.post(runnable);
    }

    /* (non-Javadoc)
     * @see android.content.ContextWrapper#attachBaseContext(android.content.Context)
     */
    @Override
    protected void attachBaseContext(Context base) {
        // TODO Auto-generated method stub
        super.attachBaseContext(base);
        this.mBaseContext = base;
        BundleParser.parser(getBaseContext());

        try {
            Field declaredField = Globals.class
                    .getDeclaredField("sInstalledVersionName");
            declaredField.setAccessible(true);
            declaredField.set(null, this.mBaseContext.getPackageManager()
                    .getPackageInfo(base.getPackageName(), 0).versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.mAtlasInitializer = new OpenAtlasInitializer(this, getPackageName(),isUpdate());
        //this.mAtlasInitializer.injectApplication();
        this.mAtlasInitializer.init();
    }
    private boolean isUpdate() {
        try {
            PackageInfo packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            SharedPreferences sharedPreferences =getSharedPreferences(OpenAtlasInternalConstant.OPENATLAS_CONFIGURE, 0);
            int last_version_code = sharedPreferences.getInt("last_version_code", 0);
            CharSequence last_version_name = sharedPreferences.getString("last_version_name", "");
//return true;
         return packageInfo.versionCode > last_version_code || ((packageInfo.versionCode == last_version_code && !TextUtils.equals(Globals.getInstalledVersionName(), last_version_name)) );
        } catch (Throwable e) {
            Log.e("OpenAtlasInitializer", "Error to get PackageInfo >>>", e);
            throw new RuntimeException(e);
        }
    }
    @Override
    public  void onCreate() {
        super.onCreate();

        this.mAtlasInitializer.startUp();

    }

    @Override
    public final boolean bindService(Intent intent,
                               ServiceConnection serviceConnection, int flags) {
        return new ContextImplHook(getBaseContext(), null).
                bindService(intent, serviceConnection, flags);
    }

    @Override
    public final void startActivity(Intent intent) {
        new ContextImplHook(getBaseContext(), getClassLoader()).startActivity(intent);
    }


    @Override
    public final ComponentName startService(Intent intent) {
        return new ContextImplHook(getBaseContext(), null).startService(intent);
    }

    @Override
    public final SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory cursorFactory) {
        String processName = OpenAtlasUtils.getProcessNameByPID(Process.myPid());
        if (!TextUtils.isEmpty(processName)) {
            Log.i("SQLiteDatabase", processName);
            if (!processName.equals(getPackageName())) {
                String[] split = processName.split(":");
                if (split != null && split.length > 1) {
                    processName = split[1] + "_" + name;
                    Log.i("SQLiteDatabase", "openOrCreateDatabase:" + processName);
                    return hookDatabase(processName, mode, cursorFactory);
                }
            }
        }
        return hookDatabase(name, mode, cursorFactory);
    }

    private SQLiteDatabase hookDatabase(String name, int mode, CursorFactory cursorFactory) {
        if (VERSION.SDK_INT >= 11) {
            return super.openOrCreateDatabase(name, mode, cursorFactory);
        }
        SQLiteDatabase sQLiteDatabase = null;
        try {
            return super.openOrCreateDatabase(name, mode, cursorFactory);
        } catch (SQLiteException e) {
            e.printStackTrace();
            if (Globals.getApplication().deleteDatabase(name)) {
                return super.openOrCreateDatabase(name, mode, cursorFactory);
            }
            return sQLiteDatabase;
        }
    }

    static {
        mAppHandler = new Handler();
    }
}