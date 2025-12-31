package com.dsic.dsicfota.data;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.dsic.dsicfota.R;
import com.dsic.dsicfota.declaration.FotaConstants;

public class RefreshUpdateStatusTask extends AsyncTask<String, String, Boolean> {
    private ProgressDialog _progress_dialog = null;
    private SettingManager _setting_manager = null;
    private NetStateChecker _net_state_checker = null;
    private CALLBACK _callback = null;
    private Context _context = null;
    private FotaConstants.UPDATE_TYPE _update_type;
    private FotaConstants.UPDATE_NETWORK _update_network;
    private String _request_version;
    private String _request_campaign_id;

    public interface CALLBACK {
        void RESULT_CALLBACK(boolean refresh_success);
    }

    public RefreshUpdateStatusTask(final Context context, final FotaConstants.UPDATE_TYPE update_type, final FotaConstants.UPDATE_NETWORK update_network,
                                   final String request_version, final String request_campaign_id, final CALLBACK callback) {
        _context = context;
        _setting_manager = new SettingManager(_context);
        _net_state_checker = new NetStateChecker(_context);
        _callback = callback;
        _update_type = update_type;
        _update_network = update_network;
        _request_version = request_version;
        _request_campaign_id = request_campaign_id;
    }

    @Override
    protected void onPreExecute() {
        _progress_dialog = new ProgressDialog(_context);
        _progress_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        _progress_dialog.setMessage(_context.getResources().getString(R.string.fota_on_update_checking));
        _progress_dialog.setCanceledOnTouchOutside(false);
        _progress_dialog.setCancelable(false);
        _progress_dialog.show();
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        return request_update_campaign(_update_type, _update_network, _request_version, _request_campaign_id);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        Toast.makeText(_context, values[0], Toast.LENGTH_SHORT).show();
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (_progress_dialog != null) {
            _progress_dialog.dismiss();
            _progress_dialog = null;
        }
        _callback.RESULT_CALLBACK(result);

        super.onPostExecute(result);
    }

    @Override
    protected void onCancelled() {
        if (_progress_dialog != null) {
            _progress_dialog.dismiss();
            _progress_dialog = null;
        }
        super.onCancelled();
    }


    private boolean request_update_campaign(FotaConstants.UPDATE_TYPE update_type, FotaConstants.UPDATE_NETWORK update_network, String request_version, String request_campaign_id) {
        //network가 사용 가능한지 확인해보고
        if (_net_state_checker.is_network_available() == false) {
            publishProgress(_context.getResources().getString(R.string.fota_network_inavailable));
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.NONE) {
            publishProgress(_context.getResources().getString(R.string.fota_message_update_network_none));
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.ONLY_WIFI && _net_state_checker.is_wifiNetwork_available() == false) {
            publishProgress(_context.getResources().getString(R.string.fota_message_wifi_network_unavailable));
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.ONLY_CELL && _net_state_checker.is_cellNetwork_available() == false) {
            publishProgress(_context.getResources().getString(R.string.fota_message_mobile_network_unavailable));
            return false;
        }

        if (update_network == FotaConstants.UPDATE_NETWORK.WIFI_AND_CELL && _net_state_checker.is_network_available() == false) {
            publishProgress(_context.getResources().getString(R.string.fota_message_all_network_unavailable));
            return false;
        }


        UpdateChecker.getInstance().update_campaign_information(_context, update_type, request_version, request_campaign_id);
        return UpdateChecker.getInstance().get_update_campaign().is_checked_update();

    }
}
