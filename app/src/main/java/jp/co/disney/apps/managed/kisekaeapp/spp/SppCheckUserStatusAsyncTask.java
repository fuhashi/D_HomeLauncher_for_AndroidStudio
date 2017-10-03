package jp.co.disney.apps.managed.kisekaeapp.spp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;

public class SppCheckUserStatusAsyncTask extends AsyncTask<String, Integer, Boolean>{

//	private ProgressDialog dialog = null;
	Context myContext;
	private SppCheckUserStatusTaskCallback callback;

	private Timer mTimer = null;
	private long startTime = 0;

	public SppCheckUserStatusAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (SppCheckUserStatusTaskCallback) context;
	}

//	public SppCheckDisneyStyleAsyncTask(Context context, SPPCheckDisneyStyleTaskCallback c) {
//		super();
//		myContext = context;
//		callback = c;
//	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		DebugLog.instance.outputLog("value", "sppcheck start!");
//		dialog = new ProgressDialog(myContext, R.style.MyDialogTheme);
////		dialog = new ProgressDialog(myContext);
//		// スタイルを設定
////		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//		dialog.setProgressStyle(android.R.style.Widget_ProgressBar);
//		dialog.setCancelable(false);
//		dialog.setCanceledOnTouchOutside(false);
//		dialog.show();

		//開始時刻を取得
		startTime = System.currentTimeMillis();

		mTimer = new java.util.Timer(true);
		mTimer.schedule(new TimerTask() {

			@Override
			public void run() {
				if((System.currentTimeMillis() - startTime) >= 10 * 1000){//計10秒
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

	public static final int AUTH_ERROR = -1, OFFLINE = -2;

	/**
	 * 課金会員としての状況を確認する
	 * returnは成否、状況の詳細は別途フィールド変数に格納する
	 */
	@Override
	protected Boolean doInBackground(String... arg0) {
		//保存動作。dismiss時には止める
		DebugLog.instance.outputLog("value", "Disney会員状況チェック");
		
		InputStream in = null;
		HttpURLConnection conn = null;
		try {
				if(isCancelled()){
					finishReason = OFFLINE;
					return false;
				}

				//オフラインチェック
				if(!SPPUtility.checkNetwork(myContext)){
					DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:オフライン");

					finishReason = OFFLINE;
					return false;

				}

				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:オフラインチェック完了");

				if(isCancelled()){
					finishReason = OFFLINE;
					return false;
				}

				//チェック
				String[] param = SPPUtility.getConnectEnv(myContext.getApplicationContext());
				String domain = "ssapi.disney.co.jp";//"live2.ssapi.disney.co.jp";
				if(param[0].equals("liv")){
					domain = "ssapi.disney.co.jp";
				}else if(param[0].equals("stg")){
					domain = "staging.ssapi.disney.co.jp";
				}
				
				//TODO
				URL url = new URL("https://" + domain + "/webapi/v1/SPPJudgment");
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-type", "application/json");
				conn.setRequestProperty("Accept","application/json");
				conn.setDoOutput(true);
				
				/*
https://www.google.co.jp/?client=firefox-b#q=android+httpurlconnection+post+body&gfe_rd=cr
http://stackoverflow.com/questions/20020902/android-httpurlconnection-how-to-set-post-data-in-http-body
http://osa030.hatenablog.com/entry/2015/05/22/181155
http://yukimura1227.blog.fc2.com/blog-entry-36.html
				 */

//				HttpPost httpPost = new HttpPost("https://" + domain + "/webapi/v1/SPPJudgment");
//				httpPost.setHeader("Content-type", "application/json");
//				httpPost.setHeader("Accept","application/json");
//				HttpClient httpClient = new DefaultHttpClient();
//				//http://hc.apache.org/httpclient-3.x/preference-api.html
//				httpClient.getParams().setParameter("http.protocol.version", HttpVersion.HTTP_1_1);

				if(isCancelled()){
					finishReason = OFFLINE;
					return false;
				}

				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:indentifier追加前");
				
				TelephonyManager manager = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
				String id = manager.getSimSerialNumber();
				if(id == null || id.equals("")){
					//SIMが入ってない
					finishReason = AUTH_ERROR;
					return false;					
				}

				//body部に追加
				//http://stackoverflow.com/questions/18188041/write-in-body-request-with-httpclient
				String postDataBody = SPPUtility.createDeviceIdentifier(myContext);//ここに本文を入れる。
				
				byte[] outputInBytes = postDataBody.getBytes("UTF-8");
				OutputStream os = conn.getOutputStream();
				os.write( outputInBytes );    
				os.close();
				
				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask::postDataBody" + postDataBody);
				DebugLog.instance.outputLog("value", "SppCheckDisneyStyleAsyncTask:接続直前");

				conn.connect();
				
				in = conn.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
 
                // InputStreamからbyteデータを取得するための変数
                StringBuffer bufStr = new StringBuffer();
                String temp = null;
 
                // InputStreamからのデータを文字列として取得する
                while((temp = br.readLine()) != null) {
                    bufStr.append(temp);
                }
 
                String responseBody = null;
                responseBody = bufStr.toString();
				
//				TelephonyManager manager = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
//				String id = manager.getSimSerialNumber();
//				if(id!=null&&!id.equals("")){
//				try {
//					responseBody = httpClient.execute(httpPost, new ResponseHandler<String>() {
//						@Override
//						public String handleResponse(HttpResponse response)
//								throws ClientProtocolException, IOException {
////							DebugLog.instance.outputLog("value", "GetUserProfileIntentService::getStatusCode:" + response.getStatusLine().getStatusCode());
//							if(HttpStatus.SC_OK == response.getStatusLine().getStatusCode()){
//								return EntityUtils.toString(response.getEntity(), "UTF-8");
//							}
//							return null;
////							return "";
//						}
//					});
//				} catch (ClientProtocolException e) {
//					e.printStackTrace();
//					finishReason = AUTH_ERROR;
//					return false;
//				} catch (IOException e) {
//					e.printStackTrace();
//					finishReason = AUTH_ERROR;
//					return false;
//				} finally {
//					httpClient.getConnectionManager().shutdown();
//				}
//
//				}else{
//					responseBody ="";
//				}

				if(responseBody != null){
					parseResponse(responseBody);
				}else{
					finishReason = AUTH_ERROR;
					return false;
				}

				return true;

		} catch (Exception e) {
			DebugLog.instance.outputLog("value", "CatchException in doInBackground:" + e.getMessage());
			finishReason = AUTH_ERROR;
			return false;

		} finally {
			try {
                if(conn != null) conn.disconnect();
                if(in != null) in.close();

            } catch (IOException ioe ) {
                ioe.printStackTrace();
            }
		}
	}
	
	int bd = 0;

	/**
	 * json解析
	 * @throws JSONException 
	 */
	private void parseResponse(String jsonStr) throws JSONException{

		DebugLog.instance.outputLog("value", "SppCheckDisneyStyle//////////////////response:" + jsonStr);

		//解析
		JSONObject rootObj = new JSONObject(jsonStr);
		JSONObject parseObj = rootObj.getJSONObject("spp_judgment");
		
//		price_plan = parseObj.getString("price_plan");
		
		bd = Integer.parseInt(parseObj.getString("business_domain_code"));

		if(bd == 1){
			if(jsonStr.indexOf("BDM=BlackMarket") != -1){//ビジネスドメインが1で、BlackMarketだったらonSと見なす
				bd = 3;
			}
		}

	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		//super.onProgressUpdate(values);
//		callback.onProgressDownloadThumbs(values[0]);
	}

	@Override
	protected void onCancelled() {
//		dialog.dismiss();

		DebugLog.instance.outputLog("value", "DisneyStyle会員チェックonCancelled");
		callback.onFailedCheckUserStatus(finishReason);
		super.onCancelled();
	}

	private int finishReason = 0;
	@Override
	protected void onPostExecute(Boolean result) {
//		dialog.dismiss();
		//super.onPostExecute(result);

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

		if(result){
			DebugLog.instance.outputLog("value", "User Status Check success!:");
			callback.onFinishedCheckUserStatus(bd);
		}else{
			DebugLog.instance.outputLog("value", "User Status Check failed!:");
			//ダイアログを出す
			callback.onFailedCheckUserStatus(finishReason);
		}
	}

}
