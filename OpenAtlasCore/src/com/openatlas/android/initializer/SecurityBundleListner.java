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

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.openatlas.framework.OpenAtlas;
import com.openatlas.runtime.RuntimeVariables;
import com.openatlas.util.ApkUtils;
import com.openatlas.util.StringUtils;

import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import java.io.File;


public class SecurityBundleListner implements BundleListener {
    public static final String PUBLIC_KEY = "";
    ProcessHandler mProcessHandler;
    private Handler mSecurityCheckHandler;
    private HandlerThread mHandlerThread;


    private final class SecurityCheckHandler extends Handler {


        public SecurityCheckHandler(Looper looper) {
            super(looper);


        }

        @Override
        public void handleMessage(Message message) {

            if (message != null) {
                String location = (String) message.obj;
                if (!TextUtils.isEmpty(location) && !TextUtils.isEmpty(SecurityBundleListner.PUBLIC_KEY)) {
                    File bundleFile = OpenAtlas.getInstance().getBundleFile(location);
                    if (bundleFile != null) {
                        if (!StringUtils.contains(ApkUtils.getApkPublicKey(bundleFile.getAbsolutePath()), SecurityBundleListner.PUBLIC_KEY)) {
                            Log.e("SecurityBundleListner", "Security check failed. " + location);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(RuntimeVariables.androidApplication, "Public Key error，PLZ update your  public key", Toast.LENGTH_SHORT).show();
                                    mProcessHandler.sendEmptyMessageDelayed(0, 5000);
                                }
                            });

                        }

                    }
                }
            }
        }
    }

    public static class ProcessHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            Process.killProcess(Process.myPid());
        }
    }


    public SecurityBundleListner() {
        this.mHandlerThread = null;
        this.mProcessHandler = new ProcessHandler();

        this.mHandlerThread = new HandlerThread("Check bundle security");
        this.mHandlerThread.start();
        this.mSecurityCheckHandler = new SecurityCheckHandler(this.mHandlerThread.getLooper());
    }

    public void bundleChanged(BundleEvent bundleEvent) {

        switch (bundleEvent.getType()) {
            case BundleEvent.INSTALLED:
            case BundleEvent.UPDATED:
                Message obtain = Message.obtain();
                obtain.obj = bundleEvent.getBundle().getLocation();
                this.mSecurityCheckHandler.sendMessage(obtain);
                return;
            default:
                return;
        }
    }


}
