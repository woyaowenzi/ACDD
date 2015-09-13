package com.openatlas.homelauncher.otto;

/**
 * Created by BunnyBlue on 8/31/15.
 */
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

/**
 * Maintains a singleton instance for obtaining the bus. Ideally this would be replaced with a more efficient means
 * such as through injection directly into interested classes.
 */
public final class OttoManger {
    private static final Bus BUS = new Bus(ThreadEnforcer.ANY);

    public static Bus getInstance() {
        return BUS;
    }

    private OttoManger() {
        // No instances.
        Log.d("OttoManger","OttoManger inited >>>pid = "+ android.os.Process.myPid());
    }
}