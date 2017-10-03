package com.adobe.mobile;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.HashMap;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

public class CustomAndroidSystemObject {
    private String packageName;
    private String appVer;
    private Integer appState;

    private String modelName;
    private String osVer;
    private Activity activity;
    private String iccImei;
    private String chkAppState;
    private SharedPreferences pref;
    private String appId;
    private static final String OSNAME = "Android";
    private static final String EVENT_INSTALL = "install";
    private static final String EVENT_PAGE_VIEW = "launch";
    private static final String EVENT_VERUP = "upgrade";

    private Context context;//Widget用

    public CustomAndroidSystemObject(Activity activity, SharedPreferences pref) {
        this.activity = activity;
        this.pref = pref;
        this.packageName = this.activity.getPackageName();
        this.iccImei = "null-null";

        // check permission

        try {
            int permissionCheck = ContextCompat.checkSelfPermission(this.activity, Manifest.permission.READ_PHONE_STATE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager tm = (TelephonyManager) this.activity.getSystemService(Context.TELEPHONY_SERVICE);
                String iccid = tm.getSimSerialNumber();
                String imei = tm.getDeviceId();
                if (iccid.isEmpty()) iccid = "null";
                if (imei.isEmpty()) imei = "null";
                this.iccImei = iccid + "-" + imei;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



// AppVersion

        this.appState = new Integer(0);
        this.appVer = "";
        PackageManager packageManager = this.activity.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(this.activity.getPackageName(), PackageManager.GET_ACTIVITIES);
            this.appVer = packageInfo.versionName;
            this.appState = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        this.modelName = Build.MODEL;
        this.osVer = Build.VERSION.RELEASE;
        this.chkAppState = this.getFirstLaunch(this.appState.toString());
//        int id = activity.getResources().getIdentifier("app_name", "string", activity.getPackageName());
        int id = activity.getResources().getIdentifier("application_name", "string", activity.getPackageName());
        this.appId = activity.getResources().getString(id);
    }

    public CustomAndroidSystemObject(Context context, SharedPreferences pref) {//Widget用
//        this.activity = activity;
        this.context = context;
        this.pref = pref;
//        this.packageName = this.activity.getPackageName();
        this.packageName = this.context.getPackageName();
        this.iccImei = "null-null";

        // check permission

        try {
//            int permissionCheck = ContextCompat.checkSelfPermission(this.activity, Manifest.permission.READ_PHONE_STATE);
            int permissionCheck = ContextCompat.checkSelfPermission(this.context, Manifest.permission.READ_PHONE_STATE);
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
//                TelephonyManager tm = (TelephonyManager) this.activity.getSystemService(Context.TELEPHONY_SERVICE);
                TelephonyManager tm = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
                String iccid = tm.getSimSerialNumber();
                String imei = tm.getDeviceId();
                if (iccid.isEmpty()) iccid = "null";
                if (imei.isEmpty()) imei = "null";
                this.iccImei = iccid + "-" + imei;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



// AppVersion

        this.appState = new Integer(0);
        this.appVer = "";
//        PackageManager packageManager = this.activity.getPackageManager();
        PackageManager packageManager = this.context.getPackageManager();
        try {
//            PackageInfo packageInfo = packageManager.getPackageInfo(this.activity.getPackageName(), PackageManager.GET_ACTIVITIES);
            PackageInfo packageInfo = packageManager.getPackageInfo(this.context.getPackageName(), PackageManager.GET_ACTIVITIES);
            this.appVer = packageInfo.versionName;
            this.appState = packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        this.modelName = Build.MODEL;
        this.osVer = Build.VERSION.RELEASE;
        this.chkAppState = this.getFirstLaunch(this.appState.toString());
//        int id = activity.getResources().getIdentifier("app_name", "string", activity.getPackageName());
//        int id = activity.getResources().getIdentifier("application_name", "string", activity.getPackageName());
        int id = context.getResources().getIdentifier("application_name", "string", context.getPackageName());
//        this.appId = activity.getResources().getString(id);
        this.appId = context.getResources().getString(id);
    }//Widget用

    public String getPackageName() {
        return this.packageName;
    }

    public String getAppVer() {
        return this.appVer;
    }

    public String getOSNAME() {
        return this.OSNAME;
    }

    public String getModelName() {

        return this.modelName;
    }

    public String getOsVer() {
        return this.osVer;
    }

    public String getIccImei() {
        return this.iccImei;
    }

    public String getChkAppState() {
        return this.chkAppState;
    }

    private String getFirstLaunch(String version) {
        String ret = "";
        Editor edit = this.pref.edit();
        String installed = pref.getString("version", "0");
        if (installed.equals("0")) {
            edit.putString("version", version);
            ret = EVENT_INSTALL;
        } else if (installed.equals(version)) {
            ret = EVENT_PAGE_VIEW;
        } else {
            edit.putString("version", version);
            ret = EVENT_VERUP;
        }
        edit.commit();
        return ret;
    }

    public String getAppId() {
        return this.appId;
    }
}
