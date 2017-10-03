package jp.co.disney.apps.managed.kisekaeapp.spp;

import jp.co.disney.apps.managed.kisekaeapp.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
//import android.util.Log;


public class BaseAppDownloadActivity extends Activity implements BaseAppDownloadTaskCallback{
	private Activity me;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.baseappdl_main);
		me = this;

		//�T�C�Y����
		int viewWidth = this.getResources().getDisplayMetrics().widthPixels;
		float dPix = viewWidth/480f;
		ImageView header = (ImageView)findViewById(R.id.baseApp_dl_header);
		LinearLayout.LayoutParams headerLayout= new LinearLayout.LayoutParams((int)(480f*dPix), (int)(366f*dPix));
		//header.setPadding(0, 15, 0, 0);
		header.setLayoutParams(headerLayout);

		ImageView footer = (ImageView) findViewById(R.id.baseApp_dl_footer);
		LinearLayout.LayoutParams footerLayout= new LinearLayout.LayoutParams((int)(480f*dPix), (int)(26f*dPix));
		//footer.setPadding(0, 10, 0, 0);
		footer.setLayoutParams(footerLayout);

		float dPix2 = viewWidth/215f * 0.5f;//��ʃT�C�Y�̔���
		Button dlBtn = (Button) this.findViewById(R.id.baseApp_dl_btn);
		LinearLayout.LayoutParams btnLayout= new LinearLayout.LayoutParams((int)(443f*dPix), (int)(65f*dPix));
		dlBtn.setLayoutParams(btnLayout);

		dlBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(checkNetwork()){
//					Log.d("value", "�_�E�����[�h�J�n");
					new BaseAppDownloadAsyncTask(me).execute("");
				}else{
					createErrorDialog("baseapp_dl_stop");
				}
			}
		});
	}

	private String getText(String stringName){
		int stringId = this.getResources().getIdentifier(stringName, "string", this.getPackageName());
		return this.getResources().getString(stringId);
	}

	private boolean checkNetwork(){
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo nInfo = cm.getActiveNetworkInfo();
        if (nInfo == null) return false;
        if (nInfo.isConnected()) return true; /* NetWork�ڑ��� */
		return false;
	}

	private void createErrorDialog(String messageId){
    	AlertDialog.Builder errorDL = new AlertDialog.Builder(me);
    	errorDL.setMessage(getText(messageId));
    	errorDL.setPositiveButton("OK",
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            	me.finish();
            }
          })
        .show();
	}

	@Override
	protected void onResume() {
		super.onResume();
//		Log.d("value", "onResume");

		if(isAppInstalled("jp.co.disney.apps.base.disneymarketapp")){
//			Intent intent = new Intent(Intent.ACTION_MAIN);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			Intent intent = new Intent();
			intent.setClassName(getApplicationContext(), getPackageName() + ".SpeechRecognizerActivity");
			try {
//				Log.d("value", "SpeechRecognizerActivity start");
				startActivity(intent);
				finish();
			} catch (ActivityNotFoundException e) {
//				Log.d("value", "intent = null");
				intent = null;
			}
		}
	}

	private boolean isAppInstalled (String packageName) {
		PackageManager pm = getPackageManager();
		try {
			@SuppressWarnings("unused")
			ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
			return true;
		}
		catch (NameNotFoundException e) {
			return false;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	/*
	 * �_�E�����[�h���s���̃_�C�A���O�\��
	 * @see jp.co.disney.apps.dmd.voiceactivationlw.BaseAppDownloadTaskCallback#onFailedDownloadApp()
	 */
	@Override
	public void onFailedDownloadApp() {
		createErrorDialog("baseapp_dl_error");
	}

}
