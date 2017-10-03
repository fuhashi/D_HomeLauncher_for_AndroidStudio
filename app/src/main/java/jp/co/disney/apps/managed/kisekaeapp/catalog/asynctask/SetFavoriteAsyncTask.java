package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import java.util.Timer;
import java.util.TimerTask;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForCatalog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.AddedDateComparatorAsc;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.DataBaseParam;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import android.content.Context;
import android.os.AsyncTask;

import com.badlogic.gdx.utils.Array;
//import android.util.Log;


/**
 * アセットのお気に入り状態を切り替えるtask
 * @author sagara
 *
 */
public class SetFavoriteAsyncTask extends
		AsyncTask<ContentsDataDto, Integer, Boolean>{
//		AsyncTask<String, Integer, Boolean> implements OnCancelListener{
	
//	private ProgressDialog dialog = null;
	Context myContext;
	long startTime = 0;
	private SetFavoriteTaskCallback callback;
	private String assetId = "";
	private int contentsType = 0;
	private ContentsDataDto cto = null;

	Timer   mTimer   = null;

	public SetFavoriteAsyncTask(Context context) {
		super();
		myContext = context;
		callback = (SetFavoriteTaskCallback) context;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();

//		dialog = new ProgressDialog(myContext);
//		// タイトル, 本文を設定
//		dialog.setTitle("Downloading...");
//		// スタイルを設定
//		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//		dialog.setCancelable(true);
//		dialog.setOnCancelListener(this);
//		dialog.setCanceledOnTouchOutside(false);
//		dialog.setMax(100);
//		dialog.setProgress(0);
//		dialog.show();
		
		//開始時刻を取得（タイムアウト設定しないならこれはいらないが一応残しておく。 20150501
		startTime = System.currentTimeMillis();
		
		mTimer = new java.util.Timer(true);
		mTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				if((System.currentTimeMillis() - startTime) >= 10000){
					DebugLog.instance.outputLog("value", "timeout!!!!!");
					mTimer.cancel();
					mTimer = null;
					callCancel();
				}
				DebugLog.instance.outputLog("value", "time:" + (System.currentTimeMillis() - startTime));
			}
			
		}, 100, 100);
	}
	
	private void callCancel(){
		cancel(true);
	}

//	private boolean flagDownload = false;
	/**
	 * arg0[0]:assetID
	 * arg0[1]:コンテンツ種別
	 * return t（成功）/f（失敗）
	 */
	@Override
//	protected Boolean doInBackground(String... arg0) {
	protected Boolean doInBackground(ContentsDataDto... arg0) {
		DebugLog.instance.outputLog("value", "お気に入り切り替えtask");
		
		//パラメータ指定が足りなかったらcancel
		if(arg0.length < 1) return false;
		
		//アセットIDとコンテンツ種別（一括テーマ、壁紙、ウィジェット、アイコン）を取得
		cto = arg0[0];
		assetId = String.valueOf(cto.assetID);
		contentsType = Integer.valueOf(cto.contentsType);
		DebugLog.instance.outputLog("value", "assetId:" + assetId + "/type:" + contentsType);

		MyPageDataAccess mAccess = new MyPageDataAccess(myContext);
		
		if(isCancelled()){
//			DebugLog.instance.outputLog("value", "Cancelled!");
			return false;
		}
		
		//レコードが存在したらisFavoriteのtrue/falseを取得して対応、存在してなかったらisFavoriteをtrueの状態で挿入
		//従属スキンは基本ctoがDBと連動しているのでDB内の参照は行なわない。
		if(contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
				|| contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
				|| contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
				|| contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
			cto.isFavorite = cto.isFavorite ? false : true;
			cto.addedDate = "";
		}else{
			cto.isFavorite = true;			
		}

		Array<MyPageDataDto> recordArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), assetId);
		if(recordArray.size >= 1){
			MyPageDataDto dto = recordArray.get(0);
			
			if(contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue()){
				//元々がfavoriteがついてたら外す
				if(dto.isFavorite){
					cto.isFavorite = false;
					if(!dto.isExist && !dto.hasDownloadHistory){
						//実体ファイルもダウンロード履歴もなかったらレコードを削除
						cto.addedDate = "";
						mAccess.deleteById(cto.assetID);
						return true;
					}
				}				
			}

			DebugLog.instance.outputLog("value", "ふぁぼ変更：" + cto.isFavorite);
			//更新
//			if(mAccess.updateSkinIsExist(createMyPageDataDtoFromCto(cto), true) > 0){
			if(mAccess.updateSkinIsFavorite(cto, true) > 0){
				DebugLog.instance.outputLog("value", "お気に入り保存レコード更新成功");
				checkMaxMyPage(contentsType);
				return true;
			}else{
				DebugLog.instance.outputLog("value", "お気に入り保存レコード更新失敗");
				return false;
			}
		}else{
			//挿入
			if(mAccess.insertSkinData(cto, true) > 0){
				DebugLog.instance.outputLog("value", "お気に入り保存レコード挿入成功");
				checkMaxMyPage(contentsType);
				return true;
			}else{
				DebugLog.instance.outputLog("value", "お気に入り保存レコード挿入失敗");
				return false;
			}
		}

	}

	@Override
	protected void onProgressUpdate(Integer... values) {
//		dialog.setProgress(values[0]);
		
		//super.onProgressUpdate(values);
	}

	@Override
	protected void onCancelled() {
//		diaDebugLog.instance.outputLogismiss();
//		DebugLog.instance.outputLog("download", "onCancelled");
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		callback.onFailedSetFavorite();
		super.onCancelled();
	}

	@Override
	protected void onPostExecute(Boolean result) {
//		diaDebugLog.instance.outputLogismiss();
		DebugLog.instance.outputLog("value", "SetFavoriteAsyncTask:::onPostExecute:::result:" + result);
		
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
		//super.onPostExecute(result);
		
		if(!result){
			//favo状態変更失敗
//			DebugLog.instance.outputLog("download", "onPostExecute:resultFalse");
			callback.onFailedSetFavorite();
//			callCancel();
			return;
		}

		DebugLog.instance.outputLog("value", "favorite success!");
		ContentsOperatorForCatalog.op.setDataFromDB();
		ContentsOperatorForWidget.op.setDataFromDB();
		
		long assetId = 0l, oldAssetId = 0l;
		if(cto != null) assetId = cto.assetID;
		if(oldCtoForMypage != null) oldAssetId = oldCtoForMypage.assetID;
		callback.onFinishedSetFavorite(assetId, oldAssetId);
	}
	
//	private MyPageDataDto createMyPageDataDtoFromCto(ContentsDataDto cto){
//		MyPageDataDto dto = new MyPageDataDto();
//		/*
//this.assetID = 0;
//this.contentsType = 0;
//this.themeTag = "";
//this.isExist = false;
//this.isFavorite = false;
//this.addedDate = "";
//		 */
//		dto.assetID = cto.assetID;
//		dto.contentsType = cto.contentsType;
//		dto.themeTag = cto.themeTag;
//		dto.isExist = cto.isExist;
//		dto.isFavorite = cto.isFavorite;
//		dto.addedDate = cto.addedDate;
//		
//		return dto;
//	}
	
	private ContentsDataDto oldCtoForMypage = null;
	private void checkMaxMyPage(int type){
		//このメソッドは追加後に呼ばれるので、この時点で上限を超えていたら必ず１つマイページから消す
		
		//もしテーマ従属スキンがfavoに追加されていた場合、実際に追加されるのはthemetagが同一のテーマスキンである。
		//ので、themetagが既にレコードにあった場合順序入れ替えのみとなる。
		//themetagがある→チェック不要
		//themetagがない→１レコード増える予定、なので、１つ消しておく

		MyPageDataAccess mAccess = new MyPageDataAccess(myContext);
		Array<MyPageDataDto> mypageArrray = mAccess.findAllSkin_ForMyPage();
		
		Array<MyPageDataDto> inThemeArray = null;
		if(contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
				|| contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
				|| contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()				
				|| contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
			inThemeArray = mAccess.findAllSkin_inThemeIntoMypage();
			
			if(inThemeArray != null && inThemeArray.size > 0){
				for(MyPageDataDto dto : inThemeArray){
					if(dto.isFavorite || dto.isExist){
						mypageArrray.add(dto);
						DebugLog.instance.outputLog("value", "DB内レコード_従属スキン:" + cto.assetID);
					}
				}
			}
		}
		
		//新規スキンをマイページに追加することで既存のマイページコンテンツが１つ押し出される→押し出されたctoのisFavorite・hasDownloadHistoryはfalseに（isExistがtrueでない限りレコードから削除
		if(mypageArrray != null && mypageArrray.size > DownloadSkinAsyncTask.MYPAGE_CONTENTS_MAX){

			mypageArrray.sort(new AddedDateComparatorAsc());
			
			//isExistがtrueでもマイページからは消す、がその場合レコードは残す。日付は更新しない。
			if(contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
				//TODO ここのクラス（ContentsOperatorForWidget）は後々きちんと場合分け
				oldCtoForMypage = ContentsOperatorForWidget.op.getContentsDataFromAssetId(mypageArrray.get(0).assetID);

			}else{
				oldCtoForMypage = ContentsOperatorForCatalog.op.getContentsDataFromAssetId(mypageArrray.get(0).assetID);

			}
			//上記でctoが取れなかったら起動ピッカーで扱わないスキンが削除対象なのでDB内のレコード操作のみ行なう（ctoへの反映はカタログ側が別途行なう。
			if(oldCtoForMypage == null) oldCtoForMypage = new ContentsDataDto(mypageArrray.get(0));
			
			oldCtoForMypage.setDataFromDB(mypageArrray.get(0));
			
			DebugLog.instance.outputLog("value", "削除するアセット:" + oldCtoForMypage.assetID + "/isFavorite:" + oldCtoForMypage.isFavorite + "/hasDownloadHisotry:" + oldCtoForMypage.hasDownloadHistory);
			
			oldCtoForMypage.isFavorite = false;
			oldCtoForMypage.hasDownloadHistory = false;
			oldCtoForMypage.addedDate = "";
			if(oldCtoForMypage.isExist){
				mAccess.updateSkinIsMypage(oldCtoForMypage, false);
			}else{
				//isExistがtrueじゃなかったらレコードごと消す。
				mAccess.deleteById(oldCtoForMypage.assetID);
			}
			
			//このアセットが単独テーマのものだったら、従属スキンでも消す必要があるものがあるかもなので、それもチェック
			if(contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()
					|| contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue()){
				if(oldCtoForMypage.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
					//TODO ここのクラス（ContentsOperatorForWidget）は後々きちんと場合分け
					ContentsDataDto childCto = ContentsOperatorForWidget.op.getContentsDataFromThemeTag(oldCtoForMypage.themeTag);
					if(childCto != null){
						childCto.isFavorite = false;
						childCto.hasDownloadHistory = false;
						childCto.addedDate = "";
						if(mAccess.updateSkinIsMypage(childCto, true) <= 0){
							mAccess.insertSkinData(childCto, true);
						}
					}
				}
			}
				

		}
	}

}
