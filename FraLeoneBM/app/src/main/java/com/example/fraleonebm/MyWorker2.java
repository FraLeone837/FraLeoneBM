//usageStats
package com.example.fraleonebm;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MyWorker2 extends Worker {
    public MyWorker2(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NotNull
    @Override
    public Result doWork() {

        int i = 0;
        int threshold = getInputData().getInt("THRESHOLD", 99);
        float per = findBatPer();

        if(/*isCharging() ||*/ per>threshold){
            return Result.success();
        }

        // Do the work here-
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.disable();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager am = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            UsageStatsManager usm = (UsageStatsManager) getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 15*60*1000, time);
            if (appList != null && appList.size() == 0) {
                //no app
            }
            if (appList != null && appList.size() > 0) {
                for (UsageStats usageStats : appList) {
                    String packageName = usageStats.getPackageName();
                    Log.d("Executed app", "usage stats executed : " +usageStats.getPackageName() + "\t\t ID: " +i);
                    if(!isMyPck(packageName)) {
                        am.killBackgroundProcesses(packageName);
                    }
                    i++;

                }
            }
        }

        return Result.success();
    }

    public Boolean isCharging(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);
        return isCharging;
    }

    public float findBatPer(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float)scale;
        return batteryPct;
    }

    public Boolean isMyPck(String pckName){
        String myPckName = getApplicationContext().getPackageName();
        return pckName==myPckName;
    }

}
