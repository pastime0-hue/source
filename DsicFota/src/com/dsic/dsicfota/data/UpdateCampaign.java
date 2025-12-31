package com.dsic.dsicfota.data;

import com.dsic.dsicfotalib.declations.RECEIVE_PROTOCOL;

public class UpdateCampaign {
    private boolean _update_being = false;
    private String _update_url = "";
    private String _update_version = "";
    private String _update_campaign_name = "";
    private int _file_count;
    private RECEIVE_PROTOCOL.FILE_INFO[] _file_infos = null;
    private RECEIVE_PROTOCOL _receive_protocol = null;

    public UpdateCampaign(RECEIVE_PROTOCOL receive_protocol) {
        _receive_protocol = receive_protocol;
        parsing_protocol(receive_protocol);
    }

    private void parsing_protocol(RECEIVE_PROTOCOL receive_protocol) {
        if (receive_protocol.get_check_update() == 'Y' ||
                receive_protocol.get_check_update() == 'y') {
            _update_being = true;
        } else {
            _update_being = false;
        }

        _file_count = receive_protocol.get_file_count();
        _update_url = receive_protocol.get_update_url();
        _update_version = receive_protocol.get_update_version();
        _file_infos = receive_protocol.get_file_infos();
        _update_campaign_name = receive_protocol.get_update_campaign();
    }

    public boolean is_checked_update() {
        return _update_being;
    }

    public int get_file_count() {
        return _file_count;
    }

    public String get_update_url() {
        return _update_url;
    }

    public String get_update_version() {
        return _update_version;
    }

    public RECEIVE_PROTOCOL.FILE_INFO[] get_file_infos() {
        return _file_infos;
    }

    public long get_file_total_size() {
        long total_size = 0;
        for (int i = 0; i < _file_count; i++) {
            total_size += _file_infos[i].get_file_size();
        }
        return total_size;
    }

    public String get_update_note() {
        String update_note = "";
        for (int i = 0; i < _file_count; i++) {
            if(_file_infos[i].get_file_tag().compareTo("OS") == 0){
                update_note = _file_infos[i].get_file_info();
            }
        }
        return update_note;
    }

    public String get_campaign_name(){
        return _update_campaign_name;
    }

    public RECEIVE_PROTOCOL get_receive_protocol() {
        return _receive_protocol;
    }
}