package com.dsic.dsicfota.Utils;


import android.os.Build;

public class LOG {
    private final static String TAG = "DSIC_FOTA";
    public static void DEBUG(String value){
        if(IS_DEBUG() == true){
            android.util.Log.d(TAG, value);
        }
    }

    public static void ERR(String value){
        if(IS_DEBUG() == true){
            android.util.Log.e(TAG, value);
        }
    }

    public static boolean IS_DEBUG(){
        return Build.TYPE.compareTo("userdebug") == 0 ? true : false;
    }
}
