package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator;

import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;


public class AddedDateComparatorAsc implements java.util.Comparator<MyPageDataDto> {
	
	public AddedDateComparatorAsc() {
		super();
	}

	/**
	 * 古い順
	 */
	@Override
	public int compare(MyPageDataDto lhs, MyPageDataDto rhs) {
		Date lDate = SPPUtility.getDateCNTFormat(lhs.addedDate);
		Date rDate = SPPUtility.getDateCNTFormat(rhs.addedDate);
		
		if(lDate.getTime() > rDate.getTime()){
			return 1;
		} else if(lDate.getTime() == rDate.getTime()) {
			//アセットIDで比較？
			if(lhs.assetID >= rhs.assetID){
				return 1;
			}else{
				return -1;
			}
		} else {
			return -1;
		}
	}
	
}
