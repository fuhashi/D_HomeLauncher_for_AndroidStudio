package jp.co.disney.apps.managed.kisekaeapp.widgetbattery;

    import java.util.Random;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader.TextureParameter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import jp.co.disney.apps.managed.kisekaeapp.DebugLog;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsDataDto;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperatorForWidget;
import jp.co.disney.apps.managed.kisekaeapp.spp.SPPUtility;
import jp.co.disney.apps.managed.kisekaeapp.system.file.LocalCacheFileHandleResolver;

    public class WidgetBatteryScreen extends ApplicationAdapter{

        private WidgetPickerActivity myWidgetActivity;

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
    	boolean setScreenSize = false;
//        float asp = 1f;
//        float reAsp = 100f;
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

        Random rnd = new Random();
        float delta;
        TextureParameter param;
        //解放系

        Texture parts1Mask;
        Texture wordBMask;
    	Texture downBGMask;
        Sprite loadingSP;
        Sprite blackMaskSP;
        Sprite borderSP;

        private ShaderProgram defaultShader = null;
        private ShaderProgram maskETCShader = null;
        boolean nowLoadingFirstAssets = true;
        boolean nowLoadingSecondAssets = true;
        float cnt;

//      Color BG_GRAY = new Color(0xe6e6e6FF);
      Color BG_GRAY = new Color(0x000000FF);//OS6taiou

        Sprite  bgSprite;
        final float THUMBS_TEX_SIZE=432f;//サムネイルのサイズ
        //おすすめが26個なので
//		final int THUMBSTEXMAX = 26;
        final int THUMBSTEXMAX = 12;
        int thumbsTexKazu = THUMBSTEXMAX;
        Sprite  thumbsNowLoadingSprite;
        Sprite[]  thumbsSprite = new Sprite[THUMBSTEXMAX];
        final float LOADING_ANIM_SPEED = -300f;

        final float THUMBS_U = 1.0f;
//        final float THUMBS_V = 462.0f/512.0f;
        final float THUMBS_V = 458.0f/512.0f;

        Rectangle[] rect_like = new Rectangle[THUMBSTEXMAX];
        Rectangle[] rect_set = new Rectangle[THUMBSTEXMAX];
        float[] thumbsY = new float[THUMBSTEXMAX];//表示テクスチャY
        float[] thumbsX = new float[THUMBSTEXMAX];
        int[] thumbsOrder = new int[THUMBSTEXMAX];//自分が一番上からから何番目か把握
        int[] nowIndex = new int[THUMBSTEXMAX];//上から何番目のArtを表示しているか
        float[] loadingRota = new float[THUMBSTEXMAX];//ローディングのぐるぐる用

        float upperLimit=0f; //ここ超えたら下に
        float underLimit=0f;//ここ超えたら上に
        float thumbsAllHeight;//サムネイル全体の高さ
        float thumbsTopY;
        int thumbsTotal = 1;
        Sprite[]  thumbsSpriteText = new Sprite[THUMBSTEXMAX];
//        final float THUMBS_TEXT_Y = 463.0f/512.0f;
        final float THUMBS_TEXT_Y = 459.0f/512.0f;
        final float THUMBS_TEXT_U = 408.0f/512.0f;
        final float THUMBS_TEXT_V = 511.0f/512.0f;
        final float THUMBS_TEXT_WIDTH = 448.0f;
//        final float THUMBS_TEXT_HEIGHT = 48.0f;
        final float THUMBS_TEXT_HEIGHT = 52.0f;

    	final float THUMBS_ZONE_HEIGHT_LIST=516f;//リスト表示時のサムネイル一段分の高さ
//    	final float THUMBS_IMAGE_X_LIST = 170f;//リスト表示時のサムネイルイメージのX座標
    	final float THUMBS_IMAGE_X_LIST = 66f;//リスト表示時のサムネイルイメージのX座標
    	final float THUMBS_IMAGE_Y_LIST = 44;//リスト表示時のサムネイルイメージのY座標
//    	final float THUMBS_TEXT_X_LIST = 626f;//リスト表示時のサムネイルテキストのX座標
    	final float THUMBS_TEXT_X_LIST = 564f;//リスト表示時のサムネイルテキストのX座標
//    	final float THUMBS_TEXT_Y_LIST = 248;//リスト表示時のサムネイルテキストのY座標
//    	final float THUMBS_TEXT_Y_LIST = 236;//リスト表示時のサムネイルテキストのY座標
    	final float THUMBS_TEXT_Y_LIST = 233;//リスト表示時のサムネイルテキストのY座

        final float QUICK_TOUCH_REC = 30f;//レコメンドのクイックプレビュータッチゾーンへのマージン
//		final Vector2 ListPosiLike = new Vector2(732,85);
//		final Vector2 ListPosiSet = new Vector2(896,85);
//        final Vector2 ListPosiLike = new Vector2(649,84);
//        final Vector2 ListPosiSet = new Vector2(813,84);
        final Vector2 ListPosiLike = new Vector2(580,72);
        final Vector2 ListPosiSet = new Vector2(804,72);

        //サムネイル読み込み用
//		final int MANAGER_MAX = 32;//架空スレッド数
        final int MANAGER_MAX = 16;//架空スレッド数
        public AssetManager[] assetsMulti = new AssetManager[MANAGER_MAX];
        boolean[] nowChangingThumbs = new boolean[THUMBSTEXMAX];//今、正にロード中(変更中)かどうか
        boolean[] nowLoadingThumbs = new boolean[THUMBSTEXMAX];//サムネイル画像のロード中か
        int[] useManagerNum = new int[THUMBSTEXMAX];
        String[] requesUnloadPath = new String[THUMBSTEXMAX];
        String[] nowLoadingPath = new String[THUMBSTEXMAX];
        int managerCount = 0;

        boolean nowUseETCShader = false;
        boolean menuBtnTapOK = false;

//        public static final String WIDGETTHUMBSPATH = "/data/data/jp.co.disney.apps.managed.kisekaeapp/cache/thumbnails/";
    //スクリーンの状態
        enum ScreenState {
            SCREEN_LOADWAIT,
            SCREEN_OFFLINE,
            SCREEN_SPLASH,
            SCREEN_SKIN
        }
        ScreenState nowScreenState = ScreenState.SCREEN_LOADWAIT;
    //タッチの状態
        boolean visibleDialog = false;//ダイアログ
        enum TouchState {
            TOUCH_DIABLE,
            TOUCH_SCROLL,
            TOUCH_SCROLL_BOUNCE,
            TOUCH_ONLY_DIALOG
        }
        TouchState nowTouchState = TouchState.TOUCH_DIABLE;
        boolean isOS404 = false;

        Rectangle rect_q_like;
        Rectangle rect_q_set;
        Rectangle rect_dialog_ok;
        int state_setNum= -1;
        int state_quick_like=0;
        int state_quick_set=0;

        public int getPM(){
            return rnd.nextInt(2)*2-1;
        }

        public void setActivity(WidgetPickerActivity me){
            this.myWidgetActivity = me;
        }

    	public void setBG_GRAY(){//OS6taiou
    		BG_GRAY.set(0xe6e6e6FF);
    	}

        @Override
        public void create() {
//			 DebugLog.instance.outputLog("info", "Math.asin(1)==" + Math.asin(1));

            String osVersion = android.os.Build.VERSION.RELEASE;
            DebugLog.instance.outputLog("info", "VERSION-" + osVersion);
            //KYY04は4.0.3なのにエラーになったので…
            if(osVersion.equals("4.0.3") || osVersion.equals("4.0.4")) isOS404 = true;
            else isOS404 = false;
            InputMultiplexer multiplexer = new InputMultiplexer();
            multiplexer.addProcessor(new MyInputListener());
            multiplexer.addProcessor(new GestureDetector(new MyGestureListener()));
            Gdx.input.setInputProcessor(multiplexer);
            //インスタンス初期化
            initializeInstance();
            //初期アセットロード(ロード画面に必要なデータ)
            loadAssetsFirst();
        }

        private void initializeInstance() {
            batch = new SpriteBatch();
            camPos = new Vector2();
            camera = new OrthographicCamera();
            assets = new AssetManager();// 一回だけでOK
            for(int i=0;i<MANAGER_MAX;i++){
                assetsMulti[i] = new AssetManager(new LocalCacheFileHandleResolver());
            }
            param = new TextureParameter();
            param.minFilter = TextureFilter.Linear;
            param.magFilter = TextureFilter.Linear;
            // 一瞬読み込みで止まるのでcreateの段階でやるべき事
            defaultShader = SpriteBatch.createDefaultShader();
            final String vertexMaskETCShader = Gdx.files.internal("data/etc1.vert").readString();
            final String fragmentMaskETCShader = Gdx.files.internal("data/etc1.frag").readString();
            maskETCShader = new ShaderProgram(vertexMaskETCShader,fragmentMaskETCShader);
            ETCShaderSet();
        }
        //spriteはここで
        private void initializeSprite() {
//			thumbsNowLoadingSprite = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class),0f,0f,THUMBS_U,THUMBS_V));
            thumbsNowLoadingSprite = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class),0f,0f,1.0f/3.0f,414.0f/512.0f));
            thumbsNowLoadingSprite.setScale(1f);
            thumbsNowLoadingSprite.setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
            thumbsNowLoadingSprite.setOrigin(THUMBS_TEX_SIZE/2f, THUMBS_TEX_SIZE/2f);
            for (int i = 0; i < THUMBSTEXMAX; i++) {
                thumbsSprite[i] = new Sprite(new TextureRegion(assets.get("data/mat_black.etc1", Texture.class), 0f, 0f, THUMBS_U, THUMBS_V));
                thumbsSprite[i].setScale(1f);
                thumbsSprite[i].setSize(THUMBS_TEX_SIZE, THUMBS_TEX_SIZE);
                thumbsSprite[i].setOrigin(THUMBS_TEX_SIZE / 2f, THUMBS_TEX_SIZE / 2f);
                rect_like[i] = new Rectangle(50000,0,132,132);
                rect_set[i] = new Rectangle(50000,0,132,132);
//				thumbsSpriteText[i] = new Sprite(new TextureRegion(assets.get("data/loading_thumbs.etc1", Texture.class), 0f, THUMBS_TEXT_Y, THUMBS_TEXT_U, THUMBS_TEXT_V));
                thumbsSpriteText[i] = new Sprite(new TextureRegion(assets.get("data/mat_black.etc1", Texture.class), 0f, THUMBS_TEXT_Y, THUMBS_TEXT_U, THUMBS_TEXT_V));
                thumbsSpriteText[i].setSize(THUMBS_TEXT_WIDTH, THUMBS_TEXT_HEIGHT);
                thumbsX[i] = 0;
                thumbsOrder[i] = i;
                nowIndex[i] = i;
                loadingRota[i] = 0;
                //loading
                nowChangingThumbs[i] = true;
                nowLoadingThumbs[i] = false;
                requesUnloadPath[i] = "";
                nowLoadingPath[i] = "";
            }
            rect_dialog_ok= new Rectangle(345, centerY-200, 390, 90);
            rect_q_like= new Rectangle(0, -1000, 132, 132);
            rect_q_set= new Rectangle(0, -1000, 132, 132);
        }
        //新着の初期化
        private void initializeScreen() {
            //nullチェックマスト
//	        if(cto!=null) cto.clear();
            //TODO
//	        cto = ContentsOperatorForWidget.op.getContentsDataArrayFromType(ContentsTypeValue.CONTENTS_TYPE_WIDGET);
//	        cto = ContentsOperatorForCatalog.op.getNewArrivalContents();
            cto = ContentsOperatorForWidget.op.getNewArrivalContents();
            thumbsTotal = cto.size;
            DebugLog.instance.outputLog("api", "新着の数：" + thumbsTotal);
            //Newフラグ、日付代入
            if(ctoIsNew!=null) ctoIsNew.clear();
            if(ctoDate!=null) ctoDate.clear();
            for (int i = 0; i < thumbsTotal; i++) {
                ctoIsNew.add(cto.get(i).getNewProperty());
                ctoDate.add(cto.get(i).getDateOfContents());
            }
    		thumbsTopY = topY;
    		thumbsAllHeight=0;

    		if(THUMBS_ZONE_HEIGHT_LIST *thumbsTotal<= realHeight){
    			offsetWidth = 0;
    			isBounce = false;
    		}
    		else{
    			offsetWidth = THUMBS_ZONE_HEIGHT_LIST *thumbsTotal - realHeight;// スクロール幅
    			isBounce = true;
    		}

            flickYdist = offsetWidth;
            thumbsTexKazu = Math.min(THUMBSTEXMAX,thumbsTotal);
            mEaseY = 0;
            mLastY = mEaseY;
            for (int i = 0; i < thumbsTexKazu; i++) {
                thumbsX[i] = 0;
                thumbsY[i] = thumbsTopY - (i*THUMBS_ZONE_HEIGHT_LIST);//サムネイル一段分
                thumbsOrder[i] = i;
                nowIndex[i] = i;
                loadingRota[i] = 0;
                //loading
                nowChangingThumbs[i] = true;
                nowLoadingThumbs[i] = false;
                String thumbnailID = String.valueOf(cto.get(i).assetID);
                loadingThumbs(i, thumbnailID);
                thumbsAllHeight+=THUMBS_ZONE_HEIGHT_LIST;
            }
            //SCREENの状態を変更
            nowScreenState = ScreenState.SCREEN_SKIN;
            //TOUCHの状態を変更
            nowTouchState = TouchState.TOUCH_SCROLL;
        }

        private void loadAssetsFirst() {
            assets.load("data/bg_mat.etc1", Texture.class,param);
            assets.load("data/loading.png", Texture.class,param);
            //mat
            assets.load("data/mat_black.etc1", Texture.class,param);
            nowLoadingFirstAssets = true;
        }
        //ロード画面以外のアセット
        private void loadAssetsSecond() {
            assets.load("data/tex_parts.txt", TextureAtlas.class);
            assets.load("data/loading_thumbs.etc1", Texture.class,param);
            assets.load("data/tex_word_b_mask.etc1", Texture.class,param);
            assets.load("data/tex_parts1_mask.ktx", Texture.class,param);
    		assets.load("data/downloadtheme_bg_mask.ktx", Texture.class,param);
            //mat
            assets.load("data/menu_line.etc1", Texture.class,param);
            nowLoadingSecondAssets = true;
        }
        private void doneLoadingFirstAssets() {
            bgSprite = new Sprite(assets.get("data/bg_mat.etc1", Texture.class));
//            bgSprite.setSize(1080, 1920);
            //TODO　縦長端末対策　2017/4/18
    		bgSprite.setSize(1080, realHeight);
            loadingSP = new Sprite(assets.get("data/loading.png", Texture.class));
            loadingSP.setPosition(481, centerY-59);
            loadingSP.setSize(118, 118);
            loadingSP.setOrigin(59,59);
            blackMaskSP = new Sprite(assets.get("data/mat_black.etc1", Texture.class));
            blackMaskSP.setAlpha(0.5f);
//            blackMaskSP.setSize(1080, 1920);
            //TODO　縦長端末対策　2017/4/18
            blackMaskSP.setSize(1080, realHeight);
            nowLoadingFirstAssets = false;
            //ロード画面以外のアセットロード
            loadAssetsSecond();
            if(nowScreenState != ScreenState.SCREEN_OFFLINE) nowScreenState = ScreenState.SCREEN_SPLASH;
        }
        private void doneLoadingSecondAssets() {
            assetObj = new AssetObject((TextureAtlas) assets.get("data/tex_parts.txt"));
            //sprite
            initializeSprite();
            parts1Mask = assets.get("data/tex_parts1_mask.ktx", Texture.class);
            wordBMask = assets.get("data/tex_word_b_mask.etc1", Texture.class);
    		downBGMask = assets.get("data/downloadtheme_bg_mask.ktx", Texture.class);
            borderSP = new Sprite(assets.get("data/menu_line.etc1", Texture.class));
            borderSP.setSize(1080, 16);
            nowLoadingSecondAssets = false;
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
//			nowLoadingPath[thumbsNum] = thumbsName + ".etc1";
//			DebugLog.instance.outputLog("check","パス：" + nowLoadingPath[thumbsNum]);
            assetsMulti[useManagerNum[thumbsNum]].load(nowLoadingPath[thumbsNum],Texture.class,param);
            nowLoadingThumbs[thumbsNum] = true;
            nowChangingThumbs[thumbsNum] = true;
            managerCount++;
            if(MANAGER_MAX<=managerCount) managerCount=0;
        }
        void doneLoadingThumbs(int thumbsNum){
            DebugLog.instance.outputLog("check","doneLoadingThumbs：" + thumbsNum + "番のサムネイル");
            thumbsSprite[thumbsNum].setTexture(assetsMulti[useManagerNum[thumbsNum]].get(nowLoadingPath[thumbsNum],Texture.class));
            thumbsSpriteText[thumbsNum].setTexture(assetsMulti[useManagerNum[thumbsNum]].get(nowLoadingPath[thumbsNum],Texture.class));
            requesUnloadPath[thumbsNum] = nowLoadingPath[thumbsNum];
            nowLoadingThumbs[thumbsNum] = false;
            nowChangingThumbs[thumbsNum] = false;
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
        @Override
        public void dispose() {
            //メモリーケア
//			1st
            if(!nowLoadingFirstAssets){
                defaultShader.dispose();
                maskETCShader.dispose();
            }
//			2nd
            if(!nowLoadingSecondAssets){
                parts1Mask.dispose();
            }
            batch.dispose();
            assets.dispose();
            for(int i=0;i<MANAGER_MAX;i++){
                assetsMulti[i].dispose();
            }
            DebugLog.instance.outputLog("flamework","call dispose!!!!!!" );
        }
        @Override
        public void pause() {
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
            delta = Gdx.graphics.getDeltaTime();

            if (nowLoadingFirstAssets){
                 if(assets.update()) doneLoadingFirstAssets();
            }
            if (nowLoadingSecondAssets){
                 if(assets.update()) doneLoadingSecondAssets();
            }
            for(int i=0;i<THUMBSTEXMAX;i++){
                if(nowLoadingThumbs[i] ){
                    if(assetsMulti[useManagerNum[i]].update()){
                        doneLoadingThumbs(i);
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

            switch(nowScreenState){
            case SCREEN_OFFLINE:
                if(!nowLoadingSecondAssets) DrawOffline();
                break;
            case SCREEN_SPLASH:
                DrawSplash();
                break;
            case SCREEN_SKIN:
                DrawScreen();
                break;
            default://NOMAL
                break;
            }

    		if(nowSettingThemes){
    			DrawDownloadThemeMask();
    		}

            if(visibleDialog) DrawDialog();

            if(nowTouchState == TouchState.TOUCH_SCROLL || nowTouchState == TouchState.TOUCH_SCROLL_BOUNCE
                    || nowTouchState != TouchState.TOUCH_ONLY_DIALOG) menuBtnTapOK = true;
            else menuBtnTapOK = false;
        }
    	void DrawDownloadThemeMask(){
//    		DebugLog.instance.outputLog("loop","DrawLoadingMask=" + isDownload);
    		batch.begin();
    		setDefaultShader();
    		blackMaskSP.draw(batch);
    		batch.flush();

    		setETCShader();
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
        void DrawOffline(){
            if(!visibleDialog){
                nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
                visibleDialog = true;
            }
            batch.begin();
            bgSprite.draw(batch);
            blackMaskSP.draw(batch);
            batch.end();
            DrawDialog();
        }
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
            assetObj.word_network_error.setPosition(33, centerY-311);
            assetObj.word_network_error.draw(batch);
            batch.end();
        }
        void DrawLoadingMask(){
            DrawLoadingMask(false);
        }
        void DrawLoadingMask(boolean isDownload){
//			DebugLog.instance.outputLog("loop","DrawLoadingMask=" + isDownload);
            if(isDownload) batch.begin();
            setDefaultShader();
            blackMaskSP.draw(batch);
            loadingSP.setPosition(481, centerY-59);
            loadingSP.rotate(delta*LOADING_ANIM_SPEED);
            loadingSP.draw(batch);
//			batch.flush();//一番上に来るはずなので
            if(isDownload) batch.end();
        }
        void DrawSplash(){
            batch.begin();
            //topY-高さで上合わせ
            bgSprite.draw(batch);
            DrawLoadingMask();
            batch.end();
            if(!nowLoadingSecondAssets && LoadingAppData){
                DebugLog.instance.outputLog("api", "スプラッシュ終了!!!!!!!!!!!!!!!!");
                initializeScreen();
            }
        }
        void ETCShaderSet(){
            maskETCShader.begin();
            maskETCShader.setUniformi("u_texture", 0);
            maskETCShader.setUniformi("m_texture", 1);
            maskETCShader.setUniformf("m_alpha", 1f);
            maskETCShader.end();
        }
        void DrawScreen(){
            //スクロールロジック(後でメソッド化する)
            calcScrollFlickEase();
            //まずオーダー順で座標計算
            for(int y=0;y<thumbsTexKazu  ;y++){
                int index = thumbsOrder[y];
                //サムネイルY計算
                if(y==0) thumbsY[index] = thumbsTopY - THUMBS_ZONE_HEIGHT_LIST + mEaseY;
                else thumbsY[index] =thumbsY[thumbsOrder[y-1]] - THUMBS_ZONE_HEIGHT_LIST;
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
                //thumbsOrder計算
                calcThumbsOrder(index1,true);
                //loading
                DebugLog.instance.outputLog("flamework","Thumbs load 上超えた:::" + index1);
                String thumbnailID = String.valueOf(cto.get(nowIndex[index1]).assetID);
                loadingThumbs(index1, thumbnailID);
            }
            //下超えた
            else if(thumbsY[index2]<underLimit && nowIndex[index2]-thumbsTexKazu>=0 && THUMBSTEXMAX<thumbsTotal){//下スクロール
                DebugLog.instance.outputLog("flamework","Thumbs move down NUM:::" + index2);
                nowIndex[index2]-=thumbsTexKazu;
                thumbsY[index2] =thumbsY[thumbsOrder[0]]+THUMBS_ZONE_HEIGHT_LIST;
                thumbsTopY+=THUMBS_ZONE_HEIGHT_LIST;//順番の違い注意
                //thumbsOrder計算
                calcThumbsOrder(index2,false);
                //loading
                DebugLog.instance.outputLog("flamework","Thumbs load 下超えた:::" + index2);
                String thumbnailID = String.valueOf(cto.get(nowIndex[index2]).assetID);
                loadingThumbs(index2, thumbnailID);
            }
            //描画
            batch.begin();
            //bg_mat
            bgSprite.draw(batch);
            //サムネイル
            for(int i=0;i<thumbsTexKazu;i++){
                setQuickRectZone(i);//タッチゾーン設定
                int dIndex = thumbsOrder[i];
                if(-THUMBS_ZONE_HEIGHT_LIST< thumbsY[dIndex]  && thumbsY[dIndex] <realHeight){
                    DrawThumbsList(dIndex);
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
            //共通
            batch.end();
        }
        void DrawThumbsList(int num){
            if (nowChangingThumbs[num]) {
                thumbsNowLoadingSprite.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_TEXT_Y_LIST);
                thumbsNowLoadingSprite.draw(batch);
                loadingSP.setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST+157, thumbsY[num]+THUMBS_TEXT_Y_LIST+157);
                loadingRota[num] += delta*LOADING_ANIM_SPEED;
                loadingSP.setRotation(loadingRota[num]);
                loadingSP.draw(batch);
            }
            else {
                thumbsSprite[num].setPosition(thumbsX[num]+THUMBS_IMAGE_X_LIST, thumbsY[num]+THUMBS_IMAGE_Y_LIST);
                thumbsSprite[num].draw(batch);
                thumbsSpriteText[num].setPosition(thumbsX[num]+THUMBS_TEXT_X_LIST, thumbsY[num]+THUMBS_TEXT_Y_LIST);
                thumbsSpriteText[num].draw(batch);
            }
                borderSP.setPosition(0, thumbsY[num]-6);
                borderSP.draw(batch);
        }
        void DrawThumbsListParts(int num){
            if(state_setNum==num) state_quick_set = 1;
            else state_quick_set = 0;
            state_quick_like = (cto.get(nowIndex[num]).isFavorite)?1:0;

            assetObj.quick_like[state_quick_like].setPosition(thumbsX[num]+ListPosiLike.x, thumbsY[num]+ListPosiLike.y);//+164
            assetObj.quick_set[state_quick_set].setPosition(thumbsX[num]+ListPosiSet.x, thumbsY[num]+ListPosiSet.y);
            assetObj.quick_like[state_quick_like].draw(batch);
            assetObj.quick_set[state_quick_set].draw(batch);
            //tag
            int tagState = -1;
			if(cto.get(nowIndex[num]).isPremium) tagState = 2;
			else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
			else if(ctoIsNew.get(nowIndex[num])) tagState=0;
//            if(ctoIsNew.get(nowIndex[num])) tagState=0;
//            else if(cto.get(nowIndex[num]).isLimitted) tagState = 1;
//            else if(cto.get(nowIndex[num]).isPremium) tagState = 2;

            if(0<=tagState){
//                assetObj.thumbs_tag[tagState].setPosition(thumbsX[num]+THUMBS_TEXT_X_LIST, thumbsY[num]+311);
//                assetObj.thumbs_tag[tagState].draw(batch);
    			assetObj.thumbs_tag[tagState].setPosition(thumbsX[num]+95, thumbsY[num]+381);
    			assetObj.thumbs_tag[tagState].draw(batch);
            }
            //update--後で修正 //TODO
            //0だと重くなる　20150805
            DrawDate(ctoDate.get(nowIndex[num]),thumbsY[num]);
        }
        //"2015/7/14"
        //新着用日付描画用
    	void DrawDate(String date,float thumbY){
    		int len = date.length();
    		//マージン26
    		if(8<=len){//ちゃんと返って来てる--基本返って来る想定…
//    			//year
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
//    					DebugLog.instance.outputLog("flamework", "sharpNum;;" + sharpNum);
    				}
    				else d = Integer.parseInt("" + m);
    				if(sharpNum<i) assetObj.num_update[d].setPosition(25*(i-5)+682-9, thumbY+312);
    				else assetObj.num_update[d].setPosition(25*(i-5)+682, thumbY+312);
    				assetObj.num_update[d].draw(batch);
    			}
    		}
    	}
        void SetAddLike(int num){
            if(!nowSettingFavorite){
//				ContentsOperatorForCatalog.op.callChangeFavoriteTask(cto.get(num));
                DebugLog.instance.outputLog("api", "SetAddLike!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!::" + cto.get(num).assetID);
                ContentsOperatorForWidget.op.callChangeFavoriteTask(cto.get(num));
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
            boolean isFavorite = ContentsOperatorForWidget.op.getContentsDataFromAssetId(setID).isFavorite;
            if(isFavorite){
                 myWidgetActivity.CallToast("お気に入りに保存しました");
            }
			else{
				myWidgetActivity.CallToast("お気に入りから削除しました");
			}
            nowSettingFavorite = false;
        }
        void setQuickRectZone(int num){
            rect_like[num].x = thumbsX[num] + ListPosiLike.x + QUICK_TOUCH_REC;
            rect_like[num].y = thumbsY[num] + ListPosiLike.y + QUICK_TOUCH_REC;
            rect_set[num].x = thumbsX[num] + ListPosiSet.x + QUICK_TOUCH_REC;
            rect_set[num].y = thumbsY[num] + ListPosiSet.y + QUICK_TOUCH_REC;
        }

        void resetStateBtn(){
            state_setNum= -1;
        }

        @Override
        public void resize(int width, int height) {
    		// 起動後初回のみ画面設定　20150903//バグ対応
    		if(!setScreenSize && height!=0){
    			setViewSize(width, height);
    			saveScreenSize = height;
    		}
    		else if(saveScreenSize != height){
    			setIrregularScreen(height);
    			saveScreenSize = height;
    		}
        }
    	void setIrregularScreen(int height){
    		DebugLog.instance.outputLog("info", "setIrregularScreen" + height);
    		camera.viewportHeight = height;
    		float changeY = (viewHeight-height)*uiPer*0.5f;
    		camera.position.set(camPos.x, camPos.y+changeY, 0.0f);
    		camera.update();
    	}

        // 以前のライブラリを踏襲
        public void setViewSize(int width, int height) {
            viewWidth = width;
            viewHeight = height;
            DebugLog.instance.outputLog("info", "viewWidth=" + viewWidth);
            DebugLog.instance.outputLog("info", "viewHeight=" + viewHeight);
    		// 起動後初回のみ画面設定　20150903//バグ対応
    		setChangeScreen();
    		setScreenSize = true;
        }

        void setChangeScreen() {
            // 縦横変わっった時だけ呼ばれる
            DebugLog.instance.outputLog("flamework", "setChangeScree");
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
        }
        //フリック関係
        float dragReY = 0f;
        float  flickYdist=0f;
        //全体のデフォルトをイージングにした時の速度
        public int allEaseSpFlick					= 10;
        // イージングの際どこのポイントを基準にイージングさせるか
        public float mLastY					= 0.0f;
        public float mGoalY					= 0.0f;
        public float mEaseY					= 0.0f;
        private float offsetWidth				= 0;
        long lastFlingTime= System.currentTimeMillis();
        long lastTouchUpTime= System.currentTimeMillis();
//		long lastTouchDraggTime= System.currentTimeMillis();
        float flingStateTime;
        float addFling;
        final float FLINGMAX=1200f;
        final float FLINGPER=0.25f;
        float addDragg;
        final float DRAGGMAX=120f;
        final float DRAGGPER=1.1f;
        //サムネイルタップ可能か
        boolean tapAbble = false;
        //bounce
        float bounceDist = 150f;
        float  bounceGoal = 0f;
//	    boolean permitBounce = false;
        boolean permitBounceUP = false;
        boolean permitBounceDOWN = false;
        boolean isBounce = true;


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
            else{
//	    		DebugLog.instance.outputLog("loop", "mEaseY=" + mEaseY);
                mGoalY=offsetWidth-flickYdist;
                if(mGoalY<=-bounceDist) mGoalY=-bounceDist;
                else if(mGoalY>=offsetWidth+bounceDist) mGoalY=offsetWidth+bounceDist;
                mEaseY+=(mGoalY-mLastY)/allEaseSpFlick;
                float val = Math.abs(mLastY-mEaseY);
                if(val<5) tapAbble=true;
                else tapAbble=false;
                mLastY=mEaseY;
            }
////			//全体のイージングを計算
//			mGoalY=offsetWidth-flickYdist;
//			if(mGoalY<=0) mGoalY=0;
//			else if(mGoalY>=offsetWidth) mGoalY=offsetWidth;
//			mEaseY+=(mGoalY-mLastY)/allEaseSpFlick;
//			float val = Math.abs(mLastY-mEaseY);
//			if(val<5) tapAbble=true;
//			else tapAbble=false;
//			mLastY=mEaseY;
        }
        //TODO
        //TODO
        //TODO
        //TODO
        //とりあえずアルファ用にテーマ設定時呼ぶメソッド
        void SetTheme(ContentsDataDto setCto){
        	DebugLog.instance.outputLog("api", "SetTheme!!!!!!!");
    	    if(!SPPUtility.checkNetwork(myWidgetActivity.getApplicationContext())){
    	    	//ここでダイアログ表示
    	    	DebugLog.instance.outputLog("api", "ネットワークない(DetailTap時)!!!!!!!!!");
    	    	visibleDialog = true;
    	    	nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
    	    	return;
    	    }
        	nowSettingThemes = true;
        	nowTouchState = TouchState.TOUCH_DIABLE;
        	//メソッド呼び出し//TODO 仮
        	ContentsOperatorForWidget.op.callDownloadSkinTask(setCto);
//        	 ContentsOperatorForWidget.op.callDownloadSkinTask(ContentsOperatorForWidget.op.getContentsDataFromAssetId(10000005193L));
        }
        public void restartSetTheme(){
        	ContentsOperatorForWidget.op.restartDownloadSkinTask();
        }

        //成功時は必ず終了するので　失敗時のみ
        public void onFailedDownloadSkin(){
        	resetStateBtn();
        	nowSettingThemes = false;
            if(!visibleDialog){
                nowTouchState = TouchState.TOUCH_ONLY_DIALOG;
                visibleDialog = true;
            }
        }
        public void onFinishedAllDataDownload(){
            //情報取得成功
            DebugLog.instance.outputLog("api", "onFinishedAllDataDownload!!!!!!!");
            LoadingAppData =true;
            //TODO 一旦ここで受け取る
            //nullチェックマスト
//	        if(cto!=null) cto.clear();
//	        cto = ctoArray;
        }
        public void onFailedAllDataDownload(){
            DebugLog.instance.outputLog("api", "onFailedAllDataDownload!!!!!!!");
            nowScreenState = ScreenState.SCREEN_OFFLINE;
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
                //1(順番)
//				DebugLog.instance.outputLog("touched", "InputProcessor touchDown");
                if(pointer!=0) return false;

                if(nowTouchState != TouchState.TOUCH_DIABLE){
                    dragReY = y*uiPer;
                    permitBounceUP= false;
                    permitBounceDOWN= false;
                }
                if(nowTouchState == TouchState.TOUCH_SCROLL && isBounce){
//					DebugLog.instance.outputLog("touched", "permitBounce判定!!!!!!!!!");
                    if(flickYdist==0){
                        permitBounceUP = true;
                        DebugLog.instance.outputLog("touched", "permitBounceUP!!!!!!!!!");
                    }
                    else if(flickYdist==offsetWidth){
                        permitBounceDOWN = true;
                        DebugLog.instance.outputLog("touched", "permitBounceDOWN!!!!!!!!!");
                    }
                }
                return false;
            }
            @Override
            public boolean touchUp (int x, int y, int pointer, int button) {
//				DebugLog.instance.outputLog("touched", "InputProcessor touchUp pointer::" + pointer);
                if(pointer!=0) return false;
                //5(順番)
                if(nowTouchState == TouchState.TOUCH_SCROLL){
                    lastTouchUpTime = System.currentTimeMillis();
                    if(isBounce){
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
                }
                return false;
            }
            @Override
            public boolean touchDragged (int x, int y, int pointer) {
//				DebugLog.instance.outputLog("touched", "InputProcessor touchDragged pointer::" + pointer);
                if(pointer!=0) return false;
                //3(順番)
//				DebugLog.instance.outputLog("touched", "InputProcessor touchDragged");
                if(nowTouchState == TouchState.TOUCH_SCROLL){
                    long stopTime = System.currentTimeMillis();
                    long time = stopTime - lastTouchUpTime;
                    if(time>100){
                        float touchY = y*uiPer;
                        if(Math.abs(dragReY-touchY)>1){
                            addDragg =(dragReY-touchY)*DRAGGPER;
                            //制限
                            if(addDragg<=-DRAGGMAX) addDragg=-DRAGGMAX;
                            if(addDragg>=DRAGGMAX) addDragg=DRAGGMAX;
                            flickYdist -=addDragg;
                        }
                        if(permitBounceUP){
                            if(flickYdist>=offsetWidth) flickYdist=offsetWidth;//下にはいかせない
                            else if(flickYdist<=-bounceDist) flickYdist=-bounceDist;
                        }
                        else if(permitBounceDOWN){
                            if(flickYdist<=0) flickYdist=0;//上にはいかせない
                            else if(flickYdist>=offsetWidth+bounceDist) flickYdist=offsetWidth+bounceDist;
                        }
                        else{
                            if(flickYdist<=0) flickYdist=0;
                            else if(flickYdist>=offsetWidth) flickYdist=offsetWidth;
                        }
                            dragReY = touchY;
                    }
                }
                return false;
            }
        }
        public class MyGestureListener implements GestureListener{
            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                //2(順番)
//		    	DebugLog.instance.outputLog("touched", "GestureListener touchDown");
                return false;
            }
            @Override
            public boolean tap(float x, float y, int count, int button) {
//		    	DebugLog.instance.outputLog("touched", "GestureListener tap count::" + count);
                if(count!=1) return false;//ここはちょっと怪しい
                //InputProcessor touchUpの後
//		    	DebugLog.instance.outputLog("touched", "GestureListener tap");
                if(nowTouchState != TouchState.TOUCH_DIABLE){
                    camera.unproject(touchPos.set(x,y, 0));
                    if(nowTouchState == TouchState.TOUCH_ONLY_DIALOG){
                        if(rect_dialog_ok.contains(touchPos.x, touchPos.y)){
                            if(nowSettingThemes){
                                visibleDialog = false;
                                resetStateBtn();
                                nowSettingThemes = false;
                                nowTouchState = TouchState.TOUCH_SCROLL;
                            }
                            else{
                                visibleDialog = false;
                                nowTouchState = TouchState.TOUCH_SCROLL;
                                resetStateBtn();//ステートボタンリセット
                                if(nowScreenState == ScreenState.SCREEN_OFFLINE){
                                    DebugLog.instance.outputLog("touched", "アプリ終了させる！！！！！！！！！！！！！");
                                    myWidgetActivity.FinishApp();
                                }
                            }
                        }
                    }
                    else if(nowScreenState == ScreenState.SCREEN_SKIN && tapAbble){
                        for(int i=0;i<thumbsTexKazu;i++){
                            if(!nowChangingThumbs[i]){//ロード済
                                if(rect_like[i].contains(touchPos.x,touchPos.y)){
                                    DebugLog.instance.outputLog("touched", "rect_q_like tap!!!!!!!!!!");
                                    SetAddLike(nowIndex[i]);
                                }
                                else if(rect_set[i].contains(touchPos.x,touchPos.y)){
                                    DebugLog.instance.outputLog("touched", "rect_q_set tap!!!!!!!!!!");
                                    SetTheme(cto.get(nowIndex[i]));
                                    state_setNum= i;
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
//		    flingが発生しない時対策--タッチアップからの時間で見てやる？
            @Override
            public boolean fling(float velocityX, float velocityY, int button) {
//		    	DebugLog.instance.outputLog("touched", "GestureListener fling button::" + button);
                //7(順番)
//		    	DebugLog.instance.outputLog("touched", "GestureListener fling::" + velocityY* uiPer);
                if (nowTouchState == TouchState.TOUCH_SCROLL) {
                    long stopTime = System.currentTimeMillis();
                    long time = stopTime - lastFlingTime;
                    flingStateTime += time;
                    addFling = velocityY * uiPer * FLINGPER;
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
                }
                return false;
            }
            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                //4(順番)
//		    	DebugLog.instance.outputLog("touched", "GestureListener pan");
                return false;
            }
            @Override
            public boolean panStop(float x, float y, int pointer, int button) {
                //6(順番)
//		    	DebugLog.instance.outputLog("touched", "GestureListener panStop");
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

            //quickpreview
            public final Sprite[] quick_like = new Sprite[2];
            public final Sprite[] quick_set = new Sprite[2];
            //common
            public final Sprite dialog_bg;//
            public final Sprite[] thumbs_tag = new Sprite[3];//サムネイルのタグ　0-new 1-limited 2-premiun 3-none
            public final Sprite[] ellipseBtn = new Sprite[2];//楕円ボタン
    		public final Sprite downloadDialog;
            //word
            public final Sprite word_ok;
            //word black
            public final Sprite word_network_error;
    		public final Sprite word_downloading;
            //num
            public final Sprite[] num_update = new Sprite[11];
//            public final Sprite[] num_update_year = new Sprite[4];
            final String[] endStr = {"_nom","_sel"};
            public AssetObject (TextureAtlas atlas) {
                for(int n=0;n<2;n++){
                    //common
                    ellipseBtn[n]= atlas.createSprite("detail_btn" + endStr[n]);//
                    ellipseBtn[n].setSize(392, 92);
                    quick_like[n] = atlas.createSprite("quick_like" + endStr[n]);//
                    quick_like[n].setSize(192, 192);
                    quick_set[n] = atlas.createSprite("quick_set" + endStr[n]);//
                    quick_set[n].setSize(192, 192);
                }
                //num
    			for(int i=0;i<11;i++){
    				num_update[i]= atlas.createSprite("num_update" +  i);//
    				if(i<10) num_update[i].setSize(26, 36);
    				else num_update[i].setSize(18, 36);
    			}
                //common
                dialog_bg= atlas.createSprite("dialog_bg");//
                dialog_bg.setSize(1014, 622);
                thumbs_tag[0]= atlas.createSprite("tag_new");//
                thumbs_tag[0].setSize(170, 66);
                thumbs_tag[1]= atlas.createSprite("tag_limited");//
                thumbs_tag[1].setSize(170, 66);
                thumbs_tag[2]= atlas.createSprite("tag_premium");//
                thumbs_tag[2].setSize(170, 66);
    			downloadDialog= atlas.createSprite("downloadtheme_bg");//
    			downloadDialog.setSize(652, 240);
                //word
                word_ok= atlas.createSprite("dialog_word_ok");//
                word_ok.setSize(392, 92);
                //word black
                word_network_error= atlas.createSprite("dialog_word_networkerror");//
                word_network_error.setSize(1014, 622);
    			word_downloading= atlas.createSprite("w_downloading");//
    			word_downloading.setSize(332, 62);
            }
        }
    }
