package com.dsic.dsicfota.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.dsic.dsicfota.R;
import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.Utils.Utils;
import com.dsic.dsicfota.data.DeviceInformation;
import com.dsic.dsicfota.data.SettingManager;
import com.dsic.dsicfota.declaration.FotaConstants;
import com.dsic.dsicfota.services.ObservingUpdateService;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateSettingActivity extends AppCompatActivity {
    private SettingManager _settingManager      = null;
    private ListView _update_network_listview   = null;
    private ListView _update_period_listview    = null;
    private ArrayAdapter _update_network_arrayAdapter = null;
    private ArrayAdapter _update_period_arrayAdapter = null;
    //overrides(+)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Utils.get_device(this) == Utils.DISPLAY_SIZE.SIZE_480){
            setContentView(R.layout.activity_intialize_setting_ds4a);
        }else{
            setContentView(R.layout.activity_intialize_setting);
        }

        setTitle(R.string.title_activity_initial_setting);
        init_classes();

        init_items_view();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok:
                save_and_finish();
                break;
        }
    }
    //overrides(-)

    //static functions(+)
    public static boolean showActivity(Context context){
        Intent popupIntent = new Intent(context, UpdateSettingActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, popupIntent, PendingIntent.FLAG_ONE_SHOT| PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try{
            pi.send();
        }
        catch(Exception e){
            if(LOG.IS_DEBUG()){ e.printStackTrace();}
            return false;
        }
        return true;
    }
    //static functions(-)

    //inner functions(+)
    private void init_classes(){
        _settingManager             = new SettingManager(getApplicationContext());
        _update_network_listview    = (ListView) findViewById(R.id.update_network_list);
        _update_period_listview     = (ListView) findViewById(R.id.update_period_list);
    }

    private void init_items_view() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        //update network adapter init
        final int update_network_select = _settingManager.get_update_network().toInteger();
        _update_network_arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.select_dialog_radiobutton,getResources().getStringArray(R.array.fota_update_network_array)) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text size 25 dip for ListView each item
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                if (is_mobile_phone_ui_disable(position)) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.BLACK);
                }

                if(position == update_network_select){
                    ((ListView)parent).setItemChecked(position, true);
                }else{
                    ((ListView)parent).setItemChecked(position, false);
                }
                return view;
            }

            @Override
            public boolean isEnabled(int position) {
                if(is_mobile_phone_ui_disable(position)){
                    return false;
                }
                return super.isEnabled(position);
            }
        };


        _update_network_listview.setAdapter(_update_network_arrayAdapter);
        _update_network_listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //update period adapter init
        final int update_period_select = _settingManager.get_update_period().toInteger();
        _update_period_arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), R.layout.select_dialog_radiobutton,getResources().getStringArray(R.array.fota_menu_update_check_period_array)) {
            @Override
            public View getView(final int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                // Set the text size 25 dip for ListView each item
                tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                if(position == update_period_select){
                    ((ListView)parent).setItemChecked(position, true);
                }else{
                    ((ListView)parent).setItemChecked(position, false);
                }
                return view;
            }
        };
        _update_period_listview.setAdapter(_update_period_arrayAdapter);
        _update_period_listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    }

    private boolean is_phone(){
        DeviceInformation deviceInformation = new DeviceInformation(getApplicationContext());
        return deviceInformation.check_phone() == DeviceInformation.PHONE_TYPE.PHONE ? true : false;
    }
    private boolean is_mobile_phone_ui_disable(int position){
        if(!is_phone() && (position == 2 || position == 3)){
            return true;
        }else{
            return false;
        }

    }
    private void save_and_finish() {
        _settingManager.set_update_network(FotaConstants.UPDATE_NETWORK.fromInteger(_update_network_listview.getCheckedItemPosition()));
        _settingManager.set_update_period(FotaConstants.UPDATE_PERIOD.fromInteger(_update_period_listview.getCheckedItemPosition()));

        //바뀐 설정으로 다시 서비스를 실행한다.
        ObservingUpdateService.startObservingService(getApplicationContext(),true,0);
        finish();
    }
    //inner functions(-)
}
