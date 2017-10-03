package jp.co.disney.apps.managed.kisekaeapp;

import java.io.StringReader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;

import com.adobe.mobile.Analytics;
import com.adobe.mobile.Config;
import com.adobe.mobile.CustomAndroidSystemObject;
import com.badlogic.gdx.backends.android.AndroidApplication;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Xml;
import android.view.KeyEvent;
import android.widget.Toast;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.spp.SppCheckUserStatusTaskCallback;

public class SppBaseActivity extends AndroidApplication implements SppCheckUserStatusTaskCallback {

	protected final int GET_VERSION_INFO_CODE = 210;
	protected final int GET_AUTH_INFO_CODE = 214;

	protected Runnable runnableForGetVersionInfo, runnableForGetAuthInfo;
    protected final Handler handler = new Handler();

	protected final String AUTH_RESULT_PREF = "auth_result", AUTH_RESULT_CARRIER = "carrierId",
			AUTH_RESULT_USERPROFILE = "user_profile",
			AUTH_CARRIER_10APPS = "dcm", AUTH_CARRIER_OND = "ond", AUTH_CARRIER_ONS = "ons", AUTH_CARRIER_AU = "au", AUTH_CARRIER_CONPAS = "sbm";

    protected void startSppGetVersionInfo(){
    	//Disney Market or Disney passが入ってるかチェック
		if(!SPPUtility.isAppInstalled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){

//			if(SPPUtility.getCarrierID(getApplicationContext()).equals(AUTH_CARRIER_AU)){
				//auマーケットが入っていたら
				if(SPPUtility.isAppInstalled(getApplicationContext(), "com.kddi.market") && SPPUtility.isAppEnabled(getApplicationContext(), "com.kddi.market")){
					//
					Toast.makeText(this, "Disney passをインストールして下さい。", Toast.LENGTH_LONG).show();
					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse("auonemkt://details?id=8588000000001"));
					startActivity(intent);
					exit();
					return;

				}
//			}

			//ダウンロード開始用Activity
	        Intent intent = new Intent();
	        intent.setClassName(getPackageName(),"jp.co.disney.apps.managed.kisekaeapp.spp.BaseAppDownloadActivity");
	        startActivity(intent);
			exit();
			return;
			//ここで終了------------------------------------------------------------------------------------
		}

		if(!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
			showDialogFragment(SPP_FAILED_REASON_BASE_APP_INVALID);
		}else{
			DebugLog.instance.outputLog("value", "GetVersionInfo　start");
			//SPP認証	ここから
	        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
	        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
	        sppIntent.setComponent(compo);

	        ApplicationInfo appliInfo = null;
	        try {
	            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
	            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
	            sppIntent.putExtra("argFunc", 210);
	            sppIntent.putExtra("argPackagename", getPackageName());
	            sppIntent.putExtra("argRegistCheck", "0");
	            sppIntent.putExtra("argMemberCheck", "1");
	            sppIntent.putExtra("argAvailableCheck", "0");
	            sppIntent.putExtra("argCarrierCheck", "0");
	            sppIntent.putExtra("argUserProfile", "1");
	            startActivityForResult(sppIntent, GET_VERSION_INFO_CODE);
	        } catch (NameNotFoundException e) {}
	        //	ここまで

		}

    }


    protected final String[] Permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
	protected final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 101;
	protected final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 102;
	protected boolean PermissionCheckNG = false;//
	protected Runnable runnnableForPermission;

	protected void onCreateGL(){}
	protected void onCreateAfterPermissionGranted(){}

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= 23) {//OSバージョン
			//パーミッションチェック
			for (int i = 0; i < Permissions.length; i++) {

				if (PermissionChecker.checkSelfPermission(this
						, Permissions[i])
				!= PermissionChecker.PERMISSION_GRANTED) {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
					PermissionCheckNG = true;
					onCreateGL();//チェックに行っても落ちない対応
					// パーミッションをリクエストする
					int MY_PERMISSION_REQUEST = 0;
					if (Permissions[i].equals(Permissions[0])) {
						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE;
					}
					else if (Permissions[i].equals(Permissions[1])) {
						MY_PERMISSION_REQUEST = MY_PERMISSIONS_REQUEST_READ_PHONE_STATE;
					}

					//パーミッションリクエスト
					final int index = i;
					final int PERMISSION_REQUEST = MY_PERMISSION_REQUEST;
					runnnableForPermission = new Runnable() {
						@Override public void run() {
							ActivityCompat.requestPermissions(SppBaseActivity.this,
									new String[] { Permissions[index] },
									PERMISSION_REQUEST);
						}
					};
					handler.post(runnnableForPermission);
					break;

					//      				    @Override
					//      				    public void run() {
					//      				            ActivityCompat.requestPermissions(SplashActivity.this,
					//      				            new String[]{Permissions[index]},
					//      				          PERMISSION_REQUEST);
					//      				    }
					//      				};
					//      				handler.post(runnnableForPermission);
					//      				break;
				} else {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "onCreate_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
				}
			}

			if (!PermissionCheckNG) {
				onCreateGL();
				onCreateAfterPermissionGranted();
			}

		} else {//OSバージョン
			onCreateGL();
			onCreateAfterPermissionGranted();
		}

    }


	@Override
	protected void onDestroy() {
       	super.onDestroy();

       	handler.removeCallbacks(runnableForGetVersionInfo);
       	handler.removeCallbacks(runnableForGetAuthInfo);

	}

//	public static final String AUTH_RESULT_PREF = "auth_result", AUTH_RESULT_CARRIER = "carrierId",
//			AUTH_CARRIER_10APPS = "dcm", AUTH_CARRIER_OND = "ond", AUTH_CARRIER_ONS = "ons", AUTH_CARRIER_AU = "au", AUTH_CARRIER_CONPAS = "sbm";



	/**
	 * SPP連携から返って来るXMLの解析用。
	 * @param xml
	 * @param startTag
	 * @return
	 */
	protected HashMap<String, String> parseResultXML(String xml, String startTag){
      DebugLog.instance.outputLog("value", "response XML:" + xml);
      DebugLog.instance.outputLog("value", "response XML_startTag_" + xml +"_"+ startTag);
      HashMap<String, String> map = new HashMap<String, String>();
      XmlPullParser xmlPullParser = Xml.newPullParser();
      boolean targetTag = false;
      try {
          String name = "";
          xmlPullParser.setInput(new StringReader(xml));
          for(int eventType = xmlPullParser.getEventType(); eventType != XmlPullParser.END_DOCUMENT; eventType = xmlPullParser.next()){
              DebugLog.instance.outputLog("value", "xmlPullParser_eventType_" + eventType);
              if(eventType == XmlPullParser.START_TAG){
                  DebugLog.instance.outputLog("value", "response XML_if(eventType == XmlPullParser.START_TAG){_" + eventType+"_"+XmlPullParser.START_TAG);
//                  String name = xmlPullParser.getName();
                  name = xmlPullParser.getName();
                  if(name != null){
                      DebugLog.instance.outputLog("value", "response XML_xmlPullParser.getName()_startTag_targetTag_" + name +"_"+ startTag +"_"+targetTag);

                      if(startTag.equals(xmlPullParser.getName())){
                          targetTag = true;
//                      }else if(targetTag){
////                          if(name.equalsIgnoreCase("Result") || name.equalsIgnoreCase("AccessToken") || name.equalsIgnoreCase("IdToken")){
//                                  String val = xmlPullParser.nextText();//<Version><VersionUpComment>のところでエラーになる対応

//                                  map.put(name, val);
////                          }
                      }
                  }
              }
              //tes
              else if(targetTag&& eventType == XmlPullParser.TEXT){//<Version><VersionUpComment>のところでエラーになる対応
                  DebugLog.instance.outputLog("value", "response XML_else if(targetTag&& eventType == XmlPullParser.TEXT){_");
                      DebugLog.instance.outputLog("value", "response XML_}else if(targetTag){_xmlPullParser.getName()_" + name);
                      String val = xmlPullParser.getText();
                      map.put(name, val);
                      DebugLog.instance.outputLog("value", "response XML_map.put(name, val);_name_val_" + name +"_"+val);
                      name = "";
              }
              //tes
              else if(eventType == XmlPullParser.END_TAG){
                  DebugLog.instance.outputLog("value", "}else if(eventType == XmlPullParser.END_TAG){_");
//                  String name = xmlPullParser.getName();
                  name = xmlPullParser.getName();
                  if(name != null){
                      if(startTag.equals(xmlPullParser.getName())){
                          targetTag = false;
                      }
                  }
              }

          }
      } catch (Exception e) {
          DebugLog.instance.outputLog("value", "response XML_} catch (Exception e) {_");
          return null;
      }
      return map;
  }

	public void CallToast(final String message) {
		  new Thread(new Runnable() {
		    @Override
			public void run() {
		      handler.post(new Runnable() {
		        @Override
				public void run() {
		            // 第3引数は、表示期間（LENGTH_SHORT、または、LENGTH_LONG）
		            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
		        }
		      });
		    }
		  }).start();
	}
	public void FinishApp() {
		  new Thread(new Runnable() {
		    @Override
			public void run() {
		      handler.post(new Runnable() {
		        @Override
				public void run() {
//		        	finish();
		        	exit();
		        }
		      });
		    }
		  }).start();
	}


	protected void showDialogFragment(int reason){

		Bundle args = new Bundle();
		args.putInt("finish_reason", reason);

		FragmentManager fm = getFragmentManager();
        MainFragmentDialog dialog = new MainFragmentDialog();
        dialog.setArguments(args);
        if(reason != 0) dialog.setCancelable(false);
        dialog.show(fm, String.valueOf(reason));
	}

	protected class MainFragmentDialog extends DialogFragment {

		 @Override
	        public Dialog onCreateDialog(Bundle savedInstanceState) {
				Bundle args = getArguments();
				if( args == null ) return null;

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setView(null);

				int reason = args.getInt("finish_reason", 0);
				if(reason == 0){
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

				}else{
					switch (reason) {
					default:
					case SPP_FAILED_REASON_AUTH_ERROR:
						builder.setMessage(R.string.invalid_app_fig2);
						break;
					case SPP_FAILED_REASON_NETWORK_ERROR:
						builder.setMessage(R.string.invalid_app_fig1);
						break;
					case SPP_FAILED_REASON_BASE_APP_INVALID:
						builder.setMessage(R.string.invalid_app_fig3);
						break;
					}

					builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							FinishApp();
						}
					});

					builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
						@Override
						public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
							switch (keyCode) {
					case KeyEvent.KEYCODE_BACK:
						DebugLog.instance.outputLog("value", "back key pressed!");
						FinishApp();
					    return true;
					default:
					    return false;
					}
						}
					});

				}

				AlertDialog dialog = builder.create();
//				TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
//		        if(messageText != null) messageText.setGravity(Gravity.CENTER);

		        if(reason != 0) dialog.setCancelable(false);

				return dialog;
		 }
	 }

	public final static int SPP_FAILED_REASON_AUTH_ERROR = 1;
	public final static int SPP_FAILED_REASON_NETWORK_ERROR = 2;
	public final static int SPP_FAILED_REASON_BASE_APP_INVALID = 3;


	@Override
	public void onFailedCheckUserStatus(int reason) {
		showDialogFragment(SPP_FAILED_REASON_NETWORK_ERROR);
	}

	@Override
	public void onFinishedCheckUserStatus(int businessDomain) {
	}

	enum MeasurementTiming {
		ONCREATE,
		ONRESTART,
		ONDESTROY,
		ONRESUME,
		ONPAUSE
	}

	boolean isCreateZumi = false;

	protected void doAppMeasurement( MeasurementTiming timing ){
		//AppMeasurement4.xの対応をTrackingHelperに代わって実装

		if(timing != MeasurementTiming.ONCREATE){
			if(!isCreateZumi) return;
		}

		if(timing == MeasurementTiming.ONPAUSE){
			Config.pauseCollectingLifecycleData();
			return;
		}

		// ContextData 変数用のHashmap を用意
		HashMap cdata = new HashMap<String, Object>();

		SharedPreferences preferences = getSharedPreferences(AUTH_RESULT_PREF, Context.MODE_PRIVATE);
		String userProfile = preferences.getString(AUTH_RESULT_USERPROFILE, "");
		String carrierId = preferences.getString(AUTH_RESULT_CARRIER, "");
		DebugLog.instance.outputLog("value", "userProfile=" + userProfile);
//		Toast.makeText(getApplicationContext(), "userProfile set : " + userProfile, Toast.LENGTH_SHORT).show();

		if(!carrierId.equals("")){
			String market ="";
			if(carrierId.equals(AUTH_CARRIER_OND)){
				market = "PinkMarket";
			}else if(carrierId.equals(AUTH_CARRIER_ONS)){
				market = "BlackMarket";
			}else if(carrierId.equals(AUTH_CARRIER_AU)){
				market = "DisneyPass";
			}else if(carrierId.equals(AUTH_CARRIER_CONPAS)){
				market = "SBMMarket";
			}
			cdata.put("market", market);
		}

		if(timing == MeasurementTiming.ONCREATE){
			// SDK がapplication context にアクセスできるよう設定する
			Config.setContext(this.getApplicationContext());
			isCreateZumi = true;

			if(!userProfile.equals("")){
				cdata.put("SPPInformation", userProfile);
			}

		}else if(timing == MeasurementTiming.ONRESUME){
			Config.collectLifecycleData();

			if(!userProfile.equals("")){
				cdata.put("SPPInformation", userProfile);
			}

			Analytics.trackAction("screenView",cdata);


			return;

		}
		// アプリのインストール、アップデートを監視するオブジェクトの作成
		SharedPreferences pref = getSharedPreferences("sc_setting", MODE_PRIVATE);
		// 自動計測値の生成と整形
		CustomAndroidSystemObject caso = new CustomAndroidSystemObject(this, pref);

		cdata.put("packageName", caso.getPackageName());
		cdata.put("appVer", caso.getAppVer());
		cdata.put("osName", caso.getOSNAME());
		cdata.put("modelName", caso.getModelName());
		cdata.put("osVer", caso.getOsVer());
		cdata.put("iccImei", caso.getIccImei());

		if(timing == MeasurementTiming.ONCREATE){
			// アプリのバージョンチェック
			cdata.put("customEvent", caso.getChkAppState());
		}else if(timing == MeasurementTiming.ONRESTART){
			cdata.put("customEvent","resume");
		}else if(timing == MeasurementTiming.ONDESTROY){
			cdata.put("customEvent","shutdown");
		}

		if(timing == MeasurementTiming.ONCREATE){
			Analytics.trackState(caso.getPackageName(),cdata);
		}else if(timing == MeasurementTiming.ONRESTART){
			Analytics.trackAction("resume()",cdata);
		}else if(timing == MeasurementTiming.ONDESTROY){
			Analytics.trackAction("shutdown()",cdata);
		}

	}


}
