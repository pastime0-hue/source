package com.dsic.dsicfota.data;

import com.dsic.dsicfota.Utils.LOG;
import com.dsic.dsicfota.declaration.FotaConstants;
import com.dsic.dsicfotalib.Observer;
import com.dsic.dsicfotalib.declations.RECEIVE_PROTOCOL;
import com.dsic.dsicfotalib.declations.SEND_PROTOCOL;

import android.content.Context;

import java.util.Date;


public class UpdateChecker {
    private static UpdateChecker _update_checker = null;
    private UpdateCampaign _update_campaign = null;

    private UpdateChecker() {
        LOG.DEBUG("update checker is made");
    }

    public static UpdateChecker getInstance() {
        if (_update_checker == null) {
            _update_checker = new UpdateChecker();
        }
        return _update_checker;
    }

    public UpdateCampaign update_campaign_information(Context context, FotaConstants.UPDATE_TYPE update_type , String request_version,String request_campaign_name) {
        //RECEIVE_PROTOCOL로 해당 정보를 가져와서
        DeviceInformation devInfo = new DeviceInformation(context);
        SEND_PROTOCOL send_protocol = new SEND_PROTOCOL(devInfo.serial_number(), devInfo.bucket_name(), devInfo.get_last_update_campaign(), devInfo.manufacture_date(),
                devInfo.device_name(), devInfo.os_version(), devInfo.check_gms().name(), devInfo.customer(), devInfo.check_phone().name(), devInfo.os_build_number(), "", "", "" +
                "", devInfo.os_build_number(), update_type.get_code(), request_version,request_campaign_name);
        //test code
        /*
        SEND_PROTOCOL send_protocol = new SEND_PROTOCOL("DSICFOTA-002","dsicfota","","",
                "ds6","7.1.2","Customer global","Phone","NOGMS","1170","","","","1170",'G',"","");
         */
        //서버로 던져 데이터를 가져온다

        RECEIVE_PROTOCOL receive_protocol = Observer.getInstance().get_update_information(send_protocol);
        //업데이트 확인 시간 저장
        (new SettingManager(context)).set_last_update_check_date(new Date());
        //해당 내용을 업데이트
        _update_campaign = new UpdateCampaign(receive_protocol);

        return _update_campaign;
    }

    public UpdateCampaign get_update_campaign() {
        if (_update_campaign == null) {
            RECEIVE_PROTOCOL receive_protocol = new RECEIVE_PROTOCOL(0,'N',
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    0,null);
            _update_campaign = new UpdateCampaign(receive_protocol);
        }
        return _update_campaign;
    }

}
