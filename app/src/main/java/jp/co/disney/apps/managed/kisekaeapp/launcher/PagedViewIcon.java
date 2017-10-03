/*
 * Copyright (C) 2010 The Android Open Source Project
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
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;

/**
 * An icon on a PagedView, specifically for items in the launcher's paged view (with compound
 * drawables on the top).
 */
public class PagedViewIcon extends TextView {
    /** A simple callback interface to allow a PagedViewIcon to notify when it has been pressed */
    public static interface PressedCallback {
        void iconPressed(PagedViewIcon icon);
    }

    @SuppressWarnings("unused")
    private static final String TAG = "PagedViewIcon";
    private static final float PRESS_ALPHA = 0.4f;

    private PagedViewIcon.PressedCallback mPressedCallback;
    private boolean mLockDrawableState = false;

    private Bitmap mIcon;


    private int mIconPadTop = 0;
    private int mCellWidth;
    private int mIconSize;

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

    public PagedViewIcon(Context context) {
        this(context, null);
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setEllipsize(TextUtils.TruncateAt.END);
    }

    public void applyFromApplicationInfo(ApplicationInfo info, boolean scaleUp,
            PagedViewIcon.PressedCallback cb, int cellWidth, IconCache iconCache, float screenDensity) {
        mIcon = info.iconBitmap;
        mIconSize = mIcon.getWidth();
        mCellWidth = cellWidth;
        mScreenDensity = screenDensity;

        mPressedCallback = cb;
        setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(mIcon), null, null);

        mIconPadTop = (int) Math.ceil(8 * screenDensity);
        setPadding(0, mIconPadTop, 0, 0);

        ComponentName component = info.intent.getComponent();
        if (component != null) {
            BadgeInfo badgeInfo = iconCache.getBadgeInfoMap().get(component.getPackageName() + "/" + component.getClassName());
            if (badgeInfo != null) {
                setBadgeInfo(badgeInfo.getBadgeCount(), iconCache);
            }
        }

        setText(info.title);
        setTag(info);
    }

    public void lockDrawableState() {
        mLockDrawableState = true;
    }

    public void resetDrawableState() {
        mLockDrawableState = false;
        post(new Runnable() {
            @Override
            public void run() {
                refreshDrawableState();
            }
        });
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        // We keep in the pressed state until resetDrawableState() is called to reset the press
        // feedback
        if (isPressed()) {
            setAlpha(PRESS_ALPHA);
            if (mPressedCallback != null) {
                mPressedCallback.iconPressed(this);
            }
        } else if (!mLockDrawableState) {
            setAlpha(1f);
        }
    }

    @Override
    public void draw(Canvas canvas) {

        super.draw(canvas);

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
}
