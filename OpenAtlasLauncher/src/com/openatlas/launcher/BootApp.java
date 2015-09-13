/**
 *  OpenAtlasForAndroid Project
The MIT License (MIT) Copyright (OpenAtlasForAndroid) 2015 Bunny Blue,achellies

Permission is hereby granted, free of charge, to any person obtaining a copy of this software
and associated documentation files (the "Software"), to deal in the Software 
without restriction, including without limitation the rights to use, copy, modify, 
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to 
permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies 
or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
@author BunnyBlue
 * **/
package com.openatlas.launcher;

import com.openatlas.android.compat.OpenAtlasApp;
import com.openatlas.framework.OpenAtlasInternalConstant;
import com.openatlas.framework.AtlasConfig;

public class BootApp extends OpenAtlasApp {
	static{
		AtlasConfig.DELAY = new String[]{"com.openatlas.qrcode"};
		AtlasConfig.AUTO = new String[]{"com.openatlas.homelauncher","com.openatlas.qrcode","com.openatlas.android.game2","com.openatlas.universalimageloader.sample"};
		AtlasConfig.STORE = new String[]{"com.openatlas.android.appcenter","com.openatlas.universalimageloader.sample"};
	}

	

	static final String TAG = "TestApp";


	@Override
	public void onCreate() {

		super.onCreate();
	
		OpenAtlasInternalConstant.BundleNotFoundActivity=BundleNotFoundActivity.class;
	}





}
