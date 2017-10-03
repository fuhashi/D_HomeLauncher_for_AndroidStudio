package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.system.view.PopupLayerView;
import jp.co.disney.apps.managed.kisekaeapp.system.view.PopupView;

public class HomeMenuView extends FrameLayout implements ThemeHistoryMenuView.OnItemSelectListener {

    public static final String KEY_BUTTON_KISEKAE = "kisekae";
    public static final String KEY_BUTTON_THEME_HISTORY = "theme_history";
    public static final String KEY_BUTTON_WALLPAPER = "wallpaper";
    public static final String KEY_BUTTON_WIDGET = "add_screen";
    public static final String KEY_BUTTON_SETTINGS = "setting";

    private static final long SHOW_ANI_DURATION = 230L;
    private static final long DISMISS_ANI_DURATION = 200L;
    private static final long DISMISS_ANI_DURATION2 = 400L;

    private static final int MENU_ITEM_ID[];

    static {
        MENU_ITEM_ID = new int[] {
            R.id.menu_item_1, R.id.menu_item_2, R.id.menu_item_3, R.id.menu_item_4, R.id.menu_item_5
        };
    }

    private final MenuItem mMenuItemList[];

    private boolean mIsAnimationRunning;
    private View mLastClickedItem;
    private Map<String, View> mMenuItemMap;
    private FrameLayout mMenuFrame;
    private PopupView mPopup;

    private final Rect mHitTestRect;

    private View.OnClickListener mItemClickListener;
    private View.OnClickListener mKisekaeIconClickListener;

    private boolean mKisekaeIconClicked = false;
    private boolean mShowHistory = false;
    private ValueAnimator mThemeHistoryShowAnim;
    private ValueAnimator mThemeHistoryHideAnim;
    private float mTempT;
    private String mSelectedThemeId;
    private ThemeHistoryMenuView mHistoryMenuView;
    private KisekaeIconView mKisekaeIconView;
    private int mHistoryMenuHeight;
    private int mMenuHeight;
    private boolean mFromThemeHistory = false;

    private RelativeLayout mMenuItem1;
    private RelativeLayout mMenuItem2;
    private RelativeLayout mMenuItem3;
    private RelativeLayout mMenuItem4;
    private RelativeLayout mMenuItem5;

    private ValueAnimator mDismissTransAnim;

    public void setOnMenuItemClickListener(View.OnClickListener onClicklistener) {
        mItemClickListener = onClicklistener;
    }

    private OnHistoryItemSelectListener mOnHistoryItemSelectListener;

    public void setOnHistoryItemClickListener(OnHistoryItemSelectListener onHistoryItemSelectListener) {
        mOnHistoryItemSelectListener = onHistoryItemSelectListener;
    }

    public interface OnHistoryItemSelectListener {
        public void onItemSelect(String themeId);
    }

    public void setOnKisekaeIconClickListener(View.OnClickListener onClicklistener) {
        mKisekaeIconClickListener = onClicklistener;
    }

    private View.OnClickListener mInternalKisekaeIconClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            mKisekaeIconClicked = true;
            hideThemeHistory(true, true);
        }
    };

    private View.OnClickListener internalMenuItemClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            if (mShowHistory) {
                return;
            }

            if (mLastClickedItem == null) {

                View clickedItem = (View) view.getParent();

                String tag = (String) clickedItem.getTag();
                if (HomeMenuView.KEY_BUTTON_THEME_HISTORY.equals(tag)) {

                    if (mSubMenuView != null) {
                        closeSubMenu(false, true);
                    }
                    showThemeHistory();

                    return;
                } else if (HomeMenuView.KEY_BUTTON_SETTINGS.equals(tag)) {

                    if (mSubMenuView != null) {
                        closeSubMenu(false, true);
                    } else {
                        showSubMenuForSettings();
                    }
                    return;
                }

                mLastClickedItem = clickedItem;

                if (mSubMenuView != null) {
                    closeSubMenu(true, true);
                } else {
                    mPopup.cancel(true, DISMISS_ANI_DURATION);
                }
            }
        }
    };

    private HomeSubMenuView mSubMenuView = null;
    private ValueAnimator mSubMenuOpenAnim = null;
    private ValueAnimator mSubMenuCloseAnim = null;

    private void showSubMenuForSettings() {

        if (mSubMenuView != null) return;

        mSubMenuView = new HomeSubMenuView(getContext(), LauncherApplication.getScreenDensity());
        mSubMenuView.initForSettings(getMeasuredWidth(), true);
        showSubMenu();
    }

    private boolean mCloseMenu = false;

    private void closeSubMenu(boolean closeMenu, boolean animate) {

        if (mSubMenuView == null) return;

        if (mSubMenuOpenAnim != null) {
            mSubMenuOpenAnim.cancel();
            mSubMenuOpenAnim = null;
        }

        mCloseMenu = closeMenu;

        if (mSubMenuCloseAnim != null) {
            return;
        }

        if (animate) {

            final float oldT = mSubMenuView.getScaleX();
            mSubMenuCloseAnim = ValueAnimator.ofFloat(oldT, 0.0f);
            mSubMenuCloseAnim.setDuration((int) (200 * oldT));
            mSubMenuCloseAnim.setInterpolator(new AccelerateInterpolator());
            mSubMenuCloseAnim.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (mSubMenuView != null) {
                        float t = (Float) animation.getAnimatedValue();
                        mSubMenuView.setScaleX(t);
                        mSubMenuView.setScaleY(t);
                        mSubMenuView.setAlpha(t);
                        mSubMenuView.requestLayout();
                    }
                }
            });

            mSubMenuCloseAnim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    if (mSubMenuView != null) {
                        removeView(mSubMenuView);
                        mSubMenuView.dispose();
                        mSubMenuView = null;
                    }

                    if (mCloseMenu) {
                        mCloseMenu = false;
                        mPopup.cancel(true, DISMISS_ANI_DURATION);
                    }

                    mSubMenuCloseAnim = null;
                }

                @Override
                public void onAnimationStart(Animator animation) {}

                @Override
                public void onAnimationRepeat(Animator animation) {}

                @Override
                public void onAnimationCancel(Animator animation) {}
            });

            mSubMenuCloseAnim.start();
        } else {

            if (mSubMenuView != null) {
                removeView(mSubMenuView);
                mSubMenuView.dispose();
                mSubMenuView = null;
            }

            if (mCloseMenu) {
                mCloseMenu = false;
                mPopup.cancel(false, DISMISS_ANI_DURATION);
            }
        }
    }

    private void showSubMenu() {

        final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        lp.width = mSubMenuView.getStaticWidth();
        lp.height = mSubMenuView.getStaticHeight();

        lp.leftMargin = mMenuFrame.getMeasuredWidth() - lp.width + (int) Math.ceil(7 * LauncherApplication.getScreenDensity());
        lp.topMargin = getMeasuredHeight() - (int) Math.ceil(mMenuFrame.getMeasuredHeight() * 0.76f) - lp.height;

        mSubMenuView.setPivotX(mSubMenuView.getArrowLeftMargin() + mSubMenuView.getArrowWidth() / 2);
        mSubMenuView.setPivotY(mSubMenuView.getStaticHeight());

        mSubMenuView.setOnClickListener(new HomeSubMenuView.OnClickListener() {
            @Override
            public void onMenuClick(View v) {
                mLastClickedItem = v;
                closeSubMenu(true, true);
            }
        });

        addView(mSubMenuView, lp);

        mSubMenuOpenAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        mSubMenuOpenAnim.setDuration(200);
        mSubMenuOpenAnim.setInterpolator(new DecelerateInterpolator(1.3f));
        mSubMenuOpenAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mSubMenuView != null) {
                    float t = (Float) animation.getAnimatedValue();
                    mSubMenuView.setScaleX(t);
                    mSubMenuView.setScaleY(t);
                    mSubMenuView.setAlpha(t);
                    mSubMenuView.requestLayout();
                }
            }
        });
        mSubMenuOpenAnim.start();
    }

    private void showThemeHistory() {

        mShowHistory = true;

        final int width = mMenuFrame.getMeasuredWidth();

        int mThumbSize = (int) Math.ceil(324 * width / 1080f);
        int mPaddingTop = (int) Math.ceil(98 * width / 1080f);
        int paddingBottom = (int) Math.ceil(72 * width / 1080f);
        int mGapY = (int) Math.ceil(54 * width / 1080f);

        mHistoryMenuHeight = mThumbSize * 2 + mPaddingTop + paddingBottom + mGapY + 20;
        mMenuHeight = mMenuFrame.getMeasuredHeight();

//        mTempT = 0.0f;

        mThemeHistoryShowAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        mThemeHistoryShowAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                float t = (Float) animation.getAnimatedValue();
                mTempT = t;

                mMenuItem2.setAlpha(1 - t * t);
                mMenuItem3.setAlpha(1 - t * t);
                mMenuItem4.setAlpha(1 - t * t);
                mMenuItem5.setAlpha(1 - t * t);

                mMenuItem1.setAlpha(1 - t);
                mKisekaeIconView.setAlpha(t);
                mKisekaeIconView.setTranslationY((mHistoryMenuHeight - mMenuHeight) * (1 - t));
                mMenuFrame.setTranslationY(mHistoryMenuHeight - mMenuHeight - (mHistoryMenuHeight - mMenuHeight) * t);
            }
        });
        mThemeHistoryShowAnim.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mMenuFrame.getLayoutParams();
                lp.height = mHistoryMenuHeight;
                mMenuFrame.setLayoutParams(lp);

                mKisekaeIconView = new KisekaeIconView(getContext());
//                BitmapDrawable bmpD = (BitmapDrawable) getResources().getDrawable(R.drawable.btn_kisekae);
//                mKisekaeIconView.setImageDrawable(bmpD);
                float iconSize =  getMeasuredWidth() / 5f;
//                float scale = iconSize / (float) bmpD.getBitmap().getWidth();
                int iconRawSize = ((BitmapDrawable) mKisekaeIconView.getDrawable()).getBitmap().getWidth();
                float scale = iconSize / (float) iconRawSize;
                mKisekaeIconView.setScaleX(scale);
                mKisekaeIconView.setScaleY(scale);
                FrameLayout.LayoutParams flp2 = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                int delta = (int) Math.ceil((1.0f - scale) * iconRawSize / 2);
                flp2.topMargin =  getMeasuredHeight() - mHistoryMenuHeight - delta + 8;
                flp2.leftMargin = -delta - 6;
                mKisekaeIconView.setAlpha(0f);
                mKisekaeIconView.setOnClickListener(mInternalKisekaeIconClickListener);

                addView(mKisekaeIconView, flp2);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {

                final float iconSize = getMeasuredWidth() / 5f;

                ValueAnimator valAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
                valAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {

                        float t = (Float) animation.getAnimatedValue();
//                        mKisekaeIconView.setTranslationX(t * 20);
                        mKisekaeIconView.setTranslationY(- t * iconSize * 0.4f);
                    }
                });
                valAnim.setInterpolator(new LinearInterpolator());
                valAnim.setDuration(300L);
                valAnim.start();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mHistoryMenuView = new ThemeHistoryMenuView(getContext());
                        mHistoryMenuView.setOnItemSelectListener(HomeMenuView.this);
                        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        mMenuFrame.addView(mHistoryMenuView, flp);
                    }
                }, 100L);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });

        mThemeHistoryShowAnim.setInterpolator(new DecelerateInterpolator());
        mThemeHistoryShowAnim.setDuration(330);
        mThemeHistoryShowAnim.setStartDelay(300L);
        mThemeHistoryShowAnim.start();

    }

    @Override
    public void onItemSelect(int index, String themeId) {
        mSelectedThemeId = themeId;
        hideThemeHistory(true, true);
    }

    private void hideThemeHistory(final boolean closeMenu, boolean animate) {

        if (mThemeHistoryShowAnim != null) {
            mThemeHistoryShowAnim.cancel();
            mThemeHistoryShowAnim = null;
        }

        boolean animFlg = false;

        if (animate) {

            if (mKisekaeIconView != null || mHistoryMenuView != null) {

                animFlg = true;

                final int itemCount;
                if (mHistoryMenuView != null) {
                    itemCount = mHistoryMenuView.getItemCount();
                } else {
                    itemCount = 0;
                }

                ValueAnimator valAnim = ValueAnimator.ofFloat(1.0f, 0.0f);
                valAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {

                        float t = (Float) animation.getAnimatedValue();

                        if (mKisekaeIconView != null) {
                            mKisekaeIconView.setScaleX(t);
                            mKisekaeIconView.setScaleY(t);
                        }

                        if (mHistoryMenuView != null && itemCount > 0) {
                            for (int i = 0; i < itemCount; i++) {
                                mHistoryMenuView.setTestT(i, (1.0f - t));
                            }
                            mHistoryMenuView.invalidate();
                        }
                    }
                });
                valAnim.addListener(new Animator.AnimatorListener() {

                    @Override
                    public void onAnimationStart(Animator animation) {

                        if (mHistoryMenuView != null) {
                            mHistoryMenuView.cancelAnim();
                        }
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {

                        if (mKisekaeIconView != null) {
                            removeView(mKisekaeIconView);
                        }

                        if (mHistoryMenuView != null) {
                            mMenuFrame.removeView(mHistoryMenuView);
                        }

                        if (closeMenu) {
                            mDismissTransAnim.setDuration(DISMISS_ANI_DURATION2);
                            mFromThemeHistory = true;
                            mPopup.cancel(true, DISMISS_ANI_DURATION2);
                            mShowHistory = false;
                            mHistoryMenuView = null;
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                valAnim.setInterpolator(new AccelerateInterpolator());
                valAnim.setDuration(300L);
                valAnim.start();
            }
        } else {

            if (mKisekaeIconView != null) {
                removeView(mKisekaeIconView);
            }

            if (mHistoryMenuView != null) {
                mHistoryMenuView.cancelAnim();
                mMenuFrame.removeView(mHistoryMenuView);
            }

            if (closeMenu) {

                mShowHistory = false;
                mHistoryMenuView = null;

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mMenuFrame.getLayoutParams();
                if (lp.height != mMenuHeight) {
                    lp.height = mMenuHeight;
                    mMenuFrame.setLayoutParams(lp);
                    mMenuFrame.setTranslationY(0);
                }
                mMenuItem1.setAlpha(1.0f);
                mMenuItem2.setAlpha(1.0f);
                mMenuItem3.setAlpha(1.0f);
                mMenuItem4.setAlpha(1.0f);
                mMenuItem5.setAlpha(1.0f);

                mDismissTransAnim.setDuration(DISMISS_ANI_DURATION);

                mPopup.cancel(false, 0);
            }
        }

        if (closeMenu) return;

        float startT = mTempT;
//        float startT = (800 - 214 - mMenuFrame.getTranslationY()) / 400f;

        mThemeHistoryHideAnim = ValueAnimator.ofFloat(startT, 0.0f);
        mThemeHistoryHideAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                float t = (Float) animation.getAnimatedValue();
                mMenuItem1.setAlpha(1 - t * t);
                mMenuItem2.setAlpha(1 - t * t);
                mMenuItem3.setAlpha(1 - t * t);
                mMenuItem4.setAlpha(1 - t * t);
                mMenuItem5.setAlpha(1 - t * t);
                mMenuFrame.setTranslationY(mHistoryMenuHeight - mMenuHeight - (mHistoryMenuHeight - mMenuHeight) * t);
            }
        });
        mThemeHistoryHideAnim.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mMenuFrame.getLayoutParams();
                lp.height = mMenuHeight;
                mMenuFrame.setLayoutParams(lp);
                mMenuFrame.setTranslationY(0);
                mShowHistory = false;
                mThemeHistoryHideAnim = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mThemeHistoryHideAnim.setInterpolator(new DecelerateInterpolator());
        mThemeHistoryHideAnim.setDuration((long) (330 * startT));
        if (animFlg) {
            mThemeHistoryHideAnim.setStartDelay(400L);
        }
        mThemeHistoryHideAnim.start();
    }

    private Animator.AnimatorListener mDismissAnimListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationCancel(Animator animator) {
            mIsAnimationRunning = false;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mIsAnimationRunning = false;

            if (mFromThemeHistory) {

                mFromThemeHistory = false;

                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mMenuFrame.getLayoutParams();
                if (lp.height != mMenuHeight) {
                    lp.height = mMenuHeight;
                    mMenuFrame.setLayoutParams(lp);
                    mMenuFrame.setTranslationY(0);
                }
                mMenuItem1.setAlpha(1.0f);
                mMenuItem2.setAlpha(1.0f);
                mMenuItem3.setAlpha(1.0f);
                mMenuItem4.setAlpha(1.0f);
                mMenuItem5.setAlpha(1.0f);

                mDismissTransAnim.setDuration(DISMISS_ANI_DURATION);
            }

            post(new Runnable() {

                public void run() {
                    if(mItemClickListener != null && mLastClickedItem != null) {
                        mItemClickListener.onClick(mLastClickedItem);
                    }
                    mLastClickedItem = null;

                    if (mSelectedThemeId != null) {
                      if (mOnHistoryItemSelectListener != null) {
                          final String themeId = mSelectedThemeId;
                          mOnHistoryItemSelectListener.onItemSelect(themeId);
                      }
                      mSelectedThemeId = null;
                    }

                    if (mKisekaeIconClicked) {
                        mKisekaeIconClicked = false;
                        if (mKisekaeIconClickListener != null) {
                            mKisekaeIconClickListener.onClick(mKisekaeIconView);
                        }
                    }
                }
            });
        }

        @Override
        public void onAnimationStart(Animator animator) {
            mIsAnimationRunning = true;
        }
    };

    private Animator.AnimatorListener mShowAnimListener = new AnimatorListenerAdapter() {

        @Override
        public void onAnimationCancel(Animator animator) {
            mIsAnimationRunning = false;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            mIsAnimationRunning = false;
        }

        @Override
        public void onAnimationStart(Animator animator) {
            mIsAnimationRunning = true;

            mMenuFrame.setAlpha(1.0f);
        }
    };

    public HomeMenuView(Context context) {
        this(context, null);
    }

    public HomeMenuView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public HomeMenuView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);

        mMenuItemMap = new HashMap<String, View>();
        mHitTestRect = new Rect();

        mMenuItemList = new MenuItem[] {
            new MenuItem(KEY_BUTTON_KISEKAE, R.drawable.menu_home_1),
            new MenuItem(KEY_BUTTON_THEME_HISTORY, R.drawable.menu_home_2),
            new MenuItem(KEY_BUTTON_WALLPAPER, R.drawable.menu_home_3),
            new MenuItem(KEY_BUTTON_WIDGET, R.drawable.menu_home_9),
            new MenuItem(KEY_BUTTON_SETTINGS, R.drawable.menu_home_6)
        };
    }

    public View getItemByKey(String key) {
        return mMenuItemMap.get(key);
    }

    public void hideMenuOrThemeHistory() {

        if (mShowHistory) {
            if (mThemeHistoryHideAnim == null) {
                hideThemeHistory(false, true);
            }
        } else {

            if (mSubMenuView != null) {
                closeSubMenu(false, true);
            } else {
                mPopup.cancel(true, DISMISS_ANI_DURATION);
            }
        }
    }

    public void hideMenu(boolean animate) {

        if (mShowHistory) {
            if (mThemeHistoryHideAnim == null) {
                hideThemeHistory(true, animate);
            }
        } else {
            if (mSubMenuView != null) {
                closeSubMenu(true, animate);
            } else {

                mPopup.cancel(animate, DISMISS_ANI_DURATION);
            }
        }
    }

    public PopupView makePopup(final PopupLayerView popupLayer) {

        mPopup = popupLayer.newPopup(this);
        setupAnimations(mPopup);
        mPopup.addListener(new PopupView.PopupListener() {

            @Override
            public void onShow() {
//                LauncherApplication.getInstance().registerActivityLifecycleCallbacks(activityLifeCycleCallback);
            }

            @Override
            public void onDismiss() {
//                LauncherApplication.getInstance().unregisterActivityLifecycleCallbacks(activityLifeCycleCallback);
            }

            @Override
            public void onCancelled() {
            }

            @Override
            public void onDismissRequested() {}
        });

        mPopup.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View view, int i, KeyEvent keyevent) {
                if(keyevent.getAction() == KeyEvent.ACTION_UP) {
                    if(i == KeyEvent.KEYCODE_MENU || i == KeyEvent.KEYCODE_BACK) {
                        hideMenuOrThemeHistory();
                        return true;
                    }
                }
                return false;
            }
        });

        mPopup.setLayerBackground(new ColorDrawable(Color.argb(156, 0, 0, 0)));

        return mPopup;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mMenuFrame = (FrameLayout) findViewById(R.id.menu_frame);

        mMenuItem1 = (RelativeLayout) findViewById(R.id.menu_item_1);
        mMenuItem2 = (RelativeLayout) findViewById(R.id.menu_item_2);
        mMenuItem3 = (RelativeLayout) findViewById(R.id.menu_item_3);
        mMenuItem4 = (RelativeLayout) findViewById(R.id.menu_item_4);
        mMenuItem5 = (RelativeLayout) findViewById(R.id.menu_item_5);

        setupMenuItems(this, mMenuItemList, mMenuItemMap);

        Iterator<View> itr = mMenuItemMap.values().iterator();
        while (itr.hasNext()) {
            ((itr.next()).findViewById(R.id.menu_item_icon)).setOnClickListener(internalMenuItemClickListener);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mIsAnimationRunning;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {

            int x = (int) event.getX();
            int y = (int) event.getY();

            mMenuFrame.getHitRect(mHitTestRect);
            if(!mHitTestRect.contains(x, y)) {

                if (mSubMenuView != null) {
                    closeSubMenu(false, true);
                    return false;
                }

                if (mShowHistory) {
                    if (mThemeHistoryHideAnim == null) {
                        hideThemeHistory(false, true);
                    }
                    return false;
                }

                mPopup.cancel(true, DISMISS_ANI_DURATION);
                return false;
            }
        }

        return super.onTouchEvent(event);
    }

    private void setupAnimations(PopupView popup) {

        // 表示時アニメーション設定
        ValueAnimator showTransAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        showTransAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            private int mHeight = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                if (mHeight == 0) {
                    mHeight = mMenuFrame.getHeight();
                }

                if (mHeight == 0) {
                    mMenuFrame.setTranslationY(9999);
                    return;
                }

                float t = (Float) animation.getAnimatedValue();
                mMenuFrame.setTranslationY(mHeight * (1 - t));
            }
        });
        showTransAnim.setInterpolator(new DecelerateInterpolator());
        showTransAnim.setDuration(SHOW_ANI_DURATION);

        popup.setOnShowAnimator(showTransAnim, mShowAnimListener);

        // 非表示時アニメーション設定
        mDismissTransAnim = ValueAnimator.ofFloat(1.0f, 0.0f);
        mDismissTransAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            private int mHeight = 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {

                if (mHeight == 0) {
                    mHeight = mMenuFrame.getMeasuredHeight();
                }

                if (mHeight == 0) {
                    mMenuFrame.setTranslationY(0);
                    return;
                }

                float t = (Float) animation.getAnimatedValue();

                mMenuFrame.setAlpha(t);

                mMenuFrame.setTranslationY(mHeight * (1 - t));
            }
        });
        mDismissTransAnim.setInterpolator(new AccelerateInterpolator());
        mDismissTransAnim.setDuration(DISMISS_ANI_DURATION);

        popup.setOnDismissAnimator(mDismissTransAnim, mDismissAnimListener);
    }

    private void setupMenuItems(ViewGroup viewgroup, MenuItem buttonitems[], Map<String, View> map) {
        for (int i = 0; i < MENU_ITEM_ID.length; i++) {
            View btnView = viewgroup.findViewById(MENU_ITEM_ID[i]);
            if(btnView == null) continue;
            setupMenuItem(btnView, buttonitems[i], map);
        }
    }

    private void setupMenuItem(View view, MenuItem buttonitem, Map<String, View> map) {
        String key = buttonitem.getKey();
        view.setTag(key);
        map.put(key, view);

        ImageView icView = (ImageView)view.findViewById(R.id.menu_item_icon);
        icView.setImageResource(buttonitem.getIconResId());
    }

    private static class MenuItem {

        int getIconResId() {
            return mIconResId;
        }

        String getKey() {
            return mKey;
        }

        public MenuItem(String key, int iconResId) {
            mKey = key;
            mIconResId = iconResId;
        }

        private final String mKey;
        private final int mIconResId;
    }
}
