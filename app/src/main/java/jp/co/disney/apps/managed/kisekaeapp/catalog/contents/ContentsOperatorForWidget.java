package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import java.io.File;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadAllDataAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SetFavoriteAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.AddedDateComparatorDesc;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.PublishDateComparator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BadgeDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.BadgeDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.DataBaseParam;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WidgetPickerActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import com.badlogic.gdx.utils.Array;

public class ContentsOperatorForWidget {

    public static final ContentsOperatorForWidget op = new ContentsOperatorForWidget();
    private static Context c;
    private static Array<ContentsDataDto> ctoArray = null;
    public static final String CHANGE_DB_STATE = "jp.co.disney.apps.managed.kisekaeapp.change_db_state_from_skinpicker";

    public ContentsOperatorForWidget() {
        super();
    }

    public ContentsOperatorForWidget(Context con) {
        super();
        this.c = con;
    }

    public ContentsOperatorForWidget(Context con, Array<ContentsDataDto> array) {
        super();
        this.c = con;
        ctoArray = array;
    }

    public void SetContext(Context con) {
        this.c = con;
    }

    public void SetContentsArray(Array<ContentsDataDto> array) {
        ctoArray = array;
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

    public void reflectCtoArrayFromDB(){
        if(ctoArray == null) return;
        DebugLog.instance.outputLog("value", "reflectCtoArrayFromDB_w");
        //カタログ側で変更があった場合にこっちにも反映させる（基本favoが取れればいい。テーマのisExist,hasDLhistoryは関係ない。ウィジェットスキンisExist=trueレコードはDBに残っている。
        //DB内のテーマスキン（親アセット）のレコードを全取得し、isfavriteのみを同様のテーマタグを持つctoに反映させる。テーマの実体ファイルの有無はスキンピッカーには関係ないのでisFavoriteのみ。

        //テーマ（親アセットのレコードを取得
        MyPageDataAccess mAccess = new MyPageDataAccess(c);
        Array<MyPageDataDto> allThemeArray = mAccess.findAllSkin_Theme();

        //上記Arrayの中から現状のctoArray（子アセットの情報を持つArray）とテーマタグが一致するものだけ取得して、isFavoriteを反映
        //allThemeArrayにレコードがないものはisFavoriteがfalse
        if(allThemeArray != null || allThemeArray.size > 0){
            if(ctoArray.size > 0){
                DebugLog.instance.outputLog("value", "ctoArray is not null");
                for(ContentsDataDto cto : ctoArray){
                    boolean isSetFavo = false;
                    for(MyPageDataDto dto : allThemeArray){
                        if(cto.themeTag.equals(dto.themeTag)){
                            cto.isFavorite = dto.isFavorite;
                            DebugLog.instance.outputLog("value", "cto_themeTag:" + cto.themeTag + "/cto_isFavorite:" + cto.isFavorite);
                            isSetFavo = true;
                            break;
                        }
                    }
                    if(!isSetFavo){
                        cto.isFavorite = false;
                        DebugLog.instance.outputLog("value", "cto_themeTag:" + cto.themeTag + "/cto_isFavorite:" + cto.isFavorite);
                    }

                    //isExistがtrueのctoはDB内にレコードがある筈なのでそっちも更新（※isExistがfalseのctoはDBに挿入する必要なし。ctoに値が繁栄されているだけでよい
                    if(cto.isExist){
                        //カタログ側のreflectでisExistがtrueのレコード以外は削除されているはず
                        mAccess.updateSkinIsFavorite(cto, false);
                    }

                }
            }
        }

        //単独スキンの情報も要反映
        Array<MyPageDataDto> indWidgetSkinArray = mAccess.findAllSkin_Independent(ContentsTypeValue.CONTENTS_TYPE_WIDGET);
        if(indWidgetSkinArray != null || indWidgetSkinArray.size > 0){
            if(ctoArray != null && ctoArray.size > 0){
                for(ContentsDataDto cto : ctoArray){
                    boolean isSetFavo = false;
                    for(MyPageDataDto dto : indWidgetSkinArray){
                        if(cto.assetID == dto.assetID){
                            cto.isFavorite = dto.isFavorite;
                            isSetFavo = true;
                            break;
                        }
                    }
                    if(!isSetFavo){
                        cto.isFavorite = false;
                    }

                    //isExistがtrueのctoはDB内にレコードがある筈なのでそっちも更新（※isExistがfalseのctoはDBに挿入する必要なし。ctoに値が繁栄されているだけでよい
                    if(cto.isExist){
                        //カタログ側のreflectでisExistがtrueのレコード以外は削除されているはず
                        mAccess.updateSkinIsFavorite(cto, false);
                    }
                }
            }
        }

    }

    public ContentsDataDto getContentsDataFromThemeTag(String themeTag){
        if(ctoArray == null || ctoArray.size <= 0) return null;

        for(ContentsDataDto cto : ctoArray){
            if(themeTag.equals(cto.themeTag)){
                return cto;
//			}else if(cto.themeTag.equals(themeTag)){
//				return cto;
            }
        }


        return null;
    }


    public void sendDBStateChangeBroadcast(Context con){
        Intent intent = new Intent(ContentsOperatorForCatalog.CHANGE_DB_STATE);
        con.sendBroadcast(intent);
    }

    public void setDataFromDB(){
        if(c != null && ctoArray != null && ctoArray.size > 0){
            MyPageDataAccess mAccess = new MyPageDataAccess(c);
            Array<MyPageDataDto> mArray = mAccess.findAllSkinRecord();

            for(ContentsDataDto cto : ctoArray){
                for(MyPageDataDto dto : mArray){
                    if(cto.assetID == dto.assetID){
                        cto.setDataFromDB(dto);
                        break;
                    }
                }
            }
        }

    }

    public ContentsDataDto getContentsDataFromIndex(int index){
        if(ctoArray == null || ctoArray.size == 0 || index >= ctoArray.size){
            return null;
        }else{
            return ctoArray.get(index);
        }
    }

    public ContentsDataDto getContentsDataFromAssetId(String assetID){
        return getContentsDataFromAssetId(Long.valueOf(assetID));
    }

    public ContentsDataDto getContentsDataFromAssetId(long assetID){
        if(ctoArray == null || ctoArray.size <= 0) return null;

        for(ContentsDataDto cto : ctoArray){
            if(assetID == cto.assetID){
                return cto;
            }
        }
        return null;
    }

    public Array<ContentsDataDto> getContentsDataArrayFromType(ContentsTypeValue type){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size <= 0) return null;
        for(ContentsDataDto cto : ctoArray){
            if(cto.contentsType == type.getValue()) returnArray.add(cto);
        }

        if(returnArray.size == 0){
            return null;
        }else{
            return returnArray;
        }
    }


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
//
//	public Array<ContentsDataDto> getExistContents(ContentsTypeValue type){
//		Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();
//
//		for(ContentsDataDto cto : ctoArray){
//			if(cto.isExist && cto.contentsType == type.getValue()) returnArray.add(cto);
//		}
//
//		if(returnArray.size == 0){
//			return null;
//		}else{
//			return returnArray;
//		}
//	}

    public Array<ContentsDataDto> getNewArrivalContents(){
        Array<ContentsDataDto> returnArray = new Array<ContentsDataDto>();

        if(ctoArray == null || ctoArray.size <= 0) return null;
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

        if(ctoArray == null || ctoArray.size <= 0) return null;
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

    public Long getNewestContents(Context con, ContentsDetailTypeValue detailType){
        if(con == null) return 0L;

        //テーマも含めて最新取得の検索対象にすべき。テーマと指定されたdetailのスキン両方の中の最新。
        //テーマが該当ウィジェットを持っているか否かは…DBに入れる？
        //フォルダ精査？してテーマ内の該当ウィジェット画像のあるなしをチェックしてテーマのアセットIDを取得、ソート対象はDBから取得する
        String checkFileName = "";
        if(detailType == ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY){
            checkFileName = ContentsFileName.wdtBatteryBg1.getFileName();
        }//TODO ウィジェットが増えたら要対応

        //走査
        Array<String> themeArray = FileUtility.getAssetIDFromThemeForFileName(con, checkFileName);

        MyPageDataAccess mAccess = new MyPageDataAccess(con.getApplicationContext());
        Array<MyPageDataDto> mArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_DETAIL_CONTENTS_TYPE.getParam(), String.valueOf(detailType.getValue()));

        for(String id : themeArray){
            Array<MyPageDataDto> array = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), id);
            if(array != null && array.size > 0){
                mArray.add(array.get(0));
            }
        }

        if(mArray != null && mArray.size > 0){
            mArray.sort(new AddedDateComparatorDesc());
            return mArray.get(0).assetID;
        }else{
            return 0L;
        }
    }

    public String getContentsImagePath(Context con, long assetId, ContentsFileName fileName){
        String path = "";

        //assetIDからどのタイプであるかを判断
        MyPageDataAccess mAccess = new MyPageDataAccess(con.getApplicationContext());
        Array<MyPageDataDto> array = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(assetId));
        if(array != null && array.size > 0){
            path = FileUtility.getSkinPath(con, true, ContentsTypeValue.getEnum(array.get(0).contentsType));
            if(array.get(0).contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
                //historyの可能性有り
                String currentPath = FileUtility.getSkinPath(con, true, ContentsTypeValue.CONTENTS_TYPE_THEME) + assetId + File.separator;
                if(!new File(currentPath).exists()){
                    path = FileUtility.getSkinPath(con, false, ContentsTypeValue.getEnum(array.get(0).contentsType));
                }
            }
            path = path + assetId + File.separator + fileName.getFileName();
        }

        return path;
    }

    public String getContentsThumbsPath(long assetId){
        String path = "";

        path = FileUtility.getThumbnailsCachePath(c) + assetId + ".etc1";
        return path;
    }


    //---------------------------------AsyncTask関連


    private DownloadAllDataAsyncTask downloadAllDataAsyncTask = null;;
    private DownloadSkinAsyncTask downloadSkinAsyncTask = null;
    private SetFavoriteAsyncTask setFavoriteAsyncTask = null;

    public void cancelTask(){
        DebugLog.instance.outputLog("value", "_______________cancel_Task!_______________");

        if(downloadAllDataAsyncTask != null && !downloadAllDataAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "downloadAllDataAsyncTaskのキャンセル");
            downloadAllDataAsyncTask.cancel(true);
        }

        if(downloadSkinAsyncTask != null && !downloadSkinAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "downloadSkinAsyncTaskのキャンセル");
            downloadSkinAsyncTask.cancel(true);
        }

        if(setFavoriteAsyncTask != null && !setFavoriteAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "setFavoriteAsyncTaskのキャンセル");
            setFavoriteAsyncTask.cancel(true);
        }

    }

    private ContentsDataDto nowDownloadTargetCto = null;
    public boolean callDownloadSkinTask(final ContentsDataDto cto){

        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                
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
                   		Intent intent = new Intent(c.getPackageName() + WidgetPickerActivity.START_GETAUTHINFO_FROM_PICKER);
                   		c.sendBroadcast(intent);
                	}
            	}

            	if(!isStopDL){
                    //currentに存在してなかったらスキンのダウンロード開始
                    downloadSkinAsyncTask = new DownloadSkinAsyncTask(c);
                    downloadSkinAsyncTask.execute(cto);
//    				downloadSkinAsyncTask.execute(String.valueOf(cto.assetID), String.valueOf(cto.contentsType));
            		
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
            		downloadSkinAsyncTask.execute(nowDownloadTargetCto);

            		nowDownloadTargetCto = null;
            	}
            }
        };
        new Thread(r).start();

        return true;

    }


    public void callChangeFavoriteTask(final ContentsDataDto cto){
        DebugLog.instance.outputLog("value", "task呼び出し");
        //テーマ、単独コンテンツでお気に入り切り替え可能
//		if(cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_THEME.getValue() &&
//				cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WP.getValue() &&
//				cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() &&
//				cto.contentsType != ContentsTypeValue.CONTENTS_TYPE_ICON.getValue() ){
//			return;
//		}else{
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Looper.prepare();
                    setFavoriteAsyncTask = new SetFavoriteAsyncTask(c);
                    setFavoriteAsyncTask.execute(cto);
                }
            };
            new Thread(r).start();

//		}
    }

    public void callDownloadAllDataTask(boolean isPremium, final ContentsDetailTypeValue detail){
        int isP = 0;
        if(isPremium) isP = 1;
        final int p = isP;

        //TODO 此処でウィジェットの種類を指定したい
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                downloadAllDataAsyncTask = new DownloadAllDataAsyncTask(c);
                downloadAllDataAsyncTask.execute(p, detail.getValue());
            }
        };
        new Thread(r).start();

    }

    public void callSkinChangeHomeApp(long assetId){

        //スキンの種類によって行なう動作が違う
        ContentsDataDto cto = getContentsDataFromAssetId(assetId);

        if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
            DebugLog.instance.outputLog("value", "homeのActivity起動");

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_MAIN);
//			intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

            //あとアセットIDをput
            intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID, String.valueOf(assetId));

            //該当パッケージの該当クラス名//
//            intent.setClassName(c.getPackageName(), c.getPackageName() + ".launcher.Launcher");
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setPackage(c.getPackageName());

            c.startActivity(intent);

        }else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON.getValue()
                 || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue()
                 || cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue() ){
            //TODO アイコンも一括設定か個別設定かが必要

        }else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP.getValue() ||
                cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WP_IN_T.getValue() ){

        }else if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET.getValue() ||
                cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_WIDGET_IN_T.getValue() ){
            //TODO ウィジェットの種別が必要

        }

    }

    public void deleteUnUsedSkin(Context con, long assetId, ContentsTypeValue type){
        boolean isTheme = false;
        //DB内のレコードを取得して、isFavoriteがfalseならレコード削除して問題なし
        MyPageDataAccess mAccess = new MyPageDataAccess(con);
        Array<MyPageDataDto> mArray = mAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(assetId));
        MyPageDataDto dto = null;
        if(mArray != null && mArray.size > 0){
                dto = mArray.get(0);
                if(dto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
                    return;
                }else{
                    //TODO マイページの仕様によっては上限を超えていなければisFavoriteがtrueでなくてもマイページ表示用に残す可能性もある（※単独配信スキン
                    if(!dto.isFavorite){
                        mAccess.deleteById(assetId);
                    }else{
                        //isExistはfalseにする
                        dto.isExist = false;

                        mAccess.updateSkinIsExist(dto, false);
                    }
                }

                FileUtility.delFile(new File(FileUtility.getSkinPath(con, true, type) + assetId + File.separator));
        }else{
            BadgeDataAccess bAccess = new BadgeDataAccess(con);
            Array<BadgeDataDto> bArray = bAccess.findMyPageDataDtoFromDB(DataBaseParam.COL_ASSET_ID.getParam(), String.valueOf(assetId));
            BadgeDataDto bto = null;
            if(bArray != null && bArray.size > 0){
                bto = bArray.get(0);
                if(bto.contentsType == ContentsTypeValue.CONTENTS_TYPE_THEME.getValue()){
                    return;
                }else{
                    bAccess.deleteById(assetId);
                }

                FileUtility.delFile(new File(FileUtility.getSkinPath(con, true, type) + assetId + File.separator));
            }
        }


    }

}
