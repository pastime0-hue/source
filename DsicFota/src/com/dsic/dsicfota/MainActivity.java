package com.dsic.dsicfota;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.SystemProperties;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.DocumentsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.Utils.Md5Checksum;
import com.dsic.dsicfota.Utils.Utils;
import com.dsic.dsicfota.data.DeviceInformation;
import com.dsic.dsicfota.data.NetStateChecker;
import com.dsic.dsicfota.data.RefreshUpdateStatusTask;
import com.dsic.dsicfota.data.SettingManager;
import com.dsic.dsicfota.data.UpdateCampaign;
import com.dsic.dsicfota.data.UpdateChecker;
import com.dsic.dsicfota.declaration.FotaConstants;
import com.dsic.dsicfota.services.UpdateNotification;
import com.dsic.dsicfota.ui.UpdateSettingActivity;
import com.dsic.dsicfotalib.Observer;
import com.dsic.dsicfotalib.declations.RECEIVE_PROTOCOL;

import com.dsic.dsicfota.Utils.UpdateParser;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.dsic.dsicfota.declaration.FotaConstants.DOWNLOAD_FILE_FILENAME;
import static com.dsic.dsicfota.declaration.FotaConstants.DOWNLOAD_PATH;
import static com.dsic.dsicfota.declaration.FotaConstants.HTTP_SLASH;
import static com.dsic.dsicfota.declaration.FotaConstants.OTA_UPDATE_FINISH;
import static com.dsic.dsicfota.declaration.FotaConstants.OTA_VERSION_PROPERTY;
import static com.dsic.dsicfota.declaration.FotaConstants.UPDATE_PATH;
import dsic.server.control.SystemControl;
public class MainActivity extends AppCompatActivity {

    private boolean _append_engineer_mode = false;
    private PowerManager.WakeLock _wakelock;
    private SettingManager _setting_manager = null;
    private NetStateChecker _net_state_checker = null;
    private DownloadTask _downloadTask = null;
    private BroadcastReceiver _receiver = null;
    public static String ACTION_REFRESH_ACTIVITY = "com.dsic.dsicfota.refresh_activity";
    private static final int REQUEST = 1;
    private static final int READ_REQUEST_CODE = 42;
    private final String OTG_PATH = "/mnt/media_rw/";
    private final String REAL_PATH = "REAL_PATH";
    private final String MEDIA_NAME = "MEDIA_NAME";
    private static final String FAIL_TAG = "<FAIL>";
    private static final String SUCCESS_TAG = "<SUCCESS>";
    private static final String ROOT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String OTA_FILE_PATH = "/data/ota_package/";
    public static final String COPY_FILENAME = "update.zip";
    public static final String DST_FULL_NAME = OTA_FILE_PATH+COPY_FILENAME;
    private static final String EXTRA_START = "EXTRA_START";
    private static final String MANUAL_UPDATE = "MANUAL_UPDATE";
    private static String[] PERMISSIONS = {
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.WRITE_MEDIA_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.UPDATE_APP_OPS_STATS",
            "android.Manifest.permission.READ_PRIVILEGED_PHONE_STATE",
            "android.permission.MANAGE_SCOPED_ACCESS_DIRECTORY_PERMISSIONS",
            "android.permission.INSTALL_PACKAGES"
    };

    public static void startBarcodeUpdate(Context context, Intent intent) {
        Intent activity = new Intent();
        activity.putExtra(FotaConstants.ACTION_BARCODE_UPDATE, FotaConstants.EVENT_START);
        activity.putExtra(FotaConstants.EVENT_MODEL, intent.getStringExtra(FotaConstants.EVENT_MODEL));
        activity.putExtra(FotaConstants.EVENT_ANDROID_VERSION, intent.getStringExtra(FotaConstants.EVENT_ANDROID_VERSION));
        activity.putExtra(FotaConstants.EVENT_GMS, intent.getStringExtra(FotaConstants.EVENT_GMS));
        activity.putExtra(FotaConstants.EVENT_OS_TYPE, intent.getStringExtra(FotaConstants.EVENT_OS_TYPE));
        activity.putExtra(FotaConstants.EVENT_PHONE, intent.getStringExtra(FotaConstants.EVENT_PHONE));
        activity.putExtra(FotaConstants.EVENT_REQUEST_VERSION, intent.getStringExtra(FotaConstants.EVENT_REQUEST_VERSION));
        activity.putExtra(FotaConstants.EVENT_REQUEST_CAMPAIGN_VERSION, intent.getStringExtra(FotaConstants.EVENT_REQUEST_CAMPAIGN_VERSION));
        activity.putExtra(FotaConstants.EVENT_SELECTED_UPDATE_NETWORK, intent.getStringExtra(FotaConstants.EVENT_SELECTED_UPDATE_NETWORK));
        activity.putExtra(FotaConstants.EVENT_SELECTED_UPDATE_MODE,intent.getStringExtra(FotaConstants.EVENT_SELECTED_UPDATE_MODE));
        activity.setClassName(context.getPackageName(), MainActivity.class.getName());
        activity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(activity);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init_classes();
        init_items_view();

        set_title();
        verify_device_info();
        check_manual_update(getIntent());
        check_update_notification_action(getIntent());
        check_barcode_update_action(getIntent());

        _receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_REFRESH_ACTIVITY)) {
                    refresh_activity();
                }
            }
        };
        registerReceiver(_receiver, new IntentFilter(ACTION_REFRESH_ACTIVITY));
        PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        _wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "OTA Wakelock");

        verifyPermissions(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh_activity();
    }

    private void init_classes() {
        _net_state_checker = new NetStateChecker(getApplicationContext());
        _setting_manager = new SettingManager(getApplicationContext());
    }

    private void init_items_view() {
        //1. 기본 UI 설정
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(Color.BLACK);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        //add back press button on toolbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //3.  업데이트 유무에 따라 UI를 변경한다.
        refresh_activity();

        checkToolbarLongPress();
    }

    private boolean verify_device_info(){
        return verify_serial_number() && verify_manufacture_date() && verify_customer();
    }

    private boolean verify_serial_number(){
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        if(deviceInformation.serial_number() == null){
            //shortMessage("Serial is null");
            return false;
        }
        return true;
    }

    private boolean verify_manufacture_date(){
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        if(deviceInformation.manufacture_date().compareTo("") == 0){
            //shortMessage("can't get manufacture data : "+ deviceInformation.serial_number());
            return false;
        }
        return true;
    }

    private boolean verify_customer(){
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        if(deviceInformation.customer().compareTo("UNKNOWN") == 0){
            //shortMessage("can't get customer");
            return false;
        }
        return true;
    }

    private void check_manual_update(Intent activity_event){
        //1. activity event가 있는지 확인하다
        String extraValue = activity_event.getStringExtra(EXTRA_START);
        if (extraValue == null || extraValue.compareTo("") == 0) {
            return;
        }
        if(extraValue.compareTo(MANUAL_UPDATE) == 0) {
            manual_update();
        }
    }

    private void check_update_notification_action(Intent activity_event) {
        //1. activity event가 있는지 확인하다
        String extraValue = activity_event.getStringExtra(FotaConstants.ACTION_UPDATE_NOTIFICATION);
        if (extraValue == null || extraValue.compareTo("") == 0) {
            return;
        }
        //5. 노티바 클릭으로 들어왔으면 바로 체크로 들어간다.
        refresh_update();
    }

    private void refresh_activity(){
        if (UpdateChecker.getInstance().get_update_campaign().is_checked_update() == true) {

            //디스크 용량이 충분한지 체크해서 그렇지 않으면 리턴한다.
            if (get_enough_free_space_to_update(UpdateChecker.getInstance().get_update_campaign().get_file_total_size()) == false){
                String required_space = Utils.bytesToHuman( UpdateChecker.getInstance().get_update_campaign().get_file_total_size() -
                        Utils.get_free_space_on_disk());
                shortMessage(getApplicationContext().getResources().getString(R.string.fota_not_enough_free_space)+"\n"+
                        getApplicationContext().getResources().getString(R.string.fota_required_space)+
                        required_space);
                return;
            }

            boolean update_is_correct = true;
            //업데이트가 존재하더라도 파일 갯수가 0이거나 파일 사이즈가 0이면 업데이트를 진행하지 않는다.
            if(UpdateChecker.getInstance().get_update_campaign().get_file_count() <= 0){
                shortMessage(getApplicationContext().getResources().getString(R.string.fota_message_file_count_is_zero));
                update_is_correct = false;
            }
            if(update_is_correct == true && UpdateChecker.getInstance().get_update_campaign().get_file_total_size() <= 0){
                shortMessage(getApplicationContext().getResources().getString(R.string.fota_message_file_size_is_zero));
                update_is_correct = false;
            }

            if(update_is_correct == true){
                show_checked_update();
            }else{
                show_unchecked_update();
            }
        } else {
            show_unchecked_update();
        }
        set_title();

        ((TextView)findViewById(R.id.last_update_check_time)).
                setText(getApplicationContext().getResources().getString(R.string.fota_message_last_update_check_time)+" : "+
                        _setting_manager.get_last_updated_check_date()+ "\n["+
                        _setting_manager.get_update_network().name()+"] ["+
                        _setting_manager.get_update_period().name()+"]");
    }

    private void set_title(){

        setTitle(getResources().getString(R.string.app_name) + "  [" + get_version()+"]");
    }


    private void check_barcode_update_action(Intent activity_event) {
        String extraValue = activity_event.getStringExtra(FotaConstants.ACTION_BARCODE_UPDATE);
        if (extraValue == null || extraValue.compareTo("") == 0) {
            return;
        }

        if( request_barcode_update(activity_event.getStringExtra(FotaConstants.EVENT_MODEL),
                activity_event.getStringExtra(FotaConstants.EVENT_ANDROID_VERSION),
                activity_event.getStringExtra(FotaConstants.EVENT_GMS),
                activity_event.getStringExtra(FotaConstants.EVENT_OS_TYPE),
                activity_event.getStringExtra(FotaConstants.EVENT_PHONE),
                activity_event.getStringExtra(FotaConstants.EVENT_REQUEST_VERSION),
                activity_event.getStringExtra(FotaConstants.EVENT_REQUEST_CAMPAIGN_VERSION),
                activity_event.getStringExtra(FotaConstants.EVENT_SELECTED_UPDATE_NETWORK),
                activity_event.getStringExtra(FotaConstants.EVENT_SELECTED_UPDATE_MODE)) == false){

        }
    }

    private boolean request_barcode_update(final String MODEL, final String ANDROID_VERSION,
                                        final String GMS,
                                        final String OS_TYPE, final String PHONE, final String request_version,
                                        final String request_campaign_id, final String selected_update_network,
                                        final String selected_update_mode) {
        String errors = "";
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        if (deviceInformation.device_name().compareTo(MODEL) != 0) {
            errors += "Model:" + deviceInformation.device_name() + " ";
        }
        if (deviceInformation.os_version().compareTo(ANDROID_VERSION) != 0) {
            errors += "Android Version:" + deviceInformation.os_version() + " ";
        }
        if (deviceInformation.check_gms().name().compareTo(GMS) != 0) {
            errors += "GMS:" + deviceInformation.check_gms() + " ";
        }
        if (deviceInformation.customer().compareTo(OS_TYPE) != 0) {
            errors += "OS TYPE:" + deviceInformation.customer() + " ";
        }
        if (deviceInformation.check_phone().name().compareTo(PHONE) != 0) {
            errors += "PHONE:" + deviceInformation.check_phone() + " ";
        }
        if (request_version == null || request_version.compareTo("") == 0) {
            errors += "Request version can't be null" + " ";
        }
        if (request_campaign_id == null || request_campaign_id.compareTo("") == 0) {
            errors += "Request campaign id can't be null" + " ";
        }
        if (selected_update_network == null || selected_update_network.compareTo("") == 0) {
            errors += "selected update network can't be null" + " ";
        }
        if( selected_update_mode == null || selected_update_mode.compareTo("") == 0){
            errors += "selected update mode can't be null" + " ";
        }



        //if error is exist
        if (errors.compareTo("") != 0) {
            shortMessage( getResources().getString(R.string.message_invalid_update_barcode) + errors);
            LOG.DEBUG("barcode update error:" + errors);
            Utils.sendFinishToAutoSetting(getApplicationContext(), FotaConstants.AUTOSETUP_FOTA_INVALID_DEVICE, FotaConstants.AUTOSETUP_FOTA_INVALID_DEVICE_PARAM, errors);
            return false;
        }

        FotaConstants.UPDATE_NETWORK update_network = FotaConstants.UPDATE_NETWORK.NONE;
        int selected_value = 0;
        switch (selected_update_network){
            case "Don't case":
                selected_value = 0;
                break;
            case "WIFI Only":
                selected_value = 1;
                break;
            case "Mobile network and WIFI":
                selected_value = 2;
                break;
            case "Mobile network only":
                selected_value = 3;
                break;
        }
        if (selected_value == 0) {
            update_network = _setting_manager.get_update_network();
        } else {
            update_network = FotaConstants.UPDATE_NETWORK.fromInteger(selected_value);
        }

        int selected_barcode_update_type = 0;
        switch (selected_update_mode){
            case "Simulation Mode":
                selected_barcode_update_type = 1;
                break;
            case "Release Mode":
                selected_barcode_update_type = 2;
                break;
        }

        final FotaConstants.UPDATE_NETWORK UPDATE_DUMMY = update_network;//RESULT CALLBACK안에 애를 위해...
        RefreshUpdateStatusTask.CALLBACK callback = new RefreshUpdateStatusTask.CALLBACK() {
            @Override
            public void RESULT_CALLBACK(boolean refresh_success) {
                if (refresh_success == true) {
                    shortMessage( getResources().getString(R.string.fota_update_notification_message));
                    refresh_activity();
                    start_fota_update(UPDATE_DUMMY);
                } else {
                    refresh_activity();
                    int error_code = UpdateChecker.getInstance().get_update_campaign().get_receive_protocol().get_error_code();
                    if(error_code != Observer.RESULT.SUCCESS.getValue() ){
                        if(error_code > 100){//100 이상은 서버 연결 거절 코드
                            shortMessage( getResources().getString(R.string.fota_message_update_check_failed)+"\n"+Observer.RESULT.HTTP_CONNECTION_FAIL+":"+error_code);
                        }else{
                            shortMessage( getResources().getString(R.string.fota_message_update_check_failed)+"\n"+Observer.RESULT.fromInteger(error_code));
                        }
                    }else{
                        shortMessage( getResources().getString(R.string.fota_message_update_is_not_exist));
                    }
                }

            }
        };
        RefreshUpdateStatusTask refreshUpdateStatusTask = new RefreshUpdateStatusTask(MainActivity.this, (selected_barcode_update_type == 1) ? FotaConstants.UPDATE_TYPE.BARCODE_SIMULATION : FotaConstants.UPDATE_TYPE.BARCODE_RELEASE,
                update_network, request_version, request_campaign_id, callback);
        refreshUpdateStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

        Utils.sendFinishToAutoSetting(getApplicationContext(), FotaConstants.AUTOSETUP_FINISH, "", "");
        return true;
    }

    private boolean get_enough_free_space_to_update(long update_file_size){

        LOG.DEBUG("Free disk size : "+Utils.bytesToHuman(Utils.get_free_space_on_disk()));
        LOG.DEBUG("Update file size : "+Utils.bytesToHuman(update_file_size));
        if(Utils.get_free_space_on_disk() < update_file_size){
            return false;
        }
        return true;
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(_receiver);
        SystemProperties.set(OTA_VERSION_PROPERTY, "0");
        if (_downloadTask != null) {
            _downloadTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_basic, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //if in engineer mode. append engineer menu.
        if (_append_engineer_mode == true) {
            menu.clear();
            getMenuInflater().inflate(R.menu.menu_basic, menu);
            getMenuInflater().inflate(R.menu.menu_engineer, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_update_setting:
                //UpdateSettingActivity.showActivity(getApplicationContext());
                Intent intent = new Intent(this,UpdateSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.manual_update:
                //Toast.makeText(getApplicationContext(),"check",Toast.LENGTH_SHORT).show();
                manual_update();
                break;
            case R.id.action_update_type:
                show_update_type();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                LOG.DEBUG("Uri: " + uri.toString());
                HashMap<String,String> filePath = getRealFilePath(uri);
                requestStartManualUpdate(
                        filePath.get(MEDIA_NAME) + "\n" + getResources().getString(R.string.fota_message_os_update_confirm_message),
                        filePath.get(REAL_PATH));
            }
        }
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fota_status_text: {
                if(update_network_available() == false){
                    return;
                }
                refresh_update();
            }
            break;
            case R.id.start_update_button:
                if(update_network_available() == false){
                    return;
                }

                //업데이트가 이미 존재한다면 시작한다.
                if (UpdateChecker.getInstance().get_update_campaign().is_checked_update() == true) {
                    start_fota_update(_setting_manager.get_update_network());
                }

                break;
        }
    }

    private void refresh_update(){

        boolean free_space_enough = get_enough_free_space_to_update(UpdateChecker.getInstance().get_update_campaign().get_file_total_size());
        if (UpdateChecker.getInstance().get_update_campaign().is_checked_update() == false || free_space_enough == false) {
            RefreshUpdateStatusTask.CALLBACK callback = new RefreshUpdateStatusTask.CALLBACK() {
                @Override
                public void RESULT_CALLBACK(boolean refresh_success) {
                    if (refresh_success == true) {
                        //업데이트가 존재한다면 화면 메세지를 바꿔준다.
                        shortMessage( getResources().getString(R.string.fota_update_notification_message));

                        refresh_activity();
                        //start_fota_update(_setting_manager.get_update_network());

                        if(UpdateNotification.is_notified(getApplicationContext()) != true ){
                            UpdateNotification.getInstance(getApplicationContext()).startNotification(
                                    getApplicationContext().getResources().getString(R.string.fota_update_notification_message) + ":" + UpdateChecker.getInstance().get_update_campaign().get_update_version());
                        }
                    } else {
                        refresh_activity();
                        UpdateNotification.getInstance(getApplicationContext()).stopNotification();
                        int error_code = UpdateChecker.getInstance().get_update_campaign().get_receive_protocol().get_error_code();
                        if(error_code != Observer.RESULT.SUCCESS.getValue() ){
                            if(error_code > 100){//100 이상은 서버 연결 거절 코드
                                shortMessage( getResources().getString(R.string.fota_message_update_check_failed)+"\n"+Observer.RESULT.HTTP_CONNECTION_FAIL+" : #"+error_code);
                            }else{
                                shortMessage( getResources().getString(R.string.fota_message_update_check_failed)+"\n"+Observer.RESULT.URL_EXCEPTION+" : #"+error_code);
                            }
                        }else{
                            shortMessage( getResources().getString(R.string.fota_message_update_is_not_exist));
                        }
                    }
                }
            };
            RefreshUpdateStatusTask refreshUpdateStatusTask = new RefreshUpdateStatusTask(MainActivity.this,
                    _setting_manager.get_update_type(), _setting_manager.get_update_network(),"","",callback);
            refreshUpdateStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
        }
    }

    public void start_fota_update(FotaConstants.UPDATE_NETWORK update_network) {
        if (Utils.checkLowBattery(getApplicationContext()) == true) {
            shortMessage( getResources().getString(R.string.fota_message_battery_is_not_enough) );
        } else {
            check_network_status(update_network);
        }

    }

    public boolean update_network_available(){
        FotaConstants.UPDATE_NETWORK update_network = _setting_manager.get_update_network();
        if (_net_state_checker.is_network_available() == false) {
            shortMessage( getResources().getString(R.string.fota_network_inavailable) );
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.NONE) {
            shortMessage( getResources().getString(R.string.fota_message_update_network_none) );
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.ONLY_WIFI && _net_state_checker.is_wifiNetwork_available() == false) {
            shortMessage( getResources().getString(R.string.fota_message_wifi_network_unavailable) );
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.ONLY_CELL && _net_state_checker.is_cellNetwork_available() == false) {
            shortMessage( getResources().getString(R.string.fota_message_mobile_network_unavailable) );
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.WIFI_AND_CELL && _net_state_checker.is_network_available() == false) {
            shortMessage( getResources().getString(R.string.fota_message_all_network_unavailable) );
            return false;
        }

        return true;
    }

    private void check_network_status(FotaConstants.UPDATE_NETWORK update_network) {
        //모든 네트워크가 사용 불가인 경우
        if (_net_state_checker.is_network_available() == false) {
            shortMessage( getResources().getString(R.string.fota_network_inavailable));
            return;
        }
        //이동 통신망만 사용 가능한 경우 이동 네트워크를 사용할지 묻는다.
        if (_net_state_checker.is_cellNetwork_available() == true && _net_state_checker.is_wifiNetwork_available() == false) {
            if (update_network == FotaConstants.UPDATE_NETWORK.WIFI_AND_CELL || update_network == FotaConstants.UPDATE_NETWORK.ONLY_CELL) {
                request_update_with_cell_network();
            } else {
                shortMessage( getResources().getString(R.string.fota_message_mobile_network_update_disabled));
            }
        } else {
            //와이파이는 그냥 업데이트..
            if (update_network == FotaConstants.UPDATE_NETWORK.WIFI_AND_CELL || update_network == FotaConstants.UPDATE_NETWORK.ONLY_WIFI) {
                progress_update();
            } else {
                shortMessage( getResources().getString(R.string.fota_message_wifi_network_update_disabled));
            }
        }
    }

    private void request_update_with_cell_network() {
        double file_size = Utils.bytes_to_megabytes(UpdateChecker.getInstance().get_update_campaign().get_file_total_size());
        String file_size_mega = String.format("%.2f", file_size) + "mb";
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(getResources().getString(R.string.fota_network_request_use_cell_network) + file_size_mega)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                progress_update();
                            }
                        })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        dialog.show();
    }

    private void progress_update() {
        if(check_os_file_is_being() == false){
            shortMessage( getResources().getString(R.string.fota_check_file_exited_fail));
            return;
        }
        _downloadTask = new DownloadTask(MainActivity.this, UpdateChecker.getInstance().get_update_campaign());
        _downloadTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private boolean check_os_file_is_being(){
        boolean success = false;
        int file_count = UpdateChecker.getInstance().get_update_campaign().get_file_count();
        RECEIVE_PROTOCOL.FILE_INFO[] file_infos = UpdateChecker.getInstance().get_update_campaign().get_file_infos();
        for (int i = 0 ; i < file_count; i++){
                    if(file_infos[i].get_file_tag().compareTo("OS") == 0){
                                success = true;
                                break;
                            }
                }
        return success;
    }

    private void show_checked_update(){
        ((TextView)findViewById(R.id.start_update_button)).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.update_log)).setVisibility(View.VISIBLE);

        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());

        String update_version = getResources().getString(R.string.fota_message_update_is_exist) + " : " + UpdateChecker.getInstance().get_update_campaign().get_update_version() + "\n"+
                getResources().getString(R.string.fota_message_current_version) + ":" + deviceInformation.os_build_number();
        ((TextView) findViewById(R.id.fota_status_text)).setText(update_version);

        TextView update_log_view = (TextView)findViewById(R.id.update_log);
        String update_log = "";
        //update_log = "Update Version : " + UpdateChecker.getInstance().get_update_campaign().get_update_version()+"\n\n";
        update_log += "Patch Note\n";
        update_log += UpdateChecker.getInstance().get_update_campaign().get_update_note();
        update_log_view.setText(update_log);
    }

    private void show_unchecked_update(){
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        String update_version = getResources().getString(R.string.fota_message_current_version) + " : " + deviceInformation.os_build_number();
        ((TextView) findViewById(R.id.fota_status_text)).setText(update_version);
        ((TextView)findViewById(R.id.update_log)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.start_update_button)).setVisibility(View.INVISIBLE);
    }


    private void show_update_type() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        FotaConstants.UPDATE_TYPE update_type = _setting_manager.get_update_type();
        int selected = (update_type == FotaConstants.UPDATE_TYPE.SIMULATION) ? 1 : 0;
        alertDialog.setTitle(R.string.fota_menu_update_type);
        alertDialog.setSingleChoiceItems(R.array.fota_menu_update_array, selected, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                String[] period_array = getResources().getStringArray(R.array.fota_menu_update_array);
                shortMessage( period_array[which]);
                dialog.dismiss();

                FotaConstants.UPDATE_TYPE update_type = FotaConstants.UPDATE_TYPE.GENERAL;
                if(which == 1){
                    update_type = FotaConstants.UPDATE_TYPE.SIMULATION;
                }
                _setting_manager.set_update_type(update_type);
            }

        });
        alertDialog.show();
    }


    public static void verifyPermissions(Activity activity) {

        boolean all_granted = true;
        for (String permission : PERMISSIONS){
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                all_granted = false;
                break;
            }
        }

        if (all_granted == true){
            return;
        }


        ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS,
                REQUEST
        );

    }

    //툴바를 오랫동안 누르는지 체크합니다.
    private void checkToolbarLongPress() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                final View v = findViewById(R.id.toolbar);
                if (v != null) {
                    v.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            _append_engineer_mode = true;
                            return false;
                        }
                    });
                }
            }
        });
    }

    public String get_version(){
        return "1.6.12";
    }

    private static Toast _toast = null;
    public void shortMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(_toast != null) _toast.cancel();
                _toast = Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT);
                _toast.show();
            }
        });
    }

    public class DownloadTask extends AsyncTask<String, String, String> {
        private ProgressDialog _progress_dialog = null;
        private SettingManager _setting_manager = null;
        private UpdateCampaign _update_campaign = null;

        private Context _context = null;

        public DownloadTask(Context context, UpdateCampaign updateCampaign) {
            _context = context;
            _setting_manager = new SettingManager(_context);
            _update_campaign = updateCampaign;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            init_progress_dialog(_context.getResources().getString(R.string.fota_downloading_data));
            show_progress_menu();
            change_progress(0, _update_campaign.get_file_count(), _update_campaign.get_file_infos()[0].get_file_name(),
                    0, _update_campaign.get_file_total_size());
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            //메세지만 초기화
            String file_name = "";
            int progress_per = 0;
            long prgress_size = 0;
            //파라메터가 하나일 경우 타이틀 변경 후 메세지 초기화
            if (values.length == 1) {
                ((TextView)findViewById(R.id.update_notification)).setText(values[0]);
                return;
            }
            //파라메터가 4개일 경우 타이틀, 파일 이름, 프로그래스 퍼센테이지, 프로그래스 사이즈로 구분
            if (values.length == 4) {
                show_added_items(true);

                file_name = values[1];
                progress_per = Integer.valueOf(values[2]);
                prgress_size =  Long.valueOf(values[3]);

                double full_file_size = Utils.bytes_to_megabytes(_update_campaign.get_file_total_size());
                String full_size_mega = String.format("%.2f", full_file_size) + "mb";
                double download_size = Utils.bytes_to_megabytes(prgress_size);
                String download_size_mega = String.format("%.2f", download_size);
                _progress_dialog.setMessage(file_name + " " + download_size_mega + "/" + full_size_mega);
                _progress_dialog.setProgress(progress_per);
            }
        }

        @Override
        protected String doInBackground(String... urls) {
            //1. 파일들을 서버로부터 복사해온다. 복사 위치는 data/ota_pacakge
            SystemProperties.set(OTA_VERSION_PROPERTY, "1");//HTTP Connection - Start
            Observer.RESULT result = Observer.getInstance().download_update(_update_campaign.get_receive_protocol(), new Observer.CALLBACK_DOWNLOAD_PROGRESS() {
                @Override
                public void onDownload(int current_count, int file_total_count,//파일 총 갯수
                                       String current_download_file_name, long current_file_download_size, long current_file_size, //다운로드 중인 현재 파일의 이름, 진행 상황에 크기
                                       long total_file_download_size, long total_file_size) {
                    int download_per = (int) ((total_file_download_size / (float) total_file_size) * 100);
                    change_progress(current_count, file_total_count, current_download_file_name, download_per, total_file_download_size);
                    //딜레이 걸리면 이거 살리자
                    //try {Thread.sleep(1);}catch (Exception e){e.printStackTrace();}
                }
            }, DOWNLOAD_PATH);

            if(result != Observer.RESULT.SUCCESS){
                change_title(_context.getResources().getString(R.string.fota_download_fail)+"\n"+result.name(), 3000);
                return "File Download fail:"+result.name();
            }

            SystemProperties.set(OTA_VERSION_PROPERTY, "0");// HTTP Connection - Finish
            deinit_progress_dialog();

            show_progress_menu();
            //os file 이름을 ds_ota_update.zip으로 변경한다
            normalize_os_file(_update_campaign.get_file_infos());

            //os file이 저장 폴더에 있는지 확인한다.
            change_title(_context.getResources().getString(R.string.fota_check_file_exited), 500);
            if (is_os_file_exist() == true) {
                change_title(_context.getResources().getString(R.string.fota_check_file_exited_success), 500);
            } else {
                change_title(_context.getResources().getString(R.string.fota_check_file_exited_fail), 3000);
                return "OS_FILE_IS_NOT_EXIST";
            }


            //os 파일의 체크섬 확인

            change_title(_context.getResources().getString(R.string.fota_check_md5), 500);
            if (check_download_files_checksum(_update_campaign.get_file_infos()) == true) {
                change_title(_context.getResources().getString(R.string.fota_check_md5_correct), 500);
            } else {
                change_title(_context.getResources().getString(R.string.fota_check_md5_incorrect), 3000);
                return "CHECKSUM_INCORRECT";
            }
            // /data/data/com.dsic.dsicfota에서 /data/media/0로 복사
            /*
            change_title(_context.getResources().getString(R.string.fota_copy_file_to_update_folder), 500);
            if(copy_original_to_dest_folder() == true){
                change_title(_context.getResources().getString(R.string.fota_copy_file_to_update_folder_success), 500);
            }else{
                change_title(_context.getResources().getString(R.string.fota_copy_file_to_update_folder_fail), 3000);
                return "COPY_OS_FILE_FAILED";
            }*/

            change_title(_context.getResources().getString(R.string.fota_downloading_finished), 1000);
            try {
                //들어온 업데이트 캠페인과 타켓되는 버전을 임시로 저장해준다. 만약 성공하면 부팅시 실제로 저장하게 된다.
                LOG.DEBUG("set_temp_target_update_version : "+_update_campaign.get_update_version());
                _setting_manager.set_temp_target_update_version(_update_campaign.get_update_version());
                LOG.DEBUG("set temp campaign name : "+_update_campaign.get_campaign_name());
                _setting_manager.set_temp_campaign(_update_campaign.get_campaign_name());
                _setting_manager.set_boot_on_update(true);
                SystemProperties.set(OTA_UPDATE_FINISH, "1");//finish ota update
                //RecoverySystem.installPackage(_context, new File(UPDATE_PATH + HTTP_SLASH + DOWNLOAD_FILE_FILENAME));
            } catch (Exception e) {
                if(LOG.IS_DEBUG()){ e.printStackTrace();}
                SystemProperties.set(OTA_UPDATE_FINISH, "0");//finish ota update
                return "INSTALL_PACKAGE_ERROR";
            }

            return "OK";
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {

            }

            LOG.DEBUG("onPostExecute result = " + result);
            deinit_progress_dialog();
            hide_progress_menu();
            if(result.compareTo("OK") == 0){
                start_os_update(UPDATE_PATH + HTTP_SLASH + DOWNLOAD_FILE_FILENAME);
            }
        }

        private void init_progress_dialog(String title){
            if(_progress_dialog != null){
                return;
            }
            _progress_dialog = new ProgressDialog(_context);
            _progress_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //_progress_dialog.setTitle(_context.getResources().getString(R.string.fota_downloading_data));
            _progress_dialog.setTitle(title);
            _progress_dialog.setMessage(_update_campaign.get_file_infos()[0].get_file_name());
            _progress_dialog.setCanceledOnTouchOutside(false);
            _progress_dialog.setCancelable(false);
            _progress_dialog.setMax(100);
            _progress_dialog.show();
        }

        private void deinit_progress_dialog(){
            if (_progress_dialog != null && _progress_dialog.isShowing()) {
                _progress_dialog.dismiss();
            }
        }

        private void show_progress_menu(){
            ((TextView)findViewById(R.id.update_notification)).setVisibility(View.VISIBLE);
        }

        private void hide_progress_menu(){
            ((TextView)findViewById(R.id.update_notification)).setVisibility(View.GONE);
        }


        private void change_title(String value, int wait) {
            publishProgress(value);
            try {
                Thread.sleep(wait);
            } catch (Exception e) {
                if(LOG.IS_DEBUG()){ e.printStackTrace();}
            }
        }

        private void change_progress(int current_count, int total_count, String file_name, int progress_per, long progress_file_size) {
            String title = _context.getResources().getString(R.string.fota_downloading_data) + "(" + current_count + "/" + total_count + ")";
            publishProgress(title, file_name, String.valueOf(progress_per), String.valueOf(progress_file_size));
        }

        private void show_added_items(boolean show){
            int visible = show ? View.VISIBLE : View.GONE;
            ProgressBar progressBar = (ProgressBar)_progress_dialog.findViewById(com.android.internal.R.id.progress);
            TextView progressNumber = (TextView)_progress_dialog.findViewById(com.android.internal.R.id.progress_number);
            TextView progressPercent = (TextView)_progress_dialog.findViewById(com.android.internal.R.id.progress_percent);
            progressBar.setVisibility(visible);
            progressNumber.setVisibility(visible);
            progressPercent.setVisibility(visible);
        }

        //os file의 이름을 ds_ota_update.zip으로 변경시킨다.
        private boolean normalize_os_file(RECEIVE_PROTOCOL.FILE_INFO[] file_infos) {
            boolean result = true;
            for (int i = 0; i < file_infos.length; i++) {
                String check_sum_file = file_infos[i].get_file_name();
                if (file_infos[i].get_file_tag().compareTo("OS") == 0) {//OS일 경우 이름을 바꾼다.
                    File srcFile = new File(DOWNLOAD_PATH + HTTP_SLASH + check_sum_file);
                    srcFile.setReadable(true,false);
                    srcFile.setWritable(true,false);
                    File dstFile = new File(DOWNLOAD_PATH + HTTP_SLASH + DOWNLOAD_FILE_FILENAME);
                    dstFile.setReadable(true,false);
                    dstFile.setWritable(true,false);

                    result = Utils.rename(srcFile , dstFile);
                }
            }
            return result;
        }

        //os 파일이 실제로 있는지 확인한다
        private boolean is_os_file_exist() {
            File os_file = new File(DOWNLOAD_PATH + HTTP_SLASH + DOWNLOAD_FILE_FILENAME);
            os_file.setReadable(true,false);
            os_file.setWritable(true,false);
            //android.util.Log.d("jh","exists "+os_file.exists());
            //android.util.Log.d("jh","isFile "+os_file.isFile());
            //android.util.Log.d("jh","canRead "+os_file.canRead());
            return os_file.exists() && os_file.isFile() && os_file.canRead();
        }

        private boolean check_download_files_checksum(RECEIVE_PROTOCOL.FILE_INFO[] file_infos) {
            boolean result = false;
            for (int i = 0; i < file_infos.length; i++) {
                String check_sum_file = file_infos[i].get_file_name();
                if (file_infos[i].get_file_tag().compareTo("OS") == 0) {//OS일 경우 이름을 바꾼다.
                    check_sum_file = DOWNLOAD_FILE_FILENAME;
                }
                result = check_file_md5sum(DOWNLOAD_PATH + HTTP_SLASH + check_sum_file, file_infos[i].get_file_checksum());
                if(result == false){
                    break;
                }
            }

            return result;
        }

        private boolean check_file_md5sum(String file_path, String md5) {
            File file = new File(file_path);
            file.setReadable(true,false);
            file.setWritable(true,false);

            if (file == null || file.exists() == false) {
                LOG.DEBUG("file isn't exist:" + file_path);
                return false;
            }
            return Md5Checksum.checkMD5File(md5, file);
        }
    }

    //update dialog
    private ProgressDialog _update_progress_dialog = null;
    private void start_os_update(String file_path){
        _update_progress_dialog = new ProgressDialog(MainActivity.this);
        _update_progress_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        _update_progress_dialog.setTitle("OTA UPDATE");
        _update_progress_dialog.setMax(100);
        _update_progress_dialog.setCancelable(false);
        new Thread(new Runnable() {
            public void run() { update_os(file_path); }
        }).start();
    }

    private void update_os(String file_path){
        LOG.DEBUG("update os : "+file_path);
        File file = new File(file_path);
        file.setReadable(true,false);
        file.setWritable(true,false);

        UpdateParser.ParsedUpdate result;
        try {
            result=UpdateParser.parse(file);
        } catch (Exception e) {
            LOG.ERR("parse failed : "+String.format("For file %s ", file)+e.getMessage());
            return ;
        }

        if (result == null || !result.isValid()) {
            LOG.DEBUG("Failed verification "+ result.toString());
            return;
        }
        LOG.DEBUG(result.toString());
        _wakelock.acquire();
        UpdateEngine mUpdateEngine = new UpdateEngine();
        LOG.DEBUG( "applyPayload start ");
        try {
            mUpdateEngine.bind(_update_engine_callback);
            mUpdateEngine.applyPayload(result.mUrl, result.mOffset, result.mSize, result.mProps);
        } catch (Exception e) {
            LOG.ERR( "payload failed : "+String.format("For file %s ", file)+e.getMessage());
            return ;
        }
        LOG.DEBUG( "applyPayload end ");
        _wakelock.release();
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            super.handleMessage(msg);
            if(!_update_progress_dialog.isShowing()){
                _update_progress_dialog.show();
            }
            final int MESSAGE = msg.arg1;
            final int PERCENT = msg.arg2;
            switch(MESSAGE) {
                case UpdateEngine.UpdateStatusConstants.DOWNLOADING:
                    LOG.DEBUG("update progress: " + PERCENT);
                    _update_progress_dialog.setTitle("UPDATE : DOWNLOADING");
                    _update_progress_dialog.setProgress(PERCENT);
                    LOG.DEBUG("setProgress===" + PERCENT);
                    break;
                case UpdateEngine.UpdateStatusConstants.IDLE:
                    _update_progress_dialog.setTitle("UPDATE : IDLE");
                    break;
                case UpdateEngine.UpdateStatusConstants.UPDATE_AVAILABLE:
                    _update_progress_dialog.setTitle("UPDATE : UPDATE AVAILABLE");
                    break;
                case UpdateEngine.UpdateStatusConstants.FINALIZING:
                    _update_progress_dialog.setTitle("UPDATE : FINALIZING");
                    _update_progress_dialog.setProgress(PERCENT);
                    LOG.DEBUG("setProgress===" + PERCENT);
                    break;
                case UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT:
                    _update_progress_dialog.setTitle("UPDATE : UPDATED NEED REBOOT");
                    break;
                case UpdateEngine.UpdateStatusConstants.ATTEMPTING_ROLLBACK:
                    _update_progress_dialog.setTitle("UPDATE : ATTEMPTING ROLLBACK");
                    break;
                case UpdateEngine.UpdateStatusConstants.CHECKING_FOR_UPDATE:
                    _update_progress_dialog.setTitle("UPDATE : CHECKING FOR UPDATE");
                    break;
                case UpdateEngine.UpdateStatusConstants.DISABLED:
                    _update_progress_dialog.setTitle("UPDATE : DISABLED");
                    break;
                case UpdateEngine.UpdateStatusConstants.REPORTING_ERROR_EVENT:
                    _update_progress_dialog.setTitle("UPDATE : REPORTING ERROR EVENT");
                    break;
                case UpdateEngine.UpdateStatusConstants.VERIFYING:
                    _update_progress_dialog.setTitle("UPDATE : VERIFYING");
                    break;
            }
        }
    };

    private final UpdateEngineCallback _update_engine_callback = new UpdateEngineCallback(){
        /**
         * 정상 루틴시 메세지 순서
         * IDLE
         * UPDATE_AVAILABLE
         * DOWNLOADING.....
         * FINALIZING(2번 발생)
         * UPDATED_NEED_REBOOT
         * */
        @Override
        public void onStatusUpdate(int status,float percent){
            LOG.DEBUG("status: " + status);
            LOG.DEBUG("percent: " + percent);
            Message msg = new Message();
            msg.arg1 = status;
            if (status == UpdateEngine.UpdateStatusConstants.DOWNLOADING ||
                    status == UpdateEngine.UpdateStatusConstants.FINALIZING) {
                //            DecimalFormat df = new DecimalFormat("#");
                //            String progress = df.format(percent * 100);
                LOG.DEBUG( "update progress: " + percent+";"+(int)(percent*100));
                msg.arg2 = (int)(percent*100);
            }else{
                LOG.DEBUG( "onStatusUpdateError="+String.valueOf(status) );
            }
            handler.sendMessage(msg);
        }
        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {// 回调状态
                LOG.DEBUG( "UPDATE SUCCESS!");
                if(_update_progress_dialog != null){
                    _update_progress_dialog.dismiss();
                }
                /**
                 * ljh0915
                 * 2025/07/14 - GMS 사용을 위해 finger print를 고정함. 이에 OS 업데이트시 인식 하지 못하는 문제 발생
                 * 이에 특정적으로 업그레이드 여부를 판단하는 로직을 추가함.
                 * finger print가 275인 경우에만 적용
                 * */
                String fingerprint = SystemProperties.get("ro.build.fingerprint");
                if (fingerprint != null){
                    android.util.Log.d("LJH0915", "fingerprint-1: "
                            + fingerprint);
                }else{
                    android.util.Log.d("LJH0915", "fingerprint-1: null");
                }

                if (fingerprint != null && fingerprint.compareTo("DSIC/DS60S/DS60S:13/TKQ1.230825.002/275:user/release-keys") == 0) {
                    dsic.server.control.SystemControl.getInstance().setSystemUpgradeNotification(true);
                    android.util.Log.d("LJH0915", "Mockup to true");
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                        if(LOG.IS_DEBUG()){ e.printStackTrace();}
                    }
                }
                PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
                pm.reboot("__FOTA_REBOOT__");
            }else{
                if(_update_progress_dialog != null){
                    _update_progress_dialog.dismiss();
                }
                LOG.ERR( "onPayloadApplicationCompleteError="+String.valueOf(errorCode));
                switch(errorCode){
                    case  UpdateEngine.ErrorCodeConstants.DOWNLOAD_PAYLOAD_VERIFICATION_ERROR :
                        shortMessage("DOWNLOAD_PAYLOAD_VERIFICATION_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.DOWNLOAD_TRANSFER_ERROR :
                        shortMessage("DOWNLOAD_TRANSFER_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.ERROR :
                        shortMessage("ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.FILESYSTEM_COPIER_ERROR :
                        shortMessage("FILESYSTEM_COPIER_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.INSTALL_DEVICE_OPEN_ERROR :
                        shortMessage("INSTALL_DEVICE_OPEN_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.KERNEL_DEVICE_OPEN_ERROR :
                        shortMessage("KERNEL_DEVICE_OPEN_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.PAYLOAD_HASH_MISMATCH_ERROR :
                        shortMessage("PAYLOAD_HASH_MISMATCH_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.PAYLOAD_MISMATCHED_TYPE_ERROR :
                        shortMessage("PAYLOAD_MISMATCHED_TYPE_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.PAYLOAD_SIZE_MISMATCH_ERROR :
                        shortMessage("PAYLOAD_SIZE_MISMATCH_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.PAYLOAD_TIMESTAMP_ERROR :
                        shortMessage("PAYLOAD_TIMESTAMP_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.POST_INSTALL_RUNNER_ERROR :
                        shortMessage("POST_INSTALL_RUNNER_ERROR");
                        break;
                    case  UpdateEngine.ErrorCodeConstants.UPDATED_BUT_NOT_ACTIVE :
                        shortMessage("UPDATED_BUT_NOT_ACTIVE");
                        break;
                }
            }
        }
    };

    //================================================
    //manual update
    private void manual_update(){
        if(Utils.checkLowBattery(getApplicationContext())){
            shortMessage( getResources().getString(R.string.fota_message_battery_is_not_enough) );
            return;
        }

        File defaultPath = new File(OTA_FILE_PATH);
        defaultPath.setReadable(true,false);
        defaultPath.setWritable(true,false);

        if(!defaultPath.exists()){
            if(!defaultPath.mkdirs()) {
                LOG.ERR("Fail src directory");
                shortMessage( getResources().getString(R.string.fota_message_dir_create_failed) );
                return;
            }
        }

        int count = 0;
        String zipFile = "";
        String srcFiles[] = defaultPath.list();
        if(srcFiles != null){
            for(String filename : srcFiles){
                if(filename.endsWith(".zip")){
                    zipFile = filename;
                    count++;
                }
            }
        }
        if(count != 0){
            removeAndUpdateFileUI(count,defaultPath,zipFile);
        }else{
            showUpdateFileUI(count,defaultPath,zipFile);
        }

    }

    private void showUpdateFileUI(int count,File updateFolder,String updateFile){
        if(count == 1) {
            String srcPath = updateFolder.getPath() + "/" + updateFile;
            requestStartManualUpdate(srcPath + "\n" + getResources().getString(R.string.fota_message_os_update_confirm_message),srcPath);
        }else if(count < 1){
            showDocumentUI(getResources().getString(R.string.fota_message_not_found_file));
        }else if(count > 1){
            showDocumentUI(getResources().getString(R.string.fota_message_more_than_one_file));
        }
    }

    private void removeAndUpdateFileUI(int count,File updateFolder,String updateFile){
        String file_path = updateFolder.getPath() + "/" + updateFile;
        String msg = getResources().getString(R.string.fota_message_manual_update_remote_ota_file);

        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!TextUtils.isEmpty(file_path)){
                    File deleteFile = new File(file_path);
                    boolean result = deleteFile.delete();
                    if(!result){
                        shortMessage(getResources().getString(R.string.file_delete_failed));
                    }else{
                        showUpdateFileUI(0, updateFolder, updateFile);
                    }
                }
            }
        });
        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showUpdateFileUI(count, updateFolder, updateFile);
            }
        });
        alert.setMessage(msg);
        alert.show();
    }

    private void showDocumentUI(String message){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/zip");
        intent.putExtra("where","edm");
        intent.putExtra("message", message);
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    //key = REAL_PATH, MEDIA_NAME
    private HashMap<String,String> getRealFilePath(Uri uri){
        HashMap<String,String> result = new HashMap<>();
        String realPath = "";
        String mediaName = "";
        String docId = DocumentsContract.getDocumentId(uri);
        String[] split = docId.split(":");
        String type = split[0];
        LOG.DEBUG( "getRealFilePath path1: " + split[0]+"/path2:"+split[1]);
        if(type.equals("primary")){
            realPath = Environment.getExternalStorageDirectory() + "/" + split[1];
            mediaName = getResources().getString(R.string.fota_message_manual_update_storage_title_internal) + "/" + split[1];

        }else{
            boolean isOTG = getStorageInfo(split[0]);// sjahn OTG support
            if(isOTG) {
                realPath = OTG_PATH+ split[0] +"/"+ split[1];
                mediaName = getResources().getString(R.string.fota_message_manual_update_storage_title_otg) + "/" + split[1];
            }else {
                realPath = "/storage/"+ split[0] +"/"+ split[1];
                mediaName = getResources().getString(R.string.fota_message_manual_update_storage_title_sdcard) + "/" + split[1];
            }
        }
        result.put(REAL_PATH,realPath);
        result.put(MEDIA_NAME,mediaName);
        LOG.DEBUG("getRealFilePath=" + realPath);
        return result;
    }

    public boolean getStorageInfo(String mPath) {// sjahn OTG support
        boolean isOTG = false;
        StorageManager storageManager= (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] storageVolumeList = storageManager.getVolumeList();
        final List<VolumeInfo> volumes = storageManager.getVolumes();
        if (volumes == null) {
            return false;
        }

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PUBLIC && vol.isMountedReadable()) {
                final File path = vol.getPath();
                if (vol.getDisk().isUsb() && path.toString().contains(mPath)) {
                    isOTG = true;
                    LOG.DEBUG(mPath + " is OTG");
                    break;
                }
            }
        }

        return isOTG;
    }

    private void requestStartManualUpdate(String msg,String file_path){
        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!TextUtils.isEmpty(file_path)){
                    startManualUpdate(file_path);
                }
            }
        });
        alert.setMessage(msg);
        alert.show();
    }

    private void startManualUpdate(String file_path) {
        LOG.DEBUG("startManualUpdate = "+file_path);
        ManualUpdateTask manualUpdateTask = new ManualUpdateTask(getApplicationContext(),file_path);
        manualUpdateTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    private class ManualUpdateTask extends AsyncTask<String, Integer, String> {
        private Context _context = null;
        private String _file_path = "";
        public ManualUpdateTask(Context context, String file_path){
            _context = context;
            _file_path = file_path;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            _update_progress_dialog.setProgress(values[0]);
        }

        @Override
        protected String doInBackground(String... urls) {

            try {
                File srcFile = new File(_file_path);
                srcFile.setReadable(true,false);
                srcFile.setWritable(true,false);

                File dstPath = new File(OTA_FILE_PATH);
                dstPath.setReadable(true,false);
                dstPath.setWritable(true,false);

                if (!srcFile.exists()) {
                    LOG.ERR("Not src Path");
                    return FAIL_TAG + getResources().getString(R.string.not_found_file);
                }
                if (!dstPath.exists()) {
                    if (!dstPath.mkdirs()) {
                        LOG.ERR("Fail dst directory");
                        return FAIL_TAG + getResources().getString(R.string.dir_create_failed);
                    }
                }

                //Exist in Internal
                File dstFile = new File(OTA_FILE_PATH+COPY_FILENAME); //업데이트 파일명(ds_ota_update.zip)
                dstFile.setReadable(true,false);
                dstFile.setWritable(true,false);

                if (dstFile.exists()) {
                    LOG.DEBUG("Already exists file. -> " + dstFile.getAbsolutePath());
                    return SUCCESS_TAG + "already File exists in Internal";
                } else {
                    LOG.DEBUG("Copy File!");
                    FileInputStream inputStream = null; //복사할 파일
                    FileOutputStream outputStream = null; //복사할 폴더

                    inputStream = new FileInputStream(srcFile);
                    String dst;
                    dst = dstPath.getPath() + File.separator + COPY_FILENAME;
                    File dstNew = new File(dst);
                    dstNew.setReadable(true,false);
                    dstNew.setWritable(true,false);

                    if (dstNew.exists()) {
                        LOG.ERR("Already exists file. -> " + dstNew.getAbsolutePath());
                        if (!dstNew.delete()) // 파일이 이미 존재할 때
                            return FAIL_TAG + getResources().getString(R.string.file_delete_failed);
                    }

                    outputStream = new FileOutputStream(dstNew);
                    try {
                        LOG.DEBUG("Start File Copy.");
                        int max_length = inputStream.available();
                        LOG.DEBUG("max inputStream byte -> " + max_length);
                        _update_progress_dialog.setMax(max_length);

                        int readcount = 0;
                        byte[] buffer = new byte[256 * 1024];
                        int progress = 0;
                        while ((readcount = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, readcount);
                            progress += readcount;
                            publishProgress((int) progress);
                        }
                        outputStream.close();
                        inputStream.close();

                        if (!dstNew.exists()) {
                            return FAIL_TAG + getResources().getString(R.string.copy_failed);
                        }
                        LOG.DEBUG("Finish File Copy.");
                    } catch (IOException e) {
                        LOG.DEBUG("Fail the File Copy -> " + e.getMessage());
                        e.printStackTrace();
                        return FAIL_TAG + getResources().getString(R.string.copy_failed);
                    }
                }
            } catch(Exception e){
                LOG.ERR("Exception. -> " + e.getMessage());
                return FAIL_TAG + getResources().getString(R.string.copy_failed);
            }

            return getResources().getString(R.string.copy_success);

        }

        @Override
        protected void onPostExecute(String result) {
            LOG.DEBUG("onPostExecute result = " + result);
            if (_update_progress_dialog != null && _update_progress_dialog.isShowing()) {
                _update_progress_dialog.dismiss();
            }

            if(result.contains(FAIL_TAG)){
                shortMessage(result.replace(FAIL_TAG, ""));
            }else {
                LOG.DEBUG("checkValidFile DST_FULL_NAME="+DST_FULL_NAME+"/srcPath="+_file_path);
                try {
                    /**
                     * install시작시만 hideMainDialog()로 팝업 닫지 않고, OTA_VERSION_PROPERTY 1로 유지
                     * OTA_VERSION_PROPERTY 키잠금해제시점을 부팅후 DsicControll에서 해제.
                     * */
                    SystemProperties.set(OTA_UPDATE_FINISH, "1");
                    LOG.DEBUG("START installPackage");

                    try {
                        start_os_update(DST_FULL_NAME);
                    } catch (Exception e) {
                        LOG.ERR("Update Fail! =>"+e);
                    }

                } catch (Exception e) {
                    LOG.ERR("checkValidFile MD5 Error!!!!");
                    shortMessage(getString(R.string.copy_failed));
                }
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            _update_progress_dialog = new ProgressDialog(MainActivity.this);
            _update_progress_dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            _update_progress_dialog.setMessage(getResources().getString(R.string.copying_data));
            _update_progress_dialog.setCanceledOnTouchOutside(false);
            _update_progress_dialog.setCancelable(false);
            _update_progress_dialog.show();
            publishProgress(0);
        }
    }
}
