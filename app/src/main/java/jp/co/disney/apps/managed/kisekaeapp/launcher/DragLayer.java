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

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.BitmapUtils;

/**
 * A ViewGroup that coordinates dragging across its descendants
 */
public class DragLayer extends FrameLayout implements ViewGroup.OnHierarchyChangeListener {
    private DragController mDragController;
    private int[] mTmpXY = new int[2];

    private int mXDown, mYDown;
    private Launcher mLauncher;

    // Variables relating to resizing widgets
    private final ArrayList<AppWidgetResizeFrame> mResizeFrames =
            new ArrayList<AppWidgetResizeFrame>();
    private AppWidgetResizeFrame mCurrentResizeFrame;

    private ContextMenuView mContextMenuView;

    // Variables relating to animation of views after drop
    private ValueAnimator mDropAnim = null;
    private ValueAnimator mFadeOutAnim = null;
    private TimeInterpolator mCubicEaseOutInterpolator = new DecelerateInterpolator(1.5f);
    private DragView mDropView = null;
    private int mAnchorViewInitialScrollX = 0;
    private View mAnchorView = null;

    private boolean mHoverPointClosesFolder = false;
    private Rect mHitRect = new Rect();
    private int mWorkspaceIndex = -1;
    private int mQsbIndex = -1;
    public static final int ANIMATION_END_DISAPPEAR = 0;
    public static final int ANIMATION_END_FADE_OUT = 1;
    public static final int ANIMATION_END_REMAIN_VISIBLE = 2;

    ContextMenuView getContextMenuView() {
        return mContextMenuView;
    }

    NameEditView getNameEditView() {
        return mNameEditView;
    }

    /**
     * Used to create a new DragLayer from XML.
     *
     * @param context The application's context.
     * @param attrs The attributes set containing the Workspace's customization values.
     */
    public DragLayer(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Disable multitouch across the workspace/all apps/customize tray
        setMotionEventSplittingEnabled(false);
        setChildrenDrawingOrderEnabled(true);
        setOnHierarchyChangeListener(this);

        mLeftHoverDrawable = ContextCompat.getDrawable(context, R.drawable.page_hover_left_holo);
        mRightHoverDrawable = ContextCompat.getDrawable(context, R.drawable.page_hover_right_holo);
    }

    public void setup(Launcher launcher, DragController controller) {
        mLauncher = launcher;
        mDragController = controller;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mDragController.dispatchKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    private boolean isEventOverFolderTextRegion(Folder folder, MotionEvent ev) {
        getDescendantRectRelativeToSelf(folder.getEditTextRegion(), mHitRect);
        if (mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
            return true;
        }
        return false;
    }

    private boolean isEventOverNameEditTextRegion(NameEditView nameEditView, MotionEvent ev) {
        getDescendantRectRelativeToSelf(nameEditView.getNameEditText(), mHitRect);
        if (mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
            return true;
        }
        return false;
    }

    private boolean isEventOverFolder(Folder folder, MotionEvent ev) {
        getDescendantRectRelativeToSelf(folder, mHitRect);
        if (mHitRect.contains((int) ev.getX(), (int) ev.getY())) {
            return true;
        }
        return false;
    }

    private boolean handleTouchDown(MotionEvent ev, boolean intercept) {
        Rect hitRect = new Rect();
        int x = (int) ev.getX();
        int y = (int) ev.getY();

        for (AppWidgetResizeFrame child: mResizeFrames) {
            child.getHitRect(hitRect);
            if (hitRect.contains(x, y)) {
                if (child.beginResizeIfPointInRegion(x - child.getLeft(), y - child.getTop())) {
                    mCurrentResizeFrame = child;
                    mXDown = x;
                    mYDown = y;
                    requestDisallowInterceptTouchEvent(true);
                    return true;
                }
            }
        }

        Folder currentFolder = mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder != null && intercept) {
            if (currentFolder.isEditingName()) {
                if (!isEventOverFolderTextRegion(currentFolder, ev)) {
                    currentFolder.dismissEditingName();
                    return true;
                }
            }

            getDescendantRectRelativeToSelf(currentFolder, hitRect);
            if (!isEventOverFolder(currentFolder, ev)) {
                mLauncher.closeFolder();
                return true;
            }
        }

        if (mContextMenuView != null && intercept) {
            mContextMenuView.getHitRect(hitRect);
            if (!hitRect.contains(x, y)) {
                closeContextMenu();
                return true;
            }
        }

        if (mNameEditView != null && intercept) {

            if (mNameEditView.isEditingName()) {
                if (!isEventOverNameEditTextRegion(mNameEditView, ev)) {
                    mNameEditView.dismissEditingName();
                    return true;
                }
            }

            mNameEditView.getHitRect(hitRect);
            if (!hitRect.contains(x, y)) {
                closeNameEditView();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (handleTouchDown(ev, true)) {
                return true;
            }
        }
        clearAllResizeFrames();
//        closeContextMenu();
        return mDragController.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onInterceptHoverEvent(MotionEvent ev) {
        if (mLauncher == null || mLauncher.getWorkspace() == null) {
            return false;
        }
        Folder currentFolder = mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder == null) {
            return false;
        } else {
                AccessibilityManager accessibilityManager = (AccessibilityManager)
                        getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (accessibilityManager.isTouchExplorationEnabled()) {
                final int action = ev.getAction();
                boolean isOverFolder;
                switch (action) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        isOverFolder = isEventOverFolder(currentFolder, ev);
                        if (!isOverFolder) {
                            sendTapOutsideFolderAccessibilityEvent(currentFolder.isEditingName());
                            mHoverPointClosesFolder = true;
                            return true;
                        } else if (isOverFolder) {
                            mHoverPointClosesFolder = false;
                        } else {
                            return true;
                        }
                    case MotionEvent.ACTION_HOVER_MOVE:
                        isOverFolder = isEventOverFolder(currentFolder, ev);
                        if (!isOverFolder && !mHoverPointClosesFolder) {
                            sendTapOutsideFolderAccessibilityEvent(currentFolder.isEditingName());
                            mHoverPointClosesFolder = true;
                            return true;
                        } else if (isOverFolder) {
                            mHoverPointClosesFolder = false;
                        } else {
                            return true;
                        }
                }
            }
        }
        return false;
    }

    private void sendTapOutsideFolderAccessibilityEvent(boolean isEditingName) {
        AccessibilityManager accessibilityManager = (AccessibilityManager)
                getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager.isEnabled()) {
            int stringId = isEditingName ? R.string.folder_tap_to_rename : R.string.folder_tap_to_close;
            AccessibilityEvent event = AccessibilityEvent.obtain(
                    AccessibilityEvent.TYPE_VIEW_FOCUSED);
            onInitializeAccessibilityEvent(event);
            event.getText().add(getContext().getString(stringId));
            accessibilityManager.sendAccessibilityEvent(event);
        }
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        Folder currentFolder = mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder != null) {
            if (child == currentFolder) {
                return super.onRequestSendAccessibilityEvent(child, event);
            }
            // Skip propagating onRequestSendAccessibilityEvent all for other children
            // when a folder is open
            return false;
        }
        return super.onRequestSendAccessibilityEvent(child, event);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void addChildrenForAccessibility(ArrayList<View> childrenForAccessibility) {
        Folder currentFolder = mLauncher.getWorkspace().getOpenFolder();
        if (currentFolder != null) {
            // Only add the folder as a child for accessibility when it is open
            childrenForAccessibility.add(currentFolder);
        } else {
            super.addChildrenForAccessibility(childrenForAccessibility);
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        // If we've received this, we've already done the necessary handling
        // in onInterceptHoverEvent. Return true to consume the event.
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean handled = false;
        int action = ev.getAction();

        int x = (int) ev.getX();
        int y = (int) ev.getY();

        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (ev.getAction() == MotionEvent.ACTION_DOWN) {
                if (handleTouchDown(ev, false)) {
                    return true;
                }
            }
        }

        if (mCurrentResizeFrame != null) {
            handled = true;
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    mCurrentResizeFrame.visualizeResizeForDelta(x - mXDown, y - mYDown);
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    mCurrentResizeFrame.visualizeResizeForDelta(x - mXDown, y - mYDown);
                    mCurrentResizeFrame.onTouchUp();
                    mCurrentResizeFrame = null;
            }
        }
        if (handled) return true;
        return mDragController.onTouchEvent(ev);
    }

    /**
     * Determine the rect of the descendant in this DragLayer's coordinates
     *
     * @param descendant The descendant whose coordinates we want to find.
     * @param r The rect into which to place the results.
     * @return The factor by which this descendant is scaled relative to this DragLayer.
     */
    public float getDescendantRectRelativeToSelf(View descendant, Rect r) {
        mTmpXY[0] = 0;
        mTmpXY[1] = 0;
        float scale = getDescendantCoordRelativeToSelf(descendant, mTmpXY);
        r.set(mTmpXY[0], mTmpXY[1],
                mTmpXY[0] + descendant.getWidth(), mTmpXY[1] + descendant.getHeight());
        return scale;
    }

    public float getLocationInDragLayer(View child, int[] loc) {
        loc[0] = 0;
        loc[1] = 0;
        return getDescendantCoordRelativeToSelf(child, loc);
    }

    /**
     * Given a coordinate relative to the descendant, find the coordinate in this DragLayer's
     * coordinates.
     *
     * @param descendant The descendant to which the passed coordinate is relative.
     * @param coord The coordinate that we want mapped.
     * @return The factor by which this descendant is scaled relative to this DragLayer. Caution
     *         this scale factor is assumed to be equal in X and Y, and so if at any point this
     *         assumption fails, we will need to return a pair of scale factors.
     */
    public float getDescendantCoordRelativeToSelf(View descendant, int[] coord) {
        float scale = 1.0f;
        float[] pt = {coord[0], coord[1]};
        descendant.getMatrix().mapPoints(pt);
        scale *= descendant.getScaleX();
        pt[0] += descendant.getLeft();
        pt[1] += descendant.getTop();
        ViewParent viewParent = descendant.getParent();
        while (viewParent instanceof View && viewParent != this) {
            final View view = (View)viewParent;
            view.getMatrix().mapPoints(pt);
            scale *= view.getScaleX();
            pt[0] += view.getLeft() - view.getScrollX();
            pt[1] += view.getTop() - view.getScrollY();
            viewParent = view.getParent();
        }
        coord[0] = (int) Math.round(pt[0]);
        coord[1] = (int) Math.round(pt[1]);
        return scale;
    }

    public void getViewRectRelativeToSelf(View v, Rect r) {
        int[] loc = new int[2];
        getLocationInWindow(loc);
        int x = loc[0];
        int y = loc[1];

        v.getLocationInWindow(loc);
        int vX = loc[0];
        int vY = loc[1];

        int left = vX - x;
        int top = vY - y;
        r.set(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return mDragController.dispatchUnhandledMove(focused, direction);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {
        public int x, y;
        public boolean customPosition = false;

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public int getWidth() {
            return width;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public int getHeight() {
            return height;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getX() {
            return x;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getY() {
            return y;
        }
    }

    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            final FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) child.getLayoutParams();
            if (flp instanceof LayoutParams) {
                final LayoutParams lp = (LayoutParams) flp;
                if (lp.customPosition) {
                    child.layout(lp.x, lp.y, lp.x + lp.width, lp.y + lp.height);
                }
            }
        }
    }

    public void clearAllResizeFrames() {
        if (mResizeFrames.size() > 0) {
            for (AppWidgetResizeFrame frame: mResizeFrames) {
                frame.commitResize();
                removeView(frame);
            }
            mResizeFrames.clear();
        }
    }

    private View mLastClickedContextMenuItem;

    public void closeContextMenu() {

        if (mContextMenuView == null) return;

        if (mContextMenuOpenAnim != null) {
            mContextMenuOpenAnim.cancel();
            mContextMenuOpenAnim = null;
        }

        final float oldT = mContextMenuView.getScaleX();
        mContextMenuCloseAnim = ValueAnimator.ofFloat(oldT, 0.0f);
        mContextMenuCloseAnim.setDuration((int) (200 * oldT));
        mContextMenuCloseAnim.setInterpolator(new AccelerateInterpolator());
        mContextMenuCloseAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mContextMenuView != null) {
                    float t = (Float) animation.getAnimatedValue();
                    mContextMenuView.setScaleX(t);
                    mContextMenuView.setScaleY(t);
                    mContextMenuView.setAlpha(t);
                    mContextMenuView.requestLayout();
                }
            }
        });

        mContextMenuCloseAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {

                removeView(mContextMenuView);

                if (mLastClickedContextMenuItem == null) {
                    if (mContextMenuView != null) {
                        mContextMenuView.dispose();
                        mContextMenuView = null;
                    }
                } else {

                    final View lastClickedContextMenuItem = mLastClickedContextMenuItem;
                    mLastClickedContextMenuItem = null;

                    final CellLayout cellLayout = mContextMenuView.getCellLayout();
                    final View item = mContextMenuView.getItem();
                    mContextMenuView.dispose();
                    mContextMenuView = null;

                    String tag = (String) lastClickedContextMenuItem.getTag();
                    if (ContextMenuView.TAG_WIDGET_RESIZE.equals(tag)) {

                        final LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) item;
                        AppWidgetProviderInfo pinfo = hostView.getAppWidgetInfo();
                        if (pinfo != null &&
                                pinfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) {
                            addResizeFrame((ItemInfo) hostView.getTag(), hostView, cellLayout);
                        }
                    } else if (ContextMenuView.TAG_SHORTCUT_EDITNAME.equals(tag)) {

                        showNameEditViewForShortcut((BubbleTextView) item);

                    } else if (ContextMenuView.TAG_SHORTCUT_DELETE.equals(tag)) {
                        ShortcutInfo sinfo = (ShortcutInfo) item.getTag();
                        cellLayout.removeViewInLayout(item);
                        LauncherModel.deleteItemFromDatabase(mLauncher, sinfo);

                    } else if (ContextMenuView.TAG_WIDGET_DELETE.equals(tag)) {

                        cellLayout.removeViewInLayout(item);

                        final LauncherAppWidgetInfo wInfo = (LauncherAppWidgetInfo) item.getTag();
                        mLauncher.removeAppWidget(wInfo);
                        LauncherModel.deleteItemFromDatabase(mLauncher, wInfo);

                        final LauncherAppWidgetHost appWidgetHost = mLauncher.getAppWidgetHost();
                        if (appWidgetHost != null) {
                            // Deleting an app widget ID is a void call but writes to disk before returning
                            // to the caller...
                            new Thread("deleteAppWidgetId") {
                                public void run() {
                                    appWidgetHost.deleteAppWidgetId(wInfo.appWidgetId);
                                }
                            }.start();
                        }
                    } else if (ContextMenuView.TAG_FOLDER_DELETE.equals(tag)) {
                        cellLayout.removeViewInLayout(item);
                        FolderInfo folderInfo = (FolderInfo) item.getTag();
                        mLauncher.removeFolder(folderInfo);
                        LauncherModel.deleteFolderContentsFromDatabase(mLauncher, folderInfo);
                    } else if (ContextMenuView.TAG_FOLDER_EDITNAME.equals(tag)) {
                        showNameEditViewForFolder((FolderIcon) item);
                    } else if (ContextMenuView.TAG_SHORTCUT_UNINSTALL.equals(tag)) {
                        uninstallApplication((ShortcutInfo) item.getTag());
                    } else if (ContextMenuView.TAG_WIDGET_SKINCHANGE.equals(tag)) {
                         final LauncherAppWidgetInfo wInfo = (LauncherAppWidgetInfo) item.getTag();
                        mLauncher.startBatteryWidgetSkinChangeActivity(wInfo.appWidgetId);
                    } else if (ContextMenuView.TAG_SHORTCUT_KISEKAE.equals(tag)) {
                        ShortcutInfo sinfo = (ShortcutInfo) item.getTag();
                        mLauncher.startShortcutKisekaeActivity(sinfo);
                    }
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {}

            @Override
            public void onAnimationRepeat(Animator animation) {}

            @Override
            public void onAnimationCancel(Animator animation) {}
        });

        mContextMenuCloseAnim.start();
    }

    private void uninstallApplication(ShortcutInfo sinfo) {
        ComponentName component = sinfo.intent.getComponent();
        if (component == null) {
            return;
        }
        ResolveInfo rInfo = mLauncher.getModel().findResolveInfo(mLauncher,
                component.getPackageName(), component.getClassName());
        if (rInfo == null) {
            return;
        }

        ApplicationInfo appInfo = new ApplicationInfo(mLauncher.getPackageManager(),
                rInfo, mLauncher.getIconCache(), null);

        mLauncher.startApplicationUninstallActivity(appInfo);
    }

    private NameEditView mNameEditView;
    private ValueAnimator mNameEditViewOpenAnim;
    private ValueAnimator mNameEditViewCloseAnim;

    private void showNameEditView(int iconSize, View.OnClickListener onCompleteListner) {

        mNameEditView.setOnCompleteListner(onCompleteListner);

        mNameEditView.setOnCancelListner(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNameEditView();
            }
        });

        LayoutParams lp = new LayoutParams(-1, -1);
        lp.customPosition = true;

        lp.width = iconSize + (int) Math.ceil(238 * LauncherApplication.getScreenDensity());
        lp.height = iconSize + (int) Math.ceil(92 * LauncherApplication.getScreenDensity());

        lp.x = (int) Math.floor((getMeasuredWidth() - lp.width) / 2f);
        lp.y = (int) Math.floor(getMeasuredHeight() / 3f - lp.height / 2f);

        addView(mNameEditView, lp);

        mNameEditViewOpenAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        mNameEditViewOpenAnim.setDuration(270);
        mNameEditViewOpenAnim.setInterpolator(new DecelerateInterpolator(1.3f));
        mNameEditViewOpenAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mNameEditView != null) {
                    float t = (Float) animation.getAnimatedValue();
                    mNameEditView.setScaleX(t);
                    mNameEditView.setScaleY(t);
                    mNameEditView.setAlpha(t);
                    mNameEditView.invalidate();
                }
            }
        });
        mNameEditViewOpenAnim.start();
    }

    private void showNameEditViewForFolder(FolderIcon folderIcon) {

        final FolderInfo finfo = (FolderInfo) folderIcon.getTag();
        final Folder folder = folderIcon.getFolder();

        final int previewIconSize = (int) Math.ceil(LauncherApplication.getScreenDensity() * 48);

        // フォルダイメージ生成
        folderIcon.setTextVisibility(View.INVISIBLE);
        int iconSize = folderIcon.getFolderIconSize();
        Bitmap folderBmp = mDragController.getViewBitmap(folderIcon, (folderIcon.getTotalWidth() - iconSize) / 2,
                folderIcon.getFolderIconPaddingTop(), iconSize, iconSize);
        folderBmp =  BitmapUtils.resizeBitmap2(folderBmp, previewIconSize, previewIconSize, new Matrix(), true);

        folderIcon.setTextVisibility(View.VISIBLE);

        mNameEditView = NameEditView.fromXml(getContext());

        String folderName = finfo.title.toString();
        String hintText = mLauncher.getResources().getString(R.string.folder_hint_text);
        mNameEditView.init(folderName, hintText, folderBmp, true);

        showNameEditView(previewIconSize, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String newTitle = mNameEditView.getText();
                finfo.setTitle(newTitle);
                LauncherModel.updateItemInDatabase(mLauncher, finfo);
                folder.setFolderName(newTitle);

                closeNameEditView();
            }
        });
    }

    private void showNameEditViewForShortcut(final BubbleTextView shortcut) {

        final int previewIconSize = (int) Math.ceil(LauncherApplication.getScreenDensity() * 48);

        final ShortcutInfo sinfo = (ShortcutInfo) shortcut.getTag();

        String shortcutName;
        if (sinfo.shortcutName != null) {
            shortcutName = sinfo.shortcutName;
        }else {
            shortcutName = sinfo.title.toString();
        }

        mNameEditView = NameEditView.fromXml(getContext());

        Bitmap iconBmpSrc = sinfo.getIcon(mLauncher.getIconCache());
        Bitmap iconBmp = BitmapUtils.resizeBitmap2(iconBmpSrc,
                previewIconSize, previewIconSize, new Matrix(), false);

        boolean disposeIcon = (iconBmpSrc != iconBmp);
        mNameEditView.init(shortcutName, sinfo.title.toString(), iconBmp, disposeIcon);

        showNameEditView(previewIconSize, new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String newTitle = mNameEditView.getText();
                sinfo.shortcutName = newTitle;
                LauncherModel.updateItemInDatabase(mLauncher, sinfo);

                shortcut.updateShortcutName();

                closeNameEditView();
            }
        });
    }

    private boolean mNameEditViewCloseAnimRunning = false;

    void closeNameEditView() {

        if (mNameEditViewCloseAnimRunning) return;
        if (mNameEditView == null) return;

        mNameEditViewCloseAnimRunning = true;

        if (mNameEditViewOpenAnim != null) {
            mNameEditViewOpenAnim.cancel();
            mNameEditViewOpenAnim = null;
        }

        final float oldT = mNameEditView.getScaleX();
        mNameEditViewCloseAnim = ValueAnimator.ofFloat(oldT, 0.0f);
        mNameEditViewCloseAnim.setDuration((int) (270 * oldT));
        mNameEditViewCloseAnim.setInterpolator(new AccelerateInterpolator());
        mNameEditViewCloseAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mNameEditView != null) {
                    float t = (Float) animation.getAnimatedValue();
                    mNameEditView.setScaleX(t);
                    mNameEditView.setScaleY(t);
                    mNameEditView.setAlpha(t);
                    mNameEditView.invalidate();
                }
            }
        });
        mNameEditViewCloseAnim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mNameEditViewCloseAnimRunning = false;
                if (mNameEditView != null) {
                    removeView(mNameEditView);
                    mNameEditView.dispose();
                    mNameEditView = null;
                }
            }
            @Override
            public void onAnimationStart(Animator animation) {
            }
            @Override
            public void onAnimationRepeat(Animator animation) {
            }
            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        mNameEditViewCloseAnim.start();
    }

    public boolean hasResizeFrames() {
        return mResizeFrames.size() > 0;
    }

    public boolean isWidgetBeingResized() {
        return mCurrentResizeFrame != null;
    }

    public ValueAnimator mContextMenuOpenAnim;
    public ValueAnimator mContextMenuCloseAnim;

    public void showContextMenuForShortcut(CellLayout targetCellLayout, BubbleTextView shortcut) {

        if (mContextMenuView != null) return;

        final int posYOnDragLayer = shortcut.getTop() + shortcut.getPaddingTop()
                + targetCellLayout.getTop() + getPaddingTop();
        final boolean isUpperMenu = (posYOnDragLayer + shortcut.getHeight() / 2 > getMeasuredHeight() / 2);

        mContextMenuView = new ContextMenuView(getContext(), LauncherApplication.getScreenDensity());
        boolean canUninstall = canUninstall((ShortcutInfo) shortcut.getTag());
        mContextMenuView.initForShortcut(getMeasuredWidth(), isUpperMenu, canUninstall);

        showContextMenu(targetCellLayout, shortcut, isUpperMenu, posYOnDragLayer);
    }

    private boolean canUninstall(ShortcutInfo sinfo) {

        ComponentName component = sinfo.intent.getComponent();
        if (component == null) {
            return false;
        }

        ResolveInfo rInfo = mLauncher.getModel().findResolveInfo(mLauncher,
                component.getPackageName(), component.getClassName());
        if (rInfo == null) {
            return false;
        }

        ApplicationInfo appInfo = new ApplicationInfo(mLauncher.getPackageManager(),
                rInfo, mLauncher.getIconCache(), null);

        return (appInfo.flags & ApplicationInfo.DOWNLOADED_FLAG) != 0;
    }

    public void showContextMenuForFolder(CellLayout targetCellLayout, FolderIcon folderIcon) {

        if (mContextMenuView != null) return;

        final int posYOnDragLayer = folderIcon.getTop() + folderIcon.getPaddingTop()
                + targetCellLayout.getTop() + getPaddingTop();
        final boolean isUpperMenu = (posYOnDragLayer + folderIcon.getHeight() / 2 > getMeasuredHeight() / 2);

        mContextMenuView = new ContextMenuView(getContext(), LauncherApplication.getScreenDensity());
        mContextMenuView.initForFolder(getMeasuredWidth(), isUpperMenu);

        showContextMenu(targetCellLayout, folderIcon, isUpperMenu, posYOnDragLayer);
    }

    public void showContextMenuForWidget(CellLayout targetCellLayout, LauncherAppWidgetHostView widget) {

        if (mContextMenuView != null) return;

        // Widgetリサイズ・スキン変更可判定
        boolean canResize = false;
        boolean canSkinChange = false;
        AppWidgetProviderInfo pinfo = widget.getAppWidgetInfo();
        if (pinfo != null) {
            if ( pinfo.resizeMode != AppWidgetProviderInfo.RESIZE_NONE) {
                canResize = true;
            }

            if (ThemeUtils.isInAppWidget(pinfo.provider.getClassName(), null)) {
                canSkinChange = true;
            }
        }

        final int posYOnDragLayer = widget.getTop() + widget.getPaddingTop()
                + targetCellLayout.getTop() + getPaddingTop();
        final boolean isUpperMenu = (posYOnDragLayer + widget.getHeight() / 2 > getMeasuredHeight() / 2);

        mContextMenuView = new ContextMenuView(getContext(), LauncherApplication.getScreenDensity());
        mContextMenuView.initForWidget(getMeasuredWidth(), isUpperMenu, canResize, canSkinChange);

        showContextMenu(targetCellLayout, widget, isUpperMenu, posYOnDragLayer);
    }

    private void showContextMenu(CellLayout targetCellLayout, View item, boolean isUpperMenu, int itemPosYOnDragLayer) {

        final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        lp.customPosition = true;

        lp.width = mContextMenuView.getStaticWidth();
        lp.height = mContextMenuView.getStaticHeight();

        lp.x = item.getLeft() + (int) Math.floor((item.getWidth() - lp.width) / 2f);
        if (lp.x < 0) {
            mContextMenuView.setArrowLeftMargin(mContextMenuView.getArrowLeftMargin() + lp.x);
            lp.x = 0;
        } else if (lp.x + mContextMenuView.getStaticWidth() - targetCellLayout.getMeasuredWidth() > 0) {
            mContextMenuView.setArrowLeftMargin(mContextMenuView.getArrowLeftMargin()
                    + lp.x + mContextMenuView.getStaticWidth() - targetCellLayout.getMeasuredWidth());
            lp.x = -mContextMenuView.getStaticWidth() + targetCellLayout.getMeasuredWidth();
        }

        int gap = (int) Math.ceil(LauncherApplication.getScreenDensity() * 10);
        if (isUpperMenu) {
            lp.y = itemPosYOnDragLayer - mContextMenuView.getStaticHeight() - gap;
        } else {
            lp.y = itemPosYOnDragLayer - item.getPaddingTop() + item.getHeight() - item.getPaddingBottom() + gap;
        }

        mContextMenuView.setPivotX(mContextMenuView.getArrowLeftMargin() + mContextMenuView.getArrowWidth() / 2);
        if (isUpperMenu) {
            mContextMenuView.setPivotY(mContextMenuView.getStaticHeight());
        } else {
            mContextMenuView.setPivotY(0);
        }

        mContextMenuView.setOnClickListener(new ContextMenuView.OnClickListener() {
            @Override
            public void onMenuClick(View v) {
                mLastClickedContextMenuItem = v;
                closeContextMenu();
            }
        });

        mContextMenuView.setup(targetCellLayout, item);

        addView(mContextMenuView, lp);

        mContextMenuOpenAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
        mContextMenuOpenAnim.setDuration(200);
        mContextMenuOpenAnim.setInterpolator(new DecelerateInterpolator(1.3f));
        mContextMenuOpenAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (mContextMenuView != null) {
                    float t = (Float) animation.getAnimatedValue();
                    mContextMenuView.setScaleX(t);
                    mContextMenuView.setScaleY(t);
                    mContextMenuView.setAlpha(t);
                    mContextMenuView.requestLayout();
                }
            }
        });
        mContextMenuOpenAnim.start();
    }

    public void addResizeFrame(ItemInfo itemInfo, LauncherAppWidgetHostView widget,
            CellLayout cellLayout) {

        AppWidgetResizeFrame resizeFrame = new AppWidgetResizeFrame(getContext(),
                widget, cellLayout, this);

        LayoutParams lp = new LayoutParams(-1, -1);
        lp.customPosition = true;

        addView(resizeFrame, lp);
        mResizeFrames.add(resizeFrame);

        resizeFrame.snapToWidget(false);
    }

    public void animateViewIntoPosition(DragView dragView, final View child) {
        animateViewIntoPosition(dragView, child, null);
    }

    public void animateViewIntoPosition(DragView dragView, final int[] pos, float alpha,
            float scaleX, float scaleY, int animationEndStyle, Runnable onFinishRunnable,
            int duration) {
        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);
        final int fromX = r.left;
        final int fromY = r.top;

        animateViewIntoPosition(dragView, fromX, fromY, pos[0], pos[1], alpha, 1, 1, scaleX, scaleY,
                onFinishRunnable, animationEndStyle, duration, null);
    }

    public void animateViewIntoPosition(DragView dragView, final View child,
            final Runnable onFinishAnimationRunnable) {
        animateViewIntoPosition(dragView, child, -1, onFinishAnimationRunnable, null);
    }

    public void animateViewIntoPosition(DragView dragView, final View child, int duration,
            final Runnable onFinishAnimationRunnable, View anchorView) {
        ShortcutAndWidgetContainer parentChildren = (ShortcutAndWidgetContainer) child.getParent();
        CellLayout.LayoutParams lp =  (CellLayout.LayoutParams) child.getLayoutParams();
        parentChildren.measureChild(child);

        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);

        int coord[] = new int[2];
        float childScale = child.getScaleX();
        coord[0] = lp.x + (int) (child.getMeasuredWidth() * (1 - childScale) / 2);
        coord[1] = lp.y + (int) (child.getMeasuredHeight() * (1 - childScale) / 2);

        // Since the child hasn't necessarily been laid out, we force the lp to be updated with
        // the correct coordinates (above) and use these to determine the final location
        float scale = getDescendantCoordRelativeToSelf((View) child.getParent(), coord);
        // We need to account for the scale of the child itself, as the above only accounts for
        // for the scale in parents.
        scale *= childScale;
        int toX = coord[0];
        int toY = coord[1];
        if (child instanceof TextView) {
            TextView tv = (TextView) child;

            // The child may be scaled (always about the center of the view) so to account for it,
            // we have to offset the position by the scaled size.  Once we do that, we can center
            // the drag view about the scaled child view.
            toY += Math.round(scale * tv.getPaddingTop());
            toY -= dragView.getMeasuredHeight() * (1 - scale) / 2;
            toX -= (dragView.getMeasuredWidth() - Math.round(scale * child.getMeasuredWidth())) / 2;
        } else if (child instanceof FolderIcon) {
            // Account for holographic blur padding on the drag view
            toY -= scale * Workspace.DRAG_BITMAP_PADDING / 2;
            toY -= (1 - scale) * dragView.getMeasuredHeight() / 2;
            // Center in the x coordinate about the target's drawable
            toX -= (dragView.getMeasuredWidth() - Math.round(scale * child.getMeasuredWidth())) / 2;
        } else {
            toY -= (Math.round(scale * (dragView.getHeight() - child.getMeasuredHeight()))) / 2;
            toX -= (Math.round(scale * (dragView.getMeasuredWidth()
                    - child.getMeasuredWidth()))) / 2;
        }

        final int fromX = r.left;
        final int fromY = r.top;
        child.setVisibility(INVISIBLE);
        Runnable onCompleteRunnable = new Runnable() {
            public void run() {
                child.setVisibility(VISIBLE);
                if (onFinishAnimationRunnable != null) {
                    onFinishAnimationRunnable.run();
                }
            }
        };
        animateViewIntoPosition(dragView, fromX, fromY, toX, toY, 1, 1, 1, scale, scale,
                onCompleteRunnable, ANIMATION_END_DISAPPEAR, duration, anchorView);
    }

    public void animateViewIntoPosition2(DragView dragView, final View child, int duration,
            final Runnable onFinishAnimationRunnable, View anchorView) {
        CellItemContainer parentChildren = (CellItemContainer) child.getParent();
        BaseCellLayout.LayoutParams lp =  (BaseCellLayout.LayoutParams) child.getLayoutParams();
        parentChildren.measureChild(child);

        Rect r = new Rect();
        getViewRectRelativeToSelf(dragView, r);

        int coord[] = new int[2];
        float childScale = child.getScaleX();
        coord[0] = lp.x + (int) (child.getMeasuredWidth() * (1 - childScale) / 2);
        coord[1] = lp.y + (int) (child.getMeasuredHeight() * (1 - childScale) / 2);

        // Since the child hasn't necessarily been laid out, we force the lp to be updated with
        // the correct coordinates (above) and use these to determine the final location
        float scale = getDescendantCoordRelativeToSelf((View) child.getParent(), coord);
        // We need to account for the scale of the child itself, as the above only accounts for
        // for the scale in parents.

        scale *= childScale;
        if (lp.drawingScale > 0.0f) {
            scale *= lp.drawingScale;
        }
        int toX = coord[0];
        int toY = coord[1];
        if (child instanceof TextView) {
            TextView tv = (TextView) child;

            // The child may be scaled (always about the center of the view) so to account for it,
            // we have to offset the position by the scaled size.  Once we do that, we can center
            // the drag view about the scaled child view.
            toY += Math.round(scale * tv.getPaddingTop());
            toY -= dragView.getMeasuredHeight() * (1 - scale) / 2;
            toX -= (dragView.getMeasuredWidth() - Math.round(scale * child.getMeasuredWidth())) / 2;
        } else if (child instanceof FolderIcon) {
            // Account for holographic blur padding on the drag view
            toY -= scale * Workspace.DRAG_BITMAP_PADDING / 2;
            toY -= (1 - scale) * dragView.getMeasuredHeight() / 2;
            // Center in the x coordinate about the target's drawable
            toX -= (dragView.getMeasuredWidth() - Math.round(scale * child.getMeasuredWidth())) / 2;
        } else {
            toY -= (Math.round(scale * (dragView.getHeight() - child.getMeasuredHeight()))) / 2;
            toX -= (Math.round(scale * (dragView.getMeasuredWidth()
                    - child.getMeasuredWidth()))) / 2;
        }

        final int fromX = r.left;
        final int fromY = r.top;
        child.setVisibility(INVISIBLE);
        Runnable onCompleteRunnable = new Runnable() {
            public void run() {
                child.setVisibility(VISIBLE);
                if (onFinishAnimationRunnable != null) {
                    onFinishAnimationRunnable.run();
                }
            }
        };
        animateViewIntoPosition(dragView, fromX, fromY, toX, toY, 1, 1, 1, scale, scale,
                onCompleteRunnable, ANIMATION_END_DISAPPEAR, duration, anchorView);
    }

    public void animateViewIntoPosition(final DragView view, final int fromX, final int fromY,
            final int toX, final int toY, float finalAlpha, float initScaleX, float initScaleY,
            float finalScaleX, float finalScaleY, Runnable onCompleteRunnable,
            int animationEndStyle, int duration, View anchorView) {
        Rect from = new Rect(fromX, fromY, fromX +
                view.getMeasuredWidth(), fromY + view.getMeasuredHeight());
        Rect to = new Rect(toX, toY, toX + view.getMeasuredWidth(), toY + view.getMeasuredHeight());
        animateView(view, from, to, finalAlpha, initScaleX, initScaleY, finalScaleX, finalScaleY, duration,
                null, null, onCompleteRunnable, animationEndStyle, anchorView);
    }

    /**
     * This method animates a view at the end of a drag and drop animation.
     *
     * @param view The view to be animated. This view is drawn directly into DragLayer, and so
     *        doesn't need to be a child of DragLayer.
     * @param from The initial location of the view. Only the left and top parameters are used.
     * @param to The final location of the view. Only the left and top parameters are used. This
     *        location doesn't account for scaling, and so should be centered about the desired
     *        final location (including scaling).
     * @param finalAlpha The final alpha of the view, in case we want it to fade as it animates.
     * @param finalScale The final scale of the view. The view is scaled about its center.
     * @param duration The duration of the animation.
     * @param motionInterpolator The interpolator to use for the location of the view.
     * @param alphaInterpolator The interpolator to use for the alpha of the view.
     * @param onCompleteRunnable Optional runnable to run on animation completion.
     * @param fadeOut Whether or not to fade out the view once the animation completes. If true,
     *        the runnable will execute after the view is faded out.
     * @param anchorView If not null, this represents the view which the animated view stays
     *        anchored to in case scrolling is currently taking place. Note: currently this is
     *        only used for the X dimension for the case of the workspace.
     */
    public void animateView(final DragView view, final Rect from, final Rect to,
            final float finalAlpha, final float initScaleX, final float initScaleY,
            final float finalScaleX, final float finalScaleY, int duration,
            final Interpolator motionInterpolator, final Interpolator alphaInterpolator,
            final Runnable onCompleteRunnable, final int animationEndStyle, View anchorView) {

        // Calculate the duration of the animation based on the object's distance
        final float dist = (float) Math.sqrt(Math.pow(to.left - from.left, 2) +
                Math.pow(to.top - from.top, 2));
        final Resources res = getResources();
        final float maxDist = (float) res.getInteger(R.integer.config_dropAnimMaxDist);

        // If duration < 0, this is a cue to compute the duration based on the distance
        if (duration < 0) {
            duration = res.getInteger(R.integer.config_dropAnimMaxDuration);
            if (dist < maxDist) {
                duration *= mCubicEaseOutInterpolator.getInterpolation(dist / maxDist);
            }
            duration = Math.max(duration, res.getInteger(R.integer.config_dropAnimMinDuration));
        }

        // Fall back to cubic ease out interpolator for the animation if none is specified
        TimeInterpolator interpolator = null;
        if (alphaInterpolator == null || motionInterpolator == null) {
            interpolator = mCubicEaseOutInterpolator;
        }

        // Animate the view
        final float initAlpha = view.getAlpha();
        final float dropViewScale = view.getScaleX();
        AnimatorUpdateListener updateCb = new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float percent = (Float) animation.getAnimatedValue();
                final int width = view.getMeasuredWidth();
                final int height = view.getMeasuredHeight();

                float alphaPercent = alphaInterpolator == null ? percent :
                        alphaInterpolator.getInterpolation(percent);
                float motionPercent = motionInterpolator == null ? percent :
                        motionInterpolator.getInterpolation(percent);

                float initialScaleX = initScaleX * dropViewScale;
                float initialScaleY = initScaleY * dropViewScale;
                float scaleX = finalScaleX * percent + initialScaleX * (1 - percent);
                float scaleY = finalScaleY * percent + initialScaleY * (1 - percent);
                float alpha = finalAlpha * alphaPercent + initAlpha * (1 - alphaPercent);

                float fromLeft = from.left + (initialScaleX - 1f) * width / 2;
                float fromTop = from.top + (initialScaleY - 1f) * height / 2;

                int x = (int) (fromLeft + Math.round(((to.left - fromLeft) * motionPercent)));
                int y = (int) (fromTop + Math.round(((to.top - fromTop) * motionPercent)));

                int xPos = x - mDropView.getScrollX() + (mAnchorView != null
                        ? (mAnchorViewInitialScrollX - mAnchorView.getScrollX()) : 0);
                int yPos = y - mDropView.getScrollY();

                mDropView.setTranslationX(xPos);
                mDropView.setTranslationY(yPos);
                mDropView.setScaleX(scaleX);
                mDropView.setScaleY(scaleY);
                mDropView.setAlpha(alpha);
            }
        };
        animateView(view, updateCb, duration, interpolator, onCompleteRunnable, animationEndStyle,
                anchorView);
    }

    public void animateView(final DragView view, AnimatorUpdateListener updateCb, int duration,
            TimeInterpolator interpolator, final Runnable onCompleteRunnable,
            final int animationEndStyle, View anchorView) {
        // Clean up the previous animations
        if (mDropAnim != null) mDropAnim.cancel();
        if (mFadeOutAnim != null) mFadeOutAnim.cancel();

        // Show the drop view if it was previously hidden
        mDropView = view;
        mDropView.cancelAnimation();
        mDropView.resetLayoutParams();

        // Set the anchor view if the page is scrolling
        if (anchorView != null) {
            mAnchorViewInitialScrollX = anchorView.getScrollX();
        }
        mAnchorView = anchorView;

        // Create and start the animation
        mDropAnim = new ValueAnimator();
        mDropAnim.setInterpolator(interpolator);
        mDropAnim.setDuration(duration);
        mDropAnim.setFloatValues(0f, 1f);
        mDropAnim.addUpdateListener(updateCb);
        mDropAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
                switch (animationEndStyle) {
                case ANIMATION_END_DISAPPEAR:
                    clearAnimatedView();
                    break;
                case ANIMATION_END_FADE_OUT:
                    fadeOutDragView();
                    break;
                case ANIMATION_END_REMAIN_VISIBLE:
                    break;
                }
            }
        });
        mDropAnim.start();
    }

    public void clearAnimatedView() {
        if (mDropAnim != null) {
            mDropAnim.cancel();
        }
        if (mDropView != null) {
            mDragController.onDeferredEndDrag(mDropView);
        }
        mDropView = null;
        invalidate();
    }

    public View getAnimatedView() {
        return mDropView;
    }

    private void fadeOutDragView() {
        mFadeOutAnim = new ValueAnimator();
        mFadeOutAnim.setDuration(150);
        mFadeOutAnim.setFloatValues(0f, 1f);
        mFadeOutAnim.removeAllUpdateListeners();
        mFadeOutAnim.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                final float percent = (Float) animation.getAnimatedValue();

                float alpha = 1 - percent;
                mDropView.setAlpha(alpha);
            }
        });
        mFadeOutAnim.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                if (mDropView != null) {
                    mDragController.onDeferredEndDrag(mDropView);
                }
                mDropView = null;
                invalidate();
            }
        });
        mFadeOutAnim.start();
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        updateChildIndices();
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        updateChildIndices();
    }

    private void updateChildIndices() {
        if (mLauncher != null) {
            mWorkspaceIndex = indexOfChild(mLauncher.getWorkspace());
            mQsbIndex = indexOfChild(mLauncher.getSearchBar());
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        // TODO: We have turned off this custom drawing order because it now effects touch
        // dispatch order. We need to sort that issue out and then decide how to go about this.
        if (true ||
                mWorkspaceIndex == -1 || mQsbIndex == -1 ||
                mLauncher.getWorkspace().isDrawingBackgroundGradient()) {
            return i;
        }

        // This ensures that the workspace is drawn above the hotseat and qsb,
        // except when the workspace is drawing a background gradient, in which
        // case we want the workspace to stay behind these elements.
        if (i == mQsbIndex) {
            return mWorkspaceIndex;
        } else if (i == mWorkspaceIndex) {
            return mQsbIndex;
        } else {
            return i;
        }
    }

    private boolean mInScrollArea;
    private Drawable mLeftHoverDrawable;
    private Drawable mRightHoverDrawable;

    void onEnterScrollArea(int direction) {
        mInScrollArea = true;
        invalidate();
    }

    void onExitScrollArea() {
        mInScrollArea = false;
        invalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mInScrollArea && !LauncherApplication.isScreenLarge()) {
            Workspace workspace = mLauncher.getWorkspace();
            int width = workspace.getWidth();
            Rect childRect = new Rect();
            getDescendantRectRelativeToSelf(workspace.getChildAt(0), childRect);

            int page = workspace.getNextPage();
            HomeGridLayout leftPage = (HomeGridLayout) workspace.getChildAt(page - 1);
            HomeGridLayout rightPage = (HomeGridLayout) workspace.getChildAt(page + 1);

            if (leftPage != null && leftPage.getIsDragOverlapping()) {
                mLeftHoverDrawable.setBounds(0, childRect.top,
                        mLeftHoverDrawable.getIntrinsicWidth(), childRect.bottom);
                mLeftHoverDrawable.draw(canvas);
            } else if (rightPage != null && rightPage.getIsDragOverlapping()) {
                mRightHoverDrawable.setBounds(width - mRightHoverDrawable.getIntrinsicWidth(),
                        childRect.top, width, childRect.bottom);
                mRightHoverDrawable.draw(canvas);
            }
        }
    }
}
