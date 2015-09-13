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

import android.os.Environment;
import android.util.Log;

import com.openatlas.framework.OpenAtlas;
import com.openatlas.log.Logger;
import com.openatlas.log.LoggerFactory;

import java.io.File;
import java.util.ArrayList;


class BundleDebug {
	Logger  log=LoggerFactory.getInstance("Debug");
    boolean isDebugable;
    private boolean isDebug;
    private ArrayList<String> debugBundles;
    private final String debugFolder;

    public BundleDebug() {
        this.isDebug = false;
        this.isDebugable = false;
        this.debugBundles = new ArrayList();
        this.debugFolder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/bundle-debug";
    }

    public boolean isDebugable() {

        if (!this.isDebug) {
            return false;
        }
        File file = new File(this.debugFolder);
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File mFile:listFiles){
                if (mFile.isFile() && mFile.getName().endsWith(".so")) {
                    this.debugBundles.add(mFile.getAbsolutePath());
                    log.debug( "found external bundle " + mFile.getAbsolutePath());
                    this.isDebugable = true;
                }
            }

        }
        return this.isDebugable;
    }

    public boolean installExternalBundle(String location) {

        if (!this.isDebug || this.debugBundles.size() <= 0) {
            return false;
        }
        for (String bundle:debugBundles){

            log.debug( "processLibsBundle filePath " + bundle);
            if (bundle.contains(Utils.getFileNameFromEntryName(location).substring(3))) {
                File file = new File(bundle);
                String replace = Utils.getBaseFileName(file.getName()).replace("_", ".");
                if (OpenAtlas.getInstance().getBundle(replace) == null) {
                    try {
                        OpenAtlas.getInstance().installBundle(replace, file);
                    } catch (Throwable th) {
                        Log.e("BundleDebug", "Could not install external bundle.", th);
                    }
                    log.debug( "Succeed to install external bundle " + replace);
                }
                file.delete();
                return true;
            }
        }
        return false;
    }
}
