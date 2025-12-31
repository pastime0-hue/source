package com.dsic.dsicfota.data;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.content.Context;
import com.dsic.dsicfota.Utils.LOG;

public class NetStateChecker {
    private ConnectivityManager _connection_manager = null;
    private Context _context = null;


    public NetStateChecker(Context context) {
        _context = context;
        _connection_manager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }


    public boolean is_cellNetwork_available() {
        if (_connection_manager == null) {
            return false;
        }
        try {
            return _connection_manager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE ? true : false;
        } catch (Exception e) {
            if(LOG.IS_DEBUG()){ e.printStackTrace();}
            return false;
        }

    }

    public boolean is_wifiNetwork_available() {
        if (_connection_manager == null) {
            return false;
        }

        try {
            return _connection_manager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI
                    || _connection_manager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIMAX ? true : false;
        } catch (Exception e) {
            if(LOG.IS_DEBUG()){ e.printStackTrace();}
            return false;
        }
    }

    public boolean is_network_available() {
        return is_cellNetwork_available() || is_wifiNetwork_available();
    }
}