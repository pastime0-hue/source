package com.dsic.dsicfota.services;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.provider.Settings;
import android.util.Customer;
import android.widget.Toast;

import com.dsic.dsicfota.MainActivity;
import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.data.DeviceInformation;
import com.dsic.dsicfota.data.SettingManager;
import com.dsic.dsicfota.declaration.FotaConstants;
import com.dsic.dsicfota.ui.UpdateSettingActivity;
import com.dsic.dsicfota.Utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.dsic.dsicfota.MainActivity.OTA_FILE_PATH;
import static com.dsic.dsicfota.MainActivity.OTA_FILE_PATH;

public class AppBroadcastReceiver extends BroadcastReceiver {
    private final int START_OBSERVING_TIME = 10*1000;
    private final int CHECK_SETUPWIZARD_OBSERVING_INTERVAL = 3000;
    public AppBroadcastReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String ACTION_NAME = intent.getAction();
        LOG.DEBUG("onReceive Broadcast : "+ACTION_NAME);
        DeviceInformation deviceInformation = new DeviceInformation(context);
        if(Utils.fota_enable() == false){
            LOG.DEBUG("fota disabled");
            return;
        }else{
            LOG.DEBUG("fota enabled");
        }

        if (ACTION_NAME.compareTo(Intent.ACTION_BOOT_COMPLETED) == 0) {

            File dstFile = new File(MainActivity.DST_FULL_NAME);
            dstFile.setReadable(true,false);
            dstFile.setWritable(true,false);
            if(deviceInformation.check_gms() == DeviceInformation.GMS_TYPE.GMS){
                if(dstFile.exists()){
                    long ident = Binder.clearCallingIdentity();
                    try {
                        Files.delete(dstFile.toPath());
                    }catch(Exception e){
                        LOG.ERR("error = "+e.getMessage());
                        e.printStackTrace();
                    }
                    finally {
                        Binder.restoreCallingIdentity(ident);
                    }

                    File checkFile = new File(MainActivity.DST_FULL_NAME);
                    LOG.DEBUG("check file exist = "+checkFile.exists());
                }else{
                    LOG.DEBUG("[GMS] Update file is not exist and delete");
                }
            }else{
                LOG.DEBUG("[GMS] is not gms");
            }

            //ljh0915 초반 설정 화면 없앰.
            if ( (progress_first_working(context) == true) && (deviceInformation.check_gms() == DeviceInformation.GMS_TYPE.GMS) ) {
                return;
            }

            ObservingUpdateService.startObservingService(context, true, START_OBSERVING_TIME);
        } else if (ACTION_NAME.compareTo(FotaConstants.BARCODE_UPDATE) == 0) {
            MainActivity.startBarcodeUpdate(context, intent);
        } else if (ACTION_NAME.compareTo(FotaConstants.BR_CHECK_SETUP_WIZARD_FINISH) == 0) {//최초 OS 부팅시 SETUP WIZARD 가 종료됐는지 체크하는 메세지
            //ljh0915 초반 설정 화면 없앰.
            //check_setup_wizard_finish(context);
        } else if (ACTION_NAME.compareTo(FotaConstants.BR_OBSERVE_UPDATE) == 0) {//주기적으로 들어오는 업데이트 체크 메세지
            ObservingUpdateService.startObservingService(context, false, 0);
        } /*else if (ACTION_NAME.compareTo(Intent.ACTION_USER_INITIALIZE) == 0) {
            adjust_update_network_setting(context);
        }*/
        else if( (ACTION_NAME.compareTo("gms_setupwizard_finish") == 0) && (deviceInformation.check_gms() == DeviceInformation.GMS_TYPE.GMS) ){//GMS 버전에서 OS에서 오는 메세지. NON GMS 버전 할때는 따로 만들자..
            ObservingUpdateService.startObservingService(context, true, START_OBSERVING_TIME);
        }
    }

    //최초 부팅을 했는지 확인하고 확인시 최초 설정 엑티비티를 띄운다.
    private boolean progress_first_working(Context context) {
        SettingManager settingManager = new SettingManager(context);
        LOG.DEBUG("first boot checking->" + settingManager.is_first_boot());
        if (settingManager.is_first_boot() == true) {
            //한번 작동했으니 disable 시키고...
            settingManager.uncheck_first_boot();
            //check_setup_wizard_finish(context);//따로 받음.
            return true;
        }
        return false;
    }

    private void check_setup_wizard_finish(Context context) {
        boolean finish = false;
        try {
            finish = (Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.USER_SETUP_COMPLETE) == 1);
        } catch (Exception e) {
            if(LOG.IS_DEBUG()){ e.printStackTrace();}
        }

        LOG.DEBUG("SetupWizard completed->" + finish);
        if (finish == true) {
            UpdateSettingActivity.showActivity(context);
        } else {
            Intent intent = new Intent(FotaConstants.BR_CHECK_SETUP_WIZARD_FINISH);
            int pending_intent_id = 4231156;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, pending_intent_id, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + CHECK_SETUPWIZARD_OBSERVING_INTERVAL, pendingIntent);
        }
    }

    //PHONE에서 NOPHONE 변경시 NOPHONE 버전에서 불가능한 ONLY CELL, WIFI&CELL 옵션일시 WIFI로 변경해주는 기능
    private void adjust_update_network_setting(Context context) {
        boolean is_phone = new DeviceInformation(context).check_phone() == DeviceInformation.PHONE_TYPE.PHONE ? true : false;
        //만약 폰버전이 아니고
        if (!is_phone) {
            SettingManager settingManager = new SettingManager(context);
            //Phone 버전에만 가능한 옵션을 쓰고 있다면.
            if (settingManager.get_update_network() == FotaConstants.UPDATE_NETWORK.WIFI_AND_CELL ||
                    settingManager.get_update_network() == FotaConstants.UPDATE_NETWORK.ONLY_CELL) {
                //WIFI 업데이트로 변경한다.
                settingManager.set_update_network(FotaConstants.UPDATE_NETWORK.ONLY_WIFI);
            }
        }
    }
}
