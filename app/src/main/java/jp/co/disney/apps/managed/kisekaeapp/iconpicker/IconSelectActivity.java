package jp.co.disney.apps.managed.kisekaeapp.iconpicker;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.PermissionChecker;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.widget.TextView;

public class IconSelectActivity extends Activity {

	public static String KISEKAE_SAKI_SHORTCUTICON_DTO = "jp.co.disney.apps.managed.kisekaeapp.kisekaemoto.icon";
	private final int REQUEST_CODE_AUTH_FROM_PICKER = 2141;
    private Runnable runnableForGetAuthInfoFromPicker;
	private boolean PermissionCheckNG = false;//requestPermissionsで許可した瞬間にonCreateを通るので
	private final Handler handler = new Handler();
	
	private final String[] Permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "IconSelectGridTypeActivity_onCreate_");

		if (Build.VERSION.SDK_INT >= 23) {//OSバージョン
		//パーミッションチェック//表示中に返られた時対応
			for (int i = 0; i < Permissions.length; i++) {

				if (PermissionChecker.checkSelfPermission(this
						, Permissions[i])
				!= PermissionChecker.PERMISSION_GRANTED) {
					PermissionCheckNG = true;
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "IconSelectGridTypeActivity_onCreate_checkSelfPermission_" + Permissions[i] + "_!= PERMISSION_GRANTED");
					this.finish();
					break;
				} else {
					jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("checkSelfPermission", "IconSelectGridTypeActivity_onCreate_checkSelfPermission_" + Permissions[i] + "_== PERMISSION_GRANTED");
				}

			}
		
			if (!PermissionCheckNG) {
				onCreatePermissionOK();
			}

		} else {//OSバージョン
			onCreatePermissionOK();
		}

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
       	handler.removeCallbacks(runnableForGetAuthInfoFromPicker);

	}

	private void onCreatePermissionOK(){
		runnableForGetAuthInfoFromPicker = new Runnable() {
			
			@Override
			public void run() {
				
				if(!SPPUtility.isAppEnabled(getApplicationContext(), "jp.co.disney.apps.base.disneymarketapp")){
					displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_BASE_APP_INVALID);
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

	}
	
	//SPP認証失敗でエラーダイアログ表示
	private void displaySppFailedAlertDialog(int reason){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (reason) {
		default:
		case SplashActivity.SPP_FAILED_REASON_AUTH_ERROR:
			builder.setMessage(R.string.invalid_app_fig2);
			break;
		case SplashActivity.SPP_FAILED_REASON_NETWORK_ERROR:
			builder.setMessage(R.string.invalid_app_fig1);
			break;
		case SplashActivity.SPP_FAILED_REASON_BASE_APP_INVALID:
			builder.setMessage(R.string.invalid_app_fig3);
			break;
		}

		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});

		builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			DebugLog.instance.outputLog("value", "back key pressed!");
			finish();
		    return true;
		default:
		    return false;
		}
			}
		});

        // ダイアログの作成と描画
		AlertDialog alert = builder.show();
		TextView messageText = (TextView)alert.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);

	}

	
	void startGetAuthInfo(){
		handler.post(runnableForGetAuthInfoFromPicker);
	}
	
	private Bitmap setAlpha(Bitmap bitmap, int alpha){
		if(bitmap == null) return null;
		bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

		if(bitmap.hasAlpha()){
			return bitmap;
		}else{
			bitmap.setHasAlpha(true);
			return bitmap;
		}

	}

	//ショートカットアイコンの画像取得（この時点でローカルのどこかしらに保存されている想定（今は仮でassetsから取得）
	protected Bitmap getShortcutIconImage(String filePath){
//	    AssetManager assetManager = me.getApplicationContext().getAssets();
//	    BufferedInputStream bis = null;
//	    try {
//	        bis = new BufferedInputStream(assetManager.open(iconPath));
//	        return BitmapFactory.decodeStream(bis);
//	    } catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		} finally {
//	        try {
//	            bis.close();
//	        } catch (Exception e) {
//	            //IOException, NullPointerException
//	        	return null;
//	        }
//	    }

	    BufferedInputStream bis = null;
	    try {
	    	FileInputStream inputStream = new FileInputStream(new File(filePath));
			bis = new BufferedInputStream(inputStream);
			return setAlpha(BitmapFactory.decodeStream(bis), 122);
	    } catch (IOException e) {
			e.printStackTrace();
			return null;
	    } finally {
	        try {
	            bis.close();
	        } catch (Exception e) {
	            //IOException, NullPointerException
	        	return null;
	        }
	    }


	}


	protected void showDialogFragment(){
		String tag = "error";
		FragmentManager fm = getFragmentManager();

		DialogFragment motoDialog = (DialogFragment)fm.findFragmentByTag( tag );
		if( motoDialog != null ) motoDialog.onDismiss( motoDialog.getDialog() );


        ErrorDialogFragment dialog = new ErrorDialogFragment();
//        dialog.setArguments(args);
        dialog.setCancelable(false);
//        dialog.show(fm, "error");

		FragmentTransaction ft = fm.beginTransaction();
		ft.add(dialog, tag);
		if(!isFinishing()){
			ft.commitAllowingStateLoss();
		}

	}

	 public class ErrorDialogFragment extends DialogFragment {

		 @Override
	        public Dialog onCreateDialog(Bundle savedInstanceState) {
				Bundle args = getArguments();
//				if( args == null ) return null;

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setView(null);

				builder.setMessage(R.string.err_msg_appli_picker);

				builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//IconSelectActivity.this.finish();
						dialog.dismiss();
						dialog = null;
					}
				});

//				builder.setOnKeyListener(new DialogInterface.OnKeyListener() {
//					@Override
//					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
//						switch (keyCode) {
//				case KeyEvent.KEYCODE_BACK:
//					DebugLog.instance.outputLog("value", "back key pressed!");
//					IconSelectActivity.this.finish();
//				    return true;
//				default:
//				    return false;
//				}
//					}
//				});


				AlertDialog dialog = builder.create();
//				TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
//		        if(messageText != null) messageText.setGravity(Gravity.CENTER);

		        dialog.setCancelable(false);

				return dialog;
		 }
	 }


	 // 設定するアイテムクラス
	 public static class AppData {
		 private String appName;
		 private String appPackageName;
		 private String appClassName;
		 private Drawable appIconImage_;

		 public void setAppIconImage(Drawable image) {
			 appIconImage_ = image;
		 }

	        public Drawable getAppIconImage() {
	            return appIconImage_;
	        }

	        public void setAppName(String stringItem) {
	            this.appName = stringItem;
	        }

	        public String getAppName() {
	            return this.appName;
	        }

	        public void setAppPackageName(String packageName) {
	        	appPackageName = packageName;
	        }

	        public String getAppPackageName() {
	            return appPackageName;
	        }

	        public void setAppClassName(String className) {
	            this.appClassName = className;
	        }

	        public String getAppClassName() {
	            return this.appClassName;
	        }


	    }

	 public void successGetAuthInfo(){
		 
	 }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
		if(requestCode == REQUEST_CODE_AUTH_FROM_PICKER){
        	if(intent == null || (intent.getExtras().getString("resultXML") != null && intent.getExtras().getString("resultXML").length() == 0)){
        		displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
        		
        	}else{
            	
        		String result = intent.getExtras().getString("resultXML");
        		if (TextUtils.isEmpty(result)) {
        			displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
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
        							displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
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
        				
        				DebugLog.instance.outputLog("value", "get auth info succses from pick");
        				successGetAuthInfo();

        			}else{
        				displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
        			}

        		} catch (final Exception e) {
        			displaySppFailedAlertDialog(SplashActivity.SPP_FAILED_REASON_AUTH_ERROR);
        		}

        	}

        }

	}
	 
	 

}
