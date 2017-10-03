package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database;

import java.io.Serializable;


public class MyPageDataDto implements Serializable{

	public long		assetID;
	public int		contentsType;
	public int  	detailContentsType;
	public String	themeTag;
	
	public boolean	isExist;//スキンの内部ストレージの有無の判断に使用
	public boolean	hasDownloadHistory;
	public boolean	isFavorite;
//	public boolean	isMypage;//テーマ使用履歴での実体ファイルの有無とは連動しない
	
	public String	addedDate;
	public String	themeSetDate;
	
	public MyPageDataDto() {
		super();
		this.assetID = 0;
		this.contentsType = 0;
		this.detailContentsType = 0;
		this.themeTag = "";
		this.isExist = false;
		this.hasDownloadHistory = false;
		this.isFavorite = false;
//		this.isMypage = false;
		this.addedDate = "";
		this.themeSetDate = "";
		
	}

}
