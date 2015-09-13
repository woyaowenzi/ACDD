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
package com.openatlas.log;

import android.util.Log;

public class AndroidLogger implements Logger {
    private final String category;

    public AndroidLogger(String str) {
        this.category = str;
    }

    public AndroidLogger(Class<?> cls) {
        this(cls.getSimpleName());
    }

    @Override
    public void verbose(String message) {
        Log.v(category, message);
    }

    @Override
    public void debug(String message) {
        Log.d(category, message);
    }

    @Override
    public void info(String message) {
        Log.i(category, message);
    }

    @Override
    public void warn(String message) {
        Log.w(category, message);
    }

    @Override
    public void warn(String message, Throwable th) {
        Log.w(message, th);
    }

    @Override
    public void warn(StringBuffer stringBuffer, Throwable th) {
        warn(stringBuffer.toString(), th);
    }

    @Override
    public void error(String message) {
        Log.e(this.category, message);
    }

    @Override
    public void error(String message, Throwable th) {
        Log.e(this.category, message, th);
    }

    @Override
    public void error(StringBuffer stringBuffer, Throwable th) {
        error(stringBuffer.toString(), th);
    }

    @Override
    public void fatal(String message) {
        error(message);
    }

    @Override
    public void fatal(String message, Throwable th) {
        error(message, th);
    }

    @Override
    public boolean isVerboseEnabled() {
        return LoggerFactory.logLevel <= 2;
    }

    @Override
    public boolean isDebugEnabled() {
        return LoggerFactory.logLevel <= 3;
    }

    @Override
    public boolean isInfoEnabled() {
        return LoggerFactory.logLevel <= 4;
    }

    @Override
    public boolean isWarnEnabled() {
        return LoggerFactory.logLevel <= 5;
    }

    @Override
    public boolean isErrorEnabled() {
        return LoggerFactory.logLevel <= 6;
    }

    @Override
    public boolean isFatalEnabled() {
        return LoggerFactory.logLevel <= 6;
    }
}
