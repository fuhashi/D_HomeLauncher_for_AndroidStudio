package jp.co.disney.apps.managed.kisekaeapp;

/*********************************************************************
** App Logging Libraly  for App&LWP&Player&Widget
** 02/19/2016  V2.0.2                                   Write by TWDCJ
*********************************************************************/

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.service.wallpaper.WallpaperService;
import android.support.v4.content.PermissionChecker;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.adobe.adms.measurement.ADMS_Measurement;

public class TrackingHelper {

    private static final String TRACKING_RSID = "wdgintjpspcapps";
    private static final String TRACKING_RSID_STG = "wdgintjpspcappsdev";
    private static final String TRACKING_SERVER = "disneyinternational.112.2o7.net:80";

    public static void startActivity(Activity activity){
        ADMS_Measurement measurement = ADMS_Measurement.sharedInstance(activity);
        measurement.startActivity(activity);
    }

    public static void stopActivity(){
        ADMS_Measurement measurement = ADMS_Measurement.sharedInstance();
        measurement.stopActivity();
    }

    public static void configureAppMeasurement(Activity activity, boolean stg){
        ADMS_Measurement measurement = ADMS_Measurement.sharedInstance(activity);
        if(stg == true){
        	measurement.configureMeasurement(TRACKING_RSID_STG, TRACKING_SERVER);
        }
        else{
        	measurement.configureMeasurement(TRACKING_RSID, TRACKING_SERVER);
        }
    }
    public static void configureAppMeasurement(Context context, boolean stg){
        ADMS_Measurement measurement = ADMS_Measurement.sharedInstance(context);
        measurement.configureMeasurement(TRACKING_RSID, TRACKING_SERVER);
        if(stg == true){
        	measurement.configureMeasurement(TRACKING_RSID_STG, TRACKING_SERVER);
        }
        else{
        	measurement.configureMeasurement(TRACKING_RSID, TRACKING_SERVER);
        }
    }

    // Examples of Custom Event and AppState Tracking
    public static void trackCustomEvents (String events, boolean stg){
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        contextData.put("contextKey", "value");
        ADMS_Measurement measurement = ADMS_Measurement.sharedInstance();
        measurement.trackEvents(events, contextData);
    }

    public static void trackCustomAppState (String appState){
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        contextData.put("contextKey", "value");
        ADMS_Measurement measurement = ADMS_Measurement.sharedInstance();
        measurement.trackAppState(appState, contextData);
    }

/*      Diseny Add  */

    //prop
    private static final int    iMEMBER_NAME1    = 29;
    private static final int   iOLD_PACKAGE_NAME1= 36;
    private static final int    iOLD_APP_VER     = 37;
    private static final int    iOLD_OS_NAME     = 38;
    private static final int    iOLD_MODEL_NAME  = 39;
    private static final int    iOLD_OS_VER      = 40;
    private static final int    iEVENT_NUM1      = 51;
    private static final int    OBJECT_NAME1     = 52;
    private static final int    iNEW_MARKET_NAME1= 53;
    private static final int    iCUSTOM_VALUE2   = 54;
    private static final int    iOLD_USER_ID2    = 55;
    private static final int    iCUSTOM_LINK_URL = 56;
    //eVar
    private static final int    iOLD_USER_ID1    = 36;
    private static final int    iCUSTOM_VALUE    = 37;
    private static final int    iOLD_INSTALL_DATE= 38;
    private static final int    iEXT_DATA        = 62;
    private static final int    iEVENT_NUM2      = 66;
    private static final int    OBJECT_NAME2     = 67;
    private static final int    iNEW_MARKET_NAME2= 68;
    private static final int   iOLD_PACKAGE_NAME2= 69;
    private static final int    iMEMBER_NAME2    = 35;

    //events
    private static final String EVENT_INSTALL    = "event1";
    private static final String EVENT_PAGE_VIEW  = "event3";
    private static final String EVENT_START      = "event31";
    private static final String EVENT_STOP       = "event32";
    //private static final String EVENT_SUSPEND    = "event33";
    private static final String EVENT_RESUME     = "event34";
    private static final String EVENT_CUSTOM     = "event35";
    private static final String EVENT_VERUP      = "event38";
    private static final String EVENT_WIDGET     = "event19";
    private static final String EVENT_KEEPALIVE  = "event20";

    private static final String THIS_OS_NAME     = "Android";
    private static final String SP_PAGE_NAME     = ".intent";
    private static final String PREVIEW_APP      = "jp.co.disney.apps.dmd.dmarketpreview";
    private static final String PREF_NAME        = "market_pref";
    private static final String CONNECT_NAME     = "CONNECT_ENV";
    private static final String CONNECT_LIVE     = "liv";
    private static final String CNT_NAME         = "CNT_ENV";
    private static final String CNT_PUB          = "pub";
    private static final String PERMISSION_PHONE = "android.permission.READ_PHONE_STATE";

    private static ADMS_Measurement m_s = null;
    private static Activity m_activity = null;
    private static Context m_context = null;
    private static Context m_appli1 = null;
    private static String m_account = "";
    private static String m_cCode = "JPY";
    private static String m_cSet = "UTF-8";
    private static PackageManager m_pManager = null;
    private static String m_aVersion = "";
    private static String m_packageName = "";
    private static String m_sImei = "";
    private static String m_sIccId = "";
    private static String m_modelName = "";
    private static String m_osVersion = "";
    private static String m_marketName = "";
    private static String m_className = "";

    public static void newTrackingHelper (Activity active, String marketname, boolean stg)
    {
       if(active == null){
            return;
        }
        m_activity = active;
        m_context = m_appli1 = active.getApplication();
        m_pManager = m_appli1 .getPackageManager();
        try{
            m_aVersion = m_pManager.getPackageInfo(m_appli1.getPackageName(), 0). versionName;
            m_packageName = m_pManager.getPackageInfo(m_appli1.getPackageName(), 0).packageName;
        }
        catch(NameNotFoundException e){
            e.printStackTrace();
        }
        m_className = active.toString();
        int i = m_className.lastIndexOf("@");
        m_className = m_className.substring(0, i);
        checkAndSetPhoneState();
        m_modelName = Build.MODEL;
        m_osVersion = Build.VERSION.RELEASE;
        m_s = ADMS_Measurement.sharedInstance(m_activity.getApplication());
        m_s.clearVars();
        m_s.setOfflineTrackingEnabled(true);
        m_s.setCurrencyCode(m_cCode);
        m_s.setCharSet(m_cSet);
        if(stg == true){
            m_account = TRACKING_RSID_STG;
            m_s.setDebugLogging(true);
        }
        else{
            m_account = TRACKING_RSID;
        }
        m_s.configureMeasurement(m_account, TRACKING_SERVER);
        m_s.startActivity(active);
    }

    public static void newTrackingHelper (WallpaperService context, boolean stg)
    {
       if(context == null){
            return;
        }
        m_context = context;
        m_pManager = context.getPackageManager();
        try{
            m_aVersion = m_pManager.getPackageInfo(context.getPackageName(), 0). versionName;
            m_packageName = m_pManager.getPackageInfo(context.getPackageName(), 0).packageName;
        }
        catch (NameNotFoundException e){
            e.printStackTrace();
        }
        checkAndSetPhoneState();
        m_modelName = Build.MODEL;
        m_osVersion = Build.VERSION.RELEASE;
        m_className = context.toString();
        int i = m_className.lastIndexOf("@");
        m_className = m_className.substring(0, i);
        m_s = ADMS_Measurement.sharedInstance(context);
        m_s.clearVars();
        m_s.setOfflineTrackingEnabled(true);
        m_s.setCurrencyCode(m_cCode);
        m_s.setCharSet(m_cSet);
        if(stg == true){
            m_account = TRACKING_RSID_STG;
            m_s.setDebugLogging(true);
        }
        else{
            m_account = TRACKING_RSID;
        }
        m_s.configureMeasurement(m_account, TRACKING_SERVER);
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        contextData.put("key", "value");
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        m_s.setAppState(m_packageName);
        String event = getFirstLaunch(m_aVersion);						// get Events
        event = event + "," + EVENT_START;
        m_s.setEvents(event);
        m_s.track(contextData, null);
    }

    public static void newTrackingHelper (WallpaperService context)
    {
       if(context == null){
            return;
        }

       boolean retBool = isLive((Context)context);
       if(retBool){
           newTrackingHelper(context, false);
       }
       else{
           newTrackingHelper(context, true);
       }
    }

    public static void newTrackingHelper (Context context, boolean stg)
    {
       if(context == null){
            return;
        }
        m_context = context;
        m_pManager = context.getPackageManager();
        try{
            m_aVersion = m_pManager.getPackageInfo(context.getPackageName(), 0). versionName;
            m_packageName = m_pManager.getPackageInfo(context.getPackageName(), 0).packageName;
        }
        catch (NameNotFoundException e){
            e.printStackTrace();
        }
        checkAndSetPhoneState();
        m_modelName = Build.MODEL;
        m_osVersion = Build.VERSION.RELEASE;
        m_className = context.toString();
        int i = m_className.lastIndexOf("@");
        m_className = m_className.substring(0, i);
        m_s = ADMS_Measurement.sharedInstance(context);
        m_s.clearVars();
        m_s.setOfflineTrackingEnabled(true);
        m_s.setCurrencyCode(m_cCode);
        m_s.setCharSet(m_cSet);
        if(stg == true){
            m_account = TRACKING_RSID_STG;
            m_s.setDebugLogging(true);
        }
        else{
            m_account = TRACKING_RSID;
        }
        m_s.configureMeasurement(m_account, TRACKING_SERVER);
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        m_s.setAppState(m_packageName);
        String event = getFirstLaunch(m_aVersion);                      // get Events
        event = event + "," + EVENT_START;
        m_s.setEvents(event);
        m_s.track(contextData, null);
    }

    public static void newTrackingHelper (Context context, String marketname, boolean stg)
    {
        if(context == null){
            return;
        }
        if(!marketname.equals("")){
            m_marketName = marketname;
        }
        m_context = context;
        m_pManager = m_context.getPackageManager();
        try {
            m_aVersion = m_pManager.getPackageInfo(m_context.getPackageName(), 0).versionName;
            m_packageName = m_pManager.getPackageInfo(m_context.getPackageName(), 0).packageName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        m_className = m_context.getPackageName();
        checkAndSetPhoneState();
        m_modelName = Build.MODEL;
        m_osVersion = Build.VERSION.RELEASE;
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance(m_context);
        }
        try{
            m_s.clearVars();
            m_s.setOfflineTrackingEnabled(true);
            m_s.setCurrencyCode(m_cCode);
            m_s.setCharSet(m_cSet);
            if(stg == true){
                m_account = TRACKING_RSID_STG;
                m_s.setDebugLogging(true);
            }
            else{
                m_account = TRACKING_RSID;
            }
            m_s.configureMeasurement(m_account, TRACKING_SERVER);
        }
        catch(NullPointerException e){
            e.printStackTrace();
            if(!marketname.equals("")){
                m_marketName = marketname;
            }
            m_context = context;
            m_pManager = m_context.getPackageManager();
            try{
                m_aVersion = m_pManager.getPackageInfo(m_context.getPackageName(), 0).versionName;
                m_packageName = m_pManager.getPackageInfo(m_context.getPackageName(), 0).packageName;
            }
            catch(NameNotFoundException e1){
                e1.printStackTrace();
            }
            m_className = m_context.getPackageName();
            checkAndSetPhoneState();
            m_modelName = Build.MODEL;
            m_osVersion = Build.VERSION.RELEASE;
            if(m_s == null){
                m_s = ADMS_Measurement.sharedInstance(m_context);
                if(m_s == null){
                    return;
                }
            }
            try{
                m_s.clearVars();
                m_s.setOfflineTrackingEnabled(true);
                m_s.setCurrencyCode(m_cCode);
                m_s.setCharSet(m_cSet);
                if(stg == true){
                    m_account = TRACKING_RSID_STG;
                    m_s.setDebugLogging(true);
                }
                else{
                    m_account = TRACKING_RSID;
                }
                m_s.configureMeasurement(m_account, TRACKING_SERVER);
            }
            catch(NullPointerException e2){
                e2.printStackTrace();
            }
        }
    }

    public static void start_top (Activity active, String marketname, boolean stg)
    {
        newTrackingHelper(active, marketname, stg);
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        String event = getFirstLaunch(m_aVersion);						// get Events
        event = event + "," + EVENT_START;
        m_s.setEvents(event);
        m_s.setAppState(m_packageName);
        m_s.track(contextData, null);
    }

    public static void start_top (Activity active, String marketname)
    {
        boolean retBool = isLive((Context)active);
        if(retBool){
            start_top(active, marketname, false);
        }
        else{
            start_top(active, marketname, true);
        }
    }

    public static void start_intent (Activity active, String marketname, boolean stg)
    {
        newTrackingHelper(active, marketname, stg);
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        String event = getFirstLaunch(m_aVersion);						// get Events
        event = event + "," + EVENT_START;
        m_s.setEvents(event);
        m_s.setAppState(m_packageName + SP_PAGE_NAME);
        m_s.track(contextData, null);
    }

    public static void start_intent (Activity active, String marketname)
    {
        boolean retBool = isLive((Context)active);
        if(retBool){
            start_intent(active, marketname, false);
        }
        else{
            start_intent(active, marketname, true);
        }
    }

    public static void start (Activity active, String marketname, boolean stg)
    {
        newTrackingHelper(active, marketname, stg);
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        m_s.setEvents(EVENT_CUSTOM);
        m_s.setAppState(m_packageName);
        m_s.trackLink("start()", "o", "start()", contextData, null);
    }

    public static void start (Activity active, String marketname)
    {
        boolean retBool = isLive((Context)active);
        if(retBool){
            start(active, marketname, false);
        }
        else{
            start(active, marketname, true);
        }
    }

    public static void shutdown ()
    {
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        checkAndSetPhoneState();
        checkAndSetIccID();
        m_s.setAppState(m_packageName);
        m_s.setEvents(EVENT_STOP);
        m_s.trackLink("shutdown()", "o", "shutdown()", null, null);
        if((m_activity != null) && (m_context == null)){
            //Case Activity Only
            m_s.stopActivity();
        }
    }

    public static void resume ()
    {
        if(m_appli1 == null){
            return;
        }
        if(m_context  == null){
            return;
        }
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        checkAndSetPhoneState();
        checkAndSetIccID();
        m_s.setAppState(m_packageName);
        m_s.setEvents(EVENT_RESUME);
        m_s.trackLink("resume()", "o", "resume()", null, null);
    }

    public static void logMessage (String sMsg, String sObjName)
    {
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        String message = sMsg;
        if(TextUtils.isEmpty(message) || message.equals("")){
            message = "logMessage()";
        }
        String object = sObjName;
        if(TextUtils.isEmpty(object) || object.equals("")){
            object = "logMessage()";
        }
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        setBasicVariable();
        m_s.setEvar(iCUSTOM_VALUE, Integer.toString (0));
        m_s.setProp(iCUSTOM_VALUE2, Integer.toString (0));
        m_s.setProp(iEVENT_NUM1, message);
        m_s.setEvar(iEVENT_NUM2, message);
        m_s.setProp(OBJECT_NAME1, object);
        m_s.setEvar(OBJECT_NAME2, object);
        m_s.setAppState(m_packageName);
        m_s.setEvents(EVENT_CUSTOM);
        m_s.trackLink("logMessage", "o", message, contextData, null);
    }

    public static void setExtProfile (String sProfile)
    {
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        String profile = sProfile;
        if(TextUtils.isEmpty(profile) || profile.equals("")){
            profile = "setExtProfile()";
        }
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        m_s.setEvar(iEXT_DATA, profile);
        m_s.setAppState(m_packageName);
        m_s.trackLink("setExtProfile", "o", profile, contextData, null);
    }

    public static void start_widget (Context context, String marketname, boolean stg)
    {
        if(context == null){
            return;
        }
        m_context = context;
        if(!marketname.equals("")){
            m_marketName = marketname;
        }
        newTrackingHelper(m_context, marketname, stg);
        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        contextData.put("key", "value");
        if(m_s == null){
            m_s = ADMS_Measurement.sharedInstance();
        }
        m_s.clearVars();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        String event = getFirstLaunch(m_aVersion);                      // get Events
        event = event + "," + EVENT_WIDGET;
        m_s.setEvents(event);
        m_s.setAppState(m_packageName);
        m_s.track(contextData, null);
    }

    public static void start_widget (Context context, String marketname){
        boolean retBool = isLive(context);
        if(retBool){
            start_widget(context, marketname, false);
        }
        else{
            start_widget(context, marketname, true);
        }
    }

    public static void set_keepAlive (Context context, boolean stg)
    {
        if(context == null){
            return;
        }
        if(m_context == null){
            m_context = context;
        }
        if(m_s == null){
            newTrackingHelper(context, "", false);
        }

        Hashtable<String, Object> contextData = new Hashtable<String, Object>();
        contextData.put("key", "value");
        m_s.clearVars();
        setBasicVariable();
        m_s.setProp(iOLD_PACKAGE_NAME1, m_className);
        m_s.setEvar(iOLD_PACKAGE_NAME2, m_className);
        m_s.setEvar(iCUSTOM_VALUE, "0");
        m_s.setProp(iCUSTOM_VALUE2, "0");
        m_s.setAppState(m_packageName);
        m_s.setEvents(EVENT_KEEPALIVE);
        m_s.track(contextData, null);
    }

    public static void set_keepAlive (Context context)
    {
        boolean retBool = isLive(context);
        if(retBool){
            set_keepAlive(context, false);
        }
        else{
            set_keepAlive(context, true);
        }
    }

    private static String getFirstLaunch (String version)
    {
        SharedPreferences pref = null;
        String ret = "";
        if(!(m_appli1 == null)){
            pref = m_appli1.getSharedPreferences("sc_setting", Context.MODE_PRIVATE);
        }
        else{
            if(!(m_context  == null)){
                pref = m_context.getSharedPreferences("sc_setting", Context.MODE_PRIVATE);
            }
            else{
                return "";
            }
        }
        Editor edit = pref.edit();
        String installed = pref.getString( "version", "0" );
        if( installed.equals("0")){
            edit.putString("version", version );
            ret = EVENT_INSTALL + "," + EVENT_PAGE_VIEW;
        }
        else if( installed.equals(version)){
            ret = EVENT_PAGE_VIEW;
        }
        else{
            edit.putString("version", version );
            ret = EVENT_PAGE_VIEW + "," + EVENT_VERUP;
        }
        edit.commit();
        return ret;
    }

    private static String getInstallDate ()
    {
        String ret = "";
        SharedPreferences pref = null;
        if(!(m_appli1 == null)){
            pref = m_appli1.getSharedPreferences("sc_setting", Context.MODE_PRIVATE);
        }
        else{
            if(!(m_context  == null)){
                pref = m_context.getSharedPreferences("sc_setting", Context.MODE_PRIVATE);
            }
            else{
                return "";
            }
        }
        Editor edit = pref.edit();
        String install = pref.getString("install", "");
        if( install.equals("")){
            Date dt = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
            ret = sdf.format(dt);
            edit.putString("install", ret );
            edit.commit();
        }
        else{
            ret = install;
        }
        return ret;
    }

    private static void setBasicVariable ()
    {
        if(m_appli1 == null){
            if(m_context == null){
                return;
            }
        }
        String flDate = getInstallDate();
        m_s.setProp(iOLD_APP_VER, m_aVersion);
        m_s.setProp(iOLD_OS_NAME, THIS_OS_NAME);
        m_s.setProp(iOLD_MODEL_NAME, m_modelName);
        m_s.setProp(iOLD_OS_VER, m_osVersion);
        checkAndSetPhoneState();
        checkAndSetIccID();
        m_s.setProp(iNEW_MARKET_NAME1, m_marketName);
        m_s.setEvar(iNEW_MARKET_NAME2, m_marketName);
        m_s.setEvar(iOLD_INSTALL_DATE, flDate);
        return;
    }

    private static boolean isLive (Context context)
    {
        String appName = "";
        String getVal = "";
        List<ApplicationInfo> applicationInfo = context.getPackageManager()
                                                       .getInstalledApplications(PackageManager.GET_META_DATA);
        for(ApplicationInfo info : applicationInfo){
            appName = info.packageName;
            if(appName.equals(PREVIEW_APP)){
                Context ctx;
                try{
                    ctx = context.createPackageContext(appName,0);
                    SharedPreferences pref = ctx.getSharedPreferences(PREF_NAME, Context.CONTEXT_INCLUDE_CODE);
                    getVal = pref.getString(CONNECT_NAME, "");
                    if(getVal.equals(CONNECT_LIVE)){
                        getVal = pref.getString(CNT_NAME, "");
                        if(getVal.equals(CNT_PUB)){
                            return true;
                        }
                        else{
                            return false;
                        }
                    }
                    else{
                        return false;
                    }
                }
                catch(NameNotFoundException e){
                    e.printStackTrace();
                    return true;
                }
            }
        }
//        Log.d("TrackingHelper", "isLive().ENDE = true ");
        return true;
    }

    private static void checkAndSetPhoneState(){
        Context cont = null;

        if(m_sIccId != null && m_sImei != null){
            if(!m_sIccId.equals("") && m_sImei.equals("")){
                m_s.setEvar(iOLD_USER_ID1, m_sIccId +"-"+ m_sImei); // ICCID-IMEI
                m_s.setProp(iOLD_USER_ID2, m_sIccId +"-"+ m_sImei); // ICCID-IMEI
                Log.d("checkAndSetPhoneState()", "Send Saved IccID");
                return;
            }
        }
        if(m_activity != null){
            cont = m_activity;
        }
        else if(m_context != null){
            cont = m_context;
        }
        else{
            return;
        }
        m_sImei = "";
        m_sIccId = "";
        int pmSetting = PermissionChecker.checkSelfPermission(cont, PERMISSION_PHONE);
        if(pmSetting == PermissionChecker.PERMISSION_GRANTED){
            TelephonyManager tm = (TelephonyManager)cont.getSystemService(Context.TELEPHONY_SERVICE);
            if(tm.SIM_STATE_READY == tm.getSimState()){
                m_sImei = tm.getDeviceId();
                m_sIccId = tm.getSimSerialNumber();
//                Log.d("TrackingHelper", "m_sIccId = " + m_sIccId);
            }
        }
    }

    private static void checkAndSetIccID(){
        if(m_sIccId != null && m_sImei != null){
            if(m_sIccId.equals("") && m_sImei.equals("")){
                m_s.setEvar(iOLD_USER_ID1, ""); //""
                m_s.setProp(iOLD_USER_ID2, ""); //""
            }
            else{
                m_s.setEvar(iOLD_USER_ID1, m_sIccId +"-"+ m_sImei); // ICCID-IMEI
                m_s.setProp(iOLD_USER_ID2, m_sIccId +"-"+ m_sImei); // ICCID-IMEI
            }
        }
    }
}
