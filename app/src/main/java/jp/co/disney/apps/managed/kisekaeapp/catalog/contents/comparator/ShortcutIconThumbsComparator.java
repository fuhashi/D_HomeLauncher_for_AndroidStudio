package jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;
import jp.co.disney.apps.managed.kisekaeapp.iconpicker.ShortcutIconListData;


public class ShortcutIconThumbsComparator implements java.util.Comparator<ShortcutIconListData> {
	
	public ShortcutIconThumbsComparator() {
		super();
	}

	@Override
	public int compare(ShortcutIconListData lInfo, ShortcutIconListData rInfo) {
		
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
	
	private int setNumber(ShortcutIconListData i){
		if(i.getSelfIconPath().indexOf(ContentsFileName.appIconAlarm.getFileName()) != -1){
			return 0;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconBrowser.getFileName()) != -1){
			return 1;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconCalculator.getFileName()) != -1){
			return 2;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconCalendar.getFileName()) != -1){
			return 3;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconMail.getFileName()) != -1){
			return 4;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconMessage.getFileName()) != -1){
			return 5;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconSettings.getFileName()) != -1){
			return 6;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconCamera.getFileName()) != -1){
			return 7;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconGallery.getFileName()) != -1){
			return 8;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconPhone.getFileName()) != -1){
			return 9;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconContacts.getFileName()) != -1){
			return 10;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconPlayStore.getFileName()) != -1){
			return 11;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appIconMusic.getFileName()) != -1){
			return 12;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage1.getFileName()) != -1){
			return 13;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage2.getFileName()) != -1){
			return 14;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage3.getFileName()) != -1){
			return 15;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage4.getFileName()) != -1){
			return 16;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage5.getFileName()) != -1){
			return 17;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage6.getFileName()) != -1){
			return 18;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage7.getFileName()) != -1){
			return 19;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage8.getFileName()) != -1){
			return 20;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage9.getFileName()) != -1){
			return 21;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage10.getFileName()) != -1){
			return 22;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage11.getFileName()) != -1){
			return 23;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage12.getFileName()) != -1){
			return 24;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage13.getFileName()) != -1){
			return 25;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage14.getFileName()) != -1){
			return 26;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage15.getFileName()) != -1){
			return 27;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage16.getFileName()) != -1){
			return 28;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage17.getFileName()) != -1){
			return 29;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage18.getFileName()) != -1){
			return 30;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage19.getFileName()) != -1){
			return 31;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconImage20.getFileName()) != -1){
			return 32;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav1.getFileName()) != -1){
			return 33;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav2.getFileName()) != -1){
			return 34;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav3.getFileName()) != -1){
			return 35;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav4.getFileName()) != -1){
			return 36;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav5.getFileName()) != -1){
			return 37;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav6.getFileName()) != -1){
			return 38;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav7.getFileName()) != -1){
			return 39;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav8.getFileName()) != -1){
			return 40;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav9.getFileName()) != -1){
			return 41;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav10.getFileName()) != -1){
			return 42;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav11.getFileName()) != -1){
			return 43;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav12.getFileName()) != -1){
			return 44;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav13.getFileName()) != -1){
			return 45;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav14.getFileName()) != -1){
			return 46;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav15.getFileName()) != -1){
			return 47;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav16.getFileName()) != -1){
			return 48;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav17.getFileName()) != -1){
			return 49;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav18.getFileName()) != -1){
			return 50;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav19.getFileName()) != -1){
			return 51;
		}else if(i.getSelfIconPath().indexOf(ContentsFileName.appShortcutIconZav20.getFileName()) != -1){
			return 52;

		}
		return 53;
	}
}
