package jp.co.disney.apps.managed.kisekaeapp.iconpicker;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.ShortcutIconThumbsComparator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.launcher.InstallShortcutReceiver;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

public class IconSelectGridTypeActivity extends IconSelectActivity implements DownloadSkinTaskCallback{

	public static String KISEKAE_MOTO_APP_INTENT = "jp.co.disney.apps.managed.kisekaeapp.kisekaemoto.appintent";

	private Activity me;
	
	private CustomArrayAdapter2 arrayAdapter = null;
	private GridView gridView = null;
	private int targetPosition = -1;
	
	/**
	 * 座標計算用の補正値
	 */
	float dispScaleperBase = 1f;//FHD(1080x1920)と比較しての倍率
	private MyPageDataDto iconDto = null;
	
	//既存ショートカット情報
    String setAppName = "";
    // -- アプリ起動Intent
    Intent setLaunchIntent = null;
    // -- ユーザー編集ショートカット名（null許容）
    String setShortcutName = "";
    // -- ページ番号
    int setScreen = -1;
    // -- グリッド位置X
    int setCellX = -1;
    // -- グリッド位置Y
    int setCellY = -1;
    // -- ショートカットID
    long setId = -1L;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.icon_gridtype_activity_main);

		me = this;
		
		int viewWidth = this.getResources().getDisplayMetrics().widthPixels;
		dispScaleperBase = (float)viewWidth/1080f;
//		float dens = getResources().getDisplayMetrics().density;
		
		final Intent getIntent = getIntent();
		iconDto = (MyPageDataDto)getIntent.getSerializableExtra(KISEKAE_SAKI_SHORTCUTICON_DTO);
		
        // 既存ショートカット情報受取 START
		Intent kisekaeIntent = getIntent.getParcelableExtra(KISEKAE_MOTO_APP_INTENT);
        // -- アプリ名
        setAppName = kisekaeIntent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        // -- アプリ起動Intent
        setLaunchIntent = kisekaeIntent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        // -- ユーザー編集ショートカット名（null許容）
        setShortcutName = kisekaeIntent.getStringExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_NAME_USER);
        // -- ページ番号
        setScreen = kisekaeIntent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_SCREEN, -1);
        // -- グリッド位置X
        setCellX = kisekaeIntent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_CELLX, -1);
        // -- グリッド位置Y
        setCellY = kisekaeIntent.getIntExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_CELLY, -1);
        // -- ショートカットID
        setId = kisekaeIntent.getLongExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_ID, -1L);
        // 既存ショートカット情報受取 END
        
        //-------------------各画面要素------------------------
        //ヘッダー関連の大きさと位置指定
		LinearLayout headerLayout = (LinearLayout) findViewById(R.id.setapp_header_grid);
		LinearLayout.LayoutParams headerBaseParam = new LinearLayout.LayoutParams((int)(1080*dispScaleperBase), (int)(205*dispScaleperBase));
		headerLayout.setLayoutParams(headerBaseParam);
		
		ImageView headerTitle = (ImageView) findViewById(R.id.setapp_grid_title);
		LinearLayout.LayoutParams headerTitleParam = new LinearLayout.LayoutParams((int)(261*dispScaleperBase), (int)(47*dispScaleperBase));
		headerTitleParam.setMargins((int)(412*dispScaleperBase), (int)((168-75)*dispScaleperBase), 0, 0);
		headerTitle.setLayoutParams(headerTitleParam);
		
		//フッター関連の大きさと位置指定
		LinearLayout footerLayout = (LinearLayout) findViewById(R.id.setapp_bottom_grid);
		LinearLayout.LayoutParams footerBaseParam = new LinearLayout.LayoutParams((int)(1080*dispScaleperBase), (int)(252*dispScaleperBase));
		footerBaseParam.weight = 1;
		footerLayout.setLayoutParams(footerBaseParam);
		
		ImageButton footerCancelBtn = (ImageButton) findViewById(R.id.setapp_cancel_btn_grid);
		LinearLayout.LayoutParams footerBtnParam = new LinearLayout.LayoutParams((int)(396*dispScaleperBase), (int)(80*dispScaleperBase));
		footerBtnParam.setMargins((int)(342*dispScaleperBase), (int)((83)*dispScaleperBase), 0, 0);
		footerCancelBtn.setLayoutParams(footerBtnParam);
		
		//アイコン表示Grid
		gridView = (GridView) findViewById(R.id.icon_grid);
		
		LinearLayout.LayoutParams gridViewParam = new LinearLayout.LayoutParams((int)(1080*dispScaleperBase), (int)(1240*dispScaleperBase));
		gridViewParam.setMargins(0, 0, 0, 0);
		gridView.setLayoutParams(gridViewParam);
		gridView.setPadding(0, 0, 0, 0);
		gridView.setChoiceMode(GridView.CHOICE_MODE_SINGLE);

		//Grid表示分のitemを作成
		File[] iconFile = FileUtility.getFilesInDirectory(me.getCacheDir().getAbsolutePath() + File.separator + iconDto.assetID + File.separator);
		final int iconNum = iconFile.length;

		List<ShortcutIconListData> objects = new ArrayList<ShortcutIconListData>();
		int iconMax = 20;
		if(iconNum > iconMax){
			iconMax = iconNum;
		}
//        for(int i = 0; i < iconMax; i++){
		int endNum = 20;
		if(iconNum != 20) endNum = iconNum + (4 - iconNum%4);
        for(int i = 0; i < endNum; i++){
        	ShortcutIconListData item1 = new ShortcutIconListData();
            
            //アプリアイコンはnull
            item1.setAppIconData(null);

            if(i >= iconNum){
            	//数合わせ用なので諸々空
                item1.setAppPackage("");
                item1.setAppName("");
                item1.setAppClassName("");
                item1.setSelfIconData(null);
                item1.setSelfIconPath("");
            	
            }else{
                item1.setAppPackage("");
                item1.setAppName("");
                item1.setAppClassName("");
                item1.setSelfIconData(getShortcutIconImage(iconFile[i].getAbsolutePath()));
                item1.setSelfIconPath(iconFile[i].getAbsolutePath());
            	
            }
            
            objects.add(item1);
            
            
        }

        //ソート
        Collections.sort(objects, new ShortcutIconThumbsComparator());
        iconFile = null;
        
        DebugLog.instance.outputLog("value", "_____________itemの総数_" + objects.size());

		arrayAdapter = new CustomArrayAdapter2(getApplicationContext(), R.layout.icon_listtype_list_row, objects);
		gridView.setAdapter(arrayAdapter);
		
		gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				if(arrayAdapter.getItem(position).getSelfIconData() == null){
					return;
				}

				DebugLog.instance.outputLog("value", "グリッドタップ＿" + position);

				targetPosition = position;
				
                boolean isStopDL = false;
                String carrierId = SPPUtility.getCarrierID(me.getApplicationContext());
            	if(carrierId.equals(SplashActivity.AUTH_CARRIER_OND)
            			|| carrierId.equals(SplashActivity.AUTH_CARRIER_ONS)
            			|| carrierId.equals(SplashActivity.AUTH_CARRIER_AU)
            			|| carrierId.equals(SplashActivity.AUTH_CARRIER_CONPAS)){
            		//GetAuthInfoが必要
                   	if(SPPUtility.isNeedToken(me.getApplicationContext())){
                   		DebugLog.instance.outputLog("value", "tokenが必要！");
                   		isStopDL = true;
                   		startGetAuthInfo();
                	}
            	}
            	
            	if(!isStopDL){
            		//本体ダウンロードAsynTask
                    DownloadSkinAsyncTask downloadSkinAsyncTask = new DownloadSkinAsyncTask(me);
                    downloadSkinAsyncTask.isShowProgress = true;
                    downloadSkinAsyncTask.execute(new ContentsDataDto(iconDto));
            	}
                			
			}
		});
		
		ImageButton cancelBtn = (ImageButton) findViewById(R.id.setapp_cancel_btn_grid);
		cancelBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				me.finish();
			}
		});

		//ザブトンアイコンに、起動時に選択したアプリアイコンを合成
		setAppForZavtoneIcon();

	}

	
	
/*
	public Bitmap loadBitmapInnerStorage(Context context, int index) throws IOException {
		BufferedInputStream bis = null;
		String loadFileName = "myPhoto_" + String.valueOf(index) + ".png";
		
	    try {
		bis = new BufferedInputStream(context.openFileInput(loadFileName));
		return setAlpha(BitmapFactory.decodeStream(bis), 122);
	    } finally {
	        try {
	            bis.close();
	        } catch (Exception e) {
	            //IOException, NullPointerException
	        }
	    }
	}
*/	

	@Override
	public void successGetAuthInfo() {
		super.successGetAuthInfo();
		
        DownloadSkinAsyncTask downloadSkinAsyncTask = new DownloadSkinAsyncTask(me);
        downloadSkinAsyncTask.isShowProgress = true;
        downloadSkinAsyncTask.execute(new ContentsDataDto(iconDto));

	}



	private void setAppForZavtoneIcon(){
		//LaunchIntentから取得
//		ComponentName component = setLaunchIntent.getComponent();
//		String packageName = component.getPackageName();
//		String className = component.getClassName();
//		
//		DebugLog.instance.outputLog("value", "パッケージ名_" + packageName + "/クラス名_" + className);
//		
		PackageManager pm = getPackageManager();
//		
//		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
//        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//        mainIntent.setComponent(component);

//        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
        List<ResolveInfo> apps = pm.queryIntentActivities(setLaunchIntent, 0);
        
		Drawable appIcon = null;

        if (apps.size() > 0) {
            for (ResolveInfo info : apps) {
                Resources resources;
                try {
                    resources = pm.getResourcesForApplication(info.activityInfo.applicationInfo);
                } catch (PackageManager.NameNotFoundException e) {
                    resources = null;
                }
                if (resources != null) {
                    int iconId = info.getIconResource();
                    if (iconId != 0) {
//                        return getFullResIcon(resources, iconId);
                    	appIcon = resources.getDrawable(iconId);
                    }
                }
                
                if(appIcon == null){
                	appIcon = Resources.getSystem().getDrawable(android.R.mipmap.sym_def_app_icon);
                }

                
            }
        }

		for(int j = 0; j < arrayAdapter.getCount(); j++){
			ShortcutIconListData data = arrayAdapter.getItem(j);

			//if　zavtone
			if(data.getSelfIconPath().indexOf("zavtone") != -1){
				//ザブトン系だったら画像合成
				Bitmap zavtoneBmp = getShortcutIconImage(data.getSelfIconPath());
				Bitmap appIconBaseBmp = null;

				try {
					appIconBaseBmp = ((BitmapDrawable) appIcon).getBitmap();
				}catch (ClassCastException e){
					int width = ((Drawable) appIcon).getIntrinsicWidth();
					int height = ((Drawable) appIcon).getIntrinsicHeight();
					appIconBaseBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
					Canvas canvas = new Canvas(appIconBaseBmp);
					((Drawable) appIcon).setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
					((Drawable) appIcon).draw(canvas);
				}

				//元サイズを96にする
		        int srcWidth = appIconBaseBmp.getWidth();
		        int srcHeight = appIconBaseBmp.getHeight();
		        int baseSize = (srcWidth >= srcHeight) ? srcWidth : srcHeight;
		        float x = (float)96 / (float)baseSize;
		        DebugLog.instance.outputLog("value", "縮小率_" + x);
	
				Matrix matrix = new Matrix();
				matrix.setScale(x, x);
				Bitmap appIconBmp = Bitmap.createBitmap(appIconBaseBmp, 0, 0, srcWidth, srcHeight, matrix, true);
				
				Bitmap newBitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(newBitmap);
				
				canvas.drawBitmap(zavtoneBmp, 0, 0, (Paint)null); // image, x座標, y座標, Paintインスタンス
				canvas.drawBitmap(appIconBmp, 24, 24, (Paint)null); // 画像合成
				
				data.setSelfIconData(newBitmap);
				
				zavtoneBmp.recycle();
				zavtoneBmp = null;
	
			}
		}
		
	}

    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

	private class CustomArrayAdapter2 extends ArrayAdapter<ShortcutIconListData> {
		private LayoutInflater layoutInflater_ = null;
		private LinearLayout.LayoutParams iconParam = null;

		public CustomArrayAdapter2(Context context, int resourceId, List<ShortcutIconListData> objects) {
			super(context, resourceId, objects);

			this.layoutInflater_ = LayoutInflater.from(context);
			this.iconParam = new LinearLayout.LayoutParams((int)(270*dispScaleperBase), (int)(248*dispScaleperBase));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DebugLog.instance.outputLog("value", "getView_" + position);
			
			// 特定の行(position)のデータを得る
			 final ShortcutIconListData item = (ShortcutIconListData)getItem(position);
			
			// convertViewは使い回しされている可能性があるのでnullの時だけ新しく作る
			 if (null == convertView) {
				 convertView = layoutInflater_.inflate(R.layout.icon_gridtype_item, null);
			 }
			 
			// CustomDataのデータをViewの各Widgetにセットする
			 ImageView iconButton;
//			 final int iconPos = position;
			 iconButton = (ImageView)convertView.findViewById(R.id.appicon_list_selficon_btn);
			 iconButton.setLayoutParams(iconParam);
			 iconButton.setScaleType(ScaleType.FIT_CENTER);
			 iconButton.setPadding((int)(((270 - 144)/2)*dispScaleperBase), (int)(((248 - 144)/2)*dispScaleperBase), (int)(((270 - 144)/2)*dispScaleperBase), (int)(((248 - 144)/2)*dispScaleperBase));

//			 iconButton.setOnClickListener(new OnClickListener() {
//				
//				@Override
//				public void onClick(View v) {
//					DebugLog.instance.outputLog("value", "グリッドタップ＿" + iconPos);
//					
//					targetPosition = iconPos;
//
//					//本体ダウンロードAsynTask
//	                DownloadSkinAsyncTask downloadSkinAsyncTask = new DownloadSkinAsyncTask(me);
//	                downloadSkinAsyncTask.isShowProgress = true;
//	                downloadSkinAsyncTask.execute(new ContentsDataDto(iconDto));
//	                
//				}
//			});


			 if(item.getSelfIconData() == null){
				 iconButton.setBackgroundResource(R.drawable.icon_gridtype_nonitem_bg);
				iconButton.setImageBitmap(null);
//				iconButton.setClickable(false);
			 }else{
				 iconButton.setBackgroundResource(R.drawable.icon_gridtype_item_bg);
				 iconButton.setImageBitmap(item.getSelfIconData());
//				 iconButton.setClickable(true);
			 }
			 
			 return convertView;
		}
		
	}

	@Override
	public void onFailedDownloadSkin(int reason, long assetId) {
		//ダイアログを出す
		showDialogFragment();
		
	}


	@Override
	public void onFinishedDownloadSkin(ContentsDataDto settedCto,
			long delAssetIdForHistory, long delAssetIdForMypage) {

		final String action;
		if (Build.VERSION.SDK_INT >= 26) {
			action = "jp.co.disney.apps.managed.kisekaeapp.action.INSTALL_SHORTCUT";
		} else {
			action = "com.android.launcher.action.INSTALL_SHORTCUT";
		}

		Intent i = new Intent(action);

        // 着せ替えアイコンを設定（仮）
        ShortcutIconListData data = arrayAdapter.getItem(targetPosition);
        
        int size = getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        int srcWidth = data.getSelfIconData().getWidth();
        int srcHeight = data.getSelfIconData().getHeight();
        int baseSize = (srcWidth >= srcHeight) ? srcWidth : srcHeight;
        float x = (float)size / (float)baseSize;
        
        if(Float.compare(x, 1.0f) != 0){
        	DebugLog.instance.outputLog("value", "拡大縮小");
    		Matrix matrix = new Matrix();
    		matrix.setScale(x, x);
    		i.putExtra(Intent.EXTRA_SHORTCUT_ICON, Bitmap.createBitmap(data.getSelfIconData(), 0, 0, srcWidth, srcHeight, matrix, true));
        }else{
        	DebugLog.instance.outputLog("value", "拡大縮小しない");
        	i.putExtra(Intent.EXTRA_SHORTCUT_ICON, data.getSelfIconData());
        }

        // 対象を着せ替えアプリのみに設定
        i.setComponent(new ComponentName("jp.co.disney.apps.managed.kisekaeapp",
                "jp.co.disney.apps.managed.kisekaeapp.launcher.InstallShortcutReceiver"));

        // 引き継ぎ情報設定
        i.putExtra(Intent.EXTRA_SHORTCUT_NAME, setAppName);
        i.putExtra(Intent.EXTRA_SHORTCUT_INTENT, setLaunchIntent);
        if (setShortcutName != null && !setShortcutName.equals("")) {
            i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_NAME_USER, setShortcutName);
        }
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_SCREEN, setScreen);
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_CELLX, setCellX);
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_CELLY, setCellY);
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_ID, setId);

        // ※既存のショートカットを削除するために必要です。
        i.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_OVERWRITE, true);

        // 送信
        IconSelectGridTypeActivity.this.sendBroadcast(i);
        
        //ホーム起動
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setPackage(me.getApplicationContext().getPackageName());

        me.getApplicationContext().startActivity(intent);

		me.finish();

		
	}
}
