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
package com.openatlas.framework;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Process;

import com.openatlas.framework.bundlestorage.BundleArchive;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;
import com.openatlas.log.OpenAtlasMonitor;
import com.openatlas.runtime.ClassNotFoundInterceptorCallback;
import com.openatlas.runtime.RuntimeVariables;
import com.openatlas.util.BundleLock;
import com.openatlas.util.FileUtils;
import com.openatlas.util.OpenAtlasFileLock;
import com.openatlas.util.OpenAtlasUtils;
import com.openatlas.util.StringUtils;

import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.startlevel.StartLevel;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

//import org.osgi.framework.ServiceReference;

public final class Framework {
    private static final AdminPermission ADMIN_PERMISSION = new AdminPermission();
    private static String BASEDIR = null;
    private static String BUNDLE_LOCATION = null;
    static int CLASSLOADER_BUFFER_SIZE = 0;
    static boolean DEBUG_BUNDLES = true;
    static boolean DEBUG_CLASSLOADING = true;
    static boolean DEBUG_PACKAGES = true;
    static boolean DEBUG_SERVICES = true;
    static final String FRAMEWORK_VERSION = "1.0.0";
    private static final String DOWN_GRADE_FILE = "down_grade_list";
    static int LOG_LEVEL;
    static String STORAGE_LOCATION;
    @SuppressWarnings("unused")
    private static boolean STRICT_STARTUP;
    static List<BundleListener> bundleListeners = new ArrayList<BundleListener>();
    static Map<String, Bundle> bundles = new ConcurrentHashMap<String, Bundle>();
    private static ClassNotFoundInterceptorCallback classNotFoundCallback;
    static List<FrameworkListener> frameworkListeners = new ArrayList<FrameworkListener>();
    static boolean frameworkStartupShutdown = false;
    static int initStartlevel = 1;
    static final Logger log = LoggerFactory.getInstance("Framework");
    static Properties properties;
    static boolean restart = false;
    static int startlevel = 0;
    static List<BundleListener> syncBundleListeners = new ArrayList<BundleListener>();
    static SystemBundle systemBundle;
    static ClassLoader systemClassLoader;
    static List<String> writeAheads = new ArrayList<String>();



    private static final class SystemBundle implements Bundle,  StartLevel {
        private final Dictionary<String, String> props;
        int state;

        class ShutdownThread extends Thread {
            final boolean restart;

            ShutdownThread(boolean restart) {
                this.restart = restart;
            }

            @Override
            public void run() {
                Framework.shutdown(this.restart);
            }
        }

        class UpdateLevelThread extends Thread {
            final int targetLevel;

            UpdateLevelThread(int i) {
                this.targetLevel = i;
            }

            @Override
            public void run() {
                List bundles = Framework.getBundles();
                SystemBundle.this.setLevel((Bundle[]) bundles.toArray(new Bundle[bundles.size()]), this.targetLevel, false);
                Framework.notifyFrameworkListeners(BundleEvent.UPDATED, Framework.systemBundle, null);
                Framework.storeMetadata();
            }
        }


        class RefreshBundlesThread extends Thread {
            final Bundle[] bundleArray;

            RefreshBundlesThread(Bundle[] bundleArr) {
                this.bundleArray = bundleArr;
            }

            @Override
            public void run() {
            }
        }

        SystemBundle() {
            this.props = new Hashtable<String, String>();
            this.props.put(Constants.BUNDLE_NAME, Constants.SYSTEM_BUNDLE_LOCATION);
            this.props.put(Constants.BUNDLE_VERSION, Framework.FRAMEWORK_VERSION);
            this.props.put(Constants.BUNDLE_VENDOR, "OpenAtlas");
        }

        @Override
        public long getBundleId() {
            return 0;
        }

        @Override
        public Dictionary<String, String> getHeaders() {
            return this.props;
        }

        @Override
        public String getLocation() {
            return Constants.SYSTEM_BUNDLE_LOCATION;
        }

//        @Override
//        public ServiceReference[] getRegisteredServices() {
//            return null;
//        }

        @Override
        public URL getResource(String name) {
            return getClass().getResource(name);
        }

//        @Override
//        public ServiceReference[] getServicesInUse() {
//            return null;
//        }

        @Override
        public int getState() {
            return this.state;
        }

        @Override
        public boolean hasPermission(Object permission) {
            return true;
        }

        @Override
        public void start() throws BundleException {
        }

        @Override
        public void stop() throws BundleException {
            shutdownThread(false);
        }

        @Override
        public void uninstall() throws BundleException {
            throw new BundleException("Cannot uninstall the System Bundle");
        }

        @Override
        public void update() throws BundleException {
            shutdownThread(true);
        }

        private void shutdownThread(boolean z) {
            new ShutdownThread(z).start();
        }

        @Override
        public void update(InputStream inputStream) throws BundleException {
            shutdownThread(true);
        }

        @Override
        public void update(File file) throws BundleException {
            shutdownThread(true);
        }

        @Override
        public int getBundleStartLevel(Bundle bundle) {
            if (bundle == this) {
                return 0;
            }
            BundleImpl bundleImpl = (BundleImpl) bundle;
            if (bundleImpl.state != BundleEvent.INSTALLED) {
                return bundleImpl.currentStartlevel;
            }
            throw new IllegalArgumentException("Bundle " + bundle + " has been uninstalled");
        }

        @Override
        public int getInitialBundleStartLevel() {
            return Framework.initStartlevel;
        }

        @Override
        public int getStartLevel() {
            return Framework.startlevel;
        }

        @Override
        public boolean isBundlePersistentlyStarted(Bundle bundle) {
            if (bundle == this) {
                return true;
            }
            BundleImpl bundleImpl = (BundleImpl) bundle;
            if (bundleImpl.state != BundleEvent.INSTALLED) {
                return bundleImpl.persistently;
            }
            throw new IllegalArgumentException("Bundle " + bundle + " has been uninstalled");
        }

        @Override
        public void setBundleStartLevel(Bundle bundle, int level) {
            if (bundle == this) {
                throw new IllegalArgumentException("Cannot set the start level for the system bundle.");
            }
            BundleImpl bundleImpl = (BundleImpl) bundle;
            if (bundleImpl.state == BundleEvent.INSTALLED) {
                throw new IllegalArgumentException("Bundle " + bundle + " has been uninstalled");
            } else if (level <= 0) {
                throw new IllegalArgumentException("Start level " + level + " is not Component valid level");
            } else {
                bundleImpl.currentStartlevel = level;
                bundleImpl.updateMetadata();
                if (level <= Framework.startlevel && bundle.getState() != BundleEvent.RESOLVED && bundleImpl.persistently) {
                    try {
                        bundleImpl.startBundle();
                    } catch (Throwable e) {
                        e.printStackTrace();
                        Framework.notifyFrameworkListeners(BundleEvent.STARTED, bundle, e);
                    }
                } else if (level <= Framework.startlevel) {
                } else {
                    if (bundle.getState() != BundleEvent.STOPPED || bundle.getState() != BundleEvent.STARTED) {
                        try {
                            bundleImpl.stopBundle();
                        } catch (Throwable e2) {
                            Framework.notifyFrameworkListeners(BundleEvent.STARTED, bundle, e2);
                        }
                    }
                }
            }
        }

        @Override
        public void setInitialBundleStartLevel(int level) {
            if (level <= 0) {
                throw new IllegalArgumentException("Start level " + level + " is not Component valid level");
            }
            Framework.initStartlevel = level;
        }

        @Override
        public void setStartLevel(int i) {
            if (i <= 0) {
                throw new IllegalArgumentException("Start level " + i + " is not Component valid level");
            }
            new UpdateLevelThread(i).start();
        }

        @SuppressLint({"UseSparseArrays"})
        private void setLevel(Bundle[] bundles, int startlevel, boolean z) {
            if (Framework.startlevel != startlevel) {
                int iStartlevelHigh = startlevel > Framework.startlevel ? 1 : 0;
                int levelDiff = iStartlevelHigh != 0 ? startlevel - Framework.startlevel : Framework.startlevel - startlevel;
                Map hashMap = new HashMap(0);
                for (Bundle mBundle:bundles){
                    if (mBundle != Framework.systemBundle && (z || ((BundleImpl)mBundle).persistently)) {
                        int mLevelDiff;
                        BundleImpl bundleImpl = (BundleImpl) mBundle;
                        if (iStartlevelHigh != 0) {
                            mLevelDiff = (bundleImpl.currentStartlevel - Framework.startlevel) - 1;
                        } else {
                            mLevelDiff = Framework.startlevel - bundleImpl.currentStartlevel;
                        }
                        if (mLevelDiff >= 0 && mLevelDiff < levelDiff) {
                            Framework.addValue(hashMap, Integer.valueOf(mLevelDiff), bundleImpl);
                        }
                    }
                }

                for (int j = 0; j < levelDiff; j++) {
                    if (iStartlevelHigh != 0) {
                        Framework.startlevel++;
                    } else {
                        Framework.startlevel--;
                    }
                    List list = (List) hashMap.get(Integer.valueOf(j));
                    if (list != null) {
                        BundleImpl[] bundleImplArr = (BundleImpl[]) list.toArray(new BundleImpl[list.size()]);
                        for (int i = 0; i < bundleImplArr.length; i++) {
                            if (iStartlevelHigh != 0) {
                                try {
                                    System.out.println("STARTING " + bundleImplArr[i].location);
                                    bundleImplArr[i].startBundle();
                                } catch (Throwable e) {
                                    e.printStackTrace();
                                    e.printStackTrace();
                                    Framework.notifyFrameworkListeners(FrameworkEvent.ERROR, Framework.systemBundle, e);
                                }
                            } else if (bundleImplArr[i].getState() != 1) {
                                System.out.println("STOPPING " + bundleImplArr[i].location);
                                try {
                                    bundleImplArr[(bundleImplArr.length - i) - 1].stopBundle();
                                } catch (BundleException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                Framework.startlevel = startlevel;
            }
        }

        //@Override
        public void refreshPackages(Bundle[] bundleArr) {
            new RefreshBundlesThread(bundleArr).start();
        }

        @Override
        public String toString() {
            return "SystemBundle";
        }
    }

    static BundleImpl installNewBundle(String location, File apkFile) throws BundleException {
        BundleImpl bundleImpl;
        File mBundleArchiveFile = null;
        try {
            BundleLock.WriteLock(location);
            bundleImpl = (BundleImpl) Framework.getBundle(location);
            if (bundleImpl != null) {
                BundleLock.WriteUnLock(location);
            } else {
                mBundleArchiveFile = new File(STORAGE_LOCATION, location);

                OpenAtlasFileLock.getInstance().LockExclusive(mBundleArchiveFile);
                if (mBundleArchiveFile.exists()) {
                    bundleImpl = restoreFromExistedBundle(location, mBundleArchiveFile);
                    if (bundleImpl != null) {
                        BundleLock.WriteUnLock(location);
                        if (mBundleArchiveFile != null) {
                            OpenAtlasFileLock.getInstance().unLock(mBundleArchiveFile);
                        }
                    }
                }
                bundleImpl = new BundleImpl(mBundleArchiveFile, location, null, apkFile, true);
                storeMetadata();
                BundleLock.WriteUnLock(location);
                if (mBundleArchiveFile != null) {
                    OpenAtlasFileLock.getInstance().unLock(mBundleArchiveFile);
                }
            }
        } catch (Throwable e) {

            e.printStackTrace();
            BundleLock.WriteUnLock(location);
            throw new BundleException(e.getMessage());
        }

        return bundleImpl;
    }

    static boolean restoreBundle(String[] packageNames) {

        try {
            for (String pkgName : packageNames) {
                File archiveFile = new File(STORAGE_LOCATION, pkgName);
                if (!archiveFile.exists() || !BundleArchive.downgradeRevision(archiveFile)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static BundleImpl installNewBundle(String location, InputStream archiveInputStream) throws BundleException {
        BundleImpl bundleImpl = null;
        File mBundleArchiveFile = null;
        try {
            BundleLock.WriteLock(location);
            bundleImpl = (BundleImpl) getBundle(location);
            if (bundleImpl != null) {
                BundleLock.WriteUnLock(location);

            } else {
                mBundleArchiveFile = new File(STORAGE_LOCATION, location);
                OpenAtlasFileLock.getInstance().LockExclusive(mBundleArchiveFile);
                if (mBundleArchiveFile.exists()) {
                    bundleImpl = restoreFromExistedBundle(location, mBundleArchiveFile);
                    if (bundleImpl != null) {
                        BundleLock.WriteUnLock(location);
                        if (location != null) {
                            OpenAtlasFileLock.getInstance().unLock(mBundleArchiveFile);
                        }
                    }
                }
                bundleImpl = new BundleImpl(mBundleArchiveFile, location, archiveInputStream, null, true);
                storeMetadata();
                BundleLock.WriteUnLock(location);
                if (mBundleArchiveFile != null) {
                    OpenAtlasFileLock.getInstance().unLock(mBundleArchiveFile);
                }

            }
        } catch (Throwable v0) {
            BundleLock.WriteUnLock(location);
        }

        return bundleImpl;
    }


    private Framework() {
    }

    static void startup(Properties properties) throws BundleException {
        if (properties == null) {
            properties = new Properties();
        }
        Framework.properties = properties;
        startup();
    }

    private static void startup() throws BundleException {
        int startlevel;
        frameworkStartupShutdown = true;
        System.out.println("---------------------------------------------------------");
        System.out.println("  OpenAtlas OSGi 1.0.0  Pre-Release on " + Build.MODEL + "/" + Build.CPU_ABI + "/"
                + VERSION.RELEASE +" SDK version "+Build.VERSION.SDK_INT+ " starting ...");
        System.out.println("---------------------------------------------------------");
        long currentTimeMillis = System.currentTimeMillis();
        initialize();
        Framework.launch();
        boolean init = getProperty("osgi.init", false);
        if (init) {
            startlevel = -1;
        } else {
            startlevel = restoreProfile();
            restart = true;
        }
        if (startlevel == -1) {
            restart = false;
            File file = new File(STORAGE_LOCATION);
            if (init && file.exists()) {
                System.out.println("Purging storage ...");
                try {
                    deleteDirectory(file);
                } catch (Throwable e) {
                    throw new RuntimeException("deleteDirectory failed", e);
                }
            }
            try {
                file.mkdirs();
                Integer.getInteger("osgi.maxLevel", Integer.valueOf(1)).intValue();
                initStartlevel = getProperty("osgi.startlevel.bundle", 1);
                startlevel = getProperty("osgi.startlevel.framework", 1);
            } catch (Throwable e) {
                throw new RuntimeException("mkdirs failed", e);
            }
        }

        notifyFrameworkListeners(FrameworkEvent.STARTING, systemBundle, null);
        systemBundle.setLevel(getBundles().toArray(new Bundle[bundles.size()]), startlevel, false);
        frameworkStartupShutdown = false;
        if (!restart) {
            try {
                storeProfile();
            } catch (Throwable e) {
                throw new RuntimeException("storeProfile failed", e);
            }
        }
        long currentTimeMillis2 = System.currentTimeMillis() - currentTimeMillis;
        System.out.println("---------------------------------------------------------");
        System.out.println("  Framework " + (restart ? "restarted" : "started") + " in " + currentTimeMillis2 + " milliseconds.");
        System.out.println("---------------------------------------------------------");
        System.out.flush();
        systemBundle.state = BundleEvent.RESOLVED;
        try {
            notifyFrameworkListeners(FrameworkEvent.STARTED, systemBundle, null);
        } catch (Throwable e) {
            throw new RuntimeException("notifyFrameworkListeners failed", e);
        }
    }

    public static ClassLoader getSystemClassLoader() {
        return systemClassLoader;
    }

    public static List<Bundle> getBundles() {
        List<Bundle> arrayList = new ArrayList<Bundle>(bundles.size());
        synchronized (bundles) {
            arrayList.addAll(bundles.values());
        }
        return arrayList;
    }

    public static Bundle getBundle(String location) {
        return bundles.get(location);
    }

    public static Bundle getBundle(long j) {
        return null;
    }

    static void shutdown(boolean restart) {
        System.out.println("---------------------------------------------------------");
        System.out.println("  OpenAtlas OSGi shutting down ...");
        System.out.println("  Bye !");
        System.out.println("---------------------------------------------------------");
        systemBundle.state = BundleEvent.UNINSTALLED;
        systemBundle.setLevel(getBundles().toArray(new Bundle[bundles.size()]), 0, true);
        bundles.clear();
        systemBundle.state = BundleEvent.INSTALLED;
        if (restart) {
            try {
                startup();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }
    }

    public static void initialize() {

        File filesDir = RuntimeVariables.androidApplication.getFilesDir();
        if (filesDir == null || !filesDir.exists()) {
            filesDir = RuntimeVariables.androidApplication.getFilesDir();
        }
        BASEDIR = properties.getProperty(OpenAtlasInternalConstant.OPENATLAS_BASEDIR, filesDir.getAbsolutePath());
        BUNDLE_LOCATION = properties.getProperty(OpenAtlasInternalConstant.OPENATLAS_BUNDLE_LOCATION, "file:" + BASEDIR);
        CLASSLOADER_BUFFER_SIZE = getProperty(OpenAtlasInternalConstant.OPENATLAS_CLASSLOADER_BUFFER_SIZE, 1024 * 10);
        LOG_LEVEL = getProperty(OpenAtlasInternalConstant.OPENATLAS_LOG_LEVEL, 6);
        DEBUG_BUNDLES = getProperty(OpenAtlasInternalConstant.OPENATLAS_DEBUG_BUNDLES, false);
        DEBUG_PACKAGES = getProperty(OpenAtlasInternalConstant.OPENATLAS_DEBUG_PACKAGES, false);
        DEBUG_SERVICES = getProperty(OpenAtlasInternalConstant.OPENATLAS_DEBUG_SERVICES, false);
        DEBUG_CLASSLOADING = getProperty(OpenAtlasInternalConstant.OPENATLAS_DEBUG_CLASSLOADING, false);
        if (getProperty(OpenAtlasInternalConstant.OPENATLAS_DEBUG, false)) {
            System.out.println("SETTING ALL DEBUG FLAGS");
            LOG_LEVEL = 3;
            DEBUG_BUNDLES = true;
            DEBUG_PACKAGES = true;
            DEBUG_SERVICES = true;
            DEBUG_CLASSLOADING = true;
        }
        STRICT_STARTUP = getProperty(OpenAtlasInternalConstant.OPENATLAS_STRICT_STARTUP, false);
        String property = properties.getProperty("org.osgi.framework.system.packages");
        if (property != null) {
            StringTokenizer stringTokenizer = new StringTokenizer(property, ",");
            int countTokens = stringTokenizer.countTokens();
            for (int i = 0; i < countTokens; i++) {
                BundleClassLoader.FRAMEWORK_PACKAGES.add(stringTokenizer.nextToken().trim());
            }
        }
        properties.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, System.getProperty("java.specification.name") + "/" + System.getProperty("java.specification.version"));
        String key = Constants.FRAMEWORK_OS_NAME;
         property = System.getProperty("os.name");
        if (property == null) {
            property = "undefined";
        }
        properties.put(key, property);

        key = Constants.FRAMEWORK_OS_VERSION;
        property = System.getProperty("os.version");
        if (property == null) {
            property = "undefined";
        }
        properties.put(key, property);

        key = Constants.FRAMEWORK_PROCESSOR;
        property = System.getProperty("os.arch");
        if (property == null) {
            property = "undefined";
        }
        properties.put(key, property);
        properties.put(Constants.FRAMEWORK_VERSION, FRAMEWORK_VERSION);
        properties.put(Constants.FRAMEWORK_VENDOR, "OpenAtlas");
        property = Locale.getDefault().getLanguage();

        key = Constants.FRAMEWORK_LANGUAGE;
        if (property == null) {
            property = "en";
        }
        properties.put(key,property);


    }

    private static void launch() {
        STORAGE_LOCATION = properties.getProperty(OpenAtlasInternalConstant.INSTALL_LOACTION, properties.getProperty("org.osgi.framework.dir", BASEDIR + File.separatorChar + "storage"))
                + File.separatorChar;
        systemBundle = new SystemBundle();
        systemBundle.state = BundleEvent.UPDATED;
    }

    public static boolean getProperty(String key, boolean defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        String value = (String) properties.get(key);
        return value != null ? Boolean.valueOf(value).booleanValue() : defaultValue;
    }

    public static int getProperty(String key, int defaultValue) {
        if (properties == null) {
            return defaultValue;
        }
        String value = (String) properties.get(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    public static String getProperty(String key) {
        if (properties == null) {
            return null;
        }
        return (String) properties.get(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties == null ? defaultValue : (String) properties.get(key);
    }

    protected static void warning(String message) throws RuntimeException {
        if (getProperty(OpenAtlasInternalConstant.OPENATLAS_STRICT_STARTUP, false)) {
            throw new RuntimeException(message);
        }
        System.err.println("WARNING: " + message);
    }

    private static void storeProfile() {
        BundleImpl[] bundleImplArr = getBundles().toArray(new BundleImpl[bundles.size()]);
        for (BundleImpl updateMetadata : bundleImplArr) {
            updateMetadata.updateMetadata();
        }
        storeMetadata();
    }

    static void storeMetadata() {

        try {
            File metaFile = new File(STORAGE_LOCATION, "meta");

            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(metaFile));
            dataOutputStream.writeInt(startlevel);
            String join = StringUtils.join(writeAheads.toArray(), ",");
            if (join == null) {
                join = "";
            }
            dataOutputStream.writeUTF(join);
            dataOutputStream.flush();
            dataOutputStream.close();

        } catch (IOException e) {
            OpenAtlasMonitor.getInstance().trace(Integer.valueOf(OpenAtlasMonitor.WRITE_META_FAIL), "", "", "storeMetadata failed ", e);
            log.error("Could not save meta data.", e);
        }
    }


    private static int restoreProfile() {
        try {
            System.out.println("Restoring profile");
            File meta = new File(STORAGE_LOCATION, "meta");
            if (meta.exists()) {
                DataInputStream dataInputStream = new DataInputStream(new FileInputStream(meta));
                int readInt = dataInputStream.readInt();
                String[] split = StringUtils.split(dataInputStream.readUTF(), ",");
                if (split != null) {
                    writeAheads.addAll(Arrays.asList(split));
                }
                dataInputStream.close();
                if (!getProperty(OpenAtlasInternalConstant.OPENATLAS_AUTO_LOAD, true)) {
                    return readInt;
                }
                File storageLocation = new File(STORAGE_LOCATION);
                mergeWalsDir(new File(STORAGE_LOCATION, "wal"), storageLocation);
                MergeWirteAheads(storageLocation);
                File[] listFiles = storageLocation.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File file, String str) {
                        return !str.matches("^[0-9]*");
                    }
                });
                int i = 0;
                while (i < listFiles.length) {
                    if (listFiles[i].isDirectory() && new File(listFiles[i], "meta").exists()) {
                        try {
                            System.out.println("RESTORED BUNDLE " + new BundleImpl(listFiles[i]).location);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e.getCause());
                        }
                    }
                    i++;
                }
                return readInt;
            }
            System.out.println("Profile not found, performing clean start ...");
            return -1;
        } catch (Exception e2) {
            e2.printStackTrace();
            return 0;
        }
    }

    private static void mergeWalsDir(File walFile, File storageLocation) {
        if (writeAheads != null && writeAheads.size() > 0) {
            for (int i = 0; i < writeAheads.size(); i++) {
                if (writeAheads.get(i) != null) {
                    File mHeadDir = new File(walFile, writeAheads.get(i));
                    if (mHeadDir != null) {
                        try {
                            if (mHeadDir.exists()) {
                                File[] mHeadFiles = mHeadDir.listFiles();
                                if (mHeadFiles != null) {
                                    for (File mHeadFile : mHeadFiles) {
                                        if (mHeadFile.isDirectory()) {
                                            File targetFile = new File(storageLocation, mHeadFile.getName());
                                            if (targetFile.exists()) {
                                                File[] reversionList = mHeadFile.listFiles(new FilenameFilter() {
                                                    @Override
                                                    public boolean accept(File file, String str) {
                                                        return str.startsWith(BundleArchive.REVISION_DIRECTORY);
                                                    }
                                                });
                                                if (reversionList != null) {
                                                    for (File mFile : reversionList) {
                                                        if (new File(mFile, "meta").exists()) {
                                                            mFile.renameTo(new File(targetFile, mFile.getName()));
                                                        }
                                                    }
                                                }
                                            } else {
                                                mHeadFile.renameTo(targetFile);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Throwable e) {
                            log.error("Error while merge wal dir", e);
                        }
                    }
                    writeAheads.set(i, null);
                }
            }
        }
        if (walFile.exists()) {
            walFile.delete();
        }
    }

    public static void deleteDirectory(File mDirectory) {
        File[] listFiles = mDirectory.listFiles();
        for (int i = 0; i < listFiles.length; i++) {
            if (listFiles[i].isDirectory()) {
                deleteDirectory(listFiles[i]);
            } else {
                listFiles[i].delete();
            }
        }
        mDirectory.delete();
    }


    static BundleImpl installNewBundle(String bundleName) throws BundleException {
        try {
            String location = bundleName.indexOf(":") > -1 ? bundleName : BUNDLE_LOCATION + File.separatorChar + bundleName;
            return installNewBundle(location, new URL(location).openConnection().getInputStream());
        } catch (Throwable e) {
            throw new BundleException("Cannot retrieve bundle from " + bundleName, e);
        }
    }

    private static BundleImpl restoreFromExistedBundle(String location, File file) {

        try {
            return new BundleImpl(file);
        } catch (Throwable e) {
            OpenAtlasMonitor.getInstance().trace(Integer.valueOf(-1), "", "", "restore bundle failed " + location + e);
            log.error("restore bundle failed" + location, e);
            return null;
        }
    }

    static void installOrUpdate(String[] locations, File[] archiveFiles) throws BundleException {
        if (locations == null || archiveFiles == null || locations.length != archiveFiles.length) {
            throw new IllegalArgumentException("locations and files must not be null and must be same length");
        }
        String valueOf = String.valueOf(System.currentTimeMillis());
        File file = new File(new File(STORAGE_LOCATION, "wal"), valueOf);
        file.mkdirs();
        int i = 0;
        while (i < locations.length) {
            if (!(locations[i] == null || archiveFiles[i] == null)) {
                try {
                    BundleLock.WriteLock(locations[i]);
                    Bundle bundle = getBundle(locations[i]);
                    if (bundle != null) {
                        bundle.update(archiveFiles[i]);
                    } else {
                        BundleImpl bundleImpl = new BundleImpl(new File(file, locations[i]), locations[i],  null, archiveFiles[i], false);
                    }
                    BundleLock.WriteUnLock(locations[i]);
                } catch (Throwable th) {
                    BundleLock.WriteUnLock(locations[i]);
                }
            }
            i++;
        }
        writeAheads.add(valueOf);
        storeMetadata();
    }


    static void notifyBundleListeners(int event, Bundle bundle) {

        if (!syncBundleListeners.isEmpty() || !bundleListeners.isEmpty()) {
            BundleEvent bundleEvent = new BundleEvent(event, bundle);
            BundleListener[] bundleListenerArr = syncBundleListeners.toArray(new BundleListener[syncBundleListeners.size()]);
            for (BundleListener bundleChanged : bundleListenerArr) {
                bundleChanged.bundleChanged(bundleEvent);
            }
            if (!bundleListeners.isEmpty()) {
                bundleListenerArr = bundleListeners.toArray(new BundleListener[bundleListeners.size()]);
                for (BundleListener bundleListener : bundleListenerArr) {
                    bundleListener.bundleChanged(bundleEvent);
                }

            }
        }
    }

    static void addFrameworkListener(FrameworkListener frameworkListener) {
        frameworkListeners.add(frameworkListener);
    }

    static void removeFrameworkListener(FrameworkListener frameworkListener) {
        frameworkListeners.remove(frameworkListener);
    }

    private static void restoreBundles() throws IOException {
        File file = new File(STORAGE_LOCATION, DOWN_GRADE_FILE);
        for (String pkg : FileUtils.getStrings(file)) {

            File locationFolder = new File(STORAGE_LOCATION, pkg);
            if (locationFolder.exists()) {
                String[] list = locationFolder.list();
                String version = null;
                if (list != null) {
                    for (String string : list) {
                        if (string.startsWith("version")
                                || Long.parseLong(StringUtils.substringAfter(string, ".")) <= 0) {
                            version = string;
                        }
                    }

                }
                if (version == null) {
                    FileUtils.deleteFile(locationFolder.getAbsolutePath());
                } else {
                    File tmp = new File(locationFolder, version);
                    if (tmp.exists()) {
                        FileUtils.deleteFile(tmp.getAbsolutePath());
                    }
                }
            }
        }
        if (file.exists()) {
            FileUtils.deleteFile(file.getAbsolutePath());
        }
    }

    private static void MergeWirteAheads(File storageLocation) {
        try {
            File wal = new File(STORAGE_LOCATION, "wal");
            String curProcessName = OpenAtlasUtils.getProcessNameByPID(Process.myPid());
            log.debug("restoreProfile in process " + curProcessName);
            String packageName = RuntimeVariables.androidApplication.getPackageName();
            if (curProcessName != null && packageName != null && curProcessName.equals(packageName)) {
                mergeWalsDir(wal, storageLocation);
            }
        } catch (Throwable th) {
            if (Build.MODEL == null || !Build.MODEL.equals("HTC 802w")) {
                log.error(th.getMessage(), th.getCause());
                return;
            }
           
        }
    }

    static void addBundleListener(BundleListener bundleListener) {
        bundleListeners.add(bundleListener);
    }

    static void removeBundleListener(BundleListener bundleListener) {
        bundleListeners.remove(bundleListener);
    }

    static void notifyFrameworkListeners(int event, Bundle bundle, Throwable th) {
        if (!frameworkListeners.isEmpty()) {
            FrameworkEvent frameworkEvent = new FrameworkEvent(event, bundle, th);
            FrameworkListener[] frameworkListenerArr = frameworkListeners.toArray(new FrameworkListener[frameworkListeners.size()]);
            for (FrameworkListener frameworkListener : frameworkListenerArr) {
                frameworkListener.frameworkEvent(frameworkEvent);
            }
        }
    }



    static void clearBundleTrace(BundleImpl bundleImpl) {

        if (bundleImpl.registeredFrameworkListeners != null) {
            frameworkListeners.removeAll(bundleImpl.registeredFrameworkListeners);
            bundleImpl.registeredFrameworkListeners = null;
        }

        if (bundleImpl.registeredBundleListeners != null) {
            bundleListeners.removeAll(bundleImpl.registeredBundleListeners);
            syncBundleListeners.removeAll(bundleImpl.registeredBundleListeners);
            bundleImpl.registeredBundleListeners = null;
        }

    }

    static void addValue(Map map, Object key, Object value) {
        List list = (List) map.get(key);
        if (list == null) {
            list = new ArrayList();
        }
        list.add(value);
        map.put(key, list);
    }

    static void removeValue(Map map, Object[] objArr, Object obj) {
        for (int i = 0; i < objArr.length; i++) {
            List list = (List) map.get(objArr[i]);
            if (list != null) {
                list.remove(obj);
                if (list.isEmpty()) {
                    map.remove(objArr[i]);
                } else {
                    map.put(objArr[i], list);
                }
            }
        }
    }





    public static boolean isFrameworkStartupShutdown() {
        return frameworkStartupShutdown;
    }

    public static ClassNotFoundInterceptorCallback getClassNotFoundCallback() {
        return classNotFoundCallback;
    }

    public static void setClassNotFoundCallback(ClassNotFoundInterceptorCallback classNotFoundInterceptorCallback) {
        classNotFoundCallback = classNotFoundInterceptorCallback;
    }


}
