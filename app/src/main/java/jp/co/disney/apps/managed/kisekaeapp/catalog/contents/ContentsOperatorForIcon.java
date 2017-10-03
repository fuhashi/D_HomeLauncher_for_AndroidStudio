package jp.co.disney.apps.managed.kisekaeapp.catalog.contents;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadAllDataAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadIconThumbSeparateAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.SetFavoriteAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.PublishDateComparator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataAccess;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.iconpicker.IconSelectActivity;
import jp.co.disney.apps.managed.kisekaeapp.iconpicker.IconSelectGridTypeActivity;
import jp.co.disney.apps.managed.kisekaeapp.iconpicker.IconSelectListTypeActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import com.badlogic.gdx.utils.Array;

public class ContentsOperatorForIcon {

    public static final ContentsOperatorForIcon op = new ContentsOperatorForIcon();
    private static Context c;
    private static Array<ContentsDataDto> ctoArray = null;
//    public static final String CHANGE_DB_STATE = "jp.co.disney.apps.managed.kisekaeapp.change_db_state_from_skinpicker";

    public ContentsOperatorForIcon() {
        super();
    }

    public ContentsOperatorForIcon(Context con) {
        super();
        this.c = con;
    }

    public ContentsOperatorForIcon(Context con, Array<ContentsDataDto> array) {
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
        DebugLog.instance.outputLog("value", "reflectCtoArrayFromDB_i");
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
/*
    public ContentsDataDto getContentsDataFromThemeTag(String themeTag){
        if(ctoArray == null || ctoArray.size <= 0) return null;

        for(ContentsDataDto cto : ctoArray){
            if(themeTag.equals(cto.themeTag)){
                return cto;
            }
        }

        return null;
    }
*/

    public void sendDBStateChangeBroadcast(Context con){
        Intent intent = new Intent(ContentsOperatorForCatalog.CHANGE_DB_STATE);
        con.sendBroadcast(intent);
    }
/*
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
*/
/*
    public ContentsDataDto getContentsDataFromIndex(int index){
        if(ctoArray == null || ctoArray.size == 0 || index >= ctoArray.size){
            return null;
        }else{
            return ctoArray.get(index);
        }
    }
*/
/*
    public ContentsDataDto getContentsDataFromAssetId(String assetID){
        return getContentsDataFromAssetId(Long.valueOf(assetID));
    }
*/
    public ContentsDataDto getContentsDataFromAssetId(long assetID){
        if(ctoArray == null || ctoArray.size <= 0) return null;

        for(ContentsDataDto cto : ctoArray){
            if(assetID == cto.assetID){
                return cto;
            }
        }
        return null;
    }
/*
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
*/
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
/*    
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
*/
    //---------------------------------AsyncTask関連

    private DownloadAllDataAsyncTask downloadAllDataAsyncTask = null;;
    private DownloadSkinAsyncTask downloadSkinAsyncTask = null;
    private DownloadIconThumbSeparateAsyncTask downloadIconThumbAsyncTask = null;
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
        
        if(downloadIconThumbAsyncTask != null && !downloadIconThumbAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "downloadIconThumbAsyncTaskのキャンセル");
            downloadIconThumbAsyncTask.cancel(true);
        }
        
        if(setFavoriteAsyncTask != null && !setFavoriteAsyncTask.isCancelled()){
            DebugLog.instance.outputLog("value", "setFavoriteAsyncTaskのキャンセル");
            setFavoriteAsyncTask.cancel(true);
        }

    }
    
    public void callIconSelectGridActivity(ContentsDataDto cto, Intent fromIntent){
    	//リスト表示のアイコン呼び出し元は通常のカタログ遷移と、アイコンきせかえアプリからの２パタン。
        DebugLog.instance.outputLog("value", "アイコンピッカーActivity起動");

        Intent intent = new Intent(c.getApplicationContext(), IconSelectGridTypeActivity.class);
        intent.setAction(Intent.ACTION_VIEW);

        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID, String.valueOf(cto.assetID));
        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, cto.contentsType);
//      intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_DETAIL_CONTENTS_TYPE, cto.detailContentsType);
        
        intent.putExtra(IconSelectActivity.KISEKAE_SAKI_SHORTCUTICON_DTO, cto.getMyDto());
        intent.putExtra(IconSelectGridTypeActivity.KISEKAE_MOTO_APP_INTENT, fromIntent);
//        intent.setClassName(c.getPackageName(), c.getPackageName() + ".iconpicker.IconSelectGridTypeActivity");
        
 
        c.startActivity(intent);

    }
    
    public void callIconSelectListActivity(ContentsDataDto cto){
    	//リスト表示のアイコン呼び出し元は通常のカタログ遷移と、アイコンきせかえアプリからの２パタン。
        DebugLog.instance.outputLog("value", "アイコンピッカーActivity起動");

        Intent intent = new Intent(c.getApplicationContext(), IconSelectListTypeActivity.class);
        intent.setAction(Intent.ACTION_VIEW);

        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_ASSET_ID, String.valueOf(cto.assetID));
        intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_CONTENTS_TYPE, cto.contentsType);
//      intent.putExtra(ContentsOperator.INTENT_ACTION_EXTRA_DETAIL_CONTENTS_TYPE, cto.detailContentsType);
        
        intent.putExtra(IconSelectActivity.KISEKAE_SAKI_SHORTCUTICON_DTO, cto.getMyDto());
        
        intent.putExtra(ContentsOperatorForCatalog.INTENT_EXTRA_ISFROMCATALOG, false);
//            intent.setClassName(c.getPackageName(), c.getPackageName() + ".iconpicker.IconSelectListTypeActivity");

        c.startActivity(intent);

    }


    public boolean callDownloadSkinTask(final ContentsDataDto cto){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                
                if(cto.contentsType == ContentsTypeValue.CONTENTS_TYPE_SHORTCUT_ICON_IN_T.getValue()){
                    downloadIconThumbAsyncTask = new DownloadIconThumbSeparateAsyncTask(c);
                    downloadIconThumbAsyncTask.execute(cto);

                }else{
                    //currentに存在してなかったらスキンのダウンロード開始
                    downloadSkinAsyncTask = new DownloadSkinAsyncTask(c);
                    downloadSkinAsyncTask.execute(cto);
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
                DebugLog.instance.outputLog("value", "アイコンから起動_" + detail.getValue());
                downloadAllDataAsyncTask = new DownloadAllDataAsyncTask(c);
                downloadAllDataAsyncTask.execute(p, detail.getValue());
            }
        };
        new Thread(r).start();

    }

/*
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
*/
/*
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
*/
}
