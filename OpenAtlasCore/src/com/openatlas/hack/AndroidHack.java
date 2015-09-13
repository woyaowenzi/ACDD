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
package com.openatlas.hack;

import android.app.Application;
import android.app.Instrumentation;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.openatlas.hack.Hack.HackDeclaration.HackAssertionException;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;
import com.openatlas.runtime.DelegateClassLoader;
import com.openatlas.runtime.DelegateResources;
import com.openatlas.runtime.RuntimeVariables;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/****
 * Hack Android ActivityThread
 ***/
public class AndroidHack {
    private static Object _mLoadedApk;
    private static Object _sActivityThread;
    public static final int RECEIVER                = 113;
   static Logger logger= LoggerFactory.getInstance("AndroidHack");
    static final class HandlerHack implements Callback {
        final Object activityThread;
        final Handler handler;

        HandlerHack(Handler handler, Object obj) {
            this.handler = handler;
            this.activityThread = obj;
        }

        @Override
        public boolean handleMessage(Message message) {
            try {
                AndroidHack.ensureLoadedApk();
                this.handler.handleMessage(message);
                AndroidHack.ensureLoadedApk();
            } catch (Throwable th) {
                th.printStackTrace();

                if ((th instanceof ClassNotFoundException)
                        || th.toString().contains("ClassNotFoundException")) {
                    if (message.what != RECEIVER) {
                        Object loadedApk = AndroidHack.getLoadedApk(
                                RuntimeVariables.androidApplication,
                                this.activityThread,
                                RuntimeVariables.androidApplication
                                        .getPackageName());
                        if (loadedApk == null) {
                            logger.error("",new RuntimeException("loadedapk is null"));
                        } else {
                            ClassLoader classLoader = OpenAtlasHacks.LoadedApk_mClassLoader.get(loadedApk);
                            if (classLoader instanceof DelegateClassLoader) {
                                logger.error("",new RuntimeException("From OpenAtlas:classNotFound ---", th));

                            } else {
                                logger.error("",new RuntimeException("wrong classloader in loadedapk---" + classLoader.getClass().getName(), th));

                            }
                        }
                    }
                } else if ((th instanceof ClassCastException)
                        || th.toString().contains("ClassCastException")) {
                    Process.killProcess(Process.myPid());
                } else {
                    logger.error("", new RuntimeException(th));
                }
            }
            return true;
        }
    }

    static class ActvityThreadGetter implements Runnable {
        ActvityThreadGetter() {
        }

        @Override
        public void run() {
            try {
                AndroidHack._sActivityThread = OpenAtlasHacks.ActivityThread_currentActivityThread
                        .invoke(OpenAtlasHacks.ActivityThread.getmClass());
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (OpenAtlasHacks.ActivityThread_currentActivityThread) {
                OpenAtlasHacks.ActivityThread_currentActivityThread.notify();
            }
        }
    }

    static {
        _sActivityThread = null;
        _mLoadedApk = null;
    }

    public static Object getActivityThread() throws Exception {
        if (_sActivityThread == null) {
            if (Thread.currentThread().getId() == Looper.getMainLooper()
                    .getThread().getId()) {
                _sActivityThread = OpenAtlasHacks.ActivityThread_currentActivityThread
                        .invoke(null);
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                synchronized (OpenAtlasHacks.ActivityThread_currentActivityThread) {
                    handler.post(new ActvityThreadGetter());
                    OpenAtlasHacks.ActivityThread_currentActivityThread.wait();
                }
            }
        }
        return _sActivityThread;
    }

    /**
     * we  nedd hook H(handler),hanlde message
     ***/
    public static Handler hackH() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception(
                    "Failed to get ActivityThread.sCurrentActivityThread");
        }
        try {
            Handler handler = (Handler) OpenAtlasHacks.ActivityThread
                    .field("mH")
                    .ofType(Hack.into("android.app.ActivityThread$H")
                            .getmClass()).get(activityThread);
            Field declaredField = Handler.class.getDeclaredField("mCallback");
            declaredField.setAccessible(true);
            declaredField.set(handler, new HandlerHack(handler,
                    activityThread));
        } catch (HackAssertionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ensureLoadedApk() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception(
                    "Failed to get ActivityThread.sCurrentActivityThread");
        }
        Object loadedApk = getLoadedApk(RuntimeVariables.androidApplication,
                activityThread,
                RuntimeVariables.androidApplication.getPackageName());
        if (loadedApk == null) {
            loadedApk = createNewLoadedApk(RuntimeVariables.androidApplication,
                    activityThread);
            if (loadedApk == null) {
                throw new RuntimeException("can't create loadedApk");
            }
        }
        activityThread = loadedApk;
        if (!((OpenAtlasHacks.LoadedApk_mClassLoader
                .get(activityThread)) instanceof DelegateClassLoader)) {
            OpenAtlasHacks.LoadedApk_mClassLoader.set(activityThread,
                    RuntimeVariables.delegateClassLoader);
            OpenAtlasHacks.LoadedApk_mResources.set(activityThread,
                    RuntimeVariables.delegateResources);
        }
    }

    public static Object getLoadedApk(Application application, Object obj,
                                      String str) {
        WeakReference weakReference = (WeakReference) ((Map) OpenAtlasHacks.ActivityThread_mPackages
                .get(obj)).get(str);
        if (weakReference == null || weakReference.get() == null) {
            return null;
        }
        _mLoadedApk = weakReference.get();
        return _mLoadedApk;
    }

    public static Object createNewLoadedApk(Application application, Object obj) {
        try {
            Method declaredMethod;
            ApplicationInfo applicationInfo = application.getPackageManager()
                    .getApplicationInfo(application.getPackageName(), 1152);
            application.getPackageManager();
            Resources resources = application.getResources();
            if (resources instanceof DelegateResources) {
                declaredMethod = resources
                        .getClass()
                        .getSuperclass()
                        .getDeclaredMethod("getCompatibilityInfo");
            } else {
                declaredMethod = resources.getClass().getDeclaredMethod(
                        "getCompatibilityInfo");
            }
            declaredMethod.setAccessible(true);
            Class cls = Class.forName("android.content.res.CompatibilityInfo");
            Object invoke = declaredMethod.invoke(application.getResources()
            );
            Method declaredMethod2 = OpenAtlasHacks.ActivityThread.getmClass()
                    .getDeclaredMethod("getPackageInfoNoCheck",
                            ApplicationInfo.class, cls);
            declaredMethod2.setAccessible(true);
            invoke = declaredMethod2.invoke(obj, applicationInfo, invoke);
            _mLoadedApk = invoke;
            return invoke;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
/**
 * inject  system  classloader,we need handle  load class from  bundle
 * @param packageName  package name
 * @param classLoader    delegate  classloader
 * ***/
    public static void injectClassLoader(String packageName, ClassLoader classLoader)
            throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception(
                    "Failed to get ActivityThread.sCurrentActivityThread");
        }
        Object loadedApk = getLoadedApk(RuntimeVariables.androidApplication,
                activityThread, packageName);
        if (loadedApk == null) {
            loadedApk = createNewLoadedApk(RuntimeVariables.androidApplication,
                    activityThread);
        }
        if (loadedApk == null) {
            throw new Exception("Failed to get ActivityThread.mLoadedApk");
        }
        OpenAtlasHacks.LoadedApk_mClassLoader.set(loadedApk, classLoader);
    }

    public static void injectApplication(String packageName, Application application)
            throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception(
                    "Failed to get ActivityThread.sCurrentActivityThread");
        }
        Object loadedApk = getLoadedApk(application, activityThread,
                application.getPackageName());
        if (loadedApk == null) {
            throw new Exception("Failed to get ActivityThread.mLoadedApk");
        }
        OpenAtlasHacks.LoadedApk_mApplication.set(loadedApk, application);
        OpenAtlasHacks.ActivityThread_mInitialApplication.set(activityThread,
                application);
    }

    /***
     * hack Resource  use delegate resource,process  resource in bundle
     *
     * @param application host application object
     * @param resources   delegate resource
     *****/
    public static void injectResources(Application application,
                                       Resources resources) throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception(
                    "Failed to get ActivityThread.sCurrentActivityThread");
        }
        Object loadedApk = getLoadedApk(application, activityThread,
                application.getPackageName());
        if (loadedApk == null) {
            activityThread = createNewLoadedApk(application, activityThread);
            if (activityThread == null) {
                throw new RuntimeException(
                        "Failed to get ActivityThread.mLoadedApk");
            }
            if (!((OpenAtlasHacks.LoadedApk_mClassLoader
                    .get(activityThread)) instanceof DelegateClassLoader)) {
                OpenAtlasHacks.LoadedApk_mClassLoader.set(activityThread,
                        RuntimeVariables.delegateClassLoader);
            }
            loadedApk = activityThread;
        }
        OpenAtlasHacks.LoadedApk_mResources.set(loadedApk, resources);
        OpenAtlasHacks.ContextImpl_mResources.set(application.getBaseContext(),
                resources);
        OpenAtlasHacks.ContextImpl_mTheme.set(application.getBaseContext(), null);
    }

    /***
     * get Instrumentation,should be  hacked Instrumentation
     */
    public static Instrumentation getInstrumentation() throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread != null) {
            return OpenAtlasHacks.ActivityThread_mInstrumentation
                    .get(activityThread);
        }
        throw new Exception(
                "Failed to get ActivityThread.sCurrentActivityThread");
    }

    /***
     * hack Instrumentation,we replace Instrumentation used HackInstrumentation<br>
     * such start activity in Instrumentation ,before this ,we need verify  target class is loaded or
     * load  target class,and so on
     **/
    public static void injectInstrumentationHook(Instrumentation instrumentation)
            throws Exception {
        Object activityThread = getActivityThread();
        if (activityThread == null) {
            throw new Exception(
                    "Failed to get ActivityThread.sCurrentActivityThread");
        }
        OpenAtlasHacks.ActivityThread_mInstrumentation.set(activityThread,
                instrumentation);
    }

    @SuppressWarnings("unused")
    public static void injectContextHook(ContextWrapper contextWrapper,
                                         ContextWrapper contextWrapperValue) {
        OpenAtlasHacks.ContextWrapper_mBase.set(contextWrapper, contextWrapperValue);
    }
}
