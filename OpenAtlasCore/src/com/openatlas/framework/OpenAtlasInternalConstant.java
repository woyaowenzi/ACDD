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
 **/
package com.openatlas.framework;


/**
 * OpenAtlas  Platform Configuration
 * @author BunnyBlue
 *
 */
public class OpenAtlasInternalConstant {
    /****闪屏activity****/
    public static final String BOOT_ACTIVITY = "com.openatlas.welcome.Welcome";
    public static final String BOOT_ACTIVITY_DEFAULT = "com.openatlas.launcher.welcome";
    public static final String ACTION_BROADCAST_BUNDLES_INSTALLED = "com.openatlas.action.BUNDLES_INSTALLED";
    public static final String ATLAS_APP_DIRECTORY = "com.openatlas.AppDirectory";
    public static final String INSTALL_LOACTION = "com.openatlas.storage";
    public static final String COM_OPENATLAS_DEBUG_BUNDLES = "com.openatlas.debug.bundles";
    public static final String OPENATLAS_PUBLIC_KEY = "com.openatlas.publickey";
    public static final String OPENATLAS_BASEDIR = "com.openatlas.basedir";
    public static final String OPENATLAS_BUNDLE_LOCATION = "com.openatlas.jars";
    public static final String OPENATLAS_CLASSLOADER_BUFFER_SIZE = "com.openatlas.classloader.buffersize";
    public static final String OPENATLAS_LOG_LEVEL = "com.openatlas.log.level";
    public static final String OPENATLAS_DEBUG_BUNDLES = "com.openatlas.debug.bundles";
    public static final String OPENATLAS_DEBUG_PACKAGES = "com.openatlas.debug.packages";
    public static final String OPENATLAS_DEBUG_SERVICES = "com.openatlas.debug.services";
    public static final String OPENATLAS_DEBUG_CLASSLOADING = "com.openatlas.debug.classloading";
    public static final String OPENATLAS_DEBUG = "com.openatlas.debug";
    public static final String OPENATLAS_FRAMEWORK_PACKAGE = "com.openatlas.framework";

    public static final String OPENATLAS_STRICT_STARTUP = "com.openatlas.strictStartup";
    public static final String OPENATLAS_AUTO_LOAD = "com.openatlas.auto.load";
    public  static  final  String OPENATLAS_CONFIGURE=".openatlas_configs";
    public static Class<?> BundleNotFoundActivity = null;
    /********disable compile code****/
    public  static  final  boolean CODE_ENABLE_COMPILE=false;

}
