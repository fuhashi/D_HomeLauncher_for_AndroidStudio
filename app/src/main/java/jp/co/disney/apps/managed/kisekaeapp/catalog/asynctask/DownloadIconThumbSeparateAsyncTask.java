package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.CntApiConnector.Param;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
//import android.util.Log;


/**
 * アイコンのサムネイルを全て個別取得する
 * @author sagara
 *
 */
public class DownloadIconThumbSeparateAsyncTask extends
		AsyncTask<ContentsDataDto, Integer, Boolean>{

	Context myContext;
	long startTime = 0;
	private DownloadIconThumbSeparateTaskCallback callback;
	private ContentsDataDto cto = null;

	private int timeout = 15 * 1000;
	Timer   mTimer   = null;

	public DownloadIconThumbSeparateAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (DownloadIconThumbSeparateTaskCallback) context;//あとで要コメント解除
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

	private boolean flagDownload = false;

	/**
	 * return t（成功）/f（失敗）
	 */
	@Override
	protected Boolean doInBackground(ContentsDataDto... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "アイコンサムネ保存task_" + arg0.length);

		//パラメータ指定が足りなかったらcancel
		if(arg0.length < 1) return false;
		
		cto = arg0[0];

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
		//アセットを検索して該当のアセットの情報を取ってくる。
		long assetID = arg0[0].assetID;
		
		//キャッシュフォルダ生成
		FileUtility.makeDirectory(myContext.getCacheDir().getAbsolutePath());
		FileUtility.makeDirectory(myContext.getCacheDir().getAbsolutePath() + File.separator + assetID + File.separator);
		
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

		if(responseBody.equals("")) return false;

		DebugLog.instance.outputLog("value", "connect:responseBody" + responseBody);

		try {
			HashMap<String, String> urlMap = parseResponse(responseBody);
			
			if(urlMap == null) return false;
			
			//ダウンロード開始
			for(String key : urlMap.keySet()){
				DebugLog.instance.outputLog("value", "connect:downloadURL:" + urlMap.get(key));
				
				if(isCancelled()){
					return false;
				}
				
				flagDownload = downloadThumbnailFile(key, urlMap.get(key));
				
				if(!flagDownload) return false;
			}
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
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

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		//どの段階で失敗ないし明示的キャンセルが来たかを知っている必要がある
		if(cto == null){
			callback.onFailedDownloadIconThumbsSeparateNetwork(0);
		}else{
			callback.onFailedDownloadIconThumbsSeparateNetwork(cto.assetID);
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
			callback.onFailedDownloadIconThumbsSeparateNetwork(cto.assetID);
			return;
		}

		if(flagDownload){
//			DebugLog.instance.outputLog("value", "Thumb download success!:" + info.getFileName() + ":" + (System.currentTimeMillis() - startTime));
			callback.onFinishedDownloadIconThumbsSeparate(cto.assetID);

		}else{
			//ダウンロード時以外での失敗
//			DebugLog.instance.outputLog("download", "onPostExecute:flagDownloadFalse");
			callback.onFailedDownloadIconThumbsSeparate(cto.assetID);

		}
	}


	////////////////////////
	
	
	
	//jsonを解析してアイコンサムネイルのダウンロードURLを取得
	private HashMap<String, String> parseResponse(String response) throws JSONException{
		HashMap<String, String> map = new HashMap<String, String>();
		
		//assetInfo[]>assetDetail>fileAttribute[]>isThumbnail=="3"&&description=="isIconThumb"
		JSONObject rootObj = new JSONObject(response);
		JSONArray assetInfoAry = rootObj.getJSONArray("assetInfo");

		//assetDetail(これが各個Asset情報)
		if(assetInfoAry != null && assetInfoAry.length() > 0){
			//※assetInfoは1つの想定
			for(int i = 0; i < assetInfoAry.length(); i++){
				//assetDetailを探す
				JSONObject infoObj = assetInfoAry.getJSONObject(i);

				JSONObject assetDetail = infoObj.getJSONObject("assetDetail");
				JSONArray fileArray = assetDetail.getJSONArray("fileAttribute");
				
				for(int j = 0; j < fileArray.length(); j++){
					JSONObject fileObj = fileArray.getJSONObject(j);
					
					// isThumbnailが3のファイルが「その他」、備考欄にアイコンサムネである旨
					if(fileObj.getInt("isThumbnail") == 3 && fileObj.getString("description").equals("isIconThumb") ){
						map.put(fileObj.getString("fileName"), fileObj.getString("downloadUrl"));
					}
				}
			}
		}
		
		//ここで、mapの中にないけどcacheフォルダにある同テーマのアイコンファイルがあったら削除しておく
		File[] iconFile = FileUtility.getFilesInDirectory(myContext.getCacheDir().getAbsolutePath() + File.separator + cto.assetID + File.separator);
		//このファイルの中にmapのfileNameに含まれるic始まりの名前と合致しないものがあったら削除
		for(int i = 0; i<iconFile.length; i++){
			File f = iconFile[i];
			
			if(isCancelled()){
				return null;
			}
			
			boolean isSameFileExist = false;

			int index = 0;
			for(String key : map.keySet()){
				index++;
				
				if(isCancelled()){
					return null;
				}
				
				String mapFileName = formatFileName(key);
				
				//同じかチェック
				if(f.getName().equals(mapFileName)){
					isSameFileExist = true;
				}
			}
			
			//mapの走査が終わってもisSameFileExist=falseだったら削除
			if(!isSameFileExist) f.delete();
			
		}

		
		/*
			for(String key : urlMap.keySet()){
				DebugLog.instance.outputLog("value", "connect:downloadURL:" + urlMap.get(key));
				
				if(isCancelled()){
					return false;
				}
				
				flagDownload = downloadThumbnailFile(key, urlMap.get(key));
				
				if(!flagDownload) return false;
			}

		 */
		
		return map;

	}
	
	private String formatFileName(String fileName){
       	//正規表現が面倒なので一旦".png"は削除
      	String formatName = fileName.replace(".png", "");
       	
       	DebugLog.instance.outputLog("value", "ファイル作成（整形中）= " + formatName);
       	
       	formatName = formatName.replaceAll("(_T\\d{5})", "");
       	formatName = formatName + ".png";
       	
       	DebugLog.instance.outputLog("value", "ファイル作成（整形後）= " + formatName);

       	return formatName;
       	
	}

	private boolean downloadThumbnailFile(String fileName, String thumbsURL){
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
	        
           	DebugLog.instance.outputLog("value", "ファイル作成= " + fileName);
           	
           	//ファイル名からテーマIDを削除
           	fileName = formatFileName(fileName);

	        File outputFile = new File(myContext.getCacheDir().getAbsolutePath() + File.separator + String.valueOf(cto.assetID) + File.separator, fileName);
	        if(outputFile.exists()){
	        	DebugLog.instance.outputLog("value", "既存ファイル= " + fileName);
	        	return true;
	        }
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

//	private String downloadUrl = "";//最初はトークンなしのURLを通常に取得して、それを元にトークンありのURLを別途取得して代入



//	@Override
//	public void onCancel(DialogInterface dialog) {
//		this.cancel(true);
//	}

}
