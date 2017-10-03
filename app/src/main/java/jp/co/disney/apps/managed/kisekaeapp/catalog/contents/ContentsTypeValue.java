package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

public enum ContentsTypeValue {

	//コンテンツ種別
	CONTENTS_TYPE_THEME(1),
	CONTENTS_TYPE_WP_IN_T(2),
	CONTENTS_TYPE_WIDGET_IN_T(3),
	CONTENTS_TYPE_DRAWER_ICON_IN_T(4),
	CONTENTS_TYPE_SHORTCUT_ICON_IN_T(8),
	
	CONTENTS_TYPE_WP(5),
	CONTENTS_TYPE_WIDGET(6),
	CONTENTS_TYPE_SHORTCUT_ICON(7);
	
	private int value;
	ContentsTypeValue(int v) { this.value = v; }
    public int getValue() { return value; }

    public static ContentsTypeValue getEnum(int num){
    	//SampleEnum[] enumArray = SampleEnum.values();
    	ContentsTypeValue[]	enumArray = ContentsTypeValue.values();
    		
    	for(ContentsTypeValue enumInt : enumArray){
    		if(num == enumInt.getValue()){
    			return enumInt;
    		}
    	}
    	return null;
    }
}
