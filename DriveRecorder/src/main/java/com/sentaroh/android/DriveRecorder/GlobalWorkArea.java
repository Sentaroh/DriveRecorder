package com.sentaroh.android.DriveRecorder;

import android.content.Context;

public class GlobalWorkArea {
    static private GlobalParameters gp=null;
    static public GlobalParameters getGlobalParameters(Context c) {
        if (gp ==null) {
            gp =new GlobalParameters();
            gp.initSettingParms(c);
            gp.loadSettingParms(c);
        }
        return gp;
    }
}
