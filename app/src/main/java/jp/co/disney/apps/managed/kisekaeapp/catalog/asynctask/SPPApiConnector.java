package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;



public class SPPApiConnector extends HttpConnector{

	// field
	// ----------------------------------------------------------------

//	private static final boolean IS_TEST = true;

//	public static final String URL_BASE = IS_TEST?
//		"http://test-meowbattle.satsuxbatsu.com/":
//		"http://meowbattle.satsuxbatsu.com/";

	// "http://test-meowbattle.uistore.net/":
	// "http://www-meowbattle.uistore.net/";
//	public static final String SSL_BASE = IS_TEST?
//		"http://test-meowbattle.satsuxbatsu.com/":
//		"http://meowbattle.satsuxbatsu.com/";
//	// "http://test-meowbattle.uistore.net/":
//	// "https://www-meowbattle.uistore.net/";
	
	
	public static final String API_GET_DOWNLOAD_TOKEN = "/webapi/v2/DownloadURLwithToken";
	public static final String API_GET_CONTENTS_LIST = "/webapi/v2/ContentList";
	public static final String API_GET_DOWNLOAD_TOKEN_UNTIED = "/webapi/v1/cnt/DownloadTokenUntied";
	public static final String API_GET_SPP_INFORMATION = "/webapi/v2/SPPInformation";
//	public static final String API_SEARCH_KEYWORD_ASSET_DIR = "/JsonAPI/V3.0/searchKeyWordAsset?";
//	public static final String API_CHECK_SESSION_KEY = "/spptool/SessionKeyCheck";

	private String urlBase = "";
	private String paramData = null;
	private URL url = null;
	private Param param = null;

	private int responseStatus = -1;
	private JSONArray responseData = null;
	private String responseDataString = "";

	// method
	// ----------------------------------------------------------------
	
	public SPPApiConnector(Context context) {
		super();
		
		//端末ID取得
		setDeviceId();
		//OSバージョン取得
		setOsVersion();
		//ICCID取得
		setUID(context);
		
		setTimeout(10 * 1000);//接続タイムアウト10秒
	}
	
	
	public void setEnviromentForSPP(Context con){
		String[] param = SPPUtility.getConnectEnv(con);
		domain = "ssapi.disney.co.jp";
		
		if(param[0].equals("liv")){
			domain = "ssapi.disney.co.jp";
		}else if(param[0].equals("stg")){
			domain = "staging.ssapi.disney.co.jp";
		}else if(param[0].equals("dev")){
			domain = "dev.ssapi.disney.co.jp";
		}
		
		enviroment = param[1];

	}


	/**
	 * Get response status
	 * @return Response status
	 */
	public int getResponseStatus(){
		return responseStatus;
	}

	/**
	 * Get response data length
	 * @return Response data length
	 */
	public int getResponseDataLength(){
		return responseData.length();
	}

	/**
	 * Get integer response data
	 * @param index Data index
	 * @param name Data name
	 * @return Integer response data
	 * @throws JSONException
	 */
	public int getIntegerResponseData( int index, String name )
		throws JSONException{
		if( ( index < 0 ) || ( responseData == null ) ||
			( index >= responseData.length() ) ) return -1;
		return responseData.getJSONObject( index ).getInt( name );
	}

	/**
	 * Get integer response data
	 * @param context Context object
	 * @param index Data index
	 * @param nameId Data name resource ID
	 * @return Integer response data
	 * @throws JSONException
	 */
	public int getIntegerResponseData( Context context, int index, int nameId )
		throws JSONException{
		return getIntegerResponseData( index, context.getString( nameId ) );
	}

	/**
	 * Get string response data
	 * @param index Data index
	 * @param name Data name
	 * @return String response data
	 * @throws JSONException
	 */
	public String getStringResponseData( int index, String name )
		throws JSONException{
		if( ( index < 0 ) || ( index >= responseData.length() ) ) return null;
		return responseData.getJSONObject( index ).getString( name );
	}

	/**
	 * Get string response data
	 * @param context Context object
	 * @param index Data index
	 * @param nameId Data name resource ID
	 * @return String response data
	 * @throws JSONException
	 */
	public String getStringResponseData( Context context, int index, int nameId )
		throws JSONException{
		return getStringResponseData( index, context.getString( nameId ) );
	}

	/**
	 * Get response JSON object
	 * @param index JSON object index
	 * @return Response JSON object
	 * @throws JSONException
	 */
	public JSONObject getResponseObject( int index ) throws JSONException{
		if( ( responseData == null ) || ( index < 0 ) ||
			( index >= responseData.length() ) ) return null;
		return responseData.getJSONObject( index );
	}
	
	
	public String getResponseBody(){
		return responseDataString;
	}
	
	

//	/**
//	 * Get API enumerator
//	 * @return API enumerator
//	 */
//	public Param getParam(){
//		return param;
//	}
//
	
	@Override
	protected void onConnectionStart() {
		DebugLog.instance.outputLog("value", "url:" + url);
		super.onConnectionStart();
	}

	private String myAuthorization = "";
	private boolean isAuthorizationNeeded = false;
	public void setAuthorization(String auth, boolean b){
		myAuthorization = auth;
		isAuthorizationNeeded = b;
	}
	

	public SPPApiConnector setParameter(){
		if(paramData != null){
			try {
				url = new URL(urlBase + paramData);
				return this;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		url = null;
		return this;
	}
	/**
	 * Add parameter to POST
	 * @param key Parameter key
	 * @param value Parameter value
	 * @return This ApiConnecter object
	 */
	public SPPApiConnector addParameter( String key, String value, boolean encodeOn ){
		if( ( key != null ) && ( key.length() > 0 ) &&
			( value != null ) && ( value.length() > 0 ) ){
			if(encodeOn) value = encodeParameter(value);
			if( paramData == null ){
				paramData = ( key + "=" + value );
			}else{
				paramData += ( "&" + key + "=" + value );
			}
		}
		return this;
	}

	/**
	 * Add parameter to POST
	 * @param key Parameter key
	 * @param value Parameter value
	 * @return This ApiConnecter object
	 */
	public SPPApiConnector addParameter( Param p, String value, boolean encodeOn ){
		if( ( p.toString() != null ) && ( p.toString().length() > 0 ) &&
//				( value != null ) && ( value.length() > 0 ) ){
			( value != null ) ){
			if(encodeOn) value = encodeParameter(value);
			if( paramData == null ){
				paramData = ( p.toString() + "=" + value );
			}else{
				paramData += ( "&" + p.toString() + "=" + value );
			}
		}
//		String urlStr = url.toString();
		return this;
	}

	/**
	 * Add parameter to POST
	 * @param context Context object
	 * @param keyId Parameter key resource ID
	 * @param value Parameter value
	 * @return This ApiConnecter object
	 */
	public SPPApiConnector addParameter( Context context, int keyId, String value, boolean encodeOn ){
		return addParameter( context.getString( keyId ), value, encodeOn );
	}
	
	/**
	 * Set Parameter
	 * @param api API enumerator to set
	 * @param isSsl Is SSL used or not
	 * @return This ApiConnecter object
	 */
	public SPPApiConnector setAccessEnviroment( Context con, String selectApi ){
		setEnviromentForSPP(con);
		urlBase = "https://" + domain + selectApi + "?";				
//		this.param = api;
		return this;
	}
	
	public SPPApiConnector setAccessEnviromentAccessKey(Context con, String assetId, String fileId, String selectApi){
		setEnviromentForSPP(con);
		urlBase = "https://" + domain + selectApi + "/" + assetId + "/" + fileId + "?";				
		return this;
	}

	/** Get URL to connect */
	@Override
	protected URL getUrl(){
		URL ret = url;
		url = null;
		return ret;
	}

	/** Set HTTP request header */
	@Override
	protected void setRequestHeader( HttpURLConnection connection ){
		super.setRequestHeader( connection );

		// Set POST method
//		connection.setDoOutput( true );
		try {
			connection.setRequestMethod("GET");
		} catch (ProtocolException e1) {
			e1.printStackTrace();
		}
		
		if(isAuthorizationNeeded){
			DebugLog.instance.outputLog("value", "setAutho!!!!!");
//			connection.setRequestProperty( "Authorization", "Bearer " + myAuthorization.substring(0, 20) );
			connection.setRequestProperty( "Authorization", "Bearer " + myAuthorization );
//			connection.setRequestProperty( "Authorization", "Bearer " + encodeParameter(myAuthorization) );
//			connection.setRequestProperty( "Authorization", Base64.encodeToString(("Bearer " + myAuthorization).getBytes(), Base64.DEFAULT) );
//			DebugLog.instance.outputLog("value", "setAutho!!!!!::" + Base64.encodeToString(("Bearer " + myAuthorization).getBytes(), Base64.DEFAULT));
		}
		
		for(int i = 0; i < connection.getHeaderFields().size(); i++){
			DebugLog.instance.outputLog("value", "connection.getHeaderField(" + i + "):" + connection.getHeaderFieldKey(i) + "/" + connection.getHeaderField(i));//http1.1ならば設定の必要なし
		}

		//今回はPOSTではなくGETなので不要
//		if( ( paramData == null ) || ( paramData.length() <= 0 ) ){
//			paramData = null;
//			return;
//		}
//
//		// Create post data
//		OutputStream stream = null;
//		try{
//			stream = connection.getOutputStream();
//		}catch( IOException e ){
//			onCaughtException( e );
//			return;
//		}
//		PrintStream out = new PrintStream( stream );
//		out.print( paramData );
//		out.close();
//
//		paramData = null;
	}

	/** Called when the connection is succeeded */
	@Override
	protected URL onConnectionSuccess( URL url, HttpURLConnection connection ){
		// Get content type
		String contentType = connection.getContentType();

		// Get content stream
		BufferedReader reader = null;
		try{
			reader = new BufferedReader( new InputStreamReader(
				connection.getInputStream(), getCharSet( contentType ) ) );
			StringBuffer buffer = new StringBuffer();
			for( String l = reader.readLine(); l != null; l = reader.readLine() ){
				buffer.append( l );
			}
			
			
			responseDataString = buffer.toString();
			JSONObject root = new JSONObject( responseDataString );
			responseStatus = root.getInt( "status" );
			responseData = root.getJSONArray( "data" );
			
			
			paramData = null;
			url = null;
		}catch( IOException e ){
			onCaughtException( e );
		}catch( JSONException e ){
			onCaughtException( e );
		}finally{
			try{
				reader.close();
			}catch( IOException e ){
				onCaughtException( e );
			}
		}
		return null;
	}
	
	
	

	// enumerate
	// ----------------------------------------------------------------

//	@Override
//	protected URL onConnectionFailure(URL url, HttpURLConnection connection) {
//		// Get content stream
//		BufferedReader reader = null;
//		try{
//			
//			reader = new BufferedReader(new InputStreamReader(connection.getErrorStream(), "UTF-8"));
//			StringBuilder sb = new StringBuilder();
//			char[] b = new char[1024];
//			int line;
//			while (0 <= (line = reader.read(b))) {
//			    sb.append(b, 0, line);
//			}
//
//
//		DebugLog.instance.outputLog("value", "failed!!!!!!!!!_" + sb.toString());
//
//		reader.close();
//		
//		}catch( IOException e ){
//			onCaughtException( e );
//		}finally{
//			try{
//				reader.close();
//			}catch( IOException e ){
//				onCaughtException( e );
//			}
//		}
//		
//		return super.onConnectionFailure(url, connection);
//	}


	/** APIs enumerate */
	public enum Param{
		
		download_from,
		site_cd,
		link_status,
		url,
		dlct,
		auto,
		dspp,
		secret,
		user_agent,
		spp_login_unnecessary_flg,
		app_id,
		iccid,
		id_token,
		iur,
		asset_id,
		dlskip,
		noPurchase,
		env,
		src,
		providedSiteId;

		// enumerator
		// ----------------------------------------------------------------

//		PARAM_ENVIROMENT{
//			@Override
//			public String getParamName(){
//				return "env";
//			}
//		},
//		PARAM_SRC{
//			@Override
//			public String getParamName(){
//				return "src";
//			}
//		},
//		PARAM_USER_ID{
//			@Override
//			public String getParamName(){
//				return "uid";
//			}
//		},
//		PARAM_RANKING{
//			@Override
//			public String getParamName(){
//				return "ranking";
//			}
//		},
//		PARAM_ID_TYPE{
//			@Override
//			public String getParamName(){
//				return "idType";
//			}
//		},
//		PARAM_ID{
//			@Override
//			public String getParamName(){
//				return "id";
//			}
//		},
//		PARAM_PROVIDED_SITE_ID{
//			@Override
//			public String getParamName(){
//				return "providedSiteId";
//			}
//		},
//		PARAM_SITE_ID{
//			@Override
//			public String getParamName(){
//				return "siteId";
//			}
//		},
//		PARAM_DEVICE_ID{
//			@Override
//			public String getParamName(){
//				return "deviceId";
//			}
//		},
//		PARAM_OS_VERSION{
//			@Override
//			public String getParamName(){
//				return "osVersion";
//			}
//		},
//		PARAM_CATEGORY_ID{
//			@Override
//			public String getParamName(){
//				return "categoryId";
//			}
//		},
//		PARAM_CARRIER_ID{
//			@Override
//			public String getParamName(){
//				return "carrierId";
//			}
//		},
//		PARAM_ASSET_TYPE_ID{
//			@Override
//			public String getParamName(){
//				return "assetTypeId";
//			}
//		},
//		PARAM_APPLICATION_FLAG{
//			@Override
//			public String getParamName(){
//				return "isApplicationFlag";
//			}
//		},
//		PARAM_DISNEY_FLAG{
//			@Override
//			public String getParamName(){
//				return "disneyFlag";
//			}
//		},
//		PARAM_PICKUP_FLAG{
//			@Override
//			public String getParamName(){
//				return "pickUpFlag";
//			}
//		},
//		PARAM_LIMIT_FLAG{
//			@Override
//			public String getParamName(){
//				return "limitFlag";
//			}
//		},
//		PARAM_USER_LEVEL_FROM{
//			@Override
//			public String getParamName(){
//				return "userLevelFrom";
//			}
//		},
//		PARAM_USER_LEVEL_TO{
//			@Override
//			public String getParamName(){
//				return "userLevelTo";
//			}
//		},
//		PARAM_PREMIUM_FLAG{
//			@Override
//			public String getParamName(){
//				return "premiumFlag";
//			}
//		},
//		PARAM_APP_PURCHASE_FLAG{
//			@Override
//			public String getParamName(){
//				return "inAppPurchaceFlag";
//			}
//		},
//		PARAM_NOT_PUBLISHED{
//			@Override
//			public String getParamName(){
//				return "isNotPublished";
//			}
//		},
//		PARAM_CHARACTER_ID{
//			@Override
//			public String getParamName(){
//				return "characterId";
//			}
//		},
//		PARAM_SEARCH_START_DATE{
//			@Override
//			public String getParamName(){
//				return "searchStartDate";
//			}
//		},
//		PARAM_SEARCH_END_DATE{
//			@Override
//			public String getParamName(){
//				return "searchEndDate";
//			}
//		},
//		PARAM_TAG_GROUP{
//			@Override
//			public String getParamName(){
//				return "tagGroup";
//			}
//		},
//		PARAM_TAG{
//			@Override
//			public String getParamName(){
//				return "tag";
//			}
//		},
//		PARAM_KEYWORD{
//			@Override
//			public String getParamName(){
//				return "keyWord";
//			}
//		},
//		PARAM_ON_FLAG{
//			@Override
//			public String getParamName(){
//				return "orFlag";
//			}
//		},
//		PARAM_NG_KEYWORD{
//			@Override
//			public String getParamName(){
//				return "ngKeyWord";
//			}
//		},
//		PARAM_SEARCH_OFFSET{
//			@Override
//			public String getParamName(){
//				return "searchOffset";
//			}
//		},
//		PARAM_SEARCH_LIMIT{
//			@Override
//			public String getParamName(){
//				return "searchLimit";
//			}
//		},
//		PARAM_ASSET_ID{
//			@Override
//			public String getParamName(){
//				return "assetId";
//			}
//		},
//		PARAM_DIVISION{
//			@Override
//			public String getParamName(){
//				return "division";
//			}
//		};

		// method
		// ----------------------------------------------------------------

//		/**
//		 * Get string API name
//		 * @return String API name
//		 */
//		public abstract String getParamName();

	}

}