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

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Bundle;

import com.openatlas.framework.OpenAtlasInternalConstant;

import com.openatlas.launcher.welcome.WelcomeFragment;
import com.openatlas.runtime.Globals;


public class LauncherActivity extends Activity {
	WelcomeFragment mFragment;
    public static boolean isAtlasDexopted() {
        PackageInfo packageInfo = null;
        try {
            packageInfo = Globals.getApplication().getPackageManager().getPackageInfo(Globals.getApplication().getPackageName(), 0);
        } catch (Throwable e) {
           e.printStackTrace();
        }
        SharedPreferences sharedPreferences = Globals.getApplication().getSharedPreferences(OpenAtlasInternalConstant.OPENATLAS_CONFIGURE, 0);
        if (packageInfo == null || !"dexopt".equals(sharedPreferences.getString(packageInfo.versionName, ""))) {
            return false;
        }
        return false;
    }
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		setContentView(R.layout.activity_loader_tesst);
        setContentView(R.layout.welcome_frame);
        this.mFragment = new WelcomeFragment();
        getFragmentManager().beginTransaction().add(R.id.frame, this.mFragment).commitAllowingStateLoss();
	}
	



	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		//libcom_taobao_scan.so

	}
	/**
	 * 
	 */
	public static void doLaunchoverUT() {
		
		
	}
}
