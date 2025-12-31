package com.dsic.dsicfota.declaration;

import com.dsic.dsicfota.Utils.LOG;

public class FotaConstants {
    public static final boolean DEBUG = false;

    public static final String EMPTY = "";
    public static final String HYPHEN = "-";

    public static final String HTTP_PREFIX = "http://";
    public static final String HTTP_SLASH = "/";

    public static final String DOWNLOAD_FAIL = "Exception: ";
    public static final String DOWNLOAD_SUCCESS = "Download : Success";

    // FOTA 파일 복사 또는 다운로드 진행중인지 체크. 시작 1, 종료 0
    public static final String OTA_VERSION_PROPERTY = "persist.sys.ota.version";

    // FOTA를 시작하여 RECOVERY모드를 진행할 경우 1로 설정. 업데이트 후 부팅시 캐시삭제용.
    public static final String OTA_UPDATE_FINISH = "persist.sys.ota.finish";

    public static final String BARCODE_UPDATE = "com.android.settings.autosetup.BARCODE_UPDATE";

    public static final String DOWNLOAD_FILE_FILENAME = "update.zip";
    //public static final String DOWNLOAD_FILE_FILENAME = "ds6-q-gms-178-user.zip";
    //public static final String DOWNLOAD_PATH = "/sdcard/Download";
    //public static final String DOWNLOAD_PATH = "/data/data/com.dsic.dsicfota";
    public static final String DOWNLOAD_PATH = "/data/ota_package";
    public static final String UPDATE_PATH = "/data/ota_package";
    //public static final String DOWNLOAD_PATH = "/data/data/com.dsic.dsicfota";
    //public static final String BUCKET_NAME = "TEST BUCKET";
    public static final String BUCKET_NAME = "dsicfota";//정식버전
    //public static final String BUCKET_NAME = "dsictest";//개발용

    public static final String GMS_PACKAGE = "com.google.android.gms";

    public static final String ACTION_UPDATE_NOTIFICATION = "ACTION_UPDATE_NOTIFICATION";
    public static final String EVENT_START = "EVENT_START";

    public static final String ACTION_BARCODE_UPDATE = "ACTION_BARCODE_UPDATE";

    public static final String EVENT_MODEL = "p_DIR_NM01";
    public static final String EVENT_ANDROID_VERSION = "p_DIR_NM02";
    public static final String EVENT_GMS = "p_DIR_NM03";
    public static final String EVENT_OS_TYPE = "p_DIR_NM04";
    public static final String EVENT_PHONE = "p_DIR_NM05";
    public static final String EVENT_REQUEST_VERSION = "p_REQ_VER";
    public static final String EVENT_REQUEST_CAMPAIGN_VERSION = "p_REQ_CPN_DWN_ID";
    public static final String EVENT_SELECTED_UPDATE_NETWORK = "SELECTED_UPDATE_NETWORK";
    public static final String EVENT_SELECTED_UPDATE_MODE = "SELECTED_UPDATE_MODE";

    public static final String BR_CHECK_SETUP_WIZARD_FINISH = "com.dsic.dsicfota.CHECK_SETUP_WIZARD_FINISH";
    public static final String BR_OBSERVE_UPDATE = "com.dsic.dsicfota.OBSERVE_UPDATE";

    public static final String AUTOSETUP_FINISH = "AUTOSET_FINISHED";
    public static final String AUTOSETUP_FOTA_INVALID_DEVICE = "AUTOSETUP_FOTA_INVALID_DEVICE";
    public static final String AUTOSETUP_FOTA_INVALID_DEVICE_PARAM = "AUTOSETUP_FOTA_INVALID_DEVICE_PARAM";

    public static final int NOTIFY_NUMBER = 0x7f020059;
    public enum UPDATE_TYPE {
        UNKNOWN(0),
        GENERAL(1),
        BARCODE_RELEASE(2),
        SIMULATION(3),
        BARCODE_SIMULATION(4);
        private int _value;

        UPDATE_TYPE(int value) {
            this._value = value;
        }

        public char get_code() {
            char value = 0x00;
            switch (_value) {
                case 1:
                    value = 'G';//일반 릴리즈
                    break;
                case 2:
                    value = 'B';//바코드 릴리즈
                    break;
                case 3:
                    value = 'S';//일반 시뮬레이션
                    break;
                case 4:
                    value = 'T';//바코드 시뮬레이션
                    break;
            }
            return value;
        }

        public static UPDATE_TYPE fromInteger(final int value){
            if(value < 0|| value > 4){
                LOG.ERR("update type from Integer failed->"+value);
                return UPDATE_TYPE.UNKNOWN;
            }
            return UPDATE_TYPE.values()[value];
        }

        public int toInteger(){
            return _value;
        }
    }

    public static enum UPDATE_PERIOD{
        SIX_HOUR(0),
        TWELVE_HOUR(1),
        ONE_DAY(2);
        private int _value;
        UPDATE_PERIOD(int value){
            this._value = value;
        }

        public static UPDATE_PERIOD fromInteger(final int value){
            if(value < 0|| value > 2){
                LOG.ERR("update period from Integer failed->"+value);
                return UPDATE_PERIOD.ONE_DAY;
            }
            return UPDATE_PERIOD.values()[value];
        }

        public int get_second(){
            int second = 0;
            final int ONE_HOUR_SEC = 60*60;
            switch (_value){
                case 0:
                    second = ONE_HOUR_SEC * 6;
                    break;
                case 1:
                    second = ONE_HOUR_SEC * 12;
                    break;
                case 2:
                    second = ONE_HOUR_SEC * 24;
                    break;

            }
            return second;
        }

        public int toInteger(){
            return this._value;
        }
    }

    public static enum UPDATE_NETWORK{
        NONE(0),
        ONLY_WIFI(1),
        WIFI_AND_CELL(2),
        ONLY_CELL(3);
        private int _value;
        UPDATE_NETWORK(int value){
            this._value = value;
        }

        public static UPDATE_NETWORK fromInteger(final int value){
            if(value < 0|| value > 3){
                LOG.ERR("update network from Integer failed->"+value);
                return UPDATE_NETWORK.ONLY_WIFI;
            }
            return UPDATE_NETWORK.values()[value];
        }

        public int toInteger(){
            return this._value;
        }
    }
}
