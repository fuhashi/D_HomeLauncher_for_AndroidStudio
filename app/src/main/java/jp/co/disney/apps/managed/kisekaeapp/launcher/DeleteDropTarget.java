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

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class DeleteDropTarget extends ButtonDropTarget {
//    private static int DELETE_ANIMATION_DURATION = 285;
    private static int DELETE_ANIMATION_DURATION = 430;

    private BitmapDrawable mTrashCanBody;
    private BitmapDrawable mTrashCanCap;

    private ValueAnimator trashCanOpenAnim;
    private ValueAnimator trashCanCloseAnim;

    private float mCapSlope = 0f;
    private float mTranslate = 0f;

    public DeleteDropTarget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeleteDropTarget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public int getStaticHeight() {
        return mTrashCanBody.getBitmap().getHeight();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the hover color
        final Context context = getContext();
        mHoverColor = ContextCompat.getColor(context, R.color.delete_target_hover_tint);

        mTrashCanBody = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.ic_tb_white);
        mTrashCanCap = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.ic_tb_white_cover);

        trashCanOpenAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        trashCanOpenAnim.setDuration(250);
        trashCanOpenAnim.setInterpolator(new DecelerateInterpolator());
        trashCanOpenAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                mCapSlope = 20 * t;
                mTranslate = t;
                invalidate();
            }
        });

        trashCanCloseAnim = ValueAnimator.ofFloat(1.0f, 0.0f);
        trashCanCloseAnim.setDuration(250);
        trashCanCloseAnim.setInterpolator(new AccelerateInterpolator());
        trashCanCloseAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                mCapSlope = 20 * t;
                mTranslate = t;
                invalidate();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = mTrashCanBody.getBitmap().getWidth();
        int height = mTrashCanBody.getBitmap().getHeight();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int bodyWidth = canvas.getWidth();
        final int bodyHeight = canvas.getHeight();

        mTrashCanBody.setBounds(0, 0, bodyWidth, bodyHeight);
        mTrashCanBody.draw(canvas);

        int capWidth = mTrashCanCap.getBitmap().getWidth();
        int capHeight = mTrashCanCap.getBitmap().getHeight();

        int left = (bodyWidth - capWidth) / 2;
        int top = 51 * bodyHeight / 198;

        int saveCount = canvas.save();

        canvas.rotate(-mCapSlope, left, top + capHeight);
//        canvas.translate(mTranslate * -7, mTranslate * 7);

        canvas.clipRect(left, top, left + capWidth, top + capHeight);

        mTrashCanCap.setBounds(left, top, left + capWidth, top + capHeight);
        mTrashCanCap.draw(canvas);

        canvas.restoreToCount(saveCount);
    }

    Rect getIconRect(int viewWidth, int viewHeight, int drawableWidth, int drawableHeight) {
        DragLayer dragLayer = mLauncher.getDragLayer();

        // Find the rect to animate to (the view is center aligned)
        Rect to = new Rect();
        dragLayer.getViewRectRelativeToSelf(this, to);

        final int width = drawableWidth;
        final int height = drawableHeight;

        final int left = to.left;
        final int right = left + width;

        final int top = to.top;
        final int bottom = top +  height;

        to.set(left, top, right, bottom);

        // Center the destination rect about the trash icon
        final int xOffset = (int) -(viewWidth - width) / 2;
        final int yOffset = (int) -(viewHeight - height) / 2;
        to.offset(xOffset, yOffset);

        return to;
    }

    private boolean isAllAppsApplication(DragSource source, Object info) {
        return (source instanceof AppsCustomizePagedView) && (info instanceof ApplicationInfo);
    }
    private boolean isAllAppsWidget(DragSource source, Object info) {
        if (source instanceof AppsCustomizePagedView) {
            if (info instanceof PendingAddItemInfo) {
                PendingAddItemInfo addInfo = (PendingAddItemInfo) info;
                switch (addInfo.itemType) {
                    case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                    case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
                        return true;
                }
            }
        }
        return false;
    }
    private boolean isDragSourceWorkspaceOrFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) || (d.dragSource instanceof Folder);
    }
    private boolean isWorkspaceOrFolderApplication(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof ShortcutInfo);
    }
    private boolean isWorkspaceOrFolderWidget(DragObject d) {
        return isDragSourceWorkspaceOrFolder(d) && (d.dragInfo instanceof LauncherAppWidgetInfo);
    }
    private boolean isWorkspaceFolder(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof FolderInfo);
    }
    private boolean isWorkspaceDummyItem(DragObject d) {
        return (d.dragSource instanceof Workspace) && (d.dragInfo instanceof DummyInfo);
    }

    private void setHoverState() {

        if (trashCanCloseAnim.isStarted()) {
            trashCanCloseAnim.cancel();
        }
        if (!trashCanOpenAnim.isStarted()) {
            trashCanOpenAnim.start();
        }
    }
    private void resetHoverState(boolean animated) {

        if (trashCanOpenAnim.isStarted()) {
            trashCanOpenAnim.cancel();
        }

        if (animated) {
            if (!trashCanCloseAnim.isStarted()) {
                trashCanCloseAnim.start();
            }
        } else {
            mCapSlope = 0;
            mTranslate = 0;
            invalidate();
        }
    }

    @Override
    public boolean acceptDrop(DragObject d) {
        // We can remove everything including App shortcuts, folders, widgets, etc.
        return true;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        boolean isVisible = true;

        // If we are dragging a widget from AppsCustomize, hide the delete target
        if (isAllAppsWidget(source, info)) {
            isVisible = false;
        }

        if (isAllAppsApplication(source, info)) {
            isVisible = false;
        }

        mActive = isVisible;
        resetHoverState(false);
        ((ViewGroup) getParent()).setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDragEnd() {
        super.onDragEnd();
        mActive = false;
    }

    public void onDragEnter(DragObject d) {
//        super.onDragEnter(d);

        d.dragView.setAlpha(0.6f);
        setHoverState();
    }

    public void onDragExit(DragObject d) {
//        super.onDragExit(d);

        if (!d.dragComplete) {
            d.dragView.setAlpha(1f);
            resetHoverState(true);
        } else {
//            // Restore the hover color if we are deleting
//            d.dragView.setColor(mHoverColor);
        }
    }

    private void animateToTrashAndCompleteDrop(final DragObject d) {

        DragLayer dragLayer = mLauncher.getDragLayer();
        Rect from = new Rect();
        dragLayer.getViewRectRelativeToSelf(d.dragView, from);

//        Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
//                mCurrentDrawable.getIntrinsicWidth(), mCurrentDrawable.getIntrinsicHeight());
        Rect to = getIconRect(d.dragView.getMeasuredWidth(), d.dragView.getMeasuredHeight(),
                mTrashCanBody.getIntrinsicWidth(), mTrashCanBody.getIntrinsicHeight());
        float scale = (float) to.width() / from.width();

        mSearchDropTargetBar.deferOnDragEnd();
        Runnable onAnimationEndRunnable = new Runnable() {
            @Override
            public void run() {
                mSearchDropTargetBar.onDragEnd();
                mLauncher.exitSpringLoadedDragMode();
                completeDrop(d);
            }
        };

        float finalAlpha;
        if (Float.compare(scale, 1f) > 0) {
            finalAlpha = d.dragView.getAlpha() / scale;
        } else {
            finalAlpha = d.dragView.getAlpha() * scale;
        }
        dragLayer.animateView(d.dragView, from, to, finalAlpha, 1f, 1f, 0.1f, 0.1f,
                DELETE_ANIMATION_DURATION, new DecelerateInterpolator(2),
                new LinearInterpolator(), onAnimationEndRunnable,
                DragLayer.ANIMATION_END_DISAPPEAR, null);
    }

    private void completeDrop(DragObject d) {
        ItemInfo item = (ItemInfo) d.dragInfo;

        if (isWorkspaceOrFolderApplication(d)) {
            LauncherModel.deleteItemFromDatabase(mLauncher, item);
        } else if (isWorkspaceFolder(d)) {
            // Remove the folder from the workspace and delete the contents from launcher model
            FolderInfo folderInfo = (FolderInfo) item;
            mLauncher.removeFolder(folderInfo);
            LauncherModel.deleteFolderContentsFromDatabase(mLauncher, folderInfo);
        } else if (isWorkspaceOrFolderWidget(d)) {
            // Remove the widget from the workspace
            mLauncher.removeAppWidget((LauncherAppWidgetInfo) item);
            LauncherModel.deleteItemFromDatabase(mLauncher, item);

            final LauncherAppWidgetInfo launcherAppWidgetInfo = (LauncherAppWidgetInfo) item;
            final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
            if (appWidgetHost != null) {
                // Deleting an app widget ID is a void call but writes to disk before returning
                // to the caller...
                new Thread("deleteAppWidgetId") {
                    public void run() {
                        appWidgetHost.deleteAppWidgetId(launcherAppWidgetInfo.appWidgetId);
                    }
                }.start();
            }
        } else if (isWorkspaceDummyItem(d)) {
            LauncherModel.deleteItemFromDatabase(mLauncher, item);
        }
    }

    public void onDrop(DragObject d) {
        animateToTrashAndCompleteDrop(d);
    }
}
