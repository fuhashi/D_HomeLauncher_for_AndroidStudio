package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import java.io.File;

import android.content.Context;

public 	class ThumbInfo {
	ContentsFileName type = null;
	String fileName = "";
	String downloadURL = "";
	String assetId = "";//ドロワーなどのアセットが無いものは空文字のまま
	int thumbnailIndex = -1;
	String folderName = "";
	ContentsDataDto myCto = null;
	
	public ThumbInfo() {
		super();
	}
	
	public ThumbInfo(String downloadURL) {
		super();
		this.downloadURL = downloadURL;
	}

	public void setURL(String url){
		downloadURL = url;
	}
	
	public void setFileName(String name){
		this.fileName = name;
	}

	public void setType(ContentsFileName type){
		this.type = type;
	}
	public void setType(String name){
		setFileName(name);
		ContentsFileName[] alltype = ContentsFileName.values();
		for(ContentsFileName t : alltype){
			if(name.indexOf(t.getFileName()) != -1){
				this.type = t;
				break;
			}
		}
	}
	public void setAssetId(String id){
		assetId = id;
	}
	public void setAssetId(long id){
		assetId = String.valueOf(id);
	}
	public void setFolderName(String name){
		this.folderName = name;
	}
	public void setIndex(int num){
		this.thumbnailIndex = num;
	}
	public void setCto(ContentsDataDto cto){
		this.myCto = cto;
	}
	
	public ContentsFileName getFileType(){
		return type;
	}
	public String getDownloadUrl(){
		return downloadURL;
	}
	public String getAssetId(){
		return assetId;
	}
	public String getFileName(){
		return fileName;
	}
	public int getThumbnailIndex(){
		return thumbnailIndex;
	}
	public String getFolderName(){
		return folderName;
	}
	public ContentsDataDto getCto(){
		return myCto;
	}
	
	public boolean isExistThumbs(Context c){
//		String path = FileUtility.getDetailThumbnailsRootPath(c) + assetId + File.separator + type.getFileName();
		String path = FileUtility.getDetailThumbnailsRootPath(c) + folderName + File.separator + fileName;
		return FileUtility.isExistFile(path);
	}
	
	public String getThumbsPath(Context c){
		return FileUtility.getDetailThumbnailsRootPath(c) + folderName + File.separator + fileName;
	}
	
	public String getThumbsPathForLoad(Context c){
		return "/thumbnails/detail/" + folderName + File.separator + fileName;
	}

}
