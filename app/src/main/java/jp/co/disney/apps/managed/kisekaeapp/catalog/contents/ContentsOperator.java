package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import java.io.File;
import java.util.HashMap;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.AddedDateComparatorDesc;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.ThemeSettedDateComparatorDesc;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.DataBaseParam;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import android.content.Context;
import android.os.Looper;

import com.badlogic.gdx.utils.Array;


/**
 * @author sagara
 *
 */
public class ContentsOperator {

	public static final ContentsOperator op = new ContentsOperator();
//	public static Context c;
	public static final String INTENT_ACTION_EXTRA_ASSET_ID = "now.setted.assetid",
								INTENT_ACTION_EXTRA_CONTENTS_TYPE = "now.setted.type",
								INTENT_ACTION_EXTRA_DETAIL_CONTENTS_TYPE = "now.setted.type.detail";

	public ContentsOperator() {
		super();
	}
	
//	public void setContext(Context c){
//		this.c = c;
//	}
	
	//テーマの諸々の情報も此処で取得？
	
	/*
テーマ一括変更時に変更される画像

壁紙
ドロワー背景
フォルダ画像（開閉
ドロワーアイコン
アプリアイコン

	 */
	
	/*
"/data/data/" + myContext.getPackageName() + "/files/skin/"

	 */
	
	/**
	 * アセットIDを指定して内部ストレージ内の該当ディレクトリのパスを取得する
	 * アセットIDの名前がついたディレクトリがなかったら空文字を返す
	 * @param assetId
	 * @return
	 */
	public String getDirectoryPath(Context c, String assetId, int typeValue){
		//assetIdのディレクトリが存在していたらそこのパスをreturn
		ContentsTypeValue type = ContentsTypeValue.getEnum(typeValue);
		if(type == null) return "";
		
		//FileUtility.makeDirectory("/data/data/" + myContext.getPackageName() + "/files/skin/");
		File dir = new File(FileUtility.getSkinPath(c, true, type));
		File[] files = dir.listFiles();
		
		for(File f : files){
			//ファイルのパスにassetIdが含まれるディレクトリがないかチェック
			if(f.isDirectory()){
				if(f.getName().equals(assetId)){
					DebugLog.instance.outputLog("value", "getDirectoryPath:" + f.getPath());
					return f.getPath();
				}
			}
		}
		
		return "";
		
	}
	
	
	/**
	 * 個別壁紙のパス（固定）を取得する。フォルダ内にファイルが無い場合は空文字を返す。
	 * @param context
	 * @return string
	 */
	public String getIndividualWpPath(Context c){
		String wpPath = FileUtility.getSkinRootPath(c) + "current" + File.separator + "wp" + File.separator + ContentsFileName.individualWp1.getFileName();
		if(FileUtility.isExistFile(wpPath)){
			return wpPath;
		}else{
			return "";
		}
		
	}

	
	/**
	 * 引数のコンテンツタイプに応じて該当ディレクトリの何がしかのファイルパスをreturnする
	 * @param type
	 * @return
	 */
	public String getCurrentThemeContentsImagePath(Context c, String dirPath, ContentsFileName type){
//		String path = "/data/data/" + con.getPackageName() + "/files/skin/current/theme/";
		
		String nowTheme = FileUtility.getNowThemeID(c);
		if(!nowTheme.equals("")){
			return dirPath + type.getFileName();
		}else{
			return "";
		}

	}
		
	//ホーム側から画像を使うタイミングは
	//通常のテーマ変更（ホームメニューの履歴からの変更でも一度画像ファイルをcurrentに移す
	
	
	/**
	 * 履歴に存在するアセットを返す（テーマ使用履歴用（なのでテーマと壁紙のみ対応
	 * @param type
	 * @return
	 */
	public Array<Long> getHistoryContentsAssetID(Context c, ContentsTypeValue type){
/*
		String nowTheme = "";
		if(list != null && list.length > 0){
			for(File f : list){
				if(f.isDirectory()){
					//TODO 実体ファイルが更にフォルダ構造を持っている形式だとしたら此処に条件追加
					nowTheme = f.getName();
				}
			}
		}
		return nowTheme;
		
 */
//		String path = "";
//		if(type == ContentsValue.CONTENTS_TYPE_THEME){
//			path = "/data/data/" + c.getPackageName() + "/files/skin/history/theme/";
//		}else if(type == ContentsValue.CONTENTS_TYPE_WP){
//			path = "/data/data/" + c.getPackageName() + "/files/skin/history/wp/";
//		}else{
//			return null;
//		}
//		
//		//中身のディレクトリのみ取得
//		Array<String> dirArray =  FileUtility.getDirectoryFromPath(c, path);
//		
//		//から、最後のディレクトリ名をlongとして抜き出す
//		
		
		//DBからisExistのものを取得してassetIDのみを返す
		MyPageDataAccess mAccess = new MyPageDataAccess(c);
		Array<MyPageDataDto> history = mAccess.findAllSkin_isExist(type.getValue());
		
		//設定順（isAddedDateを更新しているのでそれを基準に）でソート
		history.sort(new ThemeSettedDateComparatorDesc());
		
		Array<Long> returnArray = new Array<Long>();
		for(MyPageDataDto dto : history){
			returnArray.add(dto.assetID);
		}
		
		return returnArray;

	}
	
	/**
	 * テーマ使用履歴に表示するサムネイルのパス文字列を取得する
	 * @param type
	 * @param assetID
	 * @return
	 */
	public String getHistoryThumbnailImagePath(Context c, ContentsTypeValue type, long assetID){
		String path = FileUtility.getSkinRootPath(c) + "history" + File.separator;
//		String path = "/data/data/" + c.getPackageName() + "/files/skin/history/";
		
		//現在設定中のだったらcurrentから取得
		String nowTheme = FileUtility.getNowThemeID(c);
		if(nowTheme.equals(String.valueOf(assetID))){
			path = FileUtility.getSkinRootPath(c) + "current" + File.separator;
		}
		
		switch (type) {
		default:
			return "";
		case CONTENTS_TYPE_THEME:
			//assetIDをキーにDBからレコード取得して壁紙かテーマか場合わけ
//			MyPageDataAccess mAccess = new MyPageDataAccess(c);
//			Array<MyPageDataDto> array = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(assetID));
//			if(array != null && array.size >= 1){
//				if(array.get(0).contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
//					return path + "theme" + File.separator + assetID + File.separator +  ContentsFileName.historyThumb.getFileName();
//				}else{
//					return path + "wp" + File.separator + assetID + File.separator +  ContentsFileName.historyThumb.getFileName();
//				}
//			}
			return path + "theme" + File.separator + assetID + File.separator +  ContentsFileName.historyThumb.getFileName();
		case CONTENTS_TYPE_WP:
			return path + "wp" + File.separator + assetID + File.separator +  ContentsFileName.historyThumb.getFileName();
		}

	}
	


	//テーマ使用履歴関連
	//アセットIDと画像のパスを取る
	public Array<HashMap<Long, String>> getHistoryContentsInfo(Context c, ContentsTypeValue type){
		MyPageDataAccess mAccess = new MyPageDataAccess(c);
		Array<MyPageDataDto> history = mAccess.findAllSkin_isExist(type.getValue());
		
		//設定順でsort
		history.sort(new AddedDateComparatorDesc());
		
		Array<HashMap<Long, String>> returnArray = new Array<HashMap<Long, String>>();
		for(MyPageDataDto dto : history){
			HashMap<Long, String> map = new HashMap<Long, String>();
			String path = getHistoryThumbnailImagePath(c, type, dto.assetID);
			
			map.put(dto.assetID, path);
			returnArray.add(map);
		}
		return returnArray;
		
	}
	
	private DownloadSkinAsyncTask downloadSkinAsyncTask = null;
	public void cancelTask(){
		if(downloadSkinAsyncTask != null && !downloadSkinAsyncTask.isCancelled()){
			downloadSkinAsyncTask.cancel(true);
		}
	}
	
	public boolean callSetSkinTask(final Context c, long assetId){
		if(c == null) return false;
		MyPageDataAccess mAccess = new MyPageDataAccess(c);
		Array<MyPageDataDto> assetArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(assetId));
		if(assetArray == null || assetArray.size <= 0) return false;
		
		final ContentsDataDto cto = new ContentsDataDto(assetArray.get(0));
		Runnable r = new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				downloadSkinAsyncTask = new DownloadSkinAsyncTask(c);
				downloadSkinAsyncTask.execute(cto);
			}
		};
		new Thread(r).start();

		return true;
	}

	
}
