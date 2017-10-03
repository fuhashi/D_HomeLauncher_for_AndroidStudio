package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database;

//本番暗号化
//import net.sqlcipher.SQLException;
//import net.sqlcipher.database.SQLiteDatabase;
//import net.sqlcipher.database.SQLiteException;
//import net.sqlcipher.database.SQLiteOpenHelper;

import java.io.File;
import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
//本番暗号化時コメントオン
//本番暗号化時コメントオン





/*
外川備忘録　13/5/13

読み込み用のDBを作成する時は
 //デフォルトのDB作る場合以下使用
 を使う。
 ・コンストラクタの中
 ・onCreateの中
 ・GLGameのonCreateの中
 の三か所
読み込み用のDBを作成後はルート端末からＤＢ抜き出して、
名前を"catfight_default.db"にしてaseetsフォルダにコピー
その後
 //デフォルトのDB作る場合以下使用
 の三か所をコメントにして、コンストラクタの中の
//コピーしたのを使う場合以下使用
を使用する。

ＤＢはとりあえず暗号化はしてない(確認するため)
暗号化時は
net.sqlcipher.database系のライブラリを使用して
//本番暗号化
のコメントの箇所、四か所をコメントオフしてパスワードで開くようにする。
読み込み用のDBを作成する時の箇所も見落とさないように注意。
暗号化しないで作ってしまうとそもそも開けないかな。
追加5/21
GLGameのonCreateの中
		// SQLCipherライブラリのイニシャルロード
		//本番暗号化
//		SQLiteDatabase.loadLibs(this);
コメントオフ

 */


public class BaseDatabase {

	public static final String DATABASE_NAME = "mypage.db";
	private static final int DATABASE_VERSION = 1;
//	private static final String DB_NAME_ASSET = "disneystyle_default.db";
	//The Android のデフォルトでのデータベースパス
	private static String DB_PATH;


	/** アイテムID */
//	public static final String COMMON_ID_COL = "_id";
	//テーブル名
	public static final String MYPAGE_SKIN_TABLE_NAME = "skindata", BADGE_SKIN_TABLE_NAME = "fromdm_skindata";
//	public static final String USERDATA_TABLE_NAME = "userdata";


	//カラム名
	//ARTテーブル
//	public static final String	ART_ARTID	=	"artid";


	//カラム配列
	/** カラム配列(ARTテーブル) */
//	public static final String[] ART_DATA_COLUMNS = {
//		ART_ARTID,
//		ART_STL001,
//		ART_STL002,
//		ART_THUMBVERSION,
//		ART_POINTADDED,
//		ART_DELETEFLG
//		};
	public static final String[] SKIN_DATA_COLUMNS = {
		DataBaseParam.COL_ASSET_ID.getParam(),
		DataBaseParam.COL_CONTENTS_TYPE.getParam(),
		DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(),
		DataBaseParam.COL_THEME_TAG.getParam(),
		DataBaseParam.COL_ADDED_DATE.getParam(),
		DataBaseParam.COL_THEME_SETTED_DATE.getParam(),
		DataBaseParam.COL_EXIST.getParam(),
		DataBaseParam.COL_FAVORITE.getParam(),
		DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam()
		};
	public static final String[] BADGE_SKIN_DATA_COLUMNS = {
		DataBaseParam.COL_ASSET_ID.getParam(),
		DataBaseParam.COL_CONTENTS_TYPE.getParam(),
		DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(),
		DataBaseParam.COL_ADDED_DATE.getParam()
		};


	private SQLiteDatabase mDb;
	private DatabaseHelper mDatabaseHelper;

	public static final boolean DEBUG_FLAG = true;
	//後で消す//TODO
////	public static boolean dbMadeNow = false;//nyanbatのデフォルト
//	public static boolean dbMadeNow = true;

	//コンストラクタ
		public BaseDatabase(Context context) {

//			DB_PATH = "/data/data/" + context.getApplicationContext().getPackageName() + "/databases/";
			DB_PATH = context.getDatabasePath(DATABASE_NAME).getParent();
			if(!DB_PATH.endsWith(File.separator)) DB_PATH = DB_PATH + File.separator;
			DebugLog.instance.outputLog("value5", "DBコンストラクタ_" + DB_PATH);

			//コピーしたのを使う場合以下使用
//			if(!dbMadeNow) setDatabase(context);

			 //デフォルトのDB作る場合以下使用
//			else{
			try {
				mDatabaseHelper = new DatabaseHelper(context);
			//本番暗号化
//				mDb = mDatabaseHelper.getWritableDatabase(password);
				mDb = mDatabaseHelper.getWritableDatabase();
			} catch (Exception e) {
				DebugLog.instance.outputLog("value", "BaseDatabase::Exception e:" + e.toString());
			}
//			}
			 //デフォルトのDB作る場合

		}
//		private void setDatabase(Context context) {
//			mDatabaseHelper = new DatabaseHelper(context);
//		    try {
//		    	mDatabaseHelper.createEmptyDataBase();
//		    	mDb = mDatabaseHelper.openDataBase();
//		    } catch (IOException ioe) {
//		        throw new Error("Unable to create database");
//		    } catch(SQLException sqle){
//		        throw sqle;
//		    }
//		}

		/**
		 * ヘルパーのクローズ
		 */
	    public void close() {
	    	try {
	    		mDatabaseHelper.close();
			} catch (Exception e) {
				DebugLog.instance.outputLog("value", "ItemDatabase::close::Exception e:" + e.toString());
			}
	    }

	public synchronized Long insertBadgeSkinData(long assetId, int type, int detailType, String addedDate) {

			DebugLog.instance.outputLog("value", "insertBadgeSkinData_" + assetId);

			ContentValues values = new ContentValues();
			values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
			values.put(DataBaseParam.COL_CONTENTS_TYPE.getParam(), type);
			values.put(DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(), detailType);
			values.put(DataBaseParam.COL_ADDED_DATE.getParam(), addedDate);

			return mDb.insert(BADGE_SKIN_TABLE_NAME, null /* nullColumnHack */, values);

	   }


	public synchronized Long insertSkinData(long assetId, int type, int detailType, String themeTag, String addedDate, boolean isExist, boolean isFavorite, boolean hasDL, String themeSetDate) {

		DebugLog.instance.outputLog("value", "insertSkinData_" + themeTag);

		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		values.put(DataBaseParam.COL_CONTENTS_TYPE.getParam(), type);
		values.put(DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(), detailType);
		values.put(DataBaseParam.COL_THEME_TAG.getParam(), themeTag);
		values.put(DataBaseParam.COL_ADDED_DATE.getParam(), addedDate);
		values.put(DataBaseParam.COL_THEME_SETTED_DATE.getParam(), themeSetDate);
		values.put(DataBaseParam.COL_EXIST.getParam(), isExist);
		values.put(DataBaseParam.COL_FAVORITE.getParam(), isFavorite);
		values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), hasDL);

		return mDb.insert(MYPAGE_SKIN_TABLE_NAME, null /* nullColumnHack */, values);

   }

//	public synchronized Long updateArtData(long assetId, int type, int detailType, String groupTag, String addedDate, boolean isExist, boolean isFavorite, boolean hasDL) {
//		
//		ContentValues values = new ContentValues();
//		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
//		values.put(DataBaseParam.COL_CONTENTS_TYPE.getParam(), type);
//		values.put(DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(), detailType);
//		values.put(DataBaseParam.COL_THEME_TAG.getParam(), groupTag);
//		values.put(DataBaseParam.COL_ADDED_DATE.getParam(), addedDate);
//		values.put(DataBaseParam.COL_EXIST.getParam(), isExist);
//		values.put(DataBaseParam.COL_FAVORITE.getParam(), isFavorite);
//		values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), hasDL);
//		
//		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?",new String[] {String.valueOf(assetId) });
//	}
//
//	public synchronized Long updateArtData_fromContentManager(int assetId, int type, int detailType, String groupTag, String addedDate, boolean isExist, boolean isFavorite, boolean hasDL) {
//		ContentValues values = new ContentValues();
//		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
//		values.put(DataBaseParam.COL_CONTENTS_TYPE.getParam(), type);
//		values.put(DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(), detailType);
//		values.put(DataBaseParam.COL_THEME_TAG.getParam(), groupTag);
//		values.put(DataBaseParam.COL_ADDED_DATE.getParam(), addedDate);
//		values.put(DataBaseParam.COL_EXIST.getParam(), isExist);
//		values.put(DataBaseParam.COL_FAVORITE.getParam(), isFavorite);
//		values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), hasDL);
//
//		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?",new String[] {String.valueOf(assetId) });
//	}


//	//アートデータ更新不要箇所を更新しない対応(暫定)
//	public synchronized Long updateArtData_zan(int assetId,
//			int art_nowsetting, int art_nowfavorite,int art_pointadded) {
//		ContentValues values = new ContentValues();
//		values.put(ART_ARTID,art_artid);
//		values.put(ART_NOWSETTING,art_nowsetting);
//		values.put(ART_NOWFAVORITE,art_nowfavorite);
//		values.put(ART_POINTADDED,art_pointadded);
//	        return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?",new String[] {String.valueOf(assetId) });
//	    }

//	//アートデータ更新_art_thumbavailable、art_seasoneventのみ更新
//	public synchronized Long updateArtData_onlyThumbavailable(int assetId, int art_seasonevent,int art_thumbavailable) {
//		ContentValues values = new ContentValues();
//		values.put(ART_SEASONEVENT,art_seasonevent);
//		values.put(ART_THUMBAVAILABLE,art_thumbavailable);
//
//	        return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?",new String[] {String.valueOf(assetId) });
//	    }
//	
	
	//isExist、isFavorite、その両方、を更新するメソッド
	public synchronized Long updateSkinData_isExistisFavorite(long assetId, boolean isExist, boolean isFavorite, boolean hasDL) {
		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		if(isExist){
			values.put(DataBaseParam.COL_EXIST.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_EXIST.getParam(), 0);
		}
		if(isFavorite){
			values.put(DataBaseParam.COL_FAVORITE.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_FAVORITE.getParam(), 0);
		}
		if(hasDL){
			values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), 0);
		}
		
		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?", new String[] {String.valueOf(assetId) });
	}

	public synchronized Long updateSkinData_onlyisExistAndDL(long assetId, boolean isExist, boolean hasDL, boolean isUpdate_addedDate) {
		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		if(isExist){
			values.put(DataBaseParam.COL_EXIST.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_EXIST.getParam(), 0);
		}
		if(hasDL){
			values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), 0);
		}
		
		if(isUpdate_addedDate){
			Date d = new Date();
			String date = FileUtility.getStringFormattedDayOnly(d);
			if(date.equals(""))	return 0L;
			values.put(DataBaseParam.COL_ADDED_DATE.getParam(), date);
		}
		
		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?", new String[] {String.valueOf(assetId) });
	}

	public synchronized Long updateSkinData_onlyThemeSetDate(long assetId) {
		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		
		Date d = new Date();
		String date = FileUtility.getStringFormattedDayOnly(d);
		if(date.equals(""))	return 0L;
		values.put(DataBaseParam.COL_THEME_SETTED_DATE.getParam(), date);
		
		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?", new String[] {String.valueOf(assetId) });
	}
	
	public synchronized Long updateSkinData_onlyisFavorite(long assetId, boolean isFavorite, boolean isUpdate_addedDate) {
		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		if(isFavorite){
			values.put(DataBaseParam.COL_FAVORITE.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_FAVORITE.getParam(), 0);
		}
		
		if(isUpdate_addedDate){
			Date d = new Date();
			String date = FileUtility.getStringFormattedDayOnly(d);
			if(date.equals(""))	return 0L;
			values.put(DataBaseParam.COL_ADDED_DATE.getParam(), date);
		}
		
		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?", new String[] {String.valueOf(assetId) });
	}

	public synchronized Long updateSkinData_onlyisMypage(long assetId, boolean isFavorite, boolean hasDL, boolean isUpdate_addedDate) {
		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		if(isFavorite){
			values.put(DataBaseParam.COL_FAVORITE.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_FAVORITE.getParam(), 0);
		}
		if(hasDL){
			values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), 1);
		}else{
			values.put(DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam(), 0);
		}
		
		if(isUpdate_addedDate){
			Date d = new Date();
			String date = FileUtility.getStringFormattedDayOnly(d);
			if(date.equals(""))	return 0L;
			values.put(DataBaseParam.COL_ADDED_DATE.getParam(), date);
		}
		
		return (long)mDb.update(MYPAGE_SKIN_TABLE_NAME, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?", new String[] {String.valueOf(assetId) });
	}
	

	public synchronized Long updateSkinData_onlyaddedDate(String tableName, long assetId) {
		ContentValues values = new ContentValues();
		values.put(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		//現在日時取得
		Date d = new Date();
		String date = FileUtility.getStringFormattedDayOnly(d);
		if(date.equals(""))	return 0L;
		values.put(DataBaseParam.COL_ADDED_DATE.getParam(), date);
		
		return (long)mDb.update(tableName, values, DataBaseParam.COL_ASSET_ID.getParam() + "=?", new String[] {String.valueOf(assetId) });
	}


	//ユーザーデータ部分更新
//	public synchronized Long updateUserData_part(int assetId,
//			int user_stl001, int user_stl002, int user_stl003, int user_stl004,
//			int user_col001, int user_col002, int user_col003, int user_col004,
//			int user_cha031, int user_artist001, int user_artist002,
//			int user_artist003, String user_birthday, String user_updateddate)  {
//		ContentValues values = new ContentValues();
//		if ((user_stl001 == 0) && (user_stl002 == 0) && (user_stl003 == 0)
//				&& (user_stl004 == 0) && (user_col001 == 0)
//				&& (user_col002 == 0) && (user_col003 == 0)
//				&& (user_col004 == 0) && (user_cha031 == 0)
//				&& (user_artist001 == 0) && (user_artist002 == 0)
//				&& (user_artist003 == 0) && (user_birthday.equals(""))
//				&& (user_updateddate.equals(""))) {//初期化時
////values.put(USER_USER_ID,user_user_id);
//			values.put(USER_STL001,user_stl001);
//			values.put(USER_STL002,user_stl002);
//			values.put(USER_STL003,user_stl003);
//			values.put(USER_ARTIST002,user_artist002);
//			values.put(USER_ARTIST003,user_artist003);
//			values.put(USER_BIRTHDAY,user_birthday);
//			values.put(USER_UPDATEDDATE,user_updateddate);
//
//		}else{//何かに値が入っている。
//
//			//if(!user_user_id.equals(""))values.put(USER_USER_ID,user_user_id);
//			if(user_stl001 !=0)	values.put(USER_STL001,user_stl001);
//			if(user_stl002 !=0)	values.put(USER_STL002,user_stl002);
//			if(user_stl003 !=0)	values.put(USER_STL003,user_stl003);
//			if(user_stl004 !=0)	values.put(USER_STL004,user_stl004);
//			if(user_cha029 !=0)	values.put(USER_CHA029,user_cha029);
//			if(user_cha030 !=0)	values.put(USER_CHA030,user_cha030);
//			if(user_cha031 !=0)	values.put(USER_CHA031,user_cha031);
//			if(user_artist001 !=0)	values.put(USER_ARTIST001,user_artist001);
//			if(user_artist002 !=0)	values.put(USER_ARTIST002,user_artist002);
//			if(user_artist003 !=0)	values.put(USER_ARTIST003,user_artist003);
//			if(!user_birthday.equals(""))	values.put(USER_BIRTHDAY,user_birthday);
//			if(!user_updateddate.equals(""))	values.put(USER_UPDATEDDATE,user_updateddate);
//		}
//
//		return (long)mDb.update(USERDATA_TABLE_NAME, values, USER_USER_ID + "=?",new String[] {user_user_id });
//	}

//	//ユーザーデータ部分更新
//	public synchronized Long updateUserData_part_init(int assetId,
//			int user_stl001, int user_stl002, int user_stl003, int user_stl004,
//			int user_col001, int user_col002, int user_col003, int user_col004,
//			int user_cha031, int user_artist001, int user_artist002,
//			int user_artist003, String user_birthday, String user_updateddate)  {
//		ContentValues values = new ContentValues();
//		if(!user_user_id.equals(""))values.put(USER_USER_ID,user_user_id);
//		if(user_stl001 !=0)	values.put(USER_STL001,user_stl001);
//		if(user_cha031 !=0)	values.put(USER_CHA031,user_cha031);
//		if(user_artist001 !=0)	values.put(USER_ARTIST001,user_artist001);
//		if(user_artist002 !=0)	values.put(USER_ARTIST002,user_artist002);
//		if(user_artist003 !=0)	values.put(USER_ARTIST003,user_artist003);
//		if(!user_birthday.equals(""))	values.put(USER_BIRTHDAY,user_birthday);
//		if(!user_updateddate.equals(""))	values.put(USER_UPDATEDDATE,user_updateddate);
//    return (long)mDb.update(USERDATA_TABLE_NAME, values, USER_USER_ID + "=?",new String[] {"" });
//	}

//	//ユーザーデータ更新不要箇所を更新しない対応(暫定)
//	public synchronized Long updateUserData_zan(int assetId,
//			int user_stl001, int user_stl002, int user_stl003, int user_stl004,
//			int user_col001, int user_col002, int user_col003, int user_col004,
//			int user_cha031, int user_artist001, int user_artist002,
//			int user_artist003, String user_birthday, String user_updateddate)  {
//		ContentValues values = new ContentValues();
////		values.put(USER_USER_ID,user_user_id);
//		values.put(USER_STL001,user_stl001);
//		values.put(USER_STL002,user_stl002);
//		values.put(USER_STL003,user_stl003);
//		values.put(USER_STL004,user_stl004);
//		values.put(USER_ARTIST002,user_artist002);
//		values.put(USER_ARTIST003,user_artist003);
////		values.put(USER_BIRTHDAY,user_birthday);
//		values.put(USER_UPDATEDDATE,user_updateddate);
//    return (long)mDb.update(USERDATA_TABLE_NAME, values, USER_USER_ID + "=?",new String[] {user_user_id });
//	}
	    /**
	     * Table 全件削除。
	     */
//	    public void deleteAllTable(String tableName) {
//	    	try{
//	    		mDb.delete(tableName, COMMON_ID_COL + " like '%'", null);
//	    	} catch(Exception e) {
//	    	}
//	    }

	    /**
	     * Table 指定のカラムによる削除。
	     * ※int型のみ
	     */
	    public void deleteByColumn(String tableName,  String columnName, long value) {
	    	try{
	    		DebugLog.instance.outputLog("value", "レコード削除：assetID：" + value);
	    		mDb.delete(tableName, columnName + " = " + value , null);
	    	} catch(Exception e) {
	    	}
	    }

	    /**
	     * 指定のカラムによる複数件取得。
	     * ※int型のみ
	     * @param tableName
	     * @param columnName
	     * @param value
	     * @return
	     */
	    public Cursor findTableByColumn(String tableName, String columnName, String value) {
	    	// rawQueryでSELECTを実行
	    	StringBuffer sql = new StringBuffer();
	    	sql.append("select *");
	    	sql.append(" from " + tableName);
	    	sql.append(" where ");
//	    	sql.append(columnName + " = " + value);
	    	sql.append(columnName + " = '" + value + "'");
	    	sql.append(";");
	    	DebugLog.instance.outputLog("value", "rawQueryでSELECTを実行:" + sql);
	    	return mDb.rawQuery(sql.toString(), null);
	    }

	    /**
	     * 指定のカラムによる複数件取得。
	     * ※int型のみ
	     * @param tableName
	     * @param columnName
	     * @param value
	     * @return
	     */
	    public Cursor findTableByColumn(String tableName, String columnName, Integer value) {
	    	// rawQueryでSELECTを実行
	    	StringBuffer sql = new StringBuffer();
	    	sql.append("select *");
	    	sql.append(" from " + tableName);
	    	sql.append(" where ");
//	    	sql.append(columnName + " = " + value.toString());
	    	sql.append(columnName + " = '" + value.toString() + "'");
	    	sql.append(";");
	    	return mDb.rawQuery(sql.toString(), null);
	    }
	    
	    /**
	     * 指定の複数カラムによる複数件取得（and
	     * ※int型のみ
	     * @param tableName
	     * @param columnName
	     * @param value
	     * @return
	     */
	    public Cursor findTableByColumnAnd(String tableName, String[] columnName, String[] value) {
	    	// rawQueryでSELECTを実行
	    	StringBuffer sql = new StringBuffer();
	    	sql.append("select *");
	    	sql.append(" from " + tableName);
	    	sql.append(" where ");
//	    	sql.append(columnName + " = " + value.toString());
	    	for(int i = 0; i < columnName.length - 1; i++){
		    	sql.append(columnName[i] + " = '" + value[i] + "' and ");
	    	}
	    	sql.append(columnName[columnName.length - 1] + " = '" + value[columnName.length - 1].toString() + "'");
	    	sql.append(";");
	    	return mDb.rawQuery(sql.toString(), null);
	    }
	    
	    public Cursor findTableByColumnAnd(String tableName, String[] columnName, Integer[] value) {
	    	String[] values = new String[value.length];
	    	for(int i = 0; i < value.length; i++){
	    		values[i] = value[i].toString();
	    	}
	    	return findTableByColumnAnd(tableName, columnName, values);
	    }


	    /**
	     * 指定の複数カラムによる複数件取得（or
	     * ※int型のみ
	     * @param tableName
	     * @param columnName
	     * @param value
	     * @return
	     */
	    public Cursor findTableByColumnOr(String tableName, String[] columnName, Integer[] value) {
	    	// rawQueryでSELECTを実行
	    	StringBuffer sql = new StringBuffer();
	    	sql.append("select *");
	    	sql.append(" from " + tableName);
	    	sql.append(" where ");
//	    	sql.append(columnName + " = " + value.toString());
	    	for(int i = 0; i < columnName.length - 1; i++){
		    	sql.append(columnName[i] + " = '" + value[i].toString() + "' or ");
	    	}
	    	sql.append(columnName[columnName.length - 1] + " = '" + value[columnName.length - 1].toString() + "'");
	    	sql.append(";");
	    	return mDb.rawQuery(sql.toString(), null);
	    }
	    
	    /**
	     * 指定した複数カラムによる複数件取得（or
	     */
	    public Cursor findTableByColumnOrAndOr(String tableName, String[] column1Name, Integer[] value1, String[] column2Name, Integer[] value2) {
	    	
	    	//WHERE   (deptno=20 OR deptno=30) AND job='MANAGER';
	    	
	    	// rawQueryでSELECTを実行
	    	StringBuffer sql = new StringBuffer();
	    	sql.append("select *");
	    	sql.append(" from " + tableName);
	    	sql.append(" where (");
//	    	sql.append(columnName + " = " + value.toString());
	    	for(int i = 0; i < column1Name.length - 1; i++){
		    	sql.append(column1Name[i] + " = '" + value1[i].toString() + "' or ");
	    	}
	    	sql.append(column1Name[column1Name.length - 1] + " = '" + value1[column1Name.length - 1].toString() + "')");
	    	
	    	sql.append(" and (");
	    	
	    	for(int i = 0; i < column2Name.length - 1; i++){
		    	sql.append(column2Name[i] + " = '" + value2[i].toString() + "' or ");
	    	}
	    	sql.append(column2Name[column2Name.length - 1] + " = '" + value2[column2Name.length - 1].toString() + "')");
	    	
	    	sql.append(";");
	    	
	    	DebugLog.instance.outputLog("value", "sql:" + sql.toString());
	    	return mDb.rawQuery(sql.toString(), null);
	    }

	    /**
	     * Table 全件取得。
	     */
	    public Cursor queryAllTable(String tableName, String[] columns) {
	        return mDb.query(tableName, columns, null,
	                null, null, null, null);
	    }
//	    /**
//	     * Table 全件取得 (有効な分のみ)。
//	     */
//	    public Cursor queryAllTable_valid(String tableName, String[] columns) {
//	        return mDb.query(tableName, columns, ART_THUMBAVAILABLE + "=? and " +ART_DELETEFLG+"=?",
//	        		new String[]{ "1","0" }, null, null, null);
//	    }

	public class DatabaseHelper extends SQLiteOpenHelper {

		Context dbContext;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			dbContext = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			//デフォルトのDB作る場合以下使用
//			if(dbMadeNow) makeDB(db);
			makeDB(db);
		}
		private void makeDB(SQLiteDatabase db){
			DebugLog.instance.outputLog("MyApp", "makeDB(SQLiteDatabase db){_");

			//boolean falseだったら0、trueだったら1
			//SKINテーブル
			db.execSQL("CREATE TABLE " + MYPAGE_SKIN_TABLE_NAME + " ("
					 + DataBaseParam.COL_ASSET_ID.getParam() + " INTEGER PRIMARY KEY,"
					 + DataBaseParam.COL_CONTENTS_TYPE.getParam() + " INTEGER,"
					 + DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() + " INTEGER,"
					 + DataBaseParam.COL_THEME_TAG.getParam() + " TEXT,"
					 + DataBaseParam.COL_ADDED_DATE.getParam() + " STRING,"
					 + DataBaseParam.COL_THEME_SETTED_DATE.getParam() + " STRING,"
					 + DataBaseParam.COL_EXIST.getParam() + " INTEGER,"
					 + DataBaseParam.COL_FAVORITE.getParam() + " INTEGER,"
					 + DataBaseParam.COL_HAS_DOWNLOAD_HISTORY.getParam() + " INTEGER"
//					 + DataBaseParam.COL_MYPAGE.getParam() + " INTEGER"
					+ ");");

			db.execSQL("CREATE TABLE " + BADGE_SKIN_TABLE_NAME + " ("
					 + DataBaseParam.COL_ASSET_ID.getParam() + " INTEGER PRIMARY KEY,"
					 + DataBaseParam.COL_CONTENTS_TYPE.getParam() + " INTEGER,"
					 + DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam() + " INTEGER,"
					 + DataBaseParam.COL_ADDED_DATE.getParam() + " STRING"
					+ ");");


		}


		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS DIARY");
			onCreate(db);
		}
		//デフォルトはコピーして使う方法
//		/**
//	     * asset に格納したデータベースをコピーするための空のデータベースを作成する
//	     *
//	     **/
//	    public void createEmptyDataBase() throws IOException{
//	        boolean dbExist = checkDataBaseExists();
//
//	        if(dbExist){
//	            // すでにデータベースは作成されている
//	        	DebugLog.instance.outputLog("value", "すでにデータベースは作成されている" );
//	        }else{
//	            // このメソッドを呼ぶことで、空のデータベースが
//	            // アプリのデフォルトシステムパスに作られる
//	        	//本番暗号化
////	        	DatabaseHelper.this.getReadableDatabase(password);
//	        	DatabaseHelper.this.getReadableDatabase();
//
//	            try {
//	                // asset に格納したデータベースをコピーする
//	                copyDataBaseFromAsset();
//	            } catch (IOException e) {
//	                throw new Error("Error copying database");
//	            }
//	        }
//	    }
	    /**
	     * 再コピーを防止するために、すでにデータベースがあるかどうか判定する
	     *
	     * @return 存在している場合 {@code true}
	     */
	    private boolean checkDataBaseExists() {
	        SQLiteDatabase checkDb = null;

	        try{
	            String dbPath = DB_PATH + DATABASE_NAME;
	          //本番暗号化
//	            checkDb = SQLiteDatabase.openDatabase(dbPath,password, null, SQLiteDatabase.OPEN_READWRITE);
	            checkDb = SQLiteDatabase.openDatabase(dbPath,null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
	        }catch(SQLiteException e){
	            // データベースはまだ存在していない
	        }

	        if(checkDb != null){
	            checkDb.close();
	        }
	        return checkDb != null ? true : false;
	    }
//	    /**
//	     * asset に格納したデーだベースをデフォルトの
//	     * データベースパスに作成したからのデータベースにコピーする
//	     * */
//	    private void copyDataBaseFromAsset() throws IOException{
//
//	        // asset 内のデータベースファイルにアクセス
////	        InputStream mInput = dbContext.getAssets().open(DB_NAME_ASSET);
//
//	        // デフォルトのデータベースパスに作成した空のDB
//	        String outFileName = DB_PATH + DATABASE_NAME;
//
//	        OutputStream mOutput = new FileOutputStream(outFileName);
//
//	        // コピー
//	        byte[] buffer = new byte[1024];
//	        int size;
//	        while ((size = mInput.read(buffer)) > 0){
//	            mOutput.write(buffer, 0, size);
//	        }
//
//	        //Close the streams
//	        mOutput.flush();
//	        mOutput.close();
//	        mInput.close();
//	    }
	    public SQLiteDatabase openDataBase() throws SQLException{
	        //Open the database
	        String myPath = DB_PATH + DATABASE_NAME;
	        //本番暗号化
//	        mDb = SQLiteDatabase.openDatabase(myPath, password,null, SQLiteDatabase.OPEN_READWRITE);
	        mDb = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READWRITE);
	        return mDb;
	    }

	    @Override
	    public synchronized void close() {
	        if(mDb != null){
	        	mDb.close();
	        	mDb = null;
	        }

	        super.close();
	    }

	}
}
