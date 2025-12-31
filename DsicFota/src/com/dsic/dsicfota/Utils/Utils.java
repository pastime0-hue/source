package com.dsic.dsicfota.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;

import android.view.Display;
import android.graphics.Point;

public class Utils{
    public static boolean STAND_ALONE = false;
    private static final int LOW_BATTERY_LEVEL = 20;
    public static double bytes_to_megabytes(long byte_size){
        return byte_size / (double)(1024 * 1024);
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024*1024*5];//버퍼 5메가
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    public static boolean rename(File src, File dst){
        return src.renameTo(dst);
    }

    public static void delete(File file){
        file.delete();
    }

    public static String normalize_second(int second){
        final int MIN = 60;
        final int HOUR = 60*60;
        //LOG.DEBUG(second+" second");
        if(second < MIN){
            return String.format("%ds",second);
        }
        if(second < HOUR){
            return String.format("%dm %ds",second / MIN, second % MIN);
        }

        return String.format("%dh %dm %ds",second / HOUR, (second - ((second / HOUR)*HOUR)) / MIN, second % MIN);
    }

    public static void sendFinishToAutoSetting(Context context, String action_name , String extra_name, String param){
        Intent intent = new Intent(action_name);
        intent.putExtra(extra_name, param);
        LOG.DEBUG("FOTA SEND INTENT:ACTION:"+action_name+"->Extra:"+extra_name+"->Param:"+param);
        context.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    public static boolean checkLowBattery(Context context) {
        int battLevel = 0;
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager bm = (BatteryManager)context.getSystemService(Context.BATTERY_SERVICE);
            battLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }else{
            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

            float batteryPct = level / (float) scale;
            battLevel = (int) (batteryPct * 100);
        }

        LOG.DEBUG("checkLowBattery Level=" + battLevel );

        if (battLevel < LOW_BATTERY_LEVEL){
            return true;
        }else {
            return false;
        }
    }

    public static long get_free_space_on_disk(){
     
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable;
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
        }
        else {
            bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();
        }
        return bytesAvailable;

        /*
        long dummy_space = 922746880;//880mb
        return dummy_space;

         */
    }

    public static String floatForm (double d)
    {
        return new DecimalFormat("#.##").format(d);
    }


    public static String bytesToHuman (long size)
    {
        long Kb = 1  * 1024;
        long Mb = Kb * 1024;
        long Gb = Mb * 1024;
        long Tb = Gb * 1024;
        long Pb = Tb * 1024;
        long Eb = Pb * 1024;

        if (size <  Kb)                 return floatForm(        size     ) + " byte";
        if (size >= Kb && size < Mb)    return floatForm((double)size / Kb) + " KB";
        if (size >= Mb && size < Gb)    return floatForm((double)size / Mb) + " MB";
        if (size >= Gb && size < Tb)    return floatForm((double)size / Gb) + " GB";
        if (size >= Tb && size < Pb)    return floatForm((double)size / Tb) + " TB";
        if (size >= Pb && size < Eb)    return floatForm((double)size / Pb) + " PB";
        if (size >= Eb)                 return floatForm((double)size / Eb) + " EB";

        return "???";
    }

    public static boolean fota_enable(){
        int fotaInt = SystemProperties.getInt("persist.sys.fota.enable", 1);
        if(fotaInt == 1){
            return true;
        }else{
            return false;
        }
    }

    public enum DISPLAY_SIZE{
        UNKNOWN_SIZE,
        SIZE_480,
        SIZE_720,
    }
    public static DISPLAY_SIZE get_device(Activity activity){
        DISPLAY_SIZE pda_device = DISPLAY_SIZE.UNKNOWN_SIZE;
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        //DS5AX 480,728(800)
        //DS6 720, 1184(1280)
        //DS4A 480, 728(800)
        //LOG.DEBUG("width" + width);
        //LOG.DEBUG("height" + height);
        if(width == 480){
            pda_device = DISPLAY_SIZE.SIZE_480;
        }else if(width == 720){
            pda_device = DISPLAY_SIZE.SIZE_720;
        }
        return pda_device;
    }

    public enum PLATFORM{
        UNKNOWN,
        MSM8953,
        SDM660;
        public static String get_name(PLATFORM platform){
            String result = "UNKNOWN";
            switch (platform){
                case MSM8953:
                    result = "msm8953";
                    break;
                case SDM660:
                    result = "sdm660";
                    break;
            }
            return result;
        }

        public static PLATFORM get_platform(String name){
            if(name == null || name.compareTo("") == 0){
                return UNKNOWN;
            }
            name = name.toLowerCase();

            PLATFORM platform = UNKNOWN;
            if(name.compareTo("msm8953") == 0){
                platform = MSM8953;
            }else if(name.compareTo("sdm660") == 0){
                platform = SDM660;
            }
            return platform;
        }

    }

    public static PLATFORM get_platform_type(){
        String platform_name = SystemProperties.get("ro.board.platform", "");
        return PLATFORM.get_platform(platform_name);
    }
}