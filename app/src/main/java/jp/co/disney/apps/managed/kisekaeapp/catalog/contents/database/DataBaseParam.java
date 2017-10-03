package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database;

public enum DataBaseParam {

	/* マイページDB用 */	
	//テーブル名
	TABLE_NAME_HISTORY("historydata"),
//	TABLE_NAME_FAVORITE("favodata"),
	
	//カラム名
	COL_ASSET_ID("assetid"),
	COL_CONTENTS_TYPE("contents_type"),
	COL_THEME_TAG("theme_tag"),
	COL_ADDED_DATE("added_date"),
	COL_THEME_SETTED_DATE("theme_set_date"),
	COL_EXIST("exist"),
	COL_FAVORITE("favorite"),
//	COL_MYPAGE("mypage"),
	COL_HAS_DOWNLOAD_HISTORY("has_dl_history"),
	
	COL_DETAIL_CONTENTS_TYPE("detail_contents_type");
	
	private String param;
	DataBaseParam(String v) { this.param = v; }
    public String getParam() { return param; }

}
