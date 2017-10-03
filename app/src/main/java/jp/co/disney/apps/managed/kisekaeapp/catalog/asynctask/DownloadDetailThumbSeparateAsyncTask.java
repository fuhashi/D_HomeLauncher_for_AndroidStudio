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
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.badlogic.gdx.utils.Array;
//import android.util.Log;


/**
 * 詳細サムネを指定URLから個別取得する
 * @author sagara
 *
 */
public class DownloadDetailThumbSeparateAsyncTask extends
		AsyncTask<ThumbInfo, Integer, Boolean>{
//		AsyncTask<String, Integer, Boolean> implements OnCancelListener{

//	private ProgressDialog dialog = null;
	Context myContext;
	long startTime = 0;
	private DownloadDetailThumbSeparateTaskCallback callback;
//	private String assetId = "";
//	private String entity_type = "";

	private int timeout = 15 * 1000;
	Timer   mTimer   = null;

	public DownloadDetailThumbSeparateAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (DownloadDetailThumbSeparateTaskCallback) context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

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
	private boolean flagDownload = false;
	private ThumbInfo info = null;

	/**
	 * arg0[0]:assetID
	 * arg0[1]:コンテンツ種別
	 * return t（成功）/f（失敗）
	 */
	@Override
	protected Boolean doInBackground(ThumbInfo... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "詳細サムネ保存task_" + arg0.length);

		//パラメータ指定が足りなかったらcancel
		if(arg0.length < 1) return false;

		info = arg0[0];

		if(isCancelled()){
			return false;
		}
		//Thread.sleep(1000);
		//publishProgress((i+1) * 10);

		//オフラインチェック
		if(!SPPUtility.checkNetwork(myContext)){
			return false;
		}

		//DL処理開始
		downloadUrl = info.getDownloadUrl();
		DebugLog.instance.outputLog("value", "connect:downloadURL:" + downloadUrl);//取得できてる

		//サムネが入ってるzipをダウンロード
		flagDownload = downloadThumbnailsSimple(downloadUrl);

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

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		//どの段階で失敗ないし明示的キャンセルが来たかを知っている必要がある
		if(info == null){
			callback.onFailedDownloadDetailThumbsSeparateNetwork(null);
		}else{
			callback.onFailedDownloadDetailThumbsSeparateNetwork(info);
		}
		super.onCancelled();
	}

	@Override
	protected void onPostExecute(Boolean result) {
//		diaDebugLog.instance.outputLogismiss();

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		//super.onPostExecute(result);

		if(!result){
			//ダウンロード関連で失敗
//			DebugLog.instance.outputLog("download", "onPostExecute:resultFalse");
			callback.onFailedDownloadDetailThumbsSeparateNetwork(info);
			return;
		}

		if(flagDownload){
			DebugLog.instance.outputLog("value", "Thumb download success!:" + info.getFileName() + ":" + (System.currentTimeMillis() - startTime));
			callback.onFinishedDownloadDetailThumbsSeparate(info);

		}else{
			//ダウンロード時以外での失敗
//			DebugLog.instance.outputLog("download", "onPostExecute:flagDownloadFalse");
			callback.onFailedDownloadDetailThumbsSeparate(info);

		}
	}


	public boolean downloadThumbnails(String thumbsURL){
		//TODO カタログ用詳細サムネのディレクトリを別途生成（ここのディレクトリはアプリ終了時に削除
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/");
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

//			File saveFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/detail/", info.getAssetId() + ".zip");
			File saveFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext), info.getAssetId() + ".zip");
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

			  while (-1 != (size = is.read(buffer))) {
				  zipOs.write(buffer, 0, size);
			  }

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
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/" + info.getAssetId() + "/");
		//TODO　起動時の読込してないから仮でサムネフォルダ作る
		FileUtility.makeDirectory(FileUtility.getThumbnailsRootPath(myContext));
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext));
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext) + info.getFolderName() + "/");

		try {
			URL url = new URL(thumbsURL);

           	DebugLog.instance.outputLog("value", "接続：" + thumbsURL);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setConnectTimeout(timeout - (int)(System.currentTimeMillis() - startTime));
	        connection.connect();
	        
			if(isCancelled()){
				return false;
			}
	        
           	DebugLog.instance.outputLog("value", "ファイル作成= " + info.getFileName());

	        File outputFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext) + info.getFolderName() + "/", info.getFileName());
	        FileOutputStream fos = new FileOutputStream(outputFile);

	        // ダウンロード開始
           	DebugLog.instance.outputLog("value", "ダウンロード開始");
           	connection.setReadTimeout(timeout - (int)(System.currentTimeMillis() - startTime));
	        InputStream is = connection.getInputStream();
	        byte[] buffer = new byte[1024 * 4];
	        int len = 0;
	        
			if(isCancelled()){
	            DebugLog.instance.outputLog("value", "キャンセル_書き込み開始前" + outputFile.getAbsolutePath());
	            if(outputFile.exists()) FileUtility.delFile(outputFile);
				return false;
			}

	        while ((len = is.read(buffer)) != -1) {
 	            fos.write(buffer, 0, len);
 	            
 	            if(isCancelled()){
 	            	DebugLog.instance.outputLog("value", "キャンセル_" + outputFile.getAbsolutePath());
 	            	fos.close();
 	            	is.close();
 	            	if(outputFile.exists()) FileUtility.delFile(outputFile);
 	            	return false;
 	            }
 	        }

	        fos.close();
	        is.close();
		} catch (IOException e) {
			return false;
		}

		return true;

	}

	/**
	 * zipダウンロード、解凍してthumbnailsフォルダに保存
	 * @param thumbsURL
	 * @return
	 */
	public boolean doDownloadThumbnailsZip(String thumbsURL){

		//TODO カタログ用詳細サムネのディレクトリを別途生成（ここのディレクトリはアプリ終了時に削除
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/");
//		FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/thumbnails/detail/" + info.getAssetId() + "/");
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext));
		FileUtility.makeDirectory(FileUtility.getDetailThumbnailsRootPath(myContext) + info.getAssetId() + "/");

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
//				File downloadFile = new File("/data/data/" + myContext.getPackageName() +  "/files/thumbnails/detail/" + info.getAssetId() + "/", filename[filename.length - 1]);
				File downloadFile = new File(FileUtility.getDetailThumbnailsRootPath(myContext) + info.getAssetId() + File.separator, filename[filename.length - 1]);
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


	//ファイルの「その他」
	//assetDetail＞fileAttribute＞isThumbnail=3,description!=qp
	private Array<ThumbInfo> getThumbsDetailFromJson(String jsonStr) throws JSONException {
		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::");
//		DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::jsonStr:" + jsonStr);
//		String[] returnArray = {"", ""};//0：更新フラグ、1:URL
		Array<ThumbInfo> returnArray = new Array<ThumbInfo>();

		JSONObject root = new JSONObject(jsonStr);
//		returnArray[2] = root.getString("assetId");
		JSONObject rootObj = root.getJSONObject("assetDetail");

		JSONArray fileArray = rootObj.getJSONArray("fileAttribute");
		for(int i = 0; i < fileArray.length(); i++){
			JSONObject childObj = fileArray.getJSONObject(i);

			// isThumbnailが3のファイルが「その他」、備考に"qp"との記述がない
			if(childObj.getString("isThumbnail").equals("3") && !childObj.getString("description").equals("qp") ){
				ThumbInfo info = new ThumbInfo(childObj.getString("downloadUrl"));
				info.setType(childObj.getString("fileName"));

				DebugLog.instance.outputLog("value", "getThumbsDetailFromJson::filename:" + childObj.getString("fileName"));

				returnArray.add(info);
			}
		}

		return returnArray;
	}



	private String downloadUrl = "";//最初はトークンなしのURLを通常に取得して、それを元にトークンありのURLを別途取得して代入
	private String[] tokenArray = null;
	private String fileName = "";

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
