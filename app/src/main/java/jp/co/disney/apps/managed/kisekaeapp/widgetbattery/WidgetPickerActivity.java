package jp.co.disney.apps.managed.kisekaeapp.widgetbattery;


import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.SppBaseActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadAllDataTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SetFavoriteTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.spp.SppCheckUserStatusAsyncTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.Array;

public class WidgetPickerActivity extends SppBaseActivity implements DownloadAllDataTaskCallback, DownloadSkinTaskCallback,
																	SetFavoriteTaskCallback, 
																	ActivityCompat.OnRequestPermissionsResultCallback{

//	ListView lv;
    int AppWidgetId = 0;
    
    
    public static final String START_GETAUTHINFO_FROM_PICKER = ".start_get_auth_info_from_widgetpicker";
	private final int REQUEST_CODE_AUTH_FROM_PICKER = 2141;
//    private final int REQUEST_CODE_GET_AUTH_INFO = 214;
	private BroadcastReceiver getauthReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			handler.post(runnableForGetAuthInfoFromPicker);
		}
	};

    private BroadcastReceiver changeDbStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ContentsOperatorForWidget.op.reflectCtoArrayFromDB();
		}
	};
    private boolean isDbChanged = false;

    private Runnable runnableForGetAuthInfoFromPicker;

//    private void startSppGetVersionInfo(){
////		 此処でDisneyマーケットアプリの有無確認
//   	//Disney Market or Disney passが入ってるかチェック
//		if(!SPPUtility.isAppInstalled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
//
////			if(SPPUtility.getCarrierID(getApplicationContext()).equals(SplashActivity.AUTH_CARRIER_AU)){
//				//auマーケットが入っていたら
//				if(SPPUtility.isAppInstalled(getApplicationContext(), "com.kddi.market") && SPPUtility.isAppEnabled(getApplicationContext(), "com.kddi.market")){
//					//
//					Toast.makeText(this, "Disney passをインストールして下さい。", Toast.LENGTH_LONG).show();
//					Intent intent = new Intent(Intent.ACTION_VIEW,
//							Uri.parse("auonemkt://details?id=8588000000001"));
//					startActivity(intent);
//					exit();
//					return;
//
//				}
////			}
//
//			//ダウンロード開始用Activity
//	        Intent intent = new Intent();
//	        intent.setClassName(getPackageName(),"jp.co.disney.apps.managed.kisekaeapp.spp.BaseAppDownloadActivity");
//	        startActivity(intent);
//			exit();
//			return;
//			//ここで終了------------------------------------------------------------------------------------
//		}
//
//		if(!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
//			displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_BASE_APP_INVALID);
//		}else{
//			DebugLog.instance.outputLog("value", "GetVersionInfo　start");
//			//SPP認証	ここから
//	        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
//	        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
//	        sppIntent.setComponent(compo);
//
//	        ApplicationInfo appliInfo = null;
//	        try {
//	            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
//	            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
//	            sppIntent.putExtra("argFunc", 210);
//	            sppIntent.putExtra("argPackagename", getPackageName());
//	            sppIntent.putExtra("argRegistCheck", "0");
//	            sppIntent.putExtra("argMemberCheck", "1");
//	            sppIntent.putExtra("argAvailableCheck", "0");
//	            sppIntent.putExtra("argCarrierCheck", "0");
//	            sppIntent.putExtra("argUserProfile", "1");
//	            startActivityForResult(sppIntent, GET_VERSION_INFO_CODE);
//	        } catch (NameNotFoundException e) {}
//	        //	ここまで
//
//		}
//
//   }

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    	switch (requestCode) {
	        case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
	            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
	            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {_");
	                // パーミッションが必要な処理
	                if (PermissionChecker.checkSelfPermission(
	                		this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
	                        != PackageManager.PERMISSION_GRANTED) {
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "WRITE_EXTERNAL_STORAGE!= PackageManager.PERMISSION_GRANTED２");
		            	this.finish();
	                }else{
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "WRITE_EXTERNAL_STORAGE== PackageManager.PERMISSION_GRANTED２");
	                }

	            }
	            break;
	        case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE:
	            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
	                // パーミッションが必要な処理
	                if (PermissionChecker.checkSelfPermission(
	                		this, Manifest.permission.READ_PHONE_STATE)
	                        != PackageManager.PERMISSION_GRANTED) {
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE!= PackageManager.PERMISSION_GRANTED２");
		            	this.finish();
	                }else{
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE== PackageManager.PERMISSION_GRANTED２");
	                }

	            }
	            break;
	    	}
	}

//	private final String[] Permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
//	private final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 101;
//	private final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 102;
//	private boolean PermissionCheckNG = false;//
//	private Runnable runnnableForPermission;

    ApplicationListener launcherlistenerWidget;
    /** Called when the activity is first created. */

    protected void onCreateGL() {
//      requestWindowFeature(Window.FEATURE_NO_TITLE);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.disableAudio = true;
//		config.a =8;
//		config.r =8;
//		config.g =8;
//		config.b =8;
		launcherlistenerWidget = new WidgetBatteryScreen();
		((WidgetBatteryScreen)launcherlistenerWidget).setActivity(this);
		FrameLayout layout = new FrameLayout(this);
		layout.setBackgroundColor(0xffe6e6e6);
      layout.addView(initializeForView(launcherlistenerWidget, config));
      setContentView(layout);

//		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//   setContentView(R.layout.widgetbattery_pickersam);

      Intent i =getIntent();
      AppWidgetId = i.getIntExtra("AppWidgetId",-1);

      //OS6_PermissionCheck対応
      SharedPreferences mPref = getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
      int onRestartAppWidgetId = mPref.getInt("onRestartAppWidgetId", 0);
      if(onRestartAppWidgetId!=0)AppWidgetId = onRestartAppWidgetId;
      jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onCreateGL_AppWidgetId_onRestartAppWidgetId_" + AppWidgetId +"_"+ onRestartAppWidgetId);//taihi

      IntentFilter ifilter = new IntentFilter(ContentsOperatorForWidget.CHANGE_DB_STATE);
      registerReceiver(changeDbStateReceiver, ifilter);
      
      IntentFilter ifilter2 = new IntentFilter(getPackageName() + START_GETAUTHINFO_FROM_PICKER);
      registerReceiver(getauthReceiver, ifilter2);


		//TODO　本来は起動時intentでどのウィジェットかを受け取ってcallDownloadAllDataTaskの第二引数に適切な値を渡す
		ContentsOperatorForWidget.op.SetContext(WidgetPickerActivity.this);

    }

    protected void onCreateAfterPermissionGranted() {
    	((WidgetBatteryScreen)launcherlistenerWidget).setBG_GRAY();
    	
//        runnnableFor10Apps = new Runnable() {
//
//  			@Override
//  			public void run() {
//  				sendAuthActivity();
//  			}
//  		};

  		runnableForGetVersionInfo = new Runnable() {
  			@Override
  			public void run() {
  				startSppGetVersionInfo();
  			}
  		};

		runnableForGetAuthInfo = new Runnable() {
			
			@Override
			public void run() {
				
				if(!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
					showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
				}else{
					DebugLog.instance.outputLog("value", "GetAuthInfo　start");
					
			        //GetAuthInfo
			        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
			        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
			        sppIntent.setComponent(compo);

			        ApplicationInfo appliInfo = null;
			        try {
			            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
			            sppIntent.putExtra("argFunc", 214);
			            startActivityForResult(sppIntent, GET_AUTH_INFO_CODE);
			        } catch (NameNotFoundException e) {}
				}

			}
		};

		runnableForGetAuthInfoFromPicker = new Runnable() {
			
			@Override
			public void run() {
				
				if(!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
					showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
				}else{
					DebugLog.instance.outputLog("value", "GetAuthInfo　start");
					
			        //GetAuthInfo
			        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
			        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
			        sppIntent.setComponent(compo);

			        ApplicationInfo appliInfo = null;
			        try {
			            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
			            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
			            sppIntent.putExtra("argFunc", 214);
			            startActivityForResult(sppIntent, REQUEST_CODE_AUTH_FROM_PICKER);
			        } catch (NameNotFoundException e) {}
				}

			}
		};



        if(SPPUtility.isDebugFlag){
            ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
        }else{
        	handler.post(runnableForGetVersionInfo);
//            sendAuthActivity();
        }

//        ContentsOperatorForCatalog.op.SetContext(WidgetPickerActivity.this);
//        ContentsOperatorForCatalog.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.CONTENTS_DETAIL_TYPE_DEFAULT);

        try{
      	  Class.forName("android.os.AsyncTask");
      	 }catch(ClassNotFoundException e){}
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();

       	ContentsOperatorForWidget.op.cancelTask();
       	ContentsOperatorForWidget.op.clearContentsData();

       	handler.removeCallbacks(runnableForGetAuthInfoFromPicker);

       	handler.removeCallbacks(runnnableForPermission);

       	try {
			unregisterReceiver(changeDbStateReceiver);
		} catch (Exception e) {
		}
       	try {
			unregisterReceiver(getauthReceiver);
		} catch (Exception e) {
		}

        SharedPreferences mPref = getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        Editor e = mPref.edit();
        e.putInt("onRestartAppWidgetId", 0);
        e.commit();

	}

	@Override
	protected void onRestart() {
		super.onRestart();
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onRestart");

        if (Build.VERSION.SDK_INT >= 23) {//OSバージョン
			//パーミッションチェック
			for (int i = 0; i < Permissions.length; i++) {

				if (PermissionChecker.checkSelfPermission(this
						, Permissions[i])
				!= PermissionChecker.PERMISSION_GRANTED) {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
					handler.removeCallbacks(runnnableForPermission);
					PermissionCheckNG = true;
					// パーミッションをリクエストする
					int MY_PERMISSION_REQUEST = 0;
					if (Permissions[i].equals(Permissions[0])) {
						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
					} else if (Permissions[i].equals(Permissions[1])) {
						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;
					}

					//パーミッションリクエスト
					final int index = i;
					final int PERMISSION_REQUEST = MY_PERMISSION_REQUEST;
					runnnableForPermission = new Runnable() {

						@Override public void run() {
							ActivityCompat.requestPermissions(WidgetPickerActivity.this,
									new String[] { Permissions[index] },
									PERMISSION_REQUEST);
						}
					};
					handler.post(runnnableForPermission);
					break;

				} else {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
				}

				if (!PermissionCheckNG) {
				}
			}
		} else {//OSバージョン
		}

        SharedPreferences mPref = getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        Editor e = mPref.edit();
        e.putInt("onRestartAppWidgetId", AppWidgetId);
        e.commit();
	}


	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	protected void onNewIntent(Intent intent) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onNewIntent1_AppWidgetId_i.getIntExtra(\"AppWidgetId\",-1)_" + AppWidgetId +"_"+intent.getIntExtra("AppWidgetId",-1));
        AppWidgetId = intent.getIntExtra("AppWidgetId",-1);
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onNewIntent2_AppWidgetId_i.getIntExtra(\"AppWidgetId\",-1)_" + AppWidgetId +"_"+intent.getIntExtra("AppWidgetId",-1));
	}

	@Override
	protected void onPause() {
		super.onPause();

		if(isDbChanged) ContentsOperatorForWidget.op.sendDBStateChangeBroadcast(getApplicationContext());
		isDbChanged = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode == GET_VERSION_INFO_CODE){
        	if(intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)){
//        		callAppFinishDialog();
        		showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        	}else{
        		HashMap<String, String> map = parseResultXML(intent.getStringExtra("resultXML"), "GetVersionInfo");
        		if(map == null){
        			//解析不正
//        			callAppFinishDialog();
        			showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        		}else{

        			//Errorがあったら終了
        			Iterator entries = map.entrySet().iterator();
        			while(entries.hasNext()) {
        				Map.Entry entry = (Map.Entry)entries.next();
        				String keyName = (String)entry.getKey();
        				DebugLog.instance.outputLog("value", "key:" + keyName);
        				if(keyName.equals("Error")){
        					exit();
        					return;
        				}
        			}

        			String userStatus = map.get("Status");
        			if(userStatus == null) userStatus = "false";
          			//エラー対策
        			int resultVersion;
        			if(map.get("Result") == null || map.get("Result").equals("")) resultVersion = 100;
        			else resultVersion = Integer.parseInt(map.get("Result"));

        			DebugLog.instance.outputLog("value", "result::::::::::::::" + userStatus);
        			DebugLog.instance.outputLog("value", "resultVersion::::::::::::::" + resultVersion);
           			if(Boolean.parseBoolean(userStatus) && resultVersion==0){

           				handler.post(runnableForGetAuthInfo);

            		}else{
            			DebugLog.instance.outputLog("value", "result:" + resultVersion );
            			if((!Boolean.parseBoolean(userStatus) && resultVersion == 0) || (!Boolean.parseBoolean(userStatus) && resultVersion == 1)){
//                			callAppFinishDialog();
            				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
            			}else{
            				exit();
            			}
            		}

        		}
        	}

        }else if(requestCode == GET_AUTH_INFO_CODE || requestCode == REQUEST_CODE_AUTH_FROM_PICKER){
        	if(intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)){
        		showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        		
        	}else{
            	
        		String result = intent.getExtras().getString("resultXML");
        		if (TextUtils.isEmpty(result)) {
        			showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        		}
        		try {
        			final XmlPullParserFactory factory = XmlPullParserFactory
        					.newInstance();
        			factory.setNamespaceAware(true);
        			final XmlPullParser parser = factory.newPullParser();
        			
        			String accessToken = "", token = "";

        			parser.setInput(new StringReader(result));
        			int eventType = parser.getEventType();
        			while (eventType != XmlPullParser.END_DOCUMENT) {
        				if (eventType == XmlPullParser.START_TAG) {
        					final String tag = parser.getName().toLowerCase();
        					if ("result".equals(tag)) {
        						if (!"0".equals(parser.nextText().toLowerCase())) {
        							showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        						}
        					} else if ("accesstoken".equals(tag)) {
        						accessToken = parser.nextText();
        					} else if ("idtoken".equals(tag)) {
        						token = parser.nextText();
        					}
        				} else if (eventType == XmlPullParser.END_TAG) {

        				}
        				eventType = parser.next();
        			}
        			
        			if(!token.equals("") && !accessToken.equals("")){
        				//prefへの保存処理
        				SharedPreferences preferences = getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        				SharedPreferences.Editor e = preferences.edit();
        				Date date = new Date(System.currentTimeMillis());
        				//現在時刻
        				long nowTimeLong = date.getTime() - (2 * 60 * 1000);
        				e.putLong(SPPUtility.TOKEN_GET_LASTTIME, nowTimeLong);
        				
        				e.putString(SPPUtility.PREF_KEY_ID_TOKEN, token);
        				e.putString(SPPUtility.PREF_KEY_ACCESS_TOKEN, accessToken);
        				e.commit();
        				
        				if(requestCode == GET_AUTH_INFO_CODE){
        		        	//成功
        					DebugLog.instance.outputLog("value", "get auth info succses");
//        				    ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
        				    
        				    checkUserCarrier();
        				}else if(requestCode == REQUEST_CODE_AUTH_FROM_PICKER){
        					DebugLog.instance.outputLog("value", "get auth info succses from pick");
        					((WidgetBatteryScreen)launcherlistenerWidget).restartSetTheme();
        				}
        			}else{
        				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        			}

        		} catch (final Exception e) {
        			showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        		}

        	}

        }

	}

	@Override
	public void onFinishedAllDataDownload(final Array<ContentsDataDto> ctoArray) {
		//完了
		DebugLog.instance.outputLog("value", "全データ取得完了");

		ContentsOperatorForWidget.op.SetContentsArray(ctoArray);
//		 ContentsOperatorForCatalog.op.SetContentsArray(ctoArray);

		 ((WidgetBatteryScreen)launcherlistenerWidget).onFinishedAllDataDownload();
/*
		if(ctoArray != null){
			ContentsOperatorForWidget.op.SetContentsArray(ctoArray);
			String[] members = new String[ctoArray.size];


			int i =0;
			for(ContentsDataDto cto : ctoArray){
				DebugLog.instance.outputLog("value", "取得したアセットID：" + cto.assetID);
				DebugLog.instance.outputLog("value", "上記アセットIDのデータの実体があるかないか：" + cto.isExist);
				members[i]=String.valueOf(cto.assetID);
				i+=1;
			}


	        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
	                android.R.layout.simple_expandable_list_item_1, members);

	        lv.setAdapter(adapter);
	        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
	            @Override
	            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	                //ここに処理を書く
	            	 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onItemClick_AppWidgetId_members[position]_" + AppWidgetId +"_"+  ctoArray.get(position).assetID);
	            	ContentsOperatorForWidget.op.callDownloadSkinTask(ctoArray.get(position));
	            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onItemClick_AppWidgetId_members[position]_" + AppWidgetId +"_"+  ctoArray.get(position).assetID);

	            }
	        });
		}
		*/
	}

	@Override
	public void onFailedDownloadSkin(int reason, long assetId) {
		((WidgetBatteryScreen)launcherlistenerWidget).onFailedDownloadSkin();
	}

	@Override
	public void onFinishedDownloadSkin(ContentsDataDto settedCto, long delAssetIdForHistory, long delAssetIdForMypage) {

		isDbChanged = true;

		if(AppWidgetId != 0){
    	 Intent inte = new Intent(WidgetBattery.WIDGET_BATTERY_SKINCHANGE);//tes
    	 inte.putExtra("appWidgetId", AppWidgetId);
    	 inte.putExtra("assetID", settedCto.assetID);
    	 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onItemClick2_AppWidgetId_members[position]_" + AppWidgetId +"_"+ settedCto.assetID);
    	 PendingIntent contentIntent = PendingIntent.getBroadcast(WidgetPickerActivity.this,AppWidgetId,inte,PendingIntent.FLAG_ONE_SHOT);

	     long now = System.currentTimeMillis();
			AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC,now+10, contentIntent);

			WidgetPickerActivity.this.finish();
		}

	}

	@Override
	public void onFailedAllDataDownload() {
		//失敗
		((WidgetBatteryScreen)launcherlistenerWidget).onFailedAllDataDownload();
	}

	@Override
	public void onFailedSetFavorite() {
		// TODO 自動生成されたメソッド・スタブ
		((WidgetBatteryScreen)launcherlistenerWidget).onFailedSetFavorite();
	}

	@Override
	public void onFinishedSetFavorite(long favoriteAssetId, long unfavoriteAssetIdAssetId) {
		isDbChanged = true;

		((WidgetBatteryScreen)launcherlistenerWidget).onFinishedSetFavorite(favoriteAssetId);
	}
	Handler mHandler = new Handler();
	public void SetSkin(final long settedAssetId) {
		  new Thread(new Runnable() {
		    @Override
			public void run() {
		      mHandler.post(new Runnable() {
		        @Override
				public void run() {
		    		if(AppWidgetId != 0){
		    	    	 Intent inte = new Intent(WidgetBattery.WIDGET_BATTERY_SKINCHANGE);//tes
		    	    	 inte.putExtra("appWidgetId", AppWidgetId);
		    	    	 inte.putExtra("assetID", settedAssetId);
		    	    	 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onItemClick2_AppWidgetId_members[position]_" + AppWidgetId +"_"+ settedAssetId);
		    	    	 PendingIntent contentIntent = PendingIntent.getBroadcast(WidgetPickerActivity.this,AppWidgetId,inte,PendingIntent.FLAG_ONE_SHOT);

		    		     long now = System.currentTimeMillis();
		    				AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
		    				alarmManager.set(AlarmManager.RTC,now+10, contentIntent);

		    				WidgetPickerActivity.this.finish();
		    			}
		        }
		      });
		    }
		  }).start();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			DebugLog.instance.outputLog("touched", "戻るキー");
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("アプリを終了しますか？");
			builder.setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					FinishApp();
				}
			});
	        // ダイアログの作成と描画
//			builder.show();
			AlertDialog alert = builder.show();
			TextView messageText = (TextView)alert.findViewById(android.R.id.message);
	        messageText.setGravity(Gravity.CENTER);
//	        messageText.setPadding(10, 10, 10, 10);
//	        messageText.setTextColor(Color.RED);

			return false;
//			return true;
		}else{
			return super.onKeyDown(keyCode, event);
		}
	}

	private void checkUserCarrier(){
    	//一度10appsでチェック済みだったら、KDDI,softbank,onS,onDの端末ならば10appsを飛ばす
    	SharedPreferences preferences = getSharedPreferences(AUTH_RESULT_PREF, Context.MODE_PRIVATE);
    	String carrierId = preferences.getString(AUTH_RESULT_CARRIER, "");
    	if(carrierId.equals(AUTH_CARRIER_OND)
    			|| carrierId.equals(AUTH_CARRIER_ONS)
    			|| carrierId.equals(AUTH_CARRIER_AU)
    			|| carrierId.equals(AUTH_CARRIER_CONPAS)){
    		//COR-021によるチェック不要
    		ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
    	}else{
    		new SppCheckUserStatusAsyncTask(this).execute("");
    	}

	}
	@Override
	public void onFailedCheckUserStatus(int reason) {
		showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
	}

	@Override
	public void onFinishedCheckUserStatus(int businessDomain) {
		//きせかえで必要なユーザの課金キャリアをチェック
		
    	SharedPreferences preferences = getSharedPreferences(AUTH_RESULT_PREF, Context.MODE_PRIVATE);
       	Editor editor = preferences.edit();
		
        switch (businessDomain) {
        default:
        case 0:
        	DebugLog.instance.outputLog("value", "Not Member");
        	showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
            break;
        case 2:
        	DebugLog.instance.outputLog("value", "DisneyMobileOnDocomo");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_OND);
        	editor.commit();

        	ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
            break;
        case 3:
        	DebugLog.instance.outputLog("value", "DisneyMobileOnSoftBank");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_ONS);
        	editor.commit();

        	ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
            break;
        case 4:
        	DebugLog.instance.outputLog("value", "Kddi");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_AU);
        	editor.commit();

        	ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
            break;
        case 5:
        	DebugLog.instance.outputLog("value", "SoftBankMobile");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_CONPAS);
        	editor.commit();

        	ContentsOperatorForWidget.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
            break;
        }
        
	}


}