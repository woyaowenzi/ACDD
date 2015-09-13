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

import com.openatlas.bundleInfo.BundleInfoList;
import com.openatlas.framework.bundlestorage.Archive;
import com.openatlas.framework.bundlestorage.BundleArchiveRevision.DexLoadException;
import com.openatlas.hack.OpenAtlasHacks;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;

import org.osgi.framework.BundleException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;

/***
 * bundle class loader ,load class from bundle
 **/
public final class BundleClassLoader extends ClassLoader {
    private static final List<URL> EMPTY_LIST;
    static final HashSet<String> FRAMEWORK_PACKAGES;
    static final Logger log;
    final Archive archive;
    BundleImpl bundle;
    List<BundleClassLoader> dependencyClsLoaders;
    /***
     * remove next version
     *********/
    @SuppressWarnings("unused")
    @Deprecated
    private static final class BundleURLHandler extends URLStreamHandler {
        private final InputStream inputStream;

        private BundleURLHandler(final InputStream inputStream) {
            this.inputStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    return inputStream.read();
                }

                @Override
                public int read(byte[] buffer) throws IOException {
                    return inputStream.read(buffer);
                }
            };
        }

        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return new URLConnection(url) {
                @Override
                public void connect() throws IOException {

                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return inputStream;
                }
            };
        }

        @Override
        protected int hashCode(URL url) {
            return this.inputStream.hashCode();
        }
    }

    static {
        log = LoggerFactory.getInstance("BundleClassLoader");
        FRAMEWORK_PACKAGES = new HashSet<String>();
        FRAMEWORK_PACKAGES.add(OpenAtlasInternalConstant.OPENATLAS_FRAMEWORK_PACKAGE);
        FRAMEWORK_PACKAGES.add("org.osgi.framework");
        FRAMEWORK_PACKAGES.add("org.osgi.service.packageadmin");
        FRAMEWORK_PACKAGES.add("org.osgi.service.startlevel");
        EMPTY_LIST = new ArrayList<URL>();
    }

    BundleClassLoader(BundleImpl bundleImpl) throws BundleException {
        super(Object.class.getClassLoader());
//        this.exports = new String[0];
//        this.imports = new String[0];
//        this.requires = new String[0];
        // this.activatorClassName = null;
        // this.activator = null;
        this.bundle = bundleImpl;
        this.archive = bundleImpl.archive;
        if (this.archive == null) {
            throw new BundleException("Not Component valid bundle: " + bundleImpl.location);
        }
    }

    public BundleImpl getBundle() {
        return this.bundle;
    }

    @Deprecated
    private void checkExecutionEnviroment(String[] requireEnv, String[] execEnv)
            throws BundleException {
        if (requireEnv.length != 0) {
            Set hashSet = new HashSet(Arrays.asList(execEnv));
            int i = 0;
            while (i < requireEnv.length) {
                if (!hashSet.contains(requireEnv[i])) {
                    i++;
                } else {
                    return;
                }
            }
            throw new BundleException("Platform does not provide EEs " + Arrays.asList(requireEnv));
        }
    }

    boolean resolveBundle(boolean resolve, HashSet<BundleClassLoader> hashSet) throws BundleException {

        if (Framework.DEBUG_CLASSLOADING && log.isInfoEnabled()) {
            log.info("BundleClassLoader: Resolving " + this.bundle + (resolve ? " (critical)" : " (not critical)"));
        }
        List<String> pkgs=BundleInfoList.getInstance().
                getDependencyForBundle(bundle.getLocation());
        dependencyClsLoaders=new ArrayList<BundleClassLoader>(pkgs.size());
        for (int i=0;i<pkgs.size();i++){
            dependencyClsLoaders.add((BundleClassLoader) OpenAtlas.getInstance().getBundleClassLoader(pkgs.get(i)));
        }

//        HashSet hashSetExports=null;
//        if (this.exports.length > 0) {
//             hashSetExports = new HashSet(this.exports.length);
//            for (String parsePackageString : this.exports) {
//                hashSetExports.add(Package.parsePackageString(parsePackageString)[0]);
//            }
//
//        }
//        if (this.imports.length > 0) {
//            if (this.importDelegations == null) {
//                this.importDelegations = new HashMap(this.imports.length);
//            }
//            for (int i = 0; i < this.imports.length; i++) {
//                String obj = Package.parsePackageString(this.imports[i])[0];
//                if (!FRAMEWORK_PACKAGES.contains(obj)
//                        && this.importDelegations.get(obj) == null
//                        && (hashSetExports == null || !hashSetExports.contains(obj))) {
//                    BundleClassLoader bundleClassLoader = Framework.getImport(
//                            this.bundle, this.imports[i], resolve, hashSet);
//                    if (bundleClassLoader != null) {
//                        if (bundleClassLoader != this) {
//                            this.importDelegations.put(obj, bundleClassLoader);
//                        }
//                    } else if (resolve) {
//                        throw new BundleException("Unsatisfied import "
//                                + this.imports[i] + " for bundle "
//                                + this.bundle.toString(),
//                                new ClassNotFoundException(
//                                        "Unsatisfied import "
//                                                + this.imports[i]));
//                    } else {
//                        if (this.exports.length > 0) {
//                            Framework.export(this, this.exports, false);
//                        }
//                        if (!Framework.DEBUG_CLASSLOADING
//                                || !log.isInfoEnabled()) {
//                            return false;
//                        }
//                        log.info("BundleClassLoader: Missing import "
//                                + this.imports[i]
//                                + ". Resolving attempt terminated unsuccessfully.");
//                        return false;
//                    }
//                }
//            }
//        }
//        if (this.exports.length > 0) {
//            if (this.importDelegations == null) {
//                this.importDelegations = new HashMap(this.imports.length);
//            }
//            for (int i = 0; i < this.exports.length; i++) {
//                BundleClassLoader bundleClassLoader = Framework.getImport(
//                        this.bundle,
//                        Package.parsePackageString(this.exports[i])[0], false,
//                        null);
//                if (!(bundleClassLoader == null || bundleClassLoader == this)) {
//                    this.importDelegations.put(
//                            Package.parsePackageString(this.exports[i])[0],
//                            bundleClassLoader);
//                }
//            }
//        }
//        if (this.exports.length > 0) {
//            Framework.export(this, this.exports, true);
//        }
        return true;
    }

    void cleanup(boolean staleExportedPackage) {
        ArrayList arrayList = new ArrayList();
        if (this.bundle != null) {
//            if (staleExportedPackage) {
//                this.bundle.staleExportedPackages = (Package[]) arrayList
//                        .toArray(new Package[arrayList.size()]);
//            } else {
//                this.bundle.staleExportedPackages = null;
//            }
        }
//        if (this.importDelegations != null) {
//            String[] delegations = this.importDelegations.keySet()
//                    .toArray(new String[this.importDelegations.size()]);
//            for (String mImportDelegation : delegations) {
//                Package exportPackage = Framework.exportedPackages
//                        .get(new Package(mImportDelegation, null, false));
//                if (!(exportPackage == null || exportPackage.importingBundles == null)) {
//                    exportPackage.importingBundles.remove(this.bundle);
//                    if (exportPackage.importingBundles.isEmpty()) {
//                        exportPackage.importingBundles = null;
//                        if (exportPackage.removalPending) {
//                            Framework.exportedPackages.remove(exportPackage);
//                        }
//                    }
//                }
//            }
//        }
        //       this.importDelegations = null;
        // this.activator = null;

        if (staleExportedPackage) {
            if (arrayList.size() == 0) {
                this.bundle = null;
            }

        }
    }

    @Override
    protected Class<?> findClass(String clazz) throws ClassNotFoundException {
        if (FRAMEWORK_PACKAGES.contains(packageOf(clazz))) {
            return Framework.systemClassLoader.loadClass(clazz);
        }
        Class<?> findOwnClass = findOwnClass(clazz);
        if (findOwnClass != null) {
            return findOwnClass;
        }

        for (int i=0;i<dependencyClsLoaders.size();i++) {
            BundleClassLoader dependencyLoader = dependencyClsLoaders.get(i);
            if (dependencyLoader != null) {
                findOwnClass = findDelegatedClass(dependencyLoader, clazz);
                if (findOwnClass != null) {
                    return findOwnClass;

                }

            }
        }
        try {
            findOwnClass = Framework.systemClassLoader.loadClass(clazz);
            if (findOwnClass != null) {
                return findOwnClass;
            }
        } catch (Exception e) {
        }
        throw new ClassNotFoundException("Can't find class " + clazz
                + " in BundleClassLoader: " + this.bundle.getLocation());
    }

    private Class<?> findOwnClass(String clazz) {
        try {
            return this.archive.findClass(clazz, this);
        } catch (Exception e) {
            if (!(e instanceof DexLoadException)) {
                return null;
            }
            throw ((DexLoadException) e);
        }
    }

    private static Class<?> findDelegatedClass(
            BundleClassLoader bundleClassLoader, String clazz) {
        Class<?> findLoadedClass;
        synchronized (bundleClassLoader) {
            findLoadedClass = bundleClassLoader.findLoadedClass(clazz);
            if (findLoadedClass == null) {
                findLoadedClass = bundleClassLoader.findOwnClass(clazz);
            }
        }
        return findLoadedClass;
    }

    @Override
    protected URL findResource(String name) {
        String stripTrailing = stripTrailing(name);
        List findOwnResources = findOwnResources(stripTrailing, false);
        if (findOwnResources.size() > 0) {
            return (URL) findOwnResources.get(0);
        }
        return null;
//        List findImportedResources = findImportedResources(stripTrailing, false);
//        return findImportedResources.size() > 0 ? (URL) findImportedResources
//                .get(0) : null;
    }

    @Override
    protected Enumeration<URL> findResources(String name) {
        String stripTrailing = stripTrailing(name);
        Collection findOwnResources = findOwnResources(stripTrailing, true);
        // findOwnResources.addAll(findImportedResources(stripTrailing, true));
        return Collections.enumeration(findOwnResources);
    }

    private List<URL> findOwnResources(String name, boolean z) {
        try {
            return this.archive.getResources(name);
        } catch (IOException e) {
            e.printStackTrace();
            return EMPTY_LIST;
        }
    }


    @Override
    protected String findLibrary(String nickname) {
        String mapLibraryName = System.mapLibraryName(nickname);
        File findLibrary = this.archive.findLibrary(mapLibraryName);
        if (findLibrary != null) {
            return findLibrary.getAbsolutePath();
        }
        try {
            return (String) OpenAtlasHacks.ClassLoader_findLibrary.invoke(
                    Framework.systemClassLoader, nickname);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String toString() {
        return "BundleClassLoader[Bundle" + this.bundle + "]";
    }

    private static String[] readProperty(Attributes attributes, String name)
            throws BundleException {
        String value = attributes.getValue(name);
        if (value == null || !value.equals("")) {
            return splitString(value);
        }
        return new String[0];
    }

    private static String[] splitString(String string) {
        int i = 0;
        if (string == null) {
            return new String[0];
        }
        StringTokenizer stringTokenizer = new StringTokenizer(string, ",");
        if (stringTokenizer.countTokens() == 0) {
            return new String[]{string};
        }
        String[] strArr = new String[stringTokenizer.countTokens()];
        while (i < strArr.length) {
            strArr[i] = stringTokenizer.nextToken().trim();
            i++;
        }
        return strArr;
    }

    private static String stripTrailing(String name) {
        return (name.startsWith("/") || name.startsWith("\\")) ? name.substring(1)
                : name;
    }

    private static String packageOf(String name) {
        int lastIndexOf = name.lastIndexOf(46);
        return lastIndexOf > -1 ? name.substring(0, lastIndexOf) : "";
    }

}
