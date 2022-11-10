package com.example.fraleonebm;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity3 extends AppCompatActivity {
    Boolean running;
    float prevPer;
    int prevRange;
    Thread batUpdating;
    Handler handlerBat;
    Boolean needPermission;
    Class worker = MyWorker4.class;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.mainicon);

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        Boolean saveModeOn = sharedPref.getBoolean("saveModeStatus", false);
        checkSwitch(saveModeOn);
        int threshold = sharedPref.getInt("threshold", 40);
        setThreshold(threshold);
        needPermission = false;
        prepareThreshold();
//        help4();
    }

    @Override
    protected void onStart() {
        super.onStart();
        running = true;
        prevPer = -1;
        prevRange = -1;
        handlerBat = new Handler();
        final Runnable updateBat = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void run() {
                float batteryPct = findBatPer();
                changeBatPic((int)batteryPct);
                changeBatPer(batteryPct);
            }
        };
        batUpdating = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    handlerBat.post(updateBat);
                    SystemClock.sleep(5000);
                }
            }
        });
        batUpdating.start();

    }

    @Override
    protected void onStop() {
        running = false;
        super.onStop();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void checkSwitch(Boolean saveModeOn){
        Switch mswitch = (Switch) findViewById(R.id.saveMode);
        if(saveModeOn){
            mswitch.setChecked(true);
        }
        else{
            mswitch.setChecked(false);
        }
    }

    public void setThreshold(int t){
        if(t<100){
            EditText e = (EditText) findViewById(R.id.threshold);
            String str = String.valueOf(t);
            e.setText(str);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void saveMode(View view) {
        Switch mswitch = findViewById(R.id.saveMode);
        if (mswitch.isChecked()) {
            if(needPermission) {
                Boolean permission = checkPermission();
                if (permission) {
                    lunchWork();
                }
            }
            else{
                lunchWork();
            }
        }
        else{
            deleteWork();
        }
        updateData();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void newThreshold(View view){
        EditText e =findViewById(R.id.threshold);
        e.clearFocus();
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int threshold = sharedPref.getInt("threshold", 40);
        int newThreshold = getThreshold();
        if(newThreshold != threshold) {
            saveMode(view);
        }
    }

    public void showInfo(View view){
        Dialog d=new Dialog(this);
        d.setTitle("Info");
        d.setCancelable(true);
        d.setContentView(R.layout.dialog2);
        d.show();
    }

    public float findBatPer(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level * 100 / (float)scale;
        return batteryPct;
    }

    public void changeBatPic(int batteryPct){
        int range = ((int)((batteryPct - (batteryPct % 10))/10)/2)*20;

        if(prevRange != range) {
            ImageView bPic = findViewById(R.id.bPic);
            if (range >= 80) {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.b100);
                Bitmap b2 = Bitmap.createScaledBitmap(b, 300, 600, false);
                bPic.setImageBitmap(b2);
            } else if (range == 60) {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.b80);
                Bitmap b2 = Bitmap.createScaledBitmap(b, 300, 600, false);
                bPic.setImageBitmap(b2);
            } else if (range == 40) {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.b60);
                Bitmap b2 = Bitmap.createScaledBitmap(b, 300, 600, false);
                bPic.setImageBitmap(b2);
            } else if (range == 20) {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.b40);
                Bitmap b2 = Bitmap.createScaledBitmap(b, 300, 600, false);
                bPic.setImageBitmap(b2);
            } else {
                Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.b20);
                Bitmap b2 = Bitmap.createScaledBitmap(b, 300, 600, false);
                bPic.setImageBitmap(b2);
            }
        }
        prevRange = range;
    }

    public void changeBatPer(float batteryPct){
        if (prevPer != batteryPct) {
            String str = (String.valueOf((int) batteryPct) + "%");
            print(str, R.id.bPer);

            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent intent = registerReceiver(null, ifilter);
            int deviceHealth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,0);;
            String currentBatteryHealth="Battery\nstatus";
            if(deviceHealth == BatteryManager.BATTERY_HEALTH_COLD){
                currentBatteryHealth=currentBatteryHealth+":\nCold";
            }
            if(deviceHealth == BatteryManager.BATTERY_HEALTH_DEAD){
                currentBatteryHealth=currentBatteryHealth+":\nDead";
            }
            if (deviceHealth == BatteryManager.BATTERY_HEALTH_GOOD){
                currentBatteryHealth=currentBatteryHealth+":\nGood";
            }
            if(deviceHealth == BatteryManager.BATTERY_HEALTH_OVERHEAT){
                currentBatteryHealth=currentBatteryHealth+":\nOverHeat";
            }
            if (deviceHealth == BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE){
                currentBatteryHealth=currentBatteryHealth+":\nOvervoltage";
            }
            if (deviceHealth == BatteryManager.BATTERY_HEALTH_UNKNOWN){
                currentBatteryHealth=currentBatteryHealth+":\nUnknown";
            }
            if (deviceHealth == BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE){
                currentBatteryHealth=currentBatteryHealth+":\nUnspecified Failure";
            }
            float batteryTemp = (float)(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,0))/10;
            print(currentBatteryHealth, R.id.bStatus);
            print("Battery\ntemperature:\n"+batteryTemp, R.id.bTemp);
        }
        prevPer = batteryPct;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Boolean checkPermission() {
        Boolean usageAccess = checkUsageAccess();
            if(!usageAccess){
                askPermission();
                return checkUsageAccess();
            }
        return usageAccess;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void askPermission() {
        Dialog d=new Dialog(this);
        d.setTitle("Usage Access Permission Request");
        d.setCancelable(false);
        d.setContentView(R.layout.dialog1);
        Button yes=(Button) d.findViewById(R.id.granted);
        yes.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                d.dismiss();
                new Thread(){
                    @Override
                    public void run() {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                    }
                }.start();
                finish();
                System.exit(0);
            }
        });
        Button no=(Button) d.findViewById(R.id.denied);
        no.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                d.dismiss();
            }
        });
        d.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Boolean checkUsageAccess(){
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(getPackageName(), 0);
            AppOpsManager appOpsManager = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName);
            return (mode == AppOpsManager.MODE_ALLOWED);

        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void print(Object o, int id){
        TextView t = findViewById(id);
        t.setText(o.toString());
    }

    public int getThreshold(){
        int finalValue;
        EditText threshold = (EditText) findViewById(R.id.threshold);
        String value= threshold.getText().toString();
        try
        {
            finalValue=Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            finalValue = 40;
        }
        return finalValue;
    }

    public void updateData(){
        Switch mswitch = findViewById(R.id.saveMode);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("saveModeStatus", mswitch.isChecked());
        editor.putInt("threshold", getThreshold());
        editor.apply();
        new Thread(){
            @Override
            public void run() {
                editor.commit();
            }
        }.start();
    }

    public void prepareThreshold(){
        EditText etValue = findViewById(R.id.threshold);
        etValue.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {

                    etValue.clearFocus();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(etValue.getWindowToken(), 0);

                    SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                    int threshold = sharedPref.getInt("threshold", 40);
                    int newThreshold = getThreshold();
                    if(newThreshold != threshold) {
                        saveMode(findViewById(R.id.mainlay));
                    }
                    return true;
                }
                return false;
            }

        });
    }


    public void prepareWork(int index) {
//        Constraints constraints = new Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.UNMETERED)
//            .setRequiresCharging(true)
//            .build();


        String tag = "FLeoneCleanBackground" + index;
        int delay = index*15;
        int finalValue = getThreshold();


        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(worker, 15, /*TimeUnit.MINUTES,5,*/ TimeUnit.MINUTES)
//            .setConstraints(constraints)

//            .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)

                .setInputData(new Data.Builder().putInt("THRESHOLD", finalValue).build())

                .addTag(tag)

                .setInitialDelay(delay , TimeUnit.SECONDS)

                .build();

        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork(tag, ExistingPeriodicWorkPolicy.REPLACE, workRequest);
    }

    public void lunchWork(){
        deleteWork();
        for(int index = 0; index < 60; index++){
            prepareWork(index);
        }
    }

    public void deleteWork(){
        for(int index = 0; index < 60; index++){
            String tag = "FLeoneCleanBackground" + index;
            WorkManager.getInstance(getApplicationContext()).cancelAllWorkByTag(tag);
        }
    }

    public void help4(){
        int i = 0;
        List<PackageInfo> packages;
        PackageManager pm = getApplicationContext().getPackageManager();
        //get a list of installed apps through packages
        packages = pm.getInstalledPackages(0);
        ActivityManager mActivityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (PackageInfo packageInfo : packages) {
//            TextView out = findViewById(R.id.output);
//            String str = out.getText().toString() + '\n' + packageInfo.packageName + i;
//            print(packageInfo.packageName+i, R.id.output);
            i++;
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void help2(){
        int i = 0;
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
//                TextView out = findViewById(R.id.output);
//                String str = out.getText().toString() + '\n' + packageName + i;
//                print(packageName+i, R.id.output);
                i++;
            }
        }
    }
    public void help3(){
        int i = 0;
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = getApplicationContext().getPackageManager();
        //get a list of installed apps.
        packages = pm.getInstalledApplications(0);
        ActivityManager mActivityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        for (ApplicationInfo packageInfo : packages) {
//            TextView out = findViewById(R.id.output);
//            String str = out.getText().toString() + '\n' + packageInfo.packageName + i;
//            print(packageInfo.packageName+i, R.id.output);
            i++;
        }
    }

}

