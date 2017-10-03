/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.system.gesture.CheckLongPressHelper;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.HolographicOutlineHelper;

/**
 * TextView that draws a bubble behind the text. We cannot use a LineBackgroundSpan
 * because we want to make the bubble taller than the text and TextView's clip is
 * too aggressive.
 */
public class BubbleTextView extends TextView {
    static final float CORNER_RADIUS = 4.0f;
    static final float SHADOW_LARGE_RADIUS = 4.0f;
    static final float SHADOW_SMALL_RADIUS = 1.75f;
    static final float SHADOW_Y_OFFSET = 2.0f;
    static final int SHADOW_LARGE_COLOUR = 0xDD000000;
    static final int SHADOW_SMALL_COLOUR = 0xCC000000;
    static final float PADDING_H = 8.0f;
    static final float PADDING_V = 3.0f;

    private int mPrevAlpha = -1;

    private final HolographicOutlineHelper mOutlineHelper = new HolographicOutlineHelper();
    private final Canvas mTempCanvas = new Canvas();
    private final Rect mTempRect = new Rect();
    private boolean mDidInvalidateForPressedState;
    private Bitmap mPressedOrFocusedBackground;
    private int mFocusedOutlineColor;
    private int mFocusedGlowColor;
    private int mPressedOutlineColor;
    private int mPressedGlowColor;

    private boolean mBackgroundSizeChanged;
    private Drawable mBackground;

    private boolean mStayPressed;
    private CheckLongPressHelper mLongPressHelper;

    private int mCellWidth;
    private int mIconSize;
    private int mIconPadTop;
    private int mTextPad;

    private int mWorkspaceTextColor;

    private float mScreenDensity;

    // 描画用
    private int mBadgeCount = -1;
    private boolean mBadgeDspFlg = false;
    private Drawable mBadgeBg;

    private int mBadgeWidth;
    private int mBadgeHeight;
    private int mBadgeBgLeft;
    private int mBadgeBgTop;
    private int mBadgeLeft;
    private int mBadgeTop;

    private String mBadgeCountStr;

    private Paint mPaint = new Paint();

    public void updateBadge(IconCache iconCache) {

        ShortcutInfo info = (ShortcutInfo) getTag();
        ComponentName component = info.intent.getComponent();
        if (component != null) {
            BadgeInfo badgeInfo = iconCache.getBadgeInfoMap().get(component.getPackageName() + "/" + component.getClassName());
            if (badgeInfo != null) {
                setBadgeInfo(badgeInfo.getBadgeCount(), iconCache);
            }
        }
    }

    public void updateBadgeLayout() {

        mBadgeHeight = (int) Math.round(72 / 3.0f * mScreenDensity);

        mPaint.setColor(Color.WHITE);
        // mPaint.setStyle(Style.FILL);
        float textSize = mBadgeHeight / 2.0f;
        mPaint.setTextSize(textSize);
        mPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        final int badgeMargin = (int) Math.floor(mBadgeHeight / 3.0f);
        mBadgeBgTop = mIconPadTop - badgeMargin;

        final int badgeCount = mBadgeCount;
        final int badgeLeftCorre;
        if (badgeCount < 10) {
            mBadgeWidth = mBadgeHeight;
            badgeLeftCorre = (int) Math.floor(textSize / 3.5f);
        } else if (badgeCount < 100){
            mBadgeWidth = (int) Math.floor(mBadgeHeight * 1.2f);
            badgeLeftCorre = (int) Math.floor(textSize / 2.6f);
        } else if (badgeCount < 1000) {
            mBadgeWidth = (int) Math.floor(mBadgeHeight * 1.5f);
            badgeLeftCorre = (int) Math.floor(textSize / 2.6f);
        } else {
            mBadgeWidth = (int) Math.floor(mBadgeHeight * 1.8f);
            badgeLeftCorre = (int) Math.floor(textSize / 2.6f);
        }

        mBadgeBgLeft = mCellWidth - ((mCellWidth - mIconSize) / 2) - 1 + badgeMargin - mBadgeWidth;
        mBadgeLeft = mBadgeBgLeft + (mBadgeHeight / 2) - badgeLeftCorre;
        mBadgeTop = mBadgeBgTop + (mBadgeHeight / 2) + (int) Math.floor(textSize / 4.0f);

        invalidate();
    }

    public void setBadgeInfo(int badgeCount, IconCache iconCache) {

        mBadgeBg = iconCache.getBadgeBackground();

        mBadgeCount = badgeCount;

        if (badgeCount < 1000) {
            mBadgeCountStr = String.valueOf(badgeCount);
        } else {
            mBadgeCountStr = "999+";
        }

        if (badgeCount > 0) {
            mBadgeDspFlg = true;
        } else {
            mBadgeDspFlg = false;
        }

        updateBadgeLayout();
    }

    public int getIconSize() {
        return mIconSize;
    }

    public int getIconPaddingTop() {
        return mIconPadTop;
    }

    public int getTextPadding() {
        return mTextPad;
    }

    public BubbleTextView(Context context) {
        super(context);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mLongPressHelper = new CheckLongPressHelper(this);
        mBackground = getBackground();

//        final Resources res = getResources();
        mFocusedOutlineColor = mFocusedGlowColor = mPressedOutlineColor = mPressedGlowColor =
            ContextCompat.getColor(getContext(), android.R.color.white);
        mWorkspaceTextColor = ContextCompat.getColor(getContext(), R.color.workspace_icon_text_color);

        setEllipsize(TextUtils.TruncateAt.END);

        setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache,
            int cellWidth, int icSize, int paddingTop, int textPadding, float screenDensity) {
        Bitmap b = info.getIcon(iconCache);

        mScreenDensity = screenDensity;

        FastBitmapDrawable d = new FastBitmapDrawable(b);
        setShortcutLayout(d, cellWidth, icSize, paddingTop, textPadding);
        setCompoundDrawables(null, d, null, null);

        ComponentName component = info.intent.getComponent();
        if (component != null) {
            BadgeInfo badgeInfo = iconCache.getBadgeInfoMap().get(component.getPackageName() + "/" + component.getClassName());
            if (badgeInfo != null) {
                setBadgeInfo(badgeInfo.getBadgeCount(), iconCache);
            }
        }

        if (info.shortcutName != null) {
            setText(info.shortcutName);
        } else {
            setText(info.title);
        }
        setTag(info);
    }

    public void updateShortcutName() {

        ShortcutInfo info = (ShortcutInfo) getTag();

        if (info.shortcutName != null) {
            setText(info.shortcutName);
        } else {
            setText(info.title);
        }
    }

    public void setShortcutLayout(int cellWidth, int icSize, int paddingTop, int textPadding) {
        final Drawable[] ds = getCompoundDrawables();
        if (ds[1] == null) return;

        setShortcutLayout(ds[1], cellWidth, icSize, paddingTop, textPadding);
    }

    public void setShortcutLayout(Drawable d, int cellWidth, int icSize, int paddingTop, int textPadding) {

        mCellWidth = cellWidth;
        mIconSize = icSize;
        mIconPadTop = paddingTop;
        mTextPad = textPadding;

//        mBadgeWidth = Math.round(mIconSize / 2.0f * 1.2f);
//        mBadgeHeight = Math.round(mIconSize / 2.0f);

        d.setBounds(0, 0, icSize, icSize);
        setPadding(paddingTop, paddingTop, paddingTop, 0);

//        // 描画アイコンサイズ変更による影響を補正
//        final int drawableWidth = d.getIntrinsicWidth();
//        int drawablePadding = (int) Math.ceil(textPadding * drawableWidth / (float) icSize);

        setCompoundDrawablePadding(textPadding);

        updateBadgeLayout();
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground || super.verifyDrawable(who);
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    protected void drawableStateChanged() {
        if (isPressed()) {
            // In this case, we have already created the pressed outline on ACTION_DOWN,
            // so we just need to do an invalidate to trigger draw
            if (!mDidInvalidateForPressedState) {
                setCellLayoutPressedOrFocusedIcon();
            }
        } else {
            // Otherwise, either clear the pressed/focused background, or create a background
            // for the focused state
            final boolean backgroundEmptyBefore = mPressedOrFocusedBackground == null;
            if (!mStayPressed) {
                mPressedOrFocusedBackground = null;
            }
            if (isFocused()) {
                if (getLayout() == null) {
                    // In some cases, we get focus before we have been layed out. Set the
                    // background to null so that it will get created when the view is drawn.
                    mPressedOrFocusedBackground = null;
                } else {
                    mPressedOrFocusedBackground = createGlowingOutline(
                            mTempCanvas, mFocusedGlowColor, mFocusedOutlineColor);
                }
                mStayPressed = false;
                setCellLayoutPressedOrFocusedIcon();
            }
            final boolean backgroundEmptyNow = mPressedOrFocusedBackground == null;
            if (!backgroundEmptyBefore && backgroundEmptyNow) {
                setCellLayoutPressedOrFocusedIcon();
            }
        }

        Drawable d = mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        super.drawableStateChanged();
    }

    /**
     * Draw this BubbleTextView into the given Canvas.
     *
     * @param destCanvas the canvas to draw on
     * @param padding the horizontal and vertical padding to use when drawing
     */
    private void drawWithPadding(Canvas destCanvas, int padding) {
        final Rect clipRect = mTempRect;
        getDrawingRect(clipRect);

        // adjust the clip rect so that we don't include the text label
        clipRect.bottom =
            getExtendedPaddingTop() - (int) BubbleTextView.PADDING_V + getLayout().getLineTop(0);

        // Draw the View into the bitmap.
        // The translate of scrollX and scrollY is necessary when drawing TextViews, because
        // they set scrollX and scrollY to large values to achieve centered text
        destCanvas.save();
        destCanvas.scale(getScaleX(), getScaleY(),
                (getWidth() + padding) / 2, (getHeight() + padding) / 2);
        destCanvas.translate(-getScrollX() + padding / 2, -getScrollY() + padding / 2);
        destCanvas.clipRect(clipRect, Op.REPLACE);
        draw(destCanvas);
        destCanvas.restore();
    }

    /**
     * Returns a new bitmap to be used as the object outline, e.g. to visualize the drop location.
     * Responsibility for the bitmap is transferred to the caller.
     */
    private Bitmap createGlowingOutline(Canvas canvas, int outlineColor, int glowColor) {
        final int padding = HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS;
        final Bitmap b = Bitmap.createBitmap(
                getWidth() + padding, getHeight() + padding, Bitmap.Config.ARGB_8888);

        canvas.setBitmap(b);
        drawWithPadding(canvas, padding);
        mOutlineHelper.applyExtraThickExpensiveOutlineWithBlur(b, canvas, glowColor, outlineColor);
        canvas.setBitmap(null);

        return b;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                // getExtendedPaddingTopは呼ぶタイミングによっては安全ではなく、
                // NullPointerExceptionが発生する可能性がある。
                // (新しいAndroidのバージョンでは対応されている。)
                // createGlowingOutlineを呼ぶ前に、getExtendedPaddingTopを呼んでも安全なことを確実にしておく。
                try {
                    getExtendedPaddingTop();
                } catch (NullPointerException e) {
                    return true;
                }

                // So that the pressed outline is visible immediately when isPressed() is true,
                // we pre-create it on ACTION_DOWN (it takes a small but perceptible amount of time
                // to create it)
                if (mPressedOrFocusedBackground == null) {
                    mPressedOrFocusedBackground = createGlowingOutline(
                            mTempCanvas, mPressedGlowColor, mPressedOutlineColor);
                }
                // Invalidate so the pressed state is visible, or set a flag so we know that we
                // have to call invalidate as soon as the state is "pressed"
                if (isPressed()) {
                    mDidInvalidateForPressedState = true;
                    setCellLayoutPressedOrFocusedIcon();
                } else {
                    mDidInvalidateForPressedState = false;
                }

                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                // If we've touched down and up on an item, and it's still not "pressed", then
                // destroy the pressed outline
                if (!isPressed()) {
                    mPressedOrFocusedBackground = null;
                }

                mLongPressHelper.cancelLongPress();
                break;
        }
        return result;
    }

    void setStayPressed(boolean stayPressed) {
        mStayPressed = stayPressed;
        if (!stayPressed) {
            mPressedOrFocusedBackground = null;
        }
        setCellLayoutPressedOrFocusedIcon();
    }

    void setCellLayoutPressedOrFocusedIcon() {
        if (getParent() instanceof ShortcutAndWidgetContainer) {
            ShortcutAndWidgetContainer parent = (ShortcutAndWidgetContainer) getParent();
            if (parent != null) {
                CellLayout layout = (CellLayout) parent.getParent();
                layout.setPressedOrFocusedIcon((mPressedOrFocusedBackground != null) ? this : null);
            }
        }
    }

    void clearPressedOrFocusedBackground() {
        mPressedOrFocusedBackground = null;
        setCellLayoutPressedOrFocusedIcon();
    }

    Bitmap getPressedOrFocusedBackground() {
        return mPressedOrFocusedBackground;
    }

    int getPressedOrFocusedBackgroundPadding() {
        return HolographicOutlineHelper.MAX_OUTER_BLUR_RADIUS / 2;
    }

    @Override
    public void draw(Canvas canvas) {
        final Drawable background = mBackground;
        if (background != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            if (mBackgroundSizeChanged) {
                background.setBounds(0, 0,  getRight() - getLeft(), getBottom() - getTop());
                mBackgroundSizeChanged = false;
            }

            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);

            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }

        // フォルダアイコンのプレビュー描画でBoundsが更新されるので、
        // onDrawの度に再設定する。
        Drawable[] ds = getCompoundDrawables();
        if (ds != null && ds[1] != null) {
            ds[1].setBounds(0, 0, mIconSize, mIconSize);
        }

        if (getCurrentTextColor() != mWorkspaceTextColor) {
            getPaint().clearShadowLayer();
            super.draw(canvas);

        } else {

            // シャドウを2回描画
            getPaint().setShadowLayer(SHADOW_LARGE_RADIUS, 0.0f, SHADOW_Y_OFFSET, SHADOW_LARGE_COLOUR);
            super.draw(canvas);
            canvas.save(Canvas.CLIP_SAVE_FLAG);
            canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(),
                    getScrollX() + getWidth(),
                    getScrollY() + getHeight(), Region.Op.INTERSECT);
            getPaint().setShadowLayer(SHADOW_SMALL_RADIUS, 0.0f, 0.0f, SHADOW_SMALL_COLOUR);
            super.draw(canvas);
            canvas.restore();
        }

        // バッジ描画
        final Drawable badgeBg = mBadgeBg;
        if (mBadgeDspFlg && badgeBg != null) {

            final int scrollX = getScrollX();
            final int scrollY = getScrollY();

            badgeBg.setBounds(mBadgeBgLeft, mBadgeBgTop, mBadgeBgLeft + mBadgeWidth, mBadgeBgTop + mBadgeHeight);

            if ((scrollX | scrollY) == 0) {
                badgeBg.draw(canvas);
                canvas.drawText(mBadgeCountStr, mBadgeLeft, mBadgeTop, mPaint);
            } else {
                canvas.translate(scrollX, scrollY);
                badgeBg.draw(canvas);
                canvas.drawText(mBadgeCountStr, mBadgeLeft, mBadgeTop, mPaint);
                canvas.translate(-scrollX, -scrollY);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mBackground != null) mBackground.setCallback(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBackground != null) mBackground.setCallback(null);
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        if (mPrevAlpha != alpha) {
            mPrevAlpha = alpha;
            super.onSetAlpha(alpha);
        }
        return true;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }
}
