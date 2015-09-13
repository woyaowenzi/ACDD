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
package com.openatlas.framework.bundlestorage;

import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;

import com.openatlas.bundleInfo.BundleInfoList;
import com.openatlas.dexopt.InitExecutor;
import com.openatlas.framework.Framework;
import com.openatlas.hack.OpenAtlasHacks;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;
import com.openatlas.runtime.RuntimeVariables;
import com.openatlas.util.ApkUtils;
import com.openatlas.util.OpenAtlasFileLock;
import com.openatlas.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;
import dalvik.system.DexFile;
/***
 * BundleArchiveRevision is Bundle Archive real implementation
 * ***/
public class BundleArchiveRevision {
    static final String BUNDLE_FILE_NAME = "bundle.zip";
    static final String BUNDLE_LEX_FILE = "bundle.lex";
    static final String BUNDLE_ODEX_FILE = "bundle.dex";
    static final String FILE_PROTOCOL = "file:";
    static final String REFERENCE_PROTOCOL = "reference:";
    static final Logger log;
    private final File bundleFile;
    private ClassLoader dexClassLoader;
    private DexFile dexFile;
    private boolean isDexFileUsed;
    private Manifest manifest;
    private final File revisionDir;
    private final String revisionLocation;
    private final long revisionNum;
    private ZipFile zipFile;

    class BundleArchiveRevisionClassLoader extends DexClassLoader {
        /**
         * @param dexPath the list of jar/apk files containing classes and resources, delimited by File.pathSeparator, which defaults to ":" on Android
         * @param optimizedDirectory directory where optimized dex files should be written; must not be null
         * @param libraryPath the list of directories containing native libraries, delimited by File.pathSeparator; may be null
         * @param
         * **/
        BundleArchiveRevisionClassLoader(String dexPath, String optimizedDirectory, String libraryPath,
                                         ClassLoader parent) {
            super(dexPath, optimizedDirectory, libraryPath, parent);
        }

        @Override
        public String findLibrary(String name) {
            String findLibrary = super.findLibrary(name);
            if (!TextUtils.isEmpty(findLibrary)) {
                return findLibrary;
            }
            File findSoLibrary = BundleArchiveRevision.this
                    .findSoLibrary(System.mapLibraryName(name));
            if (findSoLibrary != null && findSoLibrary.exists()) {
                return findSoLibrary.getAbsolutePath();
            }
            try {
                return (String) OpenAtlasHacks.ClassLoader_findLibrary.invoke(
                        Framework.getSystemClassLoader(), name);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class DexLoadException extends RuntimeException {
        /**
         *
         */
        private static final long serialVersionUID = 1L;

        DexLoadException(String str) {
            super(str);
        }
    }

    static {
        log = LoggerFactory.getInstance("BundleArchiveRevision");
    }

    BundleArchiveRevision(String location, long revisionNum, File revisionDir, InputStream inputStream)
            throws IOException {
        boolean withNativeLib=false;
        this.revisionNum = revisionNum;
        this.revisionDir = revisionDir;
        if (!this.revisionDir.exists()) {
            this.revisionDir.mkdirs();
        }
        this.revisionLocation = FILE_PROTOCOL;
        this.bundleFile = new File(revisionDir, BUNDLE_FILE_NAME);
        ApkUtils.copyInputStreamToFile(inputStream, this.bundleFile);
        BundleInfoList instance = BundleInfoList.getInstance();
        instance.dumpBundleInfos();
        withNativeLib=instance.getHasSO(location);
        if (withNativeLib) {
            installSoLib(this.bundleFile);
        }
        updateMetadata();
    }

    BundleArchiveRevision(String packageName, long revisionNum, File revisionDir, File archiveFile)
            throws IOException {
        boolean hasSO = false;
        this.revisionNum = revisionNum;
        this.revisionDir = revisionDir;
        BundleInfoList instance = BundleInfoList.getInstance();
        if (instance == null || !instance.getHasSO(packageName)) {

        } else {
            hasSO = true;
        }
        if (!this.revisionDir.exists()) {
            this.revisionDir.mkdirs();
        }
        if (archiveFile.canWrite()) {
            if (isSameDriver(revisionDir, archiveFile)) {
                this.revisionLocation = FILE_PROTOCOL;
                this.bundleFile = new File(revisionDir, BUNDLE_FILE_NAME);
                archiveFile.renameTo(this.bundleFile);
            } else {
                this.revisionLocation = FILE_PROTOCOL;
                this.bundleFile = new File(revisionDir, BUNDLE_FILE_NAME);
                ApkUtils.copyInputStreamToFile(new FileInputStream(archiveFile),
                        this.bundleFile);
            }
            if (hasSO) {
                installSoLib(this.bundleFile);
            }
        } else if (Build.HARDWARE.toLowerCase().contains("mt6592")
                && archiveFile.getName().endsWith(".so")) {
            this.revisionLocation = FILE_PROTOCOL;
            this.bundleFile = new File(revisionDir, BUNDLE_FILE_NAME);
            Runtime.getRuntime().exec(
                    String.format("ln -s %s %s",
                            new Object[]{archiveFile.getAbsolutePath(),
                                    this.bundleFile.getAbsolutePath()}));
            if (hasSO) {
                installSoLib(archiveFile);
            }
        } else if (OpenAtlasHacks.LexFile == null
                || OpenAtlasHacks.LexFile.getmClass() == null) {
            this.revisionLocation = REFERENCE_PROTOCOL
                    + archiveFile.getAbsolutePath();
            this.bundleFile = archiveFile;
            if (hasSO) {
                installSoLib(archiveFile);
            }
        } else {
            this.revisionLocation = FILE_PROTOCOL;
            this.bundleFile = new File(revisionDir, BUNDLE_FILE_NAME);
            ApkUtils.copyInputStreamToFile(new FileInputStream(archiveFile),
                    this.bundleFile);
            if (hasSO) {
                installSoLib(this.bundleFile);
            }
        }
        updateMetadata();
    }

    BundleArchiveRevision(String location, long revisionNum, File revisionDir) throws IOException {
        File metaFile = new File(revisionDir, "meta");
        if (metaFile.exists()) {
            DataInputStream dataInputStream = new DataInputStream(
                    new FileInputStream(metaFile));
            this.revisionLocation = dataInputStream.readUTF();
            dataInputStream.close();

            this.revisionNum = revisionNum;
            this.revisionDir = revisionDir;
            if (!this.revisionDir.exists()) {
                this.revisionDir.mkdirs();
            }

            if (StringUtils
                    .startWith(this.revisionLocation, REFERENCE_PROTOCOL)) {
                this.bundleFile = new File(StringUtils.substringAfter(
                        this.revisionLocation, REFERENCE_PROTOCOL));
                return;
            } else {
                this.bundleFile = new File(revisionDir, BUNDLE_FILE_NAME);
                return;
            }
        }
        throw new IOException("Could not find meta file in "
                + revisionDir.getAbsolutePath());
    }

    void updateMetadata() throws IOException {

        File metaFile = new File(this.revisionDir, "meta");
        DataOutputStream dataOutputStream = null;
        try {
            if (!metaFile.getParentFile().exists()) {
                metaFile.getParentFile().mkdirs();
            }
            dataOutputStream = new DataOutputStream(new FileOutputStream(metaFile));

            dataOutputStream.writeUTF(this.revisionLocation);
            dataOutputStream.flush();
            {
                try {
                    dataOutputStream.close();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }


        } catch (IOException e) {

            throw new IOException("Could not save meta data " + metaFile.getAbsolutePath(), e);
        } finally {

            if (dataOutputStream != null) {
                try {
                    dataOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public long getRevisionNum() {
        return this.revisionNum;
    }

    public File getRevisionDir() {
        return this.revisionDir;
    }

    public File getRevisionFile() {
        return this.bundleFile;
    }

    public File findSoLibrary(String str) {
        File file = new File(String.format("%s%s%s%s", new Object[]{
                this.revisionDir, File.separator, "lib", File.separator}), str);
        return (file.exists() && file.isFile()) ? file : null;
    }

    public boolean isDexOpted() {
        if (OpenAtlasHacks.LexFile == null
                || OpenAtlasHacks.LexFile.getmClass() == null) {
            return new File(this.revisionDir, BUNDLE_ODEX_FILE).exists();
        }
        return new File(this.revisionDir, BUNDLE_LEX_FILE).exists();
    }

    public synchronized void optDexFile() {
        if (!isDexOpted()) {
            if (OpenAtlasHacks.LexFile == null
                    || OpenAtlasHacks.LexFile.getmClass() == null) {
                File oDexFile = new File(this.revisionDir, BUNDLE_ODEX_FILE);
                long currentTimeMillis = System.currentTimeMillis();
                try {
                    if (!OpenAtlasFileLock.getInstance().LockExclusive(oDexFile)) {
                        log.error("Failed to get file lock for "
                                + this.bundleFile.getAbsolutePath());
                    }
                    if (oDexFile.length() <= 0) {
                        InitExecutor.optDexFile(
                                this.bundleFile.getAbsolutePath(),
                                oDexFile.getAbsolutePath());
                        loadDex(oDexFile);
                        OpenAtlasFileLock.getInstance().unLock(oDexFile);
                        // "bundle archieve dexopt bundle " +
                        // this.bundleFile.getAbsolutePath() + " cost time = " +
                        // (System.currentTimeMillis() - currentTimeMillis) +
                        // " ms";
                    }
                } catch (Throwable e) {
                    log.error(
                            "Failed optDexFile '"
                                    + this.bundleFile.getAbsolutePath()
                                    + "' >>> ", e);
                } finally {
                    OpenAtlasFileLock mAtlasFileLock = OpenAtlasFileLock.getInstance();
                    mAtlasFileLock.unLock(oDexFile);
                }
            } else {
                DexClassLoader dexClassLoader = new DexClassLoader(
                        this.bundleFile.getAbsolutePath(),
                        this.revisionDir.getAbsolutePath(), null,
                        ClassLoader.getSystemClassLoader());
            }
        }
    }

    private synchronized void loadDex(File file) throws IOException {
        if (this.dexFile == null) {
            this.dexFile = DexFile.loadDex(this.bundleFile.getAbsolutePath(),
                    file.getAbsolutePath(), 0);
        }
    }

    public void installSoLib(File archiveFile) {
        try {
            ZipFile zipFile = new ZipFile(archiveFile);
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                String name = zipEntry.getName();
                String abi ="armeabi";;
                if (Build.CPU_ABI.contains("x86")) {
                    abi = "x86";
                }
                if (name.indexOf(String.format("%s%s", new Object[]{"lib/",
                        abi})) != -1) {
                    abi = String
                            .format("%s%s%s%s%s",
                                    this.revisionDir,
                                    File.separator,
                                    "lib",
                                    File.separator,
                                    name.substring(
                                            name.lastIndexOf(File.separator) + 1,
                                            name.length()));
                    if (zipEntry.isDirectory()) {
                        File abiFolder = new File(abi);
                        if (!abiFolder.exists()) {
                            abiFolder.mkdirs();
                        }
                    } else {
                        File abiFolder = new File(abi.substring(0,
                                abi.lastIndexOf("/")));
                        if (!abiFolder.exists()) {
                            abiFolder.mkdirs();
                        }
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                                new FileOutputStream(abi));
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(
                                zipFile.getInputStream(zipEntry));
                        byte[] bArr = new byte[4096];
                        for (int read = bufferedInputStream.read(bArr); read != -1; read = bufferedInputStream
                                .read(bArr)) {
                            bufferedOutputStream.write(bArr, 0, read);
                        }
                        bufferedOutputStream.close();
                    }
                }
            }
            zipFile.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InputStream openAssetInputStream(String fileName) throws IOException {
        try {
            AssetManager assetManager = AssetManager.class
                    .newInstance();
            if (((Integer) OpenAtlasHacks.AssetManager_addAssetPath.invoke(
                    assetManager, this.bundleFile.getAbsolutePath()))
                    .intValue() != 0) {
                return assetManager.open(fileName);
            }
        } catch (Throwable e) {
            log.error("Exception while openNonAssetInputStream >>>", e);
        }
        return null;
    }

    public InputStream openNonAssetInputStream(String assetName) throws IOException {
        try {
            AssetManager assetManager = AssetManager.class
                    .newInstance();
            int intValue = ((Integer) OpenAtlasHacks.AssetManager_addAssetPath
                    .invoke(assetManager, this.bundleFile.getAbsolutePath()))
                    .intValue();
            if (intValue != 0) {
                return assetManager.openNonAssetFd(intValue, assetName)
                        .createInputStream();
            }
        } catch (Throwable e) {
            log.error("Exception while openNonAssetInputStream >>>", e);
        }
        return null;
    }

    Class<?> findClass(String name, ClassLoader classLoader)
            throws ClassNotFoundException {
        try {
            if (OpenAtlasHacks.LexFile == null
                    || OpenAtlasHacks.LexFile.getmClass() == null) {
                if (!isDexOpted()) {
                    optDexFile();
                }
                if (this.dexFile == null) {
                    loadDex(new File(this.revisionDir, BUNDLE_ODEX_FILE));
                }
                Class<?> loadClass = this.dexFile.loadClass(name, classLoader);
                this.isDexFileUsed = true;
                return loadClass;
            }
            if (this.dexClassLoader == null) {
                File file = new File(RuntimeVariables.androidApplication
                        .getFilesDir().getParentFile(), "lib");
                this.dexClassLoader = new BundleArchiveRevisionClassLoader(
                        this.bundleFile.getAbsolutePath(),
                        this.revisionDir.getAbsolutePath(),
                        file.getAbsolutePath(), classLoader);
            }
            return (Class) OpenAtlasHacks.DexClassLoader_findClass.invoke(
                    this.dexClassLoader, name);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        } catch (Throwable e) {
            if (!(e instanceof ClassNotFoundException)) {
                if (e instanceof DexLoadException) {
                    throw ((DexLoadException) e);
                }
                log.error("Exception while find class in archive revision: "
                        + this.bundleFile.getAbsolutePath(), e);
            }
            return null;
        }
    }

    List<URL> getResources(String name) throws IOException {
        List<URL> arrayList = new ArrayList();
        ensureZipFile();
        if (!(this.zipFile == null || this.zipFile.getEntry(name) == null)) {
            try {
                arrayList.add(new URL("jar:" + this.bundleFile.toURL() + "!/"
                        + name));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        return arrayList;
    }

    void close() throws Exception {
        if (this.zipFile != null) {
            this.zipFile.close();
        }
        if (this.dexFile != null) {
            this.dexFile.close();
        }
    }

    private boolean isSameDriver(File file, File file2) {
        return StringUtils
                .equals(StringUtils.substringBetween(file.getAbsolutePath(),
                        "/", "/"), StringUtils.substringBetween(
                        file2.getAbsolutePath(), "/", "/"));
    }

    private void ensureZipFile() throws IOException {
        if (this.zipFile == null) {
            this.zipFile = new ZipFile(this.bundleFile, 1);
        }
    }
}
