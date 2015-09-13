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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

/***
 * define  interface of Bundle,every bundle is a zip file ,also  we can think its  archive file
 ***/
public interface Archive {
    /***
     * close bundle file
     **/
    void close();

    /**
     * Finds the class with the specified <a href="#name">binary name</a>.
     * This method should be overridden by class loader implementations that
     * follow the delegation model for loading classes, and will be invoked by
     * the loadClass method after checking the
     * parent class loader for the requested class.  The default implementation
     * throws a <tt>ClassNotFoundException</tt>.
     *
     * @param clazz The <a href="#clazz">binary name</a> of the class
     * @return The resulting <tt>Class</tt> object
     * @throws ClassNotFoundException If the class could not be found
     */
    Class<?> findClass(String clazz, ClassLoader classLoader)
            throws ClassNotFoundException;

    /**
     * Returns the absolute path name of a native library.  The VM invokes this
     * method to locate the native libraries that belong to classes loaded with
     * this class loader. If this method returns <tt>null</tt>, the VM
     * searches the library along the path specified as the
     * "<tt>java.library.path</tt>" property.
     *
     * @param name The library name
     * @return The absolute path of the native library
     */
    File findLibrary(String name);

    /***
     * get bundle file
     ***/
    File getArchiveFile();

    /***
     * get runing bundle impl
     **/
    BundleArchiveRevision getCurrentRevision();



    /****
     * get resource from bundle
     *
     * @param name resource name
     **/
    List<URL> getResources(String name) throws IOException;

    /***
     * vaild  bundle execute dexopt or not
     **/
    boolean isDexOpted();

    /**
     * install new bundle
     *
     * @param packageName bundle name
     * @param bundleDir   bundle storge  folder
     * @param bundleFile  bundle  archive file
     * @throws IOException
     ***/
    BundleArchiveRevision newRevision(String packageName, File bundleDir, File bundleFile)
            throws IOException;

    /**
     * install new bundle
     *
     * @param packageName       bundle name
     * @param bundleDir         bundle storge  folder
     * @param bundleInputStream bundle  archive  input  stream,if bundle  file  in  zip,not on local fs
     * @throws IOException
     ***/
    BundleArchiveRevision newRevision(String packageName, File bundleDir,
                                      InputStream bundleInputStream) throws IOException;

    /**
     * get asset inputstream from bundle
     * @param assetName  asset name
     ***/
    InputStream openAssetInputStream(String assetName) throws IOException;

    /**
     * get non-asset inputstream from bundle
     * @param name  resource name
     ***/
    InputStream openNonAssetInputStream(String name) throws IOException;

    /***
     * pre-process  dex file ,optdex or dex2oat
     **/
    void optDexFile();

    /***
     * remove bundle cache
     ***/
    void purge() throws Exception;
}
