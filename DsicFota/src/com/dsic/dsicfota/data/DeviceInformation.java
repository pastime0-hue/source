package com.dsic.dsicfota.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Customer;

import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.Utils.SerialNumberParser;
import com.dsic.dsicfota.Utils.Utils;
import com.dsic.dsicfota.declaration.FotaConstants;
import android.telephony.TelephonyManager;
import java.util.List;
import com.dsic.dsicfota.R;

public class DeviceInformation {
    public enum PHONE_TYPE{
        PHONE(0),
        NOPHONE(1);

        private int _type = 0;
        PHONE_TYPE(int type){
            _type = type;
        }
    }

    public enum GMS_TYPE{
        GMS(0),
        NOGMS(1);
        private int _type = 0;
        GMS_TYPE(int type){
            _type = type;
        }
    }

    private Context _context;
    private SettingManager _settingManager = null;
    public DeviceInformation(Context context) {
        if (context == null) {
            throw new NullPointerException("Device information's parameter can't be null");
        }
        _context = context;
        _settingManager = new SettingManager(_context);
    }

    public String serial_number() {
        return Build.getSerial();
    }

    public String bucket_name() {
        return FotaConstants.BUCKET_NAME;
    }

    public String get_last_update_campaign(){
        return _settingManager.get_last_updated_campaign();
    }

    public String manufacture_date(){
        SerialNumberParser serialNumberParser = new SerialNumberParser();
        if( serialNumberParser.parse_serial_number(serial_number()) == false){
            return "";
        }
        return serialNumberParser.get_date();
    }

    //DS6등 모델 이름
    public String device_name() {
        String device_name = Build.MODEL;
        return device_name;
    }

    //7.1.2등 OS Revision
    public String os_version() {//그야말로 안드로이드 버전 7.1.2 같은..
        return Build.VERSION.RELEASE;
    }

    //com.google.android.gms package가 있다면 GMS로 판별
    public GMS_TYPE check_gms() {
        boolean SUPPORT_GMS = false;
        PackageManager packageManager = _context.getPackageManager();
        List<ApplicationInfo> installedApplications = packageManager.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
        if(installedApplications == null){
            throw new NullPointerException("install application list is null");
        }

        for (android.content.pm.ApplicationInfo applicationInfo : installedApplications) {
            if (applicationInfo.packageName.equals(FotaConstants.GMS_PACKAGE)) {
                SUPPORT_GMS = true;
            }
        }
        return SUPPORT_GMS ? GMS_TYPE.GMS : GMS_TYPE.NOGMS;
    }

    public String customer() {
        if(Utils.STAND_ALONE){
            return "GLOBAL";
        }

        final String STR_CUSTOMER[] ={ "GLOBAL","SKT 3G","SKT LTE", "GLOBAL LTE" , "KT", "POST", "DSSC"/*, "KOSECO", "CU"*/};
        String customer = "UNKNOWN";
        final String origin_name = Customer.get();
        //LOG.DEBUG("customer name : "+origin_name);
        switch (origin_name){
            case Customer.GLOBAL:
                customer = STR_CUSTOMER[0];
                break;
            case Customer.SKT:
                customer = STR_CUSTOMER[1];
                break;
            case Customer.SKTLTE:
                customer = STR_CUSTOMER[2];
                break;
            case Customer.GLOBALLTE:
                customer = STR_CUSTOMER[3];
                break;
            case Customer.KTLTE:
                customer = STR_CUSTOMER[4];
                break;
            case Customer.POSTLTE:
                customer = STR_CUSTOMER[5];
                break;
            case Customer.DSSC:
                customer = STR_CUSTOMER[6];
                break;
            default:
                //LOG.ERR("unspecific name");
                customer = "GLOBAL";//만약 없으면 오리진 네임으로나마..
                break;
        }

        return customer;
    }

    public PHONE_TYPE check_phone() {
        if(device_name().contains("DS90") == true){
            return SystemProperties.get("ro.build.characteristics","tablet").contains("phone") ? PHONE_TYPE.PHONE : PHONE_TYPE.NOPHONE;
        }
        //나중에 체크 필요
        String device_type =_context.getString(R.string.device_type);
        LOG.DEBUG("device type : "+device_type);
        //TelephonyManager tel = (TelephonyManager) _context.getSystemService(Context.TELEPHONY_SERVICE);
        if (device_type.compareTo("tablet") == 0) {
            return PHONE_TYPE.NOPHONE;
        } else {
            return PHONE_TYPE.PHONE;
        }
        //return SystemProperties.get("ro.build.characteristics","tablet").contains("phone") ? PHONE_TYPE.PHONE : PHONE_TYPE.NOPHONE;
    }

    public String os_build_number() {
        String version = SystemProperties.get("ro.build.internal.id","");
        return version;
    }//os 빌드 넘버
}
