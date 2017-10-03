package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;


public class RankingComparator implements java.util.Comparator<ContentsDataDto> {
	
	public RankingComparator() {
		super();
	}

	@Override
	public int compare(ContentsDataDto lhs, ContentsDataDto rhs) {
		
		//※値が多い順
		if(lhs.ranking < rhs.ranking){
			return 1;
		} else if(lhs.ranking == rhs.ranking) {
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
