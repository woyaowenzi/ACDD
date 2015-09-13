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
 */
package com.openatlas.android.initializer;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import com.openatlas.android.task.Coordinator;
import com.openatlas.android.task.Coordinator.TaggedRunnable;
import com.openatlas.framework.OpenAtlasInternalConstant;
import com.openatlas.runtime.Globals;
import com.openatlas.bundleInfo.BundleInfoList;
import com.openatlas.framework.OpenAtlas;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;
import com.openatlas.util.ApkUtils;

import java.util.Properties;

public class OpenAtlasInitializer {
	Logger log=LoggerFactory.getInstance("OpenAtlasInitializer");
    private static long initStartTime = 0;
    private static boolean inTargetApp;
    private Application mApplication;
    private String mPackageName;
    private BundleDebug mDebug;
    private boolean tryInstall;

    private Properties mProperties = new Properties();
    private boolean isUpdate = false;


    public OpenAtlasInitializer(Application application, String packagename, boolean isUpdate) {
        this.mApplication = application;
        this.mPackageName = packagename;

        this.isUpdate = isUpdate;
        if (application.getPackageName().equals(packagename)) {
            inTargetApp = true;
        }
    }

    public void init() {
    
        initStartTime = System.currentTimeMillis();

        try {
            OpenAtlas.getInstance().init(this.mApplication);
            log.debug("OpenAtlas framework inited end " + this.mPackageName + " " + (System.currentTimeMillis() - initStartTime) + " ms");
        } catch (Throwable e) {
            Log.e("OpenAtlasInitializer", "Could not init atlas framework !!!", e);
            throw new RuntimeException("atlas initialization fail" + e.getMessage());
        }
    }

    public void startUp() {

        this.mProperties.put(OpenAtlasInternalConstant.BOOT_ACTIVITY, OpenAtlasInternalConstant.BOOT_ACTIVITY);
        this.mProperties.put(OpenAtlasInternalConstant.COM_OPENATLAS_DEBUG_BUNDLES, "true");
        this.mProperties.put(OpenAtlasInternalConstant.ATLAS_APP_DIRECTORY, this.mApplication.getFilesDir().getParent());

        try {

            Globals.init(this.mApplication, OpenAtlas.getInstance().getDelegateClassLoader());
            this.mDebug = new BundleDebug();
            if (this.mApplication.getPackageName().equals(this.mPackageName)) {
                if (!( verifyRuntime() || !ApkUtils.isRootSystem())) {
                    this.mProperties.put(OpenAtlasInternalConstant.OPENATLAS_PUBLIC_KEY, SecurityBundleListner.PUBLIC_KEY);
                    OpenAtlas.getInstance().addBundleListener(new SecurityBundleListner());
                }
                if (this.isUpdate || this.mDebug.isDebugable()) {
                    this.mProperties.put("osgi.init", "true");
                }
            }
           BundlesInstaller mBundlesInstaller = BundlesInstaller.getInstance();
            OptDexProcess mOptDexProcess = OptDexProcess.getInstance();
            if (this.mApplication.getPackageName().equals(this.mPackageName) && (this.isUpdate || this.mDebug.isDebugable())) {
            	mBundlesInstaller.init(this.mApplication,  this.mDebug, inTargetApp);
                mOptDexProcess.init(this.mApplication);
            }
            log.debug("OpenAtlas framework prepare starting in process " + this.mPackageName + " " + (System.currentTimeMillis() - initStartTime) + " ms");
            OpenAtlas.getInstance().setClassNotFoundInterceptorCallback(new ClassNotFoundInterceptor());
            if (InstallPolicy.install_when_findclass && BundleInfoList.getInstance().getBundles()==null) {
            	InstallPolicy.install_when_oncreate = true;
                this.tryInstall = true;
            }

            try {
                OpenAtlas.getInstance().startup(this.mProperties);
                installBundles(mBundlesInstaller, mOptDexProcess);
                log.debug("OpenAtlas framework end startUp in process " + this.mPackageName + " " + (System.currentTimeMillis() - initStartTime) + " ms");
            } catch (Throwable e) {
                Log.e("OpenAtlasInitializer", "Could not start up atlas framework !!!", e);
                throw new RuntimeException(e);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Could not set Globals !!!", e);
        }
    }

    private void installBundles(final BundlesInstaller mBundlesInstaller, final OptDexProcess mOptDexProcess) {

        if (this.mDebug.isDebugable()) {
        	InstallPolicy.install_when_oncreate = true;
        }
        if (this.mApplication.getPackageName().equals(this.mPackageName)) {
            if (InstallPolicy.install_when_oncreate) {

            }
            if (this.isUpdate || this.mDebug.isDebugable()) {
                if (InstallPolicy.install_when_oncreate) {
                    Coordinator.postTask(new  TaggedRunnable("AtlasStartup") {
						@Override
						public void run() {
							mBundlesInstaller.process(true, false);
							mOptDexProcess.processPackages(true, false);
							
						}
					});

                    return;
                }
                Utils.notifyBundleInstalled(mApplication);
                Utils.updatePackageVersion(this.mApplication);
                Utils.saveAtlasInfoBySharedPreferences(this.mApplication);
            } else if (!this.isUpdate) {
                if (this.tryInstall) {
                    Coordinator.postTask(new TaggedRunnable("AtlasStartup") {
						@Override
						public void run() {
							mBundlesInstaller.process(false, false);
							mOptDexProcess.processPackages(false, false);
						}
					});
                    return;
                }else{
                     Utils.notifyBundleInstalled(mApplication);
                }
            }
        }
    }



    @SuppressLint({"DefaultLocale"})
    private boolean verifyRuntime() {
    
        if ((Build.BRAND == null || !Build.BRAND.toLowerCase().contains("xiaomi") || Build.HARDWARE == null || !Build.HARDWARE.toLowerCase().contains("mt65")) && VERSION.SDK_INT >= 14) {
            return false;
        }
        return true;
    }


}
