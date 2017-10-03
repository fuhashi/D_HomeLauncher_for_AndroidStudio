package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator;

import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;


public class ThemeSettedDateComparatorDesc implements java.util.Comparator<MyPageDataDto> {
	
	public ThemeSettedDateComparatorDesc() {
		super();
	}

	/**
	 * 新しい順
	 */
	@Override
	public int compare(MyPageDataDto lhs, MyPageDataDto rhs) {
		Date lDate = SPPUtility.getDateCNTFormat(lhs.themeSetDate);
		Date rDate = SPPUtility.getDateCNTFormat(rhs.themeSetDate);
		
		if(lDate.getTime() < rDate.getTime()){
			return 1;
		} else if(lDate.getTime() == rDate.getTime()) {
			//アセットIDで比較？
			if(lhs.assetID <= rhs.assetID){
				return 1;
			}else{
				return -1;
			}
		} else {
			return -1;
		}
	}
	
}
