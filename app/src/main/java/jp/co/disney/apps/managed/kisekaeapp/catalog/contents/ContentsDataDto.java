package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import java.security.PublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.badlogic.gdx.utils.Array;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;



public class ContentsDataDto extends MyPageDataDto{

	/*
	アセットID
	コンテンツ種類
	色
	キャラ
	オススメon/off
	オススメ番号
	アセットの公開日付？もしくは登録順に昇順でIDが振られるので、アセットIDで並べ替えればいいかも
	テーマタグ（子アセットを引っ掛けるため
	historyフォルダ、currentフォルダにデータがあるものは「ダウンロード済み」on
	 */
//	public long		assetID;//Stringのほうがいい？？？？要検討
//	public int		contentsType;
//	public int		color;//この辺は複数になるかも
	public Array<ContentsCharaValue>	chara;//この辺は複数になるかも
	public boolean	pickup;
	public int		pickupNumber;
	public String	publishDate;//これを新着順の順序判断にする（公開日から一定期間はNew!を出す
	public String	publishDateForDisplay;
//	public String	themeTag;
//	public boolean	isExist;
	
	public int		ranking;
	
	public boolean	isPremium;
	public boolean	isLimitted;
	
//	public boolean	isSettable;
	
	public int		qpNum;
	
//	public boolean	nowSetting;
	
	/*
	ランキングはランキングAPIにて順番だけ（アセットID）取得して並べ替える
	 */
	
	/*
	マイページ表示コンテンツ、表示順番はDBから取得
	ダウンロード済みとして表示するものは「ダウンロード済み」がonになっているアセット（起動時に判断する想定
	 */
//	public boolean	isFavorite;
//	public String	addedDate;
	
	/*
	 テーマのアセットだった場合のみ、テーマが含む壁紙数、ウィジェット数、アイコン数を保持する
	 */
//	public int		containedWP;
//	public int		containedWidget;
//	public int		containedIcon;	
	
	public ContentsDataDto(MyPageDataDto dto){
		this.assetID = dto.assetID;
		this.contentsType = dto.contentsType;
		this.detailContentsType = dto.detailContentsType;
		this.themeTag = dto.themeTag;
		this.isExist = dto.isExist;
		this.hasDownloadHistory = dto.hasDownloadHistory;
		this.isFavorite = dto.isFavorite;
//		this.isMypage = dto.isMypage;
		this.addedDate = dto.addedDate;
		this.themeSetDate = dto.themeSetDate;

//		this.color = 0;
		this.chara = new Array<ContentsCharaValue>();
		this.pickup = false;
		this.pickupNumber = 0;
		this.publishDate = "";
		this.publishDateForDisplay = "";
		this.ranking = 0;
		this.isPremium = false;
		this.isLimitted = false;
//		this.isSettable = true;
		this.qpNum = 1;
		
	}
	
	public ContentsDataDto() {
		super();
//		this.assetID = 0;
//		this.contentsType = 0;
//		this.color = 0;
		this.chara = new Array<ContentsCharaValue>();
		this.pickup = false;
		this.pickupNumber = 0;
		this.publishDate = "";
		this.publishDateForDisplay = "";
//		this.themeTag = "";
//		this.isExist = false;
		this.ranking = 0;
		this.isPremium = false;
		this.isLimitted = false;
//		this.isSettable = true;
		this.qpNum = 1;
		
//		this.isFavorite = false;
//		this.addedDate = "";
		

	}
	
	public void setDataFromDB(MyPageDataDto dto) {
		this.assetID = dto.assetID;
		this.contentsType = dto.contentsType;
		this.detailContentsType = dto.detailContentsType;
		this.themeTag = dto.themeTag;
		this.isExist = dto.isExist;
		this.hasDownloadHistory = dto.hasDownloadHistory;
		this.isFavorite = dto.isFavorite;
		this.addedDate = dto.addedDate;
		this.themeSetDate = dto.themeSetDate;
	}
	
	public String getContentsTypeId(){
		if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
			DebugLog.instance.outputLog("value", "テーマのアセット種別");
			return "ct_dnkp";
//		}else if(contentsType	== ContentsValue.CONTENTS_TYPE_WP.getValue()){
//			return "";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
			DebugLog.instance.outputLog("value", "壁紙inThemeのアセット種別");
			return "ct_dnkpk";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()){
			DebugLog.instance.outputLog("value", "ウィジェットのアセット種別");
			return "ct_dnksw";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
			DebugLog.instance.outputLog("value", "ウィジェットinThemeのアセット種別");
			return "ct_dnkpw";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()){
			DebugLog.instance.outputLog("value", "ショートカットアイコンのアセット種別");
			return "ct_dnksi";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()){
			DebugLog.instance.outputLog("value", "ドロワーアイコンinThemeのアセット種別");
			return "ct_dnkpi";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
			DebugLog.instance.outputLog("value", "ショートカットアイコンinThemeのアセット種別");
			return "ct_dnkps";
		}
		return "";

	}
	
	public String getContentsTypeDirectoryName(){
		if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
			return "theme";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_WP.getValue() || contentsType	== ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
			return "wp";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() || contentsType	== ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()){
			return "widget";
		}else if(contentsType	== ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() || contentsType	== ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue() || contentsType	== ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
			return "icon";
		}
		return "";
	}
	
	public String getDateOfContents(){
		if(publishDate.equals("")) return publishDateForDisplay;
		if(publishDateForDisplay.equals("")){
			SimpleDateFormat sdfBefore =  new SimpleDateFormat("yyyy'-'MM'-'dd'T'kk':'mm':'ss");
			SimpleDateFormat sdfAfter =  new SimpleDateFormat("yyyy'/'M'/'d");
			try {
				publishDateForDisplay = sdfAfter.format(sdfBefore.parse(publishDate));
			} catch (ParseException e) {
				e.printStackTrace();
			}			
		}
		
		return publishDateForDisplay;

	}
	
	public boolean getNewProperty(){
		SimpleDateFormat sdfBefore =  new SimpleDateFormat("yyyy'-'MM'-'dd'T'kk':'mm':'ss");
		SimpleDateFormat sdfDate =  new SimpleDateFormat("yyyy'/'M'/'d");
		
		//TODO そもそも提供開始日付が一定より前だったらnewはつけない
		//本来はリリース日のちょっとずらした時間にすべきだが、仮で現状の最新のアセットの上からみっつにNewが出せるようにする
		
		Date baseOldDate = SPPUtility.getDateCNTFormat("2015-09-17T01:00:00");
		Date basePublishDate = SPPUtility.getDateCNTFormat(publishDate);
		
		if(basePublishDate.getTime() - baseOldDate.getTime() < 0){
			return false;
		}
		
		try {
			
			String nowdateString = sdfDate.format(new Date());
			DebugLog.instance.outputLog("value", "現在日付" + nowdateString);
			String publishDateString = sdfDate.format(sdfBefore.parse(publishDate));
			DebugLog.instance.outputLog("value", "提供開始日付" + publishDateString);
			Date nowDate = sdfDate.parse(nowdateString);
			Date contentsDate = sdfDate.parse(publishDateString);
			
			
			if((nowDate.getTime() - contentsDate.getTime()) > 1000L * 60L * 60L * 24L * 30L){
//				DebugLog.instance.outputLog("value", "比較結果:" + (nowDate.getTime() - contentsDate.getTime()));
				return false;
			}else{
				return true;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	public MyPageDataDto getMyDto(){
		MyPageDataDto dto = new MyPageDataDto();
		
		dto.assetID = assetID;
		dto.contentsType = contentsType;
		dto.detailContentsType = detailContentsType;
		dto.themeTag = themeTag;
		
		dto.isExist = isExist;
		dto.hasDownloadHistory = hasDownloadHistory;
		dto.isFavorite = isFavorite;
		
		dto.addedDate = addedDate;
		dto.themeSetDate = themeSetDate;
		
		return dto;
	}

}
