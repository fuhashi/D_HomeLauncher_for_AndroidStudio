package jp.co.disney.apps.managed.kisekaeapp;

import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

public class AuthGetActivity extends Activity {
	
	private final int REQUEST_CODE_AUTH = 214;
	private String assetID = "";

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		DebugLog.instance.outputLog("value", "----------------called AuthGetActivity--------------");
		
		Intent intent = getIntent();
		assetID = intent.getStringExtra(SPPUtility.INTENT_EXTRA_ASSET_ID);
		
        //GetAuthInfo
        Intent sppIntent = new Intent(Intent.ACTION_MAIN);
        ComponentName compo = new ComponentName("jp.co.disney.apps.base.disneymarketapp", "jp.co.disney.apps.base.disneymarketapp.actBase");
        sppIntent.setComponent(compo);

        ApplicationInfo appliInfo = null;
        try {
            appliInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            sppIntent.putExtra("argAppId", appliInfo.metaData.getString("AppId").substring(3));
            sppIntent.putExtra("argFunc", 214);
            startActivityForResult(sppIntent, REQUEST_CODE_AUTH);            	

        } catch (NameNotFoundException e) {
        	startDownloadService("");
        	finish();
        }


	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if(data != null){
			startDownloadService(data.getStringExtra("resultXML"));
		}else{
			startDownloadService("");
		}
		
		finish();
	}
	
	private void startDownloadService(String data){
		DebugLog.instance.outputLog("value", "\\\\\\\\\\\\\\\\\\\\\\\\\\startDownloadService");
		
		// ダウンロード継続
		Intent dlSvc = new Intent(this, DisneyMarketBadgeLinkageIntentService.class);
		dlSvc.setAction(getPackageName() + ".DOWNLOAD_CONTINUE");
		dlSvc.putExtra(SPPUtility.INTENT_EXTRA_ASSET_ID, assetID);
		if (data != null && !data.equals("")) {
			DebugLog.instance.outputLog("value", "\\\\\\\\\\\\\\\\\\\\\\\\\\startDownloadService_putExtra_" + data);
			dlSvc.putExtra(DisneyMarketBadgeLinkageIntentService.INTENT_EXTRA_RESULT_XML, data);
		}
		startService(dlSvc);
	}
	
	
}
