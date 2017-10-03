package jp.co.disney.apps.managed.kisekaeapp.catalog.screens;

import java.io.File;
import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.assets.loaders.resolvers.LocalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.SplashActivity;
import jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask.DownloadSkinAsyncTask;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsCharaValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForCatalog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.FileUtility;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;

public class  CatalogScreen extends ApplicationAdapter{

	private SplashActivity myActivity;

	private SpriteBatch batch;
	private OrthographicCamera camera;
	private Vector2 camPos;
	public AssetManager assets;
	// 端末の解像度
	private int viewWidth = 1080;
	private int viewHeight = 1920;
	// uiPerで計算したステージの解像度で計算した解像度
	private float realWidth = 1080;
	private float realHeight = 1920;
	private float uiPer = 1.0f;
	int saveScreenSize = 0;
//	float asp = 1f;
//	float reAsp = 100f;
	boolean setScreenSize = false;
	float topY = -1701;
	float centerY = 850;
	final float ONE = 1f;
	Vector3 touchPos = new Vector3();
	private boolean LoadingAppData = false;//起動に必要な情報取得完了
	private  Array<ContentsDataDto> cto = null;
	private  Array<Boolean> ctoIsNew = new Array<Boolean>();//newかどうか---これだけはローカルで持つ
	private  Array<String> ctoDate = new Array<String>();//日付---これもローカルで持つ
	boolean nowSettingFavorite = false;//追加は一件ずつ
	boolean nowSettingThemes = false;//テーマ等設定中
	boolean visibleNoArt = false;
//	boolean isDocomoUser = false;
	boolean isPremiunError = false;
	private boolean finishBannerLoading = false;//バナーの表示が終わるまではタッチ無効化
	private boolean finishTouchSet = false;//タッチセット完了

	Random rnd = new Random();
	float delta;
	TextureParameter param;
	TextureParameter paramRepeat;
	//解放系
	Texture thumbsMask;
	Texture parts1Mask;
	Texture wordBMask;
	Texture downBGMask;
	Texture circleMask;//丸のマスク
	FrameBuffer frameBufferCash;
	FrameBuffer frameBufferMask;
	Sprite matWhiteSP;
	Sprite circleMaskSP;//丸のマスク
	Sprite matBlackSP;
	Sprite quickPreviewMaskSP;
	Sprite cashScreenSP;
	Sprite maskScreenSP;
	Sprite loadingSP;
	Sprite loadingSmallSP;
	Sprite blackMaskSP;
	Sprite borderSP;
	Sprite reloadWordSP;
	Sprite sortBgTopSP;
	Sprite sortBgBottomSP;
	Sprite  bgMaskSprite;
	TextureRegion sortBgRG;
//	Sprite matColorSP[] = new Sprite[9];
	Sprite detailWordSP;
	Sprite infoListSP;
	Sprite quickPreviewFlashSP;
	Sprite popupBottomSP;
	Sprite popupTopSP;
	Sprite matGlaySP;
	TextureRegion popupLeftRG;
	TextureRegion popupRightRG;
	Sprite splashLogoSP;
	int popUpSideHeight;

	private ShaderProgram defaultShader = null;
	private ShaderProgram maskETCShader = null;
	boolean nowLoadingFirstAssets = true;
	boolean nowLoadingSecondAssets = true;
	float cnt;
	float circleZoom = 0.0f;
	float circleMaskAlpha = 1.0f;
	Vector2 circlePosition = new Vector2();
	Color BG_GRAY = new Color(0x000000FF);//OS6taiou
//	Color BG_GRAY = new Color(0xe6e6e6FF);//OS6taiou
//	Color BG_GRAY = new Color(0xe7e7e7FF);
	Sprite  bgSprite;
	boolean cashStart;
	//丸マスクの拡大率を出すために表示領域の斜め幅を定義(1080x1920で想定)
//	final float CIRCLE_RAD = 2200f;//(float) Math.sqrt(1920*1920+1080*1080)は2202.907170082298315819なので;
	//丸マスクの拡大率--グローバルメニュー版
//	float CIRCLE_MENU_ZOOM = 27.16f;//4400/162=27.16049382716049
//	final float CIRCLE_ZOOMTIME = 0.5f;
	float CIRCLE_MENU_ZOOM = 35f;//4400/162=27.16049382716049//少し大きめ
	final float CIRCLE_ZOOMTIME = 0.6f;
	final float THUMBS_TEX_SIZE=432f;//サムネイルのサイズ
	//おすすめが26個なので
	final int THUMBSTEXMAX = 26;
	Sprite matColorSP[] = new Sprite[9];
	int thumbsTexKazu = THUMBSTEXMAX;
	Sprite  thumbsNowLoadingSprite;
	Sprite[]  thumbsSprite = new Sprite[THUMBSTEXMAX];

	final float LOADING_ANIM_SPEED = -300f;

	final float THUMBS_U = 1.0f/3.0f;
	final float THUMBS_V = 414.0f/512.0f;

	final float BOTTOM_MARGINE = 210;//下のマージン
	final float TOP_MARGINE = 126;//上のマージン
	final float TOP_MARGINE_LIST = 100;//上のマージン -40
	final float TOP_MARGINE_MATRIX = 140;//上のマージン
	final float TOP_MARGINE_TOUCH = 180;//上のマージン

	Rectangle[] thumbsTouch = new Rectangle[THUMBSTEXMAX];
	Rectangle[] rect_detail = new Rectangle[THUMBSTEXMAX];
	Rectangle[] rect_like = new Rectangle[THUMBSTEXMAX];
	Rectangle[] rect_set = new Rectangle[THUMBSTEXMAX];
	float[] thumbsY = new float[THUMBSTEXMAX];//表示テクスチャY
	float[] thumbsX = new float[THUMBSTEXMAX];
	int[] thumbsOrder = new int[THUMBSTEXMAX];//自分が一番上からから何番目か把握
	float[] thumbsScale = new float[THUMBSTEXMAX];//表示テクスチャY
	float[] thumbsBallScale = new float[THUMBSTEXMAX];//表示テクスチャY
	int[] nowIndex = new int[THUMBSTEXMAX];//上から何番目のArtを表示しているか
	float[] loadingRota = new float[THUMBSTEXMAX];//ローディングのぐるぐる用
	float  loadingRotaMask = 0f;//ローディングのぐるぐる--マスク用
	int[] thumbsQpVol = new int[THUMBSTEXMAX];//クイックプレヴューの数
	boolean[] thumbsIsQuick = new boolean[THUMBSTEXMAX];//クイックプレビューモードかどうか
	//クイックプレビュー
	float[] thumbsQ_scrollAlpha= new float[THUMBSTEXMAX];//
	float[] thumbsQ_scrollXAdd = new float[THUMBSTEXMAX];//
	float[] thumbsQ_scrollX = new float[THUMBSTEXMAX];//
	float[] thumbsQ_waitScroll = new float[THUMBSTEXMAX];//
	boolean[] thumbsQ_stopScroll = new boolean[THUMBSTEXMAX];//
	boolean[] thumbsQ_slideOK = new boolean[THUMBSTEXMAX];//クイックプレビューのスライドOKか
	float[] thumbsQ_flash = new float[THUMBSTEXMAX];//
	final float QPFLASH_SPEED = 2f;
//	EaseMoving[] emThumbsQuickMove = new EaseMoving[THUMBSTEXMAX];

	float upperLimit=0f; //ここ超えたら下に
	float underLimit=0f;//ここ超えたら上に
	float thumbsAllHeight;//サムネイル全体の高さ
	float thumbsTopY;
	int thumbsTotal = 1;
	Sprite[]  thumbsSpriteText = new Sprite[THUMBSTEXMAX];
	final float THUMBS_TEXT_Y = 415.0f/512.0f;
	final float THUMBS_TEXT_U = 408.0f/1024.0f;
	final float THUMBS_TEXT_V = 467.0f/512.0f;
//	final float THUMBS_TEXT_WIDTH = 408.0f;
	final float THUMBS_TEXT_WIDTH = 448.0f;

	final float THUMBS_TEXT_HEIGHT = 52.0f;

	final float THUMBS_ZONE_HEIGHT_LIST=516f;//リスト表示時のサムネイル一段分の高さ
//	final float THUMBS_IMAGE_X_LIST = 170f;//リスト表示時のサムネイルイメージのX座標
	final float THUMBS_IMAGE_X_LIST = 66f;//リスト表示時のサムネイルイメージのX座標
	final float THUMBS_IMAGE_Y_LIST = 44;//リスト表示時のサムネイルイメージのY座標
//	final float THUMBS_TEXT_X_LIST = 626f;//リスト表示時のサムネイルテキストのX座標
	final float THUMBS_TEXT_X_LIST = 564f;//リスト表示時のサムネイルテキストのX座標
//	final float THUMBS_TEXT_Y_LIST = 248;//リスト表示時のサムネイルテキストのY座標

//	final float THUMBS_TEXT_Y_LIST = 236;//リスト表示時のサムネイルテキストのY座標
	final float THUMBS_TEXT_Y_LIST = 233;//リスト表示時のサムネイルテキストのY座標

	final float THUMBS_ZONE_HEIGHT_MATRIX=576f;//マトリックス表示時のサムネイル一段分の高さ
	final float THUMBS_ZONE_WIDTH_MATRIX=516f;//マトリックス表示時のサムネイル一段分の幅
	final float THUMBS_IMAGE_X_MATRIX = 42f;//マトリックス表示時のサムネイルイメージのX座標
	final float THUMBS_IMAGE_Y_MATRIX = 144;//マトリックス表示時のサムネイルイメージのY座標
	final float THUMBS_BTN_X_MATRIX = 25f;//マトリックス表示時のボタン(左)のX座標
	final float THUMBS_BTN_Y_MATRIX = 0;//マトリックス表示時のボタン(左)のY座標

//	final Vector2 ListPosiDetail = new Vector2(567,84);
//	final Vector2 ListPosiLike = new Vector2(731,84);
//	final Vector2 ListPosiSet = new Vector2(895,84);
	final Vector2 ListPosiDetail = new Vector2(505,72);//-62+3
	final Vector2 ListPosiLike = new Vector2(695,72);
	final Vector2 ListPosiSet = new Vector2(885,72);
	final Vector2 MatrixPosiDetail = new Vector2(24,-1);
	final Vector2 MatrixPosiLike = new Vector2(162,-1);
	final Vector2 MatrixPosiSet = new Vector2(300,-1);

	//クイックプレビュー
	float q_scrollXAdd = 0;
	float q_scrollX = 0;
	float q_waitScroll = 0;
	float q_scrollAlpha = 0;
	boolean q_stopScroll = true;
	//sort
	boolean visibleSort = false;//出現して機能出来る状態
	boolean drawableSort = false;//描画
	int sortBarAddHeight = 0;
	float sortBarScale = 0.8f;
	float sortHeaderScale = 1.0f;
	EaseMoving emSortBarZoom;
	EaseMoving[] emSortButtonZoom = new EaseMoving[7];
	float[] sortButtonScale = new float[7];
	final float SORTBAR_ZOOMTIME = 0.4f;
	final float SORTBAR_MAX = 1001f;
	final float SORTBUTTON_ZOOMTIME = 0.4f;
	Rectangle[] rect_sortChara = new Rectangle[7];
	int[] state_sortChara = new int[7];
	float waitSortTime = 0f;
	boolean sortWhiteOut = false;
//	float myBoxHeaderScale = 1.0f;
	float recomendFooterRota = 0.0f;
	float newFooterScale = 1.0f;
	boolean startMyboxButtonAnim = false;
	float myboxButtonAnimAlpha = 0f;
	float myboxButtonAnimWait = 0f;

//	final float SORT_WHITEOUT_WAITTIME = 0.3f;
//	boolean type0 = true;
	//for 7/31MTG
//	//パターン1　基本
	final float SORT_WHITEOUT_WAITTIME = 0.5f;//イチオシ

	//shuffle
	Array<ShuffleDto> shuffleDto = new Array<ShuffleDto>();
	Array<Integer> colorBallNum = new Array<Integer>();
	int shuffleSize = 0;
	EaseMoving[] emThumbsZoom = new EaseMoving[THUMBSTEXMAX];
	final float MOVETIME_CENTER = 0.8f;
	final float ZOOMTIME_ZOOM = 0.4f;
	final float ZOOMIN_BALL = 1.0f;
	final float ZOOMOUT_BALL = 0.3f;
	final float MOVETIME_FALL = 0.6f;
	final float THUMBS_SMALL = 0.55f;
	final float FALLTIME_SA3= 1.8f;
	final float ZOOMTIME_SA3 = 0.6f;
	Vector2 centerTarget;
	int shuffle1state = 0;//0が消えて　1が出現
	int shuffle2state = 0;//0が消えて　1が出現
	int shuffle3state = 0;//0が消えて　1が出現
	int reserveShuffleAnim = -1;//クイックプレビューが開いてたら、閉じてから開始用
	int reShuffleAnim = -1;//前回のアニメーション
	private  Array<Integer> shuffleNumArray = null;
	int shuffleCount = 0;
	final int shaffleBallTotal = 26;
//recomend
	int reservePrevieZoomNum = -1;//クイックプレビューが開いてる時、別のサムネイルタップした
	final float THUMBS_ZONE_HEIGHT_RECOMEND=432f;//リスト表示時のサムネイル一段分の高さ
	final float THUMBS_REC_MAGINE=30f;//レコメンド時、上下左右のマージン
	float[] thumbsReX = new float[THUMBSTEXMAX];//recomendのディレイ用
	float[] thumbsReY = new float[THUMBSTEXMAX];//recomendのディレイ用
	float[] thumbsReStartX = new float[THUMBSTEXMAX];//recomendのプレビュー時のスタート座標
	float[] thumbsReStartY = new float[THUMBSTEXMAX];//recomendのプレビュー時のスタート座標
	double[] thumbsRePreTheta = new double[THUMBSTEXMAX];//recomendのプレビュー時の目標角度
	boolean[] tapAbleRec = new boolean[THUMBSTEXMAX];//recomendのタップ判定用
	final Vector2[] previewPosiDetail = {new Vector2(35,475),new Vector2(-179,331)};
	final Vector2[] previewPosiLike = {new Vector2(255,459),new Vector2(-13,459)};
	final Vector2[] previewPosiSet = {new Vector2(419,331),new Vector2(205,475)};
	final Vector2[]  previewPosiDetailAdd = {new Vector2(-221,15),new Vector2(-165 ,-129)};
	final Vector2[] previewPosiSetAdd = {new Vector2(163,-129),new Vector2(219 , 15)};
	//レコメンドのディレイっぷりの調整値。要調整かも
	final float recDelayMinSpeed = 5.0f;
	final float recDelayDistSpeed = 0.004f;
	//縦横フリックの制限値 値が小さいほうが制限多い
	final float REC_FLICK_LIMIT = 75f;
	int previewNum = 0;
	float previewReValue = 1f;
	final float ZOOMPER_REC = 1.17f;
	final float ZOOMTIME_REC = 0.6f;
	final float ZOOMTIME_REC_BTN = 0.3f;
	final float ZOOMBACK_REC = 2.1f;
	final float ZOOMBACK_BALL = 1.1f;
	final float ZOOMTIME_QUICK = 0.8f;
	final float ZOOMTIME_QUICK_FLICK = 0.5f;
	final float WAITTIME_QUICK = 1.5f;
	//zoomした際のスプライトの座標補正値(クイックプレビューのボタン配置用)
	final float ZOOMPM_REC = 41;//zoomした際のスプライトの座標補正値(
	final float REC_PREVIEW_MARGINE = 234;//クイックプレビューのボタンの確保領域
	final float REC_PREVIEW_HEIGHT = 514;//プレビュー時の高さ
	final float REC_ZOOM_DIST = 75f;
	float quickBtnScale = 1f;
	boolean visibleQuickBtn = false;
	final float QUICK_TOUCH_REC = 30f;//レコメンドのクイックプレビュータッチゾーンへのマージン
	//detail
	ContentsDataDto  detailCto = null;
	boolean  detailisNew = false;//個別
	int detailThumbsNum = 0;
	float pageBtnLeftX = 0f;
	boolean detailTapOK = false;
	float thumbsLeftX =0 ;
	int thumbsTotalDetail = 1;
	final int THUMBSTEXMAX_DETAIL = 6;
	int thumbsTexKazuDetail = 1;
	Sprite[]  thumbsSpriteDetail = new Sprite[THUMBSTEXMAX_DETAIL];
	float thumbsDetailY;//表示テクスチャY
	float[] thumbsDetailX = new float[THUMBSTEXMAX_DETAIL];
	Sprite  thumbsSpriteDetailText;
	int[] thumbsOrderDetail = new int[THUMBSTEXMAX_DETAIL];//自分が一番上からから何番目か把握
	int[] nowIndexDetail = new int[THUMBSTEXMAX_DETAIL];//上から何番目のArtを表示しているか
	float[] easeXBreak = new float[THUMBSTEXMAX_DETAIL];//スクロールの区切り目
//	float[] loadingRotaDetail = new float[THUMBSTEXMAX_DETAIL];//ローディングのぐるぐる用
	int easeTarget = 0;//イージングのターゲット
	final float THUMBS_TEXT_DETAIL_V = 511.0f/512.0f;
	final float THUMBS_TEXT_DETAIL_WIDTH = 616.0f;
	final float THUMBS_TEXT_DETAIL_HEIGHT = 96.0f;

	final float DETAIL_TEX_WIDTH=624f;//detailサムネイルのサイズ
	final float DETAIL_TEX_HEIGHT=1104f;//detailサムネイルのサイズ
	final float DETAIL_LEFT = 229;//1pix右に
	final float DETAIL_MAGINE = 128;
	final float DETAIL_ZONE_WIDTH = DETAIL_TEX_WIDTH+DETAIL_MAGINE;
	final float DETAIL_LEFT_LIMIT = DETAIL_ZONE_WIDTH*-3+DETAIL_LEFT + 10; //少し余裕作る
	final float DETAIL_RIGHT_LIMIT = DETAIL_ZONE_WIDTH*4+DETAIL_LEFT - 10; //少し余裕作る
	int[] thumbsReserveDetail = new int[THUMBSTEXMAX_DETAIL];//スプライトごとに左から何番目のサムネイルの表示をするかの予約番号的なのを格納
	Array<Boolean> detailArtNowCallAPI = new Array<Boolean>();//ダウンロードのAPIを読んでいる最中--2重で走るのを防止
	Array<Boolean> alreadyCalledDetailSet= new Array<Boolean>();//3個ずつのやつをすでに読んでいる
	Array<Boolean> detailArtFailed = new Array<Boolean>();//タイムアウト等で取得に失敗したアート
	Array<Boolean> detailArtReloadReserved = new Array<Boolean>();//タイムアウト等で取得に失敗したアートの再取得予約
	float detailbtnAlphaRight = 1f;

	int nowCenterDetailNum = 0;//中央に表示されているサムネイル番号
	int reCenterDetailNum = 0;//中央に表示されているサムネイル番号(前回)
	int nowCenterDetailSPNum = 0;//中央に表示されているサムネイルスプライト番号
	ContentsFileName nowCenterDetailType = ContentsFileName.ThumbDetailWp1;//中央に表示されているコンテンツのタイプ
	//サムネイル読み込み用
	final int MANAGER_MAX = 32;//架空スレッド数
	public AssetManager[] assetsMulti = new AssetManager[MANAGER_MAX];
	boolean[] nowChangingThumbs = new boolean[THUMBSTEXMAX];//今、正にロード中(変更中)かどうか
	boolean[] nowLoadingThumbs = new boolean[THUMBSTEXMAX];//サムネイル画像のロード中か
	int[] useManagerNum = new int[THUMBSTEXMAX];
	String[] requesUnloadPath = new String[THUMBSTEXMAX];
	String[] nowLoadingPath = new String[THUMBSTEXMAX];
	int managerCount = 0;
	//サムネイル読み込み用_detail
	final int MANAGER_MAX_DETAIL = 6;//架空スレッド数
	public AssetManager[] assetsMultiDetail = new AssetManager[MANAGER_MAX_DETAIL];
	boolean[] nowChangingThumbsDetail = new boolean[MANAGER_MAX_DETAIL];//今、正にロード中(変更中)かどうか
	boolean[] nowLoadingThumbsDetail = new boolean[MANAGER_MAX_DETAIL];//サムネイル画像のロード中か
	int[] useManagerNumDetail = new int[MANAGER_MAX_DETAIL];
	String[] requesUnloadPathDetail = new String[MANAGER_MAX_DETAIL];
	String[] nowLoadingPathDetail = new String[MANAGER_MAX_DETAIL];
	int[] nowThumbInfoIndex = new int[MANAGER_MAX_DETAIL];
	int managerCountDetail = 0;

	boolean nowUseETCShader = false;
	boolean menuBtnTapOK = false;

	int infoKazu = 3;//docomo以外は2段
	final float[] infoY = {390,532,674};
	Rectangle[] rect_info = new Rectangle[3];
	int tapInfo = -1;
	final Color infoSelColor = new Color(0xb2b2b2FF);
	final Color infoNomColor = new Color(0xFFFFFFFF);
	//Tutrial
	public AssetManager assetsTutrial;
	boolean nowLoadingAssetsTutrial = true;
	int nowCenterTutrialNum = 0;
	boolean tutrialTapOK = false;
	float tutrialLeftX =0 ;
	final int TUTRIAL_KAZU = 7;
	float thumbsTutrialY = 0;
	Sprite[]  thumbsSpriteTutrial = new Sprite[TUTRIAL_KAZU];
	float thumbsTutrial ;//表示テクスチャY
	float[] thumbsTutrialX = new float[TUTRIAL_KAZU];
	float[] easeXBreakTutrial = new float[TUTRIAL_KAZU];//スクロールの区切り目
//	final float TUTRIAL_TEX_WIDTH=624f;//detailサムネイルのサイズ
//	final float TUTRIAL_TEX_HEIGHT=1104f;//detailサムネイルのサイズ
	final float TUTRIAL_TEX_WIDTH=864f;//detailサムネイルのサイズ
	final float TUTRIAL_TEX_HEIGHT=1366f;//detailサムネイルのサイズ
	final float TUTRIAL_LEFT = 108;//(1080-TUTRIAL_TEX_WIDTH)/2
//	final float TUTRIAL_MAGINE = 128;
	final float TUTRIAL_MAGINE = 144;
	final float TUTRIAL_ZONE_WIDTH = TUTRIAL_TEX_WIDTH+TUTRIAL_MAGINE;
	boolean reserveFirstTutrial = false;
	boolean isFirstTutrial = false;
	boolean isFirstVisible3Tutrial = false;

//スクリーンの状態
	private  Array<ScreenState> ScreenHistory =  new Array<ScreenState>();//newかどうか---これだけはローカルで持つ;//5個分のヒストリーを格納
	private  boolean TransitionFromBackKey = false;//バックキーからの遷移かどうか
//	private   ScreenState[] ScreenHistory = new ScreenState[5];//5個分のヒストリーを格納
	boolean NowTransition = false;//遷移中
	boolean TransitionMaskDraw = false;
	enum ScreenState {
		SCREEN_LOADWAIT,
		SCREEN_OFFLINE,
		SCREEN_TUTRIAL,
		SCREEN_SPLASH,
		SCREEN_RECOMEND,
		SCREEN_NEWART,
		SCREEN_RUNKING,
		SCREEN_MYBOX,
		SCREEN_DETAIL,
		SCREEN_INFO,
	}
	ScreenState nowScreenState = ScreenState.SCREEN_LOADWAIT;
	ScreenState reserveScreenState = ScreenState.SCREEN_LOADWAIT;
	ScreenState saveScreenState = ScreenState.SCREEN_LOADWAIT;
	//おすすめ時の画面の状態
	enum RecomendState {
		SHUFFLE_ANIM1,
		SHUFFLE_ANIM2,
		SHUFFLE_ANIM3,
		PEVIEW_ZOOMIN,
		PEVIEW_ZOOMOUT,
		PEVIEW_MODE,
		NOMAL_MODE
	}
	RecomendState nowRecomendState = RecomendState.NOMAL_MODE;
	//詳細画面の状態
	enum DetailState {
		DETAIL_NOMAL,
		DETAIL_LOADING,
		DETAIL_LOADING_ERROR,
		DETAIL_DOWNLOADING,
	}
	DetailState nowDetailState = DetailState.DETAIL_NOMAL;
//タッチの状態
	boolean visibleDialog = false;//ダイアログ
	enum TouchState {
		TOUCH_DIABLE,
		TOUCH_DIABLE_QUICK,
		TOUCH_SCROLL,
		TOUCH_SCROLL_AUTO,
		TOUCH_SCROLL_BOUNCE,
		TOUCH_ONLY_MENU,
		TOUCH_ONLY_DIALOG,
	}
	TouchState nowTouchState = TouchState.TOUCH_DIABLE;
	TouchState saveTouchState = nowTouchState;
	//遷移アニメーションの状態
	enum AnimationState {
		ANIM_NONE,
		ANIM_NEW_TAP,
		ANIM_RECOMEND_TAP,
		ANIM_MYBOX_TAP,
//		ANIM_DETAIL_TAP,
		ANIM_RUNKING_TAP,
		ANIM_SORT_OPEN,
		ANIM_SORT_CLOSE,
//		ANIM_DETAIL_CLOSE,
		ANIM_CASH_SCREEN,
//		ANIM_CASH_DETAIL_SCREEN
	}
	AnimationState nowAnimationState = AnimationState.ANIM_NONE;
	AnimationState reserveAnimationState = nowAnimationState;
	boolean isOS404 = false;
	Rectangle rect_new;
	Rectangle rect_recomend;
	Rectangle rect_ranking;
	Rectangle rect_mybox;
	Rectangle rect_sort;
//	Rectangle rect_d_close;
//	Rectangle rect_d_close;
	Rectangle rect_dialog_ok;
	Rectangle rect_ditail_tap;
	Rectangle rect_ditail_leftBtn;
	Rectangle rect_ditail_RightBtn;
	Rectangle rect_ditail_like;
	Rectangle rect_tutrial_finish;
	Rectangle rect_popup_close;
	float rect_new_y;
	float rect_recomend_y;
	float rect_ranking_y;
	float rect_mybox_y;
	float rect_sort_y;
	Rectangle rect_q_detail;
	Rectangle rect_q_like;
	Rectangle rect_q_set;


	int state_header_mybox=0;
	int state_header_sort=0;
	int state_footer_new=0;
	int state_footer_recomend=1;
	int state_footer_ranking=0;
	int state_quick_detail=0;
	int state_quick_like=0;
	int state_quick_set=0;
	int state_detailNum= -1;
	int state_setNum= -1;
	int state_ellipseBtnLeft =0;
	int state_ellipseBtnRight =0;

	final float REC_SCALE_SMALL = 380f/432f;
	final float REC_SCALE_BIG = 468f/432f;
	final float REC_SCALE_PREVIEW = 514f/432f;
	final float[] thumbsRecomendX = {90,560,57,596,102,578,59,603,91,560,60,598,
																87,580,60,603,92,560,60,603,105,580,59,603,91,559};
	final float[] thumbsRecomendY = {654,501,1134,984,1653,1467,2158,1972,2646,2492,3118,2977,
																3662,3465,4151,3963,4634,4479,5111,4966,5631,5450,6139,5948,6628,6475};//topYから
	final float[] thumbsRecomendScale = {REC_SCALE_SMALL,REC_SCALE_BIG,REC_SCALE_BIG,REC_SCALE_SMALL,REC_SCALE_SMALL,
			REC_SCALE_BIG,REC_SCALE_BIG,REC_SCALE_SMALL,REC_SCALE_SMALL,REC_SCALE_BIG,REC_SCALE_BIG,REC_SCALE_SMALL,REC_SCALE_SMALL,
			REC_SCALE_BIG,REC_SCALE_BIG,REC_SCALE_SMALL,REC_SCALE_SMALL,REC_SCALE_BIG,REC_SCALE_BIG,REC_SCALE_SMALL,REC_SCALE_SMALL,
			REC_SCALE_BIG,REC_SCALE_BIG,REC_SCALE_SMALL,REC_SCALE_SMALL,REC_SCALE_BIG};//topYから

	EaseMoving emPreviewZoom;
	EaseMoving emCircleZoom;
	EaseMoving emMaskAlpha;
	EaseMoving[] emPreviewMove = new EaseMoving[THUMBSTEXMAX];
	EaseMoving emQuickBtnZoom;
	EaseMoving emQuickMove;
	final float ZOOMTIME_TAP = 0.5f;
	EaseMoving emTapMenu;
	EaseMoving emTapRecomemd;
	EaseMoving emFavoliteAdd;

	int visibleThumbsSize=0;//表示数
	boolean finisedLoadRemain = false;//後読み込み

	public final static String SCREEN_PREFERENCE_NAME = "catalogscreen_app_pref";

	ContentsCharaValue[] sortCharaValue ={ContentsCharaValue.CHARA_MICKEY,ContentsCharaValue.CHARA_MINNIE,ContentsCharaValue.CHARA_DONALD,
			ContentsCharaValue.CHARA_DAISY,ContentsCharaValue.CHARA_POOH,ContentsCharaValue.CHARA_PRINCESS,ContentsCharaValue.CHARA_PARK};
	//戻るキー対応
	public boolean BackToScreen(){
		int historySize = ScreenHistory.size;
		if(nowAnimationState == AnimationState.ANIM_NONE && nowTouchState != TouchState.TOUCH_DIABLE && menuBtnTapOK){
			if(nowScreenState == ScreenState.SCREEN_DETAIL){
				initializeDetailClose();
				DebugLog.instance.outputLog("Trans", "戻るキーでSCREEN_DETAIL閉じた");
			}
			else if(nowScreenState == ScreenState.SCREEN_INFO){
				initializeInfoClose();
				DebugLog.instance.outputLog("Trans", "戻るキーでSCREEN_INFO閉じた");
			}
			else if(nowScreenState == ScreenState.SCREEN_TUTRIAL){
				if((isFirstVisible3Tutrial && 3<=nowCenterTutrialNum) || !isFirstVisible3Tutrial){
					initializeTutrialClose();
					DebugLog.instance.outputLog("Trans", "戻るキーでSCREEN_TUTRIAL閉じた");
				}
//				else{
//					return false;
//				}
			}
			else if(0<historySize){
				ScreenState TransitionSceeen = ScreenHistory.get(historySize-1);
				DebugLog.instance.outputLog("Trans", "戻るキーで" + TransitionSceeen  + "に遷移");
				switch(TransitionSceeen){
				case SCREEN_NEWART:
					reserveAnimationState = AnimationState.ANIM_NEW_TAP;
					nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
					break;
				case SCREEN_RECOMEND:
					reserveAnimationState = AnimationState.ANIM_RECOMEND_TAP;
					nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
					break;
				case SCREEN_RUNKING:
					reserveAnimationState = AnimationState.ANIM_RUNKING_TAP;
					nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
					break;
				case SCREEN_MYBOX:
					reserveAnimationState = AnimationState.ANIM_MYBOX_TAP;
					nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
					break;
				default:
					break;
				}
				ScreenHistory.removeIndex(historySize-1);
				TransitionFromBackKey =true;//戻るキーからの遷移を履歴に残さない
			}
			else{
				return false;
			}
		}
		else if(historySize == 0 && !visibleDialog && (nowScreenState == ScreenState.SCREEN_RECOMEND || nowScreenState == ScreenState.SCREEN_NEWART
				|| nowScreenState == ScreenState.SCREEN_RUNKING || nowScreenState == ScreenState.SCREEN_MYBOX)) return false;
		return true;
	}
	void EditScreenHistory(){
		if(nowScreenState == ScreenState.SCREEN_RECOMEND || nowScreenState == ScreenState.SCREEN_NEWART
				|| nowScreenState == ScreenState.SCREEN_RUNKING || nowScreenState == ScreenState.SCREEN_MYBOX){
			DebugLog.instance.outputLog("Trans", "EditScreenHistory in TransitionFromBackKey::" + TransitionFromBackKey);
			 if(!TransitionFromBackKey){
				 if(0<ScreenHistory.size){//同じの格納しようとしていないか確認
					 if(ScreenHistory.get(ScreenHistory.size-1) != nowScreenState){
						 if(ScreenHistory.size==10){
							 ScreenHistory.removeIndex(0);
						 }
						 ScreenHistory.add(nowScreenState);
					 }
					 else{
						 DebugLog.instance.outputLog("Trans", "ScreenHistoryの最後と同じ::" + ScreenHistory.size);
					 }
				 }
				 else{
					 ScreenHistory.add(nowScreenState);
				 }
				 DebugLog.instance.outputLog("Trans", "ScreenHistoryに格納" + nowScreenState);
				 DebugLog.instance.outputLog("Trans", "ScreenHistoryのサイズ " + ScreenHistory.size);
			 }
		}
		TransitionFromBackKey = false;
	}

	public int getPM(){
		return rnd.nextInt(2)*2-1;
	}

	public void setActivity(SplashActivity me){
		this.myActivity = me;
	}

	public void setBG_GRAY(){//OS6taiou
		BG_GRAY.set(0xe6e6e6FF);
	}

	@Override
	public void create() {
//		 DebugLog.instance.outputLog("info", "Math.asin(1)==" + Math.asin(1));

        String osVersion = android.os.Build.VERSION.RELEASE;
        DebugLog.instance.outputLog("info", "VERSION-" + osVersion);
        //KYY04は4.0.3なのにエラーになったので…
        if(osVersion.equals("4.0.3") || osVersion.equals("4.0.4")) isOS404 = true;
        else isOS404 = false;

		//インスタンス初期化
		initializeInstance();
		//初期アセットロード(ロード画面に必要なデータ)
		loadAssetsFirst();
	}
	void resetBtnState(){
		state_header_mybox=0;
		state_footer_new=0;
		state_footer_recomend=0;
		state_footer_ranking=0;
		state_detailNum= -1;
		state_setNum= -1;
		state_ellipseBtnLeft =0;
		state_ellipseBtnRight =0;
	}
	void resetQuickState(){
		q_scrollXAdd = 0;
		q_scrollX = 0;
		q_waitScroll = 0;
		q_stopScroll = true;
		q_scrollAlpha = 0f;
//		emQuickMove.ResetTime();
		emQuickMove.ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK);
	}
	void enableQuickState(int num){
		thumbsQ_scrollAlpha[num] = 1f;
		thumbsQ_scrollXAdd[num] = 0;
		thumbsQ_scrollX[num] = 0;
		thumbsQ_waitScroll[num] = 1.2f;//最初の待ち時間は短縮(0.3秒)
//		thumbsQ_waitScroll[num] = 0;
		thumbsQ_stopScroll[num] = true;
		thumbsIsQuick[num] = true;
		thumbsQpVol[num] = cto.get(nowIndex[num]).qpNum;
		emPreviewMove[num].ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK);
		thumbsQ_flash[num] = 1f;
	}
	void disableQuickState(int num){
		DebugLog.instance.outputLog("info", "クイックプレビューリセット:::" + num);
		thumbsSprite[num].setAlpha(ONE);
		thumbsSprite[num].setU(0);
		thumbsSprite[num].setU2(THUMBS_U);
		thumbsQ_scrollAlpha[num] = 1f;
		thumbsQ_scrollXAdd[num] = 0;
		thumbsQ_scrollX[num] = 0;
		thumbsQ_waitScroll[num] = 0;
		thumbsQ_stopScroll[num] = true;
		thumbsIsQuick[num] = false;
		thumbsQ_slideOK[num] = false;
		thumbsQ_flash[num] = 0f;
	}
	//get preference
	private SharedPreferences getSharedPreference(Context context){
		return context.getSharedPreferences(SCREEN_PREFERENCE_NAME, Context.MODE_PRIVATE | Context.MODE_MULTI_PROCESS);
	}


//	thumbsSprite[previewNum].setU(0);
//	thumbsSprite[previewNum].setU2(THUMBS_U);
	private void initializeInstance() {
		batch = new SpriteBatch();
		camPos = new Vector2();
		camera = new OrthographicCamera();
		assets = new AssetManager();// 一回だけでOK
		for(int i=0;i<MANAGER_MAX;i++){
			assetsMulti[i] = new AssetManager(new LocalFileHandleResolver());
		}
		for(int i=0;i<MANAGER_MAX_DETAIL;i++){
			assetsMultiDetail[i] = new AssetManager(new LocalFileHandleResolver());
		}
		assetsTutrial = new AssetManager();
		param = new TextureParameter();
		param.minFilter = TextureFilter.Linear;
		param.magFilter = TextureFilter.Linear;
		paramRepeat = new TextureParameter();
		paramRepeat.minFilter = TextureFilter.Linear;
		paramRepeat.magFilter = TextureFilter.Linear;
		paramRepeat.wrapU = TextureWrap.Repeat;
//		param.format = Format.RGB888;
		// 一瞬読み込みで止まるのでcreateの段階でやるべき事
		defaultShader = SpriteBatch.createDefaultShader();
		final String vertexMaskETCShader = Gdx.files.internal("data/etc1.vert").readString();
		final String fragmentMaskETCShader = Gdx.files.internal("data/etc1.frag").readString();
		maskETCShader = new ShaderProgram(vertexMaskETCShader,fragmentMaskETCShader);
		ETCShaderSet();
	}
	//spriteはここで
	private void initializeSprite() {
		//inputここで
		if(finishBannerLoading){
			finishTouchSet = true;
	        InputMultiplexer multiplexer = new InputMultiplexer();
	        multiplexer.addProcessor(new MyInputListener());
	        multiplexer.addProcessor(new GestureDetector(new MyGestureListener()));
			Gdx.input.setInputProcessor(multiplexer);
			DebugLog.instance.outputLog("info", "InputMultiplexer　セット　in initializeSprite");
		}
		thumbsNowLoadingSprite = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class),0f,0f,THUMBS_U,THUMBS_V));
		thumbsNowLoadingSprite.setScale(1f);
		thumbsNowLoadingSprite.setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
		thumbsNowLoadingSprite.setOrigin(THUMBS_TEX_SIZE/2f, THUMBS_TEX_SIZE/2f);
		for (int i = 0; i < THUMBSTEXMAX; i++) {
			if(i<9){
				colorBallNum.add(i);
				matColorSP[i] = new Sprite(assets.get("data/mat_col" + (i+1)  + ".etc1", Texture.class));
				matColorSP[i].setScale(0f);
				matColorSP[i].setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
				matColorSP[i].setOrigin(THUMBS_TEX_SIZE / 2f, THUMBS_TEX_SIZE / 2f);
			}
//			thumbsSprite[i] = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class), 0f, 0f, THUMBS_U, THUMBS_V));
			thumbsSprite[i] = new Sprite(new TextureRegion(assets.get("data/mat_white.etc1", Texture.class), 0f, 0f, THUMBS_U, THUMBS_V));
			thumbsSprite[i].setScale(1f);
			thumbsSprite[i].setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
			thumbsSprite[i].setOrigin(THUMBS_TEX_SIZE / 2f, THUMBS_TEX_SIZE / 2f);
			thumbsTouch[i] = new Rectangle(50000,0,THUMBS_TEX_SIZE,THUMBS_TEX_SIZE);//画面外へ
			rect_detail[i] = new Rectangle(50000,0,132,132);
			rect_like[i] = new Rectangle(50000,0,132,132);
			rect_set[i] = new Rectangle(50000,0,132,132);
//			thumbsSpriteText[i] = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class), 0f, THUMBS_TEXT_Y, THUMBS_TEXT_U, THUMBS_TEXT_V));
			thumbsSpriteText[i] = new Sprite(new TextureRegion(assets.get("data/mat_white.etc1", Texture.class), 0f, THUMBS_TEXT_Y, THUMBS_TEXT_U, THUMBS_TEXT_V));
			thumbsSpriteText[i].setSize(THUMBS_TEXT_WIDTH, THUMBS_TEXT_HEIGHT);
			thumbsX[i] = 0;
			thumbsOrder[i] = i;
			nowIndex[i] = i;
			loadingRota[i] = 0;
			thumbsScale[i] = 1f;
			//loading
			nowChangingThumbs[i] = true;
			nowLoadingThumbs[i] = false;
			requesUnloadPath[i] = "";
			nowLoadingPath[i] = "";
//	        emThumbsQuickMove[i] = new EaseMoving(0, THUMBS_U,ZOOMTIME_QUICK);
			emThumbsZoom[i] = new EaseMoving(ONE, 0, ZOOMTIME_ZOOM,ZOOMBACK_REC);
			emPreviewMove[i] = new EaseMoving(1f,1f,ZOOMTIME_REC,ZOOMBACK_REC-0.3f);//少し小さく…
		}
		for (int i = 0; i < THUMBSTEXMAX_DETAIL; i++) {
//			thumbsSpriteDetail[i] = new Sprite(assets.get("data/loading_thumbs.etc1", Texture.class));
			thumbsSpriteDetail[i] = new Sprite(assets.get("data/mat_white.etc1", Texture.class));
			thumbsSpriteDetail[i].setSize(DETAIL_TEX_WIDTH, DETAIL_TEX_HEIGHT);
			thumbsDetailX[i] = 0;
			thumbsOrderDetail[i] = i;
			nowIndexDetail[i] = i;
//			loadingRotaDetail[i] = 0;
			//loading
			nowChangingThumbsDetail[i] = true;
			nowLoadingThumbsDetail[i] = false;
			requesUnloadPathDetail[i] = "";
			nowLoadingPathDetail[i] = "";
		}
//		thumbsSpriteDetailText = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class),THUMBS_TEXT_U,THUMBS_TEXT_Y,1,THUMBS_TEXT_DETAIL_V));
		thumbsSpriteDetailText = new Sprite(new TextureRegion(assets.get("data/mat_white.etc1", Texture.class),THUMBS_TEXT_U,THUMBS_TEXT_Y,1,THUMBS_TEXT_DETAIL_V));
		thumbsSpriteDetailText.setSize(THUMBS_TEXT_DETAIL_WIDTH, THUMBS_TEXT_DETAIL_HEIGHT);
		 //footer rect--realHeightわかってる？
		DebugLog.instance.outputLog("info", "realHeight(initializeSprite)=" + realHeight);
		 //162,162//+40 +41
		rect_new = new Rectangle(34, 36, 162, 162);
		rect_recomend = new Rectangle(460, 36, 162, 162);
		rect_ranking = new Rectangle(886, 36, 162, 162);
		//144,144 //+48 +49
		rect_mybox = new Rectangle(43, realHeight - 167, 144, 144);
		rect_sort = new Rectangle(895, realHeight - 167, 144, 144);
//		rect_d_close = new Rectangle(911, realHeight - 185, 144, 144);
//		rect_d_close = new Rectangle(895, realHeight - 167, 144, 144);
		rect_dialog_ok= new Rectangle(346, centerY-199, 390, 90);
		rect_ditail_tap = new Rectangle(DETAIL_LEFT, centerY-530, DETAIL_TEX_WIDTH, DETAIL_TEX_HEIGHT);
		rect_q_detail= new Rectangle(0, -1000, 132, 132);
		rect_q_like= new Rectangle(0, -1000, 132, 132);
		rect_q_set= new Rectangle(0, -1000, 132, 132);
		shuffleNumArray = new Array<Integer>();
		for(int i=0;i<3;i++){
			rect_info[i] = new Rectangle(0, topY-infoY[i],1080, 140);
			shuffleNumArray.add(i);
		}
		shuffleNumArray.shuffle();
		popUpSideHeight = (int) (realHeight - 128 - (6+20))+2;
		thumbsDetailY = centerY-530;
		rect_ditail_leftBtn = new Rectangle(191, thumbsDetailY-241, 390, 90);
		rect_ditail_RightBtn = new Rectangle(621, thumbsDetailY-241, 390, 90);
		rect_tutrial_finish = new Rectangle(345, centerY-742+72, 390, 90);//tutrialY = centerY-742
		rect_popup_close = new Rectangle(882, realHeight-198, 130, 130);//882, realHeight-198
		rect_ditail_like = new Rectangle(7+QUICK_TOUCH_REC, thumbsDetailY-293 + QUICK_TOUCH_REC, 132, 132);
        //第四引数 backValue = 1.70158f;//反動距離的な　大きくすると距離が伸びる
        emPreviewZoom = new EaseMoving(ONE, ZOOMPER_REC, ZOOMTIME_REC,ZOOMBACK_REC);
        emCircleZoom = new EaseMoving(0, ONE,CIRCLE_ZOOMTIME);
        emMaskAlpha = new EaseMoving(ONE,0, CIRCLE_ZOOMTIME);
        emQuickBtnZoom = new EaseMoving(0, ONE,ZOOMTIME_REC_BTN);
        emQuickMove = new EaseMoving(0, THUMBS_U,ZOOMTIME_QUICK);
		emSortBarZoom = new EaseMoving(0, SORTBAR_MAX,SORTBAR_ZOOMTIME);
		emTapMenu = new EaseMoving(0.8f, ONE, ZOOMTIME_TAP,ZOOMBACK_REC*0.25f);
		emTapMenu.SetStateTime(ZOOMTIME_TAP);
		emTapRecomemd = new EaseMoving(0, 360,ZOOMTIME_TAP*1.4f);
		emTapRecomemd.SetStateTime(ZOOMTIME_TAP*1.4f);
		emFavoliteAdd = new EaseMoving(0.8f, ONE, ZOOMTIME_TAP,ZOOMBACK_REC);
		emFavoliteAdd.SetStateTime(ZOOMTIME_TAP);
		for (int i = 0; i <7; i++) {
	        //第四引数 backValue = 1.70158f;//反動距離的な　大きくすると距離が伸びる
			emSortButtonZoom[i] = new EaseMoving(0, ONE,SORTBUTTON_ZOOMTIME,ZOOMBACK_REC);
			sortButtonScale[i] = 0f;
			rect_sortChara[i] = new Rectangle(901, topY-306-(i*143), 132, 132);
			state_sortChara[i] = 0;
		}
		centerTarget = new Vector2(540-THUMBS_TEX_SIZE*0.5f,centerY -THUMBS_TEX_SIZE*0.5f);
	}
	public void onFinishBannerLoading(){
		finishBannerLoading = true;//タッチ可の条件
	}
	void initializeQuickBtn(float scale){
		assetObj.quick_btn.setScale(scale);
		assetObj.quick_detail[0].setScale(scale);
		assetObj.quick_like[0].setScale(scale);
		assetObj.quick_set[0].setScale(scale);
		assetObj.quick_detail[1].setScale(scale);
		assetObj.quick_like[1].setScale(scale);
		assetObj.quick_set[1].setScale(scale);
	}
	//新着の初期化
	private void initializeListScreen(boolean isNewScreen) {
        //nullチェックマスト
        if(cto!=null) cto.clear();
        if(isNewScreen){
        	cto = ContentsOperatorForCatalog.op.getNewArrivalContents();
        	thumbsTotal = cto.size;
        }
        else{
        	cto = ContentsOperatorForCatalog.op.getRankingContents(ContentsTypeValue.CONTENTS_TYPE_THEME);//ランキング--テーマのみ
        	thumbsTotal = 10;//強制
        }
        DebugLog.instance.outputLog("api", "Listの数：" + thumbsTotal);
        //Newフラグ、日付代入
        if(ctoIsNew!=null) ctoIsNew.clear();
        if(ctoDate!=null) ctoDate.clear();
        for (int i = 0; i < thumbsTotal; i++) {
        	ctoIsNew.add(cto.get(i).getNewProperty());
        	ctoDate.add(cto.get(i).getDateOfContents());
        }

		thumbsTopY = topY-TOP_MARGINE_LIST;
		thumbsAllHeight=0;
		offsetWidth = THUMBS_ZONE_HEIGHT_LIST *thumbsTotal - realHeight+TOP_MARGINE_LIST+BOTTOM_MARGINE;// スクロール幅
		isBounce = true;
		flickYdist = offsetWidth;
		thumbsTexKazu = Math.min(THUMBSTEXMAX,thumbsTotal);
		mEaseY = 0;
		mLastY = mEaseY;
		quickPreviewMaskSP.setScale(ONE);
		for (int i = 0; i < thumbsTexKazu; i++) {
			//初期化--ここでかは後で検討
			thumbsSprite[i].setScale(1f);
			//クイックプレビューリセット
			thumbsSprite[i].setAlpha(1f);
			thumbsTouch[i].width = THUMBS_TEX_SIZE;
			thumbsTouch[i].height = THUMBS_TEX_SIZE;
			thumbsX[i] = 0;
			thumbsY[i] = thumbsTopY - (i*THUMBS_ZONE_HEIGHT_LIST);//サムネイル一段分
			thumbsTouch[i].x = thumbsX[i]+THUMBS_IMAGE_X_LIST;
			thumbsTouch[i].y = thumbsY[i]+THUMBS_IMAGE_Y_LIST;
			thumbsOrder[i] = i;
			nowIndex[i] = i;
			thumbsScale[i]  = 1f;
			loadingRota[i] = 0;
			disableQuickState(i);
			DebugLog.instance.outputLog("info", "クイックプレビューの数：" + i + "::"+ thumbsQpVol[i]);
			//loading
			nowChangingThumbs[i] = true;
			nowLoadingThumbs[i] = false;
			thumbsAllHeight+=THUMBS_ZONE_HEIGHT_LIST;
		}
//		finisedLoadRemain = true;
		//必要な分だけ読む--遅い端末対策
		visibleThumbsSize = getVisibleThumbsVolume(true);
		finisedLoadRemain = false;
		for (int i = 0; i < visibleThumbsSize; i++) {
			String thumbnailID = String.valueOf(cto.get(i).assetID);
			loadingThumbs(i, thumbnailID);
		}
//		DebugLog.instance.outputLog("api", "cto.size：" +cto.size);

		initializeQuickBtn(1f);
		resetBtnState();
		sortWhiteOut =false;
		 if(isNewScreen){
				for (int i = 0; i < 7; i++) state_sortChara[i]= 0;
				initializeSortReset();
				state_footer_new = 1;
		 }
		 else state_footer_ranking = 1;
		 if(isNewScreen){
			 if(nowScreenState != ScreenState.SCREEN_NEWART)  EditScreenHistory();
			 nowScreenState = ScreenState.SCREEN_NEWART;
		 }
		 else{
			 EditScreenHistory();
			 nowScreenState = ScreenState.SCREEN_RUNKING;
		 }

	}
	//新着の初期化
	private void initializeRecomendScreen() {
		/*
        //nullチェックマスト
        if(cto!=null) cto.clear();
        cto = ContentsOperatorForCatalog.op.getPickUpContents();
        if(26<=cto.size) thumbsTotal = 26;
        else if(18<=cto.size) thumbsTotal = 18;
        else{
        	thumbsTotal = (int)(cto.size/2) *2; //18個以下ケア
        }
        DebugLog.instance.outputLog("api", "thumbsTotal：" +thumbsTotal);
        cto.shuffle();//シャッフル
        //Newフラグ代入//シャッフル後
        if(ctoIsNew!=null) ctoIsNew.clear();
        for (int i = 0; i < thumbsTotal; i++) {
        	ctoIsNew.add(cto.get(i).getNewProperty());
        }
//    	float gap = thumbsRecomendY[thumbsTotal-2] + (198 -((THUMBS_TEX_SIZE-(thumbsRecomendScale[thumbsTotal-2]*THUMBS_TEX_SIZE))/2));
//    	DebugLog.instance.outputLog("api", "hikukazu：" +((THUMBS_TEX_SIZE-(thumbsRecomendScale[thumbsTotal-2]*THUMBS_TEX_SIZE))/2));
//    	DebugLog.instance.outputLog("api", "thumbsRecomendY[thumbsTotal-2] ：" +thumbsRecomendY[thumbsTotal-2] );
//    	DebugLog.instance.outputLog("api", "gap：" +gap);
        if(thumbsTotal == 26) offsetWidth =6800 - realHeight;//
        else if(thumbsTotal == 18) offsetWidth =4806 - realHeight;//18個
        //18個以下ケア
        else if(2<=thumbsTotal){
        	float gap = thumbsRecomendY[thumbsTotal-2] + (198 -((THUMBS_TEX_SIZE-(thumbsRecomendScale[thumbsTotal-2]*THUMBS_TEX_SIZE))/2));
        	offsetWidth =gap - realHeight;
        }
        */

        //phase3改修-------------------------------------------------------------------------------------------------------------
        //26個以下はありえない
        //nullチェックマスト
        if(cto!=null) cto.clear();
        cto = ContentsOperatorForCatalog.op.getPickUpContents();
        DebugLog.instance.outputLog("catalog", "おすすめ数：" + cto.size);
        //必要ならシャッフル
        //TODO とりあえずシャッフルなら以下--いずれ決める
        cto.shuffle();//シャッフル

        for (int i = 0; i < cto.size; i++){
        	DebugLog.instance.outputLog("catalog", "おすすめ　　:: " + i  + "番目 : ID ::" + cto.get(i).assetID);
        }
        //おすすめが26個に満たない場合
        if(cto.size<26){
            Array<ContentsDataDto> ctoadd = null;
            ctoadd = ContentsOperatorForCatalog.op.getNotPickUpContents();
            ctoadd.shuffle();
            int addkazu = 26-cto.size;
            for (int i = 0; i <addkazu; i++){
            	cto.add(ctoadd.get(i));
            	DebugLog.instance.outputLog("catalog", "おすすめ以外:: " + i  + "番目 : ID ::" + ctoadd.get(i).assetID);
            }
        }
        thumbsTotal = 26;
        offsetWidth =6800 - realHeight;

        //Newフラグ代入//シャッフル後
        if(ctoIsNew!=null) ctoIsNew.clear();
        for (int i = 0; i < thumbsTotal; i++) {
        	ctoIsNew.add(cto.get(i).getNewProperty());
        }

        //phase3改修-------------------------------------------------------------------------------------------------------------

		isBounce = true;
		mEaseY = 0;
		mLastY = mEaseY;
        flickYdist =offsetWidth-mLastY;
		//reset
		flingStateTime = 0;
	    addFling = 0;
	    //念のため
	    reserveShuffleAnim=-1;
	    reservePrevieZoomNum=-1;
	    visibleQuickBtn = false;
	    thumbsTexKazu = Math.min(THUMBSTEXMAX,thumbsTotal);
		for (int i = 0; i < thumbsTexKazu; i++) {
				//初期化--ここでかは後で検討
				//クイックプレビューリセット
				thumbsSprite[i].setU(0);
				thumbsSprite[i].setU2(THUMBS_U);
				thumbsSprite[i].setAlpha(1f);
				thumbsTouch[i].width = THUMBS_TEX_SIZE;
				thumbsTouch[i].height = THUMBS_TEX_SIZE;
				thumbsTouch[i].x = 50000;//画面外へ
				thumbsTouch[i].y = 50000;//画面外へ
				thumbsX[i] = thumbsRecomendX[i];
				thumbsY[i] = topY-thumbsRecomendY[i];
				thumbsQpVol[i] = cto.get(i).qpNum;
				DebugLog.instance.outputLog("api", "qpNum：" + i + "::" +thumbsQpVol[i]);
				thumbsReX[i] = thumbsX[i];
				thumbsReY[i] = thumbsY[i];
				thumbsOrder[i] = i;
				nowIndex[i] = i;
				loadingRota[i] = 0;
//				if(nowRecomendState == RecomendState.SHUFFLE_ANIM1) thumbsBallScale[i] = 0f;
//				else thumbsScale[i] = thumbsRecomendScale[i];
				thumbsBallScale[i] = 0f;
				thumbsScale[i] = thumbsRecomendScale[i];
				tapAbleRec[i] = false;
				//loading
				nowChangingThumbs[i] = true;
				nowLoadingThumbs[i] = false;
			}
		//必要な分だけ読む--遅い端末対策
//		shuffleSize = getAnimThumbsVolume();
		visibleThumbsSize = getAnimThumbsVolume();
		shuffleSize = visibleThumbsSize;//前後する可能性を考慮
		finisedLoadRemain = false;
		for (int i = 0; i < visibleThumbsSize; i++) {
			String thumbnailID = String.valueOf(cto.get(i).assetID);
			loadingThumbs(i, thumbnailID);
		}
		if(nowScreenState != ScreenState.SCREEN_RECOMEND)  EditScreenHistory();
			//初回時
			if(nowScreenState==ScreenState.SCREEN_SPLASH){
//				//ここで判定
//				isDocomoUser = SPPUtility.isDocomoDevice(myActivity.getApplicationContext());
				//SCREENの状態を変更
				nowScreenState = ScreenState.SCREEN_RECOMEND;
				//TOUCHの状態を変更
				nowTouchState = TouchState.TOUCH_SCROLL;
				nowRecomendState = RecomendState.NOMAL_MODE;
				if(!finisedLoadRemain) loadRemainThumbs();
//				//シャッフルアニメで始めて見る
//				nowRecomendState = RecomendState.SHUFFLE_ANIM1;
			}
			else nowScreenState = ScreenState.SCREEN_RECOMEND;
			//シャッフルアニメで始めて見る
//			nowRecomendState = RecomendState.SHUFFLE_ANIM1;
	        if(nowRecomendState == RecomendState.SHUFFLE_ANIM1){
	        	initializeOpenningAnim(0);
	        }
	        else if(nowRecomendState == RecomendState.SHUFFLE_ANIM2){
	        	initializeOpenningAnim(1);
	        }
	        else if(nowRecomendState == RecomendState.SHUFFLE_ANIM3){
	        	initializeOpenningAnim(2);
	        }
	        else nowRecomendState = RecomendState.NOMAL_MODE;
			initializeQuickBtn(0);
			resetBtnState();
			state_footer_recomend = 1;
		}

	void loadRemainThumbs(){
		for (int  i= visibleThumbsSize; i < thumbsTexKazu; i++) {
			String thumbnailID = String.valueOf(cto.get(i).assetID);
			loadingThumbs(i, thumbnailID);
		}
		DebugLog.instance.outputLog("flamework", "残り読み込みーー ");
		finisedLoadRemain = true;
	}
	//詳細の初期化
		private void initializeDetailScreen() {
			//API叩く前にロードの下の表示用に決めておくべきこと
			thumbsDetailY = centerY-530;
			detailTapOK = false;
			loadingRota[0] = 0;
			initializeQuickBtn(1f);
			q_waitScroll = 0;
			nowTouchState = TouchState.TOUCH_DIABLE;
			CallDetailFirstAPI();
			nowScreenState = ScreenState.SCREEN_DETAIL;
		}
		//INFOの初期化
		private void initializeInfoScreen() {
//			if(isDocomoUser) infoKazu = 3;
//			else infoKazu = 2;
			infoKazu = 2;
			saveScreenState = nowScreenState;
			saveTouchState = nowTouchState;
			nowScreenState = ScreenState.SCREEN_INFO;
			nowTouchState = TouchState.TOUCH_ONLY_MENU;
		}
		//Myboxの初期化
		private void initializeMyBoxScreen(){
	        //nullチェックマスト
	        if(cto!=null) cto.clear();
	        cto = ContentsOperatorForCatalog.op.getMyPageContents();
	        if(cto==null){
	        	DebugLog.instance.outputLog("api", "cto==null!!!!!!!!!!");
	        	thumbsTotal = 0;
	        }
	        else thumbsTotal = Math.min(50,cto.size);
	        DebugLog.instance.outputLog("api", "MyBoxの数：" + thumbsTotal);
	        if(thumbsTotal==0) visibleNoArt = true;
	        else visibleNoArt = false;
	        //Newフラグ代入
	        if(ctoIsNew!=null) ctoIsNew.clear();
	        for (int i = 0; i < thumbsTotal; i++) {
	        	ctoIsNew.add(cto.get(i).getNewProperty());
	        }

			thumbsTopY = topY - TOP_MARGINE_MATRIX;
			thumbsAllHeight=0;
			int total = Math.round(thumbsTotal/2f);
			DebugLog.instance.outputLog("info", "total：" +total);
			if(THUMBS_ZONE_HEIGHT_MATRIX *total<= realHeight-(BOTTOM_MARGINE + TOP_MARGINE_MATRIX)){
				offsetWidth = 0;
				isBounce = false;
			}
			else{
				offsetWidth = THUMBS_ZONE_HEIGHT_MATRIX *total - realHeight+BOTTOM_MARGINE + TOP_MARGINE_MATRIX;// スクロール幅
				isBounce = true;
			}
			flickYdist = offsetWidth;
			thumbsTexKazu = Math.min(THUMBSTEXMAX,thumbsTotal);
			mEaseY = 0;
			mLastY = mEaseY;
			for (int i = 0; i < thumbsTexKazu; i++) {
				//初期化--ここでかは後で検討
				thumbsSprite[i].setScale(1f);
				//クイックプレビューリセット
				thumbsSprite[i].setU(0);
				thumbsSprite[i].setU2(THUMBS_U);
				thumbsSprite[i].setAlpha(1f);
				thumbsTouch[i].width = THUMBS_TEX_SIZE;
				thumbsTouch[i].height = THUMBS_TEX_SIZE;
				thumbsX[i] = THUMBS_ZONE_WIDTH_MATRIX*(i%2) + 24;
				thumbsY[i] = thumbsTopY - ((i/2)*THUMBS_ZONE_HEIGHT_MATRIX);//サムネイル一段分
				thumbsTouch[i].x = 50000;//画面外
				thumbsTouch[i].y = 50000;//画面外
				thumbsOrder[i] = i;
				nowIndex[i] = i;
				thumbsScale[i]  = 1f;
				loadingRota[i] = 0;
				//loading
				//loading
				nowChangingThumbs[i] = true;
				nowLoadingThumbs[i] = false;
//				String thumbnailID = String.valueOf(cto.get(i).assetID);
//				loadingThumbs(i, thumbnailID);
				if(i%2==0) thumbsAllHeight+=THUMBS_ZONE_HEIGHT_MATRIX;
			}
			//必要な分だけ読む--遅い端末対策
			visibleThumbsSize = getVisibleThumbsVolume(false);
			finisedLoadRemain = false;
			for (int i = 0; i < visibleThumbsSize; i++) {
				String thumbnailID = String.valueOf(cto.get(i).assetID);
				loadingThumbs(i, thumbnailID);
			}
			initializeQuickBtn(1f);
			resetBtnState();
			state_header_mybox = 1;
			 EditScreenHistory();
			nowScreenState = ScreenState.SCREEN_MYBOX;
	}
	private void initializeCharaSort(ContentsCharaValue charaValue){
		waitSortTime = 0f;
		//nullチェックマスト
        if(cto!=null) cto.clear();
        cto = ContentsOperatorForCatalog.op.getCharaContents(charaValue);
        if(cto==null) thumbsTotal = 0;
        else thumbsTotal = cto.size;
        DebugLog.instance.outputLog("api", "キャラソートの数：" + thumbsTotal);
        //Newフラグ代入
        if(ctoIsNew!=null) ctoIsNew.clear();
        if(ctoDate!=null) ctoDate.clear();
        for (int i = 0; i < thumbsTotal; i++) {
        	ctoIsNew.add(cto.get(i).getNewProperty());
        	ctoDate.add(cto.get(i).getDateOfContents());
        }
		thumbsTopY = topY-TOP_MARGINE_LIST;
		thumbsAllHeight=0;

		if(THUMBS_ZONE_HEIGHT_LIST *thumbsTotal<= realHeight-TOP_MARGINE_LIST-BOTTOM_MARGINE){
			offsetWidth = 0;
			isBounce = false;
		}
		else{
			offsetWidth = THUMBS_ZONE_HEIGHT_LIST *thumbsTotal - realHeight +TOP_MARGINE_LIST+BOTTOM_MARGINE;// スクロール幅
			isBounce = true;
		}
		flickYdist = offsetWidth;
		thumbsTexKazu = Math.min(THUMBSTEXMAX,thumbsTotal);
		mEaseY = 0;
		mLastY = mEaseY;
		for (int i = 0; i < thumbsTexKazu; i++) {
			//初期化--ここでかは後で検討
			thumbsSprite[i].setScale(1f);
			//クイックプレビューリセット
//			thumbsSprite[i].setU(0);
//			thumbsSprite[i].setU2(THUMBS_U);
			thumbsSprite[i].setAlpha(1f);
			thumbsX[i] = 0;
			thumbsY[i] = thumbsTopY - (i*THUMBS_ZONE_HEIGHT_LIST);//サムネイル一段分
			thumbsTouch[i].width = THUMBS_TEX_SIZE;
			thumbsTouch[i].height = THUMBS_TEX_SIZE;
			thumbsTouch[i].x = thumbsX[i]+THUMBS_IMAGE_X_LIST;
			thumbsTouch[i].y = thumbsY[i]+THUMBS_IMAGE_Y_LIST;
			thumbsOrder[i] = i;
			nowIndex[i] = i;
			thumbsScale[i]  = 1f;
			loadingRota[i] = 0;
			disableQuickState(i);
			//loading
			nowChangingThumbs[i] = true;
			nowLoadingThumbs[i] = false;
			String thumbnailID = String.valueOf(cto.get(i).assetID);
			loadingThumbs(i, thumbnailID);
			thumbsAllHeight+=THUMBS_ZONE_HEIGHT_LIST;
		}
		EditScreenHistory();
//		initializeSortReset();
		initializeQuickBtn(1f);
		resetBtnState();
		state_footer_new = 1;
//		initializeSortClose();
	}
	private void loadAssetsFirst() {
		assets.load("data/bg_mat.etc1", Texture.class,param);
		assets.load("data/splash_icon.ktx", Texture.class,param);
		assets.load("data/loading.png", Texture.class,param);
		//mat
		assets.load("data/mat_black.etc1", Texture.class,param);
		nowLoadingFirstAssets = true;
	}
	//ロード画面以外のアセット
	private void loadAssetsSecond() {
		assets.load("data/tex_parts.txt", TextureAtlas.class);
		assets.load("data/thumbnail_mask.etc1", Texture.class,paramRepeat);
		assets.load("data/loading_thumbs.etc1", Texture.class,param);
		assets.load("data/preview_mask.png", Texture.class,param);
		assets.load("data/tex_word_b_mask.etc1", Texture.class,param);
		assets.load("data/tex_parts1_mask.ktx", Texture.class,param);
		assets.load("data/downloadtheme_bg_mask.ktx", Texture.class,param);
		assets.load("data/circle_mask.etc1", Texture.class,param);
		assets.load("data/circleball_mask.etc1", Texture.class,param);
		assets.load("data/reload_thumbnail.etc1", Texture.class,param);
		//mat
		assets.load("data/mat_white.etc1", Texture.class,param);
		//mat
		assets.load("data/menu_line.etc1", Texture.class,param);
		assets.load("data/sort_anim.png", Texture.class,param);
		//popup
		assets.load("data/popup_top.ktx", Texture.class,param);
		assets.load("data/popup_bottom.ktx", Texture.class,param);
		assets.load("data/popup_left.ktx", Texture.class,param);
		assets.load("data/popup_right.ktx", Texture.class,param);
		assets.load("data/mat_glay.etc1", Texture.class,param);
		for (int i = 1; i <= 9; i++) {
			assets.load("data/mat_col" + i  + ".etc1", Texture.class,param);
		}
		nowLoadingSecondAssets = true;
	}
	private void doneLoadingFirstAssets() {
		DebugLog.instance.outputLog("info", "centerY 出てる？：" + (((centerY - 128) / 4f) - 32));

		bgSprite = new Sprite(assets.get("data/bg_mat.etc1", Texture.class));
//      bgSprite.setSize(1080, 1920);
      //TODO　縦長端末対策　2017/4/18
		bgSprite.setSize(1080, realHeight);
		loadingSP = new Sprite(assets.get("data/loading.png", Texture.class));
		loadingSP.setPosition(481, centerY-59);
		loadingSP.setSize(118, 118);
		loadingSP.setOrigin(59,59);
		loadingSmallSP = new Sprite(assets.get("data/loading.png", Texture.class));
		loadingSmallSP.setPosition(508, ((centerY - 115) / 4f) - 32);
		loadingSmallSP.setSize(64, 64);
		loadingSmallSP.setOrigin(32,32);
		blackMaskSP = new Sprite(assets.get("data/mat_black.etc1", Texture.class));
		blackMaskSP.setAlpha(0.5f);
//		blackMaskSP.setSize(1080, 1920);
	    //TODO　縦長端末対策　2017/4/18
		blackMaskSP.setSize(1080, realHeight);
		splashLogoSP = new Sprite(assets.get("data/splash_icon.ktx", Texture.class));
		splashLogoSP.setSize(706, 230);
		nowLoadingFirstAssets = false;
		//ロード画面以外のアセットロード
		loadAssetsSecond();
		if(nowScreenState != ScreenState.SCREEN_OFFLINE) nowScreenState = ScreenState.SCREEN_SPLASH;
	}
	private void doneLoadingSecondAssets() {
		// enable texture filtering for pixel smoothing
//		TextureAtlas atlas = assets.get("data/tex_parts.txt");
//		for (Texture t : atlas.getTextures()) {
//			t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
//		}
//		assetObj = new AssetObject(atlas);
		assetObj = new AssetObject((TextureAtlas) assets.get("data/tex_parts.txt"));
		//sprite
		initializeSprite();
		thumbsMask = assets.get("data/thumbnail_mask.etc1", Texture.class);
		parts1Mask = assets.get("data/tex_parts1_mask.ktx", Texture.class);
		wordBMask = assets.get("data/tex_word_b_mask.etc1", Texture.class);
		downBGMask = assets.get("data/downloadtheme_bg_mask.ktx", Texture.class);
//		thumbsMaskSP = new Sprite(new TextureRegion(thumbsMask,0f,0f,THUMBS_U,THUMBS_V));
//		thumbsMaskSP.setScale(1f);
//		thumbsMaskSP.setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
//		thumbsMaskSP.setOrigin(THUMBS_TEX_SIZE / 2f, THUMBS_TEX_SIZE / 2f);
		quickPreviewMaskSP = new Sprite(assets.get("data/preview_mask.png", Texture.class));
		quickPreviewMaskSP.setScale(1f);
		quickPreviewMaskSP.setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
		quickPreviewMaskSP.setOrigin(THUMBS_TEX_SIZE / 2f, THUMBS_TEX_SIZE / 2f);
//		quickPreviewFlashSP = new Sprite(assets.get("data/preview_thumbsflash.etc1", Texture.class));
		quickPreviewFlashSP = new Sprite(assets.get("data/mat_white.etc1", Texture.class));
		quickPreviewFlashSP.setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);

		circleMask = assets.get("data/circleball_mask.etc1", Texture.class);
		circleMaskSP = new Sprite(assets.get("data/circle_mask.etc1", Texture.class));
		circleMaskSP.setSize(162, 162);
		circleMaskSP.setOrigin(81,81);
		matWhiteSP = new Sprite(assets.get("data/mat_white.etc1", Texture.class));
		matBlackSP = new Sprite(assets.get("data/mat_black.etc1", Texture.class));
		matWhiteSP.setSize(1080, realHeight);
		matBlackSP.setSize(162, 162);
//		matWhiteSP.setOrigin(81,81);
		matBlackSP.setOrigin(81,81);
		bgMaskSprite = new Sprite(assets.get("data/bg_mat.etc1", Texture.class));
		bgMaskSprite.setSize(1080, realHeight);
//		frameBufferMask = new FrameBuffer(Format.RGB565, 1024, 2048, false);
		frameBufferMask = new FrameBuffer(Format.RGB565, 512, 1024, false);
		frameBufferCash = new FrameBuffer(Format.RGB565, 1024, 2048, false);
		cashScreenSP = new Sprite(frameBufferCash.getColorBufferTexture());
		cashScreenSP.setSize(1080, realHeight);
		cashScreenSP.flip(false, true);
		maskScreenSP = new Sprite(frameBufferMask.getColorBufferTexture());
		maskScreenSP.setSize(1080, realHeight);
		maskScreenSP.flip(false, true);
		borderSP = new Sprite(assets.get("data/menu_line.etc1", Texture.class));
		borderSP.setSize(1080, 16);
		sortBgRG = new TextureRegion(assets.get("data/sort_anim.png", Texture.class));
		sortBgTopSP = new Sprite(assets.get("data/sort_anim.png", Texture.class));
		sortBgTopSP.setSize(256, 128);
		sortBgTopSP.setOrigin(128, 0);
		sortBgBottomSP = new Sprite(assets.get("data/sort_anim.png", Texture.class));
		sortBgBottomSP.flip(false, true);
		sortBgBottomSP.setSize(256, 128);
		sortBgBottomSP.setOrigin(128, 128);
		reloadWordSP = new Sprite(assets.get("data/reload_thumbnail.etc1", Texture.class));
		reloadWordSP.setSize(256, 128);
		infoListSP = new Sprite(assets.get("data/mat_white.etc1", Texture.class));
		infoListSP.setSize(1080, 140);
		popupBottomSP = new Sprite(assets.get("data/popup_bottom.ktx", Texture.class));
//		popupBottomSP.setSize(1052, 64);
		popupBottomSP.setSize(1080, 64);
		popupTopSP = new Sprite(assets.get("data/popup_top.ktx", Texture.class));
//		popupTopSP.setSize(1052, 64);
		popupTopSP.setSize(1080, 64);
		matGlaySP = new Sprite(assets.get("data/mat_glay.etc1", Texture.class));
		popupLeftRG = new TextureRegion(assets.get("data/popup_left.ktx", Texture.class));
		popupRightRG = new TextureRegion(assets.get("data/popup_right.ktx", Texture.class));

		//予約があればここでチュートリアル表示
		if(reserveFirstTutrial) initializeTutrial(true);

		nowLoadingSecondAssets = false;

		DebugLog.instance.outputLog("info", "setScreenSize保存　in doneLoadingSecondAssets");
		setScreenSize = true;//TODO スクリーンの高さを保存するタイミングをずらす(DoneLoading後)
	}
	void loadingThumbs(int thumbsNum,String thumbsName){
		//解放
		if (!requesUnloadPath[thumbsNum].equals("")) {
			DebugLog.instance.outputLog("check", "データ解放：" + requesUnloadPath[thumbsNum] + "：" + thumbsNum + "番のサムネイル：" + useManagerNum[thumbsNum] + "番のマネージャー使用");
			assetsMulti[useManagerNum[thumbsNum]].unload(requesUnloadPath[thumbsNum]);
			requesUnloadPath[thumbsNum] = "";
		}
		useManagerNum[thumbsNum] = managerCount;
		nowLoadingPath[thumbsNum] = "thumbnails/" + thumbsName + ".etc1";
		DebugLog.instance.outputLog("check","パス：" + nowLoadingPath[thumbsNum]);
		assetsMulti[useManagerNum[thumbsNum]].load(nowLoadingPath[thumbsNum],Texture.class,paramRepeat);
		nowLoadingThumbs[thumbsNum] = true;
		nowChangingThumbs[thumbsNum] = true;
		managerCount++;
		if(MANAGER_MAX<=managerCount) managerCount=0;
	}
	void doneLoadingThumbs(int thumbsNum){
		DebugLog.instance.outputLog("check","doneLoadingThumbs：" + thumbsNum + "番のサムネイル"+ useManagerNum[thumbsNum] + "番のマネージャー使用");
		thumbsSprite[thumbsNum].setTexture(assetsMulti[useManagerNum[thumbsNum]].get(nowLoadingPath[thumbsNum],Texture.class));
    	thumbsSpriteText[thumbsNum].setTexture(assetsMulti[useManagerNum[thumbsNum]].get(nowLoadingPath[thumbsNum],Texture.class));
		requesUnloadPath[thumbsNum] = nowLoadingPath[thumbsNum];
		nowLoadingThumbs[thumbsNum] = false;
		nowChangingThumbs[thumbsNum] = false;
	}
	void loadingThumbsDetail(int thumbsNum, int thumbInfoIndex){
		DebugLog.instance.outputLog("check","thumbsNum：" + thumbsNum + "番のサムネイル" + useManagerNumDetail[thumbsNum] + "番のマネージャー使用");
		//解放
		if (!requesUnloadPathDetail[thumbsNum].equals("")) {
			DebugLog.instance.outputLog("check", "データ解放Detail：" + requesUnloadPathDetail[thumbsNum] + "：" + thumbsNum + "番のサムネイル：" + useManagerNumDetail[thumbsNum] + "番のマネージャー使用");
			assetsMultiDetail[useManagerNumDetail[thumbsNum]].unload(requesUnloadPathDetail[thumbsNum]);
			requesUnloadPathDetail[thumbsNum] = "";
		}
			nowThumbInfoIndex[thumbsNum] = thumbInfoIndex;
			useManagerNumDetail[thumbsNum] = managerCountDetail;
			nowLoadingPathDetail[thumbsNum] = thumbInfoArray.get(thumbInfoIndex).getThumbsPathForLoad(myActivity.getApplicationContext());
			assetsMultiDetail[useManagerNumDetail[thumbsNum]].load(nowLoadingPathDetail[thumbsNum],Texture.class,param);
			nowLoadingThumbsDetail[thumbsNum] = true;
			nowChangingThumbsDetail[thumbsNum] = true;
			managerCountDetail++;
			if(MANAGER_MAX_DETAIL<=managerCountDetail) managerCountDetail=0;
	}
	void doneLoadingThumbsDetail(int thumbsNum){
		DebugLog.instance.outputLog("check","doneLoadingThumbsDetail：" + thumbsNum + "番のサムネイル" + useManagerNumDetail[thumbsNum] + "番のマネージャー使用");
		thumbsSpriteDetail[thumbsNum].setTexture(assetsMultiDetail[useManagerNumDetail[thumbsNum]].get(nowLoadingPathDetail[thumbsNum],Texture.class));
		requesUnloadPathDetail[thumbsNum] = nowLoadingPathDetail[thumbsNum];
		nowLoadingThumbsDetail[thumbsNum] = false;
		nowChangingThumbsDetail[thumbsNum] = false;
	}
	void calcThumbsOrder(int texNum , boolean moveUnder){
		for(int i=0;i<thumbsTexKazu;i++){
			if(i==0){
				if(moveUnder) thumbsOrder[i]=(texNum+1<thumbsTexKazu)?texNum+1 : texNum-(thumbsTexKazu-1);
				else thumbsOrder[i]=texNum;
			}
			else thumbsOrder[i]=(thumbsOrder[i-1]+1<thumbsTexKazu)?thumbsOrder[i-1]+1 : thumbsOrder[i-1]-(thumbsTexKazu-1);
		}
	}
	void calcThumbsOrderDetail(int texNum , boolean moveRight){
		for(int i=0;i<thumbsTexKazuDetail;i++){
			if(i==0){
				if(moveRight) thumbsOrderDetail[i]=(texNum+1<thumbsTexKazuDetail)?texNum+1 : texNum-(thumbsTexKazuDetail-1);
				else thumbsOrderDetail[i]=texNum;
			}
			else thumbsOrderDetail[i]=(thumbsOrderDetail[i-1]+1<thumbsTexKazuDetail)?thumbsOrderDetail[i-1]+1 : thumbsOrderDetail[i-1]-(thumbsTexKazuDetail-1);
		}
	}
	@Override
	public void dispose() {
		DebugLog.instance.outputLog("check", "dispose screen!!!!!!!!!!!!!!");
		//メモリーケア
//		1st
		if(!nowLoadingFirstAssets){
			defaultShader.dispose();
			maskETCShader.dispose();
		}
//		2nd
		if(!nowLoadingSecondAssets){
			frameBufferCash.dispose();
			frameBufferMask.dispose();
		}
		batch.dispose();
		assets.dispose();
		assetsTutrial.dispose();
		for(int i=0;i<MANAGER_MAX;i++){
			assetsMulti[i].dispose();
		}
		for(int i=0;i<MANAGER_MAX_DETAIL;i++){
			assetsMultiDetail[i].dispose();
		}
	}
	@Override
	public void pause() {
		DebugLog.instance.outputLog("info", "pause screen!!!!!!!!!!!!!!");
		//tutrialをここで解放
		if(!nowLoadingAssetsTutrial && nowScreenState != ScreenState.SCREEN_TUTRIAL){
			DebugLog.instance.outputLog("check", "pause tutrial解放!!!!!!!!!!!!!!");
			RereaseTutrialAssets();
		}
	}
	void setDefaultShader(){
		if (nowUseETCShader) {
			batch.setShader(defaultShader);
			nowUseETCShader = false;
		}
	}
	void setETCShader(){
		if (!nowUseETCShader) {
			//必要な機種だけ
			if (isOS404) ETCShaderSet();
			batch.setShader(maskETCShader);
			nowUseETCShader = true;
		}
	}
	@Override
	public void render() {
//		//ここで念の為にやるのが一番確実
//		if(!finishTouchSet){
//			if(finishBannerLoading && !nowLoadingSecondAssets){
//				finishTouchSet = true;
//		        InputMultiplexer multiplexer = new InputMultiplexer();
//		        multiplexer.addProcessor(new MyInputListener());
//		        multiplexer.addProcessor(new GestureDetector(new MyGestureListener()));
//				Gdx.input.setInputProcessor(multiplexer);
//				DebugLog.instance.outputLog("info", "InputMultiplexer　セット　in render");
//			}
//		}

		delta = Gdx.graphics.getDeltaTime();
		if (nowLoadingFirstAssets){
			 if(assets.update()) doneLoadingFirstAssets();
		}
		if (nowLoadingSecondAssets){
			 if(assets.update()) doneLoadingSecondAssets();
		}
		for(int i=0;i<THUMBSTEXMAX;i++){
			if(nowLoadingThumbs[i]){
				if(assetsMulti[useManagerNum[i]].update()){
					doneLoadingThumbs(i);
				}
			}
		}
		for(int i=0;i<THUMBSTEXMAX_DETAIL;i++){
			if(nowLoadingThumbsDetail[i] ){
				boolean updateSuccess =false;
				try{
					updateSuccess = assetsMultiDetail[useManagerNumDetail[i]].update();
				}
				catch(GdxRuntimeException e){
					//失敗
					DebugLog.instance.outputLog("Async","失敗!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!==" + i);
					DebugLog.instance.outputLog("Async","失敗!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!==" + thumbInfoArray.get(nowThumbInfoIndex[i]).getFileName());
					FileUtility.delFile(new File(thumbInfoArray.get(nowThumbInfoIndex[i]).getThumbsPath(myActivity.getApplicationContext())));
					failedSaveDetailImage(nowThumbInfoIndex[i]);
					nowLoadingThumbsDetail[i] = false;
					updateSuccess = false;
				}
				if(updateSuccess){
					doneLoadingThumbsDetail(i);
				}
			}
		}

		batch.setProjectionMatrix(camera.combined);
		setDefaultShader();
		// FPS計測3秒に1回
		cnt += delta;
		if (3f < cnt) {
			DebugLog.instance.outputLog("flamerate","FPS=" + Gdx.graphics.getFramesPerSecond());
			cnt = 0f;
		}
		Gdx.gl20.glClearColor(BG_GRAY.r, BG_GRAY.g, BG_GRAY.b, 1);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

//		DebugLog.instance.outputLog("loop", "render!!!!!!!!!!!!!!");

//		if(NowTransition) DrawCircleMask();
		switch(nowScreenState){
		case SCREEN_OFFLINE:
			if(!nowLoadingSecondAssets) DrawOffline();
			break;
		case SCREEN_SPLASH:
			DrawSplash();
			break;
		case SCREEN_RECOMEND:
			DrawRecomend();
			break;
		case SCREEN_NEWART:
		case SCREEN_RUNKING:
			DrawListScreen();
			break;
		case SCREEN_MYBOX:
			DrawMyBox();
			break;
		case SCREEN_DETAIL:
			DrawDetail();
			break;
		case SCREEN_INFO:
			DrawInfo();
			break;
		case SCREEN_TUTRIAL:
			DrawTutrial();
			break;
		default://NOMAL
			break;
		}
//		if(nowAnimationState == AnimationState.ANIM_DETAIL_CLOSE) DrawDetailCloseAnim();
//		if(!nowLoadingSecondAssets) DrawDownloadThemeMask();
		if(nowSettingThemes){//TODO 0215
			if(nowCenterDetailType == ContentsFileName.ThumbDetailIconShortut && state_ellipseBtnRight==1) DrawLoadingMaskIcon();
			else DrawDownloadThemeMask();
		}

		if(visibleDialog) DrawDialog();
//		DrawDialog(false); //プレミアム取得エラー

//		if(nowAnimationState == AnimationState.ANIM_NONE){
//			if(nowTouchState == TouchState.TOUCH_SCROLL || nowTouchState == TouchState.TOUCH_SCROLL_BOUNCE || nowTouchState == TouchState.TOUCH_ONLY_MENU
//					|| (nowRecomendState == RecomendState.PEVIEW_MODE && nowTouchState != TouchState.TOUCH_ONLY_DIALOG)) menuBtnTapOK = true;
//		}
		if(nowTouchState == TouchState.TOUCH_SCROLL || nowTouchState == TouchState.TOUCH_SCROLL_BOUNCE || nowTouchState == TouchState.TOUCH_ONLY_MENU
				|| (nowRecomendState == RecomendState.PEVIEW_MODE && nowTouchState != TouchState.TOUCH_ONLY_DIALOG)) menuBtnTapOK = true;
		else menuBtnTapOK = false;
	}
	void DrawOffline(){
		//ここにも来る可能性あるーー20150909
		if(!finishTouchSet){
			if(finishBannerLoading && !nowLoadingSecondAssets){
				finishTouchSet = true;
		        InputMultiplexer multiplexer = new InputMultiplexer();
		        multiplexer.addProcessor(new MyInputListener());
		        multiplexer.addProcessor(new GestureDetector(new MyGestureListener()));
				Gdx.input.setInputProcessor(multiplexer);
				DebugLog.instance.outputLog("info", "InputMultiplexer　セット　in DrawOffline");
			}
		}
		if(!visibleDialog){
			saveTouchState= nowTouchState;
			nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
			visibleDialog = true;
		}
		batch.begin();
		bgSprite.draw(batch);
		blackMaskSP.draw(batch);
		batch.end();
		DrawDialog();
	}
//	void DrawDialog(){
//		DrawDialog(true);
//	}
	void DrawDialog(){
		batch.begin();
		setETCShader();
		parts1Mask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		parts1Mask.bind(0);
		assetObj.dialog_bg.setPosition(33, centerY-311);
		assetObj.dialog_bg.draw(batch);
		assetObj.ellipseBtn[1].setPosition(344, centerY-201);
		assetObj.ellipseBtn[1].draw(batch);
		batch.flush();

		wordBMask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		wordBMask.bind(0);
		assetObj.word_ok.setPosition(341, centerY-201);
		assetObj.word_ok.draw(batch);
		if(isPremiunError){
			assetObj.word_premium_error.setPosition(33, centerY-311);
			assetObj.word_premium_error.draw(batch);
		}
		else{
			assetObj.word_network_error.setPosition(33, centerY-311);
			assetObj.word_network_error.draw(batch);
		}
		batch.end();
	}
	void DrawLoadingMask(){
		DrawLoadingMask(false);
	}
	void DrawDownloadThemeMask(){
//		DebugLog.instance.outputLog("loop","DrawLoadingMask=" + isDownload);
		batch.begin();
		setDefaultShader();
		blackMaskSP.draw(batch);
		batch.flush();

		setETCShader();
//		parts1Mask.bind(1);
//		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
//		parts1Mask.bind(0);
		//TODO
		downBGMask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		downBGMask.bind(0);
		assetObj.downloadDialog.setPosition(214, centerY-120);
		assetObj.downloadDialog.draw(batch);
		batch.flush();
		wordBMask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		wordBMask.bind(0);
		assetObj.word_downloading.setPosition(444, centerY-31);
		assetObj.word_downloading.draw(batch);
		batch.flush();
		setDefaultShader();
		loadingSP.setPosition(300, centerY-59);//
		loadingSP.rotate(delta*LOADING_ANIM_SPEED);
		loadingSP.draw(batch);
		batch.end();
	}
	void DrawLoadingMaskIcon(){
		batch.begin();
		setDefaultShader();
		blackMaskSP.draw(batch);
		loadingSP.setPosition(481, centerY-59);
		loadingRotaMask += delta*LOADING_ANIM_SPEED;
		loadingSP.setRotation(loadingRotaMask);
//		loadingSP.rotate(delta*LOADING_ANIM_SPEED);
		loadingSP.draw(batch);
		batch.end();
	}
	void DrawLoadingMask(boolean isSplash){
//		DebugLog.instance.outputLog("loop","DrawLoadingMask=" + isDownload);
//		if(isDownload) batch.begin();
		setDefaultShader();
		if(isSplash){
//			float pos =
//			loadingSmallSP.setPosition(508, centerY-59-72);
			loadingRotaMask += delta*LOADING_ANIM_SPEED;
			loadingSmallSP.setRotation(loadingRotaMask);
			loadingSmallSP.draw(batch);
		}
		else{
			blackMaskSP.draw(batch);
			loadingSP.setPosition(481, centerY-59);
			loadingRotaMask += delta*LOADING_ANIM_SPEED;
			loadingSP.setRotation(loadingRotaMask);
//			loadingSP.rotate(delta*LOADING_ANIM_SPEED);
			loadingSP.draw(batch);
		}
//		loadingRotaMask += delta*LOADING_ANIM_SPEED;
//		loadingSP.setRotation(loadingRotaMask);
//		loadingSP.draw(batch);
	}
	void DrawSplash(){
		batch.begin();
		//topY-高さで上合わせ
		bgSprite.draw(batch);
		splashLogoSP.setPosition(187, centerY-115);
		splashLogoSP.draw(batch);
		DrawLoadingMask(true);
		batch.end();
		if(!nowLoadingSecondAssets && LoadingAppData){
			DebugLog.instance.outputLog("api", "スプラッシュ終了!!!!!!!!!!!!!!!!");
			initializeRecomendScreen();
		}
	}
	//各サムネイルの移動目標値算出
//	直交座標A(x,y)とB(x2,y2)の間の距離を求める関数
	float getDistance(float x, float y, float x2, float y2) {
	    double distance = Math.sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
	    return (float) distance;
	}
//	ABの角度を求める関数
	double getRadian(double x, double y, double x2, double y2) {
	    double radian = Math.atan2(x2 - x, y2 - y);
	    return radian;
	}
	//おすすめシャッフル時、必要な画像読むための数取得
	int getAnimThumbsVolume(){
		int vol = 0;
		for(int i=0;i<thumbsTotal;i++){
			if((-THUMBS_ZONE_HEIGHT_RECOMEND< thumbsY[i] + ZOOMPM_REC  && thumbsY[i] - ZOOMPM_REC <realHeight)){
//				vol++;
				vol=i+1;
			}
		}
//		DebugLog.instance.outputLog("info", "getAnimThumbsVolume vol= " + vol);
		return vol;
	}
	//遷移時、必要な画像読むための数取得
	int getVisibleThumbsVolume(boolean isList){
		int vol = 0;
		for(int i=0;i<thumbsTexKazu;i++){
			if(isList){
				if(-THUMBS_ZONE_HEIGHT_LIST< thumbsY[i]  && thumbsY[i] <realHeight) vol++;
			}
			else{
				if(-THUMBS_ZONE_HEIGHT_MATRIX< thumbsY[i]  && thumbsY[i] <realHeight) vol++;
			}
		}

		DebugLog.instance.outputLog("info", "getVisibleThumbsVolume vol= " + vol);
		return vol;
	}
	void initializeOpenningAnim(int ptn){
		//近いものから中心に向かう為の計測
		if(shuffleDto!=null) shuffleDto.clear();
		colorBallNum.shuffle();
		int colIndex = 0;
//		for(int i=0;i<thumbsTotal;i++){
//			if((-THUMBS_ZONE_HEIGHT_RECOMEND< thumbsY[i] + ZOOMPM_REC  && thumbsY[i] - ZOOMPM_REC <realHeight)){
		for(int i=0;i<shuffleSize;i++){
				float dist = getDistance(centerTarget.x,centerTarget.y,thumbsX[i],thumbsY[i]);
				DebugLog.instance.outputLog("flamework", "dist= " + dist);
				if(ptn==0){
					double theta = Math.atan2(centerTarget.y-thumbsY[i],centerTarget.x-thumbsX[i]);
					DebugLog.instance.outputLog("flamework", "i= " + i);
					DebugLog.instance.outputLog("flamework", "theta= " + theta);
					thumbsRePreTheta[i] = theta;
					thumbsReStartX[i] = thumbsX[i];
					thumbsReStartY[i] = thumbsY[i];
					emPreviewMove[i].ResetPosition(0f,  thumbsRecomendScale[i] , ZOOMIN_BALL,true);//カラーボールズームインに使う
					emThumbsZoom[i].ResetPosition(thumbsRecomendScale[i], 0,ZOOMOUT_BALL);//カラーボールズームアウトに使う
				}
				else if(ptn==1){
					emPreviewMove[i].ResetPosition(0f,  thumbsRecomendScale[i]*1.15f , ZOOMIN_BALL*0.35f);//カラーボールズームインに使う
					emThumbsZoom[i].ResetPosition(thumbsRecomendScale[i]*1.15f, 0,ZOOMOUT_BALL);//カラーボールズームアウトに使う
				}
				else{
					//実際にサムネイルが裏にあるボール
					dist = thumbsRecomendY[i];
					float kyori = thumbsRecomendY[25]+THUMBS_TEX_SIZE*2;
//					emPreviewMove[i].ResetPosition(topY+THUMBS_TEX_SIZE, topY-thumbsRecomendY[i], MOVETIME_FALL*2f);//カラーボールの動きに使う
					emPreviewMove[i].ResetPosition(topY-thumbsRecomendY[i]+kyori, topY-thumbsRecomendY[i], FALLTIME_SA3);//カラーボールの動きに使う
					emThumbsZoom[i].ResetPosition(thumbsRecomendScale[i],0,ZOOMTIME_SA3);//カラーボールズームアウトに使う
					thumbsY[i] = topY+THUMBS_TEX_SIZE;
					thumbsBallScale[i] = thumbsRecomendScale[i];
				}

				ShuffleDto sto = new ShuffleDto(i,dist,colorBallNum.get(colIndex));
				shuffleDto.add(sto);
				colIndex++;
				if(9<=colIndex) colIndex=0;
//		}
//			}
		}
		//シャッフル3だけはカラーボール余計に
		if(ptn==2){
			for(int i=shuffleSize;i<shaffleBallTotal;i++){
//				emPreviewMove[i].ResetPosition(topY+THUMBS_TEX_SIZE, -THUMBS_TEX_SIZE*i, MOVETIME_FALL*5f);//カラーボールの動きに使う
				float kyori = thumbsRecomendY[25]+THUMBS_TEX_SIZE*2;
				emPreviewMove[i].ResetPosition(topY-thumbsRecomendY[i]+kyori, topY-thumbsRecomendY[i], FALLTIME_SA3);//カラーボールの動きに使う
				thumbsY[i] = topY+THUMBS_TEX_SIZE;
				thumbsX[i] = thumbsRecomendX[i];
				thumbsBallScale[i] = thumbsRecomendScale[i];
				float dist = thumbsRecomendY[i];
				ShuffleDto sto = new ShuffleDto(i,dist,colorBallNum.get(colIndex));
				shuffleDto.add(sto);
				colIndex++;
				if(9<=colIndex) colIndex=0;
			}
			for(int i=0;i<shaffleBallTotal;i++){
				DebugLog.instance.outputLog("flamework", "distCheckNum.get(i).num= " + shuffleDto.get(i).num);
				DebugLog.instance.outputLog("flamework", "distCheckNum.get(i).dist= " + shuffleDto.get(i).dist);
			}
		}
//		shuffleSize = shuffleDto.size;
		DebugLog.instance.outputLog("flamework", "shuffleSize= " + shuffleSize);
		//距離順にソート
		shuffleDto.sort(new DistanceComparator());
		for(int i=0;i<shuffleSize;i++){
			DebugLog.instance.outputLog("flamework", "distCheckNum.get(i).num= " + shuffleDto.get(i).num);
			DebugLog.instance.outputLog("flamework", "distCheckNum.get(i).dist= " + shuffleDto.get(i).dist);
		}
		nowTouchState = TouchState.TOUCH_DIABLE;
		shuffle1state = 0;
		shuffle2state = 0;
		shuffle3state = 0;
		if(ptn==0){
			shuffle1state = 1;
			nowRecomendState = RecomendState.SHUFFLE_ANIM1;
		}
		else if(ptn==1){
			shuffle2state = 1;
			nowRecomendState = RecomendState.SHUFFLE_ANIM2;
		}
		else{
			shuffle3state = 1;
			nowRecomendState = RecomendState.SHUFFLE_ANIM3;
		}
	}
	//前回と被らないものを返す
	int GetShuffleAnimPattern(){
//		if(shuffleNumArray!=null) shuffleNumArray.clear();
//		int ptn = reShuffleAnim;
//		while(ptn==reShuffleAnim){
//			ptn = rnd.nextInt(3);
//		}
//		reShuffleAnim = ptn;
//		DebugLog.instance.outputLog("info", "ptn= " + ptn);
////		return ptn;
//		return 2;
//		shuffleCount++;
		if(2<shuffleCount){
			shuffleNumArray.shuffle();
			while(shuffleNumArray.get(0)==reShuffleAnim){
			shuffleNumArray.shuffle();
			DebugLog.instance.outputLog("info", "Shuffle!!!!!!!!!!!! ");
			}
			shuffleCount = 0;
		}
		reShuffleAnim = shuffleNumArray.get(shuffleCount);
		shuffleCount++;
		DebugLog.instance.outputLog("info", "ShuffleAnim= " + reShuffleAnim);
		return reShuffleAnim;
	}
	void initializeShuffleAnim(int ptn){

		//近いものから中心に向かう為の計測
		if(shuffleDto!=null) shuffleDto.clear();
		for(int i=0;i<thumbsTotal;i++){
			if((-THUMBS_ZONE_HEIGHT_RECOMEND< thumbsY[i] + ZOOMPM_REC  && thumbsY[i] - ZOOMPM_REC <realHeight)){
				float dist = getDistance(centerTarget.x,centerTarget.y,thumbsX[i],thumbsY[i]);
				if(ptn==0){
					double theta = Math.atan2(centerTarget.y-thumbsY[i],centerTarget.x-thumbsX[i]);
					DebugLog.instance.outputLog("flamework", "i= " + i);
					DebugLog.instance.outputLog("flamework", "theta= " + theta);
					thumbsRePreTheta[i] = theta;
					thumbsReStartX[i] = thumbsX[i];
					thumbsReStartY[i] = thumbsY[i];
					emThumbsZoom[i].ResetPosition(thumbsRecomendScale[i], 0, ZOOMTIME_ZOOM);
					emPreviewMove[i].ResetPosition(0, dist , MOVETIME_CENTER);
				}
				else if(ptn==1){
					emThumbsZoom[i].ResetPosition(thumbsRecomendScale[i], 0, ZOOMTIME_ZOOM*1.75f);
				}
				else if(ptn==2){
					dist = topY-thumbsY[i];
					DebugLog.instance.outputLog("flamework", "i= " + i);
					DebugLog.instance.outputLog("flamework", "distFtop= " + dist);
					emThumbsZoom[i].ResetPosition(thumbsRecomendScale[i],thumbsRecomendScale[i]*THUMBS_SMALL, ZOOMTIME_ZOOM*2f);//zoom
					emPreviewMove[i].ResetPosition(thumbsY[i], thumbsY[i]-realHeight-THUMBS_TEX_SIZE , MOVETIME_FALL);//move
				}
				ShuffleDto sto = new ShuffleDto(i,dist);
				shuffleDto.add(sto);
			}
		}
		shuffleSize = shuffleDto.size;
		DebugLog.instance.outputLog("flamework", "shuffleSize= " + shuffleSize);
		//距離順にソート
		shuffleDto.sort(new DistanceComparator());
		for(int i=0;i<shuffleSize;i++){
			DebugLog.instance.outputLog("flamework", "distCheckNum.get(i).num= " + shuffleDto.get(i).num);
			DebugLog.instance.outputLog("flamework", "distCheckNum.get(i).dist= " + shuffleDto.get(i).dist);
		}
		nowTouchState = TouchState.TOUCH_DIABLE;
		shuffle1state = 0;
		shuffle2state = 0;
		shuffle3state = 0;
		if(ptn==0){
			shuffle1state = 0;
			nowRecomendState = RecomendState.SHUFFLE_ANIM1;
		}
		else if(ptn==1){
			shuffle2state = 0;
			nowRecomendState = RecomendState.SHUFFLE_ANIM2;
		}
		else{
			shuffle3state = 0;
			nowRecomendState = RecomendState.SHUFFLE_ANIM3;
		}
	}
	void finishShaffleAnim(){
		DebugLog.instance.outputLog("flamework", "SHUFFLE_ANIM1アニメ終了!!!!!!!!!!!!!!!" + nowTouchState);
		nowAnimationState = AnimationState.ANIM_NONE;
		reserveAnimationState = nowAnimationState;
		nowRecomendState = RecomendState.NOMAL_MODE;
		nowTouchState = TouchState.TOUCH_SCROLL;
		//残りを読み込む
		if(!finisedLoadRemain) loadRemainThumbs();
	}
	int addDistNum = 0;
	float addDist = 0;
	void calcTargetREC(int previewNum,boolean zoomIn){
		addDistNum = 0;
		addDist = 0;
		//画面見切れてる際の補正値
		float addX = 0;
		float addY = 0;
//下のものから計算
		if(zoomIn){//zoom時
			//X--30が確保領域
			if(thumbsX[previewNum] - ZOOMPM_REC - 30<0){
				addX= (thumbsX[previewNum] - ZOOMPM_REC)*-1f + 30;
//				DebugLog.instance.outputLog("info", "左オーバー！！！ = " + addX);
			}
			else if(1080-THUMBS_TEX_SIZE-ZOOMPM_REC-30<thumbsX[previewNum] ){
				addX= 1080-THUMBS_TEX_SIZE-ZOOMPM_REC-30 - thumbsX[previewNum] ;
//				DebugLog.instance.outputLog("info", "右オーバー！！！ = " + addX);
			}
			if(thumbsY[previewNum] - ZOOMPM_REC-BOTTOM_MARGINE<0){//下
				addY= (thumbsY[previewNum] - ZOOMPM_REC-BOTTOM_MARGINE)*-1f;
//				DebugLog.instance.outputLog("info", "下オーバー！！！ = " + addY);
			}
			else if(topY<thumbsY[previewNum] + THUMBS_TEX_SIZE +ZOOMPM_REC + REC_PREVIEW_MARGINE + TOP_MARGINE){//上
//				if(previewNum%2 == 0) addY= topY-(thumbsY[previewNum] + THUMBS_TEX_SIZE + ZOOMPM_REC + REC_PREVIEW_MARGINE + TOP_MARGINE);
//				else  addY= topY-(thumbsY[previewNum] + THUMBS_TEX_SIZE + ZOOMPM_REC + REC_PREVIEW_MARGINE);
				//右も同じ条件
				addY= topY-(thumbsY[previewNum] + THUMBS_TEX_SIZE + ZOOMPM_REC + REC_PREVIEW_MARGINE + TOP_MARGINE);
//				DebugLog.instance.outputLog("info", "上オーバー！！！ = " + addY);
			}
			float addZoomTime = (float) Math.min(Math.sqrt(addX*addX+addY*addY)/4000,0.2);
			DebugLog.instance.outputLog("value", "addZoomTime = " + addZoomTime);
			int pos = previewNum%4;
			for(int i=thumbsTotal-1;i>=0;i--){
				float distX =thumbsRecomendX[i] -  thumbsRecomendX[previewNum];
				float distY =  thumbsRecomendY[previewNum] - thumbsRecomendY[i];
				float dist = REC_ZOOM_DIST;
				if(i==previewNum) dist = 0;
//				//縦は強制確保
				//縦のやつ//2の倍数
				if(i<previewNum && previewNum%2 == i%2) {
					DebugLog.instance.outputLog("info", "縦のやつ = " + i);
					if(addDistNum==0){//1回だけ
						float plus = 620;
						if(thumbsRecomendScale[previewNum] == REC_SCALE_BIG) plus = 570;//大きさによって距離変える
						addDist = plus - (thumbsRecomendY[previewNum]-thumbsRecomendY[i]);
						addDistNum = i;
					}
					dist += addDist;
				}
				//横のやつ
				//位置によって移動距離調整
				switch(pos){
				case 0://一個前の距離調整
					if(i == previewNum+1){
//						DebugLog.instance.outputLog("info", "調整！！！ = " + i);
						dist  += (580 - (thumbsRecomendX[i] -  thumbsRecomendX[previewNum]));
					}
					break;
				case 1://一個前の距離調整
					if(i == previewNum-3){
//						DebugLog.instance.outputLog("info", "調整！！！ = " + i);
						dist  += (540 - (thumbsRecomendX[previewNum] - thumbsRecomendX[i]));
					}
					break;
				case 3://一個前の距離調整
					if(i == previewNum-3){
//						DebugLog.instance.outputLog("info", "調整！！！ = " + i);
						dist  += (520 - (thumbsRecomendX[previewNum] - thumbsRecomendX[i]));
					}
//					break;
				}
				//計算
				double theta = Math.atan2(distY, distX);
				double  vectX = Math.cos(theta)*dist + addX;
				double  vectY = Math.sin(theta)*dist + addY;
				dist = (float) Math.sqrt(vectX*vectX+vectY*vectY);
				//移動角度計算 多少ばらつかせる
				double add = 0;
//				if(i!=previewNum) add = rnd.nextInt(200)/1000f*getPM();
				thumbsRePreTheta[i] = Math.atan2(vectY, vectX)+ add;
				emPreviewMove[i].ResetPosition(0, dist , ZOOMTIME_REC+ addZoomTime);
				thumbsReStartX[i] = thumbsX[i];
				thumbsReStartY[i] = thumbsY[i];
			}
			emPreviewZoom.ResetPosition(thumbsRecomendScale[previewNum], REC_SCALE_PREVIEW, ZOOMTIME_REC+ addZoomTime);
			visibleQuickBtn = false;
			emQuickBtnZoom.ResetPosition(0, ONE,ZOOMTIME_REC_BTN);
			//初期化
			//クイックプレビュー
			resetQuickState();
		}
		else{//zoom out
			//tap時点のeaseX,Yの正しい値を計算 //戻るときには確実に正しい値になる
			mEaseY = thumbsY[previewNum] - topY + thumbsRecomendY[previewNum];
			if(mEaseY<0){
				addY= mEaseY*-1f;
			}
			else if(offsetWidth<mEaseY){
				addY= offsetWidth - mEaseY;
			}
//			DebugLog.instance.outputLog("value", "addX = " + addX);
//			DebugLog.instance.outputLog("value", "addY = " + addY);
			float addZoomTime = (float) Math.min(Math.sqrt(addX*addX+addY*addY)/4000,0.2);
			DebugLog.instance.outputLog("value", "addZoomTime = " + addZoomTime);
			//tap時点のeaseX,Yの正しい値を計算 //戻るときには確実に正しい値になる
			mEaseY = thumbsY[previewNum] - topY + thumbsRecomendY[previewNum] + addY;
			mLastY = mEaseY;
			flickYdist =offsetWidth-mLastY;
			for(int i=thumbsTotal-1;i>=0;i--){
				thumbsReStartX[i] = thumbsX[i];
				thumbsReStartY[i] = thumbsY[i];
				//ゴール地点は整列地点
				float targetX = thumbsRecomendX[i];
				float targetY = topY - thumbsRecomendY[i] + mEaseY;
				float dist = getDistance(thumbsX[i], thumbsY[i],targetX, targetY);
				float distX =  targetX-thumbsX[i] ;
				float distY =   targetY-thumbsY[i];
				thumbsRePreTheta[i] = Math.atan2(distY, distX);
				emPreviewMove[i].ResetPosition(0 , dist, ZOOMTIME_REC+ addZoomTime);
			}
			emPreviewZoom.ResetPosition(REC_SCALE_PREVIEW, thumbsRecomendScale[previewNum],ZOOMTIME_REC+ addZoomTime);
			emQuickBtnZoom.ResetPosition(ONE,0,ZOOMTIME_REC_BTN);
			//初期化
			//クイックプレビュー

		}
	}
	boolean scrollCheckAbleRec = false;
	//計算用にフィールド変数
	void DrawRecomend(){
		//inputここで--逆にここ以外行けないし
		if(!finishTouchSet){
			if(finishBannerLoading && !nowLoadingSecondAssets){
				finishTouchSet = true;
				DebugLog.instance.outputLog("info", "InputMultiplexer　セット　in DrawRecomend");
		        InputMultiplexer multiplexer = new InputMultiplexer();
		        multiplexer.addProcessor(new MyInputListener());
		        multiplexer.addProcessor(new GestureDetector(new MyGestureListener()));
				Gdx.input.setInputProcessor(multiplexer);
			}
		}
		//スクロールロジック
		if(nowRecomendState == RecomendState.NOMAL_MODE){
			calcScrollFlickEaseRec();
//			int moveCount = 0;
//			int stopCount = 0;
			for(int i=0;i<thumbsTotal;i++){
//			for(int i=thumbsTexKazu-1;i>=0;i--){
				//サムネイルY計算
				float targetY = topY - thumbsRecomendY[i] + mEaseY;
//				float dist = topY - thumbsY[i];
//				//上フリック
//				if(targetY-thumbsReY[i]<0) dist = THUMBS_ZONE_HEIGHT_RECOMEND+thumbsY[i];
				float dist = topY - thumbsY[i];
				//上フリック
				if(targetY-thumbsReY[i]<0) dist = thumbsY[i];;
				float distSP = Math.max(dist*recDelayDistSpeed,recDelayMinSpeed);
				thumbsY[i]+=(targetY-thumbsReY[i])/distSP;
				thumbsReY[i]=thumbsY[i];
				float ajust = (THUMBS_TEX_SIZE - (THUMBS_TEX_SIZE*thumbsScale[i]))*0.5f;
				thumbsTouch[i].x = thumbsX[i]+ajust;
				thumbsTouch[i].y = thumbsY[i]+ajust;
				thumbsTouch[i].width = THUMBS_TEX_SIZE*thumbsRecomendScale[i];
				thumbsTouch[i].height = THUMBS_TEX_SIZE*thumbsRecomendScale[i];
				//tap出来るか判断--遅延があるのでサムネイルごと--後で値は調整するかも
				double checkDistY = Math.abs(targetY-thumbsY[i]);
				if(checkDistY<5) tapAbleRec[i] = true;
				else tapAbleRec[i] = false;
				//scroll出来るか判断--後で値は調整するかも
				//見えてるサムネイルカウント--縦横スクロール制御
//				if((-THUMBS_ZONE_HEIGHT_RECOMEND< thumbsY[i]  && thumbsY[i]<realHeight)){
//					moveCount++;
//					if(checkDistY<=REC_FLICK_LIMIT){
//						stopCount++;
//					}
//				}
			}
//			if(moveCount==stopCount && !Gdx.input.isTouched()) nowRecomendScrollState = RecomendScrollState.SCROLL_ZERO;

		}
		else if(nowRecomendState == RecomendState.PEVIEW_ZOOMIN){
			if(emPreviewZoom.isFinishAnim()){
				thumbsScale[previewNum]  = REC_SCALE_PREVIEW;
				nowRecomendState = RecomendState.PEVIEW_MODE;
				reserveFlickYdist = 0;
				q_waitScroll = 1.2f;//最初の待ち時間は短縮(0.3秒)
				//タッチ領域設定
				for(int i=0;i<thumbsTotal;i++){
					float ajust = (THUMBS_TEX_SIZE - (THUMBS_TEX_SIZE*thumbsScale[i]))*0.5f;
					thumbsTouch[i].x = thumbsX[i]+ajust;
					thumbsTouch[i].y = thumbsY[i]+ajust;
					thumbsTouch[i].width = THUMBS_TEX_SIZE*thumbsRecomendScale[i];
					thumbsTouch[i].height = THUMBS_TEX_SIZE*thumbsRecomendScale[i];
				}
				thumbsTouch[previewNum].x = thumbsX[previewNum]+ZOOMPM_REC;
				thumbsTouch[previewNum].y = thumbsY[previewNum]+ZOOMPM_REC;
				thumbsTouch[previewNum].width = THUMBS_TEX_SIZE*REC_SCALE_PREVIEW;
				thumbsTouch[previewNum].height = THUMBS_TEX_SIZE*REC_SCALE_PREVIEW;
				DebugLog.instance.outputLog("flamework", "nowRecomendState = RecomendState.PEVIEW_MODE");
			}
			else{

				for(int i=0;i<thumbsTotal;i++){
						float rad = emPreviewMove[i].actEaseOutBack(delta);
						thumbsX[i] = (float) (thumbsReStartX[i]+ Math.cos(thumbsRePreTheta[i])*rad);
						thumbsY[i] = (float) (thumbsReStartY[i]+ Math.sin(thumbsRePreTheta[i])*rad);
				}
				//バウンス前に開始
				if(REC_SCALE_PREVIEW<=thumbsScale[previewNum]) visibleQuickBtn = true;
				thumbsScale[previewNum]  = emPreviewZoom.actEaseOutBack(delta);
			}
		}
		else if(nowRecomendState == RecomendState.PEVIEW_ZOOMOUT){
			if(emPreviewZoom.isFinishAnim()){
				//スクロール途中にタップした際のズレを補正
				for(int i=0;i<thumbsTotal;i++){
					thumbsReX[i]=thumbsX[i];
					thumbsReY[i]=thumbsY[i];
				}
				thumbsScale[previewNum]  = thumbsRecomendScale[previewNum];
				thumbsSprite[previewNum].setAlpha(1f);
				thumbsSprite[previewNum].setU(0);
				thumbsSprite[previewNum].setU2(THUMBS_U);
				if(0<=reserveShuffleAnim){
					initializeShuffleAnim(reserveShuffleAnim);
					reserveShuffleAnim = -1;
				}
				else if(0<=reservePrevieZoomNum){
					DebugLog.instance.outputLog("flamework", "予約してたのをZOOM IN！！！！！::" + reservePrevieZoomNum);
    				previewNum = reservePrevieZoomNum;
    				//各サムネイルの移動目標値算出
    				calcTargetREC(previewNum,true);
//    				emPreviewZoom.ResetPosition(ONE, ZOOMPER_REC, ZOOMTIME_REC);
    				nowRecomendState = RecomendState.PEVIEW_ZOOMIN;
    				nowTouchState = TouchState.TOUCH_DIABLE_QUICK;
    				reservePrevieZoomNum = -1;
				}
				else{
					if(reserveFlickYdist!=0){
//			    		mGoalY=offsetWidth-flickYdist;
//			    		if(mGoalY<=-bounceDist) mGoalY=-bounceDist;
//			    		else if(mGoalY>=offsetWidth+bounceDist) mGoalY=offsetWidth+bounceDist;

						flickYdist += reserveFlickYdist;
						if(offsetWidth<flickYdist) flickYdist=offsetWidth;
						else if(flickYdist<0) flickYdist = 0;

						DebugLog.instance.outputLog("flamework", "自動フリック！！！！！！！！！::: reserveFlickYdist::" + reserveFlickYdist);
//						DebugLog.instance.outputLog("touched", "自動フリック！！！！！！！！！::: flickYdist::" + flickYdist);
					}
					nowRecomendState = RecomendState.NOMAL_MODE;
					nowTouchState = TouchState.TOUCH_SCROLL;
				}
				reserveFlickYdist = 0;
				DebugLog.instance.outputLog("flamework", "nowRecomendState = RecomendState.NOMAL_MODE");
			}
			else{
				for(int i=0;i<thumbsTotal;i++){
					float rad;
					//予約あり-スピードアップ
					if(0<=reserveShuffleAnim || 0<=reservePrevieZoomNum) rad = emPreviewMove[i].actEOUTQuadratic(delta*1.6f);
					else rad = emPreviewMove[i].actEaseOutBack(delta);
//					rad = emPreviewMove[i].actEaseOutBack(delta);
					thumbsX[i] = (float) (thumbsReStartX[i]+ Math.cos(thumbsRePreTheta[i])*rad);
					thumbsY[i] = (float) (thumbsReStartY[i]+ Math.sin(thumbsRePreTheta[i])*rad);
				}
				//予約あり
				if(0<=reserveShuffleAnim || 0<=reservePrevieZoomNum) thumbsScale[previewNum]  = emPreviewZoom.actEOUTQuadratic(delta*1.6f);
				else thumbsScale[previewNum]  = emPreviewZoom.actEaseOutBack(delta);
//				thumbsScale[previewNum]  = emPreviewZoom.actEaseOutBack(delta);
			}
		}
		//シャッフルアニメ1
		else if(nowRecomendState == RecomendState.SHUFFLE_ANIM1){
			if(shuffle1state == 0){
				int lastNum = shuffleDto.get(shuffleSize-1).num;
				if(emThumbsZoom[lastNum].isFinishAnim()) {
					thumbsScale[lastNum] = 0f;
					DebugLog.instance.outputLog("flamework", "全部小さくなった");
					initializeRecomendScreen();
				}
				else{
					for(int i=0;i<shuffleSize;i++){
						int n = shuffleDto.get(i).num;
						int beforeNum = 0;
						float rad;
						if(i!=0){
							beforeNum = shuffleDto.get(i-1).num;
							if(0.08f<emPreviewMove[beforeNum].getStatetime()){
								if(emThumbsZoom[n].isFinishAnim()){
									thumbsScale[n] = 0f;
								}
								else{
									rad = emPreviewMove[n].actEaseOutBack(delta);
									thumbsX[n] = (float) (thumbsReStartX[n]+ Math.cos(thumbsRePreTheta[n])*rad);
									thumbsY[n] = (float) (thumbsReStartY[n]+ Math.sin(thumbsRePreTheta[n])*rad);
									if(MOVETIME_CENTER-(MOVETIME_CENTER*0.5f)<emPreviewMove[n].getStatetime()){
										thumbsScale[n] = emThumbsZoom[n].actEIN(delta);
									}
								}
							}
						}
						else{
							if(emThumbsZoom[n].isFinishAnim()){
								thumbsScale[n] = 0f;
							}
							else{
								rad = emPreviewMove[n].actEaseOutBack(delta);
								thumbsX[n] = (float) (thumbsReStartX[n]+ Math.cos(thumbsRePreTheta[n])*rad);
								thumbsY[n] = (float) (thumbsReStartY[n]+ Math.sin(thumbsRePreTheta[n])*rad);
								if(MOVETIME_CENTER-(MOVETIME_CENTER*0.5f)<emPreviewMove[n].getStatetime()){
									thumbsScale[n] = emThumbsZoom[n].actEIN(delta);
								}
							}
						}
					}
				}
			}
			//出現
			else{
				int lastNum = shuffleDto.get(shuffleSize-1).num;
				if(emThumbsZoom[lastNum].isFinishAnim()) {
					thumbsBallScale[lastNum] = 0f;
					finishShaffleAnim();
				}
				else{
					for(int i=0;i<shuffleSize;i++){
						int n = shuffleDto.get(i).num;
						int beforeNum = 0;
						if(i!=0){
							beforeNum = shuffleDto.get(i-1).num;
							if(0.08f<emPreviewMove[beforeNum].getStatetime()){
								if(emThumbsZoom[n].isFinishAnim()){
									thumbsBallScale[n] = 0f;
								}
								else{
									if(emPreviewMove[n].isFinishAnim()){
										thumbsBallScale[n] = emThumbsZoom[n].actEIN(delta);
									}
									else thumbsBallScale[n] = emPreviewMove[n].actEaseOutElastic(delta);
//									else thumbsBallScale[n] = emPreviewMove[n].actEaseOutBounce(delta);
								}
							}
						}
						else{
							if(emThumbsZoom[n].isFinishAnim()){
								thumbsBallScale[n] = 0f;
							}
							else{
								if(emPreviewMove[n].isFinishAnim()){
									thumbsBallScale[n] = emThumbsZoom[n].actEIN(delta);
								}
								else thumbsBallScale[n] = emPreviewMove[n].actEaseOutElastic(delta);
//								else thumbsBallScale[n] = emPreviewMove[n].actEaseOutBounce(delta);
							}
						}
					}
				}
			}
		}
		//シャッフルアニメ2
		else if(nowRecomendState == RecomendState.SHUFFLE_ANIM2){
			if(shuffle2state == 0){
				int lastNum = shuffleDto.get(shuffleSize-1).num;
				if(emThumbsZoom[lastNum].isFinishAnim()) {
					thumbsScale[lastNum] = 0f;
					DebugLog.instance.outputLog("flamework", "全部小さくなった");
					initializeRecomendScreen();
				}
				else{
					for(int i=0;i<shuffleSize;i++){
						int n = shuffleDto.get(i).num;
						int beforeNum = 0;
						if(i!=0){
							beforeNum = shuffleDto.get(i-1).num;
							if(0.16f<emThumbsZoom[beforeNum].getStatetime()){
								if(emThumbsZoom[n].isFinishAnim()){
									thumbsScale[n] = 0f;
								}
								else{
									thumbsScale[n] = emThumbsZoom[n].actEIN(delta);
								}
							}
						}
						else{
							if(emThumbsZoom[n].isFinishAnim()){
								thumbsScale[n] = 0f;
							}
							else{
								thumbsScale[n] = emThumbsZoom[n].actEIN(delta);
							}
						}
					}
				}
			}
			//出現
			else{
				int lastNum = shuffleDto.get(shuffleSize-1).num;
				if(emThumbsZoom[lastNum].isFinishAnim()) {
					thumbsBallScale[lastNum] = 0f;
					finishShaffleAnim();
				}
				else{
					for(int i=0;i<shuffleSize;i++){
						int n = shuffleDto.get(i).num;
						int beforeNum = 0;
						if(i!=0){
							beforeNum = shuffleDto.get(i-1).num;
							if(0.16f<emPreviewMove[beforeNum].getStatetime()){
								if(emThumbsZoom[n].isFinishAnim()){
									thumbsBallScale[n] = 0f;
								}
								else{
									if(emPreviewMove[n].isFinishAnim()){
										thumbsBallScale[n] = emThumbsZoom[n].actEIN(delta);
									}
//									else thumbsBallScale[n] = emPreviewMove[n].actEaseOutElastic(delta);
//									else thumbsBallScale[n] = emPreviewMove[n].actEaseOutBack(delta);
									else thumbsBallScale[n] = emPreviewMove[n].actEOUTQuadratic(delta);
								}
							}
						}
						else{
							if(emThumbsZoom[n].isFinishAnim()){
								thumbsBallScale[n] = 0f;
							}
							else{
								if(emPreviewMove[n].isFinishAnim()){
									thumbsBallScale[n] = emThumbsZoom[n].actEIN(delta);
								}
//								else thumbsBallScale[n] = emPreviewMove[n].actEaseOutElastic(delta);
//								else thumbsBallScale[n] = emPreviewMove[n].actEaseOutBack(delta);
								else thumbsBallScale[n] = emPreviewMove[n].actEOUTQuadratic(delta);
							}
						}
					}
				}
			}
		}
		//シャッフル3
		else if(nowRecomendState == RecomendState.SHUFFLE_ANIM3){
			if(shuffle3state == 0){
				int lastNum = shuffleDto.get(shuffleSize-1).num;
				if(emPreviewMove[lastNum].isFinishAnim()) {
					DebugLog.instance.outputLog("flamework", "全部");
					initializeRecomendScreen();
				}
				else{
					for(int i=0;i<shuffleSize;i++){
						int n = shuffleDto.get(i).num;
						int beforeNum = 0;
						if(i!=0){
							beforeNum = shuffleDto.get(i-1).num;
							if(0.12f<emThumbsZoom[beforeNum].getStatetime()){
								if(emThumbsZoom[n].isFinishAnim()){
									thumbsScale[n] = thumbsRecomendScale[n]*THUMBS_SMALL;
								}
								else{
									thumbsScale[n] = emThumbsZoom[n].actEOUTQuart(delta);
								}
								if(thumbsScale[n]< thumbsRecomendScale[n]*THUMBS_SMALL*1.4f){
									thumbsY[n] = emPreviewMove[n].actEINCubic(delta);//落下
								}
							}
						}
						else{
							if(emThumbsZoom[n].isFinishAnim()){
								thumbsScale[n] = thumbsRecomendScale[n]*THUMBS_SMALL;
							}
							else{
								thumbsScale[n] = emThumbsZoom[n].actEOUTQuart(delta);
							}
							if(thumbsScale[n]< thumbsRecomendScale[n]*THUMBS_SMALL*1.4f){
								thumbsY[n] = emPreviewMove[n].actEINCubic(delta);	//落下
							}
						}
					}
				}
			}
			//出現
			else{
				for(int i=0;i<shaffleBallTotal;i++){
					int n = shuffleDto.get(i).num;
					int beforeNum = 0;
						if(i<shuffleSize){//サムネイルがあるやつ
							beforeNum = shuffleDto.get(i+1).num;
							if(emPreviewMove[0].isFinishAnim()){
								//fallは終了
								finishShaffleAnim();
							}
							if(FALLTIME_SA3*0.02f<emPreviewMove[beforeNum].getStatetime()){
								thumbsY[n] = emPreviewMove[n].actEaseOutBack(delta,ZOOMBACK_BALL);	//落下
							}
//							if(thumbsY[n]<topY*0.5f){
//								thumbsBallScale[n] = emThumbsZoom[n].actEIN(delta);
//							}
							if(FALLTIME_SA3-ZOOMTIME_SA3<emPreviewMove[n].getStatetime()){
								thumbsBallScale[n] = emThumbsZoom[n].actEIN(delta);
							}
						}
						else{
							if(i== shaffleBallTotal-1){//一番最初に落ちるやつ
//								thumbsY[n] = emPreviewMove[n].actEOUT(delta);	//落下
								thumbsY[n] = emPreviewMove[n].actEaseOutBack(delta,ZOOMBACK_BALL);	//落下
							}
							else{
								beforeNum = shuffleDto.get(i+1).num;
//								if(0.06f<emPreviewMove[beforeNum].getStatetime()){
//									thumbsY[n] = emPreviewMove[n].actEOUT(delta);	//落下
								if(FALLTIME_SA3*0.02f<emPreviewMove[beforeNum].getStatetime()){
									thumbsY[n] = emPreviewMove[n].actEaseOutBack(delta,ZOOMBACK_BALL);	//落下
								}
							}
						}
					}
			}
		}
		//描画
		batch.begin();

		if(NowTransition) DrawCircleMask();

		cashStart = false;
		if(nowAnimationState == AnimationState.ANIM_CASH_SCREEN){
			frameBufferCash.begin();
			cashStart = true;
		}
		//bg_mat
		bgSprite.draw(batch);
//		//サムネイル
		if(nowRecomendState == RecomendState.SHUFFLE_ANIM1 || nowRecomendState == RecomendState.SHUFFLE_ANIM2){
			batch.flush();
			//プレビューの高さ分確保
			for(int i=0;i<shuffleSize;i++){
				int n = shuffleDto.get(i).num;
				DrawThumbsRecomendShuffle(n);
			}
		}
		else if(nowRecomendState == RecomendState.SHUFFLE_ANIM3){//レイヤー順違う
			batch.flush();
			if(shuffle3state==1){
				for(int i=shaffleBallTotal-1;i>=0;i--){
					int n = shuffleDto.get(i).num;
					DrawThumbsRecomendShuffle(n);
				}
			}
			else{
				for(int i=shuffleSize-1;i>=0;i--){
					int n = shuffleDto.get(i).num;
					DrawThumbsRecomendShuffle(n);
				}
			}
		}
		else{
			//プレビューの高さ分確保
			for(int i=0;i<thumbsTotal;i++){
				if((-THUMBS_ZONE_HEIGHT_RECOMEND< thumbsY[i] + ZOOMPM_REC  && thumbsY[i] - ZOOMPM_REC <realHeight)){
					DrawThumbsRecomend(i);
				}
			}
		}
		//プレビューマスク
		if(nowRecomendState == RecomendState.PEVIEW_MODE || nowRecomendState == RecomendState.PEVIEW_ZOOMOUT){
			quickPreviewMaskSP.setScale(thumbsScale[previewNum]);
			quickPreviewMaskSP.setPosition(thumbsX[previewNum], thumbsY[previewNum]);
			quickPreviewMaskSP.draw(batch);
		}
		//tag
		if(nowRecomendState != RecomendState.SHUFFLE_ANIM1 && nowRecomendState != RecomendState.SHUFFLE_ANIM2
				 && nowRecomendState != RecomendState.SHUFFLE_ANIM3){
			batch.flush();
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			//プレビューの高さ分確保
			for(int i=0;i<thumbsTotal;i++){
				if((-THUMBS_ZONE_HEIGHT_RECOMEND< thumbsY[i] + ZOOMPM_REC  && thumbsY[i] - ZOOMPM_REC <realHeight)){
					if(!nowChangingThumbs[i]) DrawThumbsRecomendTag(i);
				}
			}
			batch.flush();
		}
		if(nowRecomendState != RecomendState.NOMAL_MODE && visibleQuickBtn){
			//ETC1 mask
			if(emQuickBtnZoom.isFinishAnim() && nowRecomendState == RecomendState.PEVIEW_ZOOMOUT){
				if(quickBtnScale <= 0.1f){
					quickBtnScale = 0f;
					visibleQuickBtn = false;
				}
			}
			else quickBtnScale = emQuickBtnZoom.actEIN(delta);
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			int posi = previewNum%2;
			setQuickRectZoneREC(posi);
			if(state_detailNum==previewNum) state_quick_detail = 1;
			else state_quick_detail = 0;
			if(state_setNum==previewNum) state_quick_set = 1;
			else state_quick_set = 0;
			state_quick_like = (cto.get(previewNum).isFavorite)?1:0;

			float ajustY = (REC_PREVIEW_HEIGHT- (THUMBS_TEX_SIZE*thumbsScale[previewNum]))*0.75f;
			float ajustX = (REC_PREVIEW_HEIGHT- (THUMBS_TEX_SIZE*thumbsScale[previewNum]))*0.25f;
			float ajustS = (THUMBS_TEX_SIZE*thumbsScale[previewNum])/REC_PREVIEW_HEIGHT;
			if(posi==0) ajustX*=-1f;
			assetObj.quick_btn.setScale(quickBtnScale);
			assetObj.quick_btn.setPosition(thumbsX[previewNum]+previewPosiLike[posi].x + (previewPosiDetailAdd[posi].x+ajustX)*ajustS, thumbsY[previewNum]+ previewPosiLike[posi].y + (previewPosiDetailAdd[posi].y-ajustY)*ajustS);
			assetObj.quick_btn.draw(batch);
			assetObj.quick_btn.setScale(quickBtnScale);
			assetObj.quick_btn.setPosition(thumbsX[previewNum]+previewPosiLike[posi].x+ajustX*ajustS, thumbsY[previewNum]+previewPosiLike[posi].y-ajustY*ajustS);
			assetObj.quick_btn.draw(batch);
			assetObj.quick_btn.setScale(quickBtnScale);
			assetObj.quick_btn.setPosition(thumbsX[previewNum]+previewPosiLike[posi].x +(previewPosiSetAdd[posi].x+ajustX)*ajustS, thumbsY[previewNum]+previewPosiLike[posi].y + (previewPosiSetAdd[posi].y-ajustY)*ajustS);
			assetObj.quick_btn.draw(batch);
			assetObj.quick_detail[state_quick_detail].setScale(quickBtnScale);
			assetObj.quick_detail[state_quick_detail].setPosition(thumbsX[previewNum]+previewPosiLike[posi].x + (previewPosiDetailAdd[posi].x+ajustX)*ajustS, thumbsY[previewNum]+ previewPosiLike[posi].y + (previewPosiDetailAdd[posi].y-ajustY)*ajustS);
			assetObj.quick_like[state_quick_like].setPosition(thumbsX[previewNum]+previewPosiLike[posi].x+ajustX*ajustS, thumbsY[previewNum]+previewPosiLike[posi].y-ajustY*ajustS);
			assetObj.quick_set[state_quick_set].setScale(quickBtnScale);
			assetObj.quick_set[state_quick_set].setPosition(thumbsX[previewNum]+previewPosiLike[posi].x +(previewPosiSetAdd[posi].x+ajustX)*ajustS, thumbsY[previewNum]+previewPosiLike[posi].y + (previewPosiSetAdd[posi].y-ajustY)*ajustS);
			assetObj.quick_detail[state_quick_detail].draw(batch);
			assetObj.quick_like[state_quick_like].setScale(quickBtnScale);
			assetObj.quick_like[state_quick_like].draw(batch);
			assetObj.quick_set[state_quick_set].draw(batch);
			batch.flush();
		}
		if(cashStart){
			//detailの場合はMenuもキャッシュ
//			if(reserveAnimationState == AnimationState.ANIM_DETAIL_TAP) DrawMenu();
			//settingボタンだけバッファ
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			assetObj.header_info.setPosition(844, realHeight-219);//sortの座標
			assetObj.header_info.draw(batch);
			assetObj.header_setting.setPosition(844, realHeight-219);//sortの座標
			assetObj.header_setting.draw(batch);
			batch.flush();
			frameBufferCash.end();
			//一度描画する
			if(isOS404) cashScreenSP.setTexture(frameBufferCash.getColorBufferTexture());//ここで一度セット
			setDefaultShader();
			cashScreenSP.draw(batch);
			batch.flush();
			initializeCircleAnim();
//			DebugLog.instance.outputLog("flamework", "cashStart!!!!!!!!!!!");
		}

		if(NowTransition && TransitionMaskDraw){
			//ボタンだけ描画
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			assetObj.header_info.setScale(ONE);
			assetObj.header_info.setPosition(844, realHeight-219);//sortの座標
			assetObj.header_info.draw(batch);
			assetObj.header_setting.setPosition(844, realHeight-219);//sortの座標
			assetObj.header_setting.draw(batch);
			batch.flush();
			setDefaultShader();
			bgMaskSprite.setAlpha(circleMaskAlpha);
			bgMaskSprite.draw(batch);
			batch.flush();
			setETCShader();
			maskScreenSP.getTexture().bind(1);
			cashScreenSP.getTexture().bind(0);
			cashScreenSP.draw(batch);
			batch.flush();
		}

		//共通
//		if(reserveAnimationState !=AnimationState.ANIM_DETAIL_TAP) DrawMenu();
		DrawMenu();
		batch.end();
	}
	void DrawMenu(){
//		DebugLog.instance.outputLog("loop", "here?????????" + nowScreenState);
		setETCShader();
		parts1Mask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		parts1Mask.bind(0);

			assetObj.header_btn[state_header_mybox].setScale(ONE);
			assetObj.header_btn[state_header_mybox].setPosition(-8, realHeight-219);//myboxの座標
			assetObj.header_btn[state_header_mybox].draw(batch);
		if(startMyboxButtonAnim){
			//ちょっと待機
			if(myboxButtonAnimWait<=0.3f) myboxButtonAnimWait+=delta;
			else if(0.3f<=myboxButtonAnimWait) myboxButtonAnimAlpha-=delta*3f;
//			myboxButtonAnimAlpha-=delta*2.5f;
			if(0f<=myboxButtonAnimAlpha){
				batch.flush();
				ETCShaderAlphaSet(myboxButtonAnimAlpha);
//				assetObj.header_btn[1].setScale(0.845238f);//myboxの座標
				assetObj.header_btn[1].setScale(0.846f);//myboxの座標
				assetObj.header_btn[1].setPosition(-8, realHeight-218);//myboxの座標--微調整
				assetObj.header_btn[1].draw(batch);
				batch.flush();
				ETCShaderAlphaSet(1f);
			}
			else{
				assetObj.header_btn[1].setScale(ONE);//myboxの座標
				startMyboxButtonAnim=false;
			}
		}
//		assetObj.header_mybox[state_header_mybox].setScale(ONE);
		assetObj.header_mybox[state_header_mybox].setPosition(-7, realHeight-218);//myboxの座標
		assetObj.header_mybox[state_header_mybox].draw(batch);


		//キャッシュ時はScreenStateが変わるのでSCREEN_NEWARTの時は注意
		if(nowScreenState == ScreenState.SCREEN_RECOMEND){
			if(emTapRecomemd.isFinishAnim()) recomendFooterRota=1f;
			else recomendFooterRota = emTapRecomemd.actEINCubic(delta);
			//キャッシュ時はScreenStateが変わるのでSCREEN_MYBOXの時は注意
			if(!NowTransition){
				assetObj.header_info.setScale(ONE);
				assetObj.header_info.setPosition(844, realHeight-219);//sortの座標
				assetObj.header_info.draw(batch);
				assetObj.header_setting.setPosition(844, realHeight-219);//sortの座標
				assetObj.header_setting.draw(batch);
			}
		}
		else if(nowScreenState == ScreenState.SCREEN_NEWART && !NowTransition){
			assetObj.header_btn[0].setScale(sortHeaderScale);
			assetObj.header_btn[0].setPosition(844, realHeight-219);//sortの座標
			assetObj.header_btn[0].draw(batch);
			if(state_header_sort==0){
				assetObj.header_sort.setScale(sortHeaderScale);
				assetObj.header_sort.setPosition(844, realHeight-219);//sortの座標
				assetObj.header_sort.draw(batch);
			}
			else{
				assetObj.header_close.setScale(sortHeaderScale);
				assetObj.header_close.setPosition(844, realHeight-219);//sortの座標
				assetObj.header_close.draw(batch);
			}
			if(emTapMenu.isFinishAnim()) newFooterScale=1f;
			else newFooterScale = emTapMenu.actEaseOutBack(delta);
		}
//		//キャッシュ時はScreenStateが変わるのでSCREEN_MYBOXの時は注意
//		else if(nowScreenState == ScreenState.SCREEN_MYBOX && !NowTransition){
//			assetObj.header_btn[0].setScale(ONE);
//			assetObj.header_btn[0].setPosition(844, realHeight-219);//sortの座標
//			assetObj.header_btn[0].draw(batch);
//			assetObj.header_setting.setPosition(844, realHeight-219);//sortの座標
//			assetObj.header_setting.draw(batch);
//		}
		else if(NowTransition && nowAnimationState == AnimationState.ANIM_NEW_TAP){
			//遷移中パターン
			if(emTapMenu.isFinishAnim()){
				newFooterScale=1f;
				DebugLog.instance.outputLog("loop", "FINIIIIIIIIIIIIIIIISH");
			}
			else{
				newFooterScale = emTapMenu.actEaseOutBack(delta);
				DebugLog.instance.outputLog("loop", "ZOOOOOOOOOOM");
			}
		}



//		if(newFooterScale<1f) newFooterScale+=delta*0.75f;
//		if(1f<=newFooterScale) newFooterScale=1f;
//		if(recomendFooterScale<1f) recomendFooterScale+=delta*0.75f;
//		if(1f<=recomendFooterScale) recomendFooterScale=1f;
		assetObj.footer_btn[state_footer_new].setScale(newFooterScale);
		assetObj.footer_btn[state_footer_new].setPosition(-8, -7);//newの座標
		assetObj.footer_btn[state_footer_new].draw(batch);
		assetObj.footer_new[state_footer_new].setScale(newFooterScale);
		assetObj.footer_new[state_footer_new].setPosition(-8, -7);//newの座標
		assetObj.footer_new[state_footer_new].draw(batch);
//		assetObj.footer_btn[state_footer_recomend].setRotation(recomendFooterRota);
		if(state_footer_recomend==0){
			assetObj.footer_btn[0].setPosition(418, -7);//recomendの座標
			assetObj.footer_btn[0].draw(batch);
//			assetObj.footer_recomend[0].setRotation(recomendFooterRota);
			assetObj.footer_recomend[0].setPosition(418, -7);//recomendの座標
			assetObj.footer_recomend[0].draw(batch);
		}
		else{
			assetObj.footer_recbtn.setPosition(418, -7);//recomendの座標
			assetObj.footer_recbtn.draw(batch);
			assetObj.footer_recRota.setRotation(recomendFooterRota);
			assetObj.footer_recRota.setPosition(418, -7);//recomendの座標
			assetObj.footer_recRota.draw(batch);
			assetObj.footer_recomend[1].setPosition(418, -7);//recomendの座標
			assetObj.footer_recomend[1].draw(batch);
		}


		assetObj.footer_btn[state_footer_ranking].setPosition(844, -7);//rankingの座標
		assetObj.footer_btn[state_footer_ranking].draw(batch);
		assetObj.footer_ranking[state_footer_ranking].setPosition(844, -7);//rankingの座標
		assetObj.footer_ranking[state_footer_ranking].draw(batch);
		batch.flush();
	}
	//スプライトの番号(DetailText用)
	void TapDetail(int spNum){
	    if(!SPPUtility.checkNetwork(myActivity.getApplicationContext())){
	    	//ここでダイアログ表示
	    	DebugLog.instance.outputLog("api", "ネットワークない(DetailTap時)!!!!!!!!!");
	    	visibleDialog = true;
	    	saveTouchState= nowTouchState;
	    	nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
	    	return;
	    }
	    if(detailCto!=null) detailCto = null;
	    detailCto = cto.get(nowIndex[spNum]);
	    detailisNew = detailCto.getNewProperty();
	    detailThumbsNum = nowIndex[spNum];
    	//Detail用のテキストはここで代入
	    thumbsSpriteDetailText.setTexture(assetsMulti[useManagerNum[spNum]].get(nowLoadingPath[spNum],Texture.class));
		saveScreenState = nowScreenState;
//		reserveAnimationState = AnimationState.ANIM_DETAIL_TAP;
//		if(!offTransition) nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
//		else initializeCircleAnim();

		initializeDetailScreen();
	}
	void initializeSortReset(){
		visibleSort = false;//出現して機能出来る状態
		drawableSort = false;//描画
		sortBarAddHeight = 0;
		sortBarScale = 0.8f;
		sortHeaderScale = 1.0f;
//		myBoxHeaderScale = 1.0f;
		for (int i = 0;i<7;i++){
			sortButtonScale[i]=0;
		}
		state_header_sort = 0;
	}
	void initializeSortClose(){
		DebugLog.instance.outputLog("touched", "ソート Close");
		nowAnimationState = AnimationState.ANIM_SORT_CLOSE;
		visibleSort=false;
		emSortBarZoom.ResetPosition(SORTBAR_MAX,0,SORTBAR_ZOOMTIME*0.8f);
		for (int i = 0;i<7;i++) emSortButtonZoom[i].ResetPosition( ONE,0,SORTBUTTON_ZOOMTIME*0.4f);
//		sortHeaderScale = 0.9f;
		state_header_sort = 0;
//		if(sortWhiteOut) sortWhiteOut = false;
	}

	private void initializeCircleAnim() {
		nowAnimationState = reserveAnimationState;
		//座標 +40 +41
		switch(nowAnimationState){
			case ANIM_NEW_TAP:
				initializeListScreen(true);
				circlePosition.x = 33f;
				circlePosition.y = 35f;
			break;
			case ANIM_RUNKING_TAP:
				initializeListScreen(false);
				circlePosition.x = 885f;
				circlePosition.y = 35f;
			break;
			case ANIM_RECOMEND_TAP:
				initializeRecomendScreen();
				circlePosition.x = 459f;
				circlePosition.y = 35f;
			break;
			case ANIM_MYBOX_TAP:
				initializeMyBoxScreen();
				circlePosition.x = 33f;
				circlePosition.y = realHeight-178;
			break;
//			case ANIM_DETAIL_TAP:
//				initializeDetailScreen();
//				circlePosition.x = 459f;
//				circlePosition.y = centerY-81;
//			break;
		default:
			break;
		}
//		if(offTransition){
//			if(nowAnimationState!=AnimationState.ANIM_DETAIL_TAP){
//				waitTransTime = 0;
//				transWhiteOut =true;
//			}
//			finishCircleAnim();
//		}
//		else{
			NowTransition = true;
			nowTouchState = TouchState.TOUCH_DIABLE;
//			if(nowAnimationState==AnimationState.ANIM_DETAIL_TAP) emCircleZoom.ResetPosition(0, ONE,CIRCLE_ZOOMTIME);
//			else{
//				emCircleZoom.ResetPosition(0.03f, ONE,CIRCLE_ZOOMTIME);
//				emMaskAlpha.ResetPosition(ONE, 0f,CIRCLE_ZOOMTIME);
//			}
			emCircleZoom.ResetPosition(0.03f, ONE,CIRCLE_ZOOMTIME);
			emMaskAlpha.ResetPosition(ONE, 0f,CIRCLE_ZOOMTIME);
			circleZoom = 0.0f;
			circleMaskAlpha = 1.0f;
//		}
	}

	private void finishCircleAnim() {

		switch (nowAnimationState) {
		case ANIM_NEW_TAP:
			nowScreenState = ScreenState.SCREEN_NEWART;
			break;
		case ANIM_RUNKING_TAP:
			nowScreenState = ScreenState.SCREEN_RUNKING;
			break;
		case ANIM_RECOMEND_TAP:
			nowScreenState = ScreenState.SCREEN_RECOMEND;
			break;
		case ANIM_MYBOX_TAP:
			nowScreenState = ScreenState.SCREEN_MYBOX;
			break;
//		case ANIM_DETAIL_TAP:
//			nowScreenState = ScreenState.SCREEN_DETAIL;
//			break;
		default:
			break;
		}
//		if(nowAnimationState == AnimationState.ANIM_DETAIL_TAP){
//			if(nowDetailState == DetailState.DETAIL_NOMAL){
//				DebugLog.instance.outputLog("api", "遷移アニメ終わる前に情報とれてた(finishCircleAnim)!!!!!!!!!");
//				nowTouchState = TouchState.TOUCH_SCROLL;
//			}
//			nowAnimationState = AnimationState.ANIM_NONE;
//			reserveAnimationState = nowAnimationState;
//		}
//		else{
			//残りを読み込む
			if(!finisedLoadRemain) loadRemainThumbs();
			nowTouchState = TouchState.TOUCH_SCROLL;
			nowAnimationState = AnimationState.ANIM_NONE;
			reserveAnimationState = nowAnimationState;
//		}
		//detailに遷移した時以外は個々でリセット
		if(nowScreenState != ScreenState.SCREEN_DETAIL) visibleSort = false;
		NowTransition = false;
		TransitionMaskDraw = false;
	}
	void DrawCircleMask(){
		//マスク生成
//		batch.begin();
		frameBufferMask.begin();
		matWhiteSP.draw(batch);
		//早くなり過ぎないように…fps30
		float time = delta;
		if(0.033f<=time) time = 0.033f;
		circleZoom = emCircleZoom.actEINQuadratic(time);
		circleMaskAlpha = emMaskAlpha.easeInQuart(time);
//		circleZoom = emCircleZoom.actEINExpo(time);

		circleMaskSP.setPosition(circlePosition.x,circlePosition.y);//newの座標//newの座標 +40 +41
		circleMaskSP.setScale(circleZoom*CIRCLE_MENU_ZOOM);
		circleMaskSP.draw(batch);
		batch.flush();
		frameBufferMask.end();

//		//一度描画する
		if(isOS404) maskScreenSP.setTexture(frameBufferMask.getColorBufferTexture());//ここで一度セット
		TransitionMaskDraw = true;
		if(CIRCLE_MENU_ZOOM<=circleZoom*CIRCLE_MENU_ZOOM){
			//アニメーション完了
			finishCircleAnim();
		}
//		batch.end();
	}
	void initializeInfoClose(){
		nowScreenState = saveScreenState;
		nowTouchState = saveTouchState;
//		nowTouchState = TouchState.TOUCH_SCROLL;
		DebugLog.instance.outputLog("flamework", "initializeInfoClose nowScreenState::" + nowScreenState);
		DebugLog.instance.outputLog("flamework", "initializeInfoClose nowTouchState::" + nowTouchState);
	}
	void initializeTutrialClose(){
		if(isFirstTutrial){
			DebugLog.instance.outputLog("api","Tutrial終了！！" );
			Editor gEditor = getSharedPreference(myActivity.getApplicationContext()).edit();
			gEditor.putBoolean("finishTutrial",true);
			gEditor.commit();
			if(reserveScreenState!=ScreenState.SCREEN_OFFLINE){
				nowScreenState = ScreenState.SCREEN_SPLASH;
				nowTouchState = TouchState.TOUCH_DIABLE;
			}
			else nowScreenState = ScreenState.SCREEN_OFFLINE;
			reserveScreenState = ScreenState.SCREEN_LOADWAIT;//初期化
			isFirstTutrial = false;
			isFirstVisible3Tutrial = false;
			//タイムアウト設定
			ContentsOperatorForCatalog.op.startTimerForAllDataDownload(31 * 1000);
		}
		else{
			nowScreenState=ScreenState.SCREEN_INFO;
			nowTouchState = TouchState.TOUCH_ONLY_MENU;
		}
	}
	void initializeDetailClose(){
		ContentsOperatorForCatalog.op.stopDetailDownload();
		if(visibleDialog) visibleDialog = false;
		nowScreenState = saveScreenState;
		if(nowScreenState==ScreenState.SCREEN_RECOMEND){
			resetQuickState();
			thumbsSprite[previewNum].setU(0);
			thumbsSprite[previewNum].setU2(THUMBS_U);
			thumbsSprite[previewNum].setAlpha(1f);
		}
		if(nowScreenState==ScreenState.SCREEN_RECOMEND && nowRecomendState== RecomendState.PEVIEW_MODE) nowTouchState = TouchState.TOUCH_DIABLE_QUICK;
		else nowTouchState = TouchState.TOUCH_SCROLL;
		nowAnimationState = AnimationState.ANIM_NONE;
		reserveAnimationState = nowAnimationState;
	}
	void ETCShaderSet(){
		maskETCShader.begin();
		maskETCShader.setUniformi("u_texture", 0);
		maskETCShader.setUniformi("m_texture", 1);
		maskETCShader.setUniformf("m_alpha", 1f);
		maskETCShader.end();
	}
	void ETCShaderAlphaSet(float alpha){
		maskETCShader.begin();
		maskETCShader.setUniformf("m_alpha", alpha);
		maskETCShader.end();
		batch.setShader(maskETCShader);
		nowUseETCShader = true;
	}
	void DrawListScreen(){
		if(sortWhiteOut ){
			waitSortTime+=delta;
			if(SORT_WHITEOUT_WAITTIME*0.4f<waitSortTime && visibleSort) initializeSortClose();
//			if(WHITEOUT_WAITTIME<waitSortTime) sortWhiteOut = false;
		}
		//スクロールロジック(後でメソッド化する)
		calcScrollFlickEase();
		//まずオーダー順で座標計算
		for(int y=0;y<thumbsTexKazu  ;y++){
			int index = thumbsOrder[y];
			//サムネイルY計算
        	if(y==0) thumbsY[index] = thumbsTopY - THUMBS_ZONE_HEIGHT_LIST + mEaseY;
        	else thumbsY[index] =thumbsY[thumbsOrder[y-1]] - THUMBS_ZONE_HEIGHT_LIST;
			thumbsTouch[index].y= thumbsY[index] + THUMBS_IMAGE_Y_LIST;
		}
		//余裕もたせる(上に)
		upperLimit = (realHeight+thumbsAllHeight)*0.5f+(THUMBS_ZONE_HEIGHT_LIST*0.5f);
		underLimit = (realHeight-thumbsAllHeight)*0.5f;
		//上超えた判定//トータルが24以下は無視
		int index1 = thumbsOrder[0];
		int index2 = thumbsOrder[Math.max((thumbsTexKazu-1),0)];
		if(thumbsY[index1]>=upperLimit && nowIndex[index1]+thumbsTexKazu<=thumbsTotal-1 && THUMBSTEXMAX<thumbsTotal){//上スクロール
			DebugLog.instance.outputLog("flamework","Thumbs move up NUM:::" + index1);
			thumbsTopY-=THUMBS_ZONE_HEIGHT_LIST;
			nowIndex[index1]+=thumbsTexKazu;
			thumbsY[index1] =thumbsY[thumbsOrder[thumbsTexKazu-1]]-THUMBS_ZONE_HEIGHT_LIST;
			thumbsTouch[index1].y= thumbsY[index1] + THUMBS_IMAGE_Y_LIST;
			//thumbsOrder計算
			calcThumbsOrder(index1,true);
			//loading
			if(finisedLoadRemain){
				DebugLog.instance.outputLog("flamework","Thumbs load 上超えた:::" + index1);
				String thumbnailID = String.valueOf(cto.get(nowIndex[index1]).assetID);
				loadingThumbs(index1, thumbnailID);
			}
		}
		//下超えた
		else if(thumbsY[index2]<underLimit && nowIndex[index2]-thumbsTexKazu>=0 && THUMBSTEXMAX<thumbsTotal){//下スクロール
			DebugLog.instance.outputLog("flamework","Thumbs move down NUM:::" + index2);
			nowIndex[index2]-=thumbsTexKazu;
			thumbsY[index2] =thumbsY[thumbsOrder[0]]+THUMBS_ZONE_HEIGHT_LIST;
			thumbsTopY+=THUMBS_ZONE_HEIGHT_LIST;//順番の違い注意
			thumbsTouch[index2].y= thumbsY[index2] + THUMBS_IMAGE_Y_LIST;
			//thumbsOrder計算
			calcThumbsOrder(index2,false);
			//loading
			if(finisedLoadRemain){
				DebugLog.instance.outputLog("flamework","Thumbs load 下超えた:::" + index2);
				String thumbnailID = String.valueOf(cto.get(nowIndex[index2]).assetID);
				loadingThumbs(index2, thumbnailID);
			}
		}
		//描画
		batch.begin();

		if(NowTransition) DrawCircleMask();

		cashStart = false;
		if(nowAnimationState == AnimationState.ANIM_CASH_SCREEN){
			frameBufferCash.begin();
			cashStart = true;
		}
		//bg_mat
		bgSprite.draw(batch);
		//サムネイル
		if(!sortWhiteOut){
			for(int i=0;i<thumbsTexKazu;i++){
				setQuickRectZone(i);//タッチゾーン設定
				int dIndex = thumbsOrder[i];
				if(-THUMBS_ZONE_HEIGHT_LIST< thumbsY[dIndex]  && thumbsY[dIndex] <realHeight){
					DrawThumbsList(dIndex);
				}
				else if(thumbsIsQuick[dIndex]){
					disableQuickState(dIndex);
				}
			}
			batch.flush();
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			//サムネイルのETCマスク使用要素
			for(int i=0;i<thumbsTexKazu;i++){
				int dIndex = thumbsOrder[i];
				if(-THUMBS_ZONE_HEIGHT_LIST< thumbsY[dIndex]  && thumbsY[dIndex] <realHeight){
					if(!nowChangingThumbs[dIndex]) DrawThumbsListParts(dIndex);
				}
			}
			batch.flush();
		}
		//ソート
		if(nowScreenState == ScreenState.SCREEN_NEWART){
			if(nowAnimationState == AnimationState.ANIM_SORT_OPEN){
				if(sortHeaderScale<1f) sortHeaderScale+=delta*0.75f;
				if(1f<=sortHeaderScale) sortHeaderScale=1f;
				if(sortBarScale<1f){
					sortBarScale+=delta*3f;
				}
				if(1f<=sortBarScale){
					sortBarScale=1f;
					sortBgBottomSP.setScale(sortBarScale);
					sortBgTopSP.setScale(sortBarScale);
					if(emSortBarZoom.isFinishAnim()){
						sortBarAddHeight = (int) SORTBAR_MAX;
						for (int i = 0;i<7;i++){
							if(i==0) sortButtonScale[i] = emSortButtonZoom[i].actEaseOutBack(delta);
							else if(0.5f<sortButtonScale[i-1]) sortButtonScale[i] = emSortButtonZoom[i].actEaseOutBack(delta);
						}
						if(emSortButtonZoom[6].isFinishAnim()){
							DebugLog.instance.outputLog("flamework", "ソートアニメ(open)終了");
							nowAnimationState = AnimationState.ANIM_NONE;
//							nowTouchState = TouchState.TOUCH_SCROLL;
							nowTouchState = TouchState.TOUCH_ONLY_MENU;
							visibleSort = true;
						}
					}
					else sortBarAddHeight = (int) emSortBarZoom.actEIN(delta);
				}
			}
			else if(nowAnimationState == AnimationState.ANIM_SORT_CLOSE){
				if(sortHeaderScale<1f) sortHeaderScale+=delta*0.6f;
				if(1f<=sortHeaderScale) sortHeaderScale=1f;
				if(emSortBarZoom.isFinishAnim()){
					if(0.8f<sortBarScale){
						sortBarScale-=delta*1.5f;
					}
					if(sortBarScale<=0.8f){
						DebugLog.instance.outputLog("flamework", "ソートアニメ(close)終了");
						if(sortWhiteOut) sortWhiteOut = false;
						for (int i = 0;i<7;i++){
							if(sortButtonScale[i]!=0) DebugLog.instance.outputLog("flamework", "ソートアニメ(close)終了 scale::" + i  + "::" +sortButtonScale[i]);
							sortButtonScale[i] = 0f;
						}
						sortBarScale=0.8f;
						nowAnimationState = AnimationState.ANIM_NONE;
						nowTouchState = TouchState.TOUCH_SCROLL;
						drawableSort = false;
						sortBarAddHeight = 0;
					}
				}
				else{
					for (int i = 6;i>=0;i--){
						if(i==6) sortButtonScale[i] = emSortButtonZoom[i].actEIN(delta);
						else if(sortButtonScale[i+1]<0.97f) sortButtonScale[i] = emSortButtonZoom[i].actEIN(delta);
					}
//					if(emSortButtonZoom[6].isFinishAnim()) sortBarAddHeight = (int) emSortBarZoom.actEIN(delta);
					if(sortButtonScale[6]<0.5f) sortBarAddHeight = (int) emSortBarZoom.actEIN(delta);
				}
			}
			if(drawableSort){
				setDefaultShader();
				if(sortBarScale!=1f){
					sortBgTopSP.setScale(sortBarScale);
					sortBgTopSP.setPosition(838, topY-97);
					sortBgTopSP.draw(batch);
					sortBgBottomSP.setScale(sortBarScale);
					sortBgBottomSP.setPosition(838, topY-225);
					sortBgBottomSP.draw(batch);
				}
				else{
					sortBgRG.setRegionHeight(128+sortBarAddHeight);
					batch.draw(sortBgRG, 838, topY-97-sortBarAddHeight, 256, 128+sortBarAddHeight);
					sortBgBottomSP.setPosition(838, topY-225-sortBarAddHeight);
					sortBgBottomSP.draw(batch);
				}
				batch.flush();
				setETCShader();
				parts1Mask.bind(1);
				//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
				parts1Mask.bind(0);
				for(int i=0;i<7;i++){
					assetObj.chara_btn[state_sortChara[i]].setScale(sortButtonScale[i]);
					assetObj.chara_btn[state_sortChara[i]].setPosition(899, topY-308-(i*141));
					assetObj.chara_btn[state_sortChara[i]].draw(batch);
					assetObj.sort_chara[i].setScale(sortButtonScale[i]);
					assetObj.sort_chara[i].setPosition(899, topY-308-(i*141));
					assetObj.sort_chara[i].draw(batch);
				}
				batch.flush();
			}
		}

		if(cashStart){
//			if(reserveAnimationState == AnimationState.ANIM_DETAIL_TAP) DrawMenu();
			if(nowScreenState == ScreenState.SCREEN_NEWART){
				//ソートボタンだけバッファ
				setETCShader();
				parts1Mask.bind(1);
				//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
				parts1Mask.bind(0);
				assetObj.header_btn[0].setPosition(844, realHeight-219);//sortの座標
				assetObj.header_btn[0].draw(batch);
				if(state_header_sort==0){
					assetObj.header_sort.setPosition(844, realHeight-219);//sortの座標
					assetObj.header_sort.draw(batch);
				}
				else{
					assetObj.header_close.setPosition(844, realHeight-219);//sortの座標
					assetObj.header_close.draw(batch);
				}
				batch.flush();
			}
			frameBufferCash.end();
			//一度描画する
			if(isOS404) cashScreenSP.setTexture(frameBufferCash.getColorBufferTexture());//ここで一度セット
			setDefaultShader();
			cashScreenSP.draw(batch);
			batch.flush();
			initializeCircleAnim();
		}



		if(NowTransition && TransitionMaskDraw){

			if(nowScreenState == ScreenState.SCREEN_NEWART){
				//ソートボタンだけ描画
				setETCShader();
				parts1Mask.bind(1);
				//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
				parts1Mask.bind(0);
				//sortbottun
				assetObj.header_btn[0].setScale(1f);
				assetObj.header_btn[0].setPosition(844, realHeight-219);//sortの座標
				assetObj.header_btn[0].draw(batch);
				assetObj.header_sort.setScale(1f);
				assetObj.header_sort.setPosition(844, realHeight-219);//sortの座標
				assetObj.header_sort.draw(batch);
				batch.flush();
			}

			setDefaultShader();
			bgMaskSprite.setAlpha(circleMaskAlpha);
			bgMaskSprite.draw(batch);
			batch.flush();
			setETCShader();
			maskScreenSP.getTexture().bind(1);
			cashScreenSP.getTexture().bind(0);
			cashScreenSP.draw(batch);
			batch.flush();
		}

//		if(reserveAnimationState !=AnimationState.ANIM_DETAIL_TAP)  DrawMenu();
		DrawMenu();


		//共通
		batch.end();
	}
	void DrawMyBox(){
		//スクロールロジック(後でメソッド化する)
		calcScrollFlickEase();
		//まずオーダー順で座標計算
		for(int y=0;y<thumbsTexKazu  ;y++){
			int index = thumbsOrder[y];
			int indexAll = nowIndex[index];
			//matrix変更箇所
			//偶数のみ
			if(indexAll%2==0) thumbsX[index] = 24f;
			else thumbsX[index] = 540f;
			//サムネイルY計算
        	if(y==0) thumbsY[index] = thumbsTopY - THUMBS_ZONE_HEIGHT_MATRIX + mEaseY;
        	else{
        		if(indexAll%2==0) thumbsY[index] =thumbsY[thumbsOrder[y-1]] - THUMBS_ZONE_HEIGHT_MATRIX;
        		else thumbsY[index] =thumbsY[thumbsOrder[y-1]];
        	}
//        	thumbsTouch[index].y= thumbsY[index] + 30;
		}
		//余裕もたせる(上に)
		upperLimit = (realHeight+thumbsAllHeight)*0.5f+(THUMBS_ZONE_HEIGHT_MATRIX*0.5f);
		underLimit = (realHeight-thumbsAllHeight)*0.5f;
		//上超えた判定//トータルが24以下は無視
		int index1 = thumbsOrder[0];
		int index2 = thumbsOrder[Math.max((thumbsTexKazu-1),0)];
		if(thumbsY[index1]>=upperLimit && nowIndex[index1]+thumbsTexKazu<=thumbsTotal-1 && THUMBSTEXMAX<thumbsTotal){//上スクロール
			DebugLog.instance.outputLog("value","Thumbs move up NUM:::" + index1);
//			thumbsTopY-=THUMBS_ZONE_HEIGHT_LIST;
			nowIndex[index1]+=thumbsTexKazu;
			int indexAll = nowIndex[index1];
			if(indexAll%2==0){
				thumbsY[index1] =thumbsY[thumbsOrder[thumbsTexKazu-1]]-THUMBS_ZONE_HEIGHT_MATRIX;
				thumbsX[index1] = 24f;
			}
        	else{
				thumbsTopY-=THUMBS_ZONE_HEIGHT_MATRIX;
				thumbsY[index1] =thumbsY[thumbsOrder[thumbsTexKazu-1]];
				thumbsX[index1] = 540f;
        	}
//			thumbsTouch[index1].y= thumbsY[index1] + 40;
			//thumbsOrder計算
			calcThumbsOrder(index1,true);
			//loading
			if(finisedLoadRemain){
				String thumbnailID = String.valueOf(cto.get(nowIndex[index1]).assetID);
				loadingThumbs(index1, thumbnailID);
			}
		}
		//下超えた
		else if(thumbsY[index2]<underLimit && nowIndex[index2]-thumbsTexKazu>=0 && THUMBSTEXMAX<thumbsTotal){//下スクロール
			DebugLog.instance.outputLog("value","Thumbs move down NUM:::" + index2);
			nowIndex[index2]-=thumbsTexKazu;
			int indexAll = nowIndex[index2];
			if(indexAll%2==0){
				thumbsY[index2] =thumbsY[thumbsOrder[0]]+THUMBS_ZONE_HEIGHT_MATRIX;
				thumbsX[index2] = 24f;
			}
        	else{
				thumbsTopY+=THUMBS_ZONE_HEIGHT_MATRIX;//順番の違い注意
        		thumbsY[index2] =thumbsY[thumbsOrder[0]];
				thumbsX[index2] = 540f;
        	}
//			thumbsY[index2] =thumbsY[thumbsOrder[0]]+THUMBS_ZONE_HEIGHT_LIST;
//			thumbsTopY+=THUMBS_ZONE_HEIGHT_LIST;//順番の違い注意
//			thumbsTouch[index2].y= thumbsY[index2] + 40;
			//thumbsOrder計算
			calcThumbsOrder(index2,false);
			//loading
			if(finisedLoadRemain){
				String thumbnailID = String.valueOf(cto.get(nowIndex[index2]).assetID);
				loadingThumbs(index2, thumbnailID);
			}
		}
		//描画
		batch.begin();

		if(NowTransition) DrawCircleMask();

		cashStart = false;
		if(nowAnimationState == AnimationState.ANIM_CASH_SCREEN){
			frameBufferCash.begin();
			cashStart = true;
		}
		//bg_mat
		bgSprite.draw(batch);
		//サムネイル
		for(int i=0;i<thumbsTexKazu;i++){
			setQuickRectZone(i,false);//タッチゾーン設定
			int dIndex = thumbsOrder[i];
			if(-THUMBS_ZONE_HEIGHT_MATRIX< thumbsY[dIndex]  && thumbsY[dIndex] <realHeight){
				DrawThumbsMatrix(dIndex);
			}
		}
		batch.flush();

		setETCShader();
		parts1Mask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		parts1Mask.bind(0);
		//サムネイルのETCマスク使用要素
		for(int i=0;i<thumbsTexKazu;i++){
			int dIndex = thumbsOrder[i];
			if(-THUMBS_ZONE_HEIGHT_MATRIX< thumbsY[dIndex]  && thumbsY[dIndex] <realHeight){
				if (!nowChangingThumbs[dIndex]) DrawThumbsMatrixParts(dIndex);
			}
		}
		batch.flush();
		if(visibleNoArt){
			wordBMask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			wordBMask.bind(0);
			assetObj.word_no_addbox.setPosition(139, centerY-91);
			assetObj.word_no_addbox.draw(batch);
			batch.flush();
		}

		if(cashStart){
//			if(reserveAnimationState == AnimationState.ANIM_DETAIL_TAP) DrawMenu();
//			else{
				//settingボタンだけバッファ
//				setETCShader();
//				parts1Mask.bind(1);
//				//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
//				parts1Mask.bind(0);
//				assetObj.header_btn[0].setPosition(844, realHeight-219);//sortの座標
//				assetObj.header_btn[0].draw(batch);
//				assetObj.header_setting.setPosition(844, realHeight-219);//sortの座標
//				assetObj.header_setting.draw(batch);
//				batch.flush();
//			}
			frameBufferCash.end();
			//一度描画する
			if(isOS404) cashScreenSP.setTexture(frameBufferCash.getColorBufferTexture());//ここで一度セット
			setDefaultShader();
			cashScreenSP.draw(batch);
			batch.flush();
			initializeCircleAnim();
		}

		if(NowTransition && TransitionMaskDraw){
//			//ボタンだけ描画
//			setETCShader();
//			parts1Mask.bind(1);
//			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
//			parts1Mask.bind(0);
//			assetObj.header_btn[0].setScale(ONE);
//			assetObj.header_btn[0].setPosition(844, realHeight-219);//sortの座標
//			assetObj.header_btn[0].draw(batch);
//			assetObj.header_setting.setPosition(844, realHeight-219);//sortの座標
//			assetObj.header_setting.draw(batch);
//			batch.flush();

			setDefaultShader();
			bgMaskSprite.setAlpha(circleMaskAlpha);
			bgMaskSprite.draw(batch);
			batch.flush();

			setETCShader();
			maskScreenSP.getTexture().bind(1);
			cashScreenSP.getTexture().bind(0);
			cashScreenSP.draw(batch);
			batch.flush();
		}

//		if(reserveAnimationState !=AnimationState.ANIM_DETAIL_TAP)  DrawMenu();
		DrawMenu();
		//共通
		batch.end();
	}
	void DrawInfo(){
		batch.begin();
		//bg_mat
		bgSprite.draw(batch);
		DrawPopUpBg();
		//要素
		for(int i=0;i<infoKazu;i++){
			if(tapInfo==i){
				infoListSP.setColor(infoSelColor);
			}
			else{
				infoListSP.setColor(infoNomColor);
			}
			infoListSP.setPosition(0, topY-infoY[i]);//マージン150　+ 高さ140
			infoListSP.draw(batch);
		}
//		batch.flush();

		DrawPopUpMask();

		setETCShader();
		parts1Mask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		parts1Mask.bind(0);
		//alow
		for(int i=0;i<infoKazu;i++){
			assetObj.infoAllow.setPosition(982, topY-infoY[i]+47);//sortの座標
			assetObj.infoAllow.draw(batch);
		}
//		assetObj.header_btn[0].setScale(ONE);
//		assetObj.header_btn[0].setPosition(844, realHeight-237);//sortの座標
//		assetObj.header_btn[0].draw(batch);
//		assetObj.header_close.setScale(ONE);
//		assetObj.header_close.setPosition(844, realHeight-237);//sortの座標
//		assetObj.header_close.draw(batch);
		assetObj.popup_close.setPosition(882, realHeight-198);//sortの座標
		assetObj.popup_close.draw(batch);
		batch.flush();
		wordBMask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		wordBMask.bind(0);
		for(int i=0;i<infoKazu;i++){
			assetObj.word_info[i].setPosition(74, topY-infoY[i]+39);//sortの座標
			assetObj.word_info[i].draw(batch);
		}
		batch.end();
	}

	public boolean visibleTutrial(){
		if(!getSharedPreference(myActivity.getApplicationContext()).getBoolean("finishTutrial", false)){
			DebugLog.instance.outputLog("api","Tutrial 初回！！！");
			if(!nowLoadingSecondAssets){
				reserveFirstTutrial = false;
				initializeTutrial(true);
			}
			else reserveFirstTutrial = true;
//			nowScreenState = ScreenState.SCREEN_TUTRIAL;
			return true;
		}
		return false;
	}
	void initializeTutrial(boolean first){
//		test = 0;
		if(first){
			isFirstTutrial = true;
			isFirstVisible3Tutrial = true;
		}
		else{
			isFirstTutrial = false;
			isFirstVisible3Tutrial = false;
		}
		nowLoadingAssetsTutrial = true;
		offsetWidthX = TUTRIAL_ZONE_WIDTH * ( TUTRIAL_KAZU-1);// スクロール幅
		flickXdist = offsetWidthX;
		mEaseX = 0;
		mLastX = mEaseX;
		//reset
		tutrialTouchUp = false;
		easeTarget = 0;
		flingStateTime = 0;
	    addFlingX = 0;
	    loadingSP.setRotation(0);
	    pageBtnLeftX =  540- (TUTRIAL_KAZU*38-24)/2f;
	    tutrialTapOK = false;
	    nowCenterTutrialNum = 0;
	    thumbsTutrialY = centerY-742;
	    resetStateBtn();//ステートボタンリセット
		for (int i = 0; i < TUTRIAL_KAZU; i++) {
			//初期化--ここでかは後で検討
			thumbsTutrialX[i] = TUTRIAL_LEFT+(i*TUTRIAL_ZONE_WIDTH);
			easeXBreakTutrial[i] = i*TUTRIAL_ZONE_WIDTH;
//			nowIndexTutrial[i] = i;
			loadingRota[i] = 0;
//			assetsTutrial.load("data/tutrial_image_" + (i+1) + ".ktx", Texture.class,param);
			assetsTutrial.load("data/tutrial_image_" + (i+1) + ".etc1", Texture.class,param);
		}
		tapInfo = -1;
		nowScreenState = ScreenState.SCREEN_TUTRIAL;
		nowTouchState = TouchState.TOUCH_SCROLL;
	}
	void doneLoadingTutrialAssets(){
		for(int i=0;i<TUTRIAL_KAZU;i++){
//			thumbsSpriteTutrial[i] = new Sprite(assetsTutrial.get("data/tutrial_image_" + (i+1) + ".ktx",Texture.class));
			thumbsSpriteTutrial[i] = new Sprite(assetsTutrial.get("data/tutrial_image_" + (i+1) + ".etc1",Texture.class));
			thumbsSpriteTutrial[i].setSize(TUTRIAL_TEX_WIDTH, TUTRIAL_TEX_HEIGHT);
		}
		nowLoadingAssetsTutrial = false;
	}
	void RereaseTutrialAssets(){
		for(int i=0;i<TUTRIAL_KAZU;i++){
//			assetsTutrial.unload("data/tutrial_image_" + (i+1) + ".ktx");
			assetsTutrial.unload("data/tutrial_image_" + (i+1) + ".etc1");
		}
		nowLoadingAssetsTutrial = true;//解放
	}
//	float test = 0;
	void DrawTutrial(){
		if (nowLoadingAssetsTutrial){
			 if(assetsTutrial.update()) doneLoadingTutrialAssets();
		}
		//ここにも来る可能性あるーー20150909
		if(!finishTouchSet){
			if(finishBannerLoading && !nowLoadingSecondAssets){
				finishTouchSet = true;
		        InputMultiplexer multiplexer = new InputMultiplexer();
		        multiplexer.addProcessor(new MyInputListener());
		        multiplexer.addProcessor(new GestureDetector(new MyGestureListener()));
				Gdx.input.setInputProcessor(multiplexer);
				DebugLog.instance.outputLog("info", "InputMultiplexer　セット　in DrawTutrial");
			}
		}
		batch.begin();
//		test+=delta;
//		if(2<test) nowLoadingAssetsTutrial = false;
//		else nowLoadingAssetsTutrial = true;
		if (nowLoadingAssetsTutrial){
			bgSprite.draw(batch);
			DrawPopUpBg();
			assetObj.loading_tutrial.setPosition(thumbsTutrialX[0]-2, thumbsTutrialY-2);//微調整
			assetObj.loading_tutrial.draw(batch);
			loadingSP.setSize(118, 118);
			loadingSP.setPosition(thumbsTutrialX[0]+373, thumbsTutrialY+624);
			loadingRota[0] += delta*LOADING_ANIM_SPEED;
			loadingSP.setRotation(loadingRota[0]);
			loadingSP.draw(batch);
			DrawPopUpMask();
//			setETCShader();
//			parts1Mask.bind(1);
//			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
//			parts1Mask.bind(0);
//			//closeボタン
//			assetObj.popup_close.setPosition(882, realHeight-198);
//			assetObj.popup_close.draw(batch);
		} else {
			calcScrollFlickEaseTutrial();
			//描画
			//bg_mat
			bgSprite.draw(batch);
			DrawPopUpBg();
			//サムネイル
			for(int i=0;i<TUTRIAL_KAZU;i++){
				//サムネイルY計算
	        	if(i==0) thumbsTutrialX[i] = tutrialLeftX + TUTRIAL_LEFT - mEaseX;
	        	else thumbsTutrialX[i] =thumbsTutrialX[i-1] + TUTRIAL_ZONE_WIDTH;
								//座標計算
				if(0<= thumbsTutrialX[i] + TUTRIAL_TEX_WIDTH&&  thumbsTutrialX[i]<=1080){
					DrawThumbsTutrial(i);
					//誤差を考えても確実な値
					if(TUTRIAL_LEFT-10<thumbsTutrialX[i] && thumbsTutrialX[i] < TUTRIAL_LEFT+10){
						nowCenterTutrialNum = i;
						if(isFirstVisible3Tutrial && 3<=nowCenterTutrialNum){
							isFirstVisible3Tutrial = false;//最初の3枚強制的に見せるの解除
						}
					}
				}
			}
			//Detail tap条件
			tutrialTapOK = (TUTRIAL_LEFT-5<=thumbsTutrialX[nowCenterTutrialNum]
					&& thumbsTutrialX[nowCenterTutrialNum]<=TUTRIAL_LEFT + 5);

//			batch.flush();
			DrawPopUpMask();

			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
//			//サムネイルマスク使うパーツ
			for(int i=0;i<TUTRIAL_KAZU;i++){
				if(i==nowCenterTutrialNum){
					assetObj.detail_page[1].setPosition(pageBtnLeftX + i*38, thumbsTutrialY-44);
					assetObj.detail_page[1].draw(batch);
				}
				else{
					assetObj.detail_page[0].setPosition(pageBtnLeftX + i*38, thumbsTutrialY-44);
					assetObj.detail_page[0].draw(batch);
				}
			}
			//closeボタン
			if((isFirstVisible3Tutrial && 3<=nowCenterTutrialNum) || !isFirstVisible3Tutrial){
				assetObj.popup_close.setPosition(882, realHeight-198);//sortの座標
				assetObj.popup_close.draw(batch);
			}
			//最後のボタン//TODO test
			/*
			if(nowCenterTutrialNum==TUTRIAL_KAZU-1){
				//L
				assetObj.ellipseBtn[1].setPosition(344, thumbsDetailY-243);//+60
				assetObj.ellipseBtn[1].draw(batch);
				batch.flush();
				//word_black
				//あとでーーPNGの名前を変える必要あり。もろもろ決まってから。
				wordBMask.bind(1);
				//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
				wordBMask.bind(0);
				assetObj.word_set_theme.setPosition(344, thumbsDetailY-243);
				assetObj.word_set_theme.draw(batch);
//			}
			*/
		}
		batch.end();
	}
	void DrawPopUpBg(){
		matGlaySP.setSize(1080, 20);
		matGlaySP.setPosition(0, topY-20);
		matGlaySP.draw(batch);
		matGlaySP.setSize(1080, 6);
		matGlaySP.setPosition(0, 0);
		matGlaySP.draw(batch);
		popupTopSP.setPosition(0, topY-20-64);
		popupTopSP.draw(batch);
		popupBottomSP.setPosition(0, 6);
		popupBottomSP.draw(batch);
	}
	void DrawPopUpMask(){
		setDefaultShader();
		matGlaySP.setSize(4, realHeight);
		matGlaySP.setPosition(0, 0);
		matGlaySP.draw(batch);
		matGlaySP.setPosition(1076, 0);
		matGlaySP.draw(batch);
		popupLeftRG.setRegionHeight(popUpSideHeight);
		batch.draw(popupLeftRG, 4, (topY-(20+64)-popUpSideHeight), 32, popUpSideHeight);
		popupRightRG.setRegionHeight(popUpSideHeight);
		batch.draw(popupRightRG, 1044, (topY-(20+64)-popUpSideHeight), 32, popUpSideHeight);
		batch.flush();
	}
	void DrawDetail(){
		if(nowDetailState == DetailState.DETAIL_NOMAL){
			if(nowAnimationState == AnimationState.ANIM_NONE) calcScrollFlickEaseDetail();
			//まずオーダー順で座標計算
			for(int y=0;y<thumbsTexKazuDetail  ;y++){
				int index = thumbsOrderDetail[y];
				//サムネイルY計算
	        	if(y==0) thumbsDetailX[index] = thumbsLeftX + DETAIL_LEFT - mEaseX;
	        	else thumbsDetailX[index] =thumbsDetailX[thumbsOrderDetail[y-1]] + DETAIL_ZONE_WIDTH;
			}
			//描画
			batch.begin();
			cashStart = false;
//			if(nowAnimationState == AnimationState.ANIM_CASH_DETAIL_SCREEN){
//				frameBufferCash.begin();
//				cashStart = true;
//			}
			//Detail tap条件
			detailTapOK = (DETAIL_LEFT-5<=thumbsDetailX[nowCenterDetailSPNum]
					&& thumbsDetailX[nowCenterDetailSPNum]<=DETAIL_LEFT + 5);
			//bg_mat
			bgSprite.draw(batch);
			DrawPopUpBg();
			//サムネイル
			for(int i=0;i<thumbsTexKazuDetail;i++){
				if(0<= thumbsDetailX[i] + DETAIL_TEX_WIDTH&&  thumbsDetailX[i]<=1080){
					DrawThumbsDetail(i);
				}
				//画面から見切れたタイミングで再読み込みの予約あれば読み込み
				else{
					//前回と違うのが中央来た、かつ再読み込みの予約あり--中央じゃなくなった時点で読み込みに行く
					if(detailArtReloadReserved.get(nowIndexDetail[i])){
						DebugLog.instance.outputLog("apicheck","自動で再読み込み!!!!!!!!!!!!!!!!!!!!!!:::" + nowIndexDetail[i]);
						getDetailImage(nowIndexDetail[i]);
					}
				}
			}
			thumbsSpriteDetailText.setPosition(232, thumbsDetailY+1154);
			thumbsSpriteDetailText.draw(batch);
			batch.flush();
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
//			//サムネイルマスク使うパーツ
//			for(int i=0;i<thumbsTexKazuDetail;i++){
//				if(0<= thumbsDetailX[i] + DETAIL_TEX_WIDTH&&  thumbsDetailX[i]<=1080){
//					DrawThumbsDetailTag(i);
//				}
//			}
			DrawThumbsDetailParts();
			batch.flush();
			//popup
			DrawPopUpMask();
			//closeボタン仮
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
//			assetObj.header_btn[0].setScale(ONE);
//			assetObj.header_btn[0].setPosition(844, realHeight-237);//内側
//			assetObj.header_btn[0].draw(batch);
//			assetObj.header_close.setScale(ONE);
//			assetObj.header_close.setPosition(844, realHeight-237);//内側
//			assetObj.header_close.draw(batch);
			assetObj.popup_close.setPosition(882, realHeight-198);//sortの座標
			assetObj.popup_close.draw(batch);
			batch.flush();
//			if(cashStart){
//				frameBufferCash.end();
//				//一度描画する
//				if(isOS404) cashScreenSP.setTexture(frameBufferCash.getColorBufferTexture());//ここで一度セット
//				setDefaultShader();
//				cashScreenSP.draw(batch);
//				batch.flush();
//				initializeDetailCloseAnim();
//			}
			//共通
			batch.end();
		}
		else if(nowDetailState == DetailState.DETAIL_LOADING){
			batch.begin();
			//bg_mat
			bgSprite.draw(batch);
			DrawPopUpBg();
			batch.flush();
			assetObj.downloading_detail.setPosition(DETAIL_LEFT, thumbsDetailY);
			assetObj.downloading_detail.draw(batch);
			DrawPopUpMask();
			DrawLoadingMask();
			batch.end();
		}
		else if(nowDetailState == DetailState.DETAIL_LOADING_ERROR){
			if(!visibleDialog){
				saveTouchState= nowTouchState;
				nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
				visibleDialog = true;
			}
			batch.begin();
			cashStart = false;
//			if(nowAnimationState == AnimationState.ANIM_CASH_DETAIL_SCREEN){
//				frameBufferCash.begin();
//				cashStart = true;
//			}
			//bg_mat
			bgSprite.draw(batch);
			DrawPopUpBg();
			batch.flush();
			assetObj.downloading_detail.setPosition(DETAIL_LEFT, thumbsDetailY);
			assetObj.downloading_detail.draw(batch);
			DrawPopUpMask();
			blackMaskSP.draw(batch);
//			if(cashStart){
//				batch.flush();
//				frameBufferCash.end();
//				//一度描画する
//				if(isOS404) cashScreenSP.setTexture(frameBufferCash.getColorBufferTexture());//ここで一度セット
//				setDefaultShader();
//				cashScreenSP.draw(batch);
//				batch.flush();
//				initializeDetailCloseAnim();
//			}
			batch.end();

//			//一旦自動で閉じてみる(ホントはダイアログ)
//			initializeDetailCloseAnim();
		}

	}
	void DrawThumbsTutrial(int num){
		thumbsSpriteTutrial[num].setPosition(thumbsTutrialX[num], thumbsTutrialY);
		thumbsSpriteTutrial[num].draw(batch);
	}
	void DrawThumbsTutrialParts(){
		for(int i=0;i<TUTRIAL_KAZU;i++){
			if(i==nowCenterDetailNum){
				assetObj.detail_page[1].setPosition(pageBtnLeftX + i*38, thumbsDetailY-113);
				assetObj.detail_page[1].draw(batch);
			}
			else{
				assetObj.detail_page[0].setPosition(pageBtnLeftX + i*38, thumbsDetailY-113);
				assetObj.detail_page[0].draw(batch);
			}
		}
		batch.flush();
	}
	void DrawThumbsDetail(int num){
		if (nowChangingThumbsDetail[num]) {
			assetObj.downloading_detail.setPosition(thumbsDetailX[num], thumbsDetailY);
			assetObj.downloading_detail.draw(batch);
			//TODO
//			if(detailArtFailed.get(nowIndexDetail[num])){
			if(!detailArtNowCallAPI.get(nowIndexDetail[num]) && detailArtFailed.get(nowIndexDetail[num])){//読み込み中ではない
				reloadWordSP.setPosition(thumbsDetailX[num]+184, thumbsDetailY+488);
				reloadWordSP.draw(batch);
			}
			else if(nowDetailState  != DetailState.DETAIL_LOADING){
				loadingSP.setPosition(thumbsDetailX[num]+253, thumbsDetailY+493);
				loadingRota[num] += delta*LOADING_ANIM_SPEED;
				loadingSP.setRotation(loadingRota[num] );
				loadingSP.draw(batch);
			}
		} else {
			thumbsSpriteDetail[num].setPosition(thumbsDetailX[num], thumbsDetailY);
			thumbsSpriteDetail[num].draw(batch);
		}
	}

	void DrawThumbsRecomendTag(int num){
		int tagState = -1;
		if(cto.get(nowIndex[num]).isPremium) tagState = 2;
		else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
		else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//		if(ctoIsNew.get(num)) tagState=0;
//		else if(cto.get(num).isLimitted) tagState = 1;
//		else if(cto.get(num).isPremium) tagState = 2;
		if(0<=tagState){
			float tagY =(thumbsY[num]+334)- ((THUMBS_TEX_SIZE - (THUMBS_TEX_SIZE*thumbsScale[num]))*0.5f);
			float tagX = thumbsX[num] +  (2* thumbsScale[num]);
			assetObj.thumbs_tag[tagState].setScale(1f);
//			assetObj.thumbs_tag[tagState].setPosition(thumbsX[num] + ajustX, thumbsY[num]+334 - ajust);
			assetObj.thumbs_tag[tagState].setPosition(tagX,tagY);
			assetObj.thumbs_tag[tagState].draw(batch);
		}
	}

	void DrawThumbsDetailParts(){
		for(int i=0;i<thumbsTotalDetail;i++){
			if(i==nowCenterDetailNum){
				assetObj.detail_page[1].setPosition(pageBtnLeftX + i*38, thumbsDetailY-113);
				assetObj.detail_page[1].draw(batch);
			}
			else{
				assetObj.detail_page[0].setPosition(pageBtnLeftX + i*38, thumbsDetailY-113);
				assetObj.detail_page[0].draw(batch);
			}
		}
		int state = (detailCto.isFavorite)?1:0;
		//like
		assetObj.quick_like[state].setPosition(5, thumbsDetailY-295);//+164
		assetObj.quick_like[state].draw(batch);
		//下のボタンとテキスト
//		ContentsFileName type = thumbInfoArray.get(nowCenterDetailNum).getFileType();
//		DebugLog.instance.outputLog("loop", "type::" + type);
		//alpha
		if(nowChangingThumbsDetail[nowCenterDetailSPNum]){
			batch.flush();
			ETCShaderAlphaSet(0.5f);
		}
		//L
		assetObj.ellipseBtn[state_ellipseBtnLeft].setPosition(189, thumbsDetailY-243);//+60
		assetObj.ellipseBtn[state_ellipseBtnLeft].draw(batch);
		if(state_ellipseBtnLeft==0){
			assetObj.word_set_theme.setPosition(189, thumbsDetailY-243);
			assetObj.word_set_theme.draw(batch);
		}
		batch.flush();
		//右側のボタン設定
		detailWordSP= SetDrawBtnRight(nowCenterDetailType);
//		if(!nowChangingThumbsDetail[nowCenterDetailSPNum] && detailbtnAlphaRight==0.5f){
//			batch.flush();
//			ETCShaderAlphaSet(0.5f);
//		}
		if(0<detailbtnAlphaRight){//phase1非表示にする　20150821
			//R
			assetObj.ellipseBtn[state_ellipseBtnRight].setPosition(619, thumbsDetailY-243);
			assetObj.ellipseBtn[state_ellipseBtnRight].draw(batch);
			if(state_ellipseBtnRight==0){
				detailWordSP.setPosition(619, thumbsDetailY-243);
				detailWordSP.draw(batch);
			}
			batch.flush();
		}
		//alpha
//		if(nowChangingThumbsDetail[nowCenterDetailSPNum] || detailbtnAlphaRight==0.5f){
		if(nowChangingThumbsDetail[nowCenterDetailSPNum]){
			ETCShaderAlphaSet(1.0f);
		}
		//word_black
		wordBMask.bind(1);
		//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
		wordBMask.bind(0);
		if(state_ellipseBtnLeft==1){
			assetObj.word_set_theme_sel.setPosition(189, thumbsDetailY-243);
			assetObj.word_set_theme_sel.draw(batch);
		}
		else if(state_ellipseBtnRight==1){
			detailWordSP = SetDrawBtnRightSel(nowCenterDetailType);
			detailWordSP.setPosition(619, thumbsDetailY-243);
			detailWordSP.draw(batch);
		}
		detailWordSP= GetDrawDetailWordSP(nowCenterDetailType);
		detailWordSP.setPosition(DETAIL_LEFT, thumbsDetailY-87);
		detailWordSP.draw(batch);
		batch.flush();
	}
	//アルファもセット
	Sprite SetDrawBtnRight(ContentsFileName type){
		switch(type){
		case ThumbDetailWp1:
		case ThumbDetailWp2:
		case ThumbDetailWp3:
		case ThumbDetailWp4:
		case ThumbDetailWp5:
//			detailbtnAlphaRight = 0.5f;
			detailbtnAlphaRight = 1f;//phase1非表示にする　20150821
			return assetObj.word_set_wall;
		case ThumbDetailDrawer:
//			detailbtnAlphaRight = 0.5f;
			detailbtnAlphaRight = 0.0f;//phase1非表示にする　20150821
			return assetObj.word_set_wall;
		case ThumbDetailIcon:
			detailbtnAlphaRight = 1f;
			return assetObj.word_set_drawer_icon;
		case ThumbDetailIconShortut://TODO
			detailbtnAlphaRight = 1f;
			return assetObj.word_set_drawer_icon_sc;
		case ThumbDetailWdtBattery:
			detailbtnAlphaRight = 1f;
			return assetObj.word_set_widget;
		case ThumbDetailEx1:
		case ThumbDetailEx2:
		case ThumbDetailEx3:
		case ThumbDetailEx4:
		case ThumbDetailEx5:
		case ThumbDetailEx6:
		case ThumbDetailEx7:
		case ThumbDetailEx8:
		case ThumbDetailEx9:
		case ThumbDetailEx10:
//			detailbtnAlphaRight = 0.5f;
			detailbtnAlphaRight = 0.0f;//phase1非表示にする　20150821
			return assetObj.word_set_wall;
		default:
			break;
		}
		return assetObj.word_set_wall;
	}
	Sprite SetDrawBtnRightSel(ContentsFileName type){
		switch(type){
		case ThumbDetailWp1:
		case ThumbDetailWp2:
		case ThumbDetailWp3:
		case ThumbDetailWp4:
		case ThumbDetailWp5:
			detailbtnAlphaRight = ONE;
			return assetObj.word_set_wall_sel;
		case ThumbDetailDrawer:
			detailbtnAlphaRight = ONE;
			return assetObj.word_set_wall_sel;
		case ThumbDetailIcon:
			detailbtnAlphaRight = ONE;
			return assetObj.word_set_drawer_icon_sel;
		case ThumbDetailIconShortut:
			detailbtnAlphaRight = ONE;
			return assetObj.word_set_shortcut_icon_sel;
		case ThumbDetailWdtBattery:
			detailbtnAlphaRight = ONE;
			return assetObj.word_set_widget_sel;
		case ThumbDetailEx1:
		case ThumbDetailEx2:
		case ThumbDetailEx3:
		case ThumbDetailEx4:
		case ThumbDetailEx5:
			detailbtnAlphaRight = ONE;
			return assetObj.word_set_wall_sel;
		default:
			break;
		}
		return assetObj.word_set_wall_sel;
	}
	Sprite GetDrawDetailWordSP(ContentsFileName type){
		switch(type){
		case ThumbDetailWp1:
			return assetObj.word_d_wall[0];
		case ThumbDetailWp2:
			return assetObj.word_d_wall[1];
		case ThumbDetailWp3:
			return assetObj.word_d_wall[2];
		case ThumbDetailWp4:
			return assetObj.word_d_wall[3];
		case ThumbDetailWp5:
			return assetObj.word_d_wall[4];
		case ThumbDetailDrawer:
			return assetObj.word_d_drawer_image;
		case ThumbDetailIcon:
			return assetObj.word_d_drawer_icon;
		case ThumbDetailIconShortut:
			return assetObj.word_d_shortcut_icon;
		case ThumbDetailWdtBattery:
			return assetObj.word_d_bat_widget;
		case ThumbDetailEx1:
		case ThumbDetailEx2:
		case ThumbDetailEx3:
		case ThumbDetailEx4:
		case ThumbDetailEx5:
			return assetObj.word_d_example;
		default:
			break;
		}
		return assetObj.word_d_wall[0];
	}
	void DrawThumbsList(int num){
		if (nowChangingThumbs[num]) {
			thumbsNowLoadingSprite.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
			thumbsNowLoadingSprite.draw(batch);
			loadingSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST+157, thumbsY[num]+THUMBS_IMAGE_Y_LIST+157);
			loadingRota[num] += delta*LOADING_ANIM_SPEED;
			loadingSP.setRotation(loadingRota[num]);
			loadingSP.draw(batch);
		}
//		else {
//			thumbsSprite[num].setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
//			thumbsSprite[num].draw(batch);
//			thumbsSpriteText[num].setPosition(thumbsX[num]+THUMBS_TEXT_X_LIST, thumbsY[num]+THUMBS_TEXT_Y_LIST);
//			thumbsSpriteText[num].draw(batch);
//		}
		else {
			//サムネイルの位置
			thumbsSprite[num].setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
			//クイックプレビュー
			if(thumbsIsQuick[num] && 1<thumbsQpVol[num]){
				if(thumbsQ_stopScroll[num] && !nowSettingThemes) thumbsQ_waitScroll[num]+=delta;
				else thumbsQ_scrollX[num] = emPreviewMove[num].actEOUT(delta);
				if(THUMBS_U<=thumbsQ_scrollX[num] && !thumbsQ_stopScroll[num] && !nowSettingThemes){
					thumbsQ_scrollXAdd[num]+=THUMBS_U;
					//Reset
					if(THUMBS_U*thumbsQpVol[num]<=thumbsQ_scrollXAdd[num]) thumbsQ_scrollXAdd[num]=0f;
					thumbsQ_scrollX[num] = 0;
					thumbsQ_stopScroll[num] =true;
					emPreviewMove[num].ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK);
				}
				else if(thumbsQ_scrollX[num]<=-THUMBS_U && !thumbsQ_stopScroll[num]  && !nowSettingThemes){
					thumbsQ_scrollXAdd[num]-=THUMBS_U;
					//Reset
					if(thumbsQ_scrollXAdd[num]<=-THUMBS_U*thumbsQpVol[num]) thumbsQ_scrollXAdd[num]=0f;
					thumbsQ_scrollX[num] = 0;
					thumbsQ_stopScroll[num] =true;
					emPreviewMove[num].ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK);//右に進ませる
				}
				if(WAITTIME_QUICK<=thumbsQ_waitScroll[num]){
					thumbsQ_stopScroll[num] =false;
					thumbsQ_waitScroll[num] = 0;
				}
				if(thumbsQpVol[num]==2 && THUMBS_U<thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]){
					thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
					thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
					thumbsSprite[num].draw(batch);
					//横//2個目以降透過するのを利用
					batch.flush();
					setETCShader();
					thumbsMask.bind(1);
					//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
					thumbsMask.bind(0);
					thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
					thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U*2);
					thumbsSprite[num].draw(batch);
					batch.flush();
					setDefaultShader();
				}
				else if(thumbsQpVol[num]==2 && thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]<0){
					thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]-THUMBS_U);
					thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
					thumbsSprite[num].draw(batch);
					//横//2個目以降透過するのを利用
					batch.flush();
					setETCShader();
					thumbsMask.bind(1);
					//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
					thumbsMask.bind(0);
					thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
					thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
					thumbsSprite[num].draw(batch);
					batch.flush();
					setDefaultShader();
				}
				else{
					thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
					thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
					thumbsSprite[num].draw(batch);
				}
				if(0<thumbsQ_flash[num]){
					thumbsQ_flash[num]-=delta*QPFLASH_SPEED;
					if(thumbsQ_flash[num]<=0) thumbsQ_flash[num]=0;
					quickPreviewFlashSP.setAlpha(thumbsQ_flash[num]);
					quickPreviewFlashSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
					quickPreviewFlashSP.draw(batch);
				}
				//プレビューマスク
				quickPreviewMaskSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
				quickPreviewMaskSP.draw(batch);
			}
			else if(thumbsQ_scrollAlpha[num]<1f && 1<thumbsQpVol[num]){
				thumbsQ_scrollAlpha[num]+=delta*2f;
				if(thumbsQpVol[num]==2){
					if(THUMBS_U<thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]){
						thumbsSprite[num].setAlpha(1f);
						thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
						thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
						thumbsSprite[num].setScale(thumbsScale[num]);
						thumbsSprite[num].draw(batch);
						//横//2個目以降透過するのを利用
						batch.flush();
						setETCShader();
						thumbsMask.bind(1);
						//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
						thumbsMask.bind(0);
						thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
						thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U*2);
						thumbsSprite[num].setScale(thumbsScale[num]);
						thumbsSprite[num].draw(batch);
						batch.flush();
						setDefaultShader();
					}
					else if(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]<0){
						thumbsSprite[num].setAlpha(1f);
						thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]-THUMBS_U);
						thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
						thumbsSprite[num].setScale(thumbsScale[num]);
						thumbsSprite[num].draw(batch);
						//横//2個目以降透過するのを利用
						batch.flush();
						setETCShader();
						thumbsMask.bind(1);
						//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
						thumbsMask.bind(0);
						thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
						thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
						thumbsSprite[num].setScale(thumbsScale[num]);
						thumbsSprite[num].draw(batch);
						batch.flush();
						setDefaultShader();
					}
					else{
						thumbsSprite[num].setAlpha(1f);
						thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
						thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
						thumbsSprite[num].setScale(thumbsScale[num]);
						thumbsSprite[num].draw(batch);
					}
				}
				else{
					thumbsSprite[num].setAlpha(1f);
					thumbsSprite[num].setU(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]);
					thumbsSprite[num].setU2(thumbsQ_scrollXAdd[num]+thumbsQ_scrollX[num]+THUMBS_U);
					thumbsSprite[num].setScale(thumbsScale[num]);
					thumbsSprite[num].draw(batch);
				}
				if(thumbsQ_scrollAlpha[num]>1f){
					thumbsQ_scrollAlpha[num]=1f;
					disableQuickState(num);
				}
				thumbsSprite[num].setAlpha(thumbsQ_scrollAlpha[num]);
				thumbsSprite[num].setU(0);
				thumbsSprite[num].setU2(THUMBS_U);
				thumbsSprite[num].setScale(thumbsScale[num]);
				thumbsSprite[num].draw(batch);
				quickPreviewFlashSP.setAlpha(1f-thumbsQ_scrollAlpha[num]);
				quickPreviewFlashSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
				quickPreviewFlashSP.draw(batch);
				//プレビューマスク
				quickPreviewMaskSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
				quickPreviewMaskSP.draw(batch);
			}
			else{//通常
				thumbsSprite[num].draw(batch);
			}
			//テキスト
			thumbsSpriteText[num].setPosition(thumbsX[num]+THUMBS_TEXT_X_LIST, thumbsY[num]+THUMBS_TEXT_Y_LIST);
			thumbsSpriteText[num].draw(batch);
		}
//		if(nowIndex[num]!=thumbsTotal-1){
			borderSP.setPosition(0, thumbsY[num]-6);
			borderSP.draw(batch);
//		}
	}
	void DrawThumbsListParts(int num){
		if(state_detailNum==num) state_quick_detail = 1;
		else state_quick_detail = 0;
		if(state_setNum==num) state_quick_set = 1;
		else state_quick_set = 0;
		state_quick_like = (cto.get(nowIndex[num]).isFavorite)?1:0;

		assetObj.quick_detail[state_quick_detail].setPosition(thumbsX[num]+ListPosiDetail.x, thumbsY[num]+ListPosiDetail.y);
		assetObj.quick_like[state_quick_like].setPosition(thumbsX[num]+ListPosiLike.x, thumbsY[num]+ListPosiLike.y);//+164
		assetObj.quick_set[state_quick_set].setPosition(thumbsX[num]+ListPosiSet.x, thumbsY[num]+ListPosiSet.y);
		assetObj.quick_detail[state_quick_detail].draw(batch);
		assetObj.quick_like[state_quick_like].draw(batch);
		assetObj.quick_set[state_quick_set].draw(batch);
		//tag
		int tagState = -1;
		if(cto.get(nowIndex[num]).isPremium) tagState = 2;
		else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
		else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//		if(cto.get(nowIndex[num]).isPremium) tagState = 2;
//		else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
//		else if(ctoIsNew.get(nowIndex[num])) tagState=0;

		if(0<=tagState){
//			assetObj.thumbs_tag[tagState].setPosition(thumbsX[num]+THUMBS_TEXT_X_LIST, thumbsY[num]+311);
//			assetObj.thumbs_tag[tagState].draw(batch);
			assetObj.thumbs_tag[tagState].setPosition(thumbsX[num]+95, thumbsY[num]+381);
			assetObj.thumbs_tag[tagState].draw(batch);
		}
		//update
		if(nowScreenState == ScreenState.SCREEN_NEWART) DrawDate(ctoDate.get(nowIndex[num]),thumbsY[num]);
		else{
//			assetObj.ranking_crown.setPosition(thumbsX[num]+41, thumbsY[num]+224);
//			assetObj.ranking_crown.draw(batch);
//			assetObj.num_ranking[nowIndex[num]].setPosition(thumbsX[num]+74, thumbsY[num]+232);
//			assetObj.num_ranking[nowIndex[num]].draw(batch);
			assetObj.ranking_crown.setPosition(thumbsX[num]+564, thumbsY[num]+311);
			assetObj.ranking_crown.draw(batch);
			assetObj.num_ranking[nowIndex[num]].setPosition(thumbsX[num]+597, thumbsY[num]+318);
			assetObj.num_ranking[nowIndex[num]].draw(batch);
		}

	}
	void DrawThumbsRecomend(int num){
		if (nowChangingThumbs[num]) {
			thumbsNowLoadingSprite.setScale(thumbsScale[num]);
			thumbsNowLoadingSprite.setPosition(thumbsX[num], thumbsY[num]);
			thumbsNowLoadingSprite.draw(batch);
			loadingSP.setPosition(thumbsX[num]+157, thumbsY[num]+157);
			loadingRota[num] += delta*LOADING_ANIM_SPEED;
			loadingSP.setRotation(loadingRota[num]);
			loadingSP.draw(batch);
		} else {
			thumbsSprite[num].setScale(thumbsScale[num]);
			thumbsSprite[num].setPosition(thumbsX[num], thumbsY[num]);
			//クイックプレビュー
			if(nowRecomendState == RecomendState.PEVIEW_MODE && num==previewNum && 1<thumbsQpVol[num]){
//				float limit = 3f;
//				if(thumbsQpVol[num]==2) limit = 2f;
				if(q_stopScroll  && !nowSettingThemes) q_waitScroll+=delta;
				else q_scrollX = emQuickMove.actEOUT(delta);
				if(THUMBS_U<=q_scrollX && !q_stopScroll  && !nowSettingThemes){
					q_scrollXAdd+=THUMBS_U;
					//Reset
					if(THUMBS_U*thumbsQpVol[num]<=q_scrollXAdd) q_scrollXAdd=0f;
					q_scrollX = 0;
					q_stopScroll =true;
					emQuickMove.ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK);
				}
				else if(q_scrollX<=-THUMBS_U && !q_stopScroll  && !nowSettingThemes){
					q_scrollXAdd-=THUMBS_U;
					//Reset
					if(q_scrollXAdd<=-THUMBS_U*thumbsQpVol[num]) q_scrollXAdd=0f;
					q_scrollX = 0;
					q_stopScroll =true;
					emQuickMove.ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK);//右に進ませる
				}
				if(WAITTIME_QUICK<=q_waitScroll){
					q_stopScroll =false;
					q_waitScroll = 0;
				}
				if(thumbsQpVol[num]==2 && THUMBS_U<q_scrollXAdd+q_scrollX){
					thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
					thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
					thumbsSprite[num].draw(batch);
					//横//2個目以降透過するのを利用
					batch.flush();
					setETCShader();
					thumbsMask.bind(1);
					//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
					thumbsMask.bind(0);
					thumbsSprite[num].setU(q_scrollXAdd+q_scrollX+THUMBS_U);
					thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U*2);
					thumbsSprite[num].draw(batch);
					batch.flush();
					setDefaultShader();
				}
				else if(thumbsQpVol[num]==2 && q_scrollXAdd+q_scrollX<0){
					thumbsSprite[num].setU(q_scrollXAdd+q_scrollX-THUMBS_U);
					thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX);
					thumbsSprite[num].draw(batch);
					//横//2個目以降透過するのを利用
					batch.flush();
					setETCShader();
					thumbsMask.bind(1);
					//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
					thumbsMask.bind(0);
					thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
					thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
					thumbsSprite[num].draw(batch);
					batch.flush();
					setDefaultShader();
				}
				else{
					thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
					thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
					thumbsSprite[num].draw(batch);
				}
			}
			else if(nowRecomendState == RecomendState.PEVIEW_ZOOMOUT  && num==previewNum && 1<thumbsQpVol[num]){
				q_scrollAlpha+=delta*2f;
				if(q_scrollAlpha<1f){
					if(thumbsQpVol[num]==2){
						if(THUMBS_U<q_scrollXAdd+q_scrollX){
							thumbsSprite[num].setAlpha(1f);
							thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
							thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
							thumbsSprite[num].draw(batch);
							//横//2個目以降透過するのを利用
							batch.flush();
							setETCShader();
							thumbsMask.bind(1);
							//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
							thumbsMask.bind(0);
							thumbsSprite[num].setU(q_scrollXAdd+q_scrollX+THUMBS_U);
							thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U*2);
							thumbsSprite[num].draw(batch);
							batch.flush();
							setDefaultShader();
						}
						else if(q_scrollXAdd+q_scrollX<0){
							thumbsSprite[num].setAlpha(1f);
							thumbsSprite[num].setU(q_scrollXAdd+q_scrollX-THUMBS_U);
							thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX);
							thumbsSprite[num].draw(batch);
							//横//2個目以降透過するのを利用
							batch.flush();
							setETCShader();
							thumbsMask.bind(1);
							//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
							thumbsMask.bind(0);
							thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
							thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
							thumbsSprite[num].draw(batch);
							batch.flush();
							setDefaultShader();
						}
						else{
							thumbsSprite[num].setAlpha(1f);
							thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
							thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
							thumbsSprite[num].draw(batch);
						}
					}
					else{
						thumbsSprite[num].setAlpha(1f);
						thumbsSprite[num].setU(q_scrollXAdd+q_scrollX);
						thumbsSprite[num].setU2(q_scrollXAdd+q_scrollX+THUMBS_U);
						thumbsSprite[num].draw(batch);
					}
				}
				else{
					q_scrollAlpha=1f;
					q_scrollXAdd = 0;
					q_scrollX = 0;
					q_waitScroll = 0;
					q_stopScroll = true;
				}
				thumbsSprite[num].setAlpha(q_scrollAlpha);
				thumbsSprite[num].setU(0);
				thumbsSprite[num].setU2(THUMBS_U);
				thumbsSprite[num].draw(batch);
			}
			else{
				thumbsSprite[num].draw(batch);
			}
		}
	}
	void DrawThumbsRecomendShuffle(int num){
		if(shuffle1state == 1 || shuffle2state == 1 ){
			setETCShader();
			thumbsMask.bind(1);
			thumbsMask.bind(0);
			boolean visibleTumbs = emPreviewMove[num].isFinishAnim();
			if(visibleTumbs){
				//サムネイル描画
				if (nowChangingThumbs[num]) {
					thumbsNowLoadingSprite.setScale(thumbsScale[num]);
					thumbsNowLoadingSprite.setPosition(thumbsX[num], thumbsY[num]);
					thumbsNowLoadingSprite.draw(batch);
					batch.flush();
					setDefaultShader();
					loadingSP.setPosition(thumbsX[num]+157, thumbsY[num]+157);
					loadingRota[num] += delta*LOADING_ANIM_SPEED;
					loadingSP.setRotation(loadingRota[num]);
					loadingSP.draw(batch);
					batch.flush();
					setETCShader();
					thumbsMask.bind(1);
					thumbsMask.bind(0);
				} else {
					thumbsSprite[num].setScale(thumbsScale[num]);
					thumbsSprite[num].setPosition(thumbsX[num], thumbsY[num]);
					thumbsSprite[num].draw(batch);
				}
				batch.flush();
			}

			int col = shuffleDto.get(num).col;
//			int col = 0;
			circleMask.bind(1);
			matColorSP[col].getTexture().bind(0);
			matColorSP[col].setScale(thumbsBallScale[num]);
			matColorSP[col].setPosition(thumbsX[num], thumbsY[num]);
			matColorSP[col].draw(batch);
			batch.flush();
			//tag
//			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			int tagState = -1;
			if(cto.get(nowIndex[num]).isPremium) tagState = 2;
			else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
			else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//			if(ctoIsNew.get(num)) tagState=0;
//			else if(cto.get(num).isLimitted) tagState = 1;
//			else if(cto.get(num).isPremium) tagState = 2;
			if(0<=tagState){
				float tagX= thumbsX[num] +  (2* thumbsRecomendScale[num]);
				float tagY =(thumbsY[num]+334)- ((THUMBS_TEX_SIZE - (THUMBS_TEX_SIZE*thumbsRecomendScale[num]))*0.5f);
				assetObj.thumbs_tag[tagState].setOrigin((thumbsX[num]+THUMBS_TEX_SIZE*0.5f) - tagX, (thumbsY[num]+THUMBS_TEX_SIZE*0.5f) - tagY);
				if(nowRecomendState == RecomendState.SHUFFLE_ANIM2){
					if(visibleTumbs && thumbsRecomendScale[num]>=thumbsBallScale[num]) assetObj.thumbs_tag[tagState].setScale(thumbsScale[num]/thumbsRecomendScale[num]);
					else  assetObj.thumbs_tag[tagState].setScale(thumbsBallScale[num]/thumbsRecomendScale[num]);
				}
				else{
					if(visibleTumbs) assetObj.thumbs_tag[tagState].setScale(thumbsScale[num]/thumbsRecomendScale[num]);
					else  assetObj.thumbs_tag[tagState].setScale(thumbsBallScale[num]/thumbsRecomendScale[num]);
				}
				assetObj.thumbs_tag[tagState].setPosition(tagX,tagY);
				assetObj.thumbs_tag[tagState].draw(batch);
			}
			batch.flush();
		}
		else if(shuffle3state == 1){
			setETCShader();
			thumbsMask.bind(1);
			thumbsMask.bind(0);
			boolean visibleTumbs = 0.08f<emPreviewMove[shuffleSize].getStatetime();
			if(visibleTumbs){
				//サムネイル描画
				if (nowChangingThumbs[num]) {
					thumbsNowLoadingSprite.setScale(thumbsScale[num]);
					thumbsNowLoadingSprite.setPosition(thumbsX[num], thumbsY[num]);
					thumbsNowLoadingSprite.draw(batch);
					batch.flush();
					setDefaultShader();
					loadingSP.setPosition(thumbsX[num]+157, thumbsY[num]+157);
					loadingRota[num] += delta*LOADING_ANIM_SPEED;
					loadingSP.setRotation(loadingRota[num]);
					loadingSP.draw(batch);
					batch.flush();
					setETCShader();
					thumbsMask.bind(1);
					thumbsMask.bind(0);
				} else {
					thumbsSprite[num].setScale(thumbsScale[num]);
					thumbsSprite[num].setPosition(thumbsX[num], thumbsY[num]);
					thumbsSprite[num].draw(batch);
				}
				batch.flush();
			}

			int col = shuffleDto.get(num).col;
//			int col = 0;
			circleMask.bind(1);
			matColorSP[col].getTexture().bind(0);
//			matColorSP[col].setScale(thumbsScale[num]);
			matColorSP[col].setScale(thumbsBallScale[num]);
			matColorSP[col].setPosition(thumbsX[num], thumbsY[num]);
			matColorSP[col].draw(batch);
			batch.flush();
			//tag
			if(num<thumbsTotal){
				parts1Mask.bind(1);
				//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
				parts1Mask.bind(0);
				int tagState = -1;
				if(cto.get(nowIndex[num]).isPremium) tagState = 2;
				else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
				else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//				if(ctoIsNew.get(num)) tagState=0;
//				else if(cto.get(num).isLimitted) tagState = 1;
//				else if(cto.get(num).isPremium) tagState = 2;
				if(0<=tagState){
					float tagX= thumbsX[num] +  (2* thumbsRecomendScale[num]);
					float tagY =(thumbsY[num]+334)- ((THUMBS_TEX_SIZE - (THUMBS_TEX_SIZE*thumbsRecomendScale[num]))*0.5f);
					assetObj.thumbs_tag[tagState].setOrigin((thumbsX[num]+THUMBS_TEX_SIZE*0.5f) - tagX, (thumbsY[num]+THUMBS_TEX_SIZE*0.5f) - tagY);
					if(visibleTumbs) assetObj.thumbs_tag[tagState].setScale(thumbsScale[num]/thumbsRecomendScale[num]);
					else  assetObj.thumbs_tag[tagState].setScale(thumbsBallScale[num]/thumbsRecomendScale[num]);
					assetObj.thumbs_tag[tagState].setPosition(tagX,tagY);
					assetObj.thumbs_tag[tagState].draw(batch);
				}
				batch.flush();
			}
		}
		else{
			setETCShader();
			thumbsMask.bind(1);
			thumbsMask.bind(0);
			if (nowChangingThumbs[num]) {
				thumbsNowLoadingSprite.setScale(thumbsScale[num]);
				thumbsNowLoadingSprite.setPosition(thumbsX[num], thumbsY[num]);
				thumbsNowLoadingSprite.draw(batch);
				batch.flush();
				setDefaultShader();
				loadingSP.setPosition(thumbsX[num]+157, thumbsY[num]+157);
				loadingRota[num] += delta*LOADING_ANIM_SPEED;
				loadingSP.setRotation(loadingRota[num]);
				loadingSP.draw(batch);
				batch.flush();
				setETCShader();
				thumbsMask.bind(1);
				thumbsMask.bind(0);
			} else {
				thumbsSprite[num].setScale(thumbsScale[num]);
				thumbsSprite[num].setPosition(thumbsX[num], thumbsY[num]);
				thumbsSprite[num].draw(batch);
			}
			batch.flush();
			//tag
			setETCShader();
			parts1Mask.bind(1);
			//形が一緒だから一度バインドすればいいっぽい(しかもバインドするのは何でもいい--スプライト描画時に決まる)
			parts1Mask.bind(0);
			int tagState = -1;
			if(cto.get(nowIndex[num]).isPremium) tagState = 2;
			else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
			else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//			if(ctoIsNew.get(num)) tagState=0;
//			else if(cto.get(num).isLimitted) tagState = 1;
//			else if(cto.get(num).isPremium) tagState = 2;
			if(0<=tagState){
				float tagX= thumbsX[num] +  (2* thumbsRecomendScale[num]);
				float tagY =(thumbsY[num]+334)- ((THUMBS_TEX_SIZE - (THUMBS_TEX_SIZE*thumbsRecomendScale[num]))*0.5f);
				assetObj.thumbs_tag[tagState].setOrigin((thumbsX[num]+THUMBS_TEX_SIZE*0.5f) - tagX, (thumbsY[num]+THUMBS_TEX_SIZE*0.5f) - tagY);
				assetObj.thumbs_tag[tagState].setScale(thumbsScale[num]/thumbsRecomendScale[num]);
				assetObj.thumbs_tag[tagState].setPosition(tagX,tagY);
				assetObj.thumbs_tag[tagState].draw(batch);
			}
			batch.flush();
		}
	}
	//"2015/7/14"
	//新着用日付描画用
	void DrawDate(String date,float thumbY){
		int len = date.length();
		//マージン26
		if(8<=len){//ちゃんと返って来てる--基本返って来る想定…
//			//year
			for(int i=0;i<4;i++){
				int y = Integer.parseInt("" + date.charAt(i));
				assetObj.num_update[y].setPosition(25*i+566, thumbY+312);
				assetObj.num_update[y].draw(batch);
			}
			// "/"
			assetObj.num_update[10].setPosition(666, thumbY+312);//25*4+566
			assetObj.num_update[10].draw(batch);
			//month～
			int sharpNum = 100;
			for(int i=5;i<len;i++){
				char m = date.charAt(i);
				int d;
				if(m=='/'){
					d = 10;
					sharpNum = i;
//					DebugLog.instance.outputLog("flamework", "sharpNum;;" + sharpNum);
				}
				else d = Integer.parseInt("" + m);
				if(sharpNum<i) assetObj.num_update[d].setPosition(25*(i-5)+682-9, thumbY+312);
				else assetObj.num_update[d].setPosition(25*(i-5)+682, thumbY+312);
				assetObj.num_update[d].draw(batch);
			}
		}
	}
	void DrawThumbsMatrix(int num){
		if (nowChangingThumbs[num]) {
			thumbsNowLoadingSprite.setPosition(thumbsX[num]+THUMBS_IMAGE_X_MATRIX, thumbsY[num]+THUMBS_IMAGE_Y_MATRIX);
			thumbsNowLoadingSprite.draw(batch);
			loadingSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_MATRIX+157, thumbsY[num]+THUMBS_IMAGE_Y_MATRIX+157);
			loadingRota[num] += delta*LOADING_ANIM_SPEED;
			loadingSP.setRotation(loadingRota[num]);
			loadingSP.draw(batch);
		} else {
			thumbsSprite[num].setPosition(thumbsX[num]+THUMBS_IMAGE_X_MATRIX, thumbsY[num]+THUMBS_IMAGE_Y_MATRIX);
			thumbsSprite[num].draw(batch);
		}
	}
	void DrawThumbsMatrixParts(int num){
		if(state_detailNum==num) state_quick_detail = 1;
		else state_quick_detail = 0;
		if(state_setNum==num) state_quick_set = 1;
		else state_quick_set = 0;
		state_quick_like = (cto.get(nowIndex[num]).isFavorite)?1:0;

		assetObj.quick_detail[state_quick_detail].setPosition(thumbsX[num]+MatrixPosiDetail.x, thumbsY[num]+MatrixPosiDetail.y);
		assetObj.quick_like[state_quick_like].setPosition(thumbsX[num]+MatrixPosiLike.x, thumbsY[num]+MatrixPosiLike.y);//+164
		assetObj.quick_set[state_quick_set].setPosition(thumbsX[num]+MatrixPosiSet.x, thumbsY[num]+MatrixPosiSet.y);
		assetObj.quick_detail[state_quick_detail].draw(batch);
		assetObj.quick_like[state_quick_like].draw(batch);
		assetObj.quick_set[state_quick_set].draw(batch);
		//tag
		int tagState = -1;
		if(cto.get(nowIndex[num]).isPremium) tagState = 2;
		else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
		else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//		if(ctoIsNew.get(nowIndex[num])) tagState=0;
//		else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
//		else if(cto.get(nowIndex[num]).isPremium) tagState = 2;
		if(0<=tagState){
			assetObj.thumbs_tag[tagState].setPosition(thumbsX[num]+71, thumbsY[num]+481);
			assetObj.thumbs_tag[tagState].draw(batch);
		}
	}
	void SetAddLike(int num){
		if(!nowSettingFavorite){
			ContentsOperatorForCatalog.op.callChangeFavoriteTask(cto.get(num));
			nowSettingFavorite = true;
		}
	}
	void SetAddLike(ContentsDataDto  dCto){
		if(!nowSettingFavorite){
			ContentsOperatorForCatalog.op.callChangeFavoriteTask(dCto);
			nowSettingFavorite = true;
		}
	}
	public void onFailedSetFavorite() {
		DebugLog.instance.outputLog("api", "favorete失敗!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		nowSettingFavorite = false;
	}
	public void onFinishedSetFavorite(long setID) {
		DebugLog.instance.outputLog("api", "favorete成功!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		//より安全
		boolean isFavorite = ContentsOperatorForCatalog.op.getContentsDataFromAssetId(setID).isFavorite;
		if(isFavorite){
//		if(0<=addLikeNum){
			if(nowScreenState == ScreenState.SCREEN_DETAIL){
				if(detailCto.isFavorite){
					myActivity.CallToast("お気に入りに保存しました");
//					myBoxHeaderScale=0.85f;
				}
			}
			else{
//				if(isFavorite){
					myActivity.CallToast("お気に入りに保存しました");
					if(nowScreenState != ScreenState.SCREEN_MYBOX){
						startMyboxButtonAnim = true;
						myboxButtonAnimAlpha = 1f;
						myboxButtonAnimWait = 0f;
					}
//					myBoxHeaderScale=0.85f;
//				}
			}
		}
		else{
			if(nowScreenState == ScreenState.SCREEN_DETAIL){
				if(!detailCto.isFavorite){
					myActivity.CallToast("お気に入りから削除しました");
//					myBoxHeaderScale=0.85f;
				}
			}
			else{
				myActivity.CallToast("お気に入りから削除しました");
			}
		}
		nowSettingFavorite = false;
	}
	void setQuickRectZoneREC(int pos){
		rect_q_detail.x= thumbsX[previewNum] + previewPosiDetail[pos].x + QUICK_TOUCH_REC;
		rect_q_detail.y= thumbsY[previewNum] + previewPosiDetail[pos].y + QUICK_TOUCH_REC;
		rect_q_like.x= thumbsX[previewNum] + previewPosiLike[pos].x + QUICK_TOUCH_REC;
		rect_q_like.y= thumbsY[previewNum] + previewPosiLike[pos].y + QUICK_TOUCH_REC;
		rect_q_set.x= thumbsX[previewNum] + previewPosiSet[pos].x + QUICK_TOUCH_REC;
		rect_q_set.y= thumbsY[previewNum] + previewPosiSet[pos].y + QUICK_TOUCH_REC;
	}
	void setQuickRectZone(int num){
		setQuickRectZone(num,true);
	}
	void setQuickRectZone(int num,boolean isList){
		if(isList){
			rect_detail[num].x = thumbsX[num] + ListPosiDetail.x + QUICK_TOUCH_REC;
			rect_detail[num].y = thumbsY[num] + ListPosiDetail.y + QUICK_TOUCH_REC;
			rect_like[num].x = thumbsX[num] + ListPosiLike.x + QUICK_TOUCH_REC;
			rect_like[num].y = thumbsY[num] + ListPosiLike.y + QUICK_TOUCH_REC;
			rect_set[num].x = thumbsX[num] + ListPosiSet.x + QUICK_TOUCH_REC;
			rect_set[num].y = thumbsY[num] + ListPosiSet.y + QUICK_TOUCH_REC;
		}
		else{
			rect_detail[num].x = thumbsX[num] + MatrixPosiDetail.x + QUICK_TOUCH_REC;
			rect_detail[num].y = thumbsY[num] + MatrixPosiDetail.y + QUICK_TOUCH_REC;
			rect_like[num].x = thumbsX[num] + MatrixPosiLike.x + QUICK_TOUCH_REC;
			rect_like[num].y = thumbsY[num] + MatrixPosiLike.y + QUICK_TOUCH_REC;
			rect_set[num].x = thumbsX[num] + MatrixPosiSet.x + QUICK_TOUCH_REC;
			rect_set[num].y = thumbsY[num] + MatrixPosiSet.y + QUICK_TOUCH_REC;
		}
	}

	void resetStateBtn(){
		state_detailNum= -1;
		state_setNum= -1;
		state_ellipseBtnLeft =0;
		state_ellipseBtnRight =0;
	}

	@Override
	public void resize(int width, int height) {
		DebugLog.instance.outputLog("info", "resize=" + height);
		// 起動後初回のみ画面設定　20150903//バグ対応
		if(!setScreenSize && height!=0){
			setViewSize(width, height);
			saveScreenSize = height;
			DebugLog.instance.outputLog("info", "通常時 saveScreenSize=" + saveScreenSize);
		}
		else if(saveScreenSize != height && !nowLoadingSecondAssets){//TODO 20150911 インスタンスの生成が終わってたら
//		else if(saveScreenSize != height){
			DebugLog.instance.outputLog("info", "イレギュラー時 saveScreenSize=" + saveScreenSize);
			DebugLog.instance.outputLog("info", "イレギュラー時 height=" + height);
			setIrregularScreen(height);
			saveScreenSize = height;
		}
	}


	// 以前のライブラリを踏襲
	public void setViewSize(int width, int height) {
		viewWidth = width;
		viewHeight = height;
		DebugLog.instance.outputLog("info", "viewWidth=" + viewWidth);
		DebugLog.instance.outputLog("info", "viewHeight=" + viewHeight);
		// 起動後初回のみ画面設定　20150903//バグ対応
		setChangeScreen();
//		DebugLog.instance.outputLog("info", "Model Name in setViewSize=" + Build.MODEL);
//		setScreenSize = true;//TODO　16/10/27　スクリーンの高さを保存するタイミングをずらす(DoneLoading後)
	}
	void setIrregularScreen(int height){//LG端末のバグ対応
		DebugLog.instance.outputLog("info", "setIrregularScreen" + height);
		camera.viewportHeight = height;
		float changeY = (viewHeight-height)*uiPer*0.5f;
		camera.position.set(camPos.x, camPos.y+changeY, 0.0f);
		camera.update();
		cashScreenSP.setSize(1080, height*uiPer);
		cashScreenSP.setPosition(0, changeY*2f);
		maskScreenSP.setSize(1080, height*uiPer);
		maskScreenSP.setPosition(0, changeY*2f);
	}
	void setChangeScreen() {
		// 縦横変わっった時だけ呼ばれる
		DebugLog.instance.outputLog("info", "setChangeScree");
		camera.setToOrtho(false);
		// //画面比率を計算
		uiPer = 1080f / viewWidth;
		realWidth = viewWidth * uiPer;
		realHeight = viewHeight * uiPer;
		DebugLog.instance.outputLog("info", "realWidth=" + realWidth);
		DebugLog.instance.outputLog("info", "realHeight=" + realHeight);
		topY = realHeight;
		centerY = realHeight*0.5f;
		camPos.x = (viewWidth * uiPer) / 2f;
		camPos.y = (viewHeight * uiPer) / 2f;
		camera.zoom = uiPer;
		camera.position.set(camPos.x, camPos.y, 0.0f);
		camera.update();
	}


	@Override
	public void resume() {
		DebugLog.instance.outputLog("info", "resume screen !!!!!!!!!!!!!!");
//		DebugLog.instance.outputLog("check", "resume screen!!!!!!!!!!!!!!");
		if(nowScreenState == ScreenState.SCREEN_INFO) tapInfo = -1;
	}
	//フリック関係
	float reDraggX = 0f;
	float dragReY = 0f;
	float dragReX = 0f;
	float  flickYdist=0f;
	float  reserveFlickYdist=0f;
	//全体のデフォルトをイージングにした時の速度
	public int allEaseSpFlick					= 10;
	// イージングの際どこのポイントを基準にイージングさせるか
	public float mLastY					= 0.0f;
	public float mGoalY					= 0.0f;
	public float mEaseY					= 0.0f;
	private float offsetWidth				= 0;
	long lastFlingTime= System.currentTimeMillis();
	long lastTouchUpTime= System.currentTimeMillis();
//	long lastTouchDraggTime= System.currentTimeMillis();
    float flingStateTime;
    float addFling;
    float addFlingX;
    final float FLINGMAX=1200f;
    final float FLINGMAX_DETAIL=DETAIL_ZONE_WIDTH;
    final float FLINGPER=0.25f;
    float addDragg;
    final float DRAGGMAX=120f;
    final float DRAGGPER=1.1f;
    final float DRAGGMAX_DETAIL=80f;
    final float DRAGGPER_DETAIL=0.7f;
	float slideStartX;
	float slideStartY;
	float slideDistX;
	float slideDistY;
	float slideX;
	float slideY;
	boolean q_slideOK = false;//クイックプレビューのスライドOKか
	boolean isNowQuickSlide = false;//クイックプレビューのスライド中か--リストビュー時
	boolean isNowQuickTouch = false;//クイックプレビューのスライド中か--リストビュー時
	int isNowQuickTouchNum = 0;//クイックプレビューのスライド中か--リストビュー時
	//サムネイルタップ可能か
	boolean tapAbble = false;
	//メニューボタンゾーンかどうか判定
	boolean isMenuTouchDown = false;
	//bounce
	float bounceDist = 150f;
//	float bounceDistRec = 250f;
	float bounceDistRec = 480f;//変更20150909
    float  bounceGoal = 0f;
//    boolean permitBounce = false;
    boolean permitBounceUP = false;
    boolean permitBounceDOWN = false;
    boolean isBounce = true;
//	float bounceDistX = 100f;
//    float  bounceGoalX = 0f;
//    boolean permitBounceXL = false;
//    boolean permitBounceXR = false;
    boolean detailTouchUp = false;//詳細画面のタッチアップ発生--Flingが発生しない対策
    boolean tutrialTouchUp = false;//tutrial画面のタッチアップ発生--Flingが発生しない対策

	//フリック関係X
	float  flickXdist=0f;
    float addDraggX;
	// イージングの際どこのポイントを基準にイージングさせるか
	private float mLastX					= 0.0f;
	private float mGoalX					= 0.0f;
	private float mEaseX					= 0.0f;
	private float offsetWidthX				= 0;


    public void calcScrollFlickEase(){
    	if(nowTouchState == TouchState.TOUCH_SCROLL_BOUNCE){
    		mGoalY=offsetWidth-bounceGoal;
    		mEaseY+=(mGoalY-mLastY)/7f;
    		float val = Math.abs(mLastY-mEaseY);
    		if(val<5) tapAbble=true;
    		else tapAbble=false;
    		mLastY=mEaseY;
    		if(bounceGoal==0){
    			if(mEaseY-1<offsetWidth){
    				flickYdist=0;
    				nowTouchState = TouchState.TOUCH_SCROLL;
    				DebugLog.instance.outputLog("info", "nowTouchState = nowTouchState = TouchState.TOUCH_SCROLL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    			}
    		}
    		else{
    			if(0<mEaseY+1){
    				flickYdist=offsetWidth;
    				nowTouchState = TouchState.TOUCH_SCROLL;
    				DebugLog.instance.outputLog("info", "nowTouchState = nowTouchState = TouchState.TOUCH_SCROLL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    			}
    		}
    	}
    	else if(!isNowQuickTouch){
//    		DebugLog.instance.outputLog("loop", "mEaseY=" + mEaseY);
    		mGoalY=offsetWidth-flickYdist;
    		if(mGoalY<=-bounceDist) mGoalY=-bounceDist;
    		else if(mGoalY>=offsetWidth+bounceDist) mGoalY=offsetWidth+bounceDist;
    		mEaseY+=(mGoalY-mLastY)/allEaseSpFlick;
    		float val = Math.abs(mLastY-mEaseY);
    		if(val<5) tapAbble=true;
    		else tapAbble=false;
    		mLastY=mEaseY;
    	}
////		//全体のイージングを計算
//		mGoalY=offsetWidth-flickYdist;
//		if(mGoalY<=0) mGoalY=0;
//		else if(mGoalY>=offsetWidth) mGoalY=offsetWidth;
//		mEaseY+=(mGoalY-mLastY)/allEaseSpFlick;
//		float val = Math.abs(mLastY-mEaseY);
//		if(val<5) tapAbble=true;
//		else tapAbble=false;
//		mLastY=mEaseY;
	}
    void calcFlickScrollTutrial(){
//    	DebugLog.instance.outputLog("touched", "calcFlickScrollTutrial!!!!!!!!!!!!!!!!!!!!");
		int reEaseTarget = easeTarget;
		float targetEaseX = offsetWidthX-flickXdist;
		float nowBreak = 0;
		float lessBreak = Math.abs(targetEaseX - easeXBreakTutrial[0]);
		easeTarget = 0;
		for (int i = 0; i < TUTRIAL_KAZU; i++) {
			nowBreak = Math.abs(targetEaseX - easeXBreakTutrial[i]);
			if(nowBreak<lessBreak){
				easeTarget=i;
				lessBreak = nowBreak;
			}
		}
		//1個しか進まないように
		if(reEaseTarget-easeTarget<-1){
			if(reEaseTarget==TUTRIAL_KAZU-1) easeTarget = 0;
			else easeTarget=reEaseTarget+1;
		}
		else if(reEaseTarget-easeTarget>1){
			if(reEaseTarget==0) easeTarget = TUTRIAL_KAZU-1;
			else easeTarget=reEaseTarget-1;
		}
		nowTouchState = TouchState.TOUCH_SCROLL_AUTO;
		flickXdist = offsetWidthX - easeXBreakTutrial[easeTarget];
		tutrialTouchUp = false;
    }
     void calcFlickScroll(){
		int reEaseTarget = easeTarget;
		float targetEaseX = offsetWidthX-flickXdist;
		float nowBreak = 0;
		float lessBreak = Math.abs(targetEaseX - easeXBreak[0]);
		easeTarget = 0;
		for (int i = 0; i < thumbsTexKazuDetail; i++) {
			nowBreak = Math.abs(targetEaseX - easeXBreak[i]);
			if(nowBreak<lessBreak){
				easeTarget=i;
				lessBreak = nowBreak;
			}
		}
		//1個しか進まないように
		if(nowIndexDetail[reEaseTarget]-nowIndexDetail[easeTarget]<-1){
			if(reEaseTarget==thumbsTexKazuDetail-1) easeTarget = 0;
			else easeTarget=reEaseTarget+1;
		}
		else if(nowIndexDetail[reEaseTarget]-nowIndexDetail[easeTarget]>1){
			if(reEaseTarget==0) easeTarget = thumbsTexKazuDetail-1;
			else easeTarget=reEaseTarget-1;
		}
		nowTouchState = TouchState.TOUCH_SCROLL_AUTO;
		flickXdist = offsetWidthX - easeXBreak[easeTarget];
		detailTouchUp = false;
    }
    void calcDetailXEnd(){
    	DebugLog.instance.outputLog("touched", "Auto スクロール完了でチェック開始" + nowTouchState);
		//上超えた判定//トータルが24以下は無視
		int index1 = thumbsOrderDetail[0];
		int index2 = thumbsOrderDetail[Math.max((thumbsTexKazuDetail-1),0)];
		if(thumbsDetailX[index1]<=DETAIL_LEFT_LIMIT && nowIndexDetail[index1]+thumbsTexKazuDetail<=thumbsTotalDetail-1 && THUMBSTEXMAX_DETAIL<thumbsTotalDetail){//左スクロール
			DebugLog.instance.outputLog("value","左を超えた--超えたのは:::" + nowIndexDetail[index1]);
			thumbsLeftX+= DETAIL_ZONE_WIDTH;
			nowIndexDetail[index1]+=thumbsTexKazuDetail;
			easeXBreak[index1] =DETAIL_ZONE_WIDTH*(nowIndexDetail[index1]);
			thumbsDetailX[index1] =thumbsDetailX[thumbsOrderDetail[thumbsTexKazuDetail-1]] + DETAIL_ZONE_WIDTH;
//			thumbsTouch[index1].y= thumbsY[index1] + 40;
			DebugLog.instance.outputLog("value","左を超えた--右へ作るのは:::" + nowIndexDetail[index1]);
			//thumbsOrder計算
			calcThumbsOrderDetail(index1,true);
			//loading
			//ローディングに切り替え
			nowChangingThumbsDetail[index1] = true;
			thumbsReserveDetail[index1] = nowIndexDetail[index1];
			getDetailImage(nowIndexDetail[index1]);
		}
		//下超えた
		else if(thumbsDetailX[index2]>DETAIL_RIGHT_LIMIT && nowIndexDetail[index2]-thumbsTexKazuDetail>=0 && THUMBSTEXMAX_DETAIL<thumbsTotalDetail){//下スクロール
			DebugLog.instance.outputLog("value","右を超えた--超えたのは:::" + nowIndexDetail[index2]);
			nowIndexDetail[index2]-=thumbsTexKazuDetail;
			thumbsDetailX[index2] =thumbsDetailX[thumbsOrderDetail[0]] - DETAIL_ZONE_WIDTH;;
			thumbsLeftX-=DETAIL_ZONE_WIDTH;//順番の違い注意
			easeXBreak[index2] =DETAIL_ZONE_WIDTH*(nowIndexDetail[index2]);
			DebugLog.instance.outputLog("value","右を超えた--左へ作るのは:::" + nowIndexDetail[index2]);
//			thumbsTouch[index2].y= thumbsY[index2] + 40;
			//thumbsOrder計算
			calcThumbsOrderDetail(index2,false);
			//loading
			thumbsReserveDetail[index2] = nowIndexDetail[index2];
			//ローディングに切り替え
			nowChangingThumbsDetail[index2] = true;
			getDetailImage(nowIndexDetail[index2]);
		}

		for (int i = 0; i < thumbsTexKazuDetail; i++) {
			//誤差を考えても確実な値
			if(DETAIL_LEFT-10<thumbsDetailX[i] && thumbsDetailX[i] < DETAIL_LEFT+10){
				nowCenterDetailNum = nowIndexDetail[i];
				nowCenterDetailType = thumbInfoArray.get(nowCenterDetailNum).getFileType();
				//見きれた時に自動で読み込むための予約
				if(detailArtFailed.get(nowCenterDetailNum) && !detailArtReloadReserved.get(nowCenterDetailNum)) detailArtReloadReserved.set(nowCenterDetailNum,true);
				nowCenterDetailSPNum = i;
				DebugLog.instance.outputLog("info","nowCenterDetailSPNum:::" + nowCenterDetailSPNum);
				//ここでリセット
				if(reCenterDetailNum!=nowCenterDetailNum) resetStateBtn();
				reCenterDetailNum = nowCenterDetailNum;

			}
		}
		DebugLog.instance.outputLog("value","真ん中のいるのは:::" + nowCenterDetailNum);
		if((nowCenterDetailNum+1)%3==0 && nowCenterDetailNum+1<thumbsTotalDetail){//3個ずつのを呼ぶ
			//すでに端末内に画像を保持してあれば3個ずつのを呼ぶ必要はない
			int num = (nowCenterDetailNum+1)/3;
			DebugLog.instance.outputLog("api",  "セットチェック=" + num);
			if(!alreadyCalledDetailSet.get(num)){
				alreadyCalledDetailSet.set(num, true);
				//必要分作る--3個セットの中でも呼ばなくていいのもあるかも
				int total = Math.min(3, thumbsTotalDetail-(nowCenterDetailNum+1));
				DebugLog.instance.outputLog("apicheck",  "セットのトータル数=" + total);
		    	Array<ThumbInfo> artCall =  new Array<ThumbInfo>(Math.min(3, total));
		    	for (int i = nowCenterDetailNum+1; i <= nowCenterDetailNum+total;i++){
		    			detailArtNowCallAPI.set(i, true);//APIのコール開始
		    			DebugLog.instance.outputLog("apicheck","APIのコール開始(セット):::" + i);
		    			artCall.add(thumbInfoArray.get(i));
		    	}
		    	CallGetDetailImageAPI(artCall);
			}
		}
    }
    //詳細画像を個別に呼ぶ--仮？
    void getDetailImage(int num){
		int check = num/3;
		if(alreadyCalledDetailSet.get(check) && !detailArtNowCallAPI.get(num)){
	    	//ここでリセットしとく
			detailArtFailed.set(num,false);
			detailArtReloadReserved.set(num,false);
			Array<ThumbInfo> artCall =  new Array<ThumbInfo>();
			artCall.add(thumbInfoArray.get(num));
			detailArtNowCallAPI.set(num, true);//APIのコール開始
			DebugLog.instance.outputLog("apicheck","APIのコール開始(単独):::" + num);
			CallGetDetailImageAPI(artCall);
		}
    }
    public void calcScrollFlickEaseTutrial(){
    	if(tutrialTouchUp){
    		long passTimeFromTouchUp = System.currentTimeMillis()- lastTouchUpTime;
    		if(1<=passTimeFromTouchUp){//タッチアップから1秒以上経過しているのにフリング発生しないのはおかしいから強制的に操作
    			DebugLog.instance.outputLog("touched", "タッチアップから1秒以上経過しているのにフリング発生しないのはおかしいから強制的に操作");
    			calcFlickScrollTutrial();
    		}
    	}
//    	DebugLog.instance.outputLog("touched", "mEaseX：：" + mEaseX);
//		//全体のイージングを計算
		mGoalX=offsetWidthX-flickXdist;
		if(mGoalX<=0) mGoalX=0;
		else if(mGoalX>=offsetWidthX) mGoalX=offsetWidthX;
		if(nowTouchState == TouchState.TOUCH_SCROLL_AUTO){
			mEaseX+=(mGoalX-mLastX)/5f;
			float val = Math.abs(mLastX-mEaseX);
			if(val<2){
//				tapAbble=true;
//				calcTutrialXEnd();
				nowTouchState = TouchState.TOUCH_SCROLL;
				DebugLog.instance.outputLog("touched", "Auto スクロール完了");
			}
//			else tapAbble=false;
		}
		else{
			mEaseX+=(mGoalX-mLastX)/5f;
		}
		mLastX=mEaseX;
	}
    public void calcScrollFlickEaseDetail(){
    	if(detailTouchUp && !nowSettingThemes){//テーマセット時は除外
    		long passTimeFromTouchUp = System.currentTimeMillis()- lastTouchUpTime;
    		if(1<=passTimeFromTouchUp  && nowTouchState != TouchState.TOUCH_ONLY_DIALOG){//タッチアップから1秒以上経過しているのにフリング発生しないのはおかしいから強制的に操作
    			DebugLog.instance.outputLog("touched", "タッチアップから1秒以上経過しているのにフリング発生しないのはおかしいから強制的に操作");
    			calcFlickScroll();
    		}
    	}
//		//全体のイージングを計算
		mGoalX=offsetWidthX-flickXdist;
		if(mGoalX<=0) mGoalX=0;
		else if(mGoalX>=offsetWidthX) mGoalX=offsetWidthX;
		if(nowTouchState == TouchState.TOUCH_SCROLL_AUTO){
			mEaseX+=(mGoalX-mLastX)/5f;
			float val = Math.abs(mLastX-mEaseX);
			if(val<2){
//				tapAbble=true;
				calcDetailXEnd();
				nowTouchState = TouchState.TOUCH_SCROLL;
				DebugLog.instance.outputLog("touched", "Auto スクロール完了");
			}
//			else tapAbble=false;
		}
		else{
			mEaseX+=(mGoalX-mLastX)/5f;
//			float val = Math.abs(mLastX-mEaseX);
//			if(val<2) tapAbble=true;
//			else tapAbble=false;
		}
		mLastX=mEaseX;
	}
    //recomend用Xもあり
    public void calcScrollFlickEaseRec(){
//		//全体のイージングを計算
    	if(nowTouchState == TouchState.TOUCH_SCROLL_BOUNCE){
    		mGoalY=offsetWidth-bounceGoal;
    		mEaseY+=(mGoalY-mLastY)/4f;
    		mLastY=mEaseY;
    		if(bounceGoal==0){//下の時
    			if(mEaseY-1<offsetWidth){
    				flickYdist=0;
    				nowTouchState = TouchState.TOUCH_SCROLL;
    				DebugLog.instance.outputLog("info", "AAnowTouchState = nowTouchState = TouchState.TOUCH_SCROLL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    				if(0<=reserveShuffleAnim){//バウンス後、再シャッフル
//    					emTapRecomemd.ResetTime();//回転アニメ
    					initializeShuffleAnim(reserveShuffleAnim);
    					reserveShuffleAnim = -1;
    				}
    			}
    		}
    		else{
    			if(0<mEaseY+1){
    				flickYdist=offsetWidth;
    				nowTouchState = TouchState.TOUCH_SCROLL;
    				DebugLog.instance.outputLog("info", "BBnowTouchState = nowTouchState = TouchState.TOUCH_SCROLL!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    			}
    		}

    	}
    	else{
    		mGoalY=offsetWidth-flickYdist;
    		if(mGoalY<=-bounceDist) mGoalY=-bounceDist;//permitBounceDOWN
    		else if(mGoalY>=offsetWidth+bounceDistRec) mGoalY=offsetWidth+bounceDistRec;//permitBounceUP
    		mEaseY+=(mGoalY-mLastY)/5f;
    		mLastY=mEaseY;
    	}
	}

    //テーマ設定時呼ぶメソッド
    void SetTheme(ContentsDataDto setCto){
    	SetTheme(setCto,true);
    }
    void SetTheme(ContentsDataDto setCto ,boolean isSetTheme){
    	DebugLog.instance.outputLog("api", "SetTheme!!!!!!!");
	    if(!SPPUtility.checkNetwork(myActivity.getApplicationContext())){
	    	//ここでダイアログ表示
	    	DebugLog.instance.outputLog("api", "ネットワークない(DetailTap時)!!!!!!!!!" + nowTouchState);
	    	saveTouchState= nowTouchState;
	    	nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
	    	visibleDialog = true;
	    	return;
	    }
    	nowSettingThemes = true;
    	saveTouchState = nowTouchState;
    	nowTouchState = TouchState.TOUCH_DIABLE;
    	//メソッド呼び出し
    	if(isSetTheme){
//        	ContentsDataDto kariCto = ContentsOperatorForCatalog.op.getContentsDataFromAssetId(10000004853L);
    		ContentsOperatorForCatalog.op.callDownloadSkinTask(setCto);
    	}
    	else{
//        	ContentsDataDto kariCto = ContentsOperatorForCatalog.op.getContentsDataFromAssetId(10000004853L);
//    		nowCenterDetailType = thumbInfoArray.get(nowCenterDetailNum).getFileType();
    		ContentsOperatorForCatalog.op.callDownloadSkinTask(thumbInfoArray.get(nowCenterDetailNum).getCto());
    	}
    }
    public void restartSetTheme(){
    	ContentsOperatorForCatalog.op.restartDownloadSkinTask();
    }
    //2016/2/1 iconショートカット
	public void onFailedDownloadIconThumbsSeparateCatalog(long assetId) {
		DebugLog.instance.outputLog("catalog", "アイコンサムネイル取得失敗");
    	resetStateBtn();
    	nowSettingThemes = false;
		if(!visibleDialog){
//			saveTouchState= nowTouchState;
			nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
			visibleDialog = true;
		}
	}
	public void onFailedDownloadIconThumbsSeparateNetworkCatalog(long assetId) {
		DebugLog.instance.outputLog("catalog", "アイコンサムネイル取得失敗_networkError");
    	resetStateBtn();
    	nowSettingThemes = false;
		if(!visibleDialog){
//			saveTouchState= nowTouchState;
			nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
			visibleDialog = true;
		}
	}
	public void onFinishedDownloadIconThumbsSeparateCatalog(long assetId) {
		DebugLog.instance.outputLog("catalog", "アイコンサムネイル取得成功");
    	resetStateBtn();
    	nowSettingThemes = false;
    	nowTouchState = saveTouchState;
    	//activity起動
    	ContentsOperatorForCatalog.op.callIconSelectListTypeActivity(thumbInfoArray.get(nowCenterDetailNum).getCto(), true);
	}

    public void onFailedDownloadSkin(int reason,long assetId) {
    	resetStateBtn();
    	nowSettingThemes = false;
    	isPremiunError = false;
//    	SUCCESS = 0, FAILED_REASON_NOT_PREMIUM = 1, FAILED_REASON_PREMIUM_AUTH_ERROR = 2, FAILED_REASON_NETOWORK_ERROR = 3;
    	switch(reason){
    	case DownloadSkinAsyncTask.FAILED_REASON_NOT_PREMIUM:
    		//トースト
    		nowTouchState = saveTouchState;
    		myActivity.CallToast("プレミアム会員ではありません");
    		break;
    	case DownloadSkinAsyncTask.FAILED_REASON_PREMIUM_AUTH_ERROR:
    		isPremiunError =true;
    		if(!visibleDialog){
//    			saveTouchState= nowTouchState;
    			nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
    			visibleDialog = true;
    		}
    		break;
    	case DownloadSkinAsyncTask.FAILED_REASON_NETOWORK_ERROR:
    		if(!visibleDialog){
//    			saveTouchState= nowTouchState;
    			nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
    			visibleDialog = true;
    		}
    		break;
    	}
	}
	public void onFinishedDownloadSkin(long settedAssetId) {
    	resetStateBtn();
    	nowSettingThemes = false;
    	nowTouchState = saveTouchState;
	}
//    void SetThemeFinish(){
////    	myActivity.onFinishedDownloadSkin(10000004853L, 0L, 0L);
//    	resetStateBtn();
//    	nowSettingThemes = false;
//    	nowTouchState = saveTouchState;
//    }
//    void SetThemeFailed(){
//		if(!visibleDialog){
//			nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
//			visibleDialog = true;
//		}
//    	waitTime = 0f;
//    }
    public void onFinishedAllDataDownload(){
    	//情報取得成功
    	DebugLog.instance.outputLog("api", "onFinishedAllDataDownload!!!!!!!");
    	LoadingAppData =true;
    }
    public void onFailedAllDataDownload(){
    	DebugLog.instance.outputLog("api", "onFailedAllDataDownload!!!!!!!");
    	if(nowScreenState != ScreenState.SCREEN_TUTRIAL) nowScreenState = ScreenState.SCREEN_OFFLINE;
//    	else reserveScreenState = ScreenState.SCREEN_OFFLINE;
    	reserveScreenState = ScreenState.SCREEN_OFFLINE;
    }
    //詳細ページで最初に呼ぶメソッド仮定
    void CallDetailFirstAPI(){
    	nowDetailState = DetailState.DETAIL_LOADING;
    	//CTO渡す
    	ContentsOperatorForCatalog.op.callDetailInfoGetTask(detailCto);
    }
  //詳細ページで画像取得時に呼ぶメソッド仮定
    void CallGetDetailImageAPI(Array<ThumbInfo> callNum){
		ContentsOperatorForCatalog.op.callDownloadDetailThumbsTask(callNum);
    }

    private Array<ThumbInfo> thumbInfoArray = null;

    //詳細情報取得失敗時にcallされる
    public void failedDetailInfo(){
		DebugLog.instance.outputLog("value", "詳細情報取得失敗:CatalogScreen");
		DebugLog.instance.outputLog("api", "詳細情報取得失敗:CatalogScreen");
		nowDetailState = DetailState.DETAIL_LOADING_ERROR;
    }
    //詳細の情報取得完了なメソッド仮定--ここで色々初期化する(スプラッシュも終了)
    public void DoneDetailInfo(Array<ThumbInfo> infoArray){
    	DebugLog.instance.outputLog("value", "DoneDetailInfo");
    	if(thumbInfoArray!=null) thumbInfoArray = null;
    	this.thumbInfoArray = infoArray;
		thumbsTotalDetail = infoArray.size;
		thumbsLeftX = 0;
		offsetWidthX = DETAIL_ZONE_WIDTH * ( thumbsTotalDetail-1);// スクロール幅
		flickXdist = offsetWidthX;
		mEaseX = 0;
		mLastX = mEaseX;
		//reset
		detailTouchUp = false;
		easeTarget = 0;
		flingStateTime = 0;
	    addFlingX = 0;
	    loadingSP.setRotation(0);
	    pageBtnLeftX =  540- (thumbsTotalDetail*38-24)/2f;
	    detailTapOK = false;
	    nowCenterDetailNum = 0;
	    reCenterDetailNum = 0;
	    nowCenterDetailSPNum = 0;
	    resetStateBtn();//ステートボタンリセット
    	nowCenterDetailType = thumbInfoArray.get(nowCenterDetailNum).getFileType();
		DebugLog.instance.outputLog("api", "thumbsTotalDetail=" +thumbsTotalDetail);
		thumbsTexKazuDetail = Math.min(THUMBSTEXMAX_DETAIL,thumbsTotalDetail);
//		thumbsDetailY = centerY-552;
		for (int i = 0; i < thumbsTexKazuDetail; i++) {
			thumbsReserveDetail[i] = i;//予約
			//初期化--ここでかは後で検討
			thumbsDetailX[i] = DETAIL_LEFT+(i*DETAIL_ZONE_WIDTH);
			easeXBreak[i] = i*DETAIL_ZONE_WIDTH;
			thumbsOrderDetail[i] = i;
			nowIndexDetail[i] = i;
//			loadingRotaDetail[i] = 0;
			loadingRota[i] = 0;
			//loading
			nowChangingThumbsDetail[i] = true;
			nowLoadingThumbsDetail[i] = false;
		}
    	//詳細画面の数分の配列確保
    	detailArtNowCallAPI.clear();
    	detailArtNowCallAPI =  new Array<Boolean>(thumbsTotalDetail);
//    	alreadySavedDetailImage.clear();
//    	alreadySavedDetailImage =  new Array<Boolean>(thumbsTotalDetail);
    	int detailSet = thumbsTotalDetail/3+1;
    	alreadyCalledDetailSet.clear();
    	alreadyCalledDetailSet =  new Array<Boolean>(detailSet);//一個無駄になる可能性もあるけど…
    	alreadyCalledDetailSet.add(true);//1-3枚目のセットは便宜上trueにする
    	detailArtFailed.clear();
    	detailArtFailed =  new Array<Boolean>(thumbsTotalDetail);
    	detailArtReloadReserved.clear();
    	detailArtReloadReserved =  new Array<Boolean>(thumbsTotalDetail);
     	for (int i = 0; i <thumbsTotalDetail;i++){
    		detailArtNowCallAPI.add(false);
    		detailArtFailed.add(false);
    		detailArtReloadReserved.add(false);
    	}
    	for (int i = 3; i <thumbsTotalDetail;i++) {
    		detailArtNowCallAPI.add(false);
    		if(i%3==0){//すでに端末内に画像を保持してあれば3個ずつのを呼ぶ必要はないという認識
    			int check = 0;
    			int max = Math.min(i+3, thumbsTotalDetail);
    			for (int c = i; c<max;c++){
    				if(infoArray.get(c).isExistThumbs(myActivity.getApplicationContext())) check++;
    			}
    			if(max - i == check){
    				alreadyCalledDetailSet.add(true);//保存済みなら必要ないので…
    				DebugLog.instance.outputLog("apicheck", "セットでダウンロードメソッド呼ぶ必要なし" + i + "番～の画像のセット");
    			}
    			else{
    				alreadyCalledDetailSet.add(false);
    				DebugLog.instance.outputLog("apicheck", "セットでダウンロードメソッド呼ぶ必要あり" + i + "番～の画像のセット");
    			}
    		}
    	}
    	Array<ThumbInfo> artCall;
    	//3個呼ぶけど次のセット保存済みなら6個分一気に作る
    	int num = Math.min(3, thumbsTotalDetail);
		int check = 0;
		int max = Math.min(6, thumbsTotalDetail);
		for (int c = 3; c<max;c++){
			if(infoArray.get(c).isExistThumbs(myActivity.getApplicationContext())) check++;
		}
		if(max - 3 == check){
			num = max;
		}
		DebugLog.instance.outputLog("api", "初回でロードする数(3or6)=" + num);
    	artCall =  new Array<ThumbInfo>();
    	for (int i = 0; i <num;i++) {
    		detailArtNowCallAPI.set(i,true);
    		DebugLog.instance.outputLog("value", "infoArray:" + i);
    		artCall.add(infoArray.get(i));
    	}
    	CallGetDetailImageAPI(artCall);
    }

    //詳細サムネイル画像ファイルの保存失敗時にcallされる
    //この瞬間に画面外だったらもう1回取りに行く？
    public void failedSaveDetailImage(int num){
		DebugLog.instance.outputLog("value", "詳細サムネ取得失敗:CatalogScreen::" + num + "番のアート");
		if(num==0) failedDetailInfo();//一枚目のアートならエラー扱い
		else if(num==nowCenterDetailNum) detailArtReloadReserved.set(nowCenterDetailNum,true);//失敗時に中央アートなら予約
		detailArtNowCallAPI.set(num, false);//要求APIのコールバックが返却された
		detailArtFailed.set(num, true);//取得に失敗
    }

  //画像の保存完了したメソッド仮定--仮で左からの番号返す--保存してればすぐに返ってくる
    public void DoneSaveDetailImage(int num){
		DebugLog.instance.outputLog("api", "DoneSaveDetailImage" + num + "番の画像");
    	detailArtNowCallAPI.set(num, false);//要求APIのコールバックが返却された
//    	if(!alreadySavedDetailImage.get(num)) alreadySavedDetailImage.set(num, true);//端末保存完了
    	//読み込みの必要があるか確認--読み込み開始から時間が経ちすぎて必要なくなっている可能性もある
		for (int i = 0; i < thumbsTexKazuDetail; i++) {
    		if(num == thumbsReserveDetail[i]){
    			DebugLog.instance.outputLog("api", "予約あり:" + num + "番の画像");
    			DebugLog.instance.outputLog("api", "予約あり:" + i + "番のスプライト");
    			//make
    			loadingThumbsDetail(i, num);
    			break;
    		}
		}
		//一枚目の取得が終わればローディング終わり
		if(num==0 && nowDetailState == DetailState.DETAIL_LOADING){
			DebugLog.instance.outputLog("api", "一枚目の取得終了!!!!!!!!!" + nowAnimationState);
			nowTouchState = TouchState.TOUCH_SCROLL;
//			if (nowAnimationState == AnimationState.ANIM_NONE) {
//				DebugLog.instance.outputLog("api", "アニメーション終わってる!!!!!!!!!");
//				nowTouchState = TouchState.TOUCH_SCROLL;
//			}
//			else {
//				DebugLog.instance.outputLog("api", "まだアニメーション中!!!!!!!!!");
//			}
			nowDetailState = DetailState.DETAIL_NOMAL;
		}
    }

    public class MyInputListener implements InputProcessor{
		@Override
		public boolean keyDown(int arg0) {
			return false;
		}
		@Override
		public boolean keyTyped(char arg0) {
			return false;
		}
		@Override
		public boolean keyUp(int arg0) {
			return false;
		}
		@Override
		public boolean mouseMoved(int arg0, int arg1) {
			return false;
		}
		@Override
		public boolean scrolled(int arg0) {
			return false;
		}
		@Override
		public boolean touchDown (int x, int y, int pointer, int newParam) {
//			DebugLog.instance.outputLog("touched", "InputProcessor touchDown pointer::" + pointer);
			//1(順番)
//			DebugLog.instance.outputLog("touched", "InputProcessor touchDown");
			if(pointer!=0 || nowScreenState == ScreenState.SCREEN_SPLASH) return false;

			//メニューゾーンのタッチか判定
			if(nowScreenState != ScreenState.SCREEN_DETAIL && nowScreenState != ScreenState.SCREEN_TUTRIAL){
				camera.unproject(touchPos.set(x,y, 0));
				if(	!rect_new.contains(touchPos.x, touchPos.y) &&
					!rect_recomend.contains(touchPos.x, touchPos.y) &&
					!rect_ranking.contains(touchPos.x, touchPos.y) &&
					!rect_mybox.contains(touchPos.x, touchPos.y) &&
					!rect_sort.contains(touchPos.x, touchPos.y))
				{
					isMenuTouchDown =false;
				}
				else{
					isMenuTouchDown =true;
				}
				if(nowScreenState == ScreenState.SCREEN_INFO){
					for(int i=0;i<infoKazu;i++){
						if(rect_info[i].contains(touchPos.x, touchPos.y)){
							tapInfo = i;
						}
					}
				}
				DebugLog.instance.outputLog("touched", "メニューゾーンのタッチか判定==" + isMenuTouchDown);
			}
			else isMenuTouchDown =false;
						//条件は仮


			if(nowTouchState != TouchState.TOUCH_DIABLE && !isMenuTouchDown){
				dragReY = y*uiPer;
				dragReX = x*uiPer;
//				if(nowScreenState == ScreenState.SCREEN_DETAIL) dragReX = x*uiPer;
				permitBounceUP= false;
				permitBounceDOWN= false;
			}
			if(nowTouchState == TouchState.TOUCH_SCROLL && isBounce){
//				DebugLog.instance.outputLog("touched", "permitBounce判定!!!!!!!!!");
				if(flickYdist==0){
					permitBounceUP = true;
					DebugLog.instance.outputLog("touched", "permitBounceUP!!!!!!!!!");
				}
				else if(flickYdist==offsetWidth){
					permitBounceDOWN = true;
					DebugLog.instance.outputLog("touched", "permitBounceDOWN!!!!!!!!!");
				}
			}
	    	if(nowScreenState == ScreenState.SCREEN_RECOMEND &&
	    			nowRecomendState == RecomendState.PEVIEW_MODE && !nowSettingThemes) {
	    		camera.unproject(touchPos.set(x,y, 0));
	    		if(thumbsTouch[previewNum].contains(touchPos.x,touchPos.y)){
	    			q_slideOK=true;
	    			DebugLog.instance.outputLog("touched", "q_slideOK !!!!!!!!!!!!!" );
	    		}
	    		else{q_slideOK=false;
	    		DebugLog.instance.outputLog("touched", "q_slideNOOOO !!!!!!!!!!!!!" );
	    		}
	    	}
	    	else if((nowScreenState == ScreenState.SCREEN_NEWART || nowScreenState == ScreenState.SCREEN_RUNKING) && !nowSettingThemes) {
	    		camera.unproject(touchPos.set(x,y, 0));
	    		isNowQuickTouch  = false;
    			for(int i=0;i<thumbsTexKazu;i++){
	    			if(thumbsTouch[i].contains(touchPos.x,touchPos.y) && thumbsIsQuick[i]){
	    				thumbsQ_slideOK[i] = true;
	    				isNowQuickTouch  = true;
	    				isNowQuickTouchNum = i;
	    				DebugLog.instance.outputLog("touched", "q_slideOK !!!!!!!!!!!!!" );
	    			}
	    			else thumbsQ_slideOK[i] = false;
    			}
	    	}
	    	slideStartX = x*uiPer;
	    	slideStartY = y*uiPer;
	    	DebugLog.instance.outputLog("flamework", "slideStartY::" + slideStartY);
			return false;
		}
		@Override
		public boolean touchUp (int x, int y, int pointer, int button) {
//			DebugLog.instance.outputLog("touched", "InputProcessor touchUp pointer::" + pointer);
			if(pointer!=0 || nowScreenState == ScreenState.SCREEN_SPLASH) return false;
			//5(順番)
//			DebugLog.instance.outputLog("info", "mEaseX=" + mEaseX);
//			DebugLog.instance.outputLog("touched", "InputProcessor touchUp");
			if(nowTouchState == TouchState.TOUCH_SCROLL  && !isMenuTouchDown){
				if(nowScreenState == ScreenState.SCREEN_RECOMEND){
		    		if(mEaseY>=offsetWidth+bounceDistRec-15 && reserveShuffleAnim<0){
		    			reserveShuffleAnim = GetShuffleAnimPattern();
//		    			reserveShuffleAnim = 1;
						emTapRecomemd.ResetTime();//回転アニメ
						DebugLog.instance.outputLog("check", "アニメ予約");
		    		}
		    		else reserveShuffleAnim = -1;
				}
				lastTouchUpTime = System.currentTimeMillis();
				if(nowScreenState == ScreenState.SCREEN_DETAIL) detailTouchUp = true;
				else if(nowScreenState == ScreenState.SCREEN_TUTRIAL) tutrialTouchUp = true;
				else if(isBounce){
					 if(mEaseY>offsetWidth && permitBounceUP){
			    			DebugLog.instance.outputLog("touched", "TOUCH_SCROLL_BOUNCE_UP!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
							bounceGoal=0;
							nowTouchState = TouchState.TOUCH_SCROLL_BOUNCE;
					}
					 else if(mEaseY<0 && permitBounceDOWN){
						DebugLog.instance.outputLog("touched", "TOUCH_SCROLL_BOUNCE_BOTTOM!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		    			bounceGoal=offsetWidth;
		    			nowTouchState = TouchState.TOUCH_SCROLL_BOUNCE;
		    		}
				}
				isNowQuickTouch  = false;
			}
			return false;
		}
		@Override
		public boolean touchDragged (int x, int y, int pointer) {
//			DebugLog.instance.outputLog("touched", "InputProcessor touchDragged pointer::" + pointer);
			if(pointer!=0 || nowScreenState == ScreenState.SCREEN_SPLASH) return false;
			//3(順番)
//			DebugLog.instance.outputLog("touched", "InputProcessor touchDragged");
			if(nowTouchState == TouchState.TOUCH_SCROLL  && !isMenuTouchDown){
				long stopTime = System.currentTimeMillis();
				long time = stopTime - lastTouchUpTime;
				if(time>100){
					if(nowScreenState == ScreenState.SCREEN_DETAIL || nowScreenState == ScreenState.SCREEN_TUTRIAL){
//						DebugLog.instance.outputLog("touched", "InputProcessor touchDragged");
						float touchX = x*uiPer;
						if(Math.abs(dragReX-touchX)>1){
							addDraggX =(dragReX-touchX)*DRAGGPER_DETAIL;
					    	//制限
					    	if(addDraggX<=-DRAGGMAX_DETAIL) addDraggX=-DRAGGMAX_DETAIL;
					    	if(addDraggX>=DRAGGMAX_DETAIL) addDraggX=DRAGGMAX_DETAIL;
							flickXdist -=addDraggX;
						}
						if(flickXdist<=0) flickXdist=0;
						else if(flickXdist>=offsetWidthX) flickXdist=offsetWidthX;
						dragReX = touchX;
					}
					else{
						float touchY = y*uiPer;
						//8/4 here!!
						if(isNowQuickTouch){
							camera.unproject(touchPos.set(x,y, 0));
							float touchX = x*uiPer;
							if(thumbsTouch[isNowQuickTouchNum].contains(touchPos.x,touchPos.y)){
								if(Math.abs(dragReX-touchX)<2) isNowQuickTouch = false;
							}
							else isNowQuickTouch = false;
							if(!isNowQuickTouch) DebugLog.instance.outputLog("scroll", "isNowQuickTouch false::" +Math.abs(dragReX-touchX));
						}
						if(!isNowQuickTouch){
							if(Math.abs(dragReY-touchY)>1){
						    	addDragg =(dragReY-touchY)*DRAGGPER;
						    	//制限
						    	if(addDragg<=-DRAGGMAX) addDragg=-DRAGGMAX;
						    	if(addDragg>=DRAGGMAX) addDragg=DRAGGMAX;
								flickYdist -=addDragg;
							}
							if(permitBounceUP){
								if(nowScreenState == ScreenState.SCREEN_RECOMEND){
									if(flickYdist<=-bounceDistRec) flickYdist=-bounceDistRec;//このパターン
								}
								else{
									if(flickYdist<=-bounceDist) flickYdist=-bounceDist;//このパターン
								}
							}
							else if(permitBounceDOWN){
								if(flickYdist>=offsetWidth+bounceDist) flickYdist=offsetWidth+bounceDist;//このパターン
							}
							else{
								if(flickYdist<=0) flickYdist=0;
								else if(flickYdist>=offsetWidth) flickYdist=offsetWidth;
							}
//							dragReY = touchY;
						}
						dragReY = touchY;
						DebugLog.instance.outputLog("flamework", "InputProcessor touchDragged" + flickYdist);
					}
				}
			}
			if(nowScreenState == ScreenState.SCREEN_INFO){
				if(0<=tapInfo){
					camera.unproject(touchPos.set(x,y, 0));
					if(!rect_info[tapInfo].contains(touchPos.x, touchPos.y)){
						tapInfo = -1;
					}
				}
			}
			return false;
		}
	}
	public class MyGestureListener implements GestureListener{
	    @Override
	    public boolean touchDown(float x, float y, int pointer, int button) {
	    	//2(順番)
//	    	DebugLog.instance.outputLog("touched", "GestureListener touchDown");
	        return false;
	    }
	    @Override
	    public boolean tap(float x, float y, int count, int button) {
//	    	DebugLog.instance.outputLog("touched", "GestureListener tap count::" + count);
	    	if(count!=1 || nowScreenState == ScreenState.SCREEN_SPLASH) return false;
	    	//InputProcessor touchUpの後
//	    	DebugLog.instance.outputLog("touched", "GestureListener tap");
	    	if(nowAnimationState == AnimationState.ANIM_NONE && nowTouchState != TouchState.TOUCH_DIABLE){
		    	camera.unproject(touchPos.set(x,y, 0));
		    	if(menuBtnTapOK){
//		    		DebugLog.instance.outputLog("touched", "GestureListener tap menuBtnTapOK" + );
		    		if(nowScreenState != ScreenState.SCREEN_DETAIL && nowScreenState != ScreenState.SCREEN_INFO && nowScreenState != ScreenState.SCREEN_TUTRIAL){
						//条件は仮
						if (rect_new.contains(touchPos.x, touchPos.y)) {
							if(nowScreenState == ScreenState.SCREEN_NEWART){
								DebugLog.instance.outputLog("touched", "SCREEN_NEWART リロード");
								emTapMenu.ResetTime();
							}
							reserveAnimationState = AnimationState.ANIM_NEW_TAP;
							nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
						}
						else if (rect_recomend.contains(touchPos.x, touchPos.y) && nowTouchState != TouchState.TOUCH_SCROLL_BOUNCE) {
							//シャッフルアニメ
							//プレビューモードなら閉じてから開始
							if(nowScreenState == ScreenState.SCREEN_RECOMEND){
								if(nowRecomendState == RecomendState.PEVIEW_MODE){
									reserveShuffleAnim = GetShuffleAnimPattern();
//									reserveShuffleAnim = 1;
				    				DebugLog.instance.outputLog("touched", "シャッフル予約!!!!!!!!!!");
				    				//各サムネイルの移動目標値算出
				    				calcTargetREC(previewNum,false);
				    				nowRecomendState = RecomendState.PEVIEW_ZOOMOUT;
								}
								else{
									reserveShuffleAnim = GetShuffleAnimPattern();
//									reserveShuffleAnim = 1;
									initializeShuffleAnim(reserveShuffleAnim);
								}
//								recomendFooterScale = 0.85f;
								emTapRecomemd.ResetTime();
							}
							else{
								reserveAnimationState = AnimationState.ANIM_RECOMEND_TAP;
								nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
								}
						}
						else if (rect_ranking.contains(touchPos.x, touchPos.y) && nowScreenState != ScreenState.SCREEN_RUNKING) {
							reserveAnimationState = AnimationState.ANIM_RUNKING_TAP;
							nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
						}
						else if (rect_mybox.contains(touchPos.x, touchPos.y) && nowScreenState != ScreenState.SCREEN_MYBOX) {
							reserveAnimationState = AnimationState.ANIM_MYBOX_TAP;
							nowAnimationState = AnimationState.ANIM_CASH_SCREEN;
						}
						//バウンス中対策
						else if (rect_sort.contains(touchPos.x, touchPos.y)&& nowTouchState != TouchState.TOUCH_SCROLL_BOUNCE) {
							if(nowScreenState == ScreenState.SCREEN_NEWART){
								if(!drawableSort){
									DebugLog.instance.outputLog("touched", "ソート Open");
									nowAnimationState = AnimationState.ANIM_SORT_OPEN;
				    				nowTouchState = TouchState.TOUCH_DIABLE;//タッチ一旦無効
									emSortBarZoom.ResetPosition(0, SORTBAR_MAX,SORTBAR_ZOOMTIME);
									for (int i = 0;i<7;i++) emSortButtonZoom[i].ResetPosition(0, ONE,SORTBUTTON_ZOOMTIME);
									drawableSort=true;
									sortHeaderScale = 0.85f;
									state_header_sort = 1;
								}
								else if(visibleSort){
				    				nowTouchState = TouchState.TOUCH_DIABLE;//タッチ一旦無効
				    				sortHeaderScale = 0.85f;
									initializeSortClose();
								}
							}
							else if(nowScreenState == ScreenState.SCREEN_RECOMEND){
								initializeInfoScreen();
							}
						}
		    		}
		    		else if(nowScreenState == ScreenState.SCREEN_INFO){
						for(int i=0;i<infoKazu;i++){
							if(rect_info[i].contains(touchPos.x, touchPos.y)){
								//遷移
								DebugLog.instance.outputLog("flamework", "infoタップ:::" + i);
								tapInfo = i;
								switch(tapInfo){
								case 0:
									DebugLog.instance.outputLog("flamework", "チュートリアルに遷移");
									initializeTutrial(false);
									break;
								case 1:
									DebugLog.instance.outputLog("flamework", "Helpに遷移");
									myActivity.CallHelpPage();
									break;
								case 2:
									DebugLog.instance.outputLog("flamework", "情報に遷移(Docomoのみ)");
//									myActivity.CallMemberInfoPage();
									break;
								}
							}
						}
		    			if (rect_popup_close.contains(touchPos.x, touchPos.y)) {
							initializeInfoClose();
							return false;
		    			}
		    		}
		    		else if(nowScreenState == ScreenState.SCREEN_DETAIL){
						if (rect_popup_close.contains(touchPos.x, touchPos.y)) {
							DebugLog.instance.outputLog("touched", "rect_popup_close tap::" + nowAnimationState);
//							reserveAnimationState = AnimationState.ANIM_DETAIL_CLOSE;
//							if(!offTransition) nowAnimationState = AnimationState.ANIM_CASH_DETAIL_SCREEN;
//							else{
//								initializeDetailCloseAnim();
								initializeDetailClose();
								return false;
//							}
						}
						else if(detailTapOK){
							if(rect_ditail_like.contains(touchPos.x, touchPos.y)){
								DebugLog.instance.outputLog("touched", "詳細　Favorete！！！！！！！！");
								SetAddLike(detailCto);
							}
							//読み込みOK
							if(!nowChangingThumbsDetail[nowCenterDetailSPNum]){
								if(rect_ditail_leftBtn.contains(touchPos.x, touchPos.y)){
									DebugLog.instance.outputLog("touched", "詳細　一括設定！！！！！！！！");
									state_ellipseBtnLeft = 1;
									SetTheme(detailCto);
									return false;
								}
								else if(rect_ditail_RightBtn.contains(touchPos.x, touchPos.y) && detailbtnAlphaRight==1f){
									DebugLog.instance.outputLog("touched", "詳細　個別設定！！！！！！！！ type = " + nowCenterDetailType);
									SetTheme(thumbInfoArray.get(nowCenterDetailNum).getCto(),false);
									state_ellipseBtnRight = 1;
									return false;
								}
							}
							else if(rect_ditail_tap.contains(touchPos.x, touchPos.y)) {
								DebugLog.instance.outputLog("touched", "rect_ditail_tap tap");
								//再読み込みの予約あり--
								if(detailArtReloadReserved.get(nowCenterDetailNum)){
									DebugLog.instance.outputLog("apicheck","自動で再読み込み(TAP)!!!!!!!!!!!!!!!!!!!!!!:::" + nowCenterDetailNum);
									getDetailImage(nowCenterDetailNum);
								}
							}
						}
		    		}
		    		else if(nowScreenState == ScreenState.SCREEN_TUTRIAL){
						if (rect_popup_close.contains(touchPos.x, touchPos.y)) {
							DebugLog.instance.outputLog("touched", "rect_popup_close tap::" + nowAnimationState);
							if((isFirstVisible3Tutrial && 3<=nowCenterTutrialNum) || !isFirstVisible3Tutrial){
								initializeTutrialClose();
								return false;
							}
						}
						else if(rect_tutrial_finish.contains(touchPos.x, touchPos.y) && TUTRIAL_KAZU-1==nowCenterTutrialNum){
							initializeTutrialClose();
							return false;
						}
		    		}
		    	}
//		    	boolean thumbsZone = (BOTTOM_MARGINE<touchPos.y
//		    			&& (nowScreenState == ScreenState.SCREEN_RECOMEND?((BOTTOM_MARGINE<touchPos.x && touchPos.y<topY-70) || touchPos.y<topY-TOP_MARGINE_TOUCH):touchPos.y<topY-TOP_MARGINE_TOUCH));
		    	boolean thumbsZone = false;
		    	if(nowScreenState == ScreenState.SCREEN_RECOMEND){
		    		if((BOTTOM_MARGINE<touchPos.y && touchPos.y<topY-TOP_MARGINE_TOUCH)
		    				|| ((BOTTOM_MARGINE<touchPos.y && touchPos.y<topY-TOP_MARGINE_TOUCH*0.5f)  && (BOTTOM_MARGINE<touchPos.x && touchPos.x<1080-BOTTOM_MARGINE))) thumbsZone =true;
		    		else thumbsZone =false;
		    	}
		    	else thumbsZone = (BOTTOM_MARGINE<touchPos.y && touchPos.y<topY-TOP_MARGINE_TOUCH);
		    	DebugLog.instance.outputLog("touched", "thumbsZone==" + thumbsZone);
		    	if(nowTouchState == TouchState.TOUCH_ONLY_DIALOG){
					if (rect_dialog_ok.contains(touchPos.x, touchPos.y)) {
						if(nowSettingThemes){
							visibleDialog = false;
					    	resetStateBtn();
					    	nowSettingThemes = false;
					    	nowTouchState = saveTouchState;
						}
						else{
							visibleDialog = false;
							nowTouchState = saveTouchState;
//							nowTouchState = TouchState.TOUCH_SCROLL;
						    resetStateBtn();//ステートボタンリセット
							//detailに到達していたら閉じる
							if(nowScreenState == ScreenState.SCREEN_DETAIL && nowDetailState == DetailState.DETAIL_LOADING_ERROR){
//								reserveAnimationState = AnimationState.ANIM_DETAIL_CLOSE;
//								if(!offTransition) nowAnimationState = AnimationState.ANIM_CASH_DETAIL_SCREEN;
//								else{
//									initializeDetailCloseAnim();
									initializeDetailClose();
									return false;
//								}
							}
							else if(nowScreenState == ScreenState.SCREEN_OFFLINE){
								DebugLog.instance.outputLog("touched", "アプリ終了させる！！！！！！！！！！！！！");
								myActivity.FinishApp();
							}
						}
						isPremiunError = false;//リセット
					}
		    	}
		    	else if(nowScreenState == ScreenState.SCREEN_NEWART && visibleSort){
		    		int tapCount = 0;
		    		for(int i=0;i<7;i++){
		    			if(rect_sortChara[i].contains(touchPos.x,touchPos.y)){
		    				tapCount++;
		    				if(state_sortChara[i]!=1){
			    				DebugLog.instance.outputLog("touched", "rrect_sortChara tap!!!!!!!!!!" + i);
			    				for (int s = 0; s < 7; s++) state_sortChara[s]= 0;
			    				state_sortChara[i]= 1;
			    				sortWhiteOut=true;//画面を白くする(サムネイルの描画をしない)
			    				nowTouchState = TouchState.TOUCH_DIABLE;//タッチ一旦無効
			    				initializeCharaSort(sortCharaValue[i]);
		    				}
		    			}
		    		}
		    		//tapCountで判断
			    	if(tapCount==0 && !isMenuTouchDown){
			    		//ソート閉じる
						DebugLog.instance.outputLog("touched", "Sort 解除 TAP !!!!!!!!!!!!!!");
	    				nowTouchState = TouchState.TOUCH_DIABLE;//タッチ一旦無効
//	    				sortHeaderScale = 0.9f;
						initializeSortClose();
			    	}
		    	}
		    	else if((nowScreenState == ScreenState.SCREEN_NEWART || nowScreenState == ScreenState.SCREEN_RUNKING || nowScreenState == ScreenState.SCREEN_MYBOX) && tapAbble && thumbsZone){
		    		for(int i=0;i<thumbsTexKazu;i++){
		    			if(!nowChangingThumbs[i]){//ロード済
			    			if(rect_detail[i].contains(touchPos.x,touchPos.y)){
			    				DebugLog.instance.outputLog("touched", "rect_q_detail tap!!!!!!!!!!");
			    				state_detailNum= i;
			    				TapDetail(i);
			    				return false;
			    			}
			    			else if(rect_like[i].contains(touchPos.x,touchPos.y)){
			    				DebugLog.instance.outputLog("touched", "rect_q_like tap!!!!!!!!!!");
			    				SetAddLike(nowIndex[i]);
			    			}
			    			else if(rect_set[i].contains(touchPos.x,touchPos.y)){
			    				DebugLog.instance.outputLog("touched", "rect_q_set tap!!!!!!!!!!");
			    				SetTheme(cto.get(nowIndex[i]));
			    				state_setNum= i;
			    				return false;
			    			}
			    			else if((nowScreenState == ScreenState.SCREEN_NEWART || nowScreenState == ScreenState.SCREEN_RUNKING) && thumbsTouch[i].contains(touchPos.x,touchPos.y)){
			    				DebugLog.instance.outputLog("touched", "サムネイル tap　クイックプレビューモード切り替え!!!!!!!!!!" + nowIndex[i]);
			    				 if(!thumbsIsQuick[i] && thumbsQ_scrollAlpha[i] == 1f) enableQuickState(i);
			    				else if(thumbsIsQuick[i]){
			    					thumbsQ_scrollAlpha[i] = 0f;
			    					thumbsIsQuick[i] = false;
//			    					disableQuickState(i);
			    				}
			    			}
		    			}
		    		}
		    	}
		    	//上下は無効にする(少し余裕もたせる　上180　下220)
		    	else if(nowScreenState == ScreenState.SCREEN_RECOMEND && thumbsZone){
//		    		DebugLog.instance.outputLog("touched", "SCREEN_RECOMEND::" + nowRecomendState);
//		    		DebugLog.instance.outputLog("touched", "tapAbleRec[0]::" + tapAbleRec[0]);
		    		//ノーマル時
		    		if(nowRecomendState == RecomendState.NOMAL_MODE){
			    		for(int i=0;i<thumbsTotal;i++){
			    			if(thumbsTouch[i].contains(touchPos.x,touchPos.y) && tapAbleRec[i]){
			    				DebugLog.instance.outputLog("touched", "tap=" + (i+1));
			    				previewNum = i;
			    				//各サムネイルの移動目標値算出
			    				calcTargetREC(previewNum,true);
//			    				emPreviewZoom.ResetPosition(ONE, ZOOMPER_REC, ZOOMTIME_REC);
			    				nowRecomendState = RecomendState.PEVIEW_ZOOMIN;
			    				nowTouchState = TouchState.TOUCH_DIABLE_QUICK;
			    			}
			    		}
		    		}
		    		else if(nowRecomendState == RecomendState.PEVIEW_MODE){
		    			if(rect_q_detail.contains(touchPos.x,touchPos.y)){
		    				DebugLog.instance.outputLog("touched", "rect_q_detail tap!!!!!!!!!!");
		    				state_detailNum= previewNum;
		    				TapDetail(previewNum);
		    				return false;
		    			}
		    			else if(rect_q_like.contains(touchPos.x,touchPos.y)){
		    				DebugLog.instance.outputLog("touched", "rect_q_like tap!!!!!!!!!!");
		    				SetAddLike(previewNum);
		    			}
		    			else if(rect_q_set.contains(touchPos.x,touchPos.y)){
		    				DebugLog.instance.outputLog("touched", "rect_q_set tap!!!!!!!!!!");
		    				SetTheme(cto.get(nowIndex[previewNum]));
		    				state_setNum= previewNum;
		    				return false;
		    			}
		    			else{
		    				int checkCount = 0;
				    		for(int i=0;i<thumbsTotal;i++){
				    			if(thumbsTouch[i].contains(touchPos.x,touchPos.y) && i !=previewNum){
				    				DebugLog.instance.outputLog("touched", "tap=" + (i+1));
				    				reservePrevieZoomNum = i;
				    				//各サムネイルの移動目標値算出
				    				DebugLog.instance.outputLog("touched", "PEVIEW_ZOOMOUT with Reserve!!!!!!!!!!");
				    				//各サムネイルの移動目標値算出
				    				calcTargetREC(previewNum,false);
				    				nowRecomendState = RecomendState.PEVIEW_ZOOMOUT;
				    			}
				    			else checkCount++;
				    		}
				    		if(checkCount == thumbsTotal){
			    				DebugLog.instance.outputLog("touched", "PEVIEW_ZOOMOUT!!!!!!!!!!");
			    				//各サムネイルの移動目標値算出
			    				calcTargetREC(previewNum,false);
			    				nowRecomendState = RecomendState.PEVIEW_ZOOMOUT;
				    		}
		    			}
		    		}
		    	}
	    	}
	        return false;
	    }
	    @Override
	    public boolean longPress(float x, float y) {
	        return false;
	    }
//	    flingが発生しない時対策--タッチアップからの時間で見てやる？
	    @Override
	    public boolean fling(float velocityX, float velocityY, int button) {
//	    	DebugLog.instance.outputLog("touched", "GestureListener fling button::" + button);
	    	//7(順番)
	    	if(nowScreenState == ScreenState.SCREEN_SPLASH) return false;
//	    	DebugLog.instance.outputLog("touched", "GestureListener fling::" + velocityY* uiPer);
			if (nowTouchState == TouchState.TOUCH_SCROLL  && !isMenuTouchDown) {
				//detail
				if(nowScreenState == ScreenState.SCREEN_DETAIL || nowScreenState == ScreenState.SCREEN_TUTRIAL){
					long stopTime = System.currentTimeMillis();
					long time = stopTime - lastFlingTime;
					flingStateTime += time;
					addFlingX = velocityX * uiPer * FLINGPER;
					//フリックを50％に制限
					addFlingX = velocityX * uiPer * FLINGPER*0.5f;
					if (time > 350 || flingStateTime > 600 || (time > 250 && Math.abs(addFlingX) < FLINGMAX_DETAIL)) {
						//制限
						if (addFlingX <= -FLINGMAX_DETAIL) addFlingX = -FLINGMAX_DETAIL;
						if (addFlingX >= FLINGMAX_DETAIL) addFlingX = FLINGMAX_DETAIL;
						flickXdist += addFlingX;
						if (flickXdist <= 0) flickXdist = 0;
						else if (flickXdist >= offsetWidthX) flickXdist = offsetWidthX;
						dragReX += addFlingX;
						flingStateTime = 0f;
						if(nowScreenState == ScreenState.SCREEN_DETAIL) calcFlickScroll();
						else calcFlickScrollTutrial();
					}
					lastFlingTime = System.currentTimeMillis();
//					calcFlickScroll();
				}
				else if(nowScreenState != ScreenState.SCREEN_DETAIL && !isNowQuickSlide){
//					DebugLog.instance.outputLog("scroll", "GestureListener fling::" + velocityY* uiPer);
					long stopTime = System.currentTimeMillis();
					long time = stopTime - lastFlingTime;
					flingStateTime += time;
					//フリックを50％に制限
					if(nowScreenState == ScreenState.SCREEN_RECOMEND) addFling = velocityY * uiPer * FLINGPER*0.5f;
					else addFling = velocityY * uiPer * FLINGPER;
					if (time > 350 || flingStateTime > 600 || (time > 250 && Math.abs(addFling) < 1000f)) {
						//制限
						if (addFling <= -FLINGMAX) addFling = -FLINGMAX;
						if (addFling >= FLINGMAX) addFling = FLINGMAX;
						flickYdist += addFling;
						if (flickYdist <= 0) flickYdist = 0;
						else if (flickYdist >= offsetWidth) flickYdist = offsetWidth;
						dragReY += addFling;
						flingStateTime = 0f;
					}
					lastFlingTime = System.currentTimeMillis();
					DebugLog.instance.outputLog("flamework", "fling!!!!!!!!!!!!!!!!!!!!!!!!!!" + flickYdist);
				}
				isNowQuickSlide = false;
			}
	        return false;
	    }
	    @Override
	    public boolean pan(float x, float y, float deltaX, float deltaY) {
	    	//4(順番)
//	    	DebugLog.instance.outputLog("touched", "GestureListener pan");
	    	if(nowScreenState == ScreenState.SCREEN_SPLASH || nowTouchState == TouchState.TOUCH_ONLY_DIALOG) return false;
	    	if (nowScreenState == ScreenState.SCREEN_RECOMEND &&
	    			nowRecomendState == RecomendState.PEVIEW_MODE && !nowSettingThemes) {
	    		//Xのスライドを優先させる
//	    		camera.unproject(touchPos.set(x,y, 0));
	    		float sx = x*uiPer;
	    		float sy = y*uiPer;
			    slideX= deltaX*uiPer;
			    slideDistX = sx-slideStartX;
//		    	DebugLog.instance.outputLog("touched", "slideX =" + slideX);
//		    	DebugLog.instance.outputLog("touched", "slideDistX =" + slideDistX);
		    	slideY= deltaY*uiPer;
//		    	slideDistY = touchPos.y-slideStartY;
		    	slideDistY = sy -slideStartY;
//		    	DebugLog.instance.outputLog("touched", "slideY =" + slideY);
//		    	DebugLog.instance.outputLog("touched", "slideDistY =" + slideDistY);
	    		if(slideX<-40 && slideDistX<-150 && q_slideOK){
//	    			DebugLog.instance.outputLog("touched", "START PEVIEW!!!!!!!!");
	    			if(q_stopScroll){
	    				emQuickMove.ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK_FLICK);
	    				q_waitScroll = 0f;
	    				q_stopScroll=false;
	    			}
	    		}
	    		else if(slideX>40 && slideDistX>150 && q_slideOK){
//	    			DebugLog.instance.outputLog("touched", "START PEVIEW(LEFT)!!!!!!!!");
	    			if(q_stopScroll){
	    				emQuickMove.ResetPosition(0, -THUMBS_U,ZOOMTIME_QUICK_FLICK);
	    				q_waitScroll = 0f;//ズーム後の最初の待ち時間短縮
	    				q_stopScroll=false;
	    			}
	    		}
	    		else if(Math.abs(slideY)>40 && Math.abs(slideDistY)>150){

					DebugLog.instance.outputLog("flamework", "PEVIEW 解除::::" + nowTouchState);
					if(slideDistY<0) reserveFlickYdist = Math.max(slideDistY*3f, -FLINGMAX);
					else reserveFlickYdist = Math.min(slideDistY*3f, FLINGMAX);
					DebugLog.instance.outputLog("flamework", "slideDistY in PEVIEW 解除::::" + slideDistY);
//					DebugLog.instance.outputLog("flamework", "touchPos.y in PEVIEW 解除::::" + touchPos.y);
					DebugLog.instance.outputLog("flamework", "slideY in PEVIEW 解除::::" + slideY);
					//各サムネイルの移動目標値算出
					calcTargetREC(previewNum,false);
					nowRecomendState = RecomendState.PEVIEW_ZOOMOUT;
					return false;
	    		}
	    	}
	    	if ((nowScreenState == ScreenState.SCREEN_NEWART || nowScreenState == ScreenState.SCREEN_RUNKING) && !nowSettingThemes){
//	    		camera.unproject(touchPos.set(x,y, 0));
	    		float sx = x*uiPer;
	    		float sy = y*uiPer;
	    		if(nowTouchState == TouchState.TOUCH_ONLY_MENU){
			    	slideY= deltaY*uiPer;
			    	slideDistY = sy-slideStartY;
//			    	DebugLog.instance.outputLog("touched", "slideY =" + slideY);
//			    	DebugLog.instance.outputLog("touched", "slideDistY =" + slideDistY);
		    		if(Math.abs(slideY)>40 && Math.abs(slideDistY)>150){
						DebugLog.instance.outputLog("touched", "Sort 解除" + nowTouchState);
	    				nowTouchState = TouchState.TOUCH_DIABLE;//タッチ一旦無効
//	    				sortHeaderScale = 0.9f;
						initializeSortClose();
						return false;
		    		}
	    		}
	    		else if(nowTouchState != TouchState.TOUCH_DIABLE){
//	    			isNowQuickSlide =false;
	    			for(int i=0;i<thumbsTexKazu;i++){
	    				slideX= deltaX*uiPer;
	    			    slideDistX = sx-slideStartX;
//	    		    	DebugLog.instance.outputLog("touched", "slideX =" + slideX);
//	    		    	DebugLog.instance.outputLog("touched", "slideDistX =" + slideDistX);
//	    		    	slideY= deltaY*uiPer;
//	    		    	slideDistY = sy-slideStartY;
//	    		    	DebugLog.instance.outputLog("touched", "slideY =" + slideY);
//	    		    	DebugLog.instance.outputLog("touched", "slideDistY =" + slideDistY);
	    	    		if(slideX<-30 && slideDistX<-125 && thumbsQ_slideOK[i]){
	    	    			DebugLog.instance.outputLog("touched", "START PEVIEW!!!!!!!!");
	    	    			if(thumbsQ_stopScroll[i]){
	    	    				emPreviewMove[i].ResetPosition(0, THUMBS_U,ZOOMTIME_QUICK_FLICK);
	    	    				thumbsQ_waitScroll[i] = 0f;
	    	    				thumbsQ_stopScroll[i]=false;
	    	    				isNowQuickSlide =true;
	    	    			}
	    	    		}
	    	    		else if(slideX>30 && slideDistX>125 && thumbsQ_slideOK[i]){
	    	    			DebugLog.instance.outputLog("touched", "START PEVIEW(LEFT)!!!!!!!!");
	    	    			if(thumbsQ_stopScroll[i]){
	    	    				emPreviewMove[i].ResetPosition(0, -THUMBS_U,ZOOMTIME_QUICK_FLICK);
	    	    				thumbsQ_waitScroll[i] = 0f;//ズーム後の最初の待ち時間短縮
	    	    				thumbsQ_stopScroll[i]=false;
	    	    				isNowQuickSlide =true;
	    	    			}
	    	    		}
	    			}
	    		}
	    	}
	        return false;
	    }
	    @Override
	    public boolean panStop(float x, float y, int pointer, int button) {
	    	//6(順番)
//	    	DebugLog.instance.outputLog("touched", "GestureListener panStop");
	    	slideStartX=0;
	    	slideStartY=0;
	        return false;
	    }
	    @Override
	    public boolean zoom (float originalDistance, float currentDistance){
	       return false;
	    }
	    @Override
	    public boolean pinch (Vector2 initialFirstPointer, Vector2 initialSecondPointer, Vector2 firstPointer, Vector2 secondPointer){
	       return false;
	    }
	}
	AssetObject assetObj;

	public class AssetObject {
		//footer
		public final Sprite[] footer_btn = new Sprite[2];
		public final Sprite[] footer_recomend = new Sprite[2];
		public final Sprite[] footer_new = new Sprite[2];
		public final Sprite[] footer_ranking = new Sprite[2];
		public final Sprite footer_recbtn;
		public final Sprite footer_recRota;
		//header
		public final Sprite header_info;
		public final Sprite[] header_btn = new Sprite[2];
		public final Sprite[] header_mybox = new Sprite[2];
		public final Sprite header_sort;
		public final Sprite header_setting;
		public final Sprite header_close;
		//quickpreview
		public final Sprite quick_btn;
		public final Sprite[] quick_detail = new Sprite[2];
		public final Sprite[] quick_like = new Sprite[2];
		public final Sprite[] quick_set = new Sprite[2];
		//detail
		public final Sprite downloading_detail;
//		public final Sprite detail_close;
		public final Sprite[] detail_page = new Sprite[2];
		public final Sprite loading_tutrial;
		//charactor
		public final Sprite[] sort_chara = new Sprite[7];//0-mickey 1-minie 2-donald 3- daisy 4-pooh 5-princess 6-others
		public final Sprite[] chara_btn = new Sprite[2];
		//common
		public final Sprite ranking_crown;
		public final Sprite[] ellipseBtn = new Sprite[2];//楕円ボタン
		public final Sprite dialog_bg;//
		public final Sprite[] thumbs_tag = new Sprite[3];//サムネイルのタグ　0-new 1-limited 2-premiun 3-none
		public final Sprite downloadDialog;
		public final Sprite popup_close;
		//info
		public final Sprite infoAllow;
		//word
		public final Sprite word_ok;
		public final Sprite word_set_drawer_icon;
		public final Sprite word_set_drawer_icon_sc;
		public final Sprite word_set_theme;
		public final Sprite word_set_wall;
		public final Sprite word_set_widget;
		//num
		public final Sprite[] num_update = new Sprite[11];
		public final Sprite[] num_ranking = new Sprite[11];
		//word black
		public final Sprite word_network_error;
		public final Sprite word_premium_error;
		public final Sprite word_no_addbox;
		public final Sprite[] word_d_wall = new Sprite[5];
		public final Sprite word_d_example;
		public final Sprite word_d_drawer_image;
		public final Sprite word_d_shortcut_icon;
		public final Sprite word_d_drawer_icon;
		public final Sprite word_d_bat_widget;
		public final Sprite[] word_info = new Sprite[3];
		public final Sprite word_downloading;

		public final Sprite word_set_drawer_icon_sel;
		public final Sprite word_set_shortcut_icon_sel;
		public final Sprite word_set_theme_sel;
		public final Sprite word_set_wall_sel;
		public final Sprite word_set_widget_sel;

		final String[] endStr = {"_nom","_sel"};
		public AssetObject (TextureAtlas atlas) {
			quick_btn = atlas.createSprite("quick_btn");//
			quick_btn.setSize(192, 192);
			quick_btn.setOrigin(97,97);
			//header
			header_sort = atlas.createSprite("header_sort");//
			header_sort.setSize(244, 244);
			header_sort.setOrigin(122, 122);
			header_setting = atlas.createSprite("header_setting");//
			header_setting.setSize(244, 244);
			header_close = atlas.createSprite("header_close");//
			header_close.setSize(244, 244);
			footer_recbtn = atlas.createSprite("footer_recbtn_sel");//
			footer_recbtn.setSize(244, 244);
			footer_recbtn.setOrigin(122, 122);
			footer_recRota = atlas.createSprite("footer_recomend_rota");//
			footer_recRota.setSize(244, 244);
			footer_recRota.setOrigin(121, 124);
			//header
			header_info = atlas.createSprite("header_btn_info");//
			header_info.setSize(244, 244);

			for(int n=0;n<2;n++){
				//header
				header_btn[n] = atlas.createSprite("header_btn" + endStr[n]);//
				header_btn[n].setSize(244, 244);
				header_btn[n].setOrigin(122, 122);
				//footer
				footer_btn[n] = atlas.createSprite("footer_btn" + endStr[n]);//
				footer_btn[n].setSize(244 ,244);
				footer_btn[n].setOrigin(122, 122);
				footer_recomend[n] = atlas.createSprite("footer_recomend" + endStr[n]);//
				footer_recomend[n].setSize(244, 244);
				footer_recomend[n].setOrigin(122, 122);
				footer_new[n] = atlas.createSprite("footer_new" + endStr[n]);//
				footer_new[n].setSize(244, 244);
				footer_new[n].setOrigin(122, 122);
				footer_ranking[n] = atlas.createSprite("footer_ranking" + endStr[n]);//
				footer_ranking[n].setSize(244, 244);
				//header
				header_mybox[n] = atlas.createSprite("header_mybox" + endStr[n]);//
				header_mybox[n].setSize(244, 244);
				header_mybox[n].setOrigin(122, 122);
				//quickpreview
				quick_detail[n] = atlas.createSprite("quick_detail" + endStr[n]);//
				quick_detail[n].setSize(192, 192);
				quick_detail[n].setOrigin(96,96);
				quick_like[n] = atlas.createSprite("quick_like" + endStr[n]);//
				quick_like[n].setSize(192, 192);
				quick_like[n].setOrigin(96,96);
				quick_set[n] = atlas.createSprite("quick_set" + endStr[n]);//
				quick_set[n].setSize(192, 192);
				quick_set[n].setOrigin(96,96);
				//detail
				detail_page[n]= atlas.createSprite("detail_page" + endStr[n]);//
				detail_page[n].setSize(20, 20);
				//charactor
				chara_btn[n]= atlas.createSprite("circle_chara" + endStr[n]);//
				chara_btn[n].setSize(134, 134);
				chara_btn[n].setOrigin(67, 67);
				//common
				ellipseBtn[n]= atlas.createSprite("detail_btn" + endStr[n]);//
				ellipseBtn[n].setSize(392, 92);
			}
			//charactor
			for(int i=0;i<7;i++){
				sort_chara[i]= atlas.createSprite("charactor" +  (i+1));//
				sort_chara[i].setSize(134, 134);
				sort_chara[i].setOrigin(67, 67);
			}
			//num
			for(int i=0;i<11;i++){
				num_update[i]= atlas.createSprite("num_update" +  i);//
				if(i<10) num_update[i].setSize(26, 36);
				else num_update[i].setSize(18, 36);
			}
			for(int i=0;i<10;i++){
				num_ranking[i]= atlas.createSprite("num_ranking" + (i+1));//
				num_ranking[i].setSize(42, 32);
			}
			//common
			ranking_crown= atlas.createSprite("runk_crown");//
			ranking_crown.setSize(104, 92);
			dialog_bg= atlas.createSprite("dialog_bg");//
			dialog_bg.setSize(1014, 622);
			thumbs_tag[0]= atlas.createSprite("tag_new");//
			thumbs_tag[0].setSize(170, 66);
			thumbs_tag[1]= atlas.createSprite("tag_limited");//
			thumbs_tag[1].setSize(170, 66);
			thumbs_tag[2]= atlas.createSprite("tag_premium");//
			thumbs_tag[2].setSize(170, 66);
			downloadDialog= atlas.createSprite("downloadtheme_bg");
			downloadDialog.setSize(652, 240);
			popup_close= atlas.createSprite("popup_close");//
			popup_close.setSize(130, 130);
			//info
			infoAllow = atlas.createSprite("setting_allow");//
			infoAllow.setSize(26, 46);
			//word
			word_set_drawer_icon= atlas.createSprite("word_d_set_diconall");//
			word_set_drawer_icon.setSize(392, 92);
			word_set_drawer_icon_sc= atlas.createSprite("word_d_set_sicon");
			word_set_drawer_icon_sc.setSize(392, 92);
			word_set_theme= atlas.createSprite("word_d_set_thete");//
			word_set_theme.setSize(392, 92);
			word_set_wall= atlas.createSprite("word_d_set_wall");//
			word_set_wall.setSize(392, 92);
			word_set_widget= atlas.createSprite("word_d_set_widjet");//
			word_set_widget.setSize(392, 92);

			//word black
			word_ok= atlas.createSprite("dialog_word_ok");//
			word_ok.setSize(392, 92);
			word_info[0] = atlas.createSprite("word_s_guide");//
			word_info[0].setSize(402, 62);
			word_info[1] = atlas.createSprite("word_s_help");//
			word_info[1].setSize(402, 62);
			word_info[2] = atlas.createSprite("word_s_menber");//
			word_info[2].setSize(402, 62);
			word_network_error= atlas.createSprite("dialog_word_networkerror");//
			word_network_error.setSize(1014, 622);
			word_d_example= atlas.createSprite("word_d_image");//
			word_d_example.setSize(626, 89);
			for(int i=0;i<5;i++){
				word_d_wall[i]= atlas.createSprite("word_d_wall"+  (i+1));//
				word_d_wall[i].setSize(626, 89);
			}
			word_d_drawer_image= atlas.createSprite("word_d_drawer");//
			word_d_drawer_image.setSize(626, 89);
			word_d_shortcut_icon= atlas.createSprite("word_d_sicon");//
			word_d_shortcut_icon.setSize(626, 89);
			word_d_drawer_icon= atlas.createSprite("word_d_dicon");//
			word_d_drawer_icon.setSize(626, 89);
			word_d_bat_widget= atlas.createSprite("word_d_batwidget");//
			word_d_bat_widget.setSize(626, 89);
			word_downloading= atlas.createSprite("w_downloading");//
			word_downloading.setSize(332, 62);
			word_set_drawer_icon_sel= atlas.createSprite("word_d_set_diconall_sel");//
			word_set_drawer_icon_sel.setSize(392, 92);
			word_set_shortcut_icon_sel= atlas.createSprite("word_d_set_sicon_sel");//
			word_set_shortcut_icon_sel.setSize(392, 92);
			word_set_theme_sel= atlas.createSprite("word_d_set_thete_sel");//
			word_set_theme_sel.setSize(392, 92);
			word_set_wall_sel= atlas.createSprite("word_d_set_wall_sel");//
			word_set_wall_sel.setSize(392, 92);
			word_set_widget_sel= atlas.createSprite("word_d_set_widjet_sel");//
			word_set_widget_sel.setSize(392, 92);
			word_premium_error= atlas.createSprite("dialog_word_premiumerror");//
			word_premium_error.setSize(1014, 622);
			word_no_addbox= atlas.createSprite("w_myboxempty");//
			word_no_addbox.setSize(802, 182);

			//flat
			downloading_detail = atlas.createSprite("downloading_detail");//
			downloading_detail.setSize(DETAIL_TEX_WIDTH, DETAIL_TEX_HEIGHT);
			loading_tutrial = atlas.createSprite("downloading_detail");//
//			loading_tutrial.setSize(TUTRIAL_TEX_WIDTH, TUTRIAL_TEX_HEIGHT);
			loading_tutrial.setSize(TUTRIAL_TEX_WIDTH+5, TUTRIAL_TEX_HEIGHT+4);//微調整
		}
	}
}
