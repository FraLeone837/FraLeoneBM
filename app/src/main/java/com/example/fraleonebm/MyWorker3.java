//getInstalledApplications
package com.example.fraleonebm;

import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ChangedPackages;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.pm.SharedLibraryInfo;
import android.content.pm.VersionedPackage;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.UserHandle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MyWorker3 extends Worker {
    public MyWorker3(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NotNull
    @Override
    public Result doWork() {

        int threshold = getInputData().getInt("THRESHOLD", 99);
        float per = findBatPer();

        if(/*isCharging() ||*/ per>threshold){
            return Result.success();
        }

        // Do the work here-
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.disable();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            List<ApplicationInfo> packages;
            PackageManager pm;
            pm = getApplicationContext().getPackageManager();
            //get a list of installed apps.
            packages = pm.getInstalledApplications(0);
            ActivityManager mActivityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
            for (ApplicationInfo packageInfo : packages) {
                if(!isMyPck(packageInfo.packageName)) {
                    mActivityManager.killBackgroundProcesses(packageInfo.packageName);
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
