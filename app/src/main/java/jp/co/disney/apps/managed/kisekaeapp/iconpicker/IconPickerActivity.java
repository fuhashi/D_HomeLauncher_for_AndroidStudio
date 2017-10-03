package jp.co.disney.apps.managed.kisekaeapp.iconpicker;


import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.SppBaseActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadAllDataTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadIconThumbSeparateTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SetFavoriteTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForIcon;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.launcher.InstallShortcutReceiver;
//import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.spp.SppCheckUserStatusAsyncTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.app.AlertDialog;
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

public class IconPickerActivity extends SppBaseActivity implements DownloadAllDataTaskCallback, SetFavoriteTaskCallback, 
																	DownloadIconThumbSeparateTaskCallback,
																	ActivityCompat.OnRequestPermissionsResultCallback{
//	ListView lv;

    private BroadcastReceiver changeDbStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ContentsOperatorForIcon.op.reflectCtoArrayFromDB();
		}
	};
    private boolean isDbChanged = false;

    private BroadcastReceiver finishReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			FinishApp();
		}
	};


//	//SPP認証失敗でエラーダイアログ表示
//	private void displaySppFailedAlertDialog(int reason){
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//
//		switch (reason) {
//		default:
//		case SplashActivity.SPP_FAILED_REASON_AUTH_ERROR:
//			builder.setMessage(R.string.invalid_app_fig2);
//			break;
//		case SplashActivity.SPP_FAILED_REASON_NETWORK_ERROR:
//			builder.setMessage(R.string.invalid_app_fig1);
//			break;
//		case SplashActivity.SPP_FAILED_REASON_BASE_APP_INVALID:
//			builder.setMessage(R.string.invalid_app_fig3);
//			break;
//		}
//
//		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				FinishApp();
//			}
//		});
//
//		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
//			@Override
//			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//				switch (keyCode) {
//		case KeyEvent.KEYCODE_BACK:
//			DebugLog.instance.outputLog("value", "back key pressed!");
//			FinishApp();
//		    return true;
//		default:
//		    return false;
//		}
//			}
//		});
//
//        // ダイアログの作成と描画
//		AlertDialog alert = builder.show();
//		TextView messageText = (TextView)alert.findViewById(android.R.id.message);
//        messageText.setGravity(Gravity.CENTER);
//
//	}


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    	switch (requestCode) {
	        case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
//            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onRequestPermissionsResult_MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_grantResults[0]_requestCode_" + grantResults[0] + "_"+ requestCode);
	            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
	            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {_");
	                // パーミッションが必要な処理
	                if (PermissionChecker.checkSelfPermission(
	                		this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
	                        != PackageManager.PERMISSION_GRANTED) {
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "WRITE_EXTERNAL_STORAGE!= PackageManager.PERMISSION_GRANTED２");
		            	this.finish();
	                }else{
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


    ApplicationListener launcherlistenerIcon;

    @Override
    protected void onCreateGL() {
    	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "private void onCreateGL() {");
		myIntent = getIntent();

//      requestWindowFeature(Window.FEATURE_NO_TITLE);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.disableAudio = true;
//		config.a =8;
//		config.r =8;
//		config.g =8;
//		config.b =8;
		launcherlistenerIcon = new IconPickerScreen();
		((IconPickerScreen)launcherlistenerIcon).setActivity(this);
		FrameLayout layout = new FrameLayout(this);
		layout.setBackgroundColor(0xffe6e6e6);
      layout.addView(initializeForView(launcherlistenerIcon, config));
      setContentView(layout);

//		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//   setContentView(R.layout.widgetbattery_pickersam);

      setScreen = myIntent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_SCREEN, -1);
		DebugLog.instance.outputLog("value2", "onCreate_" + myIntent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_ID, -1));


      IntentFilter ifilter = new IntentFilter(ContentsOperatorForWidget.CHANGE_DB_STATE);
      registerReceiver(changeDbStateReceiver, ifilter);
  	IntentFilter finishFilter = new IntentFilter(IconSelectListTypeActivity.FINISH_BROADCAST_ACTION);
  	registerReceiver(finishReceiver, finishFilter);


		//TODO　本来は起動時intentでどのウィジェットかを受け取ってcallDownloadAllDataTaskの第二引数に適切な値を渡す
		ContentsOperatorForIcon.op.SetContext(IconPickerActivity.this);


    }

    @Override
    protected void onCreateAfterPermissionGranted() {
    	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "private void onCreateAfterPermissionGranted {");

    	((IconPickerScreen)launcherlistenerIcon).setBG_GRAY();
    	
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

        if(SPPUtility.isDebugFlag){
            ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
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

       	ContentsOperatorForIcon.op.cancelTask();
       	ContentsOperatorForIcon.op.clearContentsData();

       	handler.removeCallbacks(runnnableForPermission);

       	try {
			unregisterReceiver(changeDbStateReceiver);
		} catch (Exception e) {
		}
       	try {
			unregisterReceiver(finishReceiver);
		} catch (Exception e) {
		}

	}

	@Override
	protected void onResume() {
		super.onResume();

		ContentsOperatorForIcon.op.SetContext(IconPickerActivity.this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
//        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onNewIntent1_AppWidgetId_i.getIntExtra(\"AppWidgetId\",-1)_" + AppWidgetId +"_"+intent.getIntExtra("AppWidgetId",-1));
//        AppWidgetId = intent.getIntExtra("AppWidgetId",-1);
//        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onNewIntent2_AppWidgetId_i.getIntExtra(\"AppWidgetId\",-1)_" + AppWidgetId +"_"+intent.getIntExtra("AppWidgetId",-1));

		DebugLog.instance.outputLog("value2", "onNewIntent_" + intent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_ID, -1));
		setScreen = intent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_SCREEN, -1);
       	myIntent = intent;
	}

	@Override
	protected void onPause() {
		super.onPause();

		if(isDbChanged) ContentsOperatorForIcon.op.sendDBStateChangeBroadcast(getApplicationContext());
		isDbChanged = false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

//        if(requestCode == REQ_CODE_AUTH) {
//        	DebugLog.instance.outputLog("value", "resultCode:" + resultCode);
//        	SharedPreferences preferences = getSharedPreferences(SplashActivity.AUTH_RESULT_PREF, Context.MODE_PRIVATE);
//
//            if (resultCode == RESULT_OK) {
//            	Editor editor = preferences.edit();
//                int r = intent.getIntExtra("RESULT", -1);
//                DebugLog.instance.outputLog("value", "VALUE:" + r);
//                switch (r) {
//                case LibConsts.RESULT_NOT_MEM:
//                	DebugLog.instance.outputLog("value", "Not Member");
//                    this.exit();
//                    break;
//                case LibConsts.RESULT_DOCOMO_DEV:
//                	DebugLog.instance.outputLog("value", "docomo Supoorted Device");
//
//                	editor.putString(SplashActivity.AUTH_RESULT_CARRIER, SplashActivity.AUTH_CARRIER_10APPS);
//                	editor.commit();
//
//                    ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
//
//                    break;
//                case LibConsts.RESULT_DMOD_DEV:
//                	DebugLog.instance.outputLog("value", "DisneyMobileOnDocomo");
//
//                	editor.putString(SplashActivity.AUTH_RESULT_CARRIER, SplashActivity.AUTH_CARRIER_OND);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_DMOS_DEV:
//                	DebugLog.instance.outputLog("value", "DisneyMobileOnSoftBank");
//
//                	editor.putString(SplashActivity.AUTH_RESULT_CARRIER, SplashActivity.AUTH_CARRIER_ONS);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_KDDI_DEV:
//                	DebugLog.instance.outputLog("value", "Kddi");
//
//                	editor.putString(SplashActivity.AUTH_RESULT_CARRIER, SplashActivity.AUTH_CARRIER_AU);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_SOFTBANK_DEV:
//                	DebugLog.instance.outputLog("value", "SoftBankMobile");
//
//                	editor.putString(SplashActivity.AUTH_RESULT_CARRIER, SplashActivity.AUTH_CARRIER_CONPAS);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_APP_FINISH:
//                	DebugLog.instance.outputLog("value", "stop app");
////                	displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
////                    this.finish();
//                	FinishApp();
//                    break;
//                default:
//                	DebugLog.instance.outputLog("value", "ERROR!");
//                	displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
////                    this.finish();
//                    break;
//                }
//            }else if(resultCode == RESULT_CANCELED) {
//            	DebugLog.instance.outputLog("value", "cancel");
////
//// 				displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
//            	SharedPreferences pref = getSharedPreferences("scheme10apps", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
//            	DebugLog.instance.outputLog("value", "cancel" + pref.getBoolean("schemeLaunch", false));
//
//				if (pref.getBoolean("schemeLaunch", false)) {
//					Editor editor = pref.edit();
//					editor.putBoolean("schemeLaunch", false);
//					editor.commit();
//
//					DebugLog.instance.outputLog("value", "10Appsサイトから再起動");
//	            	handler.post(runnnableFor10Apps);
//				} else {
//					Editor editor = pref.edit();
//					editor.putBoolean("schemeLaunch", false);
//					editor.commit();
//
//					exit();
//				}
//
//            }
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
//            			if((!Boolean.parseBoolean(userStatus) && resultVersion == 0) || resultVersion == 1){
//                			callAppFinishDialog();
            				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
            			}else{
            				exit();
            			}
            		}

        		}
        	}
        }else if(requestCode == GET_AUTH_INFO_CODE){
        	
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
//    			        ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
    					
    					checkUserCarrier();
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

		ContentsOperatorForIcon.op.SetContentsArray(ctoArray);
//		 ContentsOperatorForCatalog.op.SetContentsArray(ctoArray);

		 ((IconPickerScreen)launcherlistenerIcon).onFinishedAllDataDownload();
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

//	@Override
//	public void onFailedDownloadSkin(int reason, long assetId) {
//		((IconPickerScreen)launcherlistenerIcon).onFailedDownloadSkin();
//	}

//	@Override
//	public void onFinishedDownloadSkin(ContentsDataDto settedCto, long delAssetIdForHistory, long delAssetIdForMypage) {

//		isDbChanged = true;
//
//		if(AppWidgetId != 0){
//    	 Intent inte = new Intent(WidgetBattery.WIDGET_BATTERY_SKINCHANGE);//tes
//    	 inte.putExtra("appWidgetId", AppWidgetId);
//    	 inte.putExtra("assetID", settedCto.assetID);
//    	 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onItemClick2_AppWidgetId_members[position]_" + AppWidgetId +"_"+ settedCto.assetID);
//    	 PendingIntent contentIntent = PendingIntent.getBroadcast(IconPickerActivity.this,AppWidgetId,inte,PendingIntent.FLAG_ONE_SHOT);
//
//	     long now = System.currentTimeMillis();
//			AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
//			alarmManager.set(AlarmManager.RTC,now+10, contentIntent);
//
//			IconPickerActivity.this.finish();
//		}

//	}

	@Override
	public void onFailedAllDataDownload() {
		//失敗
		((IconPickerScreen)launcherlistenerIcon).onFailedAllDataDownload();
	}

	@Override
	public void onFailedSetFavorite() {
		// TODO 自動生成されたメソッド・スタブ
		((IconPickerScreen)launcherlistenerIcon).onFailedSetFavorite();
	}

	@Override
	public void onFinishedSetFavorite(long favoriteAssetId, long unfavoriteAssetIdAssetId) {
		isDbChanged = true;

		((IconPickerScreen)launcherlistenerIcon).onFinishedSetFavorite(favoriteAssetId);
	}
	Handler mHandler = new Handler();
	public void SetSkin(final long settedAssetId) {
		  new Thread(new Runnable() {
		    @Override
			public void run() {
		      mHandler.post(new Runnable() {
		        @Override
				public void run() {
//		    		if(AppWidgetId != 0){
//		    	    	 Intent inte = new Intent(WidgetBattery.WIDGET_BATTERY_SKINCHANGE);//tes
//		    	    	 inte.putExtra("appWidgetId", AppWidgetId);
//		    	    	 inte.putExtra("assetID", settedAssetId);
//		    	    	 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetPickerActivity_onItemClick2_AppWidgetId_members[position]_" + AppWidgetId +"_"+ settedAssetId);
//		    	    	 PendingIntent contentIntent = PendingIntent.getBroadcast(IconPickerActivity.this,AppWidgetId,inte,PendingIntent.FLAG_ONE_SHOT);
//
//		    		     long now = System.currentTimeMillis();
//		    				AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
//		    				alarmManager.set(AlarmManager.RTC,now+10, contentIntent);
//
//		    				IconPickerActivity.this.finish();
//		    			}
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

	@Override
	public void onFailedDownloadIconThumbsSeparate(long assetId) {
		//TODO 仮
		((IconPickerScreen)launcherlistenerIcon).onFailedDownloadSkin();

	}

	@Override
	public void onFailedDownloadIconThumbsSeparateNetwork(long asserId) {
		//TODO 仮
		((IconPickerScreen)launcherlistenerIcon).onFailedDownloadSkin();

	}

	Intent myIntent = null;
	int setScreen = -1;
	@Override
	public void onFinishedDownloadIconThumbsSeparate(long assetId) {

		//カタログ側でプログレスを止めて、	グリッド型かリスト型のどちらかを起動
		if(myIntent != null && setScreen != -1){//TODO 一旦この条件にする
			//グリッド型
			((IconPickerScreen)launcherlistenerIcon).onFinishedDownloadIconThumbsSeparate(false,getIntent());

		}else{
			//リスト型
			((IconPickerScreen)launcherlistenerIcon).onFinishedDownloadIconThumbsSeparate(true,null);
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
    		ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
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

        	ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
            break;
        case 3:
        	DebugLog.instance.outputLog("value", "DisneyMobileOnSoftBank");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_ONS);
        	editor.commit();

        	ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
            break;
        case 4:
        	DebugLog.instance.outputLog("value", "Kddi");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_AU);
        	editor.commit();

        	ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
            break;
        case 5:
        	DebugLog.instance.outputLog("value", "SoftBankMobile");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_CONPAS);
        	editor.commit();

        	ContentsOperatorForIcon.op.callDownloadAllDataTask(true, ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT);
            break;
        }
        
	}



}
