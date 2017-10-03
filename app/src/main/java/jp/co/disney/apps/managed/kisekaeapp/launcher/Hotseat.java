/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class Hotseat extends FrameLayout {
    @SuppressWarnings("unused")
    private static final String TAG = "Hotseat";

    private Launcher mLauncher;
    private CellLayout mContent;

    private int mAllAppsButtonRank;

    public Hotseat(Context context) {
        this(context, null);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAllAppsButtonRank = 2 * Workspace.ICON_SPAN;
    }

    public void setup(Launcher launcher) {
        mLauncher = launcher;
        setOnKeyListener(new HotseatIconKeyEventListener());
    }

    CellLayout getLayout() {
        return mContent;
    }

    public boolean isAllAppsButtonRank(int rank) {
        return rank == mAllAppsButtonRank;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = (CellLayout) findViewById(R.id.layout);
        resetLayout();
    }

    public int getContentHeight() {
        return mContent.getCellHeight();
    }

    void resetLayout() {
        mContent.removeAllViewsInLayout();

        // Add the Apps button
        Context context = getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        BubbleTextView drawerButton = (BubbleTextView)
                inflater.inflate(R.layout.application, mContent, false);

        drawerButton.setContentDescription(context.getString(R.string.all_apps_button_label));
        drawerButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLauncher != null &&
                    (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN) {
                    mLauncher.onTouchDownAllAppsButton(v);
                }
                return false;
            }
        });

        drawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(android.view.View v) {
                if (mLauncher != null) {
                    mLauncher.onClickAllAppsButton(v);
                }
            }
        });

        CellLayout.LayoutParams lp = new CellLayout.LayoutParams(mAllAppsButtonRank, 0, Workspace.ICON_SPAN, Workspace.ICON_SPAN);
        lp.canReorder = false;
        mContent.addViewToCellLayout(drawerButton, -1, 0, lp, true);
    }

    void setDrawerIcon(Bitmap drawerIconBmp) {

        BubbleTextView drawerButton = (BubbleTextView) mContent.getChildAt(mAllAppsButtonRank, 0);

        Drawable[] oldDrawerIcons = drawerButton.getCompoundDrawables();

        Drawable drawerIcon = new BitmapDrawable(getContext().getResources(), drawerIconBmp);
        drawerButton.setShortcutLayout(drawerIcon, mContent.getCellWidth() * 2, mContent.getIconSize(), mContent.getCellPaddingTop(), mContent.getTextPadding());
        drawerButton.setCompoundDrawables(null, drawerIcon, null, null);

        if (oldDrawerIcons[1] != null) {
            BitmapDrawable oldDrawerIcon = ((BitmapDrawable) oldDrawerIcons[1]);
            Bitmap oldDrawerIconBmp = oldDrawerIcon.getBitmap();
            if (oldDrawerIconBmp != null) {
                oldDrawerIconBmp.recycle();
            }
        }
    }
}
