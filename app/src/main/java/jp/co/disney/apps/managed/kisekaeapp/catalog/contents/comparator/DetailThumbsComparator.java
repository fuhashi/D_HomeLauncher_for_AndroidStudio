package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;


public class DetailThumbsComparator implements java.util.Comparator<ThumbInfo> {
	
	public DetailThumbsComparator() {
		super();
	}

	@Override
	public int compare(ThumbInfo lInfo, ThumbInfo rInfo) {
		
		//詳細ページに表示する順番（とりあえず壁紙＞ドロワー＞アイコン＞ウィジェット＞exmapleで
		int l = setNumber(lInfo);
		int r = setNumber(rInfo);
		
		//大きい方が上
		if(l > r){
			return 1;
//		} else if(lDate.getTime() == rDate.getTime()) {
//			//アセットIDで比較？
//			if(lhs.assetID >= rhs.assetID){
//				return 1;
//			}else{
//				return -1;
//			}
		} else {
			return -1;
		}
	}
	
	private int setNumber(ThumbInfo i){
		if(i.getFileType() == ContentsFileName.ThumbDetailWp1){
			return 0;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailWp2){
			return 1;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailWp3){
			return 2;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailWp4){
			return 3;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailWp5){
			return 4;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailDrawer){
			return 5;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailIcon){
			return 6;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailIconShortut){
			return 7;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailWdtBattery){
			return 8;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx1){
			return 9;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx2){
			return 10;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx3){
			return 11;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx4){
			return 12;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx5){
			return 13;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx6){
			return 14;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx7){
			return 15;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx8){
			return 16;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx9){
			return 17;
		}else if(i.getFileType() == ContentsFileName.ThumbDetailEx10){
			return 18;
		}
		return 13;
	}
}
