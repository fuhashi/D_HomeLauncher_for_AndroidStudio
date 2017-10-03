package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;

import com.badlogic.gdx.utils.Array;

import java.io.File;
import java.util.Date;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadAllDataAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadDetailThumbSeparateAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadDetailThumbSeparateTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadIconThumbSeparateAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.GetDetailSkinInfoAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SetFavoriteAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.AddedDateComparatorDesc;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.PublishDateComparator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.RankingComparator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.iconpicker.IconSelectActivity;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

public class ContentsOperatorForCatalog {

    public static final ContentsOperatorForCatalog op = new ContentsOperatorForCatalog();
    private Context c;
    private static Array<ContentsDataDto> ctoArray;
    public static final String CHANGE_DB_STATE = "jp.co.disney.apps.managed.kisekaeapp.change_db_state";
    public static boolean isStartTimeout = false;
    public void startTimerForAllDataDownload(long time){
        if(downloadAllDataAsyncTask != null && !downloadAllDataAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "ContentsOperatorForCatalog_startTimer");
            downloadAllDataAsyncTask.startTimer(time);
        }
    }

    public ContentsOperatorForCatalog() {
        super();
    }

    public ContentsOperatorForCatalog(Context con) {
        super();
        this.c = con;
    }

    public ContentsOperatorForCatalog(Context con, Array<ContentsDataDto> array) {
        super();
        this.c = con;
        ctoArray = array;
    }

    public void SetContext(Context con) {
        this.c = con;
    }

    public void SetContentsArray(Array<ContentsDataDto> array) {
        ctoArray = array;

        reflectCtoArrayFromDB();
    }

    public void clearContentsData(){
        if(ctoArray != null){
            ctoArray.clear();
            ctoArray = null;
        }
    }

    public int getContentsArraySize(){
        if(ctoArray == null){
            return 0;
        }else{
            return ctoArray.size;
        }
    }

    public void setDataFromDB(){
        if(c != null && ctoArray != null && ctoArray.size > 0){
            MyPageDataAccess mAccess = new MyPageDataAccess(c);
            Array<MyPageDataDto> mArray = mAccess.findAllSkinRecord();

            for(ContentsDataDto cto : ctoArray){
                boolean isSetted = false;
                for(MyPageDataDto dto : mArray){
                    //DB内にレコードがあったら
                    if(cto.assetID == dto.assetID){
                        cto.setDataFromDB(dto);
                        isSetted = true;
                        break;
                    }
                }
                //DB内にレコードが無かったら
                if(!isSetted){
                    cto.isExist = cto.isFavorite = cto.hasDownloadHistory = false;
                }
            }
        }

    }

    public void sendDBStateChangeBroadcast(Context con){
        Intent intent = new Intent(ContentsOperatorForWidget.CHANGE_DB_STATE);
        con.sendBroadcast(intent);
    }

    public void reflectCtoArrayFromDB(){

        DebugLog.instance.outputLog("value", "reflectCtoArrayFromDB_PickerActivity");
        //カタログ以外（スキンピッカーなど）でのfavo、setなどでDBとの乖離がある可能性があるためDB内を整理してctoArrayに反映させる

        //DB内に従属スキンがあったら、isFavorite,hasDownloadHistory,をチェックする
        //DB内のテーマの最新のレコードより日付が新しいレコードのみが反映対象
        //従属スキンでfavoしかついてないものは反映用にだけ使うレコードなので、反映後は削除。
        MyPageDataAccess mAccess = new MyPageDataAccess(c);

        //DB内の従属スキンを取得
        Array<MyPageDataDto> inThemeArray = mAccess.findAllSkin_inTheme();
        if(inThemeArray == null || inThemeArray.size <= 0){
            setDataFromDB();
            return;
        }
        inThemeArray.sort(new AddedDateComparatorDesc());

        //上記Arrayの中から、テーマのレコードで一番新しいレコードより日付が新しいものが反映元対象になる
        //（カタログを初回起動する前にピッカーで何かしら操作していた場合はthemeArrayはnull
        Date newEstAddedDate = SPPUtility.getDateCNTFormat("2000-01-01T00:00:00");
        Array<MyPageDataDto> themeArray = mAccess.findAllSkin_Theme();
        if(themeArray != null && themeArray.size > 0){
            themeArray.sort(new AddedDateComparatorDesc());
            newEstAddedDate = SPPUtility.getDateCNTFormat(themeArray.get(0).addedDate);
            themeArray.clear();
            themeArray = null;
        }

        DebugLog.instance.outputLog("value", "該当判定基準日付:" + newEstAddedDate.toString());

        //反映元対象となる従属スキンを選別
        Array<MyPageDataDto> newRecordArray = new Array<MyPageDataDto>();
        for( MyPageDataDto dto : inThemeArray){
            Date inThemeDate = SPPUtility.getDateCNTFormat(dto.addedDate);
            if(inThemeDate.getTime() >= newEstAddedDate.getTime()){
                DebugLog.instance.outputLog("value", "newRecordArrayアセット:" + dto.assetID + "/addeDate:" + dto.addedDate);
               newRecordArray.add(dto);
            }else{
                break;
            }
        }


        //themeTagが同じctoを探して、isFavoriteに代入、isExistがtrueだったらhasDownloadHistoryに代入
        if(newRecordArray != null && newRecordArray.size > 0){
            for( MyPageDataDto dto : newRecordArray ){
                ContentsDataDto cto = getContentsDataFromThemeTag(dto.themeTag);

                if(cto != null){
                    cto.isFavorite = dto.isFavorite;
                    cto.addedDate = dto.addedDate;
                    if(dto.isExist) cto.hasDownloadHistory = true;
                    DebugLog.instance.outputLog("value", "反映させるアセット:" + cto.assetID + "/isFavorite:" + cto.isFavorite + "/hasDownloadHisotry:" + cto.hasDownloadHistory + "/addedDate:" + cto.addedDate);

                    if(mAccess.updateSkinIsMypage(cto, true) <= 0){
                        DebugLog.instance.outputLog("value", "挿入:" + cto.assetID + "/addedDate:" + cto.addedDate);
                           mAccess.insertSkinData(cto, false);
                    }
                }
            }
        }

        //従属スキンのレコードでisExistに使われていないものを削除
        for(MyPageDataDto dto : inThemeArray){
            if(!dto.isExist){
                mAccess.deleteById(dto.assetID);
            }
        }
        inThemeArray.clear();

        setDataFromDB();
    }

    public ContentsDataDto getContentsDataFromIndex(int index){
        if(ctoArray == null || ctoArray.size == 0 || index >= ctoArray.size){
            return null;
        }else{
            return ctoArray.get(index);
        }
    }

    public ContentsDataDto getContentsDataFromThemeTag(String themeTag){
        if(ctoArray == null || ctoArray.size < 1) return null;
        for(ContentsDataDto cto : ctoArray){
            if(themeTag.equals(cto.themeTag)){
                return cto;
            }else if(cto.themeTag.equals(themeTag)){
                return cto;
            }
        }
        return null;
    }

    public ContentsDataDto getContentsDataFromAssetId(String assetID){
        return getContentsDataFromAssetId(Long.valueOf(assetID));
    }

    public ContentsDataDto getContentsDataFromAssetId(long assetID){
        if(ctoArray == null || ctoArray.size < 1) return null;
        for(ContentsDataDto cto : ctoArray){
            if(assetID == cto.assetID){
                return cto;
            }
        }
        return null;
    }

    public Array<ContentsDataDto> getContentsDataArrayFromType(ContentsTypeValue type){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            if(cto.contentsType == type.getValue()) returnArray.add(cto);
        }

        if(returnArray.size == 0){
            return null;
        }else{
            return returnArray;
        }
    }

    /*
     * 新着画面；新着順、色・キャラ検索
     * おすすめ画面：おすすめ番号に合わせて配置、色・キャラ検索
     * ランキング画面：ランキング順、色・キャラ検索、テーマ/ウィジェット/アイコン分け（上限あり
     * マイページ画面：設定履歴とお気に入りに入れられたものを設定日付・お気に入り設定日付順にして並べる（上限あり
     */

    //新着（アセットID順にテーマと単独系を返す

    //ランキング取得APIが取得したアセットID順が渡されるので、そのアセットID順にして返す（最初の段階ではテーマのみでよい？

    //マイページ用DBから該当アセットIDを表示順にして返す

    //与えられたArrayから指定された色を持つアセットを抜き出し、与えられたArrayの順のまま返す

    //マイページ用DBからloadしたデータとCNTから取得したデータをかみ合わせてCto作成


    //TODO 全体的に、コンテンツのタイプ指定が必要になったりする可能性アリ

//	public Array<ContentsDataDto> getExistContents(){
//		Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();
//
//		for(ContentsDataDto cto : ctoArray){
//			if(cto.isExist) returnArray.add(cto);
//		}
//
//		if(returnArray.size == 0){
//			return null;
//		}else{
//			return returnArray;
//		}
//	}

    public Array<ContentsDataDto> getExistContents(ContentsTypeValue type){
        MyPageDataAccess mAccess = new MyPageDataAccess(c);
        Array<MyPageDataDto> mypageArrray = mAccess.findAllSkin_isExist(type.getValue());

        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;
        for(ContentsDataDto cto : ctoArray){
            for(int i = 0; i < mypageArrray.size; i++){
                MyPageDataDto dto = mypageArrray.get(i);
                if(dto.assetID == cto.assetID){
                    if(cto.contentsType == type.getValue()){
                        cto.isExist = dto.isExist;
                        cto.isFavorite = dto.isFavorite;
                        cto.hasDownloadHistory = dto.hasDownloadHistory;
                        cto.addedDate = dto.addedDate;
                        returnArray.add(cto);
                        i = mypageArrray.size;
                    }
                }
            }
        }

        if(returnArray.size == 0){
            return null;
        }else{
            return returnArray;
        }
    }

    public Array<ContentsDataDto> getMyPageContents(){
        MyPageDataAccess mAccess = new MyPageDataAccess(c);
        Array<MyPageDataDto> mypageArrray = mAccess.findAllSkin_ForMyPage();

        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            for(int i = 0; i < mypageArrray.size; i++){
                MyPageDataDto dto = mypageArrray.get(i);
                if(dto.assetID == cto.assetID){
                    cto.isExist = dto.isExist;
                    cto.isFavorite = dto.isFavorite;
                    cto.hasDownloadHistory = dto.hasDownloadHistory;
                    cto.addedDate = dto.addedDate;
                    returnArray.add(cto);
                    i = mypageArrray.size;
                }
            }
        }

        if(returnArray.size == 0){
            return null;
        }else{
            //追加日付でソート
            returnArray.sort(new AddedDateComparatorDesc());

            return returnArray;
        }
    }

    public Array<ContentsDataDto> getMyPageContents(ContentsTypeValue type){
        MyPageDataAccess mAccess = new MyPageDataAccess(c);
        Array<MyPageDataDto> mypageArrray = mAccess.findAllSkin_ForMyPage(type.getValue());

        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            for(int i = 0; i < mypageArrray.size; i++){
                MyPageDataDto dto = mypageArrray.get(i);
                if(dto.assetID == cto.assetID){
                    if(cto.contentsType == type.getValue()){
                        cto.isExist = dto.isExist;
                        cto.isFavorite = dto.isFavorite;
                        cto.hasDownloadHistory = dto.hasDownloadHistory;
                        cto.addedDate = dto.addedDate;
                        returnArray.add(cto);
                        i = mypageArrray.size;
                    }
                }

            }
        }

        if(returnArray.size == 0){
            return null;
        }else{
            //追加日付でソート
            returnArray.sort(new AddedDateComparatorDesc());
            return returnArray;
        }
    }

    public Array<ContentsDataDto> getNewArrivalContents(){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray) returnArray.add(cto);

        if(returnArray.size == 0){
            return null;
        }else{
            //公開日付でソート
            returnArray.sort(new PublishDateComparator());
            return returnArray;
        }
    }
    public Array<ContentsDataDto> getNewArrivalContents(ContentsTypeValue type){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            if(cto.contentsType == type.getValue()) returnArray.add(cto);
        }

        if(returnArray.size == 0){
            return null;
        }else{
            //公開日付でソート
            returnArray.sort(new PublishDateComparator());
            return returnArray;
        }
    }

    public Array<ContentsDataDto> getRankingContents(){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            returnArray.add(cto);

//			if(returnArray.size >= 10) break;
        }

        if(returnArray.size == 0){
            return null;
        }else{
            //DL数でソート、同じだったらアセットIDで比較
            returnArray.sort(new RankingComparator());

            Array<ContentsDataDto> return10Array = new Array<ContentsDataDto>();
            for(ContentsDataDto cto : returnArray){
                return10Array.add(cto);
                DebugLog.instance.outputLog("value", "sort___" + cto.assetID + "/rank" + cto.ranking);
                if(return10Array.size >= 10) break;
            }

            return return10Array;
        }

    }
    public Array<ContentsDataDto> getRankingContents(ContentsTypeValue type){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            if(cto.contentsType == type.getValue()) returnArray.add(cto);
        }

        if(returnArray.size == 0){
            return null;
        }else{
            //DL数でソート、同じだったらアセットIDで比較
            returnArray.sort(new RankingComparator());

            Array<ContentsDataDto> return10Array = new Array<ContentsDataDto>();
            for(ContentsDataDto cto : returnArray){
                return10Array.add(cto);
                DebugLog.instance.outputLog("value", "sort___" + cto.assetID + "/rank" + cto.ranking);
                if(return10Array.size >= 10) break;
            }

            return return10Array;
        }
    }


    //おすすめONになっているものを、おすすめ順にしてテーマと単独系を返す
    public Array<ContentsDataDto> getPickUpContents(){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : ctoArray){
            if(cto.pickup) returnArray.add(cto);
        }

        if(returnArray.size == 0){
            return null;
        }else{
            //おすすめ順にソート
            returnArray.sort(new PickUpNumberComparator());
            return returnArray;
        }
    }

    public Array<ContentsDataDto> getNotPickUpContents(){
    	Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

    	if(ctoArray == null || ctoArray.size < 1) return null;

    	for(ContentsDataDto cto : ctoArray){
    		if(!cto.pickup) returnArray.add(cto);
    	}

    	if(returnArray.size == 0){
    		return null;
    	}else{
    		//新着順でソート
    		returnArray.sort(new PublishDateComparator());
        	return returnArray;
    	}

    }


    class PickUpNumberComparator implements java.util.Comparator<ContentsDataDto> {

        public PickUpNumberComparator() {	}

        @Override
        public int compare(ContentsDataDto lhs, ContentsDataDto rhs) {

            if(lhs.pickupNumber > rhs.pickupNumber){
                return 1;
            } else if(lhs.pickupNumber == rhs.pickupNumber) {
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


    //与えられたArrayから指定されたキャラを持つアセットを抜き出し、与えられたArrayの順のまま返す
    public Array<ContentsDataDto> getCharaContents( ContentsCharaValue chara){
        return getCharaContents(ctoArray, chara);
    }

    public Array<ContentsDataDto> getCharaContents(Array<ContentsDataDto> motoArray, ContentsCharaValue chara){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size < 1) return null;

        for(ContentsDataDto cto : motoArray){
            for(ContentsCharaValue c : cto.chara){
                if(c == chara) returnArray.add(cto);
            }
        }

        if(returnArray.size == 0){
            return null;
        }else{
            returnArray.sort(new PublishDateComparator());
            return returnArray;
        }
    }


    private GetDetailSkinInfoAsyncTask getDetailSkinInfoAsyncTask = null;
    private DownloadAllDataAsyncTask downloadAllDataAsyncTask = null;;
    private DownloadSkinAsyncTask downloadSkinAsyncTask = null;
    private DownloadIconThumbSeparateAsyncTask downloadIconThumbAsyncTask = null;
    private SetFavoriteAsyncTask setFavoriteAsyncTask = null;
    private Array<DownloadDetailThumbSeparateAsyncTask> downloadDetailThumbSeparateAsyncTask = new Array<DownloadDetailThumbSeparateAsyncTask>();;

    public void cancelTask(){
        DebugLog.instance.outputLog("value", "_______________cancel_Task!_______________");
        if(getDetailSkinInfoAsyncTask != null && !getDetailSkinInfoAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "GetDetailSkinInfoAsyncTaskのキャンセル");
            getDetailSkinInfoAsyncTask.cancel(true);
        }

        if(downloadAllDataAsyncTask != null && !downloadAllDataAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "downloadAllDataAsyncTaskのキャンセル");
            downloadAllDataAsyncTask.cancel(true);
        }

        if(downloadSkinAsyncTask != null && !downloadSkinAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "downloadSkinAsyncTaskのキャンセル");
            downloadSkinAsyncTask.cancel(true);
        }

        if(downloadIconThumbAsyncTask != null && !downloadIconThumbAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "downloadIconThumbAsyncTaskのキャンセル");
            downloadIconThumbAsyncTask.cancel(true);
        }

        if(setFavoriteAsyncTask != null && !setFavoriteAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "setFavoriteAsyncTaskのキャンセル");
            setFavoriteAsyncTask.cancel(true);
        }

        if(downloadDetailThumbSeparateAsyncTask != null && downloadDetailThumbSeparateAsyncTask.size > 0){
            DebugLog.instance.outputLog("value", "downloadDetailThumbSeparateAsyncTaskのキャンセル");
            for(DownloadDetailThumbSeparateAsyncTask task : downloadDetailThumbSeparateAsyncTask){
                if(task != null && !task.isCancelled()){
                    task.cancel(true);
                }
            }
        }

    }

    private ContentsDataDto nowDownloadTargetCto = null;
    public boolean callDownloadSkinTask(final ContentsDataDto cto){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();

                if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
                    downloadIconThumbAsyncTask = new DownloadIconThumbSeparateAsyncTask(c);
                    downloadIconThumbAsyncTask.execute(cto);

                }else{

                	nowDownloadTargetCto = cto;
                	DebugLog.instance.outputLog("value", "callDownloadSkinTask!_" + nowDownloadTargetCto.assetID);

                	boolean isStopDL = false;
                	String carrierId = SPPUtility.getCarrierID(c);
                	if(carrierId.equals(SplashActivity.AUTH_CARRIER_OND)
                			|| carrierId.equals(SplashActivity.AUTH_CARRIER_ONS)
                			|| carrierId.equals(SplashActivity.AUTH_CARRIER_AU)
                			|| carrierId.equals(SplashActivity.AUTH_CARRIER_CONPAS)){
                		//GetAuthInfoが必要
                       	if(SPPUtility.isNeedToken(c)){
                       		DebugLog.instance.outputLog("value", "tokenが必要！");
                       		isStopDL = true;
                       		Intent intent = new Intent(c.getPackageName() + SplashActivity.START_GETAUTHINFO_FROM_SPLASH);
                       		c.sendBroadcast(intent);
                    	}

                	}

                	if(!isStopDL){
                        //スキンのダウンロード開始
                        downloadSkinAsyncTask = new DownloadSkinAsyncTask(c);
//        				downloadSkinAsyncTask.execute(String.valueOf(cto.assetID), String.valueOf(cto.contentsType));
                        downloadSkinAsyncTask.execute(nowDownloadTargetCto);

                        nowDownloadTargetCto = null;
                	}
                }

            }
        };
        new Thread(r).start();

        return true;
    }

    public boolean restartDownloadSkinTask(){
    	DebugLog.instance.outputLog("value", "restartDownloadSkinTask!_" + nowDownloadTargetCto.assetID);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();


            	if(nowDownloadTargetCto != null){
            		//スキンのダウンロード開始
            		downloadSkinAsyncTask = new DownloadSkinAsyncTask(c);
//    				downloadSkinAsyncTask.execute(String.valueOf(cto.assetID), String.valueOf(cto.contentsType));
            		downloadSkinAsyncTask.execute(nowDownloadTargetCto);

            		nowDownloadTargetCto = null;
            	}
            }
        };
        new Thread(r).start();

        return true;

    }

    public static final String INTENT_EXTRA_ISFROMCATALOG = "is.FromCatalog";
    public void callIconSelectListTypeActivity(ContentsDataDto cto, boolean isFromCatalog){
    	//リスト表示のアイコン呼び出し元は通常のカタログ遷移と、アイコンきせかえアプリからの２パタン。
        DebugLog.instance.outputLog("value", "リスト型のアイコンピッカーActivity起動");
        //IconSelectListTypeActivity

        //ctoの親アセットのお気に入り有無を反映
        ContentsDataDto parentCto = getContentsDataFromThemeTag(cto.themeTag);
        if(parentCto != null) cto.isFavorite = parentCto.isFavorite;

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID, String.valueOf(cto.assetID));
        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, cto.contentsType);
//      intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_DETAIL_CONTENTS_TYPE, cto.detailContentsType);

        intent.putExtra(INTENT_EXTRA_ISFROMCATALOG, isFromCatalog);
        intent.putExtra(IconSelectActivity.KISEKAE_SAKI_SHORTCUTICON_DTO, cto.getMyDto());

        intent.setClassName(c.getPackageName(), c.getPackageName() + ".iconpicker.IconSelectListTypeActivity");
//        intent.addCategory(Intent.CATEGORY_HOME);
//        intent.setPackage(c.getPackageName());

        c.startActivity(intent);

    }

    public void callChangeFavoriteTask(final ContentsDataDto cto){
        //テーマ、単独コンテンツでお気に入り切り替え可能
        if(cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_THEME.getValue() &&
                cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WP.getValue() &&
                cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() &&
                cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue() ){
            return;
        }else{
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    setFavoriteAsyncTask = new SetFavoriteAsyncTask(c);
                    setFavoriteAsyncTask.execute(cto);
                }
            };
            new Thread(r).start();

        }
    }

    public void callDownloadAllDataTask(final ContentsDetailTypeValue type){
        int isP = 0;
//		if(isPremium) isP = 1;
//		final int p = isP;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                downloadAllDataAsyncTask = new DownloadAllDataAsyncTask(c);
                downloadAllDataAsyncTask.execute(type.getValue());
            }
        };
        new Thread(r).start();

    }

    public void callDetailInfoGetTask(final ContentsDataDto cto){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                //ctoからアセットIDを取得して情報を取りに行く
                getDetailSkinInfoAsyncTask = new GetDetailSkinInfoAsyncTask(c);
                getDetailSkinInfoAsyncTask.execute(String.valueOf(cto.assetID), String.valueOf(cto.contentsType), cto.themeTag);
            }
        };
        new Thread(r).start();

    }

    public void stopDetailDownload(){
        if(downloadDetailThumbSeparateAsyncTask != null && downloadDetailThumbSeparateAsyncTask.size > 0){
            DebugLog.instance.outputLog("value", "downloadDetailThumbSeparateAsyncTaskのキャンセル");
            for(DownloadDetailThumbSeparateAsyncTask task : downloadDetailThumbSeparateAsyncTask){
                if(task != null && !task.isCancelled()){
                    task.cancel(true);
                }
            }
        }

    }

    public void callDownloadDetailThumbsTask(Array<ThumbInfo> array){
        for(ThumbInfo info : array){
            if(info.isExistThumbs(c)){
                //テクスチャ作成に入れるcallbackを呼ぶ
                DownloadDetailThumbSeparateTaskCallback callback = (DownloadDetailThumbSeparateTaskCallback) c;
                callback.onFinishedDownloadDetailThumbsSeparate(info);
            }else{
                final ThumbInfo i = info;
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        Looper.prepare();
                        //ダウンロードタスク呼び出し
                        DownloadDetailThumbSeparateAsyncTask newTask = new DownloadDetailThumbSeparateAsyncTask(c);
                        downloadDetailThumbSeparateAsyncTask.add(newTask);;
//                        downloadDetailThumbSeparateAsyncTask.get(downloadDetailThumbSeparateAsyncTask.size - 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, i);
                        newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, i);
                    }
                };
                new Thread(r).start();

            }
        }

    }

    public void callDownloadDetailThumbsTask(ThumbInfo info){
        if(info.isExistThumbs(c)){
            //テクスチャ作成に入れるcallbackを呼ぶ
            DownloadDetailThumbSeparateTaskCallback callback = (DownloadDetailThumbSeparateTaskCallback) c;
            callback.onFinishedDownloadDetailThumbsSeparate(info);
        }else{
            //ダウンロードタスク呼び出し
            DownloadDetailThumbSeparateAsyncTask newTask = new DownloadDetailThumbSeparateAsyncTask(c);
            downloadDetailThumbSeparateAsyncTask.add(newTask);
//            downloadDetailThumbSeparateAsyncTask.get(downloadDetailThumbSeparateAsyncTask.size - 1).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
            newTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, info);
        }
    }

    public void callSkinChangeHomeApp(ContentsDataDto cto){

        //スキンの種類によって行なう動作が違う
//		ContentsDataDto cto = getContentsDataFromAssetId(assetId);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID, String.valueOf(cto.assetID));
        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, cto.contentsType);

        if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){

        }else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
                || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
                || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue() ){
            //TODO 一括設定か個別設定かが必要（一括・個別分けに関しては未実装　201508（アイコンごとの個別設定が未実装なのでまだ不要

        }else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() ||
                cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue() ){

        }else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() ||
                cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue() ){

            intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_DETAIL_CONTENTS_TYPE, cto.detailContentsType);

        }

        DebugLog.instance.outputLog("value", "homeのActivity起動");
//        intent.setClassName(c.getPackageName(), c.getPackageName() + ".launcher.Launcher");
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setPackage(c.getPackageName());

        c.startActivity(intent);

    }

    public void deleteDetailThumbsFolder(){
        FileUtility.delFile(new File(FileUtility.getDetailThumbnailsRootPath(c)));
    }

}
