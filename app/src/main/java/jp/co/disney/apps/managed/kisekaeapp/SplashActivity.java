package jp.co.disney.apps.managed.kisekaeapp;

import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.utils.Array;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadAllDataTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadDetailThumbSeparateTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadIconThumbSeparateTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.GetDetailSkinInfoTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SetFavoriteTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForCatalog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;
import jp.co.disney.apps.managed.kisekaeapp.catalog.screens.CatalogScreen;
import jp.co.disney.apps.managed.kisekaeapp.spp.AdBannerLayout;
import jp.co.disney.apps.managed.kisekaeapp.spp.BannerWeightComparator;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.spp.SppCheckUserStatusAsyncTask;

//public class SplashActivity extends AndroidApplication implements DownloadAllDataTaskCallback, GetDetailSkinInfoTaskCallback,
//																DownloadDetailThumbSeparateTaskCallback, DownloadSkinTaskCallback,
//																SetFavoriteTaskCallback {
//TODO　仮でActivity継承（SPP実装が完了したらAndroidApplicationでも可能な形に要変更
public class SplashActivity extends SppBaseActivity implements DownloadAllDataTaskCallback, GetDetailSkinInfoTaskCallback,
																DownloadDetailThumbSeparateTaskCallback, DownloadSkinTaskCallback,
																SetFavoriteTaskCallback, DownloadIconThumbSeparateTaskCallback,
																ActivityCompat.OnRequestPermissionsResultCallback{

	ApplicationListener launcherlistener;
//	ContentsOperatorForCatalog operator = null;

	public static final String AUTH_RESULT_PREF = "auth_result", AUTH_RESULT_CARRIER = "carrierId",
			AUTH_RESULT_USERPROFILE = "user_profile",
			AUTH_CARRIER_10APPS = "dcm", AUTH_CARRIER_OND = "ond", AUTH_CARRIER_ONS = "ons", AUTH_CARRIER_AU = "au", AUTH_CARRIER_CONPAS = "sbm";

//    private final float LOGO_SCALE = 0.66875f;
//    private final int BASE_STATUSBAR_H = 25;
//    private class CustomProgressDialog extends Dialog {
//        public CustomProgressDialog(Context context) {
//            super(context, R.style.Theme_CustomProgressDialog);
//            setContentView(R.layout.custom_progress_dialog);
//        }
//    }

    public static final String START_GETAUTHINFO_FROM_SPLASH = ".start_get_auth_info_from_splash";
	private final int REQUEST_CODE_AUTH_FROM_SPLASH = 2141;
	private BroadcastReceiver getauthReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			handler.post(runnableForGetAuthInfoFromSplash);
		}
	};

//    private CustomProgressDialog dialog;
//    private final int REQ_CODE_AUTH = 10;
//    private final int REQ_CODE_BROWSER = 2;
//    private final int GET_VERSION_INFO_CODE = 210;
//    private final int GET_AUTH_INFO_CODE = 214;
//    private final int GET_MEMBER_INFO_CODE = 216;
    private final int GET_AD_INFO_CODE = 202;
//    private boolean isEnd_BannerLoading = false;

    private Runnable runnableForGetAdInfo, runnableForGetAuthInfoFromSplash;
//    private final Handler handler = new Handler();

//    private void startSppGetVersionInfo(){
////		 此処でDisneyマーケットアプリの有無確認
//    	//Disney Market or Disney passが入ってるかチェック
//		if(!SPPUtility.isAppInstalled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
//
////			if(SPPUtility.getCarrierID(getApplicationContext()).equals(AUTH_CARRIER_AU)){
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
//			showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
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
//    }


    RelativeLayout layout = null;
	float dispScaleperBase=1f;//FHD(1080x1920)と比較しての倍率

    private BroadcastReceiver changeDbStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ContentsOperatorForCatalog.op.reflectCtoArrayFromDB();
		}
	};
	private boolean isDbChanged = false;


	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onRequestPermissionsResult_");

    	switch (requestCode) {
	        case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE:
	            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
	                // パーミッションが必要な処理
	                if (PermissionChecker.checkSelfPermission(
	                		SplashActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
	                        != PackageManager.PERMISSION_GRANTED) {
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "WRITE_EXTERNAL_STORAGE!= PackageManager.PERMISSION_GRANTED２");
		            	SplashActivity.this.finish();
	                }else{
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "WRITE_EXTERNAL_STORAGE== PackageManager.PERMISSION_GRANTED２");
	                }

	            }
	            break;
	        case MY_PERMISSIONS_REQUEST_READ_PHONE_STATE:
	            if (grantResults.length > 0 && grantResults[0] == PermissionChecker.PERMISSION_GRANTED) {
	                // パーミッションが必要な処理
	                if (PermissionChecker.checkSelfPermission(
	                		SplashActivity.this, Manifest.permission.READ_PHONE_STATE)
	                        != PackageManager.PERMISSION_GRANTED) {
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE!= PackageManager.PERMISSION_GRANTED２");
		            	SplashActivity.this.finish();
	                }else{
		            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "READ_PHONE_STATE== PackageManager.PERMISSION_GRANTED２");
	                }

	            }
	            break;
	    	}
	}

	@Override
	protected void onCreateGL() {
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();

		config.useAccelerometer = false;
		config.useCompass = false;
		config.disableAudio = true;
//		config.a =8;
//		config.r =8;
//		config.g =8;
//		config.b =8;
		launcherlistener = new CatalogScreen();
		((CatalogScreen)launcherlistener).setActivity(this);
		layout = new RelativeLayout(this);
		layout.setBackgroundColor(0xffe6e6e6);
//		layout.setBackgroundColor(0xff737373);
        layout.addView(initializeForView(launcherlistener, config));

        setContentView(layout);//onCreateAfterPermissionGranted()へ移動

        //一旦ロゴとプログレスの辺りコメントアウト
//        setContentView(R.layout.spp_splash);
//
//        ImageView logo = (ImageView)findViewById(R.id.logo);
//        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.auth10appslib_splash_logo);
//        Display disp = getWindowManager().getDefaultDisplay();
//        float density = getResources().getDisplayMetrics().density;
//        int statusBarHeight = (int)Math.ceil(BASE_STATUSBAR_H * density);
//        Matrix mtrx = new Matrix();
//        mtrx.preTranslate(-(bmp.getWidth() / 2), -(bmp.getHeight() / 2));
//        mtrx.postScale(LOGO_SCALE, LOGO_SCALE);
//        mtrx.postTranslate((disp.getWidth() / 2), ((disp.getHeight()-statusBarHeight) / 2));
//        logo.setImageMatrix(mtrx);
//        logo.invalidate();
//
//        dialog = new CustomProgressDialog(SplashActivity.this);
//        dialog.setCancelable(false);
//        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
//        wmlp.y = (int)(60 * density);
//        wmlp.x = wmlp.x + (int)(6 * density);
//        dialog.show();


/*		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;
		config.disableAudio = true;
//		config.a =8;
//		config.r =8;
//		config.g =8;
//		config.b =8;
		launcherlistener = new CatalogScreen();
		((CatalogScreen)launcherlistener).setActivity(this);
		RelativeLayout layout = new RelativeLayout(this);
		layout.setBackgroundColor(0xffe6e6e6);
        layout.addView(initializeForView(launcherlistener, config));
        setContentView(layout);
        *///TODO 仮で上記コメントアウト

//		((GLScreen) lancherlistener).SetHardViewWidth(getRealSize().x);

        IntentFilter ifilter = new IntentFilter(ContentsOperatorForCatalog.CHANGE_DB_STATE);
        registerReceiver(changeDbStateReceiver, ifilter);

        IntentFilter ifilter2 = new IntentFilter(getPackageName() + START_GETAUTHINFO_FROM_SPLASH);
        registerReceiver(getauthReceiver, ifilter2);

		// 画面サイズを取得
		int viewWidth = this.getResources().getDisplayMetrics().widthPixels;
		dispScaleperBase = (float)viewWidth/1080f;
		barHeight = getStatusBarHeight();

//        operator = new ContentsOperatorForCatalog(SplashActivity.this);
        ContentsOperatorForCatalog.op.SetContext(SplashActivity.this);
       	ContentsOperatorForCatalog.op.deleteDetailThumbsFolder();

	}

	@Override
	protected void onCreateAfterPermissionGranted() {
		((CatalogScreen)launcherlistener).setBG_GRAY();//OS6対応のパーミッションチェック時のダイアログの間のちらつきとSPP時の背景の色変化無し対応

		runnableForGetVersionInfo = new Runnable() {
			@Override
			public void run() {
//				startBaseActivityForResult(ParamCodeConsts.GetVersionInfo,
//						REQUEST_ID_GetVersionInfo,
//						SplashActivity.this.getPackageName());
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

		runnableForGetAuthInfoFromSplash = new Runnable() {

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
			            startActivityForResult(sppIntent, REQUEST_CODE_AUTH_FROM_SPLASH);
			        } catch (NameNotFoundException e) {}
				}

			}
		};

		runnableForGetAdInfo = new Runnable() {

			@Override
			public void run() {
   	    		//GetAdInfo
   				//初回起動時はバナーは表示しない
   				SharedPreferences prefs = getSharedPreferences(CatalogScreen.SCREEN_PREFERENCE_NAME, MODE_PRIVATE);
   				if(!prefs.getBoolean("finishTutrial", false)){
   					((CatalogScreen)launcherlistener).onFinishBannerLoading();
   					startDownloadAllData();
   				}else{
       	    		if(SPPUtility.checkNetwork(getApplicationContext())){
       	    			DebugLog.instance.outputLog("value", "GetAdInfo Start");

       	    			layout.setVisibility(View.GONE);

       	    	        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
       	    	        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
       	    	        sppIntent.setComponent(compo);

       	    	        ApplicationInfo appliInfo = null;
       	    	        try {
       	    	            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
       	    	            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
       	    	            sppIntent.putExtra("argFunc", 202);
       	    	            sppIntent.putExtra("argAreaCode", "15");
       	    	            startActivityForResult(sppIntent, GET_AD_INFO_CODE);
       	    	        } catch (NameNotFoundException e) {
       	    	        	((CatalogScreen)launcherlistener).onFinishBannerLoading();
       	    	        }

       	    		}else{
       	    			//バナー飛ばして、データ取得開始
       	    			((CatalogScreen)launcherlistener).onFinishBannerLoading();
       	    			startDownloadAllData();
       	    		}

   				}


			}
		};

        if(SPPUtility.isDebugFlag){
        	((CatalogScreen)launcherlistener).onFinishBannerLoading();
        	startDownloadAllData();
        }else{
        	handler.post(runnableForGetVersionInfo);//TODO 0614
//            sendAuthActivity();
        }

        try{
        	  Class.forName("android.os.AsyncTask");
        	 }catch(ClassNotFoundException e){}
	}


	@Override
	public void onPause () {
		super.onPause();
		doAppMeasurement(MeasurementTiming.ONPAUSE);

		if(isDbChanged) ContentsOperatorForCatalog.op.sendDBStateChangeBroadcast(getApplicationContext());
		isDbChanged = false;

       }

	@Override
	protected void onRestart() {
		super.onRestart();

        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "SplashActivity_onRestart");
		if (Build.VERSION.SDK_INT >= 23) {//OSバージョン
			//パーミッションチェック
			for (int i = 0; i < Permissions.length; i++) {//PermissionCheckがエラーの時は、ユーザーが非許可にした時で、その時は必ずOnCreateを通るので、PermissionCheckでエラーになる時のTrackingHelper.resume();は不要。

				if (PermissionChecker.checkSelfPermission(SplashActivity.this
						, Permissions[i])
				!= PermissionChecker.PERMISSION_GRANTED) {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "SplashActivity_onRestart_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
					handler.removeCallbacks(runnnableForPermission);
					PermissionCheckNG = true;
					onCreateGL();//チェックに行った時、背景がグレーになる事がある対応
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
							ActivityCompat.requestPermissions(SplashActivity.this,
									new String[] { Permissions[index] },
									PERMISSION_REQUEST);
						}
					};
					handler.post(runnnableForPermission);
					break;
				} else {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "SplashActivity_onRestart_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
				}
			}

			if (!PermissionCheckNG) {
				//TrackingHelper.resume();//
				doAppMeasurement(MeasurementTiming.ONRESTART);
			}

		} else {//OSバージョン
			//TrackingHelper.resume();//
			doAppMeasurement(MeasurementTiming.ONRESTART);
		}

//		TrackingHelper.resume();//OS6
	}



	@Override
	protected void onResume() {
		super.onResume();
		doAppMeasurement(MeasurementTiming.ONRESUME);
	}

	@Override
	protected void onDestroy() {
		//TrackingHelper.shutdown();
		doAppMeasurement(MeasurementTiming.ONDESTROY);
       	super.onDestroy();

       	DebugLog.instance.outputLog("value", "onDestroy!_______________");
       	ContentsOperatorForCatalog.op.cancelTask();
       	ContentsOperatorForCatalog.op.clearContentsData();
       	ContentsOperatorForCatalog.op.deleteDetailThumbsFolder();

       	handler.removeCallbacks(runnableForGetAuthInfoFromSplash);
       	handler.removeCallbacks(runnableForGetAdInfo);

       	try {
			unregisterReceiver(changeDbStateReceiver);
		} catch (Exception e) {
		}
       	try {
			unregisterReceiver(getauthReceiver);
		} catch (Exception e) {
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
    	DebugLog.instance.outputLog("value", "requestCode:" + requestCode + "/resultCode:" + resultCode);

//        if(requestCode == REQ_CODE_AUTH) {
//        	DebugLog.instance.outputLog("value", "resultCode:" + resultCode);
//        	SharedPreferences preferences = getSharedPreferences(AUTH_RESULT_PREF, Context.MODE_PRIVATE);
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
//                	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_10APPS);
//                	editor.commit();
//
//                	TrackingHelper.start_top(this, "", SPPUtility.isDebugFlag);
//
//                	((CatalogScreen)launcherlistener).onFinishBannerLoading();
//                	startDownloadAllData();
//                    break;
//                case LibConsts.RESULT_DMOD_DEV:
//                	DebugLog.instance.outputLog("value", "DisneyMobileOnDocomo");
//
//                	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_OND);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_DMOS_DEV:
//                	DebugLog.instance.outputLog("value", "DisneyMobileOnSoftBank");
//
//                	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_ONS);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_KDDI_DEV:
//                	DebugLog.instance.outputLog("value", "Kddi");
//
//                	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_AU);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_SOFTBANK_DEV:
//                	DebugLog.instance.outputLog("value", "SoftBankMobile");
//
//                	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_CONPAS);
//                	editor.commit();
//                	//通常SPP開始
//                	handler.post(runnableForGetVersionInfo);//startSppGetVersionInfo();
//                    break;
//                case LibConsts.RESULT_APP_FINISH:
//                	DebugLog.instance.outputLog("value", "stop app");
////                	showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
////                    this.finish();
//                	FinishApp();
//                    break;
//                default:
//                	DebugLog.instance.outputLog("value", "ERROR!");
//                	showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
////                    this.finish();
//                    break;
//                }
//            }else if(resultCode == RESULT_CANCELED) {
////            	SharedPreferences pref = getSharedPreferences("scheme10apps", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
////            	DebugLog.instance.outputLog("value", "cancel" + pref.getBoolean("schemeLaunch", false));
////
////				if (pref.getBoolean("schemeLaunch", false)) {
////					Editor editor = pref.edit();
////					editor.putBoolean("schemeLaunch", false);
////					editor.commit();
////
////					DebugLog.instance.outputLog("value", "10Appsサイトから再起動");
////	            	handler.post(runnnableFor10Apps);
////				} else {
////					Editor editor = pref.edit();
////					editor.putBoolean("schemeLaunch", false);
////					editor.commit();
////
////					exit();
////				}
//            }
//
//        }else if(requestCode == GET_VERSION_INFO_CODE){
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

        			DebugLog.instance.outputLog("value", "userStatus::::::::::::::" + userStatus);
        			DebugLog.instance.outputLog("value", "resultVersion::::::::::::::" + resultVersion);
           			if(Boolean.parseBoolean(userStatus) && resultVersion==0){


//           				TrackingHelper.start_top(this, "", SPPUtility.isDebugFlag);

//       					if(SPPUtility.getCarrierID(getApplicationContext()).equals(AUTH_CARRIER_CONPAS)
//       							|| SPPUtility.getCarrierID(getApplicationContext()).equals(AUTH_CARRIER_AU) ){
               				String userProfile = "";
               				userProfile = map.get("UserProfile");
               				if(!userProfile.equals("")){
                   				DebugLog.instance.outputLog("value2", "user_profile_" + userProfile);
//               					TrackingHelper.setExtProfile(userProfile);
                   		    	SharedPreferences preferences = getSharedPreferences(AUTH_RESULT_PREF, Context.MODE_PRIVATE);
                   		       	Editor editor = preferences.edit();
               		        	editor.putString(AUTH_RESULT_USERPROFILE, userProfile);
               		        	editor.commit();

//               				}
       					}


           				handler.post(runnableForGetAuthInfo);
            		}else{
            			DebugLog.instance.outputLog("value", "result:" + resultVersion );
            			if((!Boolean.parseBoolean(userStatus) && resultVersion == 0) || (!Boolean.parseBoolean(userStatus) && resultVersion == 1)){
            				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
            			}else{
            				exit();
            			}
            		}
        		}
        	}

//        }else if(requestCode == GET_MEMBER_INFO_CODE){
//        	//GetSPPMemberInfoでプレミアム情報が取れなかった場合でも、GetVersionInfoを超えている時点で会員ではあるので、このままGetAdInfoに飛ばす 20150723
//
//        	if(intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)){
//        		isPremium = false;
//        	}else{
//        		HashMap<String, String> map = parseResultXML(intent.getStringExtra("resultXML"), "GetSPPMemberInfo");
//        		if(map == null){
//        			//解析不正
//        			isPremium = false;
//        		}else{
//        			int result = Integer.parseInt(map.get("Result"));
//        			DebugLog.instance.outputLog("value", "result::::::::::::::" + result);
//
//        			if(result == 1){
//        				//異常終了→非プレミアム会員として続行
//        				isPremium = false;
//        			}else{
//            			String premium = map.get("IsPremium");
//
//            			DebugLog.instance.outputLog("value", "isPremium::::::::::::::" + isPremium);
//            			isPremium = Boolean.parseBoolean(premium);
//        			}
//
//        		}
//        	}
//
//    		//GetAdInfo
//    		if(SPPUtility.checkNetwork(getApplicationContext())){
//    	        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
//    	        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
//    	        sppIntent.setComponent(compo);
//
//    	        ApplicationInfo appliInfo = null;
//    	        try {
//    	            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
//    	            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
//    	            sppIntent.putExtra("argFunc", 202);
//    	            sppIntent.putExtra("argAreaCode", "51");
//    	            startActivityForResult(sppIntent, GET_AD_INFO_CODE);
//    	        } catch (NameNotFoundException e) {}
//
//    		}else{
//    		}
//
        }else if(requestCode == GET_AUTH_INFO_CODE || requestCode == REQUEST_CODE_AUTH_FROM_SPLASH){
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
//            	        	handler.post(runnableForGetAdInfo);//TODO

            	        	checkUserCarrier();
        				}else if(requestCode == REQUEST_CODE_AUTH_FROM_SPLASH){
        					((CatalogScreen)launcherlistener).restartSetTheme();
        				}
        			}else{
        				showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        			}

        		} catch (final Exception e) {
        			showDialogFragment(SPP_FAILED_REASON_AUTH_ERROR);
        		}

        	}

        }else if(requestCode == GET_AD_INFO_CODE){

        	layout.setVisibility(View.VISIBLE);
        	DebugLog.instance.outputLog("value", "GetAdInfoの戻り");
        	//バナー情報をget
        	//取得失敗してもアプリは終了させない→次の処理へ移行
        	if(intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)){
        		//ここでは何もしない
        		((CatalogScreen)launcherlistener).onFinishBannerLoading();
        	}else{
        		DebugLog.instance.outputLog("value", "GetAdInfoの戻り" + intent.getExtras().getString("resultXML"));
//        		HashMap<String, String> map = parseResultXML(intent.getStringExtra("resultXML"), "GetAdInfo");
        		bannerLayout = parseResultADXML( intent.getStringExtra("resultXML") );
        		if(bannerLayout != null){
        			if(bannerLayout.size > 0){
                		bannerLayout.sort(new BannerWeightComparator());
        				if(!bannerLayout.get(0).imageURL.equals("")){//画像URL取得成功
        					//それぞれのバナーの情報が帰ってきている筈。一枚も無かったらスルー（あと、複数枚ある想定なのでlayoutもそれにあわせておく。）
       						startDisplayBanner();
        				}else{
        					((CatalogScreen)launcherlistener).onFinishBannerLoading();
        				}
        			}else{
        				((CatalogScreen)launcherlistener).onFinishBannerLoading();
        			}
        		}else{
        			((CatalogScreen)launcherlistener).onFinishBannerLoading();
        		}
        	}


	        //データ取得開始
	        startDownloadAllData();

//        	if(dialog != null) dialog.dismiss();
        }
	}

	private final int gateAreaCode = 15;
	private final String AD_INFO_ITEM_TAG = "Item";
	private final String AD_INFO_BANNER_AREA_TAG = "AreaNo";
	private final String AD_INFO_BANNER_ID_TAG = "BannerID";
	private final String AD_INFO_WEIGHT_TAG = "Weight";
	private final String AD_INFO_IMAGE_TAG = "Image";
	private final String AD_INFO_LINK_TAG = "Link";
	private final String AD_INFO_BLOCKABLE_TAG = "Blockable";

	private void startDownloadAllData(){

		boolean isVisibleTutorial = ((CatalogScreen)launcherlistener).visibleTutrial();
        ContentsOperatorForCatalog.op.callDownloadAllDataTask(ContentsDetailTypeValue.CONTENTS_DETAIL_TYPE_DEFAULT);
        if(!isVisibleTutorial) ContentsOperatorForCatalog.op.startTimerForAllDataDownload(31 * 1000);//taskのdoinBackground開始とずれがある可能性があるので1秒余分に
	}

	private void setAdBlock() {

		/*
ユーザが「今後このお知らせを表示しない」のチェックを入れたら再表示させない
1回表示させたら、その日のうち(24時間以内)は再表示させない(1日1回だけ表示)
		 */


//		SharedPreferences pref = getSharedPreferences("GetAdInfoBlock", MODE_PRIVATE);

//		if (((CheckBox) findViewById(R.id.check_block)).isChecked()) {
//		for(AdBannerLayout banner : bannerLayout)
		if (!bannerLayout.get(0).blockCheckBox.isChecked()) {
			//24時間以内は再表示させない
			DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "非ブロック");
			SharedPreferences pref = getSharedPreferences(AD_BLOCK_SHARED_PREF, MODE_PRIVATE);
			SharedPreferences.Editor edit = pref.edit();
			edit.putInt(AD_BLOCK_BANNER_ID + bannerLayout.get(0).bannerId, LIMIT24H);
			edit.putLong(AD_BLOCK_LAST_DISPLAYDATE + bannerLayout.get(0).bannerId, System.currentTimeMillis());
			edit.commit();
		}else{
			//今後一切再表示させない
			DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "ブロック");
			SharedPreferences pref = getSharedPreferences(AD_BLOCK_SHARED_PREF, MODE_PRIVATE);
			SharedPreferences.Editor edit = pref.edit();
			edit.putInt(AD_BLOCK_BANNER_ID + bannerLayout.get(0).bannerId, ISBLOCK);
			edit.commit();

		}

	}


	private void removeBanner() {
		if(bannerLayout != null && bannerLayout.size > 0 ){
			//現在表示しているバナーを消して、arrayから消す、次のやつが表示対象だったら表示する
			bannerLayout.get(0).goneLayout();
			bannerLayout.removeIndex(0);
			if(bannerLayout.size > 0){
				bannerLayout.sort(new BannerWeightComparator());

				layout.addView(bannerLayout.get(0).adParent);
				bannerLayout.get(0).adParent.setVisibility(View.VISIBLE);
			}
		}
	}

	private void moveToAdPage() {
		//リンク先
		if(!bannerLayout.get(0).linkURL.equals("")){
			DebugLog.instance.outputLog("value", "linkURL_" + bannerLayout.get(0).linkURL);
			if(bannerLayout.get(0).linkURL.indexOf("dmarket://") != -1){
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(bannerLayout.get(0).linkURL));
				startActivity(intent);

			}else{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(bannerLayout.get(0).linkURL.replaceAll(" ", "")));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
		}else{
			return;
		}
		removeBanner();
	}


	private Array<AdBannerLayout> bannerLayout = null;
//	private SharedPreferences adPref = null;
	private final String AD_BLOCK_SHARED_PREF = "ad_banner_blockable", AD_BLOCK_BANNER_ID = "block_banner_id_", AD_BLOCK_LAST_DISPLAYDATE = "ad_last_display_date_";
	private final int ISBLOCK = -1, NOTBLOCK = 0, LIMIT24H = 1;
	private int barHeight = 50;

	public int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	private void startDisplayBanner(){

		//広告表示中の広告画像以外のタッチ抑止、再表示抑止不可の広告ではチェックボックスを非表示に

		if(bannerLayout.size <= 0) return;

		OnTouchListener bannerTouchListener = new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					switch (view.getId()) {
					case R.id.cancel_area:
						DebugLog.instance.outputLog("value", "banner tap cancel area!");
						setAdBlock();
						removeBanner();
						break;
					case R.id.ad_banner_image:
						DebugLog.instance.outputLog("value", "banner tap image!");
						setAdBlock();
						moveToAdPage();
						break;
					default:
						break;
					}
					break;
				}
				return true;
			}
		};

		LinearLayout.LayoutParams adParentMargin = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);

		RelativeLayout.LayoutParams adImageMargin = new RelativeLayout.LayoutParams((int)(980*dispScaleperBase), (int)(1326*dispScaleperBase));
		adImageMargin.setMargins((int)(50*dispScaleperBase), (int)((225*dispScaleperBase) - barHeight), 0, 0);

		RelativeLayout.LayoutParams cancelMargin = new RelativeLayout.LayoutParams((int)(152*dispScaleperBase), (int)(152*dispScaleperBase));
		cancelMargin.setMargins((int)(875*dispScaleperBase), (int)((225*dispScaleperBase) - barHeight), 0, 0);

		RelativeLayout.LayoutParams checkMargin = new RelativeLayout.LayoutParams((int)(952*dispScaleperBase), (int)(80*dispScaleperBase));
		checkMargin.setMargins((int)(70*dispScaleperBase), (int)((1590*dispScaleperBase) - barHeight), 0, 0);

		RelativeLayout.LayoutParams checkboxMargin = new RelativeLayout.LayoutParams((int)(74*dispScaleperBase), (int)(74*dispScaleperBase));
		checkboxMargin.setMargins(2, 2, 0, 0);

		RelativeLayout.LayoutParams checkmessageMargin = new RelativeLayout.LayoutParams((int)(727*dispScaleperBase), (int)(60*dispScaleperBase));
		checkmessageMargin.setMargins((int)((175-70)*dispScaleperBase), (int)((1597-1590)*dispScaleperBase), 0, 0);

		for(int i = 0; i < bannerLayout.size; i++){

			LayoutInflater inflater = getLayoutInflater();
//			bannerLayout[i] = new AdBannerLayout();
			bannerLayout.get(i).adParent = (LinearLayout)inflater.inflate(R.layout.ad_banner, null);
			bannerLayout.get(i).adParent.setLayoutParams(adParentMargin);

			// 広告表示中のタッチイベント浸透抑止
			bannerLayout.get(i).adParent.setOnTouchListener(bannerTouchListener);
			bannerLayout.get(i).checkAreaLayout = (RelativeLayout) ( bannerLayout.get(i).adParent.findViewById(R.id.check_area) );
			bannerLayout.get(i).cancelAreaView = (View) ( bannerLayout.get(i).adParent.findViewById(R.id.cancel_area) );
			bannerLayout.get(i).blockCheckBox = (CheckBox) ( bannerLayout.get(i).adParent.findViewById(R.id.check_block) );
			bannerLayout.get(i).bannerImageView = (ImageView) ( bannerLayout.get(i).adParent.findViewById(R.id.ad_banner_image) );
			bannerLayout.get(i).checkMessageView = (ImageView)( bannerLayout.get(i).adParent.findViewById(R.id.check_message) );
			//抑止可・不可にあわせて表示非表示を設定
			if (!bannerLayout.get(i).isBlockable) {
				DebugLog.instance.outputLog("value", "isBlockable_" + bannerLayout.get(i).isBlockable);
				bannerLayout.get(i).checkAreaLayout.setVisibility(View.INVISIBLE);
			}

			bannerLayout.get(i).bannerImageView.setOnTouchListener(bannerTouchListener);
			bannerLayout.get(i).cancelAreaView.setOnTouchListener(bannerTouchListener);
			bannerLayout.get(i).bannerImageView.setLayoutParams(adImageMargin);
			bannerLayout.get(i).cancelAreaView.setLayoutParams(cancelMargin);
			bannerLayout.get(i).checkAreaLayout.setLayoutParams(checkMargin);
			bannerLayout.get(i).blockCheckBox.setLayoutParams(checkboxMargin);
			bannerLayout.get(i).checkMessageView.setLayoutParams(checkmessageMargin);
		}

		//非表示のバナーがあったらArrayから削除
		SharedPreferences pref = getSharedPreferences(AD_BLOCK_SHARED_PREF, MODE_PRIVATE);
		for(int i = 0; i < bannerLayout.size; i++){
			int bannerState = pref.getInt(AD_BLOCK_BANNER_ID + bannerLayout.get(i).bannerId, NOTBLOCK);
			if(bannerState == LIMIT24H){
				DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "は24時間以内非表示");
				long blockLimitTime = pref.getLong(AD_BLOCK_LAST_DISPLAYDATE + bannerLayout.get(i).bannerId, 0) + 24 * 60 * 60 * 1000;
				if (blockLimitTime > System.currentTimeMillis()) {
					DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "前回表示してから24時間過ぎてないので非表示");
					//前回表示してから24時間過ぎてないので非表示
					bannerLayout.removeIndex(i);
					//インデックスをひとつ戻す
					i--;
				}
			}else if(bannerState == ISBLOCK){
				DebugLog.instance.outputLog("value", bannerLayout.get(0).bannerId + "ブロック設定されてたので非表示");
				bannerLayout.removeIndex(i);
				//インデックスをひとつ戻す
				i--;
			}
		}


//		if(!bannerLayout.get(i).imageURL.equals("")){
			try {
				AdImgDLTask adImgDLTask =new AdImgDLTask();
				adImgDLTask.execute(bannerLayout.size);
			} catch (Exception e) {
				DebugLog.instance.outputLog("value", "createBitmapDrawable_error_" + e.toString() );
				e.printStackTrace();
			}
//		}

	}

	private class AdImgDLTask extends AsyncTask<Integer, Void, Boolean> {//news画像取得
		 BitmapDrawable[] adBd = null;

	    /**
	     * バックグランドで行う処理
	     */
	    @Override
	    protected Boolean doInBackground(Integer... params) {
	    	DebugLog.instance.outputLog("value", "バナー数" + bannerLayout.size);

	    	if(params.length < 1){
	    		return false;
	    	}else{
	    		adBd = new BitmapDrawable[bannerLayout.size];
	    	}

	        try {
	        	for(int i = 0; i < bannerLayout.size; i++){
		    		DebugLog.instance.outputLog("value", "createBitmapDrawable_" +bannerLayout.get(i).imageURL);
		    		URL url = new URL(bannerLayout.get(i).imageURL);
		    		InputStream is = (InputStream) url.getContent();
		    		adBd[i] =  (BitmapDrawable)BitmapDrawable.createFromStream(is, "src" + i);

		    		if (adBd[i].getBitmap().getDensity() == Bitmap.DENSITY_NONE) {
		    			adBd[i].setTargetDensity(getResources().getDisplayMetrics());
//			    		adBd[i].setTargetDensity(getResources().getDisplayMetrics().densityDpi);
		    		}

	        	}

	        } catch (Exception e) {
	        	DebugLog.instance.outputLog("value", e.toString());
	        	return false;
	        }

	        return true;
	    }

	    /**
	     * バックグランド処理が完了
	     */
		@Override
		protected void onPostExecute(Boolean result) {

			if(result){
				for(int i = 0; i < bannerLayout.size; i++) bannerLayout.get(i).bannerImageView.setImageDrawable(adBd[i]);
			}

			//ここは一番上（weightで判断）のものだけadd
			if(bannerLayout != null && bannerLayout.size >= 1){
				layout.addView(bannerLayout.get(0).adParent);
				bannerLayout.get(0).adParent.setVisibility(View.VISIBLE);
			}

			((CatalogScreen)launcherlistener).onFinishBannerLoading();

			super.onPostExecute(result);
		}


	}

//	/**
//	 * SPP連携から返って来るXMLの解析用。
//	 * @param xml
//	 * @param startTag
//	 * @return
//	 */
	private Array<AdBannerLayout> parseResultADXML(String xml){
      //xml内部の&を特殊文字として置き換え
      xml = xml.replaceAll("&","&amp;");

      DebugLog.instance.outputLog("value", "response XML_startTag_" + xml);

      Array<AdBannerLayout> itemArray = new Array<AdBannerLayout>();

      int adNum = 0;
      boolean isItemTag = false;
      XmlPullParser xmlPullParser = Xml.newPullParser();
//      boolean targetTag = false;
      try {
          String name = "";
          xmlPullParser.setInput(new StringReader(xml));
          for(int eventType = xmlPullParser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xmlPullParser.next()){
              DebugLog.instance.outputLog("value", "adxml_eventType_" + eventType + "/" + xmlPullParser.getName());
              if(eventType == XmlPullParser.START_TAG){
//                  DebugLog.instance.outputLog("value", "response XML_if(eventType == XmlPullParser.START_TAG){_" + eventType+"_"+XmlPullParser.START_TAG);
                  name = xmlPullParser.getName();
                  if(name != null){

//                      if(startTag.equals(xmlPullParser.getName())){
//                          targetTag = true;
//                      }
                      if(name.equals(AD_INFO_ITEM_TAG)){
                    	  DebugLog.instance.outputLog("value", "_item start!!!!!!!!!!!!");
                          isItemTag = true;
                          itemArray.add(new AdBannerLayout());
                      }
//                      DebugLog.instance.outputLog("value", "response XML_xmlPullParser.getName()_startTag_targetTag_" + name +"_"+ AD_INFO_ITEM_TAG +"_"+ isItemTag);
                 }
              }
              //item tag
              else if(eventType == XmlPullParser.TEXT){//<Version><VersionUpComment>のところでエラーになる対応
//                  DebugLog.instance.outputLog("value", "response XML_else if(targetTag&& eventType == XmlPullParser.TEXT){_");
//                  DebugLog.instance.outputLog("value", "response XML_}else if(targetTag){_xmlPullParser.getName()_" + name);

                  String val = xmlPullParser.getText();

               	  DebugLog.instance.outputLog("value", "adxml_text_" + val);
               	  if(isItemTag){
                	 if(name.equals(AD_INFO_BANNER_AREA_TAG)){
                		 itemArray.get(itemArray.size - 1).area = Integer.parseInt(val);
                	 }else if(name.equals(AD_INFO_BANNER_ID_TAG)){
                    	 itemArray.get(itemArray.size - 1).bannerId = val;
                     }else if(name.equals(AD_INFO_WEIGHT_TAG)){
                    	 itemArray.get(itemArray.size - 1).weight = Integer.parseInt(val);
                     }else if(name.equals(AD_INFO_IMAGE_TAG)){
                    	 itemArray.get(itemArray.size - 1).imageURL = val;
//                     	DebugLog.instance.outputLog("value", (itemArray.size - 1) + "_imageURL" +"_"+ val);
                     }else if(name.equals(AD_INFO_LINK_TAG)){
                    	 itemArray.get(itemArray.size - 1).linkURL = val;
                     }else if(name.equals(AD_INFO_BLOCKABLE_TAG)){
                    	 itemArray.get(itemArray.size - 1).isBlockable = Boolean.parseBoolean(val);
                     }

                  }else{
                      if(name.equals("Result")){
                     	  if(!val.equals("0")){
                     		  return null;
                     	  }
                       }else if(name.equals("NumberOfResult")){
                     	  adNum = Integer.parseInt(val);
                       }
                  }

                  name = "";
              }
              //tes
              else if(eventType == XmlPullParser.END_TAG){
//                  DebugLog.instance.outputLog("value", "}else if(eventType == XmlPullParser.END_TAG){_");
//                  String name = xmlPullParser.getName();
                  name = xmlPullParser.getName();
//                  if(name != null){
//                      if(startTag.equals(xmlPullParser.getName())){
//                          targetTag = false;
//                      }
//                  }
                  if(name != null){
                      if(name.equals(AD_INFO_ITEM_TAG)){
                          isItemTag = false;
                      }
                  }
              }

          }
      } catch (Exception e) {
          DebugLog.instance.outputLog("value", "response XML_} catch (Exception e) {_" + e.toString());
          return null;
      }

      if(itemArray.size != adNum) return null;

      for(int i = 0; i < itemArray.size; i++){
    	  if(itemArray.get(i).area != gateAreaCode){
    		  itemArray.removeIndex(i);
    		  i--;
    	  }
      }
      return itemArray;
  }


	/////////////////////////////////
	////////起動時全データ（etc1のサムネ含む）取得taskのcallback
	/////////////////////////////////

	@Override
	public void onFailedAllDataDownload() {
		//失敗
		((CatalogScreen)launcherlistener).onFailedAllDataDownload();
	}
	@Override
	public void onFinishedAllDataDownload(Array<ContentsDataDto> ctoArray) {
		//完了
		DebugLog.instance.outputLog("value", "全データ取得完了");

		 ContentsOperatorForCatalog.op.SetContentsArray(ctoArray);

		 ((CatalogScreen)launcherlistener).onFinishedAllDataDownload();

	}


	/////////////////////////////////
	////////詳細情報取得taskのcallback
	/////////////////////////////////

	//情報取得失敗
	@Override
	public void onFailedDownloadDetailThumbsNetwork() {
		DebugLog.instance.outputLog("value", "詳細情報取得失敗");
		((CatalogScreen)launcherlistener).failedDetailInfo();
	}

	Array<ThumbInfo> detailInfoArray = null;
	//情報取得完了
	@Override
	public void onFinishedGetDetailInfo(Array<ThumbInfo> typeArray) {
		DebugLog.instance.outputLog("value", "詳細情報取得成功");


		//コンテンツ情報ログ表示
		if(typeArray == null){
			DebugLog.instance.outputLog("value", "詳細情報_null");
		}else{
			for(ThumbInfo info : typeArray){
				DebugLog.instance.outputLog("value", "詳細：アセットID：" + info.getCto().assetID + "/種類：" + info.getCto().contentsType);
			}

		}


		detailInfoArray = typeArray;
		((CatalogScreen)launcherlistener).DoneDetailInfo(typeArray);

	}

	/////////////////////////////////
	////////詳細サムネ取得taskのcallback
	/////////////////////////////////

	@Override
	public void onFailedDownloadDetailThumbsSeparate(ThumbInfo info) {
//		DebugLog.instance.outputLog("value", "詳細サムネ取得失敗");

		((CatalogScreen)launcherlistener).failedSaveDetailImage(info.getThumbnailIndex());
	}
	@Override
	public void onFailedDownloadDetailThumbsSeparateNetwork(ThumbInfo info) {
//		DebugLog.instance.outputLog("value", "詳細サムネ取得失敗ネットワークエラー");
		if(info == null){
			((CatalogScreen)launcherlistener).failedSaveDetailImage(0);
		}else{
			((CatalogScreen)launcherlistener).failedSaveDetailImage(info.getThumbnailIndex());
		}

	}
	@Override
	public void onFinishedDownloadDetailThumbsSeparate(ThumbInfo info) {
		DebugLog.instance.outputLog("value", "詳細サムネ取得成功or既存:" + info.getFileName());
		DebugLog.instance.outputLog("value", "_" + info.getThumbnailIndex());
		DebugLog.instance.outputLog("value", "_" + info.getFileName());

		((CatalogScreen)launcherlistener).DoneSaveDetailImage(info.getThumbnailIndex());

	}

	/////////////////////////////////
	////////スキンダウンロードtaskのcallback
	/////////////////////////////////

	@Override
	public void onFailedDownloadSkin(int reason, long assetId) {
		((CatalogScreen)launcherlistener).onFailedDownloadSkin(reason,assetId);
	}
	@Override
	public void onFinishedDownloadSkin(ContentsDataDto settedCto, long delAssetIdForHistory, long delAssetIdForMypage) {
		((CatalogScreen)launcherlistener).onFinishedDownloadSkin(settedCto.assetID);
		isDbChanged = true;
		ContentsOperatorForCatalog.op.callSkinChangeHomeApp(settedCto);
//		CallSkinChangeHomeApp(settedAssetId);
//		ContentsOperatorForCatalog.op.callSkinChangeHomeApp(settedAssetId);
	}
	/////////////////////////////////
	////////スキンお気に入り設定taskのcallback
	/////////////////////////////////

	@Override
	public void onFailedSetFavorite() {
		DebugLog.instance.outputLog("api", "favorete失敗(screen)!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		((CatalogScreen)launcherlistener).onFailedSetFavorite();
	}

	@Override
	public void onFinishedSetFavorite(long favoriteAssetId, long unfavoriteAssetIdAssetId) {
		DebugLog.instance.outputLog("api", "favorete成功(screen)!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		isDbChanged = true;
		((CatalogScreen)launcherlistener).onFinishedSetFavorite(favoriteAssetId);
	}
//	Handler mHandler = new Handler();
//	public void CallSkinChangeHomeApp(final long settedAssetId) {
//		  new Thread(new Runnable() {
//		    @Override
//			public void run() {
//		      handler.post(new Runnable() {
//		        @Override
//				public void run() {
//		        	ContentsOperatorForCatalog.op.callSkinChangeHomeApp(settedAssetId);
//		        }
//		      });
//		    }
//		  }).start();
//	}
	final int MY_HELP_ID = 220;
	public void CallHelpPage() {
		  new Thread(new Runnable() {
		    @Override
			public void run() {
		      handler.post(new Runnable() {
		        @Override
				public void run() {
//	                //ここから
//		               Intent sppIntent = new Intent(Intent.ACTION_MAIN);
//		               ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
//		               sppIntent.setComponent(compo);
//		               ApplicationInfo appliInfo = null;
//		               try {
//		                   appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
//		                   sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
//		                   sppIntent.putExtra("argFunc", 106);
//		                   startActivityForResult(sppIntent, MY_HELP_ID);
//		               } catch (NameNotFoundException e) {}
////		               //ここまで
		        	Uri uri = Uri.parse("http://ugc.disney.co.jp/blog/sphelp?app_code=012000533");
		        	Intent i = new Intent(Intent.ACTION_VIEW,uri);
		        	startActivity(i);
		        }
		      });
		    }
		  }).start();
	}

//	public void CallMemberInfoPage() {
//	          new Thread(new Runnable() {
//	            @Override
//	            public void run() {
//	              handler.post(new Runnable() {
//	                @Override
//	                public void run() {
//	                    callChangeMemberInfoActivity();
//	                }
//	              });
//	            }
//	          }).start();
//	    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			DebugLog.instance.outputLog("touched", "戻るキー");

//			displayAlertDialog(REASON_APP_FINISH);
			if(!((CatalogScreen) launcherlistener).BackToScreen()) showDialogFragment(0);

			return false;
//			return true;
		}else{
			return super.onKeyDown(keyCode, event);
		}
	}

//	public final static int SPP_FAILED_REASON_AUTH_ERROR = 1;
//	public final static int SPP_FAILED_REASON_NETWORK_ERROR = 2;
//	public final static int SPP_FAILED_REASON_BASE_APP_INVALID = 3;

	@Override
	public void onFailedDownloadIconThumbsSeparate(long assetId) {
		DebugLog.instance.outputLog("value", "アイコンサムネイル取得失敗");
		((CatalogScreen)launcherlistener).onFailedDownloadIconThumbsSeparateCatalog(assetId);
	}

	@Override
	public void onFailedDownloadIconThumbsSeparateNetwork(long assetId) {
		DebugLog.instance.outputLog("value", "アイコンサムネイル取得失敗_networkError");
		((CatalogScreen)launcherlistener).onFailedDownloadIconThumbsSeparateNetworkCatalog(assetId);
	}

	@Override
	public void onFinishedDownloadIconThumbsSeparate(long assetId) {
		DebugLog.instance.outputLog("value", "アイコンサムネイル取得成功");
		((CatalogScreen)launcherlistener).onFinishedDownloadIconThumbsSeparateCatalog(assetId);
	}

	@Override
	public void onFailedCheckUserStatus(int reason) {
		showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
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
			doAppMeasurement(MeasurementTiming.ONCREATE);
    		handler.post(runnableForGetAdInfo);
    	}else{
    		new SppCheckUserStatusAsyncTask(this).execute("");
    	}

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
        	//AppMeasurement4.x
        	doAppMeasurement(MeasurementTiming.ONCREATE);
        	//通常SPP開始
        	handler.post(runnableForGetAdInfo);
            break;
        case 3:
        	DebugLog.instance.outputLog("value", "DisneyMobileOnSoftBank");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_ONS);
        	editor.commit();
        	//AppMeasurement4.x
        	doAppMeasurement(MeasurementTiming.ONCREATE);
        	//通常SPP開始
        	handler.post(runnableForGetAdInfo);
            break;
        case 4:
        	DebugLog.instance.outputLog("value", "Kddi");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_AU);
        	editor.commit();
        	//AppMeasurement4.x
        	doAppMeasurement(MeasurementTiming.ONCREATE);
        	//通常SPP開始
        	handler.post(runnableForGetAdInfo);
            break;
        case 5:
        	DebugLog.instance.outputLog("value", "SoftBankMobile");

        	editor.putString(AUTH_RESULT_CARRIER, AUTH_CARRIER_CONPAS);
        	editor.commit();
        	//AppMeasurement4.x
        	doAppMeasurement(MeasurementTiming.ONCREATE);
        	//通常SPP開始
        	handler.post(runnableForGetAdInfo);
            break;
        }

	}


}
