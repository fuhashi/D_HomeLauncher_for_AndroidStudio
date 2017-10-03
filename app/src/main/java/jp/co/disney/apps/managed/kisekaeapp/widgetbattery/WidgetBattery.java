package jp.co.disney.apps.managed.kisekaeapp.widgetbattery;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDetailTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.adobe.mobile.Analytics;
import com.adobe.mobile.CustomAndroidSystemObject;

public class WidgetBattery extends AppWidgetProvider {

    private final static ContentsFileName[] NOFileNames = {ContentsFileName.wdtBatteryNo0, ContentsFileName.wdtBatteryNo1, ContentsFileName.wdtBatteryNo2, ContentsFileName.wdtBatteryNo3, ContentsFileName.wdtBatteryNo4, ContentsFileName.wdtBatteryNo5, ContentsFileName.wdtBatteryNo6, ContentsFileName.wdtBatteryNo7, ContentsFileName.wdtBatteryNo8, ContentsFileName.wdtBatteryNo9 };//数字ファイル名
    private final static ContentsFileName[] NOFileNames_ex10 = {ContentsFileName.wdtBatteryNo0ex10, ContentsFileName.wdtBatteryNo1ex10, ContentsFileName.wdtBatteryNo2ex10, ContentsFileName.wdtBatteryNo3ex10, ContentsFileName.wdtBatteryNo4ex10, ContentsFileName.wdtBatteryNo5ex10, ContentsFileName.wdtBatteryNo6ex10, ContentsFileName.wdtBatteryNo7ex10, ContentsFileName.wdtBatteryNo8ex10, ContentsFileName.wdtBatteryNo9ex10 };//数字ファイル名
    static String WIDGET_BATTERY_SKINCHANGE = "jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WIDGET_BATTERY_SKINCHANGE";
    static String WIDGET_BATTERY_AppMeasurementKeepAlive = "jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WIDGET_BATTERY_AppMeasurementKeepAlive";

    private static int BATTERYlevel = 0;
    private static int BATTERYleveltemp = 0;
    private static int BATTERYstatus = 0;//2:充電中(BATTERY_STATUS_CHARGING)
    private static int BATTERYstatustemp = 0;

    private static int AppMeasurementKeepAlivePeriod = 86400000;//1d_taihi
//    private static int AppMeasurementKeepAlivePeriod = 60000;//1m_tes
//    private static int AppMeasurementKeepAlivePeriod = 10000;//10s_tes


    @Override
    public void onEnabled(Context context) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onEnabled");
        super.onEnabled(context);

//        TrackingHelper. start_widget(context, "", false);//AppMeasurement対応
      //AppMeasurement4.x対応↓
        SharedPreferences pref = context.getSharedPreferences("sc_setting", context.MODE_PRIVATE);
     // 自動計測値の生成と整形
//        CustomAndroidSystemObject caso = new CustomAndroidSystemObject((Activity) this, pref);
//        CustomAndroidSystemObject caso = new CustomAndroidSystemObject((Activity)context.getApplicationContext(), pref);//x
//        CustomAndroidSystemObject caso = new CustomAndroidSystemObject((Activity)context.getApplicationInfo(), pref);//x
//        CustomAndroidSystemObject caso = new CustomAndroidSystemObject( (Activity)jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WidgetPickerActivity.class, pref);//x
//        CustomAndroidSystemObject caso = new CustomAndroidSystemObject((Activity) this.getClass(), pref);
        CustomAndroidSystemObject caso = new CustomAndroidSystemObject(context.getApplicationContext(), pref);//D社指示通り


        // ContextData 変数用のHashmap を用意
        HashMap cdata = new HashMap<String, Object>();
        cdata.put("packageName",caso.getPackageName());
        cdata.put("appVer", caso.getAppVer());
        cdata.put("osName", caso.getOSNAME());
        cdata.put("modelName", caso.getModelName());
        cdata.put("osVer", caso.getOsVer());
        cdata.put("iccImei", caso.getIccImei());
//        cdata.put("installDate", caso.getInstallDate());
		SharedPreferences preferences = context.getSharedPreferences(SplashActivity.AUTH_RESULT_PREF, Context.MODE_PRIVATE);
    	String carrierId = preferences.getString(SplashActivity.AUTH_RESULT_CARRIER, "");
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onEnabled_carrierId_" + carrierId);
//        cdata.put("market","マーケット名");
        if(!carrierId.equals("")){
        	String market ="";
        	if(carrierId.equals(SplashActivity.AUTH_CARRIER_OND)){
        		market = "OnD:Pink Market";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_ONS)){
        		market = "OnS:Black Market";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_AU)){
        		market = "ConpaK:Disney Pass";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_CONPAS)){
        		market = "ConpaS: SBMMarket";
        	}
        	cdata.put("market", market);
    	}
        cdata.put("customEvent","widget_start");
        Analytics.trackState(caso.getPackageName(),cdata);
      //AppMeasurement4.x対応↑
}

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate");
          for(int id : appWidgetIds){
              jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate_id_" + id);

                SharedPreferences mPref = context.getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);

                long lastSetAssetId = ContentsOperatorForWidget.op.getNewestContents(context, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY);
                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate_lastSetAssetId_" + lastSetAssetId);

                long assetId=0;
                if(lastSetAssetId==0){
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate_if(lastSetAssetId==0){_");
                    assetId=-1;
                }else{
                    assetId = mPref.getLong(String.valueOf(id), 0);
                    jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate_else_if(lastSetAssetId==0){__lastSetAssetId_assetId_" + lastSetAssetId +"_"+ assetId);
                    if(assetId==0){
                        assetId = lastSetAssetId;//tes
                    }else{

                    }
                }
                Editor e = mPref.edit();
                e.putLong(String.valueOf(id), assetId);
                e.commit();



                  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate_id_assetId_" + id + "_"+ assetId);


                  //セット初回rv更新_tes

                  int BATlevel = 0;
                  int BATstatus = 0;
                  if(assetId==-1){//初回貼り付け時、セット済みが無くてデフォルト使用時
                      Intent bat = context.getApplicationContext().registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                      BATlevel = bat.getIntExtra("level", 0);
                      BATstatus = bat.getIntExtra("status", 0);
                  }
                  else{//通常時
                      Editor ed = mPref.edit();
                      ed.putLong("lastSetAssetId", assetId);
                      ed.commit();

                      Intent bat = context.getApplicationContext().registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                      BATlevel = bat.getIntExtra("level", 0);
                      BATstatus = bat.getIntExtra("status", 0);
                  }


                  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onUpdate_changeSkinsBef_id_assetId_" + id + "_"+ assetId);

//                //全数値描画テスト
//                BATlevel = 0;
//                //全数値描画テスト

                  changeSkins(context, BATstatus, BATlevel, id);

                     //セット初回rv更新_tes


                     //AppMeasurementKeepAlive予約
                     Intent inten = new Intent(WIDGET_BATTERY_AppMeasurementKeepAlive);//
                     inten.putExtra("appWidgetId", id);
                     PendingIntent contentInten = PendingIntent.getBroadcast(context,id,inten,PendingIntent.FLAG_ONE_SHOT);

                     long now = System.currentTimeMillis();
                       AlarmManager alarmManage = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                       if(Build.VERSION.SDK_INT<=19){
                           alarmManage.set(AlarmManager.RTC_WAKEUP,now+AppMeasurementKeepAlivePeriod, contentInten);
                         }else{
                             alarmManage.setExact(AlarmManager.RTC_WAKEUP,now+AppMeasurementKeepAlivePeriod, contentInten);
                         }

                      //AppMeasurementKeepAlive予約


            }
        super.onUpdate(context, appWidgetManager, appWidgetIds);

        Intent in = new Intent(context, WidgetService.class);//
        context.getApplicationContext().startService(in);//

    }


    @Override
    public void onReceive(Context context, Intent intent) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onReceive_" + intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0));
        //Log.v("MyApp", "onReceive");
        super.onReceive(context, intent);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
//		jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onDeleted");
        super.onDeleted(context, appWidgetIds);

          for(int id : appWidgetIds){
              jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onDeleted_id_" + id);

              SharedPreferences mPref = context.getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
              long DeleteID = mPref.getLong(String.valueOf(id), 0);//今消したWidのアセットID
              boolean DeleteIDfromDB = true;
              Editor e = mPref.edit();
              e.remove(String.valueOf(id));
              e.commit();

              long DeleteSavedID = mPref.getLong("DeleteSavedID", 0);//消す時に最新だったため、一時退避していたアセットID
              boolean DeleteSavedIDfromDB = true;
              if(DeleteSavedID==0)DeleteSavedIDfromDB = false;
            //まず、無条件で削除
              Editor ed = mPref.edit();
              ed.remove("DeleteSavedID");
              ed.commit();
              jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onDeleted_DeleteID_DeleteSavedID_" + DeleteID +"_"+ DeleteSavedID);



                ComponentName thisWidget = new ComponentName(context, WidgetBattery.class);
                int[] appWidgetIDs = AppWidgetManager.getInstance(context).getAppWidgetIds(thisWidget);
                for(int ID : appWidgetIDs){

                    if(mPref.getLong(String.valueOf(ID), 0)==DeleteID){
                        DeleteIDfromDB =false;
//                        break;
                    }

                    if(DeleteSavedIDfromDB){
                    	if(mPref.getLong(String.valueOf(ID), 0)==DeleteSavedID){
                    	DeleteSavedIDfromDB =false;
//                    	//無条件で削除したが、使用中になっていた為、再度退避。
//                    	Editor edt = mPref.edit();
//                        edt.putLong("DeleteSavedID", DeleteSavedID);
//                        edt.commit();
////                        break;
                    	}
                    }

                    if(!DeleteIDfromDB && !DeleteSavedIDfromDB)break;
                }

              if(DeleteSavedIDfromDB){//消す時に最新だったため、一時退避していたアセットID
            	  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteSavedIDfromDB){_DeleteSavedID_DeleteID_" + DeleteSavedID +"_"+ DeleteID);
            	if(DeleteSavedID!=ContentsOperatorForWidget.op.getNewestContents(context, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY)){
            		ContentsOperatorForWidget.op.deleteUnUsedSkin(context, DeleteSavedID, ContentsTypeValue.CONTENTS_TYPE_WIDGET);
            		if(DeleteID==DeleteSavedID){
            			DeleteIDfromDB=false;
            			jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteSavedIDfromDB){_DeleteSavedID!=Newest_DeleteID==DeleteSavedID_");
            		}
              	  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteSavedIDfromDB){_DeleteSavedID!=Newest_deleteDone!");
            	}else{//最新だったため、一時退避
                    Editor edt = mPref.edit();
                    edt.putLong("DeleteSavedID", DeleteSavedID);
                    edt.commit();
                	  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteSavedIDfromDB){_DeleteSavedID==Newest");
            	}

              }
              if(DeleteIDfromDB){//今消したWidのアセットID
            	  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteIDfromDB){_DeleteID_" + DeleteID);
                	if(DeleteID!=ContentsOperatorForWidget.op.getNewestContents(context, ContentsDetailTypeValue.WIDGET_DETAIL_TYPE_BATTERY)){
                		ContentsOperatorForWidget.op.deleteUnUsedSkin(context, DeleteID, ContentsTypeValue.CONTENTS_TYPE_WIDGET);
                    	  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteIDfromDB){_DeleteID!=Newest_deleteDone!");
                	}else{//最新だったため、一時退避
                        Editor edt = mPref.edit();
                        edt.putLong("DeleteSavedID", DeleteID);
                        edt.commit();
                  	  jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(DeleteIDfromDB){_DeleteID==Newest");
                	}
                }

          		Intent intent = new Intent(WIDGET_BATTERY_AppMeasurementKeepAlive);
          	    PendingIntent contentIntent = PendingIntent.getBroadcast(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT);
          		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
          		alarmManager.cancel(contentIntent);

          		if(appWidgetIDs.length==0){//widが一つもなくなったら、サービス終了
                    Intent in = new Intent(context, WidgetService.class);//
                    context.getApplicationContext().stopService(in);//
                }
            }

    }

    @Override
    public void onDisabled(Context context) {
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "onDisabled");

        super.onDisabled(context);
    }

	private static Bitmap bitmapbg=null;
	private static Bitmap bitmapnoper = null;
	private static Bitmap bitmapno100=null;
	private static Bitmap bitmapno10=null;
	private static Bitmap bitmapno1=null;

	private static Bitmap bitmapbgresiz=null;
	private static Bitmap bitmapno100resiz=null;
	private static Bitmap bitmapno10resiz=null;
	private static Bitmap bitmapno1resiz=null;
	private static Bitmap bitmapnoperresiz=null;


    static void changeSkins(Context context,int BATstatus,int BATlevel,int thisAppWidgetId){
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "changeSkins_BATstatus_BATlevel_thisAppWidgetId_" + BATstatus +"_"+ BATlevel +"_"+ thisAppWidgetId);

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetbattery_main);
         AppWidgetManager awm = AppWidgetManager.getInstance(context);

        SharedPreferences mPref = context.getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);//test用
        long setAssetId = mPref.getLong(String.valueOf(thisAppWidgetId), 0);

        //解像度調整
        float viewWidth = context.getResources().getDisplayMetrics().widthPixels;
        float viewHeight = context.getResources().getDisplayMetrics().heightPixels;
        float dPix = 1;
        if(viewWidth<viewHeight){
            dPix = viewWidth/1080f;//taihi
        }else{
            dPix = viewWidth/1920f;//taihi
        }

        if((setAssetId==-1)||(setAssetId==0)){//デフォルト設定時

        	setDefaultSkin(context, BATstatus,BATlevel,dPix, remoteViews);

        }else{//if((setAssetId==-1)||(setAssetId==0)){//デフォルト設定時以外

//			//解像度調整
//	        int viewWidth = context.getResources().getDisplayMetrics().widthPixels;
//	        //横幅1080pixを基準にする
//	        float dPix = viewWidth/1080f;
            int resodds=1;
            BitmapFactory.Options imageOptions2 = new BitmapFactory.Options();
            if(dPix<=0.5){
                resodds = 2;
                imageOptions2.inSampleSize = 2;
            }

            String bgPath = "";  // "data/data/[パッケージ名]/files/sample.jpg"
        if(BATstatus == BatteryManager.BATTERY_STATUS_CHARGING){
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryCharge);
        }else{

        if((0<=BATlevel)&&(BATlevel<20)){
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryBg1);
        }else if((20<=BATlevel)&&(BATlevel<40)){
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryBg2);
        }else if((40<=BATlevel)&&(BATlevel<60)){
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryBg3);
        }else if((60<=BATlevel)&&(BATlevel<80)){
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryBg4);
        }else if(80<=BATlevel){
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryBg5);
        }else{
            bgPath = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryBg1);
        }
        }
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "changeSkins_bgPath_" + bgPath);

        String noPath100 = "";  //
        String noPath10 = "";  //
        String noPath1 = "";  //
        String noPathper = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryNoPer);  // "data/data/[パッケージ名]/files/sample.jpg" になる

        remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut2, View.GONE);
        remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut1, View.GONE);
        remoteViews.setViewVisibility(R.id.dummyNoImgRightOut1, View.GONE);
        remoteViews.setViewVisibility(R.id.dummyNoImgRightOut2, View.GONE);

            if(BATlevel!=100){
            remoteViews.setViewVisibility(R.id.M100Frame, View.GONE);
            remoteViews.setViewVisibility(R.id.M10Frame, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut1, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.dummyNoImgRightOut1, View.VISIBLE);

            noPath100 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames[0]);
            noPath10 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames_ex10[BATlevel/10]);
            File file10chk = new File(noPath10);
            if(file10chk.exists()){//
            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(BATlevel!=100){_if(file10chk.exists()){_noPath10_" + noPath10);
            }else{
            	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(BATlevel!=100){_else_if(file10chk.exists()){_noPath10_" + noPath10);
            	noPath10 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames[BATlevel/10]);
            }
             if(BATlevel/10==0){
            	 remoteViews.setViewVisibility(R.id.M10Frame, View.GONE);
                 remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut2, View.VISIBLE);
                 remoteViews.setViewVisibility(R.id.dummyNoImgRightOut2, View.VISIBLE);
             }
             noPath1 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames[BATlevel%10]);

            }else{//if(BATlevel!=100){
                remoteViews.setViewVisibility(R.id.M100Frame, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.M10Frame, View.VISIBLE);
                remoteViews.setViewVisibility(R.id.M1Frame, View.VISIBLE);
                noPath100 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, ContentsFileName.wdtBatteryNo1ex100);
                File file100chk = new File(noPath100);
                if(file100chk.exists()){//file100chk.delete();
                	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "BATlevel==100_if(file100chk.exists()){_noPath100_" + noPath100);
                	}else{
                    	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "BATlevel==100_else_if(file100chk.exists()){_noPath100_" + noPath100);
                	noPath100 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames[1]);
                	}
                noPath10 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames_ex10[0]);
                File file10chk2 = new File(noPath10);
                if(file10chk2.exists()){//file10chk.delete();
                	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "BATlevel==100_if(file10chk.exists()){_noPath10_" + noPath10);
                	}else{
                    	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "BATlevel==100_else_if(file10chk.exists()){_noPath10_" + noPath10);
                	noPath10 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames[0]);
                	}
                     noPath1 = ContentsOperatorForWidget.op.getContentsImagePath(context, setAssetId, NOFileNames[0]);
            }

        //個別PNG方式
        File filebg = new File(bgPath);
        if(filebg.exists()){bitmapbg = getBitmap(bgPath,resodds);	}else{}
        File file100 = new File(noPath100);
        if(file100.exists()){bitmapno100 = getBitmap(noPath100,resodds);	}else{}
        File file10 = new File(noPath10);
        if(file10.exists()){bitmapno10 = getBitmap(noPath10,resodds);	}else{}
        File file1 = new File(noPath1);
        if(file1.exists()){bitmapno1 = getBitmap(noPath1,resodds);	}else{}
        File fileper = new File(noPathper);
        if(fileper.exists()){bitmapnoper = getBitmap(noPathper,resodds);	}else{}

        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid",
        		"changeSkins_bgPath_" + bgPath
        	+ "_noPath100_" + noPath100
        	+ "_noPath10_" + noPath10
        	+ "_noPath1_" + noPath1
        	+ "_noPathper_" + noPathper
        		);

        if((bitmapno100==null)||(bitmapno10==null)||(bitmapno1==null)||(bitmapnoper==null)||(bitmapbg==null)){//どれかのbmpが取れない時。
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "bmp_null2_bitmapbg_bitmapno100_bitmapno10_bitmapno1_bitmapnoper_" + bitmapbg+"_"+bitmapno100+"_"+bitmapno10+"_"+bitmapno1+"_"+bitmapnoper);
            setDefaultSkin(context, BATstatus,BATlevel,dPix, remoteViews);
        }else{
            if(dPix==1){
                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(dPix==1){_viewWidth_viewHeight_dPix_" + viewWidth +"_"+ viewHeight +"_"+ dPix);
                //アンチエイリアス設定
                int widWidth = (int)(810*dPix);
                int widHeight = (int)(552*dPix);
                int widNoWidth = (int)(42*dPix);
                int widNoHeight = (int)(276*dPix);
                bitmapbgresiz= Bitmap.createScaledBitmap(bitmapbg, widWidth, widHeight, true);
                bitmapno100resiz= Bitmap.createScaledBitmap(bitmapno100, widNoWidth, widNoHeight, true);
                bitmapno10resiz= Bitmap.createScaledBitmap(bitmapno10,widNoWidth, widNoHeight, true);
                bitmapno1resiz= Bitmap.createScaledBitmap(bitmapno1,widNoWidth, widNoHeight, true);
                bitmapnoperresiz= Bitmap.createScaledBitmap(bitmapnoper,widNoWidth, widNoHeight, true);
            remoteViews.setImageViewBitmap(R.id.bg,bitmapbgresiz);//taihi
            remoteViews.setImageViewBitmap(R.id.M100Frame,bitmapno100resiz);//taihi
            remoteViews.setImageViewBitmap(R.id.M10Frame,bitmapno10resiz);//taihi
            remoteViews.setImageViewBitmap(R.id.M1Frame,bitmapno1resiz);//taihi
            remoteViews.setImageViewBitmap(R.id.MperFrame,bitmapnoperresiz);//taihi
            }
                else{
                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "else_if(dPix==1){_viewWidth_viewHeight_dPix_" + viewWidth +"_"+ viewHeight+"_"+ dPix);
                int widWidth = (int)(810*dPix);
                int widHeight = (int)(552*dPix);
                int widNoWidth = (int)(42*dPix);
                int widNoHeight = (int)(276*dPix);

                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "else_if(dPix==1){_viewWidth_dPix_widWidth_widHeight_resodds_" + viewWidth +"_"+ dPix +"_"+widWidth+"_"+widHeight+"_"+widNoWidth+"_"+widNoHeight +"_"+ resodds);

                //アンチエイリアス設定
                bitmapbgresiz= Bitmap.createScaledBitmap(bitmapbg, widWidth, widHeight, true);
                bitmapno100resiz= Bitmap.createScaledBitmap(bitmapno100, widNoWidth, widNoHeight, true);
                bitmapno10resiz= Bitmap.createScaledBitmap(bitmapno10,widNoWidth, widNoHeight, true);
                bitmapno1resiz= Bitmap.createScaledBitmap(bitmapno1,widNoWidth, widNoHeight, true);
                bitmapnoperresiz= Bitmap.createScaledBitmap(bitmapnoper,widNoWidth, widNoHeight, true);
            remoteViews.setImageViewBitmap(R.id.bg,bitmapbgresiz);//taihi
            remoteViews.setImageViewBitmap(R.id.M100Frame,bitmapno100resiz);//taihi
            remoteViews.setImageViewBitmap(R.id.M10Frame,bitmapno10resiz);//taihi
            remoteViews.setImageViewBitmap(R.id.M1Frame,bitmapno1resiz);//taihi
            remoteViews.setImageViewBitmap(R.id.MperFrame,bitmapnoperresiz);//taihi

            }//解像度調整
        }
        }

        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(BATTERYleveltemp != BATTERYlevel){_ING_get_END_");
        Intent i;
        i = new Intent(context.getApplicationContext(), jp.co.disney.apps.managed.kisekaeapp.widgetbattery.WidgetPickerActivity.class);
        i.putExtra("AppWidgetId", thisAppWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), thisAppWidgetId, i, 0);
        remoteViews.setOnClickPendingIntent(R.id.LinearLayout_top, pendingIntent);


        awm.updateAppWidget(thisAppWidgetId, remoteViews);
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "awm.updateAppWidget(thisAppWidgetId, remoteViews);");

        remoteViews.removeAllViews(R.layout.widgetbattery_main);
        remoteViews.removeAllViews(R.id.bg);
        remoteViews.removeAllViews(R.id.M100Frame);
        remoteViews.removeAllViews(R.id.M10Frame);
        remoteViews.removeAllViews(R.id.M1Frame);
        remoteViews.removeAllViews(R.id.MperFrame);
        remoteViews.setImageViewBitmap(R.id.bg,null);//taihi
        remoteViews.setImageViewBitmap(R.id.M100Frame,null);//taihi
        remoteViews.setImageViewBitmap(R.id.M10Frame,null);//taihi
        remoteViews.setImageViewBitmap(R.id.M1Frame,null);//taihi
        remoteViews.setImageViewBitmap(R.id.MperFrame,null);//taihi

        if(bitmapbg!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapbg!=null)");
        bitmapbg.recycle();        bitmapbg = null;   }
        if(bitmapnoper!= null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapnoper!=null)");
        bitmapnoper .recycle();         bitmapnoper= null;   }
        if(bitmapno100!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapno100!=null)");
        bitmapno100.recycle();         bitmapno100= null;   }
        if(bitmapno10!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapno10!=null)");
        bitmapno10.recycle();         bitmapno10= null;   }
        if(bitmapno1!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapno1!=null)");
        bitmapno1.recycle();         bitmapno1= null;   }

        if(bitmapbgresiz!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapbgresiz!=null)");
        bitmapbgresiz.recycle();         bitmapbgresiz= null;   }
        if(bitmapno100resiz!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapno100resiz!=null)");
        bitmapno100resiz.recycle();         bitmapno100resiz= null;   }
        if(bitmapno10resiz!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapno10resiz!=null)");
        bitmapno10resiz.recycle();         bitmapno10resiz= null;   }
        if(bitmapno1resiz!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapno1resiz!=null)");
        bitmapno1resiz.recycle();         bitmapno1resiz= null;   }
        if(bitmapnoperresiz!=null){jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(bitmapnoperresiz!=null)");
        bitmapnoperresiz.recycle();         bitmapnoperresiz= null;   }

//      //全数値描画テスト
//      //    static void changeSkins(Context context,int BATstatus,int BATlevel,int thisAppWidgetId){
//      if(BATlevel<100){
//      	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(BATTERYlevel>0){_BATTERYlevel_" + BATlevel);
//      Intent inte = new Intent(WIDGET_BATTERY_SKINCHANGE);//tes
//      inte.putExtra("appWidgetId", thisAppWidgetId);
//  	inte.putExtra("BATTERYlevel", BATlevel+1);
//  	inte.putExtra("BATTERYstatus", BATTERYstatus);
//      inte.putExtra("assetID", setAssetId);
//
//      PendingIntent contentIntent = PendingIntent.getBroadcast(context,thisAppWidgetId,inte,PendingIntent.FLAG_ONE_SHOT);
//
//       long now = System.currentTimeMillis();
//         AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//         if(Build.VERSION.SDK_INT<=19){
//           alarmManager.set(AlarmManager.RTC,now+300, contentIntent);
//         }else{
//             alarmManager.setExact(AlarmManager.RTC,now+300, contentIntent);
//         }
//
//      }else{
//      	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "else_if(BATTERYlevel>0){_BATTERYlevel_" + BATlevel);
//      }
//         //全数値描画テスト

    }

private static void setDefaultSkin(Context context,int BATstatus, int BATlevel, float dPix, RemoteViews remoteViews) {
	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "setDefaultSkin()");
		// TODO 自動生成されたメソッド・スタブ
    int resodds=1;
    BitmapFactory.Options imageOptions2 = new BitmapFactory.Options();
    if(dPix<=0.5){
        resodds = 2;
        imageOptions2.inSampleSize = 2;
    }


if(BATstatus == BatteryManager.BATTERY_STATUS_CHARGING){
	try {
		 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "try_wdtBatteryCharge");
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryCharge.getFileName(), context, resodds);
	} catch (IOException e) {					e.printStackTrace();				}
}else{

if((0<=BATlevel)&&(BATlevel<20)){
	try {
		 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "try_wdtBatteryBg1");
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryBg1.getFileName(), context, resodds);
	} catch (IOException e) {	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "catch_try_wdtBatteryBg1");				e.printStackTrace();				}
}else if((20<=BATlevel)&&(BATlevel<40)){
	try {
		 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "try_wdtBatteryBg2");
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryBg2.getFileName(), context, resodds);
	} catch (IOException e) {	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "catch_try_wdtBatteryBg2");				e.printStackTrace();				}
}else if((40<=BATlevel)&&(BATlevel<60)){
	try {
		 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "try_wdtBatteryBg3");
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryBg3.getFileName(), context, resodds);
	} catch (IOException e) {	 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "catch_try_wdtBatteryBg3");				e.printStackTrace();				}
}else if((60<=BATlevel)&&(BATlevel<80)){
	try {
		 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "try_wdtBatteryBg4");
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryBg4.getFileName(), context, resodds);
	} catch (IOException e) { jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "catch_try_wdtBatteryBg4");					e.printStackTrace();				}
}else if(80<=BATlevel){
	try {
		 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "try_wdtBatteryBg5");
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryBg5.getFileName(), context, resodds);
	} catch (IOException e) {				 jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "catch_try_wdtBatteryBg5");
			e.printStackTrace();				}
}else{
	try {
		bitmapbg = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryBg1.getFileName(), context, resodds);
	} catch (IOException e) {					e.printStackTrace();				}
}
}

try {
	bitmapnoper = loadBitmapAsset("themes/wdt_def/"+ContentsFileName.wdtBatteryNoPer.getFileName(), context, resodds);
} catch (IOException e) {					e.printStackTrace();				}


remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut2, View.GONE);
remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut1, View.GONE);
remoteViews.setViewVisibility(R.id.dummyNoImgRightOut1, View.GONE);
remoteViews.setViewVisibility(R.id.dummyNoImgRightOut2, View.GONE);

    if(BATlevel!=100){
        remoteViews.setViewVisibility(R.id.M100Frame, View.GONE);
        remoteViews.setViewVisibility(R.id.M10Frame, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut1, View.VISIBLE);
        remoteViews.setViewVisibility(R.id.dummyNoImgRightOut1, View.VISIBLE);

        try {
			bitmapno100 = loadBitmapAsset("themes/wdt_def/"+NOFileNames[0].getFileName(), context, resodds);
		} catch (IOException e) {					e.printStackTrace();				}
		try {
			bitmapno10 = loadBitmapAsset("themes/wdt_def/"+NOFileNames[BATlevel/10].getFileName(), context, resodds);
		} catch (IOException e) {					e.printStackTrace();				}
         if(BATlevel/10==0){
        	 remoteViews.setViewVisibility(R.id.M10Frame, View.GONE);
        	 remoteViews.setViewVisibility(R.id.dummyNoImgLeftOut2, View.VISIBLE);
             remoteViews.setViewVisibility(R.id.dummyNoImgRightOut2, View.VISIBLE);
         }
		try {
			bitmapno1 = loadBitmapAsset("themes/wdt_def/"+NOFileNames[BATlevel%10].getFileName(), context, resodds);
		} catch (IOException e) {					e.printStackTrace();				}

    }else{
            remoteViews.setViewVisibility(R.id.M100Frame, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.M10Frame, View.VISIBLE);
            remoteViews.setViewVisibility(R.id.M1Frame, View.VISIBLE);
            try {
				bitmapno100 = loadBitmapAsset("themes/wdt_def/"+NOFileNames[1].getFileName(), context, resodds);
			} catch (IOException e) {					e.printStackTrace();				}
			try {
				bitmapno10 = loadBitmapAsset("themes/wdt_def/"+NOFileNames[0].getFileName(), context, resodds);
			} catch (IOException e) {					e.printStackTrace();				}
			try {
				bitmapno1 = loadBitmapAsset("themes/wdt_def/"+NOFileNames[0].getFileName(), context, resodds);
			} catch (IOException e) {					e.printStackTrace();				}
    }

//
    if((bitmapno100==null)||(bitmapno10==null)||(bitmapno1==null)||(bitmapnoper==null)||(bitmapbg==null)){
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "bmp_null1_");
    }else{

    if(dPix==1){
////        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(dPix==1){_viewWidth_viewHeight_dPix_" + viewWidth +"_"+ viewHeight +"_"+ dPix);
        //アンチエイリアス設定
        int widWidth = (int)(810*dPix);
        int widHeight = (int)(552*dPix);
        int widNoWidth = (int)(42*dPix);
        int widNoHeight = (int)(276*dPix);
        bitmapbgresiz= Bitmap.createScaledBitmap(bitmapbg, widWidth, widHeight, true);
        bitmapno100resiz= Bitmap.createScaledBitmap(bitmapno100, widNoWidth, widNoHeight, true);
        bitmapno10resiz= Bitmap.createScaledBitmap(bitmapno10,widNoWidth, widNoHeight, true);
        bitmapno1resiz= Bitmap.createScaledBitmap(bitmapno1,widNoWidth, widNoHeight, true);
        bitmapnoperresiz= Bitmap.createScaledBitmap(bitmapnoper,widNoWidth, widNoHeight, true);
    remoteViews.setImageViewBitmap(R.id.bg,bitmapbgresiz);//taihi
    remoteViews.setImageViewBitmap(R.id.M100Frame,bitmapno100resiz);//taihi
    remoteViews.setImageViewBitmap(R.id.M10Frame,bitmapno10resiz);//taihi
    remoteViews.setImageViewBitmap(R.id.M1Frame,bitmapno1resiz);//taihi
    remoteViews.setImageViewBitmap(R.id.MperFrame,bitmapnoperresiz);//taihi
    }
        else{
//        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "else_if(dPix==1){_viewWidth_viewHeight_dPix_" + viewWidth +"_"+ viewHeight+"_"+ dPix);
        int widWidth = (int)(810*dPix);
        int widHeight = (int)(552*dPix);
        int widNoWidth = (int)(42*dPix);
        int widNoHeight = (int)(276*dPix);
//        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "else_if(dPix==1){_viewWidth_dPix_widWidth_widHeight_resodds_" + viewWidth +"_"+ dPix +"_"+widWidth+"_"+widHeight+"_"+widNoWidth+"_"+widNoHeight +"_"+ resodds);


        bitmapbgresiz= Bitmap.createScaledBitmap(bitmapbg, widWidth, widHeight, true);
        bitmapno100resiz= Bitmap.createScaledBitmap(bitmapno100, widNoWidth, widNoHeight, true);
        bitmapno10resiz= Bitmap.createScaledBitmap(bitmapno10,widNoWidth, widNoHeight, true);
        bitmapno1resiz= Bitmap.createScaledBitmap(bitmapno1,widNoWidth, widNoHeight, true);
        bitmapnoperresiz= Bitmap.createScaledBitmap(bitmapnoper,widNoWidth, widNoHeight, true);

    remoteViews.setImageViewBitmap(R.id.bg,bitmapbgresiz);
    remoteViews.setImageViewBitmap(R.id.M100Frame,bitmapno100resiz);
    remoteViews.setImageViewBitmap(R.id.M10Frame,bitmapno10resiz);
    remoteViews.setImageViewBitmap(R.id.M1Frame,bitmapno1resiz);
    remoteViews.setImageViewBitmap(R.id.MperFrame,bitmapnoperresiz);
    }
    }
	}

    private static Bitmap getBitmap(String path, int resOdds){
        FileInputStream in = null;
        try {

            File iconFile = new File(path);
            if (!iconFile.exists()) {
            return null;
            }

            in = new FileInputStream(iconFile);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = resOdds;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, options);
            return bmp;

        } catch (FileNotFoundException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

    }

  //assets フォルダから、画像ファイルを読み込む
    public static final Bitmap loadBitmapAsset(String fileName, Context context) throws IOException {
        final AssetManager assetManager = context.getAssets();
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(assetManager.open(fileName));
            return BitmapFactory.decodeStream(bis);
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                //IOException, NullPointerException
            }
        }
    }

    public static final Bitmap loadBitmapAsset(String fileName, Context context, int resOdds) throws IOException {
    	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "loadBitmapAsset_fileName_resOdds_" + fileName +"_"+ resOdds);
        final AssetManager assetManager = context.getAssets();
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(assetManager.open(fileName));
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = resOdds;
            Bitmap bmp = BitmapFactory.decodeStream(bis, null, options);
            return bmp;

        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                //IOException, NullPointerException
            }
        }
    }


    public static class WidgetService extends Service {

        @Override
        public void onStart(Intent in, int si) {
            //Log.v("MyApp", "onStart(Intent in, int si) {");
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetService_onStart()_appWidgetId_");
            if(in!=null){
                if(in.getExtras()!=null){
            int appWidgId = in.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetService_onStart()_appWidgetId_" + appWidgId);
                }
            }

            //skinきせかえ指令intnet
            IntentFilter filter = new IntentFilter(WIDGET_BATTERY_SKINCHANGE);
            registerReceiver(mReceiver, filter);

            //電池
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(broadcastReceiver_, intentFilter);

            //AppMeasurementKeepAliveReceiver
            IntentFilter AppMeasurementKeepAlivefilter = new IntentFilter(WIDGET_BATTERY_AppMeasurementKeepAlive);
            registerReceiver(mAppMeasurementKeepAliveReceiver, AppMeasurementKeepAlivefilter);

        }

        @Override
        public int onStartCommand(Intent in, int si, int startId) {
        	jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetService_onStartCommand()_appWidgetId_");
            if(in!=null){
                if(in.getExtras()!=null){
            int appWidgId = in.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID);
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetService_onStart()_appWidgetId_" + appWidgId);
                }
            }

            //skinきせかえ指令intnet
            IntentFilter filter = new IntentFilter(WIDGET_BATTERY_SKINCHANGE);
            registerReceiver(mReceiver, filter);

            //電池
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(broadcastReceiver_, intentFilter);

            //AppMeasurementKeepAliveReceiver
            IntentFilter AppMeasurementKeepAlivefilter = new IntentFilter(WIDGET_BATTERY_AppMeasurementKeepAlive);
            registerReceiver(mAppMeasurementKeepAliveReceiver, AppMeasurementKeepAlivefilter);

//          return START_REDELIVER_INTENT;//再起動が大分遅い
          return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent in) {
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetService_IBinder_onBind(()_");
            return null;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "WidgetService_onDestroy()_");
            //skinきせかえ指令intnet
            unregisterReceiver(mReceiver);

            //電池
            unregisterReceiver(broadcastReceiver_);

            //AppMeasurementKeepAliveReceiver
            unregisterReceiver(mAppMeasurementKeepAliveReceiver);

        }
    }

    static BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int thisAppWidgetId = intent.getIntExtra("appWidgetId", 0);
            int BATlevel = 0;
            int BATstatus = 0;
            long assetID = intent.getLongExtra("assetID", 0) ;
            if(assetID==0){//バッテリー情報変更時(ACTION_BATTERY_CHANGEDのBroadcast取得時)
                BATlevel = intent.getIntExtra ("BATTERYlevel", 0);
                BATstatus = intent.getIntExtra ("BATTERYstatus", 0);
            }else if(assetID==-1){//初回貼り付け時、セット済みが無くてデフォルト使用時
            	//退避
                Intent bat = context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                BATlevel = bat.getIntExtra("level", 0);
                BATstatus = bat.getIntExtra("status", 0);
//              //全数値描画テスト
//                BATlevel = intent.getIntExtra ("BATTERYlevel", 0);
//                BATstatus = intent.getIntExtra ("BATTERYstatus", 0);
//              //全数値描画テスト
            }
            else{//ピッカーから受信時
                SharedPreferences mPref = context.getSharedPreferences("wdt_battery_Prefs", Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
                Editor e = mPref.edit();
                e.putLong(String.valueOf(thisAppWidgetId), assetID);
                e.putLong("lastSetAssetId", assetID);
                e.commit();

                //退避
                Intent bat = context.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                BATlevel = bat.getIntExtra("level", 0);
                BATstatus = bat.getIntExtra("status", 0);

//              //全数値描画テスト
//              BATlevel = intent.getIntExtra ("BATTERYlevel", 0);
//              BATstatus = intent.getIntExtra ("BATTERYstatus", 0);
//            //全数値描画テスト
            }


            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "mReceiver received_action_thisAppWidgetId_assetID_" + action +"_"+ thisAppWidgetId +"_"+ assetID);

            changeSkins(context, BATstatus, BATlevel, thisAppWidgetId);

            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "mReceiver received_action_thisAppWidgetId_assetID_END_" + action +"_"+ thisAppWidgetId +"_"+ assetID);
        }
    };


    static BroadcastReceiver broadcastReceiver_ = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {");

                // バッテリー残量
//                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "Level : _BATTERYlevel_BATTERYleveltemp_" + String.valueOf(intent.getIntExtra("level", 0)) + "_"+ BATTERYlevel +"_"+ BATTERYleveltemp);
                BATTERYlevel = intent.getIntExtra("level", 0);
                BATTERYstatus = intent.getIntExtra("status", 0);
                jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {_BATTERYlevel_BATTERYleveltemp_BATTERYstatus_BATTERYstatustemp_" + BATTERYlevel +"_"+BATTERYleveltemp +  "_"+BATTERYstatus +"_"+ BATTERYstatustemp);
                if((BATTERYleveltemp != BATTERYlevel) || (BATTERYstatustemp != BATTERYstatus)){

                	if(BATTERYleveltemp==0&&BATTERYstatustemp==0){//WidgetPickerを立ち上げて、パーミッションチェックに行って、許可した時に、WidgetServiceのonStartCommandが起動して、ACTION_BATTERY_CHANGEDの処理を行い、その最中にWidのスキンを変えようとしたら、変にAssetIDが初期値で上書かれて、正常にスキンが変わらない、という事象の回避対応。
                        BATTERYleveltemp = BATTERYlevel;
                        BATTERYstatustemp = BATTERYstatus;
                	}else{//WidgetPickerを立ち上げて、パーミッションチェックに行って、許可した時に、WidgetServiceのonStartCommandが起動して、ACTION_BATTERY_CHANGEDの処理を行い、その最中にWidのスキンを変えようとしたら、変にAssetIDが初期値で上書かれて、正常にスキンが変わらない、という事象の回避対応。
                    BATTERYleveltemp = BATTERYlevel;
                    BATTERYstatustemp = BATTERYstatus;


                    ComponentName thisWidget = new ComponentName(context, WidgetBattery.class);
                    int[] appWidgetIds = AppWidgetManager.getInstance(context).getAppWidgetIds(thisWidget);
                    for(int id : appWidgetIds){

                        //jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(BATTERYleveltemp != BATTERYlevel){_BATTERYlevel_BATTERYleveltemp_id_" + String.valueOf(intent.getIntExtra("level", 0)) + "_"+ BATTERYlevel +"_"+ BATTERYleveltemp +"_"+id);

                        // 個別のWidにブロードキャスト送信
                        //Log.v("MyApp", "setNextTime_Anime_bgAlpha_" + bgAlpha);
                        Intent inte = new Intent(WIDGET_BATTERY_SKINCHANGE);//tes

                        inte.putExtra("appWidgetId", id);
                        inte.putExtra("BATTERYlevel", BATTERYlevel);
                        inte.putExtra("BATTERYstatus", BATTERYstatus);

                        PendingIntent contentIntent = PendingIntent.getBroadcast(context,id,inte,PendingIntent.FLAG_ONE_SHOT);
                         long now = System.currentTimeMillis();
                           AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                           if(Build.VERSION.SDK_INT<=19){
                               alarmManager.set(AlarmManager.RTC_WAKEUP,now+100, contentIntent);
                             }else{
                                 alarmManager.setExact(AlarmManager.RTC_WAKEUP,now+100, contentIntent);
                             }
                	}

                        //jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "if(BATTERYleveltemp != BATTERYlevel){_ING_get_START_");

                    }//WidgetPickerを立ち上げて、パーミッションチェックに行って、許可した時に、WidgetServiceのonStartCommandが起動して、ACTION_BATTERY_CHANGEDの処理を行い、その最中にWidのスキンを変えようとしたら、変にAssetIDが初期値で上書かれて、正常にスキンが変わらない、という事象の回避対応。

                }//if((BATTERYleveltemp != BATTERYlevel) || (BATTERYstatustemp != BATTERYstatus)){

            }
        }


    };


    static BroadcastReceiver mAppMeasurementKeepAliveReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "mAppMeasurementKeepAliveReceiver_onReceive_appWidgetId_");

//            TrackingHelper. set_keepAlive(context, false);//AppMeasurement対応
//            aa_getWidgetAlive();//AppMeasurement4.x対応
            aa_getWidgetAlive(context);//AppMeasurement4.x対応


            String action = intent.getAction();
            int thisAppWidgetId = intent.getIntExtra("appWidgetId", 0);
          //AppMeasurementKeepAlive予約
            Intent inte = new Intent(WIDGET_BATTERY_AppMeasurementKeepAlive);//tes
            inte.putExtra("appWidgetId", thisAppWidgetId);
            PendingIntent contentIntent = PendingIntent.getBroadcast(context,thisAppWidgetId,inte,PendingIntent.FLAG_UPDATE_CURRENT);
             long now = System.currentTimeMillis();
               AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
               if(Build.VERSION.SDK_INT<=19){
                   alarmManager.set(AlarmManager.RTC_WAKEUP,now+AppMeasurementKeepAlivePeriod, contentIntent);
                 }else{
                     alarmManager.setExact(AlarmManager.RTC_WAKEUP,now+AppMeasurementKeepAlivePeriod, contentIntent);
                 }

             //AppMeasurementKeepAlive予約

               jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "mAppMeasurementKeepAliveReceiver_onReceive_appWidgetId_" + thisAppWidgetId);

        }
    };

//    public static void aa_getWidgetAlive(){//AppMeasurement4.x対応
    public static void aa_getWidgetAlive(Context context){//AppMeasurement4.x対応

    	SharedPreferences pref = context.getSharedPreferences("sc_setting", context.MODE_PRIVATE);
    	// 自動計測値の生成と整形
//    	CustomAndroidSystemObject caso = new CustomAndroidSystemObject((Activity) this);
//    	CustomAndroidSystemObject caso = new CustomAndroidSystemObject(context, pref);
        CustomAndroidSystemObject caso = new CustomAndroidSystemObject(context.getApplicationContext(), pref);//D社指示通り

        // ContextData 変数用のHashmap を用意
    	HashMap cdata = new HashMap<String, Object>();
    	cdata.put("packageName",caso.getPackageName());
    	cdata.put("appVer", caso.getAppVer());
    	cdata.put("osName", caso.getOSNAME());
    	cdata.put("modelName", caso.getModelName());
    	cdata.put("osVer", caso.getOsVer());
    	cdata.put("iccImei", caso.getIccImei());
//    	cdata.put("installDate", caso.getInstallDate());
    	SharedPreferences preferences = context.getSharedPreferences(SplashActivity.AUTH_RESULT_PREF, Context.MODE_PRIVATE);
    	String carrierId = preferences.getString(SplashActivity.AUTH_RESULT_CARRIER, "");
        jp.co.disney.apps.managed.kisekaeapp.DebugLog.instance.outputLog("DKWid", "aa_getWidgetAlive_carrierId_" + carrierId);

//    	cdata.put("market","マーケット名");
    	if(!carrierId.equals("")){
        	String market ="";
        	if(carrierId.equals(SplashActivity.AUTH_CARRIER_OND)){
        		market = "OnD:Pink Market";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_ONS)){
        		market = "OnS:Black Market";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_AU)){
        		market = "ConpaK:Disney Pass";
        	}else if(carrierId.equals(SplashActivity.AUTH_CARRIER_CONPAS)){
        		market = "ConpaS: SBMMarket";
        	}
        	cdata.put("market", market);
    	}

    	cdata.put("customEvent","widget_alive");
    	Analytics.trackState(caso.getPackageName(),cdata);
    }
}

