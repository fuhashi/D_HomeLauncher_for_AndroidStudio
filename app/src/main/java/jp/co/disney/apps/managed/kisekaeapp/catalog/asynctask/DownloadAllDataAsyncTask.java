package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.CntApiConnector.Param;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsCharaValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BaseDatabase;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.DataBaseParam;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.badlogic.gdx.utils.Array;


/**
 * 個別配信しているきせかえ用データを取得し必要な情報のみ取り出しないし保存する
 * @author sagara
 *
 */
public class DownloadAllDataAsyncTask extends
		AsyncTask<Integer, Integer, Boolean>{
//		AsyncTask<String, Integer, Boolean>{
//		AsyncTask<String, Integer, Boolean> implements OnCancelListener{

//	private ProgressDialog dialog = null;
	Context myContext;
	long startTime = 0;
	private DownloadAllDataTaskCallback callback;
//	private String assetId = "";
//	private String entity_type = "";

	private Array<ContentsDataDto> ctoArray = null;
//	private int timeout = 30 * 1000;

	Timer   mTimer   = null;

	public DownloadAllDataAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (DownloadAllDataTaskCallback) context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		isExecuting = true;
//		dialog = new ProgressDialog(myContext);
//		// タイトル, 本文を設定
//		dialog.setTitle("Downloading...");
//		// スタイルを設定
//		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//		dialog.setCancelable(true);
//		dialog.setOnCancelListener(this);
//		dialog.setCanceledOnTouchOutside(false);
//		dialog.setMax(100);
//		dialog.setProgress(0);
//		dialog.show();


	}

	private void callCancel(){
		cancel(true);
	}

	private boolean isExecuting = false;
	public void startTimer(final long timeoutTime){

		if(!isExecuting) return;
		//ここでタイマーが開始できるようにする。また、場合によってはタイマーはtask起動時からスタートさせる（チュートリアルが無い＝初回起動ではない場合
		//チュートリアル表示がない場合（初回起動じゃない場合）はtaskスタートと同時にこのメソッドをActivity側から呼んでもらう。
		DebugLog.instance.outputLog("value", "start timer from SplashActivity");

		startTime = System.currentTimeMillis();

		mTimer = new java.util.Timer(true);
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if((System.currentTimeMillis() - startTime) >= timeoutTime){
					DebugLog.instance.outputLog("value", "timeout!!!!!");
					mTimer.cancel();
					mTimer = null;
					callCancel();
				}
				DebugLog.instance.outputLog("value", "time:" + (System.currentTimeMillis() - startTime));
			}

		}, 100, 100);

	}


	private boolean flagDownload = false;
	private int contentsDetailType = 0;//全データ取得対象タイプ（0=カタログ起動時、単独配信コンテンツ全て

	/**
	 * return t（成功）/f（失敗）
	 */
	@Override
	protected Boolean doInBackground(Integer... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "DownloadAllDataAsyncTask_doInBackground");

		if(arg0.length >= 1){
//			if(arg0[0] == 0){
//				isPremiumuUser = false;
//			}else{
//				isPremiumuUser = true;
//			}

//			if(arg0[0] != 0) contentsDetailType = arg0[1];
			if(arg0.length >= 2) contentsDetailType = arg0[1];
		}else{
			//パラメータ指定が足りなかったらcancel
			return false;
		}

		String dirPath = "";
		if(contentsDetailType == 0){
			dirPath = FileUtility.getThumbnailsRootPath(myContext);
		}else{
			dirPath = FileUtility.getThumbnailsCachePath(myContext);
		}

		makeDirectory(dirPath);
		
		DebugLog.instance.outputLog("value", "DownloadAllDataAsyncTask_doInBackground_contentsDetailType=" + contentsDetailType);

		///////////////////////////////DL処理開始
		int searchOffset = 0;
		while (searchOffset != -1) {
			try {

				if(isCancelled()){
					return false;
				}
				//Thread.sleep(1000);
				//publishProgress((i+1) * 10);
				//オフラインチェック
				if(!SPPUtility.checkNetwork(myContext)){
					return false;
				}


				//キャンセルチェック
				if(isCancelled()) return false;

				//アセット属性情報取得APIを使って取得
				CntApiConnector connector = new CntApiConnector(myContext);
				
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

				if(contentsDetailType == 0){
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
					.addParameter(Param.assetTypeId, "ct_dnkp ct_dnksw ct_dnksi", true)
					.addParameter(Param.providedSiteId, "aki", false)
					.addParameter(Param.categoryId, "all", false)
					.addParameter(Param.idType, "asset", false)
					.addParameter(Param.searchLimit, "64", false)
					.addParameter(Param.searchOffset, String.valueOf(searchOffset), false)

					.addParameter(Param.division, "0", false);
					
					//仮想日付が正しく設定されていたら
					if(virtualDate != null){
						connector.addParameter(Param.isNotPublished, "1", false);
					}

				}else if(contentsDetailType == ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue()){
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
					.addParameter(Param.assetTypeId, "ct_dnksw ct_dnkpw", true)
					.addParameter(Param.providedSiteId, "aki", false)
					.addParameter(Param.categoryId, "all", false)
					.addParameter(Param.idType, "asset", false)
					.addParameter(Param.searchLimit, "64", false)
					.addParameter(Param.searchOffset, String.valueOf(searchOffset), false)
					//バッテリータグ
					.addParameter(Param.tagGroup, "057", false)
					.addParameter(Param.tag, "057001", false)

					.addParameter(Param.division, "0", false);
					
					//仮想日付が正しく設定されていたら
					if(virtualDate != null){
						connector.addParameter(Param.isNotPublished, "1", false);
					}

				}else if(contentsDetailType == ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue()){
					
					
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
					.addParameter(Param.assetTypeId, "ct_dnksi ct_dnkps", true)
					.addParameter(Param.providedSiteId, "aki", false)
					.addParameter(Param.categoryId, "all", false)
					.addParameter(Param.idType, "asset", false)
					.addParameter(Param.searchLimit, "64", false)
					.addParameter(Param.searchOffset, String.valueOf(searchOffset), false)

					.addParameter(Param.division, "0", false);
					
					//仮想日付が正しく設定されていたら
					if(virtualDate != null){
						connector.addParameter(Param.isNotPublished, "1", false);
					}

				}

				connector = connector.setParameter();
				connector.setTimeout(30 * 1000);

				//ここにresponseを入れる
				String responseBody = null;
				DebugLog.instance.outputLog("value", "connect:開始オフセット:" + searchOffset);
				DebugLog.instance.outputLog("value", "connect:接続開始までにかかった時間:" + (System.currentTimeMillis() - startTime));
				startTime = System.currentTimeMillis();

				connector.connect();

				//キャンセルチェック
				if(isCancelled()) return false;

				DebugLog.instance.outputLog("value", "connect:接続終了までにかかった時間:" + (System.currentTimeMillis() - startTime));
//				DebugLog.instance.outputLog("value", "connect:getResponseCode" + connector.getResponseCode());
//				DebugLog.instance.outputLog("value", "connect:getResponseMessage" + connector.getResponseMessage());

				responseBody = connector.getResponseBody();

				if(responseBody.equals("")) return false;

				DebugLog.instance.outputLog("value", "connect:responseBody" + responseBody);

				//キャンセルチェック
				if(isCancelled()) return false;

				/////////此処からjson解析

				//それぞれのアセットのjsonに分ける
				Array<String> assetDetail = new Array<String>();

				JSONObject rootObj = new JSONObject(responseBody);
				JSONArray assetInfoAry = null;
				try {
					assetInfoAry = rootObj.getJSONArray("assetInfo");
				} catch (Exception e) {
					if(searchOffset > 0){
						return true;
					}else{
						return false;
					}
				}
				//assetDetail(これが各個Asset情報)
				for(int i = 0; i < assetInfoAry.length(); i++){
					//assetDetailを探す
					assetDetail.add(assetInfoAry.getJSONObject(i).toString());
				}

				//値が取れていたら
				if(assetDetail.size > 0){
					if(ctoArray == null)	ctoArray = new Array<ContentsDataDto>();
				}else{
					return false;
				}

				//64個全体をparse（ここで64個未満だったらループはここで終わり
				for(int i = 0; i < assetDetail.size; i++){

					//バッジコンテンツ以外を処理する
					if(!isBadgeContents(assetDetail.get(i))){
						//TODO 仮想日付が設定されていない or 仮想日付が設定されていて、提供開始日が仮想日付より過去のアセット
						if(virtualDate == null || (virtualDate != null && isVirtualDateOkAsset(virtualDate, assetDetail.get(i)))){
							
							//飛ばさないアセット
							ContentsDataDto cto = new ContentsDataDto();
							//アセット情報から必要情報取得してctoに入れる
							//タグから必要情報取得してctoに入れる
							cto = getAssetDataFromJson(cto, assetDetail.get(i));

							//キャンセルチェック
							if(isCancelled()) return false;

							//サムネ画像が内部ストレージに無かったら落としてきて保存、あったらファイルの更新日付をAPI側の更新日付と確認・比較してAPI側が新しかったら保存
							//まず当該アセットで使用するサムネ画像のファイル名を取得
							qpNum = 1;
							String[] thumbsDetail = getThumbsDetailFromJson(assetDetail.get(i));
							cto.qpNum = qpNum;

							//キャンセルチェック
							if(isCancelled()) return false;

							if(!thumbsDetail[0].equals("") && !thumbsDetail[0].equals("")){
//								File dir = new File(dirPath);
								File thumbsFile = new File(dirPath + cto.assetID + ".etc1");

								//そのファイルが該当フォルダ内に存在するか確認
								boolean doDownload = false;
								if(!thumbsFile.exists()){
									//存在してなかったら、テーマか単独系コンテンツのみサムネを落としてくる判断→そもそもCNT接続時にサムネ保存対象コンテンツのみ取れるようにしているので、この条件分岐はいらないかも 0717
//									if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
//											|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()
//											|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_ICON.getValue()
//											|| cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()){
										doDownload = true;
//									}

								}else{
									//存在してたら更新日を比較
									long lastModified = thumbsFile.lastModified();
									Date test = new Date(lastModified);
									DebugLog.instance.outputLog("value", "更新日付:" + test.toString());
									//assetDetailのupDateがアセット更新日付
									JSONObject root = new JSONObject(assetDetail.get(i));
									JSONObject detail = root.getJSONObject("assetDetail");
									String modiDate = detail.getString("upDate");
									
									//文字列をフォーマット指定でlong変換
									Date modiD = SPPUtility.getDateCNTFormat(modiDate);
									if(modiD.getTime() > lastModified) doDownload = true;

									if(!doDownload){
										//提供開始日取得
										String supStartDate = "";
										JSONArray siteArray = detail.getJSONArray("providedSite");
										for(int s = 0; s < siteArray.length(); s++){
											JSONObject site = siteArray.getJSONObject(s);
											if(site.getString("providedSiteId").equals("aki")){
												supStartDate = site.getString("supStartDate");
											}
										}
										
										Date supD = null;
										if(!supStartDate.equals("")){
											supD = SPPUtility.getDateCNTFormat(supStartDate);							
										}
										if(supD != null){
											if(supD.getTime() > modiD.getTime()){
												DebugLog.instance.outputLog("value", "提供開始日新しい___提供開始日_" + supD.toString() + "/更新日_" + modiD.toString());
												if(supD.getTime() > lastModified) doDownload = true;
											}else{
												DebugLog.instance.outputLog("value", "提供開始日古い___提供開始日_" + supD.toString() + "/更新日_" + modiD.toString());
											}
										}
									}


								}

								if(!doDownload){
									//fileが壊れていないか確認
									DebugLog.instance.outputLog("value", thumbsFile.getAbsolutePath() + "が壊れていないか確認");
									if(!checkThumbAllGreen(thumbsFile)){
										DebugLog.instance.outputLog("value", thumbsFile.getAbsolutePath() + "が壊れているので削除して再取得");
										if(thumbsFile.exists()) FileUtility.delFile(thumbsFile);
										doDownload = true;
									}
								}

								//なかったら保存、あったら更新日を見て比較、古かったらCNTから新しいのを保存
								if(doDownload){
									//保存に失敗したら終了
									if(!downloadThumbnails(thumbsDetail[0], thumbsDetail[1], thumbsDetail[2])){
										return false;
									}
								}

							}


							//諸々終わったらarrayに入れる
							ctoArray.add(cto);

	/*
	 *
	http://staging.sslib.disney.co.jp/JsonAPI/V3.0/searchAsset?env=qa&src=&uid=8981300022570981310&deviceId=305SH&osVersion=Android+4.4&assetTypeId=ct_sstyle&providedSiteId=adsdss&categoryId=all&carrierId=sbm&idType=asset&division=0&
	File thumbsFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/Thumbs_" + parseResult[0] + ".zip");

	 */

							
						}

					}

					//キャンセルチェック
					if(isCancelled()) return false;

				}

//				//実体ファイルのファイル名（拡張子含む）もresponseから解析して取得した方がいい？→アセットに対して実体ファイルが一種という前提
//
//				//ここでparser呼び出す
////				GetZipDownloadUrlParser parser = new GetZipDownloadUrlParser();
////				String downloadUrl;
////				String[] tokenArray = null;
//				try {
////					downloadUrl = parser.getObjectFromJson(responseBody, artBoxNum, myContext);
////					tokenArray = parser.getObjectArrayFromJson(responseBody, 3, myContext);
//					parseResponse(responseBody);
//					DebugLog.instance.outputLog("value", "connect:downloadURL:" + downloadUrl);//取得できてる
//
//				} catch (JSONException e) {
//					e.printStackTrace();
//					DebugLog.instance.outputLog("value", "jsonError:" + e.getMessage());
//					return false;
//				}
//
//
////				SPPApiConnector sppConnector = new SPPApiConnector(myContext);
////				sppConnector.setAccessEnviromentAccessKey(myContext, tokenArray[1], tokenArray[2], SPPApiConnector.API_GET_DOWNLOAD_TOKEN_UNTIED)
////					.addParameter(SPPApiConnector.Param.app_id, SPPUtility.getAppId(myContext), false)
////					.addParameter(SPPApiConnector.Param.iccid, sppConnector.UID, false)
////					.addParameter(SPPApiConnector.Param.download_from, "", false)
////					.addParameter(SPPApiConnector.Param.site_cd, "abs", false)
//////					.addParameter(SPPApiConnector.Param.link_status, "0", false)
////					.addParameter(SPPApiConnector.Param.url, downloadUrl, true);
////				sppConnector = sppConnector.setParameter();
////				sppConnector.connect();
////				String responseBodyFortokenURL = sppConnector.getResponseBody();
////				DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseCode" + connector.getResponseCode());
////				DebugLog.instance.outputLog("value", "SPPApiConnector:getResponseMessage" + connector.getResponseMessage());
////				DebugLog.instance.outputLog("value", "SPPApiConnector:responseBody" + responseBodyFortokenURL);
//
//				//保存先ディレクトリ作成
//				String mkDirPath = "/data/data/" + myContext.getPackageName() +  "/files/sample/";
////				String mkDirPath = "/data/data/" + myContext.getPackageName() +  "/files/art_images/" + DOWNLOAD_FILE_NAME + "/";
//				DebugLog.instance.outputLog("value", "mkdir:" + mkDirPath);
//				File f = new File(mkDirPath);
//				f.getParentFile().mkdir();
//				f.mkdir();
//
//				//String path = myContext.getResources().getString(R.string.URL);
//				//String path = "http://encode.jp/sgr/" + DOWNLOAD_FILE_NAME;
//				String path = downloadUrl;
//				int filesize = 0; // file size temporary hard coded
//				long start = System.currentTimeMillis();
//				int bytesRead;
//				int current = 0;
//				int downloaded = 0;
//				int timeout = 100000;
//				FileOutputStream fos = null;
//				BufferedOutputStream bos = null;
//
//				URL url = new URL(path);
//				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//				connection.setConnectTimeout(timeout);
//				connection.setReadTimeout(timeout);
//				connection.setRequestMethod("GET");
//				// ---------set request download--------
//				connection.setDoInput(true);
//				connection.connect();
//				// --------------------------------------
//				int lengthOfFile = connection.getContentLength();
//				if (lengthOfFile <= 0) {
//					connection.disconnect();
//					flagDownload = false;
//					return false;
//				}
//				filesize = lengthOfFile + 2;
//				lengthOfFile += downloaded;
//				// receive file
//
//				File saveFile = new File("/data/data/" + myContext.getPackageName() +  "/files/sample/", fileName);
//
//				boolean isShukushou = false;
//				if(!isShukushou){
//					BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
//					OutputStream zipOs = new FileOutputStream(saveFile);
//
//					BufferedOutputStream zipBos = new BufferedOutputStream(zipOs);
//
//					byte[] zipBuffer = new byte[1024];
//					int readByte = 0;
//
//				    while(-1 != (readByte = is.read(zipBuffer))){
//				    	zipBos.write(zipBuffer, 0, readByte);
//				    }
//				    zipBos.close();
//
//				}else{
//					FileOutputStream os = null;
//					os = new FileOutputStream(saveFile);
//
//					Bitmap bitmap = null;
//					bitmap = createBitmap(connection.getInputStream());
//
//					 // 縮小してpngで保存
//					if(fileName.indexOf(".png") != -1){
//						 bitmap.compress(CompressFormat.PNG, 100, os);
//					}else{
//						 bitmap.compress(CompressFormat.JPEG, 100, os);
//					}
//					 // 保存処理終了
//					os.close();
//					bitmap.recycle();
//					bitmap = null;
//				}
//
//				connection.disconnect();

				flagDownload = true;


				//最後まで見終わった時に値が64だったら残りがあるかもしれないので次を見に行く
				if(assetDetail.size >= 64){
					searchOffset += 64;
				//64未満で終わったらこの取得で終わり
				}else{
					searchOffset = -1;
				}


		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "CatchException in doInBackground:" + e.getMessage());
			searchOffset = -1;
			return false;
		}
	}

		//ここでDB関連処理。もともとあったら開いたものからお気に入りなどのチェックをする。
		//DBがあったら、DB内に存在してるアセットIDのレコードはお気に入りonoff・追加日付を取得して、ctoに入れる。
//		String dbpath = "/data/data/" + myContext.getApplicationContext().getPackageName() + "/databases/" + BaseDatabase.DATABASE_NAME;
		String dbpath = myContext.getDatabasePath(BaseDatabase.DATABASE_NAME).getAbsolutePath();
		DebugLog.instance.outputLog("value5", "doinBackground___" + dbpath);
		if(FileUtility.isExistFile(dbpath)){
			DebugLog.instance.outputLog("value", "___既存DBあり");
			//DBがあったら取得してチェック
			MyPageDataAccess mAccess = new MyPageDataAccess(myContext);

			////////////////////////
			for(int i = 0; i < ctoArray.size; i++){

				Array<MyPageDataDto> recordArray = null;
				if(ctoArray.get(i).contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
						|| ctoArray.get(i).contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
						|| ctoArray.get(i).contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
						|| ctoArray.get(i).contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()){
					//単独コンテンツ
					//該当のアセットIDを持つレコードがDB内にあったら反映開始
					recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(((ContentsDataDto)ctoArray.get(i)).assetID));
				}else{
					//従属コンテンツ
					//該当のテーマタグを持つレコードがDB内にあったら反映開始
//					recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_THEME_TAG.getParam(), String.valueOf(((ContentsDataDto)ctoArray.get(i)).themeTag));
					String[] columns = { DataBaseParam.COL_THEME_TAG.getParam(), DataBaseParam.COL_CONTENTS_TYPE.getParam() };
					String[] values = { ctoArray.get(i).themeTag, String.valueOf(ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()) };
					recordArray = mAccess.findMyPageDataDtoFromDB(columns, values);
					if(recordArray != null){
						DebugLog.instance.outputLog("value", "recordArray:" + recordArray.size);
					}else{
						DebugLog.instance.outputLog("value", "recordArray is null");
					}
				}

//				Array<MyPageDataDto> recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(((ContentsDataDto)ctoArray.get(i)).assetID));
				if(recordArray != null && recordArray.size > 0){
					//DB内の既存・お気に入り関連情報を反映させる
					ctoArray.get(i).isExist = recordArray.get(0).isExist;
					ctoArray.get(i).isFavorite = recordArray.get(0).isFavorite;
					ctoArray.get(i).hasDownloadHistory = recordArray.get(0).hasDownloadHistory;
					ctoArray.get(i).addedDate = recordArray.get(0).addedDate;
					ctoArray.get(i).themeSetDate = recordArray.get(0).themeSetDate;
				}
			}

		}else{
			DebugLog.instance.outputLog("value", "___既存DBなし");
			//→つまり初期状態なので現時点でDBは必要ない（DBにには既存かお気に入りに入っているかのどちらかのアセットが登録される

		}

		return true;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
//		dialog.setProgress(values[0]);

		//super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled() {
//		diaDebugLog.instance.outputLogismiss();
//		DebugLog.instance.outputLog("download", "onCancelled");

		isExecuting = false;
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		callback.onFailedAllDataDownload();
		super.onCancelled();
	}

	@Override
	protected void onPostExecute(Boolean result) {
//		diaDebugLog.instance.outputLogismiss();

		isExecuting = false;

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		//super.onPostExecute(result);

		if(!result){
			//ダウンロード失敗
//			DebugLog.instance.outputLog("download", "onPostExecute:resultFalse");
			callback.onFailedAllDataDownload();
//			callCancel();
			return;
		}
		DebugLog.instance.outputLog("value", "DownloadAllDataAsyncTask:::onPostExecute:::result:" + result);

		if(flagDownload){
			DebugLog.instance.outputLog("value", "download success!:" + (System.currentTimeMillis() - startTime));


			//以下はインストール挙動
//			// Intent生成
//			Intent intent = new Intent(Intent.ACTION_VIEW);
//			// MIME type設定
//			intent.setDataAndType(Uri.fromFile(new File("/data/data/"+ myContext.getPackageName() + "/files/" + DOWNLOAD_FILE_NAME)), "application/vnd.android.package-archive");
//			// Intent発行
//			myContext.startActivity(intent);

//			//あとcolorSet動作
//			GetColorParser colorParser = new GetColorParser();
//			try {
//				SettingsAppAdapter.instance.setTotalNumOfSelectedArt(artBoxNum, myContext);
//				colorParser.getObjectFromJson(jsonStrForColor, SettingsAppAdapter.instance.getTotalNumOfSelectedArt(myContext), myContext);
//				SettingsAppAdapter.instance.setSelectedArt(artId, SettingsAppAdapter.instance.getTotalNumOfSelectedArt(myContext), myContext);
////				SettingsAppAdapter.instance.setCurrentArt(SettingsAppAdapter.instance.getTotalNumOfSelectedArt(myContext), myContext);
//
//				callback.onFinishedDownload();
//			} catch (JSONException e) {
//				e.printStackTrace();
//				DebugLog.instance.outputLog("value", "json exception:" + e.getMessage());
//				//DLしたzip削除
//		        File file = new File("/data/data/" + myContext.getPackageName() +  "/files/art_images/");
//		        File[] files = file.listFiles();
//		        for(int k = 0; k < files.length; k++){
//		        	//該当zipがあったら削除
////		        	if(files[k].getAbsolutePath().indexOf(String.format("%1$05d", artNum)) != -1 && files[k].getAbsolutePath().endsWith("zip")){
//		        	if(files[k].getAbsolutePath().indexOf(artId) != -1 && files[k].getAbsolutePath().endsWith("zip")){
//		        		files[k].delete();
//		        		k = files.length;
//		        	}
//		        }
//
////		        DebugLog.instance.outputLog("download", "onPostExecute:jsonException");
//				callback.onFailedDownloadApp();
//			}

			callback.onFinishedAllDataDownload(ctoArray);
		}else{
			//エラー処理
//			DebugLog.instance.outputLog("download", "onPostExecute:flagDownloadFalse");
			callback.onFailedAllDataDownload();
		}
	}

	///////////////Bitmap

	private byte[] decodeByteArray(InputStream is) throws IOException{
		//一旦byteArrayにする
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] bufferArray = new byte[1024];

		while (true) {
			int len = is.read(bufferArray);
		if(len < 0) {
		    break;
		}
		bout.write(bufferArray, 0, len);
		}
		byte[] returnByteArray = bout.toByteArray();
		is.close();

		return returnByteArray;

	}

//	private Bitmap createBitmap(InputStream is) {
//		BitmapFactory.Options options = new BitmapFactory.Options();
//		options.inJustDecodeBounds = true;//Bitmapをメモリに展開しない
//
//		//inputstreamを複数回使うので、一度ByteArrayにして使用する
//		try {
//			byte[] imageArray = decodeByteArray(is);
//
//			BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length, options);
//
//			int imageHeight = options.outHeight;
//			int imageWidth = options.outWidth;
//			String imageType = options.outMimeType;
//
//			//とりあえず固定で縦幅横幅を1/2に
//			options.inSampleSize = 2;
//			options.inJustDecodeBounds = false;
//			return BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length, options);
//
//		} catch (IOException e) {
//			e.printStackTrace();
//			return null;
//		}
//
//
//	}

//	String dirPath = "";
	private void makeDirectory(String dirPath){

		DebugLog.instance.outputLog("value", "mkdir:" + dirPath);
		File f = new File(dirPath);
		f.getParentFile().mkdir();
		f.mkdir();

	}

	///////////////専用parser


	private String downloadUrl = "";//最初はトークンなしのURLを通常に取得して、それを元にトークンありのURLを別途取得して代入
	private String[] tokenArray = null;
	private String fileName = "";
	private int qpNum = 1;


	//あとで統合する。
	private String[] getThumbsDetailFromJson(String jsonStr) throws JSONException {
//		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::");
//		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::jsonStr:" + jsonStr);
		String[] returnArray = {"", "", ""};//0：更新フラグ、1:URL、2:VersionCode

		JSONObject root = new JSONObject(jsonStr);
		returnArray[2] = root.getString("assetId");
		JSONObject rootObj = root.getJSONObject("assetDetail");
		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::" + returnArray[2]);

		JSONArray fileArray = rootObj.getJSONArray("fileAttribute");
		for(int i = 0; i < fileArray.length(); i++){
			JSONObject childObj = fileArray.getJSONObject(i);

			// isThumbnailが3のファイルが「その他」（端末グループごとではない
			//　本体カタログとそれ以外で保存対象サムネイルが違う
			boolean isDownloadThumb = false;
			if(contentsDetailType == 0){
				if( childObj.getString("isThumbnail").equals("3") && childObj.getString("description").indexOf("{\"qp_num") == 0 ){
					isDownloadThumb = true;
				}
			}else{
				if( childObj.getString("isThumbnail").equals("3") && childObj.getString("description").equals("isPickerThumb") ){
					isDownloadThumb = true;
				}
			}
			if(isDownloadThumb){
				//ファイル名と
				returnArray[0] = childObj.getString("fileName");
				//ダウンロードURL取得
				returnArray[1] = childObj.getString("downloadUrl");

				DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::filename:" + returnArray[0]);

				if(contentsDetailType == 0){
					//クイックプレビュー画像枚数の取得
					JSONObject qpObject = new JSONObject(childObj.getString("description"));
					qpNum = qpObject.getInt("qp_num");
				}else{
					qpNum = 1;
				}

				//とりあえず一個の想定（今後増えるかも）なので一個取ったら終了
				 i = fileArray.length();
			}
		}

		return returnArray;
	}

//	class ThemeContains {
//		long assetID;
//		int assetType;
//		String themeTag;
//
//		ThemeContains(long assetID, int assetType, String groupTag) {
//			super();
//			this.assetID = assetID;
//			this.assetType = assetType;
//			this.themeTag = groupTag;
//		}
//
//	}
//	private Array<ThemeContains> ThemeContainsArray = new Array<DownloadAllDataAsyncTask.ThemeContains>();


	private ContentsDataDto setCharacterInfo(ContentsDataDto cto, JSONArray charaArray) throws JSONException{
		for(int i = 0; i < charaArray.length(); i++ ){
			JSONObject tag = charaArray.getJSONObject(i);

			//characterIdを取得
			if(tag.getInt("characterId") == 1){
				DebugLog.instance.outputLog("value", "ミッキーを登録");
				cto.chara.add(ContentsCharaValue.CHARA_MICKEY);

			}else if(tag.getInt("characterId") == 2){
				DebugLog.instance.outputLog("value", "ミニーを登録");
				cto.chara.add(ContentsCharaValue.CHARA_MINNIE);

			}else if(tag.getInt("characterId") == 14){
				DebugLog.instance.outputLog("value", "ドナルドを登録");
				cto.chara.add(ContentsCharaValue.CHARA_DONALD);

			}else if(tag.getInt("characterId") == 23){
				DebugLog.instance.outputLog("value", "デイジーを登録");
				cto.chara.add(ContentsCharaValue.CHARA_DAISY);

			}else if(tag.getInt("characterId") == 148
					|| tag.getInt("characterId") == 149
					|| tag.getInt("characterId") == 150
					|| tag.getInt("characterId") == 151
					|| tag.getInt("characterId") == 152
					|| tag.getInt("characterId") == 153
					|| tag.getInt("characterId") == 154
					|| tag.getInt("characterId") == 162
					|| tag.getInt("characterId") == 163
					|| tag.getInt("characterId") == 460
					|| tag.getInt("characterId") == 570
					|| tag.getInt("characterId") == 571
					|| tag.getInt("characterId") == 572
					|| tag.getInt("characterId") == 573
					|| tag.getInt("characterId") == 574){

				if(cto.chara.size == 0){
					DebugLog.instance.outputLog("value", "プーを登録");
					cto.chara.add(ContentsCharaValue.CHARA_POOH);
				//他キャラでプーを登録済みだったら飛ばす
				}else{
					boolean isPooh = false;
					for(int j = 0; j < cto.chara.size; j++){
						if(cto.chara.get(j) == ContentsCharaValue.CHARA_POOH){
							isPooh = true;
							j = cto.chara.size;
						}
					}
					if(!isPooh){
						DebugLog.instance.outputLog("value", "プーを登録");
						cto.chara.add(ContentsCharaValue.CHARA_POOH);
					}
				}

			}else if(tag.getInt("characterId") == 1014
					|| tag.getInt("characterId") == 1548
					|| tag.getInt("characterId") == 214
					|| tag.getInt("characterId") == 472
					|| tag.getInt("characterId") == 218
					|| tag.getInt("characterId") == 215
					|| tag.getInt("characterId") == 226
					|| tag.getInt("characterId") == 232
					|| tag.getInt("characterId") == 84
					|| tag.getInt("characterId") == 91
					|| tag.getInt("characterId") == 86
					|| tag.getInt("characterId") == 133
					|| tag.getInt("characterId") == 132
					|| tag.getInt("characterId") == 131
					|| tag.getInt("characterId") == 455
					|| tag.getInt("characterId") == 454
					|| tag.getInt("characterId") == 33
					|| tag.getInt("characterId") == 236
					|| tag.getInt("characterId") == 234
					|| tag.getInt("characterId") == 233
					|| tag.getInt("characterId") == 237){

				if(cto.chara.size == 0){
					DebugLog.instance.outputLog("value", "プリンセスを登録");
					cto.chara.add(ContentsCharaValue.CHARA_PRINCESS);
				//他キャラでプリンセスを登録済みだったら飛ばす
				}else{
					boolean isPrincess = false;
					for(int j = 0; j < cto.chara.size; j++){
						if(cto.chara.get(j) == ContentsCharaValue.CHARA_PRINCESS){
							isPrincess = true;
							j = cto.chara.size;
						}
					}
					if(!isPrincess){
						DebugLog.instance.outputLog("value", "プリンセスを登録");
						cto.chara.add(ContentsCharaValue.CHARA_PRINCESS);
					}
				}

			}else if(tag.getInt("characterId") == 1610){//キャラタグ＞城
				DebugLog.instance.outputLog("value", "パークを登録");
				cto.chara.add(ContentsCharaValue.CHARA_PARK);


			}else{
				if(cto.chara.size == 0){
					DebugLog.instance.outputLog("value", "othersを登録");
					cto.chara.add(ContentsCharaValue.CHARA_OTHERS);
				//他キャラでothers登録済みだったら飛ばす
				}else{
					boolean isOthers = false;
					for(int j = 0; j < cto.chara.size; j++){
						if(cto.chara.get(j) == ContentsCharaValue.CHARA_OTHERS){
							isOthers = true;
							j = cto.chara.size;
						}
					}
					if(!isOthers){
						DebugLog.instance.outputLog("value", "othersを登録");
						cto.chara.add(ContentsCharaValue.CHARA_OTHERS);
					}
				}
			}
		}

		return cto;
	}

	private boolean isBadgeContents(String jsonStr) throws JSONException{
		DebugLog.instance.outputLog("value", "isBadgeContents::");

		JSONObject root = new JSONObject(jsonStr);
		JSONObject rootObj = root.getJSONObject("assetDetail");

		JSONArray providedSiteArray = rootObj.getJSONArray("providedSite");

			//該当提供先サイトを特定
			for( int i = 0; i < providedSiteArray.length(); i++ ){
				JSONObject child = providedSiteArray.getJSONObject(i);

				if(child.getString("providedSiteId").equals("aki")){
					if(!child.getString("userLevel").equals("null")){
						if(child.getInt("userLevel") >= 1){
							return true;
						}
					}
					i = providedSiteArray.length();
				}
			}
		return false;
	}

	public static boolean isVirtualDateOkAsset(Date virtualDate, String jsonStr) throws JSONException{
		DebugLog.instance.outputLog("value", "isVirtualDateOkAsset::");

		JSONObject root = new JSONObject(jsonStr);
		JSONObject rootObj = root.getJSONObject("assetDetail");

		JSONArray providedSiteArray = rootObj.getJSONArray("providedSite");

			//該当提供先サイトを特定
			for( int i = 0; i < providedSiteArray.length(); i++ ){
				JSONObject child = providedSiteArray.getJSONObject(i);

				if(child.getString("providedSiteId").equals("aki")){
					
					if(!child.getString("supStartDate").equals("null")){
						//日付を比較
						/*
								//存在してたら更新日を比較
								long lastModified = thumbsFile.lastModified();
								Date test = new Date(lastModified);
								DebugLog.instance.outputLog("value", "更新日付:" + test.toString());
								//assetDetailのupDateがアセット更新日付
								JSONObject root = new JSONObject(assetDetail.get(i));
								JSONObject detail = root.getJSONObject("assetDetail");
								String modiDate = detail.getString("upDate");
								//文字列をフォーマット指定でlong変換
								Date modiD = SPPUtility.getDateCNTFormat(modiDate);
								if(modiD.getTime() > lastModified) doDownload = true;

						 */
						Date supStart = SPPUtility.getDateCNTFormat(child.getString("supStartDate"));
						//仮想日付の方が未来だったら表示可能アセット
						if(virtualDate.getTime() >= supStart.getTime()) return true;
					}
					i = providedSiteArray.length();
				}
			}
		return false;
	}


	/**
	 *
	 * @param jsonStr
	 * @return
	 * @throws JSONException
	 */
	private ContentsDataDto getAssetDataFromJson(ContentsDataDto cto, String jsonStr) throws JSONException{
		DebugLog.instance.outputLog("value", "getAssetDataFromJson::");

		//アセット情報からアセットID、ピックアップonoff、ピックアップ番号、公開日付を取得
		//タグから色情報、キャラ情報、テーマグループ、コンテンツ種類を取得
		//assetDetailの中のcharacter、assetDetailの中のassetTag内に色タグ、テーマグループタグ、コンテンツ種類タグ

		JSONObject root = new JSONObject(jsonStr);
		//root直下にアセットID
		cto.assetID = root.getLong("assetId");

		//assetDetailの中のprovidedSiteの中にピックアップ関連と公開日付（supStartDate？
		JSONObject rootObj = root.getJSONObject("assetDetail");

		//週間ランキング（ランキング関連はテーマ・ウィジェット・アイコンで将来的に分けて表示（phase1ではテーマのみ
		if(!rootObj.getString("weeklyRank").equals("null")){
			cto.ranking = Integer.parseInt(rootObj.getString("weeklyRank"));
		}else{
			cto.ranking = 0;
		}

		JSONArray providedSiteArray = rootObj.getJSONArray("providedSite");

		//従属系のコンテンツ
		if(rootObj.getString("assetTypeId").equals("ct_dnkpk") || rootObj.getString("assetTypeId").equals("ct_dnkpw")
				|| rootObj.getString("assetTypeId").equals("ct_dnkpi") || rootObj.getString("assetTypeId").equals("ct_dnkps")){
			DebugLog.instance.outputLog("value", "従属系コンテンツ");

//			long aId = root.getLong("assetId");
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
				cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_DRAWER.getValue();

			}else if(rootObj.getString("assetTypeId").equals("ct_dnkps")){
				DebugLog.instance.outputLog("value", "ショーカットアイコンinテーマのアセット");
				type = ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue();
				cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue();
			}

			cto.contentsType = type;

			for( int i = 0; i < providedSiteArray.length(); i++ ){
				JSONObject child = providedSiteArray.getJSONObject(i);

				if(child.getString("providedSiteId").equals("aki")){
					//従属系コンテンツがピックアップの情報を持つ必要はない
//					if(child.getInt("pickupFlag") == 1){
//						cto.pickup = true;
//						cto.pickupNumber = child.getInt("pickupWeight");
//					}
					cto.publishDate = child.getString("supStartDate");
//					DebugLog.instance.outputLog("value", "提供開始日付：" + cto.getDateOfContents());

					if(child.getInt("limitFlag") == 1){
						cto.isLimitted = true;
					}else{
						cto.isLimitted = false;
					}

					if(child.getInt("premiumFlag") == 1){
						cto.isPremium = true;
//						if(isPremiumuUser){
//							cto.isSettable = true;
//						}else{
//							cto.isSettable = false;
//						}
					}else{
						cto.isPremium = false;
//						cto.isSettable = true;
					}

					i = providedSiteArray.length();
				}

			}

			String tagId = null;
			//キャラ以外のタグ
			JSONArray tagArray = rootObj.getJSONArray("assetTag");
			for(int i = 0; i < tagArray.length(); i++ ){
				JSONObject tag = tagArray.getJSONObject(i);

				//テーマグループタグ
				if(tag.getString("tagGroupNameId").equals("055")){
					tagId = tag.getString("tagId");

				//ウィジェット種類タグ
				}else if(tag.getString("tagGroupNameId").equals("057")){
					//ウィジェットだったら種別も要把握
					if(rootObj.getString("assetTypeId").equals("ct_dnkpw")){
						String tagDetail = tag.getString("tagId");
						if(tagDetail.equals("057001")){//バッテリーウィジェット
							cto.detailContentsType = ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue();
						}//TODO 以下ウィジェットの種類が増えるごとに条件分岐を増やす
					}
				}

			}

			cto.themeTag = tagId;

		//テーマ、単独系のコンテンツだったら
		}else{
			DebugLog.instance.outputLog("value", "テーマ・単独系コンテンツ:");
//			JSONArray providedSiteArray = rootObj.getJSONArray("providedSite");

			//コンテンツ種別
			if(rootObj.getString("assetTypeId").equals("ct_dnkp")){
				DebugLog.instance.outputLog("value", "テーマのアセット");
				cto.contentsType = ContentsTypeValue.CONTENTS_TYPE_THEME.getValue();

			}else if(rootObj.getString("assetTypeId").equals("ct_dnksw")){
				DebugLog.instance.outputLog("value", "ウィジェットのアセット");
				cto.contentsType = ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue();

			}else if(rootObj.getString("assetTypeId").equals("ct_dnksi")){
				DebugLog.instance.outputLog("value", "単独ショートカットアイコンのアセット");
				cto.contentsType = ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue();
				cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue();

			}

			//該当提供先サイトを特定
			for( int i = 0; i < providedSiteArray.length(); i++ ){
				JSONObject child = providedSiteArray.getJSONObject(i);

				if(child.getString("providedSiteId").equals("aki")){
					if(child.getInt("pickupFlag") == 1){
						cto.pickup = true;
						cto.pickupNumber = child.getInt("pickupWeight");
					}
					cto.publishDate = child.getString("supStartDate");
//					DebugLog.instance.outputLog("value", "提供開始日付：" + cto.getDateOfContents());

					if(child.getInt("limitFlag") == 1){
						cto.isLimitted = true;
					}else{
						cto.isLimitted = false;
					}

					if(child.getInt("premiumFlag") == 1){
						cto.isPremium = true;
					}else{
						cto.isPremium = false;
					}

					i = providedSiteArray.length();
				}
			}


			//タグからキャラ情報、テーマグループ、コンテンツ種類を取得
			//キャラタグ
			JSONArray charaArray = rootObj.getJSONArray("character");
			if(charaArray != null) cto = setCharacterInfo(cto, charaArray);

			//キャラ以外のタグ
			JSONArray tagArray = rootObj.getJSONArray("assetTag");

			for(int i = 0; i < tagArray.length(); i++ ){
				JSONObject tag = tagArray.getJSONObject(i);

				//色タグ（現状では色タグは無し
				if(tag.getString("tagGroupNameId").equals("00")){

				//テーマグループタグ
				}else if(tag.getString("tagGroupNameId").equals("055")){
					//タグIDを取得
					cto.themeTag = tag.getString("tagId");
					DebugLog.instance.outputLog("value", "ctoにテーマタグ追加" + cto.themeTag);

				//ウィジェット種類タグ
				}else if(tag.getString("tagGroupNameId").equals("057")){
					//ウィジェットだったら種類も把握
					if(rootObj.getString("assetTypeId").equals("ct_dnksw")){
						String tagDetail = tag.getString("tagId");
						if(tagDetail.equals("057001")){//バッテリーウィジェット
							cto.detailContentsType = ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue();
						}//TODO 以下ウィジェットの種類が増えるごとに条件分岐を増やす
					}
				}

			}
		}

		return cto;
	}

	private boolean checkThumbAllGreen(File thumbsFile){
		byte[] buffer = new byte[1024 * 10];
		DataInputStream in = null;
		try {
			in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(thumbsFile))));

			int fileSize = in.readInt();
			int readBytes = 0;
			while ((readBytes = in.read(buffer)) != -1) {}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return true;

	}

	//サムネダウンロード
	/**
	 * サムネイルをダウンロードしてthumbnailsフォルダに保存
	 * @param thumbsURL
	 * @return
	 */
	public boolean downloadThumbnails(String fileName, String thumbsURL, String assetID){

		String path = thumbsURL;
		int filesize = 0; // file size temporary hard coded
//		int bytesRead;
//		int current = 0;
		int downloaded = 0;
		int timeout = 10 * 1000;//10秒
//		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		byte[] mybytearray = null;

		try {
//			URL url = new URL(path);
//			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//			connection.setConnectTimeout(timeout);
//			connection.setReadTimeout(timeout);
//			connection.setRequestMethod("GET");
//			// ---------set request download--------
//			connection.setDoInput(true);
//			connection.connect();
//			// --------------------------------------
//			int lengthOfFile = connection.getContentLength();
//			if (lengthOfFile <= 0) {
//				connection.disconnect();
////				flagDownload = false;
//				return false;
//			}
//			filesize = lengthOfFile + 2;
//			lengthOfFile += downloaded;
//			// receive file
//			mybytearray = new byte[filesize];
//			BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
//
//			//キャンセルチェック
//			if(isCancelled()) return false;
//
//			File zipFile = new File(dirPath + fileName);
//			OutputStream zipOs = new FileOutputStream(zipFile);
//
//			//キャンセルチェック
//			if(isCancelled()) return false;
//
//			BufferedOutputStream zipBos = new BufferedOutputStream(zipOs);
//
//			//キャンセルチェック
//			if(isCancelled()) return false;
//
//
//			byte[] zipBuffer = new byte[1024];
//			int readByte = 0;
//
//		    while(-1 != (readByte = is.read(zipBuffer))){
//		    	if(isCancelled()){
//		    		return false;
//		    	}
//		    	zipBos.write(zipBuffer, 0, readByte);
//		    }
//		    zipBos.close();

			//画像データを取得
			String dirPath = "";

			if(contentsDetailType == 0){
				dirPath = FileUtility.getThumbnailsRootPath(myContext);
			}else{
				dirPath = FileUtility.getThumbnailsCachePath(myContext);
			}

			if(isCancelled()) return false;

			File thumbsFile = new File(dirPath + assetID + ".etc1");
			URL imageUrl = new URL(path);
			DebugLog.instance.outputLog("value", "ThumbnailsDownloadSave:::" + path.toString());
			InputStream imageIs = imageUrl.openStream();

			if(isCancelled()){
				DebugLog.instance.outputLog("value", "キャンセル_書き込み開始前" + thumbsFile.getAbsolutePath());
				if(thumbsFile.exists()) FileUtility.delFile(thumbsFile);
				return false;
			}

			FileOutputStream fOut = new FileOutputStream(thumbsFile);

			try {
	            byte[] buf = new byte[1024];
	            int len = 0;

	            while ((len = imageIs.read(buf)) > 0) {  //終わるまで書き込み
	                fOut.write(buf, 0, len);

	    			if(isCancelled()){
	    				DebugLog.instance.outputLog("value", "キャンセル_書き込み中" + thumbsFile.getAbsolutePath());
	    				fOut.flush();
	    				fOut.close();
	    				imageIs.close();
	    				if(thumbsFile.exists()) FileUtility.delFile(thumbsFile);
	    				return false;
	    			}

	            }

	            fOut.flush();
	        } finally {
	            fOut.close();//ストリームをクローズすることを忘れずに
	            imageIs.close();
	        }
//			Log.d("value2", cacheFileName + "をキャッシュディレクトリ（" + cacheDir.getAbsolutePath() + "）に保存");

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			DebugLog.instance.outputLog("value", "ThumbnailsDownloadSave:::" + e.getMessage());
			return false;
		}

	}



//	//専用変数に値を代入
//	private void parseResponse(String response) throws JSONException{
//		/*
//	assetInfo
//		>assetDetail
//			>fileAttribute
//				>isThumbnailが0
//					>downloadUrl
//		 */
//
//		tokenArray = new String[3];
//		JSONObject rootObj = new JSONObject(response);
//		JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");
//
//		//assetDetail
//		for(int i = 0; i < assetInfoAry.length(); i++){
//			//assetDetailを探す
//			JSONObject item = assetInfoAry.getJSONObject(i);
//			JSONObject detail = item.getJSONObject("assetDetail");
//
//			//assetID
//			tokenArray[1]  = item.getString("assetId");
//
//			//descriptionを探す
////			String desc = detail.getString("description");
////			GetColorParser colorParser = new GetColorParser();
////			colorParser.getObjectFromJson(desc, artNum, context);
//
//			//fileAttributeを探す
//			JSONArray attributeAry = detail.getJSONArray("fileAttribute");
//
//			//この中からisThumbnailが2のもの
//			for(int j = 0; j < attributeAry.length(); j++){
//				JSONObject item2 = attributeAry.getJSONObject(j);
//				DebugLog.instance.outputLog("value", "object:" + j + ":" + item2.toString());
//				if(item2.getInt("isThumbnail") == 2){
//					//このデータでバージョンを比較
////					newModifiedDate = detail.getString("modifiedDate");
////					newVersionCode = Integer.parseInt(item2.getString("versionCode"));
//					downloadUrl = item2.getString("downloadUrl");
//					fileName = item2.getString("fileName");
//					tokenArray[0] = item2.getString("downloadUrl");
//					tokenArray[2] = item2.getString("fileId");
//
//					//isThumbnail = 2が一枚取れたらいいのでここで終わり。
//					j = attributeAry.length();
//					i = assetInfoAry.length();
//				}
//			}
//		}
//	}


//	private void parseResponseForToken(String response) throws JSONException{
//		JSONObject rootObj = new JSONObject(response);
//		JSONObject download_token = rootObj.getJSONObject("download_token");
//		downloadUrl = download_token.getString("download_url");
//	}

//	@Override
//	public void onCancel(DialogInterface dialog) {
//		this.cancel(true);
//	}

}
