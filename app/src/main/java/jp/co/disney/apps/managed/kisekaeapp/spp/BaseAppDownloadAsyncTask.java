package jp.co.disney.apps.managed.kisekaeapp.spp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import jp.co.disney.apps.managed.kisekaeapp.R;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
//import android.util.Log;

public class BaseAppDownloadAsyncTask extends
		AsyncTask<String, Integer, Boolean> implements OnCancelListener{
	private ProgressDialog dialog = null;
	Context myContext;
	private BaseAppDownloadTaskCallback callback;

	public BaseAppDownloadAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (BaseAppDownloadTaskCallback) context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

		dialog = new ProgressDialog(myContext);
		dialog.setTitle("Downloading...");
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(this);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setMax(100);
		dialog.setProgress(0);
		dialog.show();

	}

	private static String DOWNLOAD_FILE_NAME = "market.apk";
	private boolean flagDownload = false;
	@Override
	protected Boolean doInBackground(String... arg0) {

		try {
			//for(int i=0; i<10; i++){
				if(isCancelled()){
//					Log.d("value", "Cancelled!");
					//break;
					return false;
				}
				//Thread.sleep(1000);
				//publishProgress((i+1) * 10);
				//getResources().getString(R.string.URL) + "011200430.apk"

				String path = myContext.getResources().getString(R.string.URL);
				int filesize = 0; // file size temporary hard coded
				long start = System.currentTimeMillis();
				int bytesRead;
				int current = 0;
				int downloaded = 0;
				int timeout = 60000;
				FileOutputStream fos = null;
				BufferedOutputStream bos = null;
				byte[] mybytearray = null;

				URL url = new URL(path);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setConnectTimeout(timeout);
				connection.setReadTimeout(timeout);
				// ---------set request download--------
				connection.setDoInput(true);
				connection.connect();
				// --------------------------------------
				int lengthOfFile = connection.getContentLength();
				if (lengthOfFile <= 0) {
					connection.disconnect();
					flagDownload = false;
					return false;
				}
				filesize = lengthOfFile + 2;
				lengthOfFile += downloaded;
				// receive file
				mybytearray = new byte[filesize];
				BufferedInputStream is = new BufferedInputStream(connection.getInputStream());
				fos = myContext.openFileOutput(DOWNLOAD_FILE_NAME, Context.MODE_APPEND
						| Context.MODE_WORLD_READABLE);
				bos = new BufferedOutputStream(fos);
				bytesRead = is.read(mybytearray, 0, mybytearray.length);
				current = bytesRead;
				do{
					bytesRead = is.read(mybytearray, current,(mybytearray.length - current));
					if(bytesRead >= 0) {
						current += bytesRead;
						downloaded += bytesRead;
					}
					int progress_temp = (int) downloaded * 100 / lengthOfFile;
					//if(progress_temp % 10 == 0 && progress != progress_temp) {
					//	progress = progress_temp;
					//	dialog.setProgress(progress);
					//}
					//if(progress_temp % 10 == 0) {
					//	dialog.setProgress(progress_temp);
					//}
					publishProgress(progress_temp);

				}while (bytesRead > -1);
				bos.write(mybytearray, 0, current);
				bos.flush();
				bos.close();
				connection.disconnect();
				flagDownload = true;

			//}
		} catch (Exception e) {
//			Log.d("value", "CatchException in doInBackground");

		}
		//return 123L;

		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		dialog.setProgress(values[0]);
		//super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled() {
		try {
			dialog.dismiss();
		} catch (Exception e) {}
		//super.onCancelled();
	}

	@Override
	protected void onPostExecute(Boolean result) {
		try {
			dialog.dismiss();
		} catch (Exception e) {}
		//super.onPostExecute(result);

		if(flagDownload){
			// Intent
			Intent intent = new Intent(Intent.ACTION_VIEW);
			// MIME type
//			intent.setDataAndType(Uri.fromFile(new File("/data/data/"+ myContext.getPackageName() + "/files/" + DOWNLOAD_FILE_NAME)), "application/vnd.android.package-archive");
			intent.setDataAndType(Uri.fromFile(new File(myContext.getFilesDir().getAbsolutePath() + File.separator + DOWNLOAD_FILE_NAME)), "application/vnd.android.package-archive");
			myContext.startActivity(intent);
		}else{
			callback.onFailedDownloadApp();
		}
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		this.cancel(true);
	}

}
