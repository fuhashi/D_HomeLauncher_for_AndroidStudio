package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.CntApiConnector.Param;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.DetailThumbsComparator;
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
//import android.util.Log;


/**
 * アセットIDから詳細ページ表示用に必要な情報を取得する
 * @author sagara
 *
 */
public class GetDetailSkinInfoAsyncTask extends
		AsyncTask<String, Integer, Boolean>{
//		AsyncTask<String, Integer, Boolean> implements OnCancelListener{
	
//	private ProgressDialog dialog = null;
	Context myContext;
	long startTime = 0;
	private GetDetailSkinInfoTaskCallback callback;

	private int timeout = 15 * 1000;
	Timer   mTimer   = null;

	public GetDetailSkinInfoAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (GetDetailSkinInfoTaskCallback) context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		
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
//	private ContentsDataDto cto = null;
	private String assetId = "";
	private int contentType = -1;
	private Array<ThumbInfo> thumbsInfoArray = null;
	
	private Date virtualDate = null;
//	private Array<ContentsDataDto> childCtoArray = null;
	
	/**
	 * arg0[0]:assetID
	 * arg0[1]:コンテンツ種別
	 * arg0[2]:テーマタグ
	 * return t（成功）/f（失敗）
	 */
	@Override
	protected Boolean doInBackground(String... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "詳細サムネ保存task_" + arg0.length);
		
		//パラメータ指定が足りなかったらcancel
		if(arg0.length <= 1) return false;
		
		//アセットIDとコンテンツ種別（一括テーマ、壁紙、ウィジェット、アイコン）を取得
		assetId = arg0[0];
		contentType = Integer.parseInt(arg0[1]);
		
		//テーマだったら
		String tagId = "";
		if(contentType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
			tagId = arg0[2];
		}

		//DL処理開始
		try {
	
				if(isCancelled()){
					return false;
				}
				
				//オフラインチェック
				if(!SPPUtility.checkNetwork(myContext)){
					return false;
				}
				
				//仮想日付
				String virtualDateString = "";
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
				//テーマだったらアセットIDじゃなくてタグで取りに行く
				if(contentType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
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
					.addParameter(Param.tag, tagId, false)
					.addParameter(Param.categoryId, "all", false)
					.addParameter(Param.idType, "asset", false);
					
					//仮想日付が正しく設定されていたら
					if(virtualDate != null){
						connector.addParameter(Param.isNotPublished, "1", false);
					}

				}else{
					//それ以外だったら自分の情報のみが必要なので自分のアセットが取れれば問題ない
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
					.addParameter(Param.providedSiteId, "aki", false)
					.addParameter(Param.id, assetId, false)
					.addParameter(Param.categoryId, "all", false)
					.addParameter(Param.idType, "asset", false);
					
					//仮想日付が正しく設定されていたら
					if(virtualDate != null){
						connector.addParameter(Param.isNotPublished, "1", false);
					}

				}
				
				connector = connector.setParameter();
				connector.setTimeout(timeout - (int)(System.currentTimeMillis() - startTime));//タイムアウトはtask起動開始からの経過時間を抜いて設定
				
				//ここにresponseを入れる
				String responseBody = null;
				connector.connect();
				DebugLog.instance.outputLog("value", "接続開始時間:" + (System.currentTimeMillis() - startTime));
//				DebugLog.instance.outputLog("value", "connect:getResponseCode" + connector.getResponseCode());
//				DebugLog.instance.outputLog("value", "connect:getResponseMessage" + connector.getResponseMessage());
				responseBody = connector.getResponseBody();
				
				if(responseBody.equals("")) return false;
				
				DebugLog.instance.outputLog("value", "connect:responseBody" + responseBody);
				
				//タグが与えられたctoに沿うものが本体
				
				//TODO 何がいくつ存在するかをActivityに返す。ダウンロード処理はそのまま継続
				//ファイルの「その他」
				//assetDetail＞fileAttribute＞isThumbnail=3,description!=qp
				thumbsInfoArray = getThumbsDetailFromJson(responseBody);
				//ここでチェックして個数を明記して返すかタイプを丸ごと返すか
				thumbsInfoArray.sort(new DetailThumbsComparator());
				for(int i = 0; i < thumbsInfoArray.size; i++){
					thumbsInfoArray.get(i).setIndex(i);
				}
				
//				String path = downloadUrl;
//				int filesize = 0; // file size temporary hard coded
//				long start = System.currentTimeMillis();
//				int bytesRead;
//				int current = 0;
//				int downloaded = 0;
//				int timeout = 100000;
//				FileOutputStream fos = null;
//				BufferedOutputStream bos = null;//TODO

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
				// receive file
				
//				File saveFile = new File("/data/data/" + myContext.getPackageName() +  "/files/sample/", fileName);
				
//				boolean isShukushou = false;//TODO
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
				
//				connection.disconnect();

//				flagDownload = true;

			//}
		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "CatchException in doInBackground:" + e.getMessage());
			return false;
		}
		//return 123L;

		return true;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		//super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled() {
//		diaDebugLog.instance.outputLogismiss();
//		DebugLog.instance.outputLog("download", "onCancelled");

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		//どの段階で失敗ないし明示的キャンセルが来たかを知っている必要がある
		callback.onFailedDownloadDetailThumbsNetwork();
		super.onCancelled();
	}

	@Override
	protected void onPostExecute(Boolean result) {
//		diaDebugLog.instance.outputLogismiss();
		DebugLog.instance.outputLog("value", "onPostExecute:" + result);
		
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		//super.onPostExecute(result);
		
		if(!result){
			//ダウンロード失敗
//			DebugLog.instance.outputLog("download", "onPostExecute:resultFalse");
			callback.onFailedDownloadDetailThumbsNetwork();
			return;
		}
		DebugLog.instance.outputLog("value", "GetDetailSkinInfoAsyncTask:::onPostExecute:::result:" + result);

		DebugLog.instance.outputLog("value", "download success!:" + (System.currentTimeMillis() - startTime));
		callback.onFinishedGetDetailInfo(thumbsInfoArray);
			
	}

	public boolean downloadThumbnails(String thumbsURL){
		//TODO カタログ用詳細サムネのディレクトリを別途生成（ここのディレクトリはアプリ終了時に削除
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext));
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/" + cto.assetID + "/");

		String path = thumbsURL;
		int filesize = 0; // file size temporary hard coded
		long start = System.currentTimeMillis();
		int downloaded = 0;
		int timeout = 100000;
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;//TODO

		try {
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
				return false;
			}
			filesize = lengthOfFile + 2;
			lengthOfFile += downloaded;
			// receive file
//			mybytearray = new byte[filesize];
			DebugLog.instance.outputLog("value", "inputStream取得開始");
//			BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
			InputStream is = connection.getInputStream();
			
//			File saveFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/detail/", cto.assetID + ".zip");
			File saveFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext), assetId + ".zip");
			OutputStream zipOs = new FileOutputStream(saveFile);
			
//			BufferedOutputStream zipBos = new BufferedOutputStream(zipOs);
//			byte[] zipBuffer = new byte[1024];
//			int readByte = 0;
//			  
//		    while(-1 != (readByte = is.read(zipBuffer))){
//		    	zipBos.write(zipBuffer, 0, readByte);
//		    }
//		    zipBos.close();
			
			 int DEFAULT_BUFFER_SIZE = 1024 * 4;
			  byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			  int size = -1;
			  DebugLog.instance.outputLog("value", "outputStreamへ書き込み開始");
			  while (-1 != (size = is.read(buffer))) {
				  zipOs.write(buffer, 0, size);
			  }
			  DebugLog.instance.outputLog("value", "outputStreamへ書き込み完了");
			  is.close();
			  zipOs.close();
		} catch (Exception e) {
			return false;
		}
	    return true;
	}
	
	////////////////////////
	////////////////////////
	////////////////////////
	
	public boolean downloadThumbnailsSimple(String thumbsURL){
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/");
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext));
		try {
			URL url = new URL(thumbsURL);
			
           	DebugLog.instance.outputLog("value", "接続：" + thumbsURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.connect();
	        
           	DebugLog.instance.outputLog("value", "ファイル作成");
	        File outputFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext), assetId + ".zip");
//	        File outputFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/detail/", cto.assetID + ".zip");

	        FileOutputStream fos = new FileOutputStream(outputFile);
	        
	        // ダウンロード開始
           	DebugLog.instance.outputLog("value", "ダウンロード開始");
	        InputStream is = connection.getInputStream();
	        byte[] buffer = new byte[1024];
	        int len = 0;

	        while ((len = is.read(buffer)) != -1) {
            	DebugLog.instance.outputLog("value", "outputStreamへwrite_start");
	            fos.write(buffer, 0, len);
            	DebugLog.instance.outputLog("value", "outputStreamへwrite_______end");
	        }

	        fos.close();
	        is.close();
		} catch (IOException e) {
			return false;
		}
		
		return true;

	}
	
	
	public boolean downloadThumbnailsNew(String thumbsURL){
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext));
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/");

//		String path = thumbsURL;
		int filesize = 0; // file size temporary hard coded
		int downloaded = 0;
		int timeout = 10 * 1000;//10秒
		BufferedOutputStream bos = null;
		byte[] mybytearray = null;

		try {

			//webより画像データを取得
//			String dirPath = "/data/data/" + myContext.getPackageName() +  "/files/thumbnails/";
//			File thumbsFile = new File(dirPath + assetID + ".etc1");
			File saveFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext), assetId + ".png");
//			File saveFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/detail/", cto.assetID + ".png");

			URL imageUrl = new URL(thumbsURL);
//			DebugLog.instance.outputLog("value", "ThumbnailsDownloadSave:::" + path.toString());
//			InputStream imageIs = imageUrl.openStream();
			
			InputStream imageIs = new BufferedInputStream(imageUrl.openStream());
//			BufferedInputStream bIs 

//			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(saveFile));

			OutputStream outStream = null;
			try {  
//	            byte[] buf = new byte[1024]; 
	            int len = 0;  
	  
	            outStream = new BufferedOutputStream(new FileOutputStream(saveFile));
	            DebugLog.instance.outputLog("value", "outputStreamへ書き込み開始");
//	            while ((len = imageIs.read(buf)) > 0) {  //終わるまで書き込み  
//	                fOut.write(buf, 0, len);  
//	            }  
	            
//	            int b;
//	            while((b = imageIs.read()) != -1){
//	            	DebugLog.instance.outputLog("value", "outputStreamへwrite_start");
//	            	os.write(b);
//	            	DebugLog.instance.outputLog("value", "outputStreamへwrite_______end");
//	            }
	            
	            int data = -1;
	            int BUFFER_SIZE = 1024;
	            byte[] buf = new byte[BUFFER_SIZE];
	            while ((data = imageIs.read(buf)) != -1) {
	            	DebugLog.instance.outputLog("value", "outputStreamへwrite_start");
	                outStream.write(buf, 0, data);
	            	DebugLog.instance.outputLog("value", "outputStreamへwrite_______end");
	            }
				DebugLog.instance.outputLog("value", "outputStreamへ書き込み完了");

				outStream.flush();
//	            fOut.flush();
//				os.flush();
	        } finally {
	        	outStream.close();
//	            os.close();//ストリームをクローズすることを忘れずに  
	            imageIs.close();  
	        }  

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			DebugLog.instance.outputLog("value", "ThumbnailsDownloadSave:::" + e.getMessage());
			return false;
		}

	}

	
	////////////////////////
	////////////////////////
	////////////////////////
	
	/**
	 * zipダウンロード、解凍してthumbnailsフォルダに保存
	 * @param thumbsURL
	 * @return
	 */
	public boolean doDownloadThumbnailsZip(String thumbsURL){
		
		//TODO カタログ用詳細サムネのディレクトリを別途生成（ここのディレクトリはアプリ終了時に削除
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/");
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/" + cto.assetID + "/");
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext));
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext) + assetId + "/");

		String path = thumbsURL;
		int filesize = 0; // file size temporary hard coded
//		int bytesRead;
//		int current = 0;
		int downloaded = 0;
		int timeout = 100000;
//		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		byte[] mybytearray = null;

		try {
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
//				flagDownload = false;
				return false;
			}
			filesize = lengthOfFile + 2;
			lengthOfFile += downloaded;
			// receive file
			mybytearray = new byte[filesize];
			BufferedInputStream is = new BufferedInputStream(connection.getInputStream());

			ZipInputStream zIns = new ZipInputStream(is);
			ZipEntry zipEntry  = null;
			int zipLen = 0;

			//zipファイルに含まれるエントリに対して順にアクセス
			while ((zipEntry = zIns.getNextEntry()) != null) {
				DebugLog.instance.outputLog("value", "zipEntry.getName():" + zipEntry.getName());
				String[] filename = zipEntry.getName().split("/");
				for(int i = 0; i < filename.length; i++){
					DebugLog.instance.outputLog("value", "zipEntry:" + i + ":" + filename[i]);
				}
//				File downloadFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/detail/" + cto.assetID + "/", filename[filename.length - 1]);
				File downloadFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext) + assetId + File.separator, filename[filename.length - 1]);
				OutputStream os = new FileOutputStream(downloadFile);

				bos = new BufferedOutputStream(os);

				byte[] buffer = new byte[1024];
				while ((zipLen = zIns.read(buffer)) != -1) {
					bos.write(buffer, 0, zipLen);
				}

				zIns.closeEntry();
				bos.close();
				bos = null;
				
				//TODO このタイミングでファイル名から該当ContentsTypeを判断してcallback()
//				String name = filename[filename.length - 1];
//				if(name.indexOf("thumb_detail_theme.jpg") != -1){
//					//テーマ
//					callback.onFinishedDownloadDetailThumbs(ContentsType.theme);
//				}else if(name.indexOf("wp") != -1){//未定
//					//壁紙はナンバリングする？？しない？？？
//					callback.onFinishedDownloadDetailThumbs(ContentsType.wp);
//				}else if(name.indexOf("thumb_detail_icon.jpg") != -1){
//					//ウィジェット
//					callback.onFinishedDownloadDetailThumbs(ContentsType.appIcon);
//				}else if(name.indexOf("thumb_detail_wdt_battery.jpg") != -1){
//					//アイコン
//					callback.onFinishedDownloadDetailThumbs(ContentsType.widgetBattery);
//				}

			}

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			DebugLog.instance.outputLog("value", "ThumbnailsDownloadSave:::" + e.getMessage());
			return false;
		}

	}

	
	
	///////////////専用parser
	
/*	private Array<ContentsDataDto> getChildContentsArray(String jsonStr) throws JSONException {
		Array<ContentsDataDto> ctoArray = new Array<ContentsDataDto>();
		
		JSONObject rootObj = new JSONObject(jsonStr);
		JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");
		JSONObject detailObj = null;
		
		//assetDetail(これが各個Asset情報)
		if(assetInfoAry != null && assetInfoAry.length() > 0){
			for(int i = 0; i < assetInfoAry.length(); i++){
				//assetDetailを探す
				detailObj = assetInfoAry.getJSONObject(i);
				ContentsDataDto cto = new ContentsDataDto();
				
				cto.assetID = detailObj.getLong("assetId");

				JSONObject assetDetail = detailObj.getJSONObject("assetDetail");
				
				JSONArray providedSiteArray = assetDetail.getJSONArray("providedSite");

				//従属系のコンテンツ
				if(assetDetail.getString("assetTypeId").equals("ct_dnkpk") || assetDetail.getString("assetTypeId").equals("ct_dnkpw")
						|| assetDetail.getString("assetTypeId").equals("ct_dnkpi") || assetDetail.getString("assetTypeId").equals("ct_dnkps")){
					DebugLog.instance.outputLog("value", "従属系コンテンツ");

					int type = 0;
					if(assetDetail.getString("assetTypeId").equals("ct_dnkpk")){
						DebugLog.instance.outputLog("value", "壁紙inテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue();
						
					}else if(assetDetail.getString("assetTypeId").equals("ct_dnkpw")){
						DebugLog.instance.outputLog("value", "ウィジェットinテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue();
						//ウィジェットの細かい種別は下で取得

					}else if(assetDetail.getString("assetTypeId").equals("ct_dnkpi")){
						DebugLog.instance.outputLog("value", "ドロワーアイコンinテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue();
						cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_DRAWER.getValue();

					}else if(assetDetail.getString("assetTypeId").equals("ct_dnkps")){
						DebugLog.instance.outputLog("value", "ショーカットアイコンinテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue();
						cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue();

					}

					cto.contentsType = type;
					
					for( int j = 0; j < providedSiteArray.length(); j++ ){
						JSONObject child = providedSiteArray.getJSONObject(j);

						if(child.getString("providedSiteId").equals("aki")){
							//※従属系コンテンツがピックアップの情報を持つ必要はないので取得しない
							
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

							j = providedSiteArray.length();
						}
			
					}

					String tagId = null;
					//キャラ以外のタグ
					JSONArray tagArray = assetDetail.getJSONArray("assetTag");
					for(int j = 0; j < tagArray.length(); j++ ){
						JSONObject tag = tagArray.getJSONObject(j);

						//テーマグループタグ
						if(tag.getString("tagGroupNameId").equals("055")){
							tagId = tag.getString("tagId");

						//ウィジェット種類タグ
						}else if(tag.getString("tagGroupNameId").equals("057")){
							//ウィジェットだったら種別も要把握
							if(assetDetail.getString("assetTypeId").equals("ct_dnkpw")){
								String tagDetail = tag.getString("tagId");
								if(tagDetail.equals("057001")){//バッテリーウィジェット
									cto.detailContentsType = ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue();
								}//TODO 以下ウィジェットの種類が増えるごとに条件分岐を増やす
							}
						}
					}
					cto.themeTag = tagId;

				}
			
				ctoArray.add(cto);
			}
		}
		
//		//DBからの値
//		MyPageDataAccess mAccess = new MyPageDataAccess(myContext);
//
//		//DB内にあったらDB内の情報もctoに入れる（テーマタグで取得
//		Array<MyPageDataDto> a = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_THEME_TAG.getParam(), String.valueOf(ctoArray.get(0).themeTag));
//		if(a != null && a.size > 0){
//			for(ContentsDataDto cto : ctoArray){
////				this.isExist = dto.isExist;
////				this.hasDownloadHistory = dto.hasDownloadHistory;
////				this.isFavorite = dto.isFavorite;
////				this.addedDate = dto.addedDate;
//				
//			}
//		}
		
		
		return ctoArray;
	}
*/	
	/*
DownloadSkinTaskで使用するctoの情報は確実に保持する必要有り
assetID
contentsType
themeTag
isExist
hasDownloadHistory

setDataFromDB
getContentsTypeId

	 */
	
	
	//ファイルの「その他」
	//assetDetail＞fileAttribute＞isThumbnail=3,description!=qp
	private Array<ThumbInfo> getThumbsDetailFromJson(String jsonStr) throws JSONException {
		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::");
//		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::jsonStr:" + jsonStr);
//		String[] returnArray = {"", ""};//0：更新フラグ、1:URL
		Array<ThumbInfo> returnArray = new Array<ThumbInfo>();
		
		JSONObject rootObj = new JSONObject(jsonStr);
		JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");
		JSONObject detailObj = null;
		
		//assetDetail(これが各個Asset情報)
		if(assetInfoAry != null && assetInfoAry.length() > 0){
			for(int i = 0; i < assetInfoAry.length(); i++){
				//assetDetailを探す
				detailObj = assetInfoAry.getJSONObject(i);
				
				//仮想日付が設定されていない or 仮想日付が設定されていて、提供開始日が仮想日付より過去のアセット
				if(virtualDate == null || (virtualDate != null && DownloadAllDataAsyncTask.isVirtualDateOkAsset(virtualDate, detailObj.toString()))){

				
				ContentsDataDto cto = new ContentsDataDto();
//				DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::detailObj__" + detailObj.toString());
				
				String assetidSub = detailObj.getString("assetId");
				cto.assetID = detailObj.getLong("assetId");

				JSONObject assetDetail = detailObj.getJSONObject("assetDetail");
				JSONArray fileArray = assetDetail.getJSONArray("fileAttribute");
				
				//ctoはthumbsInfo（画像ごと）に対して幾つか同じものを使用する（※ctoはアセットごとに用意
				if(assetDetail.getString("assetTypeId").equals("ct_dnkpk") || assetDetail.getString("assetTypeId").equals("ct_dnkpw")
						|| assetDetail.getString("assetTypeId").equals("ct_dnkpi") || assetDetail.getString("assetTypeId").equals("ct_dnkps")){
					DebugLog.instance.outputLog("value", "従属系コンテンツ");
					int type = 0;
					
					if(assetDetail.getString("assetTypeId").equals("ct_dnkpk")){
						DebugLog.instance.outputLog("value", "壁紙inテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue();
					}else if(assetDetail.getString("assetTypeId").equals("ct_dnkpw")){
						DebugLog.instance.outputLog("value", "ウィジェットinテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue();
						//ウィジェットの細かい種別は下で取得

					}else if(assetDetail.getString("assetTypeId").equals("ct_dnkpi")){
						DebugLog.instance.outputLog("value", "ドロワーアイコンinテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue();
						cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_DRAWER.getValue();
					}else if(assetDetail.getString("assetTypeId").equals("ct_dnkps")){
						DebugLog.instance.outputLog("value", "ショーカットアイコンinテーマのアセット");
						type = ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue();
						cto.detailContentsType = ContentsDetailTypeValue.ICON_DETAIL_TYPE_SHORTCUT.getValue();
					}
					
					cto.contentsType = type;

					JSONArray providedSiteArray = assetDetail.getJSONArray("providedSite");
					for( int j = 0; j < providedSiteArray.length(); j++ ){
						JSONObject child = providedSiteArray.getJSONObject(j);

						if(child.getString("providedSiteId").equals("aki")){
							//※従属系コンテンツがピックアップの情報を持つ必要はないので取得しない
							
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

							j = providedSiteArray.length();
						}
			
					}
					String tagId = null;
					//キャラ以外のタグ
					JSONArray tagArray = assetDetail.getJSONArray("assetTag");
					for(int j = 0; j < tagArray.length(); j++ ){
						JSONObject tag = tagArray.getJSONObject(j);

						//テーマグループタグ
						if(tag.getString("tagGroupNameId").equals("055")){
							tagId = tag.getString("tagId");

						//ウィジェット種類タグ
						}else if(tag.getString("tagGroupNameId").equals("057")){
							//ウィジェットだったら種別も要把握
							if(assetDetail.getString("assetTypeId").equals("ct_dnkpw")){
								String tagDetail = tag.getString("tagId");
								if(tagDetail.equals("057001")){//バッテリーウィジェット
									cto.detailContentsType = ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY.getValue();
								}//TODO 以下ウィジェットの種類が増えるごとに条件分岐を増やす
							}
						}
					}
					cto.themeTag = tagId;
					
				}
				
//				DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::_______" + fileArray.length());
				for(int j = 0; j < fileArray.length(); j++){
					JSONObject childObj = fileArray.getJSONObject(j);
//					DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::__________" + childObj.toString());
					
					// isThumbnailが3のファイルが「その他」、詳細用のサムネである旨が備考欄に
					if(childObj.getInt("isThumbnail") == 3 && childObj.getString("description").equals("detailThumb") ){
//					if(childObj.getString("isThumbnail").equals("3") && childObj.getString("description").equals("detailThumb") ){
						ThumbInfo info = new ThumbInfo(childObj.getString("downloadUrl"));
						info.setType(childObj.getString("fileName"));
						
						if (assetId.equals(assetidSub) && contentType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
							//テーマ本体アセットのサムネにはドロワーとexampleもあるので要注意
							if(childObj.getString("fileName").equals(ContentsFileName.ThumbDetailDrawer.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx1.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx2.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx3.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx4.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx5.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx6.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx7.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx8.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx9.getFileName())
									|| childObj.getString("fileName").equals(ContentsFileName.ThumbDetailEx10.getFileName())){
//								info.setAssetId(assetId);
//							}else{
//								info.setAssetId(assetidSub);
							}
							info.setAssetId(assetId);
						}else{
							info.setAssetId(assetidSub);
						}
						info.setFolderName(assetId);
						
						DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::filename:" + childObj.getString("fileName"));
						
						info.setCto(cto);
						
						returnArray.add(info);
					}
				}
			}
			}
		}
		
		
		return returnArray;
	}

	private void parseResponse(String response) throws JSONException{
		/*
	assetInfo
		>assetDetail
			>fileAttribute
		 */
		
		JSONObject rootObj = new JSONObject(response);
		JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");
		
		//assetDetail
		for(int i = 0; i < assetInfoAry.length(); i++){
			//assetDetailを探す
			JSONObject item = assetInfoAry.getJSONObject(i);
			JSONObject detail = item.getJSONObject("assetDetail");
			
			//assetID
			tokenArray[1]  = item.getString("assetId");

			//fileAttributeを探す
			JSONArray attributeAry = detail.getJSONArray("fileAttribute");
			
			//この中からisThumbnailが2のもの
			for(int j = 0; j < attributeAry.length(); j++){
				JSONObject item2 = attributeAry.getJSONObject(j);
				DebugLog.instance.outputLog("value", "object:" + j + ":" + item2.toString());
				if(item2.getInt("isThumbnail") == 2){
					//このデータでバージョンを比較
//					newModifiedDate = detail.getString("modifiedDate");
//					newVersionCode = Integer.parseInt(item2.getString("versionCode"));
					downloadUrl = item2.getString("downloadUrl");
					fileName = item2.getString("fileName");
//					tokenArray[0] = item2.getString("downloadUrl");
//					tokenArray[2] = item2.getString("fileId");
					
					//isThumbnail = 2が一枚取れたらいいのでここで終わり。
					j = attributeAry.length();
					i = assetInfoAry.length();
				}	
			}
		}			
	}
	
	
	private String downloadUrl = "";//最初はトークンなしのURLを通常に取得して、それを元にトークンありのURLを別途取得して代入
	private String[] tokenArray = null;
	private String fileName = "";

	//専用変数に値を代入
	private void parseResponse_(String response) throws JSONException{
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

			//fileAttributeを探す
			JSONArray attributeAry = detail.getJSONArray("fileAttribute");
			
			//この中からisThumbnailが2のもの
			for(int j = 0; j < attributeAry.length(); j++){
				JSONObject item2 = attributeAry.getJSONObject(j);
				DebugLog.instance.outputLog("value", "object:" + j + ":" + item2.toString());
				if(item2.getInt("isThumbnail") == 2){
					//このデータでバージョンを比較
//					newModifiedDate = detail.getString("modifiedDate");
//					newVersionCode = Integer.parseInt(item2.getString("versionCode"));
					downloadUrl = item2.getString("downloadUrl");
					fileName = item2.getString("fileName");
//					tokenArray[0] = item2.getString("downloadUrl");
//					tokenArray[2] = item2.getString("fileId");
					
					//isThumbnail = 2が一枚取れたらいいのでここで終わり。
					j = attributeAry.length();
					i = assetInfoAry.length();
				}	
			}
		}			
	}
	

//	@Override
//	public void onCancel(DialogInterface dialog) {
//		this.cancel(true);
//	}

}
