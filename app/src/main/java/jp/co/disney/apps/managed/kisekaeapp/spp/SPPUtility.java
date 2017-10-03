package jp.co.disney.apps.managed.kisekaeapp.spp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.AuthGetActivity;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.TelephonyManager;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class SPPUtility {
    public static final boolean isDebugFlag = false;//TODO　最終的にはfalse;
    public static final boolean isAuthThroughFlag = false;

    public static final int CARRIER_CAMOUFLAGE_10APPS = 1, CARRIER_CAMOUFLAGE_OND = 2, CARRIER_CAMOUFLAGE_ONS = 3,
            CARRIER_CAMOUFLAGE_CONPAK = 4, CARRIER_CAMOUFLAGE_CONPAS = 5;
    public static final int CARRIER_CAMOUFLAGE_SET = 0;

    //デバッグ時キャリア擬装用
    public static String getCamoflaICCID(){
    	DebugLog.instance.outputLog("value", "-------------getCamoflaICCID---------------");
    	switch (CARRIER_CAMOUFLAGE_SET) {
		default:
		case CARRIER_CAMOUFLAGE_OND:
			return "8981100023549868945";
		case CARRIER_CAMOUFLAGE_10APPS:
			return "8981100022320193648";
		case CARRIER_CAMOUFLAGE_ONS:
			return "8981200090720742727";
		case CARRIER_CAMOUFLAGE_CONPAK:
			return "8981300022570981310";
		case CARRIER_CAMOUFLAGE_CONPAS:
			return "8981200515150351615";

		}
    }

    //デバッグ時キャリア擬装用
    public static String getCamoflaDeviceId(){
    	switch (CARRIER_CAMOUFLAGE_SET) {
		default:
		case CARRIER_CAMOUFLAGE_OND:
			return "DM-01G";
		case CARRIER_CAMOUFLAGE_10APPS:
			return "SC-06D";
		case CARRIER_CAMOUFLAGE_ONS:
			return "DM013SH";
		case CARRIER_CAMOUFLAGE_CONPAK:
			return "SOL26";
		case CARRIER_CAMOUFLAGE_CONPAS:
			return "402SO";

		}
    }

    //デバッグ時キャリア擬装用
    public static String getCamoflaOsVersion(){
    	switch (CARRIER_CAMOUFLAGE_SET) {
		default:
		case CARRIER_CAMOUFLAGE_OND:
			return "Android+5.0";
		case CARRIER_CAMOUFLAGE_10APPS:
			return "Android+4.1";
		case CARRIER_CAMOUFLAGE_ONS:
			return "Android+4.0";
		case CARRIER_CAMOUFLAGE_CONPAK:
			return "Android+4.4";
		case CARRIER_CAMOUFLAGE_CONPAS:
			return "Android+5.0";

		}
    }

    //デバッグ時キャリア擬装用
    public static String getCamoflaCarrierId(){
    	switch (CARRIER_CAMOUFLAGE_SET) {
		default:
		case CARRIER_CAMOUFLAGE_OND:
			return SplashActivity.AUTH_CARRIER_OND;
		case CARRIER_CAMOUFLAGE_10APPS:
			return SplashActivity.AUTH_CARRIER_10APPS;
		case CARRIER_CAMOUFLAGE_ONS:
			return SplashActivity.AUTH_CARRIER_ONS;
		case CARRIER_CAMOUFLAGE_CONPAK:
			return SplashActivity.AUTH_CARRIER_AU;
		case CARRIER_CAMOUFLAGE_CONPAS:
			return SplashActivity.AUTH_CARRIER_CONPAS;

		}
    }


    public static String getCarrierID(Context con){
        SharedPreferences pref = con.getSharedPreferences(SplashActivity.AUTH_RESULT_PREF, Context.MODE_PRIVATE);
        return pref.getString(SplashActivity.AUTH_RESULT_CARRIER, "");
    }

    public static String getAppId(Context con){
        String appId = "";
        ApplicationInfo appInfo = null;
        try {
            appInfo = con.getPackageManager().getApplicationInfo(con.getPackageName(), PackageManager.GET_META_DATA);
            appId = appInfo.metaData.getString("AppId").substring(3);
        } catch (Exception e) {}
        return appId;
    }

    public static String getAppName(Context con) {
        return con.getResources().getText(R.string.application_name).toString();
    }

    public static boolean isDocomoDevice(Context con){
        SharedPreferences preferences = con.getSharedPreferences(SplashActivity.AUTH_RESULT_PREF, Context.MODE_PRIVATE);
        String carrierId = preferences.getString(SplashActivity.AUTH_RESULT_CARRIER, "");

        if(carrierId.equals(SplashActivity.AUTH_CARRIER_10APPS)) return true;

        return false;
    }

    public static boolean isAppEnabled(Context con, String packageName){
        int state = con.getPackageManager().getApplicationEnabledSetting(packageName);

        if(state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT || state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED){
            return true;
        }else{
            return false;
        }
    }

    /**
     * packageNameに指定したパッケージのアプリのインストールの有無を取得（true：インストール済み）
     * @param con
     * @param packageName
     * @return
     */
    public static boolean isAppInstalled (Context con, String packageName) {
        PackageManager pm = con.getPackageManager();
        try {
            @SuppressWarnings("unused")
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
            return true;
        }
        catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 接続環境の取得
     */
    public static String[] getConnectEnv(Context con){
        String[] result = new String[]{"liv", "pub"};

        if(isAppInstalled(con, "jp.co.disney.apps.dmd.dmarketpreview")){
            try {
                Context mContext = con.createPackageContext("jp.co.disney.apps.dmd.dmarketpreview", Context.CONTEXT_RESTRICTED);
                SharedPreferences prefs = mContext.getSharedPreferences("market_pref", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
                result[0] = prefs.getString("CONNECT_ENV", "liv");
                result[1] = prefs.getString("CNT_ENV", "pub");
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    /**
     * 仮想日付関連情報の取得
     * @param con
     * @return
     */
    public static String getVirtualDateString(Context con){

        if(isAppInstalled(con, "jp.co.disney.apps.dmd.dmarketpreview")){
            try {
                Context mContext = con.createPackageContext("jp.co.disney.apps.dmd.dmarketpreview", Context.CONTEXT_RESTRICTED);
                SharedPreferences prefs = mContext.getSharedPreferences("market_pref", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
                return prefs.getString("VIRTUAL_DATETIME", "");
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }

    	return "";
    }

    /**
     * 仮想日付関連情報の取得
     * @param con
     * @return
     */
    public static Date getVirtualDate(String virtualDate){

        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
			return df.parse(virtualDate);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
    }


    /**
     * 仮想日付関連情報の取得
     * @param con
     * @return
     */
    public static boolean getVirtualDateOn(Context con){

        if(isAppInstalled(con, "jp.co.disney.apps.dmd.dmarketpreview")){
            try {
                Context mContext = con.createPackageContext("jp.co.disney.apps.dmd.dmarketpreview", Context.CONTEXT_RESTRICTED);
                SharedPreferences prefs = mContext.getSharedPreferences("market_pref", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
                return prefs.getBoolean("VIRTUAL_DATETIME_FLG", false);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }

    	return false;
    }

    /**
     * postのbody部に入れるjsonデータ作成
     * http://dangoya.jp/?p=169
     */
    public static String createDeviceIdentifier(final Context context){
        try {
            JSONObject jsonObj = new JSONObject();
            final JSONObject inObj = new JSONObject();

            DebugLog.instance.outputLog("value", "createDeviceIdentifier:");

            //ICCID
            //http://blog.livedoor.jp/hiroki0907/archives/51735248.html
            TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String id = manager.getSimSerialNumber();
            inObj.put("iccid", id);

            //SUID
            inObj.put("suid", null);

            //model code
            inObj.put("device_model_code", encodeParameter(Build.MODEL));

            //user agent
            if(Build.VERSION.SDK_INT >= 17){
                inObj.put("user_agent", WebSettings.getDefaultUserAgent(context));
            }else{
                Looper.prepare();
                Handler handler = new Handler();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        String user_agent = new WebView(context.getApplicationContext()).getSettings().getUserAgentString();
                        try {
                            inObj.put("user_agent", user_agent);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                };
                handler.post(runnable);
            }

            //au IDトークン
            inObj.put("auid_token", null);

            jsonObj.put("device_identifier", inObj);

            DebugLog.instance.outputLog("value", "createDeviceIdentifier:return:" + jsonObj.toString());

            return jsonObj.toString();

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String encodeParameter(String p){
        try {
            return URLEncoder.encode(p, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }


    /**
     * online check
     */
    public static boolean checkNetwork(Context myContext){
        ConnectivityManager cm = (ConnectivityManager) myContext.getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if (nInfo == null) return false;
        if (nInfo.isConnected()) return true; /* NetWork接続可 */
        return false;
    }

    /**
     * CNTから返った日付の文字列をDateオブジェクトに変換してreturn
     * @param dateString
     * @return
     */
    public static Date getDateCNTFormat(String dateString){
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy'-'MM'-'dd'T'kk':'mm':'ss");
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date();
        }
    }

    //COR-205関連
	public static final String GET_TOKEN_PREF = "get_token";
	public static final String TOKEN_GET_LASTTIME = "get_token_lasttime";
	public static final String PREF_KEY_ID_TOKEN = "get_token_id_token";
	public static final String PREF_KEY_ACCESS_TOKEN = "get_token_access_token";

    public static final String INTENT_EXTRA_ASSET_ID = "asset_id";

	//トークンの取得が必要かどうか確認
	public static boolean isNeedToken(Context c){
		//前回取得時から24時間経っていたら取得が必要

		Date date = new Date(System.currentTimeMillis());
		//現在時刻
		long nowTimeLong = date.getTime();//TODO
		//SharedPrefrencesから前回取得時刻を取得
		SharedPreferences preferences = c.getSharedPreferences(GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
		long lastTimeLong = preferences.getLong(TOKEN_GET_LASTTIME, 0);
		DebugLog.instance.outputLog("value", "前回TOKEN取得時間_" + new Date(lastTimeLong).toString());

		if(lastTimeLong == 0L){
			return true;
		}else{
			//24h以上経っていたら(5分のバッファ)
			if((nowTimeLong - lastTimeLong) >= (24 * 60 * 60 * 1000 - 5 * 60 * 1000)){
				return true;
			}
		}

		return false;
	}

    public static String parseResponseForToken(String fileID, String response) throws JSONException{
        JSONObject rootObj = new JSONObject(response);

        JSONObject rootListObj = rootObj.getJSONObject("download_token_list");
        JSONArray tokenlistArray = rootListObj.getJSONArray("download_token");

        for(int i = 0; i < tokenlistArray.length(); i++){
        	JSONObject tokenObj = tokenlistArray.getJSONObject(i);
        	if(tokenObj.getString("file_id").equals(fileID)){
        		return  tokenObj.getString("download_url");
        	}
        }
        return "";
    }

	public static void callAuthGetActivity(Context c, String assetID){
		DebugLog.instance.outputLog("value", "callAuthGetActivity==========");
		//token取得用のActivity呼び出し
        Intent intent = new Intent(c.getApplicationContext(), AuthGetActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.putExtra(INTENT_EXTRA_ASSET_ID, assetID);

//        intent.setClassName(getApplicationContext().getPackageName(), getApplicationContext().getPackageName() + ".AuthGetActivity");

        c.startActivity(intent);

	}



}

