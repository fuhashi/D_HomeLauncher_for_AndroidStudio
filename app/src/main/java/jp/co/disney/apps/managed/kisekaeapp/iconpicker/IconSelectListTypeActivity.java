package jp.co.disney.apps.managed.kisekaeapp.iconpicker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinTaskCallback;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForCatalog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.comparator.ShortcutIconThumbsComparator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.database.MyPageDataDto;
import jp.co.disney.apps.managed.kisekaeapp.launcher.InstallShortcutReceiver;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

public class IconSelectListTypeActivity extends IconSelectActivity implements DownloadSkinTaskCallback {

	private Activity me;

	private CustomArrayAdapter2 arrayAdapter = null;
	private ListView listView = null;
	private ImageButton setappBtn = null;
	private SizeChangeableCheckBox allCheckBox = null;
	private boolean isUnenabledPosition = true;
	private boolean isFromCatalog = true;
	
    private Dialog mAppPickerDialog = null;
	
	static final String FINISH_BROADCAST_ACTION = "jp.co.disney.apps.managed.kisekaeapp.iconpicker.finish";

	/**
	 * 座標計算用の補正値
	 */
	float dispScaleperBase = 1f;//FHD(1080x1920)と比較しての倍率
//	private float fontP = 1.0f;

	private MyPageDataDto iconDto = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.icon_listtype_activity_main);

		me = this;
		
		int viewWidth = this.getResources().getDisplayMetrics().widthPixels;
		dispScaleperBase = (float)viewWidth/1080f;
//		float dens = getResources().getDisplayMetrics().density;
//		fontP=viewWidth/(dens*320.0f);

		final Intent getIntent = getIntent();
		isFromCatalog = getIntent.getBooleanExtra(ContentsOperatorForCatalog.INTENT_EXTRA_ISFROMCATALOG, false);
		iconDto = (MyPageDataDto)getIntent.getSerializableExtra(KISEKAE_SAKI_SHORTCUTICON_DTO);
		
		//-------------------各画面要素------------------------
		LinearLayout headerLayout = (LinearLayout) findViewById(R.id.setapp_header);
		LinearLayout.LayoutParams headerParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(210*dispScaleperBase));
		headerLayout.setLayoutParams(headerParam);
		
		ImageView titleView = (ImageView) findViewById(R.id.setapp_list_title);
		LinearLayout.LayoutParams titleParam = new LinearLayout.LayoutParams((int)(261*dispScaleperBase), (int)(47*dispScaleperBase));
		titleParam.setMargins((int)(169*dispScaleperBase), (int)(93*dispScaleperBase), 0, 0);
		titleView.setLayoutParams(titleParam);
		
		ImageView captionView = (ImageView) findViewById(R.id.setapp_list_all_checkbox_caption);
		LinearLayout.LayoutParams captionParam = new LinearLayout.LayoutParams((int)(206*dispScaleperBase), (int)(27*dispScaleperBase));
		captionParam.setMargins((int)(37*dispScaleperBase), (int)(56*dispScaleperBase), 0, 0);
		captionView.setLayoutParams(captionParam);
		
		listView = (ListView) findViewById(R.id.icon_list);
		LinearLayout.LayoutParams listviewParam = new LinearLayout.LayoutParams((int)(1080*dispScaleperBase), LinearLayout.LayoutParams.MATCH_PARENT);
		listviewParam.weight = 1;
		listView.setLayoutParams(listviewParam);
		
		LinearLayout bottomLayout = (LinearLayout) findViewById(R.id.setapp_bottom);
//		LinearLayout.LayoutParams bottomParam = new LinearLayout.LayoutParams((int)(1080*dispScaleperBase), (int)(116*dispScaleperBase));
		LinearLayout.LayoutParams bottomParam = new LinearLayout.LayoutParams((int)(1080*dispScaleperBase), (int)(174*dispScaleperBase));
		bottomLayout.setLayoutParams(bottomParam);
		
		allCheckBox = (SizeChangeableCheckBox) findViewById(R.id.setapp_list_all_checkbox);
		allCheckBox.setChecked(false);
		allCheckBox.setEnabled(false);
		LinearLayout.LayoutParams allCheckboxParam = new LinearLayout.LayoutParams((int)(72*dispScaleperBase), (int)(72*dispScaleperBase));
		allCheckboxParam.setMargins((int)(104*dispScaleperBase), (int)(14*dispScaleperBase), 0, 0);
		allCheckBox.setLayoutParams(allCheckboxParam);
		
		//チェックした際は
//		allCheckBox.setOnCheckedChangeListener(new SizeChangeableCheckBox.OnCheckedChangeListener() {
//			
//			@Override
//			public void onCheckedChanged(ImageButton buttonView, boolean isChecked) {
//				//アプリが設定されている行に対してチェックを入れたり外したり
//				//ここで1つでもチェックを入れたなら設定ボタンが使用可能、1つもチェックを入れなかったら設定ボタンは使用不可
//				boolean checkedExist = false;
//				for(int i = 0; i < arrayAdapter.getCount(); i++){
//					CustomListDataNew data = arrayAdapter.getItem(i);
//					if(!data.getAppPackage().equals("") && data.getSelfIconData() != null){
//						data.setChecked(isChecked);
//						if(isChecked) checkedExist = true;
//					}
//				}
//				if(!isChecked){
//					setappBtn.setEnabled(false);
//				}else{
//					if(checkedExist){
//						setappBtn.setEnabled(true);
//					}else{
//						setappBtn.setEnabled(false);
//					}
//				}
//				
//				listView.invalidateViews();
//			}
//		});

		allCheckBox.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean isChecked = allCheckBox.isChecked();
				//アプリが設定されている行に対してチェックを入れたり外したり
				//ここで1つでもチェックを入れたなら設定ボタンが使用可能、1つもチェックを入れなかったら設定ボタンは使用不可
				boolean checkedExist = false;
				for(int i = 0; i < arrayAdapter.getCount(); i++){
					ShortcutIconListData data = arrayAdapter.getItem(i);
					if(!data.getAppPackage().equals("") && data.getSelfIconData() != null){
						data.setChecked(isChecked);
						if(isChecked) checkedExist = true;
					}
				}
				if(!isChecked){
					setappBtn.setEnabled(false);
				}else{
					if(checkedExist){
						setappBtn.setEnabled(true);
					}else{
						setappBtn.setEnabled(false);
					}
				}
				
				listView.invalidateViews();
			}
		});
		

		//キャッシュディレクトリからファイル名を取得して使用
		File[] iconFile = FileUtility.getFilesInDirectory(me.getCacheDir().getAbsolutePath() + File.separator + iconDto.assetID + File.separator);
//		iconNum = FileUtility.countFilesInDirectory(me.getCacheDir().getAbsolutePath() + File.separator + assetID + File.separator);
		int iconNum = iconFile.length;

		//初期表示分のitemを作成
		List<ShortcutIconListData> objects = new ArrayList<ShortcutIconListData>();
        for(int i = 0; i < iconNum; i++){
            ShortcutIconListData item1 = new ShortcutIconListData();
            
            //アプリアイコンはnull
            item1.setAppIconData(null);
            item1.setAppPackage("");
            item1.setAppName("");
            item1.setAppClassName("");
            item1.setSelfIconData(getShortcutIconImage(iconFile[i].getAbsolutePath()));
            item1.setSelfIconPath(iconFile[i].getAbsolutePath());
            objects.add(item1);
        }
        
        //紐付けるアプリがあったらアプリアイコン等を紐付け
        setDefaultApp(objects);
        
        //ソート
        Collections.sort(objects, new ShortcutIconThumbsComparator());

        iconFile = null;

		arrayAdapter = new CustomArrayAdapter2(getApplicationContext(), R.layout.icon_listtype_list_row, objects);

		listView.setAdapter(arrayAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				DebugLog.instance.outputLog("value", "リストタップ");
				
				if(!isUnenabledPosition){
					return;
				}

				targetPosition = position;
				
				setBanDoubleTap();
				
				//アプリ一覧を取得
				PackageManager pm = getPackageManager();
				 
				Intent mIntent = new Intent(Intent.ACTION_MAIN,null);
				mIntent.addCategory(Intent.CATEGORY_LAUNCHER); 
                ArrayList<AppData> itemList = new ArrayList<AppData>();

                List<ResolveInfo> appLists = pm.queryIntentActivities(mIntent,0);
				for(ResolveInfo info : appLists){
					 
					AppData item = new AppData();
	                try {
	                    Resources resources = null;
	                    resources = pm.getResourcesForApplication(info.activityInfo.applicationInfo);
	                    if (resources != null) {
	                        int iconId = info.getIconResource();
	                        if (iconId != 0) {
//	                            return getFullResIcon(resources, iconId);
	                        	item.setAppIconImage(resources.getDrawable(iconId));
	                        }
	                        
	                        if(item.getAppIconImage() == null){
	                        	item.setAppIconImage(Resources.getSystem().getDrawable(android.R.mipmap.sym_def_app_icon));
	                        }
	                    }

		                item.setAppName(info.loadLabel(pm).toString());
		                item.setAppPackageName(info.activityInfo.packageName);
		                item.setAppClassName(info.activityInfo.name);
		                itemList.add(item);
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
				}

				AppSelectDialogCustomAdapter adapter = new AppSelectDialogCustomAdapter(me, 0, itemList);
				
				ListView listView2 = new ListView(me);
				listView2.setAdapter(adapter);
				listView2.setOnItemClickListener(new OnItemClickListener() {
				    public void onItemClick(AdapterView<?> items,
				            View view, int position, long id) {

				        if (mAppPickerDialog != null) {
				            mAppPickerDialog.dismiss();
				            mAppPickerDialog = null;
				        }

		                AppData aData = (AppData) items.getItemAtPosition(position);
				        setAppInfoAtShortcutIcon(aData.getAppIconImage(), aData.getAppPackageName(), aData.getAppName(), aData.getAppClassName());
				    }
				});

				AlertDialog.Builder listDlg = new AlertDialog.Builder(new ContextThemeWrapper(me, R.style.MyDialogTheme));
				TextView title =  new TextView(me);
				title.setText(R.string.title_appli_picker);
				title.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER);
				title.setTextSize(22);
				title.setBackgroundColor(Color.argb(0, 0,0, 0));
				title.setTextColor(Color.WHITE);
				DisplayMetrics metrics = getResources().getDisplayMetrics();
				title.setPadding((int) (metrics.density * 16), (int) (metrics.density * 18), (int) (metrics.density * 16), (int) (metrics.density * 14));
				listDlg.setCustomTitle(title);
				listDlg.setNeutralButton(R.string.title_appli_picker_close, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
				        if (mAppPickerDialog != null) {
				        	mAppPickerDialog.dismiss();
				        	mAppPickerDialog = null;
				        }						
					}
				});

				listDlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
				    @Override
				    public void onCancel(DialogInterface dialog) {
				        if (mAppPickerDialog != null) {
				        	mAppPickerDialog.dismiss();
				        	mAppPickerDialog = null;
				        }
				    }
				});

				mAppPickerDialog = listDlg.setView(listView2).create();
				mAppPickerDialog.setOnShowListener(new DialogInterface.OnShowListener() {
					
					@Override
					public void onShow(DialogInterface dialog) {
						Button btnNeutral = ((AlertDialog)dialog).getButton(
						DialogInterface.BUTTON_NEUTRAL);
						if (btnNeutral != null) {
							btnNeutral.setTextSize(22);
							btnNeutral.setGravity(Gravity.CENTER);
						}
					}
				});
				
				mAppPickerDialog.show();
				int dividerId = mAppPickerDialog.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
				View divider = mAppPickerDialog.findViewById(dividerId);
				divider.setBackgroundColor(Color.argb(0, 0, 0, 0));

			}
		});
		
		
		setappBtn = (ImageButton) findViewById(R.id.setapp_do_btn);
		LinearLayout.LayoutParams setappBtnParam = new LinearLayout.LayoutParams((int)(396*dispScaleperBase), (int)(84*dispScaleperBase));
//		setappBtnParam.setMargins((int)(144*dispScaleperBase), (int)(16*dispScaleperBase), 0, 0);
		setappBtnParam.setMargins((int)(144*dispScaleperBase), (int)(43*dispScaleperBase), 0, 0);
		setappBtn.setLayoutParams(setappBtnParam);
		
		setappBtn.setEnabled(false);
		setappBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                if(iconDto == null){
                	showDialogFragment();
                	return;
                }
                
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
		
		ImageButton cancelBtn = (ImageButton) findViewById(R.id.setapp_cancel_btn);//戻るもしくは起動する
		LinearLayout.LayoutParams cancelBtnParam = new LinearLayout.LayoutParams((int)(396*dispScaleperBase), (int)(84*dispScaleperBase));
//		cancelBtnParam.setMargins((int)(72*dispScaleperBase), (int)(16*dispScaleperBase), 0, 0);
		cancelBtnParam.setMargins((int)(72*dispScaleperBase), (int)(43*dispScaleperBase), 0, 0);
		cancelBtn.setLayoutParams(cancelBtnParam);
		cancelBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				me.finish();
				
				//カタログから来たらホーム起動、別アプリとして起動のピッカーから来たら終了だけでよい。
				if(isFromCatalog){
//				if(getIntent.getBooleanExtra(ContentsOperatorForCatalog.INTENT_EXTRA_ISFROMCATALOG, false)){
			        Intent intent = new Intent();
			        intent.setAction(Intent.ACTION_MAIN);
			        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			        
			        intent.setClassName(me.getApplicationContext().getPackageName(), me.getApplicationContext().getPackageName() + ".SplashActivity");

			        me.getApplicationContext().startActivity(intent);
				}
			}
		});
		

		
        //allチェックボックス
		boolean checkedExist = false;
		//アプリ設定済みのアイテムに関してデフォルトCHECKONは行なわない仕様なのでコメントアウト
		for(int i = 0; i < arrayAdapter.getCount(); i++){
			ShortcutIconListData data = arrayAdapter.getItem(i);
			if(!data.getAppPackage().equals("") && data.getSelfIconData() != null){
//				data.setChecked(true);
				checkedExist = true;
			}
		}
//		setappBtn.setEnabled(checkedExist);
//		allCheckBox.setChecked(checkedExist);
		if(checkedExist) allCheckBox.setEnabled(true);


	}
	
	
	
	@Override
	public void successGetAuthInfo() {
		super.successGetAuthInfo();
		
        //本体ダウンロードAsynTask
        DownloadSkinAsyncTask downloadSkinAsyncTask = new DownloadSkinAsyncTask(me);
        downloadSkinAsyncTask.isShowProgress = true;
        downloadSkinAsyncTask.execute(new ContentsDataDto(iconDto));

	}



	private final HashMap<String, String> defKisekaeMap = new HashMap<String, String>();
    public void initKisekaeIconMap() {

        InputStream in = null;
        JsonReader reader = null;
        try {
            in = getApplicationContext().getAssets().open("ic_map.json");
            reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
            reader.beginObject();
            while (reader.hasNext()) {
                String pkgName = reader.nextName();
                String icResName = reader.nextString();
                defKisekaeMap.put(pkgName, icResName);
            }

        } catch (IOException e) {

        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }
	
	private void setDefaultApp(List<ShortcutIconListData> iconList){
		
		initKisekaeIconMap();
        
        PackageManager pm = getPackageManager();
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);

        for(ResolveInfo info : apps){
            String appName = info.activityInfo.packageName + "/" + info.activityInfo.name;
            String kisekaeIconName = defKisekaeMap.get(appName);
            
            //アイコン該当のアプリがあったら
            if (kisekaeIconName != null) {
                DebugLog.instance.outputLog("value", "チェック中: " + appName);
                DebugLog.instance.outputLog("value", "チェック中:ファイル名: " + kisekaeIconName);

            	//すでに所持しているアイコンの中に重複するパッケージ名／クラス名のものがあったら優先順位チェック
                ShortcutIconListData chofuku = null;
                for(ShortcutIconListData newIconData : iconList){
                	if(newIconData.getSelfIconPath().indexOf(kisekaeIconName) != -1){
                		if(!newIconData.getAppPackage().equals("")){
                    		DebugLog.instance.outputLog("value", "重複した_" + kisekaeIconName);
                    		chofuku = newIconData;
                		}
                	}
                }
                
                if (chofuku != null) {

                    DebugLog.instance.outputLog("value", "着せ替え重複[1]: " + chofuku.getAppName() + "[" + chofuku.getAppName() + "], " + kisekaeIconName);
                    DebugLog.instance.outputLog("value", "着せ替え重複[2]: " + info.loadLabel(pm).toString() + "[" + appName + "], " + kisekaeIconName);

                    boolean replace = false;

                    // 優先度チェック
                    if (kisekaeIconName.equals("ic_gallery")) {

                        final String[] titles = { "ギャラリー", "アルバム", "フォト" };
                        final int count = titles.length;
                        String appTitle = info.loadLabel(pm).toString();
                        DebugLog.instance.outputLog("value", "アプリ名: " + appTitle);

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppName().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }


                    } else if (kisekaeIconName.equals("ic_contacts")) {

                        final String[] titles = { "連絡先", "電話帳" };
                        final int count = titles.length;
                        String appTitle = info.loadLabel(pm).toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppName().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }

                    } else if (kisekaeIconName.equals("ic_music")) {

                        final String[] titles = { "ミュージック", "ウォークマン", "メディアプレイヤー" };
                        final int count = titles.length;
                        String appTitle = info.loadLabel(pm).toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppName().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }

                    } else if (kisekaeIconName.equals("ic_email")) {

                        final String[] titles = { "PCメール", "Eメール" };
                        final int count = titles.length;
                        String appTitle = info.loadLabel(pm).toString();

                        int newItem;
                        for (newItem = 0; newItem < count; newItem++) {
                            if (appTitle.equals(titles[newItem])) {
                                break;
                            }
                        }

                        int oldItem;
                        for (oldItem = 0; oldItem < count; oldItem++) {
                            if (chofuku.getAppName().equals(titles[oldItem])) {
                                break;
                            }
                        }

                        if (newItem < oldItem) {
                            replace = true;
                        }
                    }

                    if (replace) {
                    	
                    	//入れ替えが必要だったら該当リストの該当アイコン名のもののアプリデータを入れ替える
                    	for(ShortcutIconListData newIconData : iconList){
                    		//同じアイコン名をもつものを…
                    		if(newIconData.getSelfIconPath().indexOf(kisekaeIconName) != -1){
                    			//アプリデータ入れ替え
                    			newIconData.setAppName(info.loadLabel(pm).toString());
                    			newIconData.setAppPackage(info.activityInfo.packageName);
                    			newIconData.setAppClassName(info.activityInfo.name);
                    			
        	                    Resources resources = null;
        	                    try {
									resources = pm.getResourcesForApplication(info.activityInfo.applicationInfo);
	        	                    if (resources != null) {
	        	                        int iconId = info.getIconResource();
	        	                        if (iconId != 0) {
//	        	                            return getFullResIcon(resources, iconId);//((BitmapDrawable) appIcon).getBitmap()
											try {
												newIconData.setAppIconData(((BitmapDrawable) resources.getDrawable(iconId)).getBitmap());
											}catch (ClassCastException e){

												Drawable icon = resources.getDrawable(iconId);

												int width = icon.getIntrinsicWidth();
												int height = icon.getIntrinsicHeight();
												Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

												Canvas canvas = new Canvas(bmp);
												icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
												icon.draw(canvas);

												newIconData.setAppIconData(bmp);
											}
	        	                        }
	        	                        
	        	                        if(newIconData.getAppIconData() == null){
	        	                        	newIconData.setAppIconData(((BitmapDrawable) Resources.getSystem().getDrawable(android.R.mipmap.sym_def_app_icon)).getBitmap());
	        	                        }
	        	                    }
								} catch (NameNotFoundException e) {
									e.printStackTrace();
								}

                    		}
                    	}
 
                    }

                } else {
                	
                	DebugLog.instance.outputLog("value", "重複してない_" + kisekaeIconName);
                	
                	for(ShortcutIconListData newIconData : iconList){
                		if(newIconData.getSelfIconPath().indexOf(kisekaeIconName) != -1){
                			if(!(kisekaeIconName.equals("ic_gallery") && info.loadLabel(pm).toString().equals("メッセンジャー"))){
                       			newIconData.setAppName(info.loadLabel(pm).toString());
                    			newIconData.setAppPackage(info.activityInfo.packageName);
                    			newIconData.setAppClassName(info.activityInfo.name);
                    			
        	                    Resources resources = null;
        	                    try {
    								resources = pm.getResourcesForApplication(info.activityInfo.applicationInfo);
            	                    if (resources != null) {
            	                        int iconId = info.getIconResource();
            	                        if (iconId != 0) {
//            	                            return getFullResIcon(resources, iconId);//((BitmapDrawable) appIcon).getBitmap()

											try {
												newIconData.setAppIconData(((BitmapDrawable) resources.getDrawable(iconId)).getBitmap());
//												appIconBaseBmp = ((BitmapDrawable) appIcon).getBitmap();
											}catch (ClassCastException e){

												Drawable icon = resources.getDrawable(iconId);

												int width = icon.getIntrinsicWidth();
												int height = icon.getIntrinsicHeight();
												Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

												Canvas canvas = new Canvas(bmp);
												icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
												icon.draw(canvas);

												newIconData.setAppIconData(bmp);
											}

										}
            	                        
            	                        if(newIconData.getAppIconData() == null){
            	                        	newIconData.setAppIconData(((BitmapDrawable) Resources.getSystem().getDrawable(android.R.mipmap.sym_def_app_icon)).getBitmap());
            	                        }
            	                    }
    							} catch (NameNotFoundException e) {
    								e.printStackTrace();
    							}
     	                    
                			}
                		}
                	}


                }
            }
        }

	}
	
	private void setBanDoubleTap(){
		//一定時間触れない
        isUnenabledPosition = false;
        listView.setClickable(false);
        listView.setEnabled(false);
        new Handler().postDelayed(new Runnable() {
            public void run() {
            	DebugLog.instance.outputLog("value", "リストタップ規制解除");
                isUnenabledPosition = true;
                listView.setClickable(true);
                listView.setEnabled(true);
            }
        }, 1500L);
	}
	
	private void createShortcut(Bitmap icon, String packageName, String appName, String className, boolean isEnd){
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        
        ComponentName name=new ComponentName(packageName, className);

//        shortcutIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        shortcutIntent.setComponent(name);
        
        // ショートカットをHOMEに作成する
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, appName);
        
        int size = getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        int srcWidth = icon.getWidth();
        int srcHeight = icon.getHeight();
        int baseSize = (srcWidth >= srcHeight) ? srcWidth : srcHeight;
        float x = (float)size / (float)baseSize;
        
        if(Float.compare(x, 1.0f) != 0){
        	DebugLog.instance.outputLog("value", "拡大縮小");
    		Matrix matrix = new Matrix();
    		matrix.setScale(x, x);
			icon = Bitmap.createBitmap(icon, 0, 0, srcWidth, srcHeight, matrix, true);
    		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        }else{
        	DebugLog.instance.outputLog("value", "拡大縮小しない");
        	intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        }
		
        if (!isEnd) {
            intent.putExtra(InstallShortcutReceiver.EXTRA_SHORTCUT_NEXT_FLAG, true);
        }

		if (Build.VERSION.SDK_INT >= 26) {
			intent.setAction("jp.co.disney.apps.managed.kisekaeapp.action.INSTALL_SHORTCUT");
			sendBroadcast(intent);

			ShortcutManager mShortcutManager = getSystemService(ShortcutManager.class);

			if (mShortcutManager.isRequestPinShortcutSupported()) {

				String id = "jp.co.disney.apps.managed.kisekaeapp-" + UUID.randomUUID().toString().replaceAll("-", "");

				ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, id)
						.setShortLabel(appName)
						.setLongLabel(appName)
						.setIcon(Icon.createWithBitmap(icon))
						.setIntent(shortcutIntent)
						.build();

				List<ShortcutInfo> shortcutInfoList = new ArrayList<>();
				shortcutInfoList.add(shortcutInfo);
				mShortcutManager.requestPinShortcut(shortcutInfo, null);
			}

		} else {
			intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			sendBroadcast(intent);
		}
	}

	private void setAppInfoAtShortcutIcon(Drawable appIcon, String packageName, String appName, String className){
		DebugLog.instance.outputLog("value", "タップしたアプリのパッケージ名_" + packageName);
		DebugLog.instance.outputLog("value", "タップしたアプリのアプリ名_" + appName);
		DebugLog.instance.outputLog("value", "タップしたアプリのクラス名_" + className);
		
		
		//該当listview.Rowの操作
		//アプリアイコンを該当列に表示・チェックボックスにチェックを入れる
		ShortcutIconListData data = (ShortcutIconListData) listView.getItemAtPosition(targetPosition);
		
		boolean newSet = true;
		if(!data.getAppPackage().equals("")) newSet = false;

		data.setChecked(true);

		try {
			data.setAppIconData(((BitmapDrawable) appIcon).getBitmap());
		}catch (ClassCastException e){
			int width = ((Drawable) appIcon).getIntrinsicWidth();
			int height = ((Drawable) appIcon).getIntrinsicHeight();
			Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bmp);
			((Drawable) appIcon).setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			((Drawable) appIcon).draw(canvas);

			data.setAppIconData(bmp);
		}

		data.setAppPackage(packageName);
		data.setAppName(appName);
		data.setAppClassName(className);
//		View targetView = listView.getChildAt(targetPosition);
		
		//if　zavtone
		if(data.getSelfIconPath().indexOf("zavtone") != -1){
			//ザブトン系かつ新規アプリ登録だったら画像合成してかつ行を1つ増やす（=元々登録している状態の行を再登録しても行は増やさない）
			
			if(newSet){
				ShortcutIconListData addData = new ShortcutIconListData();
				addData.setChecked(false);
				addData.setSelfIconData(data.getSelfIconData());
				addData.setSelfIconPath(data.getSelfIconPath());
				arrayAdapter.insert(addData, targetPosition + 1);				
			}
			
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

//		listView.getAdapter().getView(targetPosition, targetView, listView);
		listView.invalidateViews();
		
		setappBtn.setEnabled(true);
		allCheckBox.setEnabled(true);
		
    	//アプリがあるものすべてにチェックが入ったら
    	boolean isAllCheck = true;
    	for(int i = 0; i < arrayAdapter.getCount(); i++){
    		ShortcutIconListData checkData = arrayAdapter.getItem(i);
    		if(!checkData.getAppPackage().equals("") && !checkData.getChecked()) isAllCheck = false;
    	}
    	if(isAllCheck) allCheckBox.setChecked(true);

		
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
	
    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////
	

    public static class AppSelectDialogCustomAdapter extends ArrayAdapter<AppData> {
        private LayoutInflater inflater;

        public AppSelectDialogCustomAdapter(Context context, int resource, List<AppData> objects) {
            super(context, resource, objects);
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            AppData item = (AppData) getItem(position);
            if (null == v)
                v = inflater.inflate(R.layout.widget_disney_picker_row, null);
            ImageView imageView;
            imageView = (ImageView) v.findViewById(R.id.row_imgview1);
            imageView.setImageDrawable(item.getAppIconImage());

            TextView stringTextView = (TextView) v
                    .findViewById(R.id.row_textview1);
            stringTextView.setText(item.getAppName());

            return v;
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////

	@Override
	protected void onPause() {
		super.onPause();
		
	    if (mAppPickerDialog != null) {
	    	mAppPickerDialog.dismiss();
	    	mAppPickerDialog = null;
	    }
	}
	
	int targetPosition = -1;
	private class CustomArrayAdapter2 extends ArrayAdapter<ShortcutIconListData> {
		private LayoutInflater layoutInflater_ = null;

		//ListDataを保持しておく
		private List<ShortcutIconListData> items_;
		private AbsListView.LayoutParams baseRowParam = null;
		private LinearLayout.LayoutParams checkboxParam = null, arrowParam = null, appIconParam = null, iconParam = null, appCaptionParam = null;
		
		private int rowAreaHeight = 184, 
				checkboxAreaWidth = 270, checkboxImageWidth = 72, checkboxImageHeight = 72,
				checkBoxPositionX = 104, checkBoxPositionY = 56,
				iconAreaWidth = 200, iconImageWidth = 144, iconImageHeight = 144,
				iconPositionX = 26, iconPositionY = 20,
				arrowAreaWidth = 175, arrowImageWidth = 58, arrowImageHeight = 33,
				arrowPositionX = 74, arrowPositionY = 79;

		public CustomArrayAdapter2(Context context, int resourceId, List<ShortcutIconListData> objects) {
			super(context, resourceId, objects);
//			layoutInflater_ = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.layoutInflater_ = LayoutInflater.from(context);
			this.items_ = objects;
			this.baseRowParam = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, (int)(184*dispScaleperBase));
			
			this.checkboxParam = new LinearLayout.LayoutParams((int)(checkboxAreaWidth*dispScaleperBase), (int)(rowAreaHeight*dispScaleperBase));
			
			this.arrowParam = new LinearLayout.LayoutParams((int)(arrowAreaWidth*dispScaleperBase), (int)(rowAreaHeight*dispScaleperBase));
			
			this.appIconParam = new LinearLayout.LayoutParams((int)(144*dispScaleperBase), (int)(144*dispScaleperBase));
			appIconParam.setMargins((int)(71*dispScaleperBase), (int)(20*dispScaleperBase), 0, 0);
			this.appCaptionParam = new LinearLayout.LayoutParams((int)(239*dispScaleperBase), (int)(94*dispScaleperBase));
			appCaptionParam.setMargins((int)(74*dispScaleperBase), (int)(47*dispScaleperBase), 0, 0);
			
			this.iconParam = new LinearLayout.LayoutParams((int)(iconAreaWidth*dispScaleperBase), (int)(rowAreaHeight*dispScaleperBase));

		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DebugLog.instance.outputLog("value", "getView_" + position);
			
			// 特定の行(position)のデータを得る
			 final ShortcutIconListData item = (ShortcutIconListData)getItem(position);
			
			// convertViewは使い回しされている可能性があるのでnullの時だけ新しく作る
			 if (null == convertView) {
				 convertView = layoutInflater_.inflate(R.layout.icon_listtype_list_row, null);
			 }
			 
			 LinearLayout baseRowLayout = (LinearLayout) convertView.findViewById(R.id.appicon_list_row_base);
			 baseRowLayout.setLayoutParams(baseRowParam);
			 baseRowLayout.setBackgroundResource(R.drawable.icon_listtype_row_bg);
			 
			// CustomDataのデータをViewの各Widgetにセットする
			 View.OnTouchListener notTouchableListener = new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return true;
				}
			};
			 
			 ImageView iconView, appIconView, arrowView;
			 iconView = (ImageView)convertView.findViewById(R.id.appicon_list_selficon);
			 iconView.setOnTouchListener(notTouchableListener);
			 appIconView = (ImageView)convertView.findViewById(R.id.appicon_list_appicon);
			 arrowView = (ImageView) convertView.findViewById(R.id.appicon_list_arrow);
			 arrowView.setOnTouchListener(notTouchableListener);
			 arrowView.setPadding((int)(arrowPositionX*dispScaleperBase), (int)(arrowPositionY*dispScaleperBase),
					 (int)((arrowAreaWidth - arrowPositionX - arrowImageWidth)*dispScaleperBase),
					 (int)((rowAreaHeight - arrowPositionY - arrowImageHeight)*dispScaleperBase));

			 iconView.setTag(position);
			 appIconView.setTag(position);
			 arrowView.setTag(position);
			 arrowView.setLayoutParams(arrowParam);
			 
			 if(item.getSelfIconData() == null){
				 iconView.setImageBitmap(null);

			 }else{
				 iconView.setImageBitmap(item.getSelfIconData());
				 iconView.setLayoutParams(iconParam);
				 iconView.setPadding((int)(iconPositionX*dispScaleperBase), (int)(iconPositionY*dispScaleperBase),
						 (int)((iconAreaWidth - iconPositionX - iconImageWidth)*dispScaleperBase),
						 (int)((rowAreaHeight - iconPositionY - iconImageHeight)*dispScaleperBase));
			 }
			 
			 if(item.getAppIconData() == null){
				 appIconView.setImageResource(R.drawable.icon_listtype_caption_text);
				 appIconView.setLayoutParams(appCaptionParam);
			 }else{
				 appIconView.setImageBitmap(item.getAppIconData());
				 appIconView.setLayoutParams(appIconParam);
			 }

			 final SizeChangeableCheckBox cBox = (SizeChangeableCheckBox) convertView.findViewById(R.id.appicon_list_checkbox);
			 cBox.setChecked(item.getChecked());
			 cBox.setFocusable(false);
			 cBox.setFocusableInTouchMode(false);
			 cBox.setLayoutParams(checkboxParam);
			 cBox.setPadding((int)(checkBoxPositionX*dispScaleperBase), (int)(checkBoxPositionY*dispScaleperBase),
					 (int)((checkboxAreaWidth - checkBoxPositionX - checkboxImageWidth)*dispScaleperBase),
					 (int)((rowAreaHeight - checkBoxPositionY - checkboxImageHeight)*dispScaleperBase));

			 if(item.getAppPackage().equals("")){
				 cBox.setEnabled(false);
				 cBox.setClickable(false);
			 }else{
				 cBox.setEnabled(true);
				 cBox.setClickable(true);
			 }
			 cBox.setOnClickListener(new SizeChangeableCheckBox.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
		            item.setChecked(((SizeChangeableCheckBox) v).isChecked());
		            DebugLog.instance.outputLog("value", "checkBox_" + ((SizeChangeableCheckBox) v).isChecked());
		            if(item.getChecked()){
		            	setappBtn.setEnabled(true);
		            	allCheckBox.setEnabled(true);
		            	
		            	//アプリがあるものすべてにチェックが入ったら
		            	boolean isAllCheck = true;
		            	for(int i = 0; i < items_.size(); i++){
		            		ShortcutIconListData data = items_.get(i);
		            		if(!data.getAppPackage().equals("") && !data.getChecked()) isAllCheck = false;
		            	}
		            	if(isAllCheck) allCheckBox.setChecked(true);
		            	
		            }else{
		            	//一個でも外したらallcheckBoxのチェックを外す
		            	allCheckBox.setChecked(false);
		            	
		            	boolean checkedExist = false;
		            	for(int i = 0; i < items_.size(); i++){
		            		ShortcutIconListData data = items_.get(i);
		            		if(data.getChecked()) checkedExist = true;
		            	}
		            	DebugLog.instance.outputLog("value", "checkedExist_" + checkedExist);
		            	if(checkedExist){
		            		setappBtn.setEnabled(true);
		            		allCheckBox.setEnabled(true);
		            	}else{
		            		setappBtn.setEnabled(false);
		            		setappBtn.setEnabled(false);
		            	}
		            }
				}
			});


//			 TextView textView;
//			 textView = (TextView)convertView.findViewById(R.id.text);
//			 textView.setText(item.getTextData());
			 
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
		
		 ArrayList<ShortcutIconListData> list = new ArrayList<ShortcutIconListData>();
		
		//ArrayAdapterの中身チェックして必要なものでintent発行
		for(int i = 0; i < arrayAdapter.getCount(); i++){
			ShortcutIconListData data = arrayAdapter.getItem(i);
			if(data.getChecked()){
				if(!data.getAppPackage().equals("") && data.getSelfIconData() != null){
					list.add(data);
//					createShortcut(data.getSelfIconData(), data.getAppPackage(), data.getAppName(), data.getAppClassName());
				}
			}
		}
		
        int len = list.size();
        for (int i = 0; i < len; i++) {
            ShortcutIconListData data = list.get(i);
            boolean isEnd = (i == len - 1);
            createShortcut(data.getSelfIconData(), data.getAppPackage(), data.getAppName(), data.getAppClassName(), isEnd);
        }
		
		me.finish();
		
		//カタログから来たらホーム起動、別アプリとして起動のピッカーから来たら終了だけでよい。
		if(isFromCatalog){
	        Intent intent = new Intent();
	        intent.setAction(Intent.ACTION_MAIN);
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

	        intent.addCategory(Intent.CATEGORY_HOME);
	        intent.setPackage(me.getApplicationContext().getPackageName());

	        me.getApplicationContext().startActivity(intent);

		}else{
			Intent finishIntent = new Intent(FINISH_BROADCAST_ACTION);
			sendBroadcast(finishIntent);
		}
	}
}
