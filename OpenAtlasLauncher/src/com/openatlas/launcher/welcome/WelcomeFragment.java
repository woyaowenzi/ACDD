/**OpenAtlasForAndroid Project

The MIT License (MIT) 
Copyright (c) 2015 Bunny Blue

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
package com.openatlas.launcher.welcome;

/**
 * @author BunnyBlue
 *
 */


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;

import com.eftimoff.androipathview.PathView;
import com.openatlas.framework.OpenAtlasInternalConstant;
import com.openatlas.launcher.LauncherActivity;
import com.openatlas.launcher.R;
import com.openatlas.runtime.Globals;


public class WelcomeFragment extends Fragment implements Callback {
    private static final int MSG_ACTIIVTY_FINISH = 12;
    private static final int MSG_ANIMATE_LOGO = 1235;
    private static final int MSG_CONSUME_FINISH = 11;
    private static final int MSG_CONSUME_TIMEOUT = 13;
    private static final int MSG_FINISH_WELCOME = 1236;
    private static final int MSG_SHOW_SLOGAN = 1234;
    private static final String TAG = "WelcomeFregment";
    private BundlesInstallBroadcastReceiver atlasBroadCast;
    private long bundlestart;
    private boolean firstResume;
    private boolean initFinish;
   // private Bitmap mBmStart;
    private Handler mHandler;
    private boolean mHasBitmap;
    private PathView[] pathViewArray;
    private View welcomSlogan;

    private class BundlesInstallBroadcastReceiver extends BroadcastReceiver {
       

        private BundlesInstallBroadcastReceiver() {
            
        }

        @Override
		public void onReceive(Context context, Intent intent) {
            try {
            	WelcomeFragment.this.consumeFinish();
            	WelcomeFragment.this.mHandler.sendEmptyMessage(WelcomeFragment.MSG_CONSUME_FINISH);
            } catch (Exception e) {
            }
        }
    }

    public WelcomeFragment() {
        this.mHandler = null;
        //this.mBmStart = null;
        this.mHasBitmap = false;
        this.initFinish = false;
        this.firstResume = true;
        this.bundlestart = 0;
    }

    @Override
	public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View imageView = null;
       // super.onCreate(bundle);
        this.mHandler = new Handler(this);
        ViewGroup viewGroup2 = (ViewGroup) layoutInflater.inflate(R.layout.welcome, viewGroup, false);
        final PathView pathView = (PathView) viewGroup2.findViewById(R.id.pathViewS);
        PathView pathViewJ = (PathView) viewGroup2.findViewById(R.id.pathViewJ);
        PathView pathViewT = (PathView) viewGroup2.findViewById(R.id.pathViewT);
        PathView pathViewB = (PathView) viewGroup2.findViewById(R.id.pathViewB);
//      final Path path = makeConvexArrow(50, 100);
//      pathView.setPath(path);
      pathView.setFillAfter(true);
      pathView.useNaturalColors();
      pathView.getPathAnimator().
      delay(100).
      duration(1500).
      interpolator(new AccelerateDecelerateInterpolator()).
      start();
      pathViewJ.setFillAfter(true);
      pathViewJ.useNaturalColors();
      pathViewJ.getPathAnimator().
      delay(100).
      duration(1500).
      interpolator(new AccelerateDecelerateInterpolator()).
      start();
      
      pathViewT.setFillAfter(true);
      pathViewT.useNaturalColors();
      pathViewT.getPathAnimator().
      delay(100).
      duration(1500).
      interpolator(new AccelerateDecelerateInterpolator()).
      start();
      
      pathViewB.setFillAfter(true);
      pathViewB.useNaturalColors();
      pathViewB.getPathAnimator().
      delay(100).
      duration(1500).
      interpolator(new AccelerateDecelerateInterpolator()).
      start();
      init();
        return viewGroup2;
 //  this.mBmStart = a.getInstance().getBootBitmap();
//        if ((!LauncherActivity.isAtlasDexopted()) ) {
//            ViewGroup viewGroup2 = (ViewGroup) layoutInflater.inflate(R.layout.welcome, viewGroup, false);
//            LinearLayout linearLayout = (LinearLayout) viewGroup2.findViewById(R.id.ll_pathframe);
//            this.pathViewArray = new PathView[linearLayout.getChildCount()];
//            for (int i = 0; i < this.pathViewArray.length; i++) {
//                this.pathViewArray[i] = (PathView) linearLayout.getChildAt(i);
//                if (i == 0) {
//                    this.pathViewArray[i].setSvgResource(R.raw.logo_shou);
//                    this.pathViewArray[i].setVisibility(View.VISIBLE);
//                } else if (i == 1) {
//                    this.pathViewArray[i].setSvgResource(R.raw.logo_ji);
//                } else if (i == 2) {
//                    this.pathViewArray[i].setSvgResource(R.raw.logo_tao);
//                } else if (i == 3) {
//                    this.pathViewArray[i].setSvgResource(R.raw.logo_bao);
//                }
//            }
//            this.welcomSlogan = viewGroup2.findViewById(R.id.welcome_slogan);
//            viewGroup2.findViewById(R.id.welcome_slogan).setVisibility(View.VISIBLE);
//            if (LauncherActivity.isAtlasDexopted()) {
//                viewGroup2.findViewById(R.id.welcome_slogan).setVisibility(View.VISIBLE);
////                ((PathView) linearLayout.getChildAt(0)).setPercentage(1.0f);
////                ((PathView) linearLayout.getChildAt(1)).setPercentage(1.0f);
////                ((PathView) linearLayout.getChildAt(2)).setPercentage(1.0f);
////                ((PathView) linearLayout.getChildAt(3)).setPercentage(1.0f);
//            } else {
//                startAnimationForWait();
//            }
//        } else {
//            imageView = new ImageView(layoutInflater.getContext());
////            if (this.mBmStart != null) {
////                imageView.setBackgroundDrawable(new BitmapDrawable(this.mBmStart));
////            }
//            imageView.setLayoutParams(new LayoutParams(-1, -1));
//            this.mHasBitmap = true;
//        }
        // return imageView;
    }

    public boolean isHasOwnBitmap() {
        return this.mHasBitmap;
    }

    private void startAnimationForWait() {
        for (int i = 0; i < this.pathViewArray.length; i++) {
            if (VERSION.SDK_INT >= MSG_CONSUME_FINISH) {
                Animator ofFloat = ObjectAnimator.ofFloat(this.pathViewArray[i], "phase", 1.0f, 0.0f);
                ofFloat.setDuration(4000);
                Animator ofFloat2 = ObjectAnimator.ofFloat(this.pathViewArray[i], "fillTransparency", 0.0f, 1.0f);
                ofFloat2.setDuration(4000);
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.play(ofFloat).before(ofFloat2);
                animatorSet.start();
            } else if (this.welcomSlogan != null) {
                this.welcomSlogan.setVisibility(0);
              //  this.pathViewArray[i].setPercentage(1.0f);
            }
        }
        if (VERSION.SDK_INT >= MSG_CONSUME_FINISH) {
            this.mHandler.sendEmptyMessageDelayed(MSG_SHOW_SLOGAN, 5500);
        }
    }

    private void startAnimationForWaitSecond() {
        for (int i = 0; i < this.pathViewArray.length; i++) {
            if (VERSION.SDK_INT >= MSG_CONSUME_FINISH) {
                AnimatorSet animatorSet = new AnimatorSet();
                ObjectAnimator ofFloat = ObjectAnimator.ofFloat(this.pathViewArray[i], "scaleX", 1.0f, 1.1f);
                ObjectAnimator ofFloat2 = ObjectAnimator.ofFloat(this.pathViewArray[i], "scaleY", 1.0f, 1.1f);
                animatorSet.playTogether(ofFloat, ofFloat2);
                animatorSet.setDuration(300);
                ofFloat.setRepeatMode(2);
                ofFloat.setRepeatCount(1);
                ofFloat2.setRepeatMode(2);
                ofFloat2.setRepeatCount(1);
                if (i == 1) {
                    animatorSet.setStartDelay(200);
                } else if (i == 2) {
                    animatorSet.setStartDelay(400);
                } else if (i == 3) {
                    animatorSet.setStartDelay(600);
                }
                animatorSet.start();
            }
        }
    }

    @Override
	public void onResume() {
        super.onResume();
        if (this.firstResume) {
            this.firstResume = false;
    
        }
    }

    @Override
	public boolean handleMessage(Message message) {
        switch (message.what) {
            case MSG_CONSUME_FINISH /*11*/:
                this.initFinish = true;
                gotoMainActivity(false);
                break;
            case MSG_CONSUME_TIMEOUT /*13*/:
                consumeFinish();
                this.initFinish = true;
                gotoMainActivity(false);
                break;
            case MSG_SHOW_SLOGAN /*1234*/:
                if (this.welcomSlogan != null) {
                    this.welcomSlogan.setVisibility(0);
                    this.welcomSlogan.startAnimation(AnimationUtils.loadAnimation(Globals.getApplication(), R.anim.welcome_slogan_anim));
                    this.mHandler.sendEmptyMessageDelayed(MSG_ANIMATE_LOGO, 1000);
                    break;
                }
                break;
            case MSG_ANIMATE_LOGO /*1235*/:
//                View findViewById = getView().findViewById(R.id.tv_tips);
//                findViewById.setVisibility(0);
//                findViewById.startAnimation(AnimationUtils.loadAnimation(Globals.getApplication(), R.anim.fade_in));
                break;
            case MSG_FINISH_WELCOME /*1236*/:
                getActivity().finish();
                break;
        }
        return true;
    }

    @Override
	public void onDestroy() {
        super.onDestroy();
       

    }






    private void init() {
      
    	
        if ("flase".equals(System.getProperty("BUNDLES_INSTALLED", "flase"))) {
            this.atlasBroadCast = new BundlesInstallBroadcastReceiver();
            getActivity().registerReceiver(this.atlasBroadCast, new IntentFilter(OpenAtlasInternalConstant.ACTION_BROADCAST_BUNDLES_INSTALLED));
            this.bundlestart = System.currentTimeMillis();
            this.mHandler.sendEmptyMessageDelayed(MSG_CONSUME_TIMEOUT, 4000);
        } else {
            this.mHandler.sendEmptyMessageDelayed(MSG_CONSUME_FINISH, 600);
        }
  
    }

    public void consumeFinish() {
       
        if (this.atlasBroadCast != null) {
            getActivity().unregisterReceiver(this.atlasBroadCast);
        }
      
        this.mHandler.removeMessages(MSG_CONSUME_TIMEOUT);
    }

    public void gotoMainActivity(boolean z) {
    	System.out.println("WelcomeFragment.gotoMainActivity()");
      //  boolean z2 = false;.//com.openatlas.homelauncher.MainActivity
        if (getActivity()!=null&&LauncherActivity.class==getActivity().getClass() ) {

        	Intent mIntent=new Intent();
    		mIntent.setClassName(getActivity(), "com.openatlas.homelauncher.MainActivity");
    		startActivity(mIntent);
            LauncherActivity.doLaunchoverUT();
            getActivity().finish();
        }else {
			Log.e(getClass().getSimpleName()	, "getActivity() is null");
		}
    }





    public void enterTaobao(View view) {
        if (this.initFinish) {
            gotoMainActivity(false);
        } else {
            startAnimationForWait();
        }
    }
}