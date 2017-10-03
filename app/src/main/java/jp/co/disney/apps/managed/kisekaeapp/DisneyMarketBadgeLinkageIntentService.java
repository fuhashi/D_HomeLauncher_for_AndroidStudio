package jp.co.disney.apps.managed.kisekaeapp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.CntApiConnector;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.CntApiConnector.Param;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SPPApiConnector;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BadgeDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BadgeDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.DataBaseParam;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.badlogic.gdx.utils.Array;
//import android.util.Log;

public class DisneyMarketBadgeLinkageIntentService extends IntentService{

    public DisneyMarketBadgeLinkageIntentService(String name){
        super(name);
    }
    public DisneyMarketBadgeLinkageIntentService(){
        super("TestIntentService");
    }
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    private String downloadUrl = "";//最初はトークンなしのURLを通常に取得して、それを元にトークンありのURLを別途取得して代入
    private String[] tokenArray = null;
    private String fileName = "";
//	private String themeTag = "";
    private String assetID ="";
    BadgeDataDto bto = null;

    @Override
    protected void onHandleIntent(Intent intent){
        
        String action = intent.getAction();
        if (action.equals(getApplicationContext().getPackageName() + ".DOWNLOAD_CONTINUE")) {
        	
        	// GetAuthInfo終了
        	String resultXML = intent.getStringExtra(INTENT_EXTRA_RESULT_XML);
        	if (saveAuthToken(resultXML)){
        		
        		assetID = intent.getStringExtra(SPPUtility.INTENT_EXTRA_ASSET_ID);
        		if(assetID == null) assetID = "";
        		if(!assetID.equals("")){
        			startDownload(); // 2-3. ダウンロード開始
        		}else{
            		finishDownload(false);
            		stopSelf(); // 3-1. ダウンロード中止
        		}
        		
        	} else { // 3. 失敗時
        		
        		finishDownload(false);
        		stopSelf(); // 3-1. ダウンロード中止
        		
        	} 
        } else {
            //解析してassetIdを取得する
            String data = intent.getDataString();
//    		data = "disneykisekaeapp://download/?asset_id=1000008800&file_id=1000040833&site_code=10000400";
            DebugLog.instance.outputLog("value", "TestIntentService__intent_data_" + data);

            assetID = data.substring(38);
            int indx = assetID.indexOf("&file_id");
            DebugLog.instance.outputLog("value", "index=" + indx);
            assetID = assetID.substring(0, indx);
            DebugLog.instance.outputLog("value", "assetID=" + assetID);
        	
        	
        	if (!SPPUtility.isNeedToken(getApplicationContext())) {
        		
        		SharedPreferences preferences = getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
        		token = preferences.getString(SPPUtility.PREF_KEY_ID_TOKEN, "");
        		accessToken = preferences.getString(SPPUtility.PREF_KEY_ACCESS_TOKEN, "");
        		
        		startDownload(); // ダウンロード開始
        	} else { // GetAuthInfoを実施
        		SPPUtility.callAuthGetActivity(getApplicationContext(), assetID);
        	}
       	}
        
        return;

    }
    
    private void startDownload(){

        //currentとhistoryのフォルダ構造を作っておく
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()));
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "current" + File.separator);
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "current" + File.separator + "theme");
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "current" + File.separator + "wp");
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "current" + File.separator + "widget");
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "current" + File.separator + "icon");
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "history" + File.separator);
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "history" + File.separator + "theme");
        FileUtility.makeDirectory(FileUtility.getSkinRootPath(getApplicationContext()) + "history" + File.separator + "wp");

        String motoTheme = FileUtility.getNowThemeID(getApplicationContext());

        setNotificationProgress(10);

        //アセットの情報を取得する

        CntApiConnector connector = new CntApiConnector(getApplicationContext());
        connector.setAccessEnviroment(getApplicationContext(), false, CntApiConnector.API_GET_ASSET_DIR)
        .addParameter(Param.env, connector.enviroment, false)
        .addParameter(Param.src, "", false);

        if(SPPUtility.CARRIER_CAMOUFLAGE_SET == 0){
            connector
            .addParameter(Param.uid, connector.UID, false)
            .addParameter(Param.deviceId, connector.deviceID, false)
            .addParameter(Param.osVersion, connector.osVersion, false)
            .addParameter(Param.carrierId, SPPUtility.getCarrierID(getApplicationContext()), false);
        }else{
            connector
            .addParameter(Param.uid, SPPUtility.getCamoflaICCID(), false)
            .addParameter(Param.deviceId, SPPUtility.getCamoflaDeviceId(), false)
            .addParameter(Param.osVersion, SPPUtility.getCamoflaOsVersion(), false)
            .addParameter(Param.carrierId, SPPUtility.getCamoflaCarrierId(), false);
        }

        connector
        .addParameter(Param.providedSiteId, "aki", false)
        .addParameter(Param.id, assetID, false)
        .addParameter(Param.categoryId, "all", false)
        .addParameter(Param.idType, "asset", false);

        connector = connector.setParameter();
        connector.setTimeout(30 * 1000);
        String responseBody = null;
        connector.connect();
        responseBody = connector.getResponseBody();

        setNotificationProgress(20);

        if(responseBody.equals("")){
            finishDownload(false);
            return;
        }else{
            String mkDirPath = "";
            try {
                bto = createBto(responseBody);
                DebugLog.instance.outputLog("value", "cto.assetID_" + bto.assetID);

                //既に内部にデータがあるかは関知しない

                //DL処理開始
                try {
                    Context myContext = getApplicationContext();
                    //オフラインチェック
                    if(!SPPUtility.checkNetwork(myContext)){
                        finishDownload(false);
                        return;
                    }

                    //アセット属性情報取得APIを使って取得
                    CntApiConnector connectorDL = new CntApiConnector(myContext);
                    connectorDL.setAccessEnviroment(myContext, false, CntApiConnector.API_GET_ASSET_DIR)
                            .addParameter(Param.env, connectorDL.enviroment, false)
                            .addParameter(Param.src, "", false);

                    if(SPPUtility.CARRIER_CAMOUFLAGE_SET == 0){
                        connectorDL
                        .addParameter(Param.uid, connectorDL.UID, false)
                        .addParameter(Param.deviceId, connector.deviceID, false)
                        .addParameter(Param.osVersion, connector.osVersion, false)
                        .addParameter(Param.carrierId, SPPUtility.getCarrierID(getApplicationContext()), false);
                    }else{
                        connectorDL
                        .addParameter(Param.uid, SPPUtility.getCamoflaICCID(), false)
                        .addParameter(Param.deviceId, SPPUtility.getCamoflaDeviceId(), false)
                        .addParameter(Param.osVersion, SPPUtility.getCamoflaOsVersion(), false)
                        .addParameter(Param.carrierId, SPPUtility.getCamoflaCarrierId(), false);
                    }

                    connectorDL
                            .addParameter(Param.providedSiteId, "aki", false)
                            .addParameter(Param.id, String.valueOf(bto.assetID), false)
                            .addParameter(Param.categoryId, "all", false)
                            .addParameter(Param.idType, "asset", false);
                    connectorDL = connectorDL.setParameter();

                    //ここにresponseを入れる
                    String responseBodyDL = null;
                    connectorDL.connect();
                    responseBodyDL = connectorDL.getResponseBody();

                    setNotificationProgress(30);

                    if(responseBodyDL.equals("")){
                        finishDownload(false);
                        return;
                    }

                    DebugLog.instance.outputLog("value", "connect:responseBodyDL" + responseBodyDL);

                    //ここでparser呼び出す
//					GetZipDownloadUrlParser parser = new GetZipDownloadUrlParser();
//					String downloadUrl;
//					String[] tokenArray = null;
                    try {
//						downloadUrl = parser.getObjectFromJson(responseBody, artBoxNum, myContext);
//						tokenArray = parser.getObjectArrayFromJson(responseBody, 3, myContext);
                        parseResponse(responseBody);
                        DebugLog.instance.outputLog("value", "connect:downloadURL" + downloadUrl);//取得できてる

                    } catch (JSONException e) {
                        e.printStackTrace();
                        DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
                        finishDownload(false);
                        return;
                    }


                    SPPApiConnector sppConnector = new SPPApiConnector(myContext);
//                    sppConnector.setAccessEnviromentAccessKey(myContext, tokenArray[1], tokenArray[2], SPPApiConnector.API_GET_DOWNLOAD_TOKEN_UNTIED)
                    sppConnector.setAccessEnviroment(myContext, SPPApiConnector.API_GET_DOWNLOAD_TOKEN)
//                        .addParameter(SPPApiConnector.Param.app_id, SPPUtility.getAppId(myContext), false);
                    	.addParameter(SPPApiConnector.Param.id_token, token, false)
                    	.addParameter(SPPApiConnector.Param.asset_id, assetID, false)
                    	.addParameter(SPPApiConnector.Param.env, sppConnector.enviroment, false)
                    	.addParameter(SPPApiConnector.Param.src, "aki", false)
                    	.addParameter(SPPApiConnector.Param.providedSiteId, "aki", false);


//                        if(SPPUtility.CARRIER_CAMOUFLAGE_SET == 0){
//                            sppConnector
//                            .addParameter(SPPApiConnector.Param.iccid, sppConnector.UID, false);
//                        }else{
//                            sppConnector
//                            .addParameter(SPPApiConnector.Param.iccid, SPPUtility.getCamoflaICCID(), false);
//                        }

                        sppConnector
                        .addParameter(SPPApiConnector.Param.download_from, "", false);
//                        .addParameter(SPPApiConnector.Param.site_cd, "aki", false)
//						.addParameter(SPPApiConnector.Param.link_status, "0", false)
//                        .addParameter(SPPApiConnector.Param.url, downloadUrl, true);
                        
                        
                    sppConnector = sppConnector.setParameter();
                    sppConnector.setAuthorization(accessToken, true);
                    sppConnector.connect();
                    String responseBodyFortokenURL = sppConnector.getResponseBody();
                    DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseCode" + connector.getResponseCode());
                    DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseMessage" + connector.getResponseMessage());

                    if(responseBodyFortokenURL.equals("")){
                        finishDownload(false);
                        return;
                    }

                    DebugLog.instance.outputLog("value", "SPPApiConnector:responseBody" + responseBodyFortokenURL);


//					GetDownloadTokenParser tokenParser = new GetDownloadTokenParser();
                    try {
//						downloadUrl = tokenParser.getObjectFromJson(responseBodyFortokenURL, 0, myContext);
                    	downloadUrl = SPPUtility.parseResponseForToken(tokenArray[2], responseBodyFortokenURL);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
                        finishDownload(false);
                        return;
                    }

                    setNotificationProgress(40);

//					//保存先ディレクトリ作成
                    mkDirPath = FileUtility.getSkinRootPath(myContext) + "current" + File.separator;

                    if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
                        mkDirPath = mkDirPath + "theme" + File.separator;
                    }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue() ){
                        mkDirPath = mkDirPath + "widget" + File.separator;
                    }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue() ){
                        mkDirPath = mkDirPath + "wp" + File.separator;
                    }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()  ){
                        mkDirPath = mkDirPath + "icon" + File.separator;
                    }
                    mkDirPath = mkDirPath + String.valueOf(bto.assetID) + File.separator;
                    FileUtility.makeDirectory(mkDirPath);

                    //保存先ディレクトリ関連操作
                    //テーマ以外はcurrent内の既存ファイルは追い出さない
                    if(bto.contentsType != ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
                        //種別に合わせたフォルダ内に該当フォルダを作成
                        if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
                            FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "wp" + File.separator + String.valueOf(bto.assetID) + File.separator);
                        }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
                            FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "widet" + File.separator + String.valueOf(bto.assetID) + File.separator);
                        }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
                            FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "icon" + File.separator + String.valueOf(bto.assetID) + File.separator);
                        }

                    }


                    //String path = myContext.getResources().getString(R.string.URL);
                    //String path = "http://encode.jp/sgr/" + DOWNLOAD_FILE_NAME;
                    String path = downloadUrl;
                    int filesize = 0; // file size temporary hard coded
                    long start = System.currentTimeMillis();
                    int bytesRead;
                    int current = 0;
                    int downloaded = 0;
                    int timeout = 100000;
                    FileOutputStream fos = null;
                    BufferedOutputStream bos = null;
                    byte[] mybytearray = null;

                    URL url = new URL(path);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);
                    connection.setRequestMethod("GET");
                    // ---------set request download--------
                    connection.setDoInput(true);
                    connection.connect();
                    // --------------------------------------
                    int lengthOfFile = connection.getContentLength();
                    if (lengthOfFile <= 0) {
                        connection.disconnect();
                        finishDownload(false);
                        return;
                    }
                    filesize = lengthOfFile + 2;
                    lengthOfFile += downloaded;
                    // receive file
//					mybytearray = new byte[filesize];

                    if(fileName.indexOf(".zip") != -1){
                        //zip解凍して保存
                        ZipInputStream zIns = new ZipInputStream(connection.getInputStream());
                        ZipEntry zipEntry = null;
                        int zipLen = 0;

                        setNotificationProgress(50);

                        //zipファイルに含まれるエントリに対して順にアクセス
                        while ((zipEntry = zIns.getNextEntry()) != null) {
                            DebugLog.instance.outputLog("value", "zipEntry.getName():" + zipEntry.getName());
                            String[] filename = zipEntry.getName().split("/");
                            for(int i = 0; i < filename.length; i++){
                                DebugLog.instance.outputLog("value", "zipEntry:" + i + ":" + filename[i]);
                            }
                            File downloadFile = new File(mkDirPath, filename[filename.length - 1]);
                            OutputStream os = new FileOutputStream(downloadFile);

                            bos = new BufferedOutputStream(os);

                            byte[] buffer = new byte[1024 * 4];
                            while ((zipLen = zIns.read(buffer)) != -1) {
                                bos.write(buffer, 0, zipLen);
                            }

                            zIns.closeEntry();
                            bos.close();
                            bos = null;

                        }

                    }else{
                        //通常保存
                        File saveFile = new File(mkDirPath, fileName);
                        BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
                        bos = new BufferedOutputStream(new FileOutputStream(saveFile));

                        byte[] zipBuffer = new byte[1024];
                        int readByte = 0;

                        setNotificationProgress(60);
                        while(-1 != (readByte = is.read(zipBuffer))){
                            bos.write(zipBuffer, 0, readByte);
                        }
                        bos.close();
                        bos = null;
                    }


                    connection.disconnect();

                } catch (Exception e) {
                    DebugLog.instance.outputLog("value", "CatchException in doInBackground:" + e.getMessage());
                    finishDownload(false);
                    return;
                }

                setNotificationProgress(80);

                //themeだった場合currentに既存のフォルダがあったらそれをhistoryに移動する（個別壁紙と個別アイコン、個別ウィジェットはcurrent内に保存するだけ）
                if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
                    if(!motoTheme.equals("") && !motoTheme.equals(String.valueOf(bto.assetID))){
                        DebugLog.instance.outputLog("value", "元のテーマのアセットID："+ motoTheme);
                        String motoPath = FileUtility.getSkinRootPath(getApplicationContext()) + "current" + File.separator + "theme" + File.separator + motoTheme + File.separator;

                        //もしmotothemeもバッジコンテンツだったらmotothemeは削除
                        BadgeDataAccess bAccess = new BadgeDataAccess(getApplicationContext());
                        Array<BadgeDataDto> bArray = bAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), motoTheme);
                        if(bArray != null && bArray.size > 0){
                            FileUtility.delFile(new File(motoPath));
                            bAccess.deleteById(Long.parseLong(motoTheme));
                        }else{
                            String sakiPath = FileUtility.getSkinRootPath(getApplicationContext()) + "history" + File.separator + "theme" + File.separator + motoTheme + File.separator;
                            try {
                                FileUtility.copyFile(motoPath, sakiPath, true);

                            } catch (IOException e) {
                                e.printStackTrace();
                                finishDownload(false);
                                return;
                            }
                        }

                    }
                }

                setNotificationProgress(85);

                //DB操作
                BadgeDataAccess mAccess = new BadgeDataAccess(getApplicationContext());
                Array<BadgeDataDto> recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(bto.assetID));
                if(recordArray != null && recordArray.size >= 1){
                    //更新
                    if(mAccess.updateSkinAddedDate(bto) > 0){
                        DebugLog.instance.outputLog("value", "スキン保存レコード更新成功");

                    }else{
                        DebugLog.instance.outputLog("value", "スキン保存レコード更新失敗");
                        if(!mkDirPath.equals("")) FileUtility.delFile(new File(mkDirPath));
                        finishDownload(false);
                        return;
                    }
                }else{
                    //挿入
                    if(mAccess.insertSkinData(bto) > 0){
                        DebugLog.instance.outputLog("value", "スキン保存レコード挿入成功：");

                    }else{
                        DebugLog.instance.outputLog("value", "スキン保存レコード挿入失敗");
                        if(!mkDirPath.equals("")) FileUtility.delFile(new File(mkDirPath));
                        finishDownload(false);
                        return;
                    }
                }

            } catch (JSONException e) {
                DebugLog.instance.outputLog("value", "exception_" + e.toString());
                e.printStackTrace();
            }

            setNotificationProgress(90);

            if(bto != null && !String.valueOf(bto.assetID).equals("")){

                Intent intentSetted = new Intent();
                intentSetted.setAction(Intent.ACTION_MAIN);
                intentSetted.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                intentSetted.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID, String.valueOf(bto.assetID));
                intentSetted.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, bto.contentsType);

                if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){

                }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
                        || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
                        || bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue() ){
                    //TODO 一括設定か個別設定かが必要（一括・個別分けに関しては未実装　201508（アイコンごとの個別設定が未実装なのでまだ不要

                }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() ||
                        bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue() ){

                }else if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() ||
                        bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue() ){

                    intentSetted.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_DETAIL_CONTENTS_TYPE, bto.detailContentsType);

                }

                DebugLog.instance.outputLog("value", "homeのActivity起動_" + bto.assetID);
//                intentSetted.setClassName(getApplicationContext().getPackageName(), getApplicationContext().getPackageName() + ".launcher.Launcher");
//                intentSetted.setClassName(getApplicationContext().getPackageName(), getApplicationContext().getPackageName() + ".launcher.MainActivity");
                intentSetted.addCategory(Intent.CATEGORY_HOME);
                intentSetted.setPackage(getApplicationContext().getPackageName());
                getApplicationContext().startActivity(intentSetted);

            }
        }



        //ダウンロード処理中はNotificationバー・エリアにダウンロード通知を出す（プログレス

        //ダウンロード処理完了時にDisneyMarketへブロードキャスト送信
        /*
Intent resultIntent = new Intent();
resultIntent.putExtra("result", result);//0 : 成功時,1 : エラー時
resultIntent.putExtra("assetId", assetId);
resultIntent.setAction("jp.co.disney.apps.base.disneymarketapp.spp30.BRAppAssetDownloadResult");
getBaseContext().sendBroadcast(resultIntent);
         */


        finishDownload(true);

    }
    
    public final static String INTENT_EXTRA_RESULT_XML = "resultXML";
    
	private String token = "";
	private String accessToken = "";

	private boolean saveAuthToken(String result){
		if (TextUtils.isEmpty(result)) {
			return false;
		}
		try {
			final XmlPullParserFactory factory = XmlPullParserFactory
					.newInstance();
			factory.setNamespaceAware(true);
			final XmlPullParser parser = factory.newPullParser();

			parser.setInput(new StringReader(result));
			int eventType = parser.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					final String tag = parser.getName().toLowerCase();
					if ("result".equals(tag)) {
						if (!"0".equals(parser.nextText().toLowerCase())) {
							return false;
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
				
				return true;
			}

		} catch (final Exception e) {
			return false;
		}

		return false;

	}
	
    private void setNotificationProgress(int prog){
//        builder.setProgress(100, prog, false); // API Level 14
//        nofManager.notify(R.string.application_name, builder.getNotification());

    }

    private void finishDownload(boolean isSuccess){
        setNotificationProgress(100);

        //ダウンロード処理可否をDisneyMarketに通知
        Intent resultIntent = new Intent();
        if(isSuccess){
            resultIntent.putExtra("result", 0);//0 : 成功時,1 : エラー時
        }else{
            resultIntent.putExtra("result", 1);//0 : 成功時,1 : エラー時
        }

        resultIntent.putExtra("assetId", assetID);
        resultIntent.setAction("jp.co.disney.apps.base.disneymarketapp.spp30.BRAppAssetDownloadResult");
        getBaseContext().sendBroadcast(resultIntent);

//        nofManager.cancel(R.string.application_name);
        stopSelf();
    }

    private void parseResponse(String response) throws JSONException{
        /*
    assetInfo
        >assetDetail
            >fileAttribute
                >isThumbnailが0
                    >downloadUrl
         */

        tokenArray = new String[3];
        JSONObject rootObj = new JSONObject(response);
        JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");

        //assetDetail
        for(int i = 0; i < assetInfoAry.length(); i++){
            //assetDetailを探す
            JSONObject item = assetInfoAry.getJSONObject(i);
            JSONObject detail = item.getJSONObject("assetDetail");

            //assetID
            tokenArray[1]  = item.getString("assetId");

            //descriptionを探す
//			String desc = detail.getString("description");
//			GetColorParser colorParser = new GetColorParser();
//			colorParser.getObjectFromJson(desc, artNum, context);

            //assetTagを探す
//			JSONArray tagAry = detail.getJSONArray("assetTag");
//			if(tagAry != null){
//				for(int j = 0; j < tagAry.length(); j++){
//					JSONObject tagItem = tagAry.getJSONObject(j);
//					if(tagItem.getString("tagGroupNameId").equals("055")){
//						themeTag = tagItem.getString("tagId");
//						DebugLog.instance.outputLog("value", "tagId:" + tagItem.getString("tagId"));
//						DebugLog.instance.outputLog("value", "tagId:" + themeTag);
//					}
//				}
//			}

            //fileAttributeを探す
            JSONArray attributeAry = detail.getJSONArray("fileAttribute");

            //この中からisThumbnailが0のもの
            if(attributeAry != null){
                for(int j = 0; j < attributeAry.length(); j++){
                    JSONObject item2 = attributeAry.getJSONObject(j);
                    DebugLog.instance.outputLog("value", "object:" + j + ":" + item2.toString());
                    if(item2.getInt("isThumbnail") == 0){
                        //このデータでバージョンを比較
//						newModifiedDate = detail.getString("modifiedDate");
//						newVersionCode = Integer.parseInt(item2.getString("versionCode"));
                        downloadUrl = item2.getString("downloadUrl");
                        fileName = item2.getString("fileName");
                        tokenArray[0] = item2.getString("downloadUrl");
                        tokenArray[2] = item2.getString("fileId");

                        //isThumbnail = 0が取れたらいいのでここで終わり。
                        j = attributeAry.length();
                        i = assetInfoAry.length();
                    }
                }
            }
        }
    }



    private boolean isPremiumUser(){
        Context myContext = getApplicationContext();

            //オフラインチェック
            if(!SPPUtility.checkNetwork(myContext)){
                return false;
            }

            //接続環境チェック
            String[] param = SPPUtility.getConnectEnv(myContext.getApplicationContext());
            String domain = "ssapi.disney.co.jp";
            if(param[0].equals("liv")){
                domain = "ssapi.disney.co.jp";
            }else if(param[0].equals("stg")){
                domain = "staging.ssapi.disney.co.jp";
            }

            HttpPost httpPost = new HttpPost("https://" + domain + "/webapi/v1/SPPJudgment");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Accept","application/json");
            HttpClient httpClient = new DefaultHttpClient();
            //http://hc.apache.org/httpclient-3.x/preference-api.html
            httpClient.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);


            //body部に追加
            //http://stackoverflow.com/questions/18188041/write-in-body-request-with-httpclient
            String postDataBody = SPPUtility.createDeviceIdentifier(myContext);//ここに本文を入れる。
            HttpEntity entity = null;
            try {
                entity = new ByteArrayEntity(postDataBody.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
            httpPost.setEntity(entity);
//			DebugLog.instance.outputLog("value", "GetUserProfileIntentService::postDataBody" + postDataBody);

            DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:接続直前");


            String responseBody = null;
            TelephonyManager manager = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
            String id = manager.getSimSerialNumber();
            if(id!=null&&!id.equals("")){
                try {
                    responseBody = httpClient.execute(httpPost, new ResponseHandler<String>() {
                        @Override
                        public String handleResponse(HttpResponse response)
                                throws ClientProtocolException, IOException {
//							DebugLog.instance.outputLog("value", "GetUserProfileIntentService::getStatusCode:" + response.getStatusLine().getStatusCode());
                            if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode()){
                                return EntityUtils.toString(response.getEntity(), "UTF-8");
                            }
                            return null;
//							return "";
                        }
                    });
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                    return false;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }

            }else{
                responseBody ="";
            }

            if(responseBody != null){
                return parsePremiumResponse(responseBody);
            }else{
                return false;
            }


    }

    /**
     * json解析
     */
    private boolean parsePremiumResponse(String jsonStr){
        //レスポンスを解析してuser_profileを保存
        //あとここで入会退会判定の結果も取得しておく
        DebugLog.instance.outputLog("value", "SppCheckDisneyStyle//////////////////response:" + jsonStr);

        boolean is_premium = false;

        //解析
        try {
            JSONObject rootObj = new JSONObject(jsonStr);
            JSONObject parseObj = rootObj.getJSONObject("spp_judgment");

            is_premium = parseObj.getBoolean("is_premium");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        DebugLog.instance.outputLog("value", "isPremium_" + is_premium);
        return is_premium;

    }


    private BadgeDataDto createBto(String json) throws JSONException{
        //それぞれのアセットのjsonに分ける
        Array<String> assetDetail = new Array<String>();

        JSONObject rootObj = new JSONObject(json);
        JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");

        //assetDetail(これが各個Asset情報)
        for(int i = 0; i < assetInfoAry.length(); i++){
            //assetDetailを探す
            assetDetail.add(assetInfoAry.getJSONObject(i).toString());
            DebugLog.instance.outputLog("value", "assetInfo__" + assetInfoAry.getJSONObject(i).toString());
        }

        //値が取れていたら
        if(assetDetail.size > 0){
            BadgeDataDto bto = new BadgeDataDto();
            bto = getAssetDataFromJson(bto, assetDetail.get(0));

//			String dbpath = "/data/data/" + getApplicationContext().getPackageName() + "/databases/" + BaseDatabase.DATABASE_NAME;
//			if(FileUtility.isExistFile(dbpath)){
//				BadgeDataAccess mAccess = new BadgeDataAccess(getApplicationContext());
//
//				Array<MyPageDataDto> recordArray = null;
//				if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
//						|| bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
//						|| bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
//						|| bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()){
//					//単独コンテンツ
//					//該当のアセットIDを持つレコードがDB内にあったら反映開始
////					recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf((bto).assetID));
//				}else{
//					//従属コンテンツ
//					//該当のテーマタグを持つレコードがDB内にあったら反映開始
//					String[] columns = { DataBaseParam.COL_THEME_TAG.getParam(), DataBaseParam.COL_CONTENTS_TYPE.getParam() };
//					String[] values = { bto.themeTag, String.valueOf(ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()) };
////					recordArray = mAccess.findMyPageDataDtoFromDB(columns, values);
//					if(recordArray != null){
//						DebugLog.instance.outputLog("value", "recordArray:" + recordArray.size);
//					}else{
//						DebugLog.instance.outputLog("value", "recordArray is null");
//					}
//				}
//
//				if(recordArray != null && recordArray.size > 0){
//					//DB内の既存・お気に入り関連情報を反映させる
//					bto.addedDate = recordArray.get(0).addedDate;
//				}
//
//			}

            return bto;
        }else{
            return null;
        }

    }

    /**
     *
     * @param jsonStr
     * @return
     * @throws JSONException
     */
    private BadgeDataDto getAssetDataFromJson(BadgeDataDto bto, String jsonStr) throws JSONException{
        DebugLog.instance.outputLog("value", "getAssetDataFromJson::");

        //アセット情報からアセットID、ピックアップonoff、ピックアップ番号、公開日付を取得
        //タグから色情報、キャラ情報、テーマグループ、コンテンツ種類を取得
        //assetDetailの中のcharacter、assetDetailの中のassetTag内に色タグ、テーマグループタグ、コンテンツ種類タグ

        JSONObject root = new JSONObject(jsonStr);
        //root直下にアセットID
        bto.assetID = root.getLong("assetId");

        //assetDetailの中のprovidedSiteの中にピックアップ関連と公開日付（supStartDate？
        JSONObject rootObj = root.getJSONObject("assetDetail");
        JSONArray providedSiteArray = rootObj.getJSONArray("providedSite");

        //従属系のコンテンツ
        if(rootObj.getString("assetTypeId").equals("ct_dnkpk") || rootObj.getString("assetTypeId").equals("ct_dnkpw")
                || rootObj.getString("assetTypeId").equals("ct_dnkpi") || rootObj.getString("assetTypeId").equals("ct_dnkps")){
            DebugLog.instance.outputLog("value", "従属系コンテンツ");

            int type = 0;
            if(rootObj.getString("assetTypeId").equals("ct_dnkpk")){
                DebugLog.instance.outputLog("value", "壁紙inテーマのアセット");
                type = ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue();

            }else if(rootObj.getString("assetTypeId").equals("ct_dnkpw")){
                DebugLog.instance.outputLog("value", "ウィジェットinテーマのアセット");
                type = ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue();
                //ウィジェットの細かい種別は下で取得

            }else if(rootObj.getString("assetTypeId").equals("ct_dnkpi")){
                DebugLog.instance.outputLog("value", "ドロワーアイコンinテーマのアセット");
                type = ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue();
                bto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_DRAWER.getValue();

            }else if(rootObj.getString("assetTypeId").equals("ct_dnkps")){
                DebugLog.instance.outputLog("value", "ショーカットアイコンinテーマのアセット");
                type = ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue();
                bto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue();
            }

            bto.contentsType = type;

            //キャラ以外のタグ
            JSONArray tagArray = rootObj.getJSONArray("assetTag");
            for(int i = 0; i < tagArray.length(); i++ ){
                JSONObject tag = tagArray.getJSONObject(i);

                //ウィジェット種類タグ
                if(tag.getString("tagGroupNameId").equals("057")){
                    //ウィジェットだったら種別も要把握
                    if(rootObj.getString("assetTypeId").equals("ct_dnkpw")){
                        String tagDetail = tag.getString("tagId");
                        if(tagDetail.equals("057001")){//バッテリーウィジェット
                            bto.detailContentsType = ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue();
                        }//TODO 以下ウィジェットの種類が増えるごとに条件分岐を増やす
                    }
                }

            }


        //テーマ、単独系のコンテンツだったら
        }else{
            DebugLog.instance.outputLog("value", "テーマ・単独系コンテンツ:");

            //コンテンツ種別
            if(rootObj.getString("assetTypeId").equals("ct_dnkp")){
                DebugLog.instance.outputLog("value", "テーマのアセット");
                bto.contentsType = ContentsTypeValue.CONTENTS_TYPE_THEME.getValue();

            }else if(rootObj.getString("assetTypeId").equals("ct_dnksw")){
                DebugLog.instance.outputLog("value", "ウィジェットのアセット");
                bto.contentsType = ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue();

            }else if(rootObj.getString("assetTypeId").equals("ct_dnksi")){
                DebugLog.instance.outputLog("value", "単独ショートカットアイコンのアセット");
                bto.contentsType = ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue();
                bto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue();

            }

            //キャラ以外のタグ
            JSONArray tagArray = rootObj.getJSONArray("assetTag");

            for(int i = 0; i < tagArray.length(); i++ ){
                JSONObject tag = tagArray.getJSONObject(i);

                //ウィジェット種類タグ
                if(tag.getString("tagGroupNameId").equals("057")){
                    //ウィジェットだったら種類も把握
                    if(rootObj.getString("assetTypeId").equals("ct_dnksw")){
                        String tagDetail = tag.getString("tagId");
                        if(tagDetail.equals("057001")){//バッテリーウィジェット
                            bto.detailContentsType = ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue();
                        }//TODO 以下ウィジェットの種類が増えるごとに条件分岐を増やす
                    }
                }

            }
        }

        return bto;
    }

    Notification.Builder builder = null;
    NotificationManager nofManager = null;
    private void sendNotification(String def){
//		Log.d("value", "VersionCheckIntentService::sendNotification");

        //http://dev.classmethod.jp/smartphone/android/android-tips-23-android4-1-notification-style/
//		Intent intent = new Intent(this, UpdateDialogFragmentActivity.class);
//		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//		intent.putExtra(UpdateDialogFragmentActivity.DIALOG_DISCLIPTION, def);

//		PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0);


//		PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);

        builder = new Notification.Builder(this);
//		builder.setContentIntent(pi);
        builder.setWhen(System.currentTimeMillis());
//		builder.setTicker(getResources().getString(R.string.updt_tickerText));
        builder.setTicker("test");
        builder.setContentTitle(def);
//		builder.setContentText(getResources().getString(R.string.updt_contentText));
        builder.setSmallIcon(R.drawable.ic_stat_notify);
        builder.setAutoCancel(true);

        nofManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        setNotificationProgress(0);
        // プログレスバー表示用のスレッド
        // プログレスバー表示更新用に毎回notifyしているので、明示的に通知を消したい場合は別途このスレッドを止める処理を行なう必要がある
        /*
        new Thread(new Runnable() {
            @Override
            public void run() {
                // バーの表示
                for (int i = 0; i <= 100; i ++) {
                    builder.setProgress(100, i, false); // API Level 14
                    nofManager.notify(0, builder.getNotification());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        ;
                    }
                }
                // 完了時の通知
//                builder.setProgress(0, 0, false); // API Level 14
//                nofManager.notify(0, builder.getNotification());
            }
        }).start();
        */

    }

}
