package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator;

import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;


public class PublishDateComparator implements java.util.Comparator<ContentsDataDto> {
	
	public PublishDateComparator() {
		super();
	}

	@Override
	public int compare(ContentsDataDto lhs, ContentsDataDto rhs) {
		Date lDate = SPPUtility.getDateCNTFormat(lhs.publishDate);
		Date rDate = SPPUtility.getDateCNTFormat(rhs.publishDate);
		
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
