package jp.co.disney.apps.managed.kisekaeapp.spp;



public class BannerWeightComparator implements java.util.Comparator<AdBannerLayout> {

	public BannerWeightComparator() {
		super();
	}

	@Override
	public int compare(AdBannerLayout lhs, AdBannerLayout rhs) {
		
		if(lhs.weight == 0){
			if(rhs.weight != 0){
				return 1;
			}
		}

		if(lhs.weight > rhs.weight){
			return 1;
		} else if(lhs.weight == rhs.weight) {
			if(Integer.parseInt(lhs.bannerId) > Integer.parseInt(rhs.bannerId)){
				return 1;
			}else{
				return -1;
			}
		} else {
			return -1;
		}
	}

}
