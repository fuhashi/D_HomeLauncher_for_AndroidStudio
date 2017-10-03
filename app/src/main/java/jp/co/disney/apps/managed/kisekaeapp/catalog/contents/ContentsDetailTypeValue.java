package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

public enum ContentsDetailTypeValue {
	
	CONTENTS_DETAIL_TYPE_DEFAULT(0),

	//コンテンツ詳細種別（ウィジェットとアイコンのみ
	WIDGET_DETAIL_TYPE_BATTERY(1),
//	WIDGET_DETAIL_TYPE_(2),
//	WIDGET_DETAIL_TYPE_(3),
//	WIDGET_DETAIL_TYPE_(4),
//	WIDGET_DETAIL_TYPE_(5),
//	WIDGET_DETAIL_TYPE_(6),
//	WIDGET_DETAIL_TYPE_(7),
//	WIDGET_DETAIL_TYPE_(8),
//	WIDGET_DETAIL_TYPE_(9),
//	WIDGET_DETAIL_TYPE_(10),
	
	ICON_DETAIL_TYPE_DRAWER(51),
	ICON_DETAIL_TYPE_SHORTCUT(52);
	
	private int value;
	ContentsDetailTypeValue(int v) { this.value = v; }
    public int getValue() { return value; }


}
