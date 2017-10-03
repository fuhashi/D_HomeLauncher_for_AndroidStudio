package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import android.content.Context;

import com.badlogic.gdx.utils.Array;

public class FileUtility {

	
	//フォーマットした文字列を返す
	public static String getStringFormattedDayOnly(Date date){
		SimpleDateFormat sdf =  new SimpleDateFormat("yyyy'-'MM'-'dd'T'kk':'mm':'ss");
		if(date == null){
			return "";
		}else{
			DebugLog.instance.outputLog("value", "日付:" + sdf.format(date));
			return sdf.format(date);
		}
	}

	public static boolean isExistFile(String filePath){
		boolean isExist = false;

		//指定されたFileName（path込み）のfileが存在するかどうか
		File file = new File(filePath);
		isExist = file.exists();

		return isExist;
	}

	public static void makeDirectory(String path){
		DebugLog.instance.outputLog("value", "mkdir:" + path);
		File f = new File(path);
//		f.getParentFile().mkdir();
		if(!f.exists())	f.mkdir();
	}
	
	private static int count = 0;
	public static int countFilesInDirectory(String path){
		File dir = new File(path);
		int count = 0;
		
		countFiles(dir.listFiles());
		return count;
	}
	
	private static void countFiles(File[] list){
		for (File f : list) {
			if (f.isDirectory()) {
				countFiles(f.listFiles());
			} else if (f.isFile()) {
				count++;
			}
		}
	}

	public static File[] getFilesInDirectory(String path){
		File dir = new File(path);
		return dir.listFiles();
	}
	
	public static void delFile(File delFile) {
//		File delFile = new File(delPath);
		
		if(!delFile.exists()){
			return;
		}
		
		if(delFile.isFile()){
			delFile.delete();
		}else if(delFile.isDirectory()){
			//ディレクトリの場合は全てのファイルを削除する
			File[] list = delFile.listFiles();
			
			for( File f : list ){
				delFile(f);
			}
			
			delFile.delete();
		}
	}
	
	//http://guchi-programmer.blogspot.jp/2013/07/java.html
	public static void copyFile(String motoPath, String sakiPath, boolean move) throws IOException{
		File motoF = new File(motoPath);
		File sakiF = new File(sakiPath);
		
		if(motoF.isDirectory()){
			if(!sakiF.exists()){
				sakiF.mkdir();
			}
			
			String[] files = motoF.list();
			
			for(String file : files) {
/*
File srcFile = new File(src, file);
File destFile = new File(dest, file);				
 */
//				File motoFile = new File(motoPath, file);
//				File sakiFile = new File(sakiPath, file);
				
				copyFile( motoPath + file, sakiPath + file, move);
			}
			
			if(move) motoF.delete();
		}else{
//			if(!sakiF.exists()){
//				sakiF.createNewFile();
//			}
			FileChannel motoChannel = null;
			FileChannel sakiChannel = null;
			try {
				motoChannel = new FileInputStream(motoF).getChannel();
				sakiChannel = new FileOutputStream(sakiF).getChannel();
				motoChannel.transferTo(0, motoChannel.size(), sakiChannel);
				
				if(move) motoF.delete();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw e;
			} finally {
				if(motoChannel != null) motoChannel.close();
				if(sakiChannel != null) sakiChannel.close();			
			}
		}
		
	}
	
	//現在のテーマのstringを取得
	public static String getNowThemeID(Context c){
//		String nowTheme = "";
//		File[] list = FileUtility.getFilesInDirectory("/data/data/" + c.getPackageName() + "/files/skin/current/theme/");
//		if(list != null && list.length > 0){
//			for(File f : list){
//				if(f.isDirectory()){
//					//TODO 実体ファイルが更にフォルダ構造を持っている形式だとしたら此処に条件追加
//					nowTheme = f.getName();
//				}
//			}
//		}

//		Array<String> dirArray = getDirectoryFromPath(c, "/data/data/" + c.getPackageName() + "/files/skin/current/theme/");
		Array<String> dirArray = getDirectoryFromPath(c, FileUtility.getSkinRootPath(c) + "current/theme/");
		if(dirArray.size > 0){
			return dirArray.get(0);
		}
		return "";
	}
	
	/*
	 * 指定したパスの中に含まれるディレクトリの名前を返す
	 */
	public static Array<String> getDirectoryFromPath(Context c, String path){
			Array<String> dirArray = new Array<String>();
			File[] list = getFilesInDirectory(path);
			if(list != null && list.length > 0){
				for(File f : list){
					if(f.isDirectory()){
						dirArray.add(f.getName());
					}
				}
			}
			return dirArray;
	}
	
	public static Array<String> getAssetIDFromThemeForFileName(Context c, String fileName){
		Array<String> pathArray = new Array<String>();
		
		String Cpath = getSkinPath(c, true, ContentsTypeValue.CONTENTS_TYPE_THEME);
		String Hpath = getSkinPath(c, false, ContentsTypeValue.CONTENTS_TYPE_THEME);

		File dir = new File(Cpath);
		File[] files = dir.listFiles();
		
		if(files != null){
			for (int i = 0; i < files.length; i++) {
				if(files[i].isDirectory()){
					File[] inFiles = getFilesInDirectory(files[i].getPath());
					for(int j = 0; j < inFiles.length; j++){
						File f = inFiles[j];
						if(f.getName().indexOf(fileName) != -1){
							pathArray.add(files[i].getName());
							j = inFiles.length;
							i++;
						}
					}
				}
				
			}			
		}
		
		File dirH = new File(Hpath);
		File[] filesH = dirH.listFiles();
		
		if(filesH != null){
			for (int i = 0; i < filesH.length; i++) {
				if(filesH[i].isDirectory()){
					File[] inFilesH = getFilesInDirectory(filesH[i].getPath());
					for(int j = 0; j < inFilesH.length; j++){
						File f = inFilesH[j];
						if(f.getName().indexOf(fileName) != -1){
							pathArray.add(filesH[i].getName());
							j = inFilesH.length;
							i++;
						}
					}
				}
				
			}			
		}
		
		
		for(String s : pathArray) DebugLog.instance.outputLog("value", "パス" + s);
		return pathArray;
		
	}
	
	
	/*
	 * 指定したディレクトリのパスを返す
	 */
	public static String getSkinRootPath(Context c){
		return c.getFilesDir() + "/skin/";
	}
	public static String getThumbnailsRootPath(Context c){
		return c.getFilesDir() + "/thumbnails/";
	}
	public static String getThumbnailsCachePath(Context c){
		return c.getCacheDir() + "/thumbnails/";
	}
	public static String getDetailThumbnailsRootPath(Context c){
		return c.getFilesDir() + "/thumbnails/detail/";
	}
	
	/*
	 * 指定したコンテンツ種別のパスを返す
	 */
	public static String getSkinPath(Context c, boolean isCurrent, ContentsTypeValue type){
		String path = "";
		if(isCurrent){
			path = getSkinRootPath(c) + "current";
		}else{
			path = getSkinRootPath(c) + "history";
		}
		if(isCurrent){
			if(type == ContentsTypeValue.CONTENTS_TYPE_THEME){
				path = path + File.separator + "theme" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON
					|| type == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T
					|| type == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T){
				path = path + File.separator + "icon" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_WIDGET
					|| type == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T){
				path = path + File.separator + "widget" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_WP
					|| type == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T){
				path = path + File.separator + "wp" + File.separator;
			}
		}else{
			if(type == ContentsTypeValue.CONTENTS_TYPE_THEME){
				path = path + File.separator + "theme" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON
					|| type == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T
					|| type == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T){
				path = path + File.separator + "icon" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_WIDGET
					|| type == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T){
				path = path + File.separator + "widget" + File.separator;
			}else if(type == ContentsTypeValue.CONTENTS_TYPE_WP
					|| type == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T){
				path = path + File.separator + "wp" + File.separator;
			}
		}
		
		DebugLog.instance.outputLog("value", "getSkinPath__" + path);
		return path;
	}
	
}
