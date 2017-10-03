package jp.co.disney.apps.managed.kisekaeapp.catalog.screens;



public class DistanceComparator implements java.util.Comparator<ShuffleDto> {

	public DistanceComparator() {
		super();
	}

	@Override
	public int compare(ShuffleDto lhs, ShuffleDto rhs) {
		//※値が少ない順
		if(lhs.dist > rhs.dist){
			return 1;
		} else if(lhs.dist == rhs.dist) {
			//numで比較--小さいものから
			if(lhs.num >= rhs.num){
				return 1;
			}else{
				return -1;
			}
		} else {
			return -1;
		}
	}

}
