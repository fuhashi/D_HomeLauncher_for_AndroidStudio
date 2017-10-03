package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.CntApiConnector.Param;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForCatalog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.AddedDateComparatorAsc;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BadgeDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BadgeDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.DataBaseParam;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.view.WindowManager.BadTokenException;

import com.badlogic.gdx.utils.Array;


/**
 * アセットIDから実体コンテンツを取得する（カウントするトークンを使用）
 * @author sagara
 *
 */
public class DownloadSkinAsyncTask extends
		AsyncTask<ContentsDataDto, Integer, Integer> implements OnCancelListener{

	private ProgressDialog dialog = null;
	Context myContext;

	public final static int SUCCESS = 0, FAILED_REASON_NOT_PREMIUM = 1, FAILED_REASON_PREMIUM_AUTH_ERROR = 2, FAILED_REASON_NETOWORK_ERROR = 3;

	long startTime = 0;
	private DownloadSkinTaskCallback callback;
	public boolean isShowProgress = false;

	private int timeout = 30 * 1000;
	Timer   mTimer   = null;

	public DownloadSkinAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (DownloadSkinTaskCallback) context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
		if(isShowProgress){

			dialog = new ProgressDialog(myContext);
			try {
                dialog.show();
        } catch (BadTokenException e) {

        }
//			dialog.setTitle("Downloading...");
			// スタイルを設定
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setCancelable(true);
			dialog.setOnCancelListener(this);
			dialog.setCanceledOnTouchOutside(false);
			
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
	        dialog.setContentView(R.layout.progressdialog);
			
			dialog.show();

		}
		
		
		//開始時刻を取得（タイムアウト設定しないならこれはいらないが一応残しておく。 20150501
		startTime = System.currentTimeMillis();

		mTimer = new java.util.Timer(true);
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if((System.currentTimeMillis() - startTime) >= timeout){
					DebugLog.instance.outputLog("value", "timeout!!!!!");
					mTimer.cancel();
					mTimer = null;
					callCancel();
				}
				DebugLog.instance.outputLog("value", "time:" + (System.currentTimeMillis() - startTime));
			}

		}, 100, 100);
	}

	private void callCancel(){
		cancel(true);
	}

//	private static String DOWNLOAD_FILE_NAME = "";
//	private boolean flagDownload = false;
	private ContentsDataDto cto = null;
	
	private final int COR205_NOT_CALL = -1, COR205_DL_COUNT_NOT_SKIP = 0, COR205_DL_COUNT_SKIP = 1;
	private String parentAssetIdString = "";

	/**
	 * arg0[0]:assetID
	 * arg0[1]:コンテンツ種別
	 * return t（成功）/f（失敗）
	 */
	@Override
//	protected Boolean doInBackground(String... arg0) {
	protected Integer doInBackground(ContentsDataDto... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "スキンの実体保存task");

		//パラメータ指定が足りなかったらcancel
		if(arg0.length <= 0) return FAILED_REASON_NETOWORK_ERROR;

		//currentとhistoryのフォルダ構造を作っておく
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext));
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator);
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "theme");
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "wp");
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "widget");
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "icon");
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "history" + File.separator);
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "history" + File.separator + "theme");
		FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "history" + File.separator + "wp");

		cto = arg0[0];

		String motoTheme = FileUtility.getNowThemeID(myContext);

		MyPageDataAccess mAccess = new MyPageDataAccess(myContext);

		//DB内にあったらDB内の情報もctoに入れる
		Array<MyPageDataDto> a = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(cto.assetID));
		if(a != null && a.size > 0){
			//cto = new ContentsDataDto(a.get(0));
			cto.setDataFromDB(a.get(0));
		}

		if(String.valueOf(cto.assetID).equals(motoTheme)){
			DebugLog.instance.outputLog("value", cto.assetID + "は現在設定中");
			//DBの追加日付・設定日付だけ更新
			mAccess.updateSkinAddedDate(cto);
			mAccess.updateSkinThemeSetDate(cto);

			return SUCCESS;
		}


		//テーマはhistoryにあったらそっちのを使う
		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
			if(cto.isExist){
				//現在設定中のものではないことを確認
				if(!String.valueOf(cto.assetID).equals(motoTheme)){
					//currentの中身のthemeをhistoryに移動、ctoのnowSettingを外してisExistをtrueに
					String motoPath = FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "theme" + File.separator + motoTheme + File.separator;
					String sakiPath = FileUtility.getSkinRootPath(myContext) + "history" + File.separator + "theme" + File.separator + motoTheme + File.separator;
					try {
						FileUtility.copyFile(motoPath, sakiPath, false);
						FileUtility.delFile(new File(motoPath));

					} catch (IOException e) {
						e.printStackTrace();
						return FAILED_REASON_NETOWORK_ERROR;
					}

					//historyの中身をcurrentに移動
					motoPath = FileUtility.getSkinRootPath(myContext) + "history" + File.separator + "theme" + File.separator + String.valueOf(cto.assetID) + File.separator;
					sakiPath = FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "theme" + File.separator + String.valueOf(cto.assetID) + File.separator;
					try {
						FileUtility.copyFile(motoPath, sakiPath, true);

					} catch (IOException e) {
						e.printStackTrace();
						return FAILED_REASON_NETOWORK_ERROR;
					}

					cto.isExist = true;
					cto.hasDownloadHistory = true;
					//DB内で追加日付更新
					mAccess.updateSkinIsExist(cto, true);
					mAccess.updateSkinThemeSetDate(cto);

//					flagDownload = true;
					return SUCCESS;
				}

			}
		}else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
			//ショートカットアイコンはダミー本体をDLしっ放しなので、それがあるならDLしない
			
			//指定箇所にあるかどうかを確認（iconフォルダのassetId.txt）
			if(FileUtility.isExistFile(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "icon" + File.separator + String.valueOf(cto.assetID) + ".txt")){
				DebugLog.instance.outputLog("value", String.valueOf(cto.assetID) + "の本体DL済み");
				//存在していたら
				cto.isExist = true;
				if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()){
					cto.hasDownloadHistory = true;
				}else{
					cto.hasDownloadHistory = false;
				}
				
				return manageDB(mAccess);
			}else{
				DebugLog.instance.outputLog("value", String.valueOf(cto.assetID) + "の本体初DL");
			}
	
			
		}
		
		//TODO プレミアム関連対象の場合、ユーザの会員状況を要把握
		String carrierId = SPPUtility.getCarrierID(myContext);
    	if(carrierId.equals(SplashActivity.AUTH_CARRIER_OND)
    			|| carrierId.equals(SplashActivity.AUTH_CARRIER_AU)
    			|| carrierId.equals(SplashActivity.AUTH_CARRIER_CONPAS)){

       		if(cto.isPremium){
    			//プレミアム会員確認

				//オフラインチェック
				if(!SPPUtility.checkNetwork(myContext)){
					return FAILED_REASON_NETOWORK_ERROR;
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

				if(isCancelled()){
					return FAILED_REASON_PREMIUM_AUTH_ERROR;
				}

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
//				DebugLog.instance.outputLog("value", "GetUserProfileIntentService::postDataBody" + postDataBody);

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
					return FAILED_REASON_PREMIUM_AUTH_ERROR;
				} catch (IOException e) {
					e.printStackTrace();
					return FAILED_REASON_PREMIUM_AUTH_ERROR;
				} finally {
					httpClient.getConnectionManager().shutdown();
				}

				}else{
					responseBody ="";
				}

				if(responseBody != null){
					if(!parsePremiumResponse(responseBody)) return FAILED_REASON_NOT_PREMIUM;
				}

    		}
    	}



//		//この辺CNTへの接続で失敗する可能性があるので、CNTへ接続して問題なく落とせた後にフォルダを移動したほうがいいかも
//		if(contentsType == ContentsValue.CONTENTS_TYPE_THEME.getValue()){
//			//テーマのみ、現在のcurrentフォルダ内にデータがあったら入れ替える必要がある為、そのデータのアセットID取得
//			motoTheme = FileUtility.getNowThemeID(myContext);
//
//			//今回指定したアセットが既にhistoryにあったらDLしないでこれを使う
//			if(cto.isExist){
//				//現在設定中のものではないことを確認
//				if(!String.valueOf(cto.assetID).equals(motoTheme)){
//					//currentの中身のthemeをhistoryに移動、ctoのnowSettingを外してisExistをtrueに
//					String motoPath = "/data/data/" + myContext.getPackageName() + "/files/skin/current/theme/" + motoTheme + "/";
//					String sakiPath = "/data/data/" + myContext.getPackageName() + "/files/skin/history/theme/" + motoTheme + "/";
//					try {
//						FileUtility.copyFile(motoPath, sakiPath, true);
//
//					} catch (IOException e) {
//						e.printStackTrace();
//						return false;
//					}
//
//					//historyの中身をcurrentに移動
//					motoPath = "/data/data/" + myContext.getPackageName() + "/files/skin/history/theme/" + assetId + "/";
//					sakiPath = "/data/data/" + myContext.getPackageName() + "/files/skin/current/theme/" + assetId + "/";
//					try {
//						FileUtility.copyFile(motoPath, sakiPath, true);
//
//					} catch (IOException e) {
//						e.printStackTrace();
//						return false;
//					}
//
//					cto.isExist = true;
//					//DB内で追加日付更新
//					mAccess.updateSkinIsExist(createMyPageDataDtoFromCto(cto), true);
//
////					flagDownload = true;
//					return true;
//				}else{
//					//現在設定中のテーマ（この条件に入ることがあるかはわからないけど念のため設定しておく
//					DebugLog.instance.outputLog("value", cto.assetID + "は現在設定中");
//					return false;
//				}
//			}
//
//		//テーマ以外はcurrent内の既存ファイルは追い出さない
//		}else{
//
//			//種別に合わせたフォルダ内に該当フォルダを作成
//			if(cto.contentsType == ContentsValue.CONTENTS_TYPE_WP.getValue() || cto.contentsType == ContentsValue.CONTENTS_TYPE_WP_IN_T.getValue()){
//				FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/skin/current/wp/" + assetId + "/");
//			}else if(cto.contentsType == ContentsValue.CONTENTS_TYPE_WIDGET.getValue() || cto.contentsType == ContentsValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
//				FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/skin/current/widet/" + assetId + "/");
//			}else if(cto.contentsType == ContentsValue.CONTENTS_TYPE_ICON.getValue() || cto.contentsType == ContentsValue.CONTENTS_TYPE_ICON_IN_T.getValue()){
//				FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/skin/current/icon/" + assetId + "/");
//			}
//
//		}
    	
    	
		int parentDLSKIP = COR205_NOT_CALL;
		int meDLSKIP = COR205_NOT_CALL;
		String mkDirPath = "";

		//DL処理開始
		try {
			//for(int i=0; i<10; i++){
				if(isCancelled()){
//					DebugLog.instance.outputLog("value", "Cancelled!");
					return FAILED_REASON_NETOWORK_ERROR;
				}
				//Thread.sleep(1000);
				//publishProgress((i+1) * 10);

				//オフラインチェック
				if(!SPPUtility.checkNetwork(myContext)){
					return FAILED_REASON_NETOWORK_ERROR;
				}

				//アセット属性情報取得APIを使って取得
				CntApiConnector connector = new CntApiConnector(myContext);
				connector.setAccessEnviroment(myContext, false, CntApiConnector.API_GET_ASSET_DIR)
						.addParameter(Param.env, connector.enviroment, false)
						.addParameter(Param.src, "", false);

				if(SPPUtility.CARRIER_CAMOUFLAGE_SET == 0){
					connector
					.addParameter(Param.uid, connector.UID, false)
					.addParameter(Param.deviceId, connector.deviceID, false)
					.addParameter(Param.osVersion, connector.osVersion, false)
					.addParameter(Param.carrierId, SPPUtility.getCarrierID(myContext), false);
				}else{
					connector
					.addParameter(Param.uid, SPPUtility.getCamoflaICCID(), false)
					.addParameter(Param.deviceId, SPPUtility.getCamoflaDeviceId(), false)
					.addParameter(Param.osVersion, SPPUtility.getCamoflaOsVersion(), false)
					.addParameter(Param.carrierId, SPPUtility.getCamoflaCarrierId(), false);
				}

				connector
					.addParameter(Param.assetTypeId, cto.getContentsTypeId(), false)
					.addParameter(Param.providedSiteId, "aki", false)
					.addParameter(Param.id, String.valueOf(cto.assetID), false)
					.addParameter(Param.categoryId, "all", false)
					.addParameter(Param.idType, "asset", false);
				connector = connector.setParameter();

				//ここにresponseを入れる
				String responseBody = null;
				connector.connect();
				DebugLog.instance.outputLog("value", "connect:getResponseCode" + connector.getResponseCode());
				DebugLog.instance.outputLog("value", "connect:getResponseMessage" + connector.getResponseMessage());
				responseBody = connector.getResponseBody();

				if(responseBody.equals("")) return FAILED_REASON_NETOWORK_ERROR;

				DebugLog.instance.outputLog("value", "connect:responseBody" + responseBody);

				/*
				 * parseして、必要な情報を抜き出して、実体ファイル（大体zipと思われる）のURL（トークンつき）を取得してDLし端末に保存する。
				 * 此処でresponseから取得できる情報は色タグキャラタグと思われる。他はzip内のxmlから取得。
				 * あと一括テーマか壁紙かウィジェットかアイコンかで保存場所？を変えた方がいいかも。
				 * 一括テーマと個別WPはローカルの履歴に表示するのでそれを考慮すること。
				 * あとウィジェットとアイコンもローカル保存する場合もあるのでそれも要対応。
				 */

				/*
				 * 取得する情報
				 * 一括テーマ：自身のアセットID、擬似子アセットID（壁紙、ウィジェット、アイコン）、キャラタグ、色タグ
				 * 従属壁紙：自身のアセットID、擬似親アセットID、キャラタグ、色タグ
				 * 従属ウィジェット：自身のアセットID、擬似親アセットID、キャラタグ、色タグ
				 * 従属アイコン：自身のアセットID、擬似親アセットID、キャラタグ、色タグ
				 * 単独ウィジェット：自身のアセットID、キャラタグ、色タグ
				 * 単独アイコン：自身のアセットID、キャラタグ、色タグ
				 */

				/*
				 * ウィジェット・アイコンの単独コンテンツは個別で落としたらそのままhistoryフォルダに保存
				 * テーマ一括はcurrent＞themeフォルダ内に保存
				 * 壁紙個別設定は履歴表示がないので、個別時はcurrent＞wpフォルダ内に保存
				 * 新しいテーマが保存されたらcurrent内のデータは全てhistoryに移動
				 * 保存やらの関連動作が全部終わったらホームアプリを起動する（intentにテーマなりなんなりが変わった旨を乗せておく
				 */

				//実体ファイルのファイル名（拡張子含む）もresponseから解析して取得した方がいい？→アセットに対して実体ファイルが一種という前提

				//ここでparser呼び出す
//				GetZipDownloadUrlParser parser = new GetZipDownloadUrlParser();
//				String downloadUrl;
//				String[] tokenArray = null;
				try {
//					downloadUrl = parser.getObjectFromJson(responseBody, artBoxNum, myContext);
//					tokenArray = parser.getObjectArrayFromJson(responseBody, 3, myContext);
					parseResponse(responseBody);
					DebugLog.instance.outputLog("value", "connect:downloadURL" + downloadUrl);//取得できてる
					if(!themeTag.equals("")) cto.themeTag = themeTag;

				} catch (JSONException e) {
					e.printStackTrace();
					DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
					return FAILED_REASON_NETOWORK_ERROR;
				}
				
				//contentslistを取得して
				//*10Apps以外
				if(!SPPUtility.isAuthThroughFlag && !carrierId.equals(SplashActivity.AUTH_CARRIER_10APPS)){
					
					SPPApiConnector contentsCheckConnector = new SPPApiConnector(myContext.getApplicationContext());

					SharedPreferences preferences = myContext.getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
	        		String tokenForList = preferences.getString(SPPUtility.PREF_KEY_ID_TOKEN, "");
	        		String accessTokenForList = preferences.getString(SPPUtility.PREF_KEY_ACCESS_TOKEN, "");
	        		
	        		if(tokenForList.equals("") || accessTokenForList.equals("")) return FAILED_REASON_NETOWORK_ERROR;
					
	        		contentsCheckConnector.setAccessEnviroment(myContext, SPPApiConnector.API_GET_CONTENTS_LIST)
                	.addParameter(SPPApiConnector.Param.id_token, tokenForList, false)
                	.addParameter(SPPApiConnector.Param.env, contentsCheckConnector.enviroment, false)
                	.addParameter(SPPApiConnector.Param.src, "aki", false)
//                	.addParameter(Param.ranking.name(), "", false)
                	.addParameter(Param.carrierId.name(), carrierId, false)
                	.addParameter(SPPApiConnector.Param.providedSiteId, "aki", false)
                	.addParameter(Param.categoryId.name(), "all", false)
//                	.addParameter(Param.isApplicationFlag.name(), "0", false)
//                	.addParameter(Param.disneyFlag.name(), "", false)
//                	.addParameter(Param.pickUpFlag.name(), "", false)
//                	.addParameter(Param.limitFlag.name(), "", false)
//                	.addParameter(Param.userLevelFrom.name(), "", false)
//                	.addParameter(Param.userLevelTo.name(), "", false)
//                	.addParameter(Param.premiumFlag.name(), "", false)
//                	.addParameter(Param.inAppPurchaceFlag.name(), "", false)
                	.addParameter(Param.isNotPublished.name(), Integer.toString(1), false);
//                	.addParameter(Param.characterId.name(), "", false)
//                	.addParameter(Param.searchStartDate.name(), "", false)
//                	.addParameter(Param.searchEndDate.name(), "", false)
//                	.addParameter(Param.searchOffset.name(), "", false)
//                	.addParameter(Param.searchLimit.name(), "", false);
                	
                	//子アセットだったらタグ、親アセットだったら自身のアセットID
	        		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
	        				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
	        				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
	        				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()){
		        		contentsCheckConnector
	                	.addParameter(Param.assetId.name(), Long.toString(cto.assetID), false);
	        			
	        		}else{
		        		contentsCheckConnector
	                	.addParameter(Param.tagGroup.name(), "055", false)
	                	.addParameter(Param.tag.name(), cto.themeTag, false);
	        			
	        		}
        		
	        		
	                contentsCheckConnector = contentsCheckConnector.setParameter();
	                contentsCheckConnector.setAuthorization(accessTokenForList, true);
	        		
					contentsCheckConnector.connect();
					String responseBodyForContentList = contentsCheckConnector.getResponseBody();
//					DebugLog.instance.outputLog("value", "contentsCheckConnector:getResponseCode" + contentsCheckConnector.getResponseCode());
//					DebugLog.instance.outputLog("value", "contentsCheckConnector:getResponseMessage" + contentsCheckConnector.getResponseMessage());
					DebugLog.instance.outputLog("value", "contentsCheckConnector:responseBody" + responseBodyForContentList);

					if(responseBodyForContentList.equals("")) return FAILED_REASON_NETOWORK_ERROR;

					try {
						
						if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
		        				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
		        				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
		        				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()){
							meDLSKIP = parseResponseForSelfContentsList(responseBodyForContentList);
							DebugLog.instance.outputLog("value", "meDLSkip_" + meDLSKIP);
							
						}else{
							parentDLSKIP = parseResponseForParentContentsList(responseBodyForContentList);
							meDLSKIP = parseResponseForSelfContentsList(responseBodyForContentList);
							DebugLog.instance.outputLog("value", "meDLSkip_" + meDLSKIP);
							DebugLog.instance.outputLog("value", "parentDLSKIP_" + parentDLSKIP);

						}
						
					} catch (JSONException e) {
						e.printStackTrace();
						DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
						return FAILED_REASON_NETOWORK_ERROR;
					}
					
				}
				
				
				SPPApiConnector sppConnector = new SPPApiConnector(myContext);
				
				if(!SPPUtility.isAuthThroughFlag && !carrierId.equals(SplashActivity.AUTH_CARRIER_10APPS)){
					
					if(meDLSKIP != COR205_NOT_CALL){
//						if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue() ||
//						cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() ||
//						cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() ||
//						cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()){

					//自身に対してのみ
	        		SharedPreferences preferences = myContext.getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
	        		String token = preferences.getString(SPPUtility.PREF_KEY_ID_TOKEN, "");
	        		String accessToken = preferences.getString(SPPUtility.PREF_KEY_ACCESS_TOKEN, "");
	        		
	                sppConnector.setAccessEnviroment(myContext, SPPApiConnector.API_GET_DOWNLOAD_TOKEN)
	                	.addParameter(SPPApiConnector.Param.id_token, token, false)
	                	.addParameter(SPPApiConnector.Param.asset_id, Long.toString(cto.assetID), false)
	                	.addParameter(SPPApiConnector.Param.env, sppConnector.enviroment, false)
	                	.addParameter(SPPApiConnector.Param.src, "aki", false)
	                	.addParameter(SPPApiConnector.Param.providedSiteId, "aki", false)
	                	.addParameter(SPPApiConnector.Param.dlskip, Integer.toString(meDLSKIP), false)
	                    .addParameter(SPPApiConnector.Param.download_from, "", false);
	                    
	                    
	                sppConnector = sppConnector.setParameter();
	                sppConnector.setAuthorization(accessToken, true);
	                
//				}
						
					}

					
				}else{
					//10Appsは問答無用でCOR-080のみ（テーマでも子アセットでも）
					sppConnector.setAccessEnviromentAccessKey(myContext, tokenArray[1], tokenArray[2], SPPApiConnector.API_GET_DOWNLOAD_TOKEN_UNTIED)
					.addParameter(SPPApiConnector.Param.app_id, SPPUtility.getAppId(myContext), false);

					if(SPPUtility.CARRIER_CAMOUFLAGE_SET == 0){
						sppConnector.addParameter(SPPApiConnector.Param.iccid, sppConnector.UID, false);
					}else{
						sppConnector.addParameter(SPPApiConnector.Param.iccid, SPPUtility.getCamoflaICCID(), false);
					}

					sppConnector
					.addParameter(SPPApiConnector.Param.download_from, "", false)
					.addParameter(SPPApiConnector.Param.site_cd, "aki", false)
//					.addParameter(SPPApiConnector.Param.link_status, "0", false)
					.addParameter(SPPApiConnector.Param.url, downloadUrl, true);
					sppConnector = sppConnector.setParameter();
				
				}
				
				sppConnector.connect();
				String responseBodyFortokenURL = sppConnector.getResponseBody();
				DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseCode" + connector.getResponseCode());
				DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseMessage" + connector.getResponseMessage());
				DebugLog.instance.outputLog("value", "SPPApiConnector:responseBody" + responseBodyFortokenURL);

				if(responseBodyFortokenURL.equals("")) return FAILED_REASON_NETOWORK_ERROR;


				try {
					if(!SPPUtility.isAuthThroughFlag && !carrierId.equals(SplashActivity.AUTH_CARRIER_10APPS)){
						downloadUrl = SPPUtility.parseResponseForToken(tokenArray[2], responseBodyFortokenURL);
					}else{
						parseResponseForToken(responseBodyFortokenURL);
					}
				} catch (JSONException e) {
					e.printStackTrace();
					DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
					return FAILED_REASON_NETOWORK_ERROR;
				}

//				DOWNLOAD_FILE_NAME = arg0[0];

//				//保存先ディレクトリ作成
//				String mkDirPath = "/data/data/" + myContext.getPackageName() +  "/files/skin/current/";
				mkDirPath = FileUtility.getSkinRootPath(myContext) + "current" + File.separator;
//				String mkDirPath = "/data/data/" + myContext.getPackageName() +  "/files/art_images/" + DOWNLOAD_FILE_NAME + "/";

				if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
					mkDirPath = mkDirPath + "theme" + File.separator;
				}else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue() ){
					mkDirPath = mkDirPath + "widget" + File.separator;
				}else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue() ){
					mkDirPath = mkDirPath + "wp" + File.separator;
				}else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()  ){
					mkDirPath = mkDirPath + "icon" + File.separator;
				}

				//壁紙とアイコン以外はフォルダ作成要
				if(cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WP.getValue() && cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() && cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
					mkDirPath = mkDirPath + String.valueOf(cto.assetID) + File.separator;
					FileUtility.makeDirectory(mkDirPath);
				}

				//保存先ディレクトリ関連操作
				//この辺CNTへの接続で失敗する可能性があるので、CNTへ接続して問題なく落とせた後にフォルダを移動したほうがいいかも
				if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
//					//テーマのみ、現在のcurrentフォルダ内にデータがあったらそれをhistoryに入れ替える必要がある→後方でやってる
//					//現在設定中のものではないことを確認
//					if(!motoTheme.equals("") && !String.valueOf(cto.assetID).equals(motoTheme)){
//						//currentの中身のthemeをhistoryに移動
//						String motoPath = FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "theme" + File.separator + motoTheme + File.separator;
//						String sakiPath = FileUtility.getSkinRootPath(myContext) + "history" + File.separator + "theme" + File.separator + motoTheme + File.separator;
//						try {
//							FileUtility.copyFile(motoPath, sakiPath, true);
//
//						} catch (IOException e) {
//							e.printStackTrace();
//							return false;
//						}
//					}

				//テーマ以外はcurrent内の既存ファイルは追い出さない
				}else{

					//種別に合わせたフォルダ内に該当フォルダを作成
//					if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
//						FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "wp" + File.separator + String.valueOf(cto.assetID) + File.separator);
//					}else 
					if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
						FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "widet" + File.separator + String.valueOf(cto.assetID) + File.separator);
					}else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
						FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "icon" + File.separator);
//						FileUtility.makeDirectory(FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "icon" + File.separator + String.valueOf(cto.assetID) + File.separator);
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
				BufferedOutputStream bos = null;//TODO
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
					return FAILED_REASON_NETOWORK_ERROR;
				}
				filesize = lengthOfFile + 2;
				lengthOfFile += downloaded;
				// receive file
//				mybytearray = new byte[filesize];

				if(fileName.indexOf(".zip") != -1){
				    //zip解凍して保存
					ZipInputStream zIns = new ZipInputStream(connection.getInputStream());
					ZipEntry zipEntry = null;
					int zipLen = 0;

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
					if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
						fileName = ContentsFileName.individualWp1.getFileName();
						if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
							//従属系の場合、この後親アセットのCOR205のcall動作があり、そこで失敗する可能性もあるため、現状のファイルがあったらそれを退避しておく
							if(FileUtility.isExistFile(mkDirPath + File.separator + fileName)){
								FileUtility.copyFile(mkDirPath + File.separator + fileName, mkDirPath + File.separator + "taihi_wp_1.jpg", false);
							}
						}
					}else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
						fileName = String.valueOf(cto.assetID) + ".txt";
					}
					File saveFile = new File(mkDirPath, fileName);
					DebugLog.instance.outputLog("value", "ファイル保存path:" + saveFile.getAbsolutePath());
					BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
					bos = new BufferedOutputStream(new FileOutputStream(saveFile));

					byte[] zipBuffer = new byte[1024];
					int readByte = 0;

				    while(-1 != (readByte = is.read(zipBuffer))){
				    	bos.write(zipBuffer, 0, readByte);
				    }
				    bos.close();
				    bos = null;
				}


				connection.disconnect();

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "CatchException in doInBackground:" + e.getMessage());
			return FAILED_REASON_NETOWORK_ERROR;
		}

		//ここで以下
		/*
・ユーザが新規スキンを設定
 →新規レコードを追加、isExistはtrue（isFavoriteがついていたらそのレコードがある筈なのでそっちを使う
 →この時内部ストレージから押し出されるスキンがあったら、
  ・マイボックス上限内だったらisExistをfalseに、
  ・マイボックス上限を超えていたらレコードを削除
 →内部ストレージから押し出されるスキンがなかったら
  ・マイボックス上限を越えていたら一番古いレコードを1つ削除
		 */

		//従属アセットだった場合、マーケットアプリへテーマ本体の履歴を表示する為に本体アセットの仮DL処理を行なう。
    	if(!SPPUtility.isAuthThroughFlag && !carrierId.equals(SplashActivity.AUTH_CARRIER_10APPS)){
    		
    		if(parentDLSKIP == COR205_DL_COUNT_NOT_SKIP){
        		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() ||
        				cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue() ||
        				cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue() ||
        				cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){

        			try {
        				if(!downloadParentAssetForMarketHistory(themeTag)){
        					DebugLog.instance.outputLog("value", "doinBackground_親アセットのDL失敗");
        					//親の処理に失敗した場合の処理は？（この時点では子アセット（本来のDL対象）は正常にDLされていてカウント処理も終えている）
        					//→子アセットの本体ファイルを削除する
        					
       						//失敗ファイル削除
        					if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
        						FileUtility.delFile(new File(mkDirPath));  						
        					}else{
        						FileUtility.delFile(new File(mkDirPath + File.separator + fileName));
        					}
        					
        					
    						if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){

        						//退避ファイルがあったらそれを戻す
        						if(FileUtility.isExistFile(mkDirPath + File.separator + "taihi_wp_1.jpg")){
        							try {
										FileUtility.copyFile(mkDirPath + File.separator + "taihi_wp_1.jpg", mkDirPath + File.separator + fileName, true);
									} catch (IOException e) {}
        						}
        					}
        					
        					return FAILED_REASON_NETOWORK_ERROR;
        				}
        			} catch (JSONException e1) {
        				e1.printStackTrace();
        			}
        		}
    		}
    		
    		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
        		if(FileUtility.isExistFile(mkDirPath + File.separator + "taihi_wp_1.jpg")){
        			FileUtility.delFile( new File(mkDirPath + File.separator + "taihi_wp_1.jpg") );
        		}
    		}
    		
    	}
		
		
		//isExistが1のレコード数がmax値を越えていたら最古のを1つ消して、元々currentに入っていたものをhistoryに移動、

		//ダウンロード成功していたら、
		//テーマもしくは個別壁紙の場合：ダウンロードしたものはcurrentに、元々currentに入っていた該当アセットIDのフォルダはhistoryに移動
		//historyに移動した際に10点を越えていたらふるいものをDBから判断して削除（DB内のレコードも削除
		//ウィジェット・アイコンの個別の場合：最初からhistoryに保存

		//themeだった場合currentに既存のフォルダがあったらそれをhistoryに移動する（個別壁紙と個別アイコン、個別ウィジェットはcurrent内に保存するだけ）
		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
			if(!motoTheme.equals("")){
				DebugLog.instance.outputLog("value", "元のテーマのアセットID："+ motoTheme);

				String motoPath = FileUtility.getSkinRootPath(myContext) + "current" + File.separator + "theme" + File.separator + motoTheme + File.separator;
				//もしバッジコンテンツだったら移動ではなく削除
				BadgeDataAccess bAccess = new BadgeDataAccess(myContext);
				Array<BadgeDataDto> bArray = bAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), motoTheme);
				if(bArray != null && bArray.size > 0){
					FileUtility.delFile(new File(motoPath));
					bAccess.deleteById(Long.parseLong(motoTheme));
				}else{
					String sakiPath = FileUtility.getSkinRootPath(myContext) + "history" + File.separator + "theme" + File.separator + motoTheme + File.separator;
					try {
						FileUtility.copyFile(motoPath, sakiPath, true);

						//この時historyの内部が10点以上（履歴9点＋設定中の1点）になったら最古のものを削除する。→最古の判断はDBから。
						//マイボックス上限内だったらisExistをfalseにするだけ。
						checkMaxHistory(cto.contentsType);

					} catch (IOException e) {
						e.printStackTrace();
						return FAILED_REASON_NETOWORK_ERROR;
					}
				}

			}
//		}else if(contentsType == ContentsValue.CONTENTS_TYPE_WIDGET.getValue()
//				|| contentsType == ContentsValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
//				|| contentsType == ContentsValue.CONTENTS_TYPE_ICON.getValue()
//				|| contentsType == ContentsValue.CONTENTS_TYPE_ICON_IN_T.getValue() ){
//
		}


		return manageDB(mAccess);
	}
	
	private boolean downloadParentAssetForMarketHistory(String themeTag) throws JSONException{
		DebugLog.instance.outputLog("value", "downloadParentAssetForMarketHistory_parent_DL_start!_" + themeTag);
		//themeTagから親のassetIDを取得する
		long parentAssetId = 0L;
		
		ContentsDataDto parentCto = ContentsOperatorForCatalog.op.getContentsDataFromThemeTag(cto.themeTag);
		if(parentCto != null && parentCto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
			parentAssetId = parentCto.assetID;

		} else if(!this.parentAssetIdString.equals("")) {
			parentAssetId = Long.valueOf(this.parentAssetIdString);
		
		} else {
			
			if(!SPPUtility.checkNetwork(myContext)){
				return false;
			}

			//仮想日付
			String virtualDateString = "";
			Date virtualDate = null;
			if(SPPUtility.getVirtualDateOn(myContext)){
				DebugLog.instance.outputLog("value", "仮想日付チェック中");
				virtualDateString = SPPUtility.getVirtualDateString(myContext);
				DebugLog.instance.outputLog("value", "仮想日付＿" + virtualDateString);
				if(!virtualDateString.equals("")){
					virtualDate = SPPUtility.getVirtualDate(virtualDateString);
					if(virtualDate == null){
						virtualDateString = "";
						DebugLog.instance.outputLog("value", "フォーマット不正");
					}else{
						DebugLog.instance.outputLog("value", "parse成功");
					}
				}
			}else{
				DebugLog.instance.outputLog("value", "仮想日付未チェック");
			}
			
			CntApiConnector connector = new CntApiConnector(myContext);
			
			connector.setAccessEnviroment(myContext, false, CntApiConnector.API_SEARCH_ASSET_DIR)
			.addParameter(Param.env, connector.enviroment, false)
			.addParameter(Param.src, "", false);
			
			if(SPPUtility.CARRIER_CAMOUFLAGE_SET == 0){
				connector
				.addParameter(Param.uid, connector.UID, false)
				.addParameter(Param.deviceId, connector.deviceID, false)
				.addParameter(Param.osVersion, connector.osVersion, false)
				.addParameter(Param.carrierId, SPPUtility.getCarrierID(myContext), false);
			}else{
				connector
				.addParameter(Param.uid, SPPUtility.getCamoflaICCID(), false)
				.addParameter(Param.deviceId, SPPUtility.getCamoflaDeviceId(), false)
				.addParameter(Param.osVersion, SPPUtility.getCamoflaOsVersion(), false)
				.addParameter(Param.carrierId, SPPUtility.getCamoflaCarrierId(), false);
			}

			connector
			.addParameter(Param.providedSiteId, "aki", false)
			.addParameter(Param.tagGroup, "055", false)
			.addParameter(Param.tag, themeTag, false)
			.addParameter(Param.categoryId, "all", false)
			.addParameter(Param.idType, "asset", false);
			
			//仮想日付が正しく設定されていたら
			if(virtualDate != null){
				connector.addParameter(Param.isNotPublished, "1", false);
			}

			connector = connector.setParameter();
			connector.setTimeout(timeout - (int)(System.currentTimeMillis() - startTime));//タイムアウトはtask起動開始からの経過時間を抜いて設定
			
			//ここにresponseを入れる
			String responseBody = null;
			connector.connect();
			DebugLog.instance.outputLog("value", "接続開始時間:" + (System.currentTimeMillis() - startTime));
//			DebugLog.instance.outputLog("value", "connect:getResponseCode" + connector.getResponseCode());
//			DebugLog.instance.outputLog("value", "connect:getResponseMessage" + connector.getResponseMessage());
			responseBody = connector.getResponseBody();
			
			if(responseBody.equals("")) return false;
			
			DebugLog.instance.outputLog("value", "connect:responseBody" + responseBody);

			//返って来たAssetDetailからThemeのアセットのアセットIDを取得
			JSONObject rootObj = new JSONObject(responseBody);
			JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");
			JSONObject detailObj = null;
			
			//assetDetail(各個Asset情報)からテーマ本体アセットの本体ファイルのアセットIDを取得し、COR-205を叩く
			if(assetInfoAry != null && assetInfoAry.length() > 0){
				for(int i = 0; i < assetInfoAry.length(); i++){
					//assetDetailを探す
					detailObj = assetInfoAry.getJSONObject(i);
					
					//仮想日付が設定されていない or 仮想日付が設定されていて、提供開始日が仮想日付より過去のアセット
					if(virtualDate == null || (virtualDate != null && DownloadAllDataAsyncTask.isVirtualDateOkAsset(virtualDate, detailObj.toString()))){
					
					String assetidSub = detailObj.getString("assetId");

					JSONObject assetDetail = detailObj.getJSONObject("assetDetail");
					JSONArray fileArray = assetDetail.getJSONArray("fileAttribute");
					
					if(assetDetail.getString("assetTypeId").equals("ct_dnkp")){
						//assetidSubがアセットID
						DebugLog.instance.outputLog("value", "テーマ本体_" + assetidSub);
						
						SPPApiConnector sppConnector = new SPPApiConnector(myContext);
						

		        		SharedPreferences preferences = myContext.getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
		        		String token = preferences.getString(SPPUtility.PREF_KEY_ID_TOKEN, "");
		        		String accessToken = preferences.getString(SPPUtility.PREF_KEY_ACCESS_TOKEN, "");
		        		
		                sppConnector.setAccessEnviroment(myContext, SPPApiConnector.API_GET_DOWNLOAD_TOKEN)
		                	.addParameter(SPPApiConnector.Param.id_token, token, false)
		                	.addParameter(SPPApiConnector.Param.asset_id, assetidSub, false)
		                	.addParameter(SPPApiConnector.Param.env, sppConnector.enviroment, false)
		                	.addParameter(SPPApiConnector.Param.src, "aki", false)
		                	.addParameter(SPPApiConnector.Param.providedSiteId, "aki", false)
		                	.addParameter(SPPApiConnector.Param.dlskip, "0", false)
		                    .addParameter(SPPApiConnector.Param.download_from, "", false);
		                    
		                    
		                sppConnector = sppConnector.setParameter();
		                sppConnector.setAuthorization(accessToken, true);
						
						sppConnector.connect();
						String responseBodyFortokenURL = sppConnector.getResponseBody();
						DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseCode" + connector.getResponseCode());
						DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseMessage" + connector.getResponseMessage());

						if(responseBodyFortokenURL.equals("")) return false;

						DebugLog.instance.outputLog("value", "SPPApiConnector:responseBody" + responseBodyFortokenURL);

						}
					}
				}
			}
			

		}

		
		
		//取得したらそのアセットに対してDL処理（実際に保存はしなくてよい
		if(parentAssetId != 0L){
			DebugLog.instance.outputLog("value", "downloadParentAssetForMarketHistory_親アセットのアセットID_" + parentAssetId);
			
			SPPApiConnector sppConnector = new SPPApiConnector(myContext);
			
			SharedPreferences preferences = myContext.getSharedPreferences(SPPUtility.GET_TOKEN_PREF, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
			String token = preferences.getString(SPPUtility.PREF_KEY_ID_TOKEN, "");
			String accessToken = preferences.getString(SPPUtility.PREF_KEY_ACCESS_TOKEN, "");
			
	        sppConnector.setAccessEnviroment(myContext, SPPApiConnector.API_GET_DOWNLOAD_TOKEN)
	        	.addParameter(SPPApiConnector.Param.id_token, token, false)
	        	.addParameter(SPPApiConnector.Param.asset_id, Long.toString(parentAssetId), false)
	        	.addParameter(SPPApiConnector.Param.env, sppConnector.enviroment, false)
	        	.addParameter(SPPApiConnector.Param.src, "aki", false)
	        	.addParameter(SPPApiConnector.Param.providedSiteId, "aki", false)
	            .addParameter(SPPApiConnector.Param.download_from, "", false);
	            
	            
	        sppConnector = sppConnector.setParameter();
	        sppConnector.setAuthorization(accessToken, true);
			
			sppConnector.connect();
			String responseBodyFortokenURL = sppConnector.getResponseBody();
			DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseCode" + sppConnector.getResponseCode());
			DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseMessage" + sppConnector.getResponseMessage());

			if(responseBodyFortokenURL.equals("")) return false;

			DebugLog.instance.outputLog("value", "SPPApiConnector:responseBody" + responseBodyFortokenURL);


//			try {
//				if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
//					downloadUrl = SPPUtility.parseResponseForToken(tokenArray[2], responseBodyFortokenURL);
//				}else{
//					parseResponseForToken(responseBodyFortokenURL);
//				}
//			} catch (JSONException e) {
//				e.printStackTrace();
//				DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
//				return false;
//			}

		}else{
			return false;
		}
		
		return true;
	}
	
	private int manageDB(MyPageDataAccess mAccess){
		//通常のDB操作(レコードが存在したらisExistをtrueにして、存在してなかったら挿入
		
		cto.isExist = true;
		ContentsDataDto parentCto = null;

		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()){
			DebugLog.instance.outputLog("value", cto.assetID + "をマイページに入れる");
			cto.hasDownloadHistory = true;
		}else{
			DebugLog.instance.outputLog("value", cto.assetID + "はマイページに入れない");
			cto.hasDownloadHistory = false;

			//もしカタログからの起動で親アセットのctoを保持していたらそちらをダウンロード済みとして登録
			parentCto = ContentsOperatorForCatalog.op.getContentsDataFromThemeTag(cto.themeTag);
			if(parentCto == null) DebugLog.instance.outputLog("value", cto.assetID + "の親CTOが取れない");
			
			if(parentCto != null && parentCto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
				parentCto.hasDownloadHistory = true;
				if(mAccess.updateSkinIsMypage(parentCto, true) <= 0){
					parentCto.addedDate = "";
					mAccess.insertSkinData(parentCto, true);
				}
			}
		}
		Array<MyPageDataDto> recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(cto.assetID));
		if(recordArray != null && recordArray.size >= 1){
			//更新
			if(mAccess.updateSkinIsExist(cto, true) > 0){
				mAccess.updateSkinThemeSetDate(cto);
				DebugLog.instance.outputLog("value", "スキン保存レコード更新成功");
				checkMaxMyPage(cto.contentsType);
				
				if(parentCto == null && 
						(cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WP.getValue()
						 )){
			        Intent intent = new Intent(ContentsOperatorForCatalog.CHANGE_DB_STATE);
			        myContext.sendBroadcast(intent);	
				}

				return SUCCESS;
			}else{
				DebugLog.instance.outputLog("value", "スキン保存レコード更新失敗");
				return FAILED_REASON_NETOWORK_ERROR;
			}
		}else{
			//挿入
			if(mAccess.insertSkinData(cto, true) > 0){
				mAccess.updateSkinThemeSetDate(cto);
				DebugLog.instance.outputLog("value", "スキン保存レコード挿入成功：" + cto.themeTag);
				checkMaxMyPage(cto.contentsType);
				
				if(parentCto == null && 
						(cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
						&& cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WP.getValue()
						 )){
			        Intent intent = new Intent(ContentsOperatorForCatalog.CHANGE_DB_STATE);
			        myContext.sendBroadcast(intent);	
				}
				return SUCCESS;
			}else{
				DebugLog.instance.outputLog("value", "スキン保存レコード挿入失敗");
				return FAILED_REASON_NETOWORK_ERROR;
			}
		}
		
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
//		dialog.setProgress(values[0]);

//		super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled() {
		if(dialog != null) dialog.dismiss();
//		DebugLog.instance.outputLog("download", "onCancelled");
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		if(cto != null){
			callback.onFailedDownloadSkin(FAILED_REASON_NETOWORK_ERROR, cto.assetID);
		}else{
			callback.onFailedDownloadSkin(FAILED_REASON_NETOWORK_ERROR, 0);
		}
		super.onCancelled();
	}

	@Override
	protected void onPostExecute(Integer result) {
		if(dialog != null) dialog.dismiss();

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		//super.onPostExecute(result);

		DebugLog.instance.outputLog("value", "ZipDownloadAsyncTask:::onPostExecute:::result:" + result);
		if(result != SUCCESS){
			//失敗
			//失敗したのに該当データがあったりした場合は要削除 TODO

			if(cto != null){
				callback.onFailedDownloadSkin(result, cto.assetID);
			}else{
				callback.onFailedDownloadSkin(result, 0);
			}
			return;
		}

//		if(flagDownload){
			DebugLog.instance.outputLog("value", "download success!");

			ContentsOperatorForCatalog.op.setDataFromDB();
			ContentsOperatorForWidget.op.setDataFromDB();
			//ダウンロード成功していたら、
			//テーマもしくは個別壁紙の場合：ダウンロードしたものはcurrentに、元々currentに入っていたものはhistoryに移動
			//historyに移動した際に10点を越えていたらふるいものをDBから判断して削除
			//ウィジェット・アイコンの個別の場合：最初からhistoryに保存


			//以下はインストール挙動
//			// Intent生成
//			Intent intent = new Intent(Intent.ACTION_VIEW);
//			// MIME type設定
//			intent.setDataAndType(Uri.fromFile(new File("/data/data/"+ myContext.getPackageName() + "/files/" + DOWNLOAD_FILE_NAME)), "application/vnd.android.package-archive");
//			// Intent発行
//			myContext.startActivity(intent);

			if(cto != null && !String.valueOf(cto.assetID).equals("")){
				long assetIdForHistory = 0l, assetIdForMypage = 0l;
				if(oldCtoForHistory != null) assetIdForHistory = oldCtoForHistory.assetID;
				if(oldCtoForMyPage != null) assetIdForMypage = oldCtoForMyPage.assetID;

//				callback.onFinishedDownloadSkin(Long.valueOf(String.valueOf(cto.assetID)));
				callback.onFinishedDownloadSkin(cto, assetIdForHistory, assetIdForMypage);
			}else{
				callback.onFinishedDownloadSkin(null, 0, 0);
			}
//		}else{
//			//エラー処理
//			callback.onFailedDownloadSkin();
//		}
	}


	/**
	 * json解析
	 */
	private boolean parsePremiumResponse(String jsonStr){
		//レスポンスを解析してuser_profileを保存
		//あとここで入会退会判定の結果も取得しておく
		DebugLog.instance.outputLog("value", "SppCheclDisneyStyle//////////////////response:" + jsonStr);

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

//	private MyPageDataDto createMyPageDataDtoFromCto(ContentsDataDto cto){
//		MyPageDataDto dto = new MyPageDataDto();
//		/*
//this.assetID = 0;
//this.contentsType = 0;
//this.themeTag = "";
//this.isExist = false;
//this.isFavorite = false;
//this.addedDate = "";
//		 */
//		dto.assetID = cto.assetID;
//		dto.contentsType = cto.contentsType;
//		dto.themeTag = cto.themeTag;
//		dto.isExist = cto.isExist;
//		dto.isFavorite = cto.isFavorite;
//		dto.addedDate = cto.addedDate;
//
//		return dto;
//	}

	public static final int HISTORY_CONTENTS_MAX = 10;
	public static final int MYPAGE_CONTENTS_MAX = 50;
	private void checkMaxMyPage(int type){
		//このメソッドは追加後に呼ばれるので、この時点で上限を超えていたら必ず１つマイページから消す

		MyPageDataAccess mAccess = new MyPageDataAccess(myContext);
		Array<MyPageDataDto> mypageArrray = mAccess.findAllSkin_ForMyPage();

		Array<MyPageDataDto> inThemeArray = null;
		if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
				|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
			inThemeArray = mAccess.findAllSkin_inThemeIntoMypage();

			if(inThemeArray != null && inThemeArray.size > 0){
				for(MyPageDataDto dto : inThemeArray){
					if(dto.isFavorite || dto.isExist){
						DebugLog.instance.outputLog("value", "追加cto:" + dto.assetID);
						mypageArrray.add(dto);
					}
				}
			}
		}

		if(mypageArrray != null && mypageArrray.size > MYPAGE_CONTENTS_MAX){
			DebugLog.instance.outputLog("value", "checkMaxMyPage上限ごえ");

			mypageArrray.sort(new AddedDateComparatorAsc());

			//isExistがtrueでもマイページからは消す、がその場合レコードは残す。日付は更新しない。
			//新規スキンをマイページに追加することで既存のマイページコンテンツが１つ押し出される→押し出されたctoのisFavorite・hasDownloadHistoryはfalseに（isExistがtrueでない限りレコードから削除
			if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
				//TODO ここのクラス（ContentsOperatorForWidget）は後々きちんと場合分け
				oldCtoForMyPage = ContentsOperatorForWidget.op.getContentsDataFromAssetId(mypageArrray.get(0).assetID);

				//上記でctoが取れなかったら単独スキンが削除対象なのでDB内のレコード操作のみ行なう（ctoへの反映はカタログ側が別途行なう。
				if(oldCtoForMyPage == null) oldCtoForMyPage = new ContentsDataDto(mypageArrray.get(0));
			}else{
				oldCtoForMyPage = ContentsOperatorForCatalog.op.getContentsDataFromAssetId(mypageArrray.get(0).assetID);
			}

			oldCtoForMyPage.setDataFromDB(mypageArrray.get(0));
			oldCtoForMyPage.isFavorite = false;
			oldCtoForMyPage.hasDownloadHistory = false;
			oldCtoForMyPage.addedDate = "";
			if(oldCtoForMyPage.isExist){
				mAccess.updateSkinIsMypage(oldCtoForMyPage, false);
			}else{
				//isExistがtrueじゃなかったらレコードごと消す。
				mAccess.deleteById(oldCtoForMyPage.assetID);
			}

			//このアセットが単独テーマのものだったら、従属スキンでも消す必要があるものがあるかもなので、それもチェック
			if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
				if(oldCtoForMyPage.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
					//TODO ここのクラス（ContentsOperatorForWidget）は後々きちんと場合分け
					ContentsDataDto childCto = ContentsOperatorForWidget.op.getContentsDataFromThemeTag(oldCtoForMyPage.themeTag);
					if(childCto != null){
						childCto.isFavorite = false;
						childCto.hasDownloadHistory = false;
						childCto.addedDate = "";
						if(mAccess.updateSkinIsMypage(childCto, true) <= 0){
							mAccess.insertSkinData(childCto, true);
						}
					}
				}
			}

		}
	}

	ContentsDataDto oldCtoForHistory = null, oldCtoForMyPage = null;
	private void checkMaxHistory(int type){

		//historyの該当フォルダ内のフォルダが10以上（=DBのisExist:1のレコードが10以上）あったらDBから最古のものを判断してそのフォルダを削除しDBも更新
		MyPageDataAccess mAccess = new MyPageDataAccess(myContext);
		Array<MyPageDataDto> existArray = mAccess.findAllSkin_isExist(type);

		DebugLog.instance.outputLog("value", "checkMaxHistory:履歴件数:" + existArray.size);
		existArray.sort(new AddedDateComparatorAsc());

		//レコード追加前・使用履歴上限を超えていたら最古のデータを削除して、レコードのisExist周辺も操作
		if(existArray.size >= HISTORY_CONTENTS_MAX) {

			//ダウンロードしたのがテーマ従属スキンだった場合、履歴の中にそれと同一のthemeTagをもつレコードがあったら数は増えないので以下動作は行なわない。
			if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
					|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
				for(MyPageDataDto dto : existArray){
					if(cto.themeTag.equals(dto.themeTag)){
						return;
					}
				}
			}


			//一番古い履歴
			DebugLog.instance.outputLog("value", "checkMaxHistory:履歴最古:" + existArray.get(0).assetID);
			oldCtoForHistory = ContentsOperatorForCatalog.op.getContentsDataFromAssetId(existArray.get(0).assetID);
			oldCtoForHistory.setDataFromDB(existArray.get(0));

//			String delPath = "/data/data/" + myContext.getPackageName() + "/files/skin/history/";
			String delPath = FileUtility.getSkinRootPath(myContext) + "history" + File.separator;

			//テーマと壁紙以外は削除しない
			if(type == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
				delPath = delPath + "theme" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() || type == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
				delPath = delPath + "widget" + File.separator;
				return;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || type == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
				delPath = delPath + "wp" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || type == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() || type == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
				delPath = delPath + "icon" + File.separator;
				return;
			}

			//実体ファイルを削除
			FileUtility.delFile(new File(delPath + oldCtoForHistory.assetID + File.separator));

			//DB内の該当レコードのisExistを削除（日付は更新しない
			oldCtoForHistory.isExist = false;

			//マイページ上限内だったらマイページには表示するので、レコードのisExistをfalseにするだけ。上限を越えていたらレコードそのものを削除
			Array<MyPageDataDto> mypageArrray = mAccess.findAllSkin_ForMyPage();
			if(mypageArrray != null && mypageArrray.size >= MYPAGE_CONTENTS_MAX){

				//※今回セットするスキンが既にマイページ用のレコードとして存在していた場合、
				boolean isDo = true;
				for(int i = 0; i < mypageArrray.size; i++){
					if(cto.assetID == mypageArrray.get(i).assetID){
						isDo = false;
						i = mypageArrray.size;
					}
				}

				if(isDo){
					//マイページ内で一番古いレコードとoldCtoForHistory（使用履歴内で一番古いレコード）が同じだったらレコード削除
					mypageArrray.sort(new AddedDateComparatorAsc());
					if(mypageArrray.get(0).assetID == oldCtoForHistory.assetID){
						DebugLog.instance.outputLog("value", "checkMaxHistory:マイページでも最古:" + oldCtoForHistory.assetID);
						oldCtoForHistory.hasDownloadHistory = false;
						oldCtoForHistory.addedDate = "";
						mAccess.deleteById(oldCtoForHistory.assetID);
						return;
					}
				}

				DebugLog.instance.outputLog("value", "checkMaxHistory:マイページでは最古じゃなかった:" + oldCtoForHistory.assetID);
				//追加スキンがマイページ枠には無く（この時点で1つマイページから削除が必要）＋使用履歴最古のものがマイページに入っていなかった場合（isExist=trueだけでDBに残ってた場合）も消す必要アリ
				boolean isOldDtoinMyPage = false;
				for(int i = 0; i < mypageArrray.size; i++){
					if(oldCtoForHistory.assetID == mypageArrray.get(i).assetID){
						isOldDtoinMyPage = true;
						i = mypageArrray.size;
					}
				}
				if(!isOldDtoinMyPage){
					DebugLog.instance.outputLog("value", "checkMaxHistory:マイページから外れてる履歴最古:" + oldCtoForHistory.assetID);
					oldCtoForHistory.hasDownloadHistory = false;
					oldCtoForHistory.addedDate = "";
					mAccess.deleteById(oldCtoForHistory.assetID);
					return;
				}

			}else{
				DebugLog.instance.outputLog("value", "checkMaxHistory:マイページはまだ上限じゃない:" + oldCtoForHistory.assetID);
			}
			mAccess.updateSkinIsExist(oldCtoForHistory, false);
			return;

		}

	}

	private String downloadUrl = "";//最初はトークンなしのURLを通常に取得して、それを元にトークンありのURLを別途取得して代入
	private String[] tokenArray = null;
	private String fileName = "";
	private String themeTag = "";

	//専用変数に値を代入
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
			JSONArray tagAry = detail.getJSONArray("assetTag");
			if(tagAry != null){
				for(int j = 0; j < tagAry.length(); j++){
					JSONObject tagItem = tagAry.getJSONObject(j);
					if(tagItem.getString("tagGroupNameId").equals("055")){
						themeTag = tagItem.getString("tagId");
						DebugLog.instance.outputLog("value", "tagId:" + tagItem.getString("tagId"));
						DebugLog.instance.outputLog("value", "tagId:" + themeTag);
					}
				}
			}

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


	private void parseResponseForToken(String response) throws JSONException{
		JSONObject rootObj = new JSONObject(response);
		JSONObject download_token = rootObj.getJSONObject("download_token");
		downloadUrl = download_token.getString("download_url");
	}
	
	private int parseResponseForParentContentsList(String response) throws JSONException {
		JSONObject root = new JSONObject(response);
		
		DebugLog.instance.outputLog("value", "parseResponseForSelfContentsList    " + root.toString());
		
		JSONArray assetInfo = root.getJSONArray("assetInfo");
		
		for(int i = 0; i < assetInfo.length(); i++){
			
			JSONObject detailRoot = assetInfo.getJSONObject(i);
			DebugLog.instance.outputLog("value", "parseResponseForSelfContentsList    " + detailRoot.toString());

			JSONObject detail = detailRoot.getJSONObject("assetDetail");
			//テーマ
			if(detail.getString("assetTypeId").equals("ct_dnkp")){
				
				parentAssetIdString = detailRoot.getString("assetId");
				
				JSONObject price = detailRoot.getJSONObject("price");
				boolean isPurchased = price.getBoolean("is_purchased");
				
				if(isPurchased){
					return COR205_DL_COUNT_SKIP;
				}else{
					return COR205_DL_COUNT_NOT_SKIP;
				}
			}
			
		}
		
		return 0;
	
	}
	
	private int parseResponseForSelfContentsList(String response) throws JSONException {
		JSONObject root = new JSONObject(response);
		
		DebugLog.instance.outputLog("value", "parseResponseForSelfContentsList    " + root.toString());
		
		JSONArray assetInfo = root.getJSONArray("assetInfo");
		
		for(int i = 0; i < assetInfo.length(); i++){
			
			JSONObject detailRoot = assetInfo.getJSONObject(i);
			DebugLog.instance.outputLog("value", "parseResponseForSelfContentsList    " + detailRoot.toString());

			JSONObject detail = detailRoot.getJSONObject("assetDetail");
			//自身
			if(detailRoot.getString("assetId").equals(Long.toString(cto.assetID))){
				
				JSONObject price = detailRoot.getJSONObject("price");
				boolean isPurchased = price.getBoolean("is_purchased");
				
				if(isPurchased){
					return COR205_DL_COUNT_SKIP;
				}else{
					return COR205_DL_COUNT_NOT_SKIP;
				}
			}
			
		}
		
		return 0;
	}


	@Override
	public void onCancel(DialogInterface dialog) {
		this.cancel(true);
	}

}
