package com.dsic.dsicfota.services;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.UserHandle;
import android.view.WindowManager;

import com.dsic.dsicfota.MainActivity;
import com.dsic.dsicfota.R;
import com.dsic.dsicfota.data.DeviceInformation;
import com.dsic.dsicfota.declaration.FotaConstants;
import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.data.SettingManager;
import com.dsic.dsicfota.Utils.Utils;
import com.dsic.dsicfota.data.NetStateChecker;
import com.dsic.dsicfota.data.UpdateChecker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class ObservingUpdateService extends Service {

    private static Intent _observing_service = null;
    public static void startObservingService(Context context, boolean first, int delay) {
        if(_observing_service != null){
            stopObservingService(context);
        }
        ComponentName componentName = new ComponentName(context.getPackageName(), ObservingUpdateService.class.getName());
        _observing_service = new Intent(context,ObservingUpdateService.class);
        _observing_service.setComponent(componentName);
        _observing_service.putExtra("first",first);
        _observing_service.putExtra("delay",delay);
        ComponentName service = context.startService(_observing_service);
        if (service == null) {
            LOG.DEBUG("start observing service error : " + componentName.getPackageName());
        }
    }

    public static void stopObservingService(Context context){
        if (_observing_service != null) {
            boolean bRes = context.stopService(_observing_service);
            _observing_service = null;
            LOG.DEBUG("stop observing service" + String.valueOf(bRes));
        }
    }

    private static boolean isObservingServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager)context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ObservingUpdateService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //실질적 시작점
    //Binding을 사용하지 않고 onStartCommand
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LOG.DEBUG("onStartCommand");
        boolean first = false;
        try {
            first = intent.getBooleanExtra("first",false);
        }catch (Exception e){
            LOG.DEBUG("first is not being");
            return START_STICKY_COMPATIBILITY;
        }
        LOG.DEBUG("is first : "+first);
        if(first == true){
            //boot on update에서 false 처리해버려 이런 문제 발생...
            SettingManager settingManager = new SettingManager(getApplicationContext());
            check_sdcard_update(settingManager.boot_on_update());
            check_fota_update_success(settingManager.boot_on_update());
            settingManager.set_boot_on_update(false);
        }



        //노티바에 아이콘이 떠 있으면 추가적으로 작동시키지 않는다.. 이렇게 하는게 맞나?
        if(UpdateNotification.is_notified(getApplicationContext()) == false){
            try {
                final int DELAY = intent.getIntExtra("delay",0);
                LOG.DEBUG("start observing wait delay->"+DELAY);
                //delayed execute
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        start_observing_update(getApplicationContext());
                    }
                },DELAY);

            }catch (Exception e){
                if(LOG.IS_DEBUG()){ e.printStackTrace();}
            }
        }else{
            LOG.DEBUG("already notified");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LOG.DEBUG("observing service is destroyed");
        super.onDestroy();
    }


    //SDCARD를 통해 업데이트가 일어났는지 체크 및 캠패인 설정
    private void check_sdcard_update(boolean boot_on_update){
        LOG.DEBUG("check_sdcard_update");
        SettingManager settingManager = new SettingManager(getApplicationContext());
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        //만약 fota를 통해 업데이트가 안 일어났고..
        if(boot_on_update == false) {
            final int saved_version = Integer.valueOf(settingManager.get_temp_target_update_version());
            final int current_version = Integer.valueOf(deviceInformation.os_build_number());
            LOG.DEBUG("[fota]sd card : "+saved_version+" current version : "+current_version);
            //저장되어 있는 버전과 현재 버전이 다르다면 sdcard를 통해 업데이트가 일어났다고 추정한다.
            if(saved_version != current_version){
                //SDCARD를 통해 업데이트가 일어났다고 치고.
                settingManager.set_last_updated_campaign(SettingManager.SDCARD_UPDATE);
                //그리고 임시 저장 캠패인은 초기화
                settingManager.set_temp_campaign(SettingManager.SDCARD_UPDATE);
            }
        }
    }

    //FOTA를 통해 업데이트가 일어났는지 체크 및 캠패인 설정
    private void check_fota_update_success(boolean boot_on_update){
        LOG.DEBUG("check_fota_update_success");
        SettingManager settingManager = new SettingManager(getApplicationContext());
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        //fota로 업데이트가 발생했으면...
        if(boot_on_update == true){
            final int saved_version = Integer.valueOf(settingManager.get_temp_target_update_version());
            final int current_version = Integer.valueOf(deviceInformation.os_build_number());
            LOG.DEBUG("[fota]saved version : "+saved_version+" current version : "+current_version);
            //만약 저장된 버전과 현재 버전이 같으면 임시로 저장되어 있는 이름으로 바꾼다.
            if(saved_version == current_version){
                LOG.DEBUG("update campaign name : "+settingManager.get_temp_campaign());
                settingManager.set_last_updated_campaign(settingManager.get_temp_campaign());
            }
            //그리고 임시 저장 캠패인은 초기화
            settingManager.set_temp_campaign(SettingManager.SDCARD_UPDATE);
            //BETA UPDATE를 진행했을 수도 있기 때문에 일반으로 바꿔준다...
            settingManager.set_update_type(FotaConstants.UPDATE_TYPE.GENERAL);
        }
    }

    public void start_observing_update(Context context) {
        LOG.DEBUG("start_observing_update");
        if(Observing_task.on_working() == false){
            LOG.DEBUG("start update observing");
            new Observing_task(context).execute("");
        }

    }

    private static class Observing_task extends AsyncTask<String, String, String> {
        private SettingManager _setting_manager = null;
        private Context _context;
        private NetStateChecker _netstatechecker;
        private static boolean _on_working = false;
        private enum UPDATE_STATUS{
            UNKNOWN,
            NETWORK_UNAVAILABLE,
            UPDATE_IS_NOT_EXIST,
            UPDATE_EXIST,
        }

        public static boolean on_working(){
            return _on_working;
        }

        public Observing_task(Context context) {
            _context = context;
            _netstatechecker = new NetStateChecker(_context);
            _setting_manager = new SettingManager(context);
        }

        @Override
        protected void onPreExecute() {
            _on_working = true;
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            UPDATE_STATUS update_status = UPDATE_STATUS.UNKNOWN;
            //첫번째 연결을 체크한다. 부팅 후 10초 후 한번 확인한다
            update_status = check_update();
            if(update_status == UPDATE_STATUS.UPDATE_EXIST){
                publishProgress("");
            }else{
                send_update_broadcast(_context,_setting_manager.get_update_period().get_second()*1000);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            LOG.DEBUG("alert_os_update");
            alert_os_update(_context, UpdateChecker.getInstance().get_update_campaign().get_update_version());
            super.onProgressUpdate(values);
        }


        private UPDATE_STATUS check_update() {
            UPDATE_STATUS checked = UPDATE_STATUS.NETWORK_UNAVAILABLE;
            for (int i = 0; i < 10 && !isCancelled() && checked == UPDATE_STATUS.NETWORK_UNAVAILABLE; i++) {
                //네트워크 환경을 체크해서 작
                if (network_available() == false) {
                    checked = UPDATE_STATUS.NETWORK_UNAVAILABLE;
                    LOG.DEBUG("network check:"+checked);
                    try {
                        Thread.sleep(10*1000);
                    }catch (InterruptedException e){
                        LOG.DEBUG(e.getMessage());
                    }
                }else{
                    checked = UPDATE_STATUS.UNKNOWN;
                }
            }

            if(checked == UPDATE_STATUS.NETWORK_UNAVAILABLE){
                return checked;
            }


            if (update_exist() == true) {
                LOG.DEBUG("update check success:update exist");
                checked = UPDATE_STATUS.UPDATE_EXIST;
            }else{
                LOG.DEBUG("update check success:update is not exist");
                checked = UPDATE_STATUS.UPDATE_IS_NOT_EXIST;
            }

            return checked;
        }

        @Override
        protected void onPostExecute(String s) {
            LOG.DEBUG("onPostExecute");
            _on_working = false;
            super.onPostExecute(s);
        }

        private void send_update_broadcast(Context context, int send_delay){
            Intent intent = new Intent(FotaConstants.BR_OBSERVE_UPDATE);
            int pending_intent_id = 4231156;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, pending_intent_id, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            print_delayed_time(send_delay/1000);

            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + send_delay, pendingIntent);
        }

        private boolean update_exist() {
            return UpdateChecker.getInstance().update_campaign_information(_context, _setting_manager.get_update_type(),"","").is_checked_update();
        }

        private boolean network_available() {
            boolean exist = false;
            FotaConstants.UPDATE_NETWORK update_network = _setting_manager.get_update_network();
            switch (update_network) {
                case NONE:
                    break;
                case ONLY_WIFI:
                    exist = _netstatechecker.is_wifiNetwork_available();
                    break;
                case WIFI_AND_CELL:
                    exist = _netstatechecker.is_network_available();
                    break;
                case ONLY_CELL:
                    exist = _netstatechecker.is_cellNetwork_available();
                    break;
            }
            LOG.DEBUG("using network type:"+update_network+":result:"+exist);
            return exist;
        }

        private void alert_os_update(final Context context, final String update_version) {
            if(UpdateNotification.is_notified(context) == true ){
                return;
            }

            UpdateNotification.getInstance(context).startNotification(
                    context.getResources().getString(R.string.fota_update_notification_message) + ":" + UpdateChecker.getInstance().get_update_campaign().get_update_version());

           if(main_activity_is_top(_context) == true){
               //main activity가 맨위라면 리프세시
               Intent intent = new Intent(MainActivity.ACTION_REFRESH_ACTIVITY);
               _context.sendBroadcastAsUser(intent, UserHandle.ALL);
               return;
           }
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(context.getResources().getString(R.string.fota_update_notification_message))
                    .setMessage(context.getResources().getString(R.string.fota_move_to_update_screen) + " " + update_version)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    start_main_activity(_context);
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                    .create();
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            dialog.show();

        }

        private void start_main_activity(Context context){
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(FotaConstants.ACTION_UPDATE_NOTIFICATION,FotaConstants.EVENT_START);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }

        private boolean main_activity_is_top(Context context){
            ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            return taskInfo.get(0).topActivity.getClassName().compareTo("com.dsic.dsicfota.MainActivity") == 0 ? true : false;
        }

        private void print_delayed_time(int delay){
            long time = System.currentTimeMillis();
            Date date = new Date(time);
            // 포맷변경 ( 년월일 시분초)
            SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // Java 시간 더하기
            Calendar cal = Calendar.getInstance();
            cal.setTime(date); // 10분 더하기 cal.add(Calendar.MINUTE, 10);
            LOG.DEBUG("Current Time : "+sdformat.format(cal.getTime()));
            //초 더하기
            LOG.DEBUG("Delayed Time : "+Utils.normalize_second(delay));
            cal.add(Calendar.SECOND, delay);
            LOG.DEBUG("Start Check Time : "+sdformat.format(cal.getTime()));
            LOG.DEBUG("check network : "+_setting_manager.get_update_network().name());

        }
    }
}
