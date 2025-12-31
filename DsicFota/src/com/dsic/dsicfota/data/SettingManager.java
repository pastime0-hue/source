package com.dsic.dsicfota.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.declaration.FotaConstants;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SettingManager {
    private SharedPreferences _prefs = null;
    private final String TARGET_UPDATED_VERSION = "TARGET_UPDATE_VERSION";
    private final String UPDATED_CAMPAIGN = "UPDATED_CAMPAIGN";
    private final String LAST_UPDATE_CHECK_DATE_KEY = "LAST_UPDATE_CHECK_DATE";
    private final String UPDATED_CAMPAIGN_TEMP = "UPDATED_CAMPAIGN_TEMP";
    private final String BOOT_ON_UPDATE = "BOOT_ON_UPDATE";
    public static final String SDCARD_UPDATE = "";//기본값 없음
    public  final String LAST_UPDATE_CHECK_DATE_VALUE = "";
    private final String UPDATED_TYPE_NAME = "UPDATE_TYPE";
    private final String UPDATE_PERIOD = "UPDATE_PERIOD";
    private final String UPDATE_NETWORK_TYPE = "UPDATE_NETWORK_TYPE";
    private final String FIRST_BOOT = "FIRST_BOOT";

    public SettingManager(Context context) {
        if (context == null) {
            throw new NullPointerException("context can't be null");
        }
        _prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (_prefs == null) {
            throw new NullPointerException("get default preference failed");
        }
    }


    //업데이트 타입
    public boolean set_update_type(FotaConstants.UPDATE_TYPE update_type) {
        return _prefs.edit().putInt(UPDATED_TYPE_NAME, update_type.toInteger()).commit();
    }

    public FotaConstants.UPDATE_TYPE get_update_type() {
        return FotaConstants.UPDATE_TYPE.fromInteger(_prefs.getInt(UPDATED_TYPE_NAME, 1));
    }


    //업데이트 주기
    public boolean set_update_period(FotaConstants.UPDATE_PERIOD update_period) {
        return _prefs.edit().putInt(UPDATE_PERIOD, update_period.toInteger()).commit();
    }

    public FotaConstants.UPDATE_PERIOD get_update_period() {
        return FotaConstants.UPDATE_PERIOD.fromInteger(_prefs.getInt(UPDATE_PERIOD, 2));
    }

    //업데이트 네트워크
    public boolean set_update_network(FotaConstants.UPDATE_NETWORK update_network) {
        return _prefs.edit().putInt(UPDATE_NETWORK_TYPE, update_network.toInteger()).commit();
    }

    public FotaConstants.UPDATE_NETWORK get_update_network() {
        return FotaConstants.UPDATE_NETWORK.fromInteger(_prefs.getInt(UPDATE_NETWORK_TYPE, 1));//ONLY WIFI
    }

    //마지막 업데이트 캠패인 이름
    public boolean set_last_updated_campaign(String campaign) {
        return _prefs.edit().putString(UPDATED_CAMPAIGN, campaign).commit();
    }

    public String get_last_updated_campaign() {
        return _prefs.getString(UPDATED_CAMPAIGN, SDCARD_UPDATE);
    }

    //마지막 업데이트 시간
    //마지막 업데이트 캠패인 이름
    public boolean set_last_update_check_date(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm");
        return _prefs.edit().putString(LAST_UPDATE_CHECK_DATE_KEY, simpleDateFormat.format(date)).commit();
    }

    public String get_last_updated_check_date() {
        return _prefs.getString(LAST_UPDATE_CHECK_DATE_KEY, LAST_UPDATE_CHECK_DATE_VALUE);
    }



    //OS 업데이트 후 재부팅 후 세팅 비교를 위한 세팅들(+)
    public String get_temp_target_update_version() {
        return _prefs.getString(TARGET_UPDATED_VERSION, "0");
    }

    public boolean set_temp_target_update_version(String update_version) {
        return _prefs.edit().putString(TARGET_UPDATED_VERSION, update_version).commit();
    }

    public String get_temp_campaign() {
        return _prefs.getString(UPDATED_CAMPAIGN_TEMP, SDCARD_UPDATE);
    }

    public boolean set_temp_campaign(String campaign) {
        return _prefs.edit().putString(UPDATED_CAMPAIGN_TEMP, campaign).commit();
    }

    public boolean boot_on_update() {//1회용임.. 한번쓰면 초기화
        boolean result = _prefs.getBoolean(BOOT_ON_UPDATE, false);
        LOG.DEBUG("boot_on_update : "+result);
        return result;
    }

    public boolean set_boot_on_update(boolean updated) {
        LOG.DEBUG("set_boot_on_update : "+updated);
        boolean result =  _prefs.edit().putBoolean(BOOT_ON_UPDATE, updated).commit();
        LOG.DEBUG("check on boot update : "+_prefs.getBoolean(BOOT_ON_UPDATE, false));
        return result;
    }
    //OS 업데이트 후 재부팅 후 세팅 비교를 위한 세팅들(-)



    //최초 부트를 체크하여 세팅을 시작하는 용도(+)
    public boolean is_first_boot() {
        return _prefs.getBoolean(FIRST_BOOT, true);
    }

    public boolean uncheck_first_boot() {
        return _prefs.edit().putBoolean(FIRST_BOOT, false).commit();
    }
    //최초 부트를 체크하여 세팅을 시작하는 용도(-)
}