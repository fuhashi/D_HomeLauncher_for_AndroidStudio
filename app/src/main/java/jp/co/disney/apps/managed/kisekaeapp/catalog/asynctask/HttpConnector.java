package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import android.content.Context;
import android.os.Build;
import android.telephony.TelephonyManager;



public abstract class HttpConnector{

	// field
	// ----------------------------------------------------------------

	// private static final String TAG = "HttpConnecter";
//	private static final String USER_AGENT = "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Trident/4.0)";

	private int responseCode = -1;
	private String responseMessage = null;
	private Exception exception = null;
	private final static String ENCODE = "UTF-8";
	
	private int timeout = 0;

	public static String domain = "sslib.disney.co.jp";
	public static String enviroment = "pub";
	public static String osVersion = "";
	public static String deviceID = "";
	public static String UID = "";

	// method
	// ----------------------------------------------------------------

	/** Connect HTTP URL */
	public final void connect(){
		onConnectionStart();
		for( URL url = getUrl(); url != null; url = connectUrl( url ) ){
			// DebugLog.instance.outputLog( TAG, "connect url:" + url );
			DebugLog.instance.outputLog("value", "connect url:" + url);
		}
		onConnectionEnd();
	}

	/**
	 * Get last response code
	 * @return Last response code
	 */
	public final int getResponseCode(){
		return responseCode;
	}

	/**
	 * Get last response message
	 * @return Last response message
	 */
	public final String getResponseMessage(){
		return responseMessage;
	}

	/**
	 * Get last caught exception
	 * @return Last caught exception
	 */
	public final Exception getCaughtException(){
		return exception;
	}

	
	public void setEnviromentForCNT(Context con){
		String[] param = SPPUtility.getConnectEnv(con);
		domain = "sslib.disney.co.jp";

		if(param[0].equals("liv")){
			domain = "sslib.disney.co.jp";
		}else if(param[0].equals("stg")){
			domain = "staging.sslib.disney.co.jp";
		}
		
		enviroment = param[1];

	}
	
	public void setOsVersion(){
		String[] os = Build.VERSION.RELEASE.split("\\.");
		
		osVersion = "Android+" + os[0] + "." + os[1];
	}
	
	public void setDeviceId(){
		deviceID = encodeParameter(Build.MODEL);	
	}
	
	public void setUID(Context context){
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		UID = manager.getSimSerialNumber();	
	}
	
	public void setTimeout(int millisec){
		timeout = millisec;
	}
	
	public String encodeParameter(String p){
		try {
			return URLEncoder.encode(p, ENCODE);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	

	/**
	 * Get character set from content type
	 * @param contentType Content type
	 * @return Character set
	 */
	protected final String getCharSet( String contentType ){
		if( contentType == null ) return "UTF-8";
		int index = contentType.indexOf( "charset=" );
		if( ( index < 0 ) || ( index > contentType.length() ) ){
			return "UTF-8";
		}
		String charSet = contentType.substring( index );
		return charSet.replaceAll( "charset=", "" );
	}

	/**
	 * Get URL to connect
	 * @return URL to connect
	 */
	protected URL getUrl(){
		return null;
	}

	/**
	 * Set HTTP request header
	 * @param connection HTTP URL connection object
	 */
	protected void setRequestHeader( HttpURLConnection connection ){
//		connection.addRequestProperty( "User-Agent", USER_AGENT );
//		connection.setInstanceFollowRedirects( false );
		connection.setUseCaches( false );
		connection.setDoInput( true );
//		connection.setDoOutput(true);
	}

	/** Called when the connection is started */
	protected void onConnectionStart(){}

	/** Called when the connection is finished */
	protected void onConnectionEnd(){}

	/**
	 * Called when the connection is succeeded
	 * @param url Current connected URL
	 * @param connection HTTP URL connection object
	 * @return URL to connect next
	 */
	protected URL onConnectionSuccess(
		URL url, HttpURLConnection connection ){
		return null;
	}

	/**
	 * Called when the connection is failed
	 * @param url Current connected URL
	 * @param connection HTTP URL connection object
	 * @return URL to connect next
	 */
	protected URL onConnectionFailure(
		URL url, HttpURLConnection connection ){
		return null;
	}

//	/**
//	 * Called when the connection is redirected
//	 * @param url Current connected URL
//	 * @param connection HTTP URL connection object
//	 * @return URL to connect next
//	 */
//	protected URL onConnectionRedirection(
//		URL url, HttpURLConnection connection ){
//		// Get location header
//		String location = connection
//			.getHeaderField( "Location" );
//		if( location == null ) return null;
//
//		// Create URL to connect next
//		try{
//			url = new URL( location );
//		}catch( MalformedURLException e ){
//			onCaughtException( e );
//			url = null;
//		}
//		return url;
//	}

	/**
	 * Called when the exception is caught
	 * @param e Caught exception object
	 */
	protected void onCaughtException( Exception e ){
		exception = e;
		e.printStackTrace();
	}

	/**
	 * Connect to URL
	 * @param url URL to connect
	 * @return URL to connect next
	 */
	private URL connectUrl( URL url ){
		HttpURLConnection connection = null;
		try{

			// Open connection
			connection = (HttpURLConnection)url.openConnection();
			
			connection.setReadTimeout(timeout);
			connection.setConnectTimeout(timeout);

			// Set request header
			setRequestHeader( connection );
			
			connection.connect();

			// Get response code
			responseCode = connection.getResponseCode();
			responseMessage = connection.getResponseMessage();
			// DebugLog.instance.outputLog( TAG, "responseCode:" + responseCode );
			switch( responseCode / 100 ){
				case 2:
					// 2xx Success
					url = onConnectionSuccess( url, connection );
					break;
//				case 3:
//					// 3xx Redirection
//					url = onConnectionRedirection( url, connection );
//					break;
				default:
					// Network Error
					url = onConnectionFailure( url, connection );
					break;
			}
//		}catch( SocketTimeoutException e){
//			
		}catch( IOException e ){
			onCaughtException( e );
			url = null;
		}finally{
			// Close HTTP connection
			connection.disconnect();
		}
		return url;
	}

}