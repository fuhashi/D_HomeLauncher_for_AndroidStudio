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
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsFileName;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.launcher.DropTarget.DragObject;
import jp.co.disney.apps.managed.kisekaeapp.launcher.FolderInfo.FolderListener;
import jp.co.disney.apps.managed.kisekaeapp.system.gesture.CheckLongPressHelper;
import jp.co.disney.apps.managed.kisekaeapp.system.view.animation.ViewAnimUtils;

/**
 * An icon that can appear on in the workspace representing an {@link UserFolder}.
 */
public class FolderIcon extends FrameLayout implements FolderListener {
    private Launcher mLauncher;
    private Folder mFolder;
    private FolderInfo mInfo;
    private static boolean sStaticValuesDirty = true;

    private CheckLongPressHelper mLongPressHelper;

    // The number of icons to display in the
    private static final int NUM_ITEMS_IN_PREVIEW = 4;
    private static final int CONSUMPTION_ANIMATION_DURATION = 100;
    private static final int DROP_IN_ANIMATION_DURATION = 400;
    private static final int INITIAL_ITEM_ANIMATION_DURATION = 350;
    private static final int FINAL_ITEM_ANIMATION_DURATION = 200;

    // The degree to which the outer ring is scaled in its natural state
    private static final float FOLDER_ICON_GROWTH_FACTOR = 0.3f;

    private static final int PREVIEW_ICON_SIZE = 54;

    private ImageView mPreviewBackground;
    private BubbleTextView mFolderName;

    FolderIconAnimator mFolderIconAnimator = null;

    // These variables are all associated with the drawing of the preview; they are stored
    // as member variables for shared usage and to avoid computation on each frame
    private int mIntrinsicIconSize;
    private int mTotalWidth;
    boolean mAnimating = false;

    private PreviewItemDrawingParams mParams = new PreviewItemDrawingParams(0, 0, 0);
    private PreviewItemDrawingParams mAnimParams = new PreviewItemDrawingParams(0, 0, 0);
    private ArrayList<ShortcutInfo> mHiddenItems = new ArrayList<ShortcutInfo>();

    private int mFolderIconSize;
    private int mFolderIconPadTop;

    boolean mHoverAnmating = false;
    private float mHoverAnimationScaleFactor;

    public int getFolderIconSize() {
        return mFolderIconSize;
    }

    public int getFolderIconPaddingTop() {
        return mFolderIconPadTop;
    }

    public int getTotalWidth() {
        return mTotalWidth;
    }

    public void setTextVisibility(int visibility) {
        mFolderName.setVisibility(visibility);
    }

    public FolderIcon(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FolderIcon(Context context) {
        super(context);
        init();
    }

    private void init() {
        mLongPressHelper = new CheckLongPressHelper(this);
    }

    public boolean isDropEnabled() {
        final ViewGroup cellLayoutChildren = (ViewGroup) getParent();
        final ViewGroup cellLayout = (ViewGroup) cellLayoutChildren.getParent();
        final Workspace workspace = (Workspace) cellLayout.getParent();
        return !workspace.isSmall();
    }

    static FolderIcon fromXml(int resId, Launcher launcher, ViewGroup group,
            FolderInfo folderInfo, IconCache iconCache, int folderIcSize,
            int paddingTop, float scale, int totalWidth, int textPadding) {
        @SuppressWarnings("all") // suppress dead code warning
        final boolean error = INITIAL_ITEM_ANIMATION_DURATION >= DROP_IN_ANIMATION_DURATION;
        if (error) {
            throw new IllegalStateException("DROP_IN_ANIMATION_DURATION must be greater than " +
                    "INITIAL_ITEM_ANIMATION_DURATION, as sequencing of adding first two items " +
                    "is dependent on this");
        }

        FolderIcon icon = (FolderIcon) LayoutInflater.from(launcher).inflate(resId, group, false);

        icon.mFolderName = (BubbleTextView) icon.findViewById(R.id.folder_icon_name);
        icon.mFolderName.setText(folderInfo.title);
        icon.mPreviewBackground = (ImageView) icon.findViewById(R.id.preview_background);

        BitmapDrawable folderIconDrawable;
//        if (launcher.getIconThemeId() != null) {
//            folderIconDrawable = getFolderIconDrawable(launcher, launcher.getIconThemeId(), launcher.iconInAppTheme(),
//                    ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue());
//        } else {
            folderIconDrawable = getFolderIconDrawable(launcher, launcher.getThemeId(), launcher.inAppTheme(),
                    ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
//        }
        icon.mPreviewBackground.setImageDrawable(folderIconDrawable);

        icon.setTag(folderInfo);
        icon.setOnClickListener(launcher);
        icon.mInfo = folderInfo;
        icon.mLauncher = launcher;
        icon.setContentDescription(String.format(launcher.getString(R.string.folder_name_format),
                folderInfo.title));
        Folder folder = Folder.fromXml(launcher);
        folder.setDragController(launcher.getDragController());
        folder.setFolderIcon(icon);
        folder.bind(folderInfo);
        icon.mFolder = folder;

        icon.setFolderLayout(folderIcSize, paddingTop, scale, totalWidth, textPadding);

        icon.mFolderIconAnimator = new FolderIconAnimator(launcher, icon, icon.getFolderIconSize());
        folderInfo.addListener(icon);

        return icon;
    }

    public void setFolderIcon(Context context, String themeId, boolean inAppTheme, int contentsType) {
        BitmapDrawable d = ((BitmapDrawable) mPreviewBackground.getDrawable());
        Bitmap dBmp = d.getBitmap();
        if (dBmp != null) {
            dBmp.recycle();
        }
        mPreviewBackground.setImageDrawable(getFolderIconDrawable(context, themeId, inAppTheme, contentsType));
    }

    static BitmapDrawable getFolderIconDrawable(Context context, String themeId, boolean inAppTheme, int contentsType) {

        if (themeId == null) {
            themeId = ThemeUtils.THEME_ID_DEFAULT;
            inAppTheme = true;
        }

        Bitmap folderIconBmp = ThemeUtils.loadKisekaeIcon(context,
                ThemeUtils.getThemeRootDirectory(context, themeId, inAppTheme, contentsType) + ContentsFileName.folderIcon.getFileName());

        return new BitmapDrawable(context.getResources(), folderIconBmp);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        sStaticValuesDirty = true;
        return super.onSaveInstanceState();
    }

    public static class FolderIconAnimator {
        public int mCellX;
        public int mCellY;
        private CellLayout mCellLayout;
        public float mAnimFolderIconSize;
        public FolderIcon mFolderIcon = null;
        public int mBaseFolderIconSize = -1;
        public static int sBaseFolderIconSize = -1;
        public static Drawable sAnimFolderIconDrawable = null;

        private ValueAnimator mAcceptAnimator;
        private ValueAnimator mNeutralAnimator;

        public FolderIconAnimator(Launcher launcher, FolderIcon folderIcon, int folderIconSize) {
            mFolderIcon = folderIcon;
            sBaseFolderIconSize = folderIconSize;
            mBaseFolderIconSize = folderIconSize;

            // We need to reload the static values when configuration changes in case they are
            // different in another configuration
            if (sStaticValuesDirty) {
//                if (launcher.getIconThemeId() != null) {
//                    sAnimFolderIconDrawable = getFolderIconDrawable(launcher, launcher.getIconThemeId(), launcher.iconInAppTheme(),
//                            ContentsTypeValue.CONTENTS_TYPE_DRAWER_ICON_IN_T.getValue());
//                } else {
                    sAnimFolderIconDrawable = getFolderIconDrawable(launcher, launcher.getThemeId(), launcher.inAppTheme(),
                            ContentsTypeValue.CONTENTS_TYPE_THEME.getValue());
//                }
                sStaticValuesDirty = false;
            }
        }

        public static void setFolderIcon(Context context, String themeId, boolean inAppTheme, int contentsType) {
            if (sAnimFolderIconDrawable != null) {
                Bitmap bmp = ((BitmapDrawable) sAnimFolderIconDrawable).getBitmap();
                if (bmp != null) {
                    bmp.recycle();
                }
            }
            sAnimFolderIconDrawable = getFolderIconDrawable(context, themeId, inAppTheme, contentsType);
        }

        public void animateToAcceptState() {
            if (mNeutralAnimator != null) {
                mNeutralAnimator.cancel();
            }
            mAcceptAnimator = ViewAnimUtils.ofFloat(mCellLayout, 0f, 1f);
            mAcceptAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int folderIconSize = mBaseFolderIconSize;
            mAcceptAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    float scaleFactor = 1 + percent * FOLDER_ICON_GROWTH_FACTOR;
                    mAnimFolderIconSize = scaleFactor * folderIconSize;

                    if (mFolderIcon != null && mFolderIcon.mHoverAnmating) {
                        mFolderIcon.mHoverAnimationScaleFactor = scaleFactor;
                        mFolderIcon.invalidate();
                    }

                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mAcceptAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {

                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(INVISIBLE);
                        mFolderIcon.mHoverAnmating = true;
                    }
                }
            });
            mAcceptAnimator.start();
        }

        public void animateToNaturalState() {
            if (mAcceptAnimator != null) {
                mAcceptAnimator.cancel();
            }
            mNeutralAnimator = ViewAnimUtils.ofFloat(mCellLayout, 0f, 1f);
            mNeutralAnimator.setDuration(CONSUMPTION_ANIMATION_DURATION);

            final int previewSize = mBaseFolderIconSize;
            mNeutralAnimator.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    final float percent = (Float) animation.getAnimatedValue();
                    float scaleFactor = 1 + (1 - percent) * FOLDER_ICON_GROWTH_FACTOR;
                    mAnimFolderIconSize = scaleFactor * previewSize;

                    if (mFolderIcon != null && mFolderIcon.mHoverAnmating) {
                        mFolderIcon.mHoverAnimationScaleFactor = scaleFactor;
                        mFolderIcon.invalidate();
                    }

                    if (mCellLayout != null) {
                        mCellLayout.invalidate();
                    }
                }
            });
            mNeutralAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {

                    if (mCellLayout != null) {
                        mCellLayout.hideFolderAccept(FolderIconAnimator.this);
                    }
                    if (mFolderIcon != null) {
                        mFolderIcon.mPreviewBackground.setVisibility(VISIBLE);
                        mFolderIcon.mHoverAnmating = false;
                    }
                }
            });
            mNeutralAnimator.start();
        }

        // Location is expressed in window coordinates
        public void getCell(int[] loc) {
            loc[0] = mCellX;
            loc[1] = mCellY;
        }

        // Location is expressed in window coordinates
        public void setCell(int x, int y) {
            mCellX = x;
            mCellY = y;
        }

        public void setCellLayout(CellLayout layout) {
            mCellLayout = layout;
        }

        public float getAnimFolderIconSize() {
            return mAnimFolderIconSize;
        }
    }

    Folder getFolder() {
        return mFolder;
    }

    FolderInfo getFolderInfo() {
        return mInfo;
    }

    private boolean willAcceptItem(ItemInfo item) {
        final int itemType = item.itemType;
        return ((itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT) &&
                !mFolder.isFull() && item != mInfo && !mInfo.opened);
    }

    public boolean acceptDrop(Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
        return !mFolder.isDestroyed() && willAcceptItem(item);
    }

    public void addItem(ShortcutInfo item) {
        mInfo.add(item);
    }

    public void onDragEnter(Object dragInfo) {
        if (mFolder.isDestroyed() || !willAcceptItem((ItemInfo) dragInfo)) return;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) getLayoutParams();
        CellLayout layout = (CellLayout) getParent().getParent();
        mFolderIconAnimator.setCell(lp.cellX, lp.cellY);
        mFolderIconAnimator.setCellLayout(layout);
        mFolderIconAnimator.animateToAcceptState();
        layout.showFolderAccept(mFolderIconAnimator);
    }

    public void onDragOver(Object dragInfo) {
    }

    public void performCreateAnimation(final ShortcutInfo destInfo, final View destView,
            final ShortcutInfo srcInfo, final DragView srcView, Rect dstRect,
            float scaleRelativeToDragLayer, Runnable postAnimationRunnable, int icSize, int cellPadTop) {

        destInfo.spanX = destInfo.spanY = 1;
        srcInfo.spanX = srcInfo.spanY = 1;

        // These correspond two the drawable and view that the icon was dropped _onto_
        Drawable animateDrawable = ((TextView) destView).getCompoundDrawables()[1];
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(),
                destView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, INITIAL_ITEM_ANIMATION_DURATION, false, null, icSize, cellPadTop);
        addItem(destInfo);

        // This will animate the dragView (srcView) into the new folder
        onDrop(srcInfo, srcView, dstRect, scaleRelativeToDragLayer, 1, postAnimationRunnable, null);
    }

    public void performDestroyAnimation(final View finalView, Runnable onCompleteRunnable, int icSize, int cellPadTop) {
        Drawable animateDrawable = ((TextView) finalView).getCompoundDrawables()[1];
        computePreviewDrawingParams(animateDrawable.getIntrinsicWidth(),
                finalView.getMeasuredWidth());

        // This will animate the first item from it's position as an icon into its
        // position as the first item in the preview
        animateFirstItem(animateDrawable, FINAL_ITEM_ANIMATION_DURATION, true,
                onCompleteRunnable, icSize, cellPadTop);
    }

    public void onDragExit(Object dragInfo) {
        onDragExit();
    }

    public void onDragExit() {
        mFolderIconAnimator.animateToNaturalState();
    }

    private void onDrop(final ShortcutInfo item, DragView animateView, Rect finalRect,
            float scaleRelativeToDragLayer, int index, Runnable postAnimationRunnable,
            DragObject d) {
        item.cellX = -1;
        item.cellY = -1;

        // Typically, the animateView corresponds to the DragView; however, if this is being done
        // after a configuration activity (ie. for a Shortcut being dragged from AllApps) we
        // will not have a view to animate
        if (animateView != null) {
            DragLayer dragLayer = mLauncher.getDragLayer();
            Rect from = new Rect();
            dragLayer.getViewRectRelativeToSelf(animateView, from);
            Rect to = finalRect;
            if (to == null) {
                to = new Rect();
                Workspace workspace = mLauncher.getWorkspace();
                // Set cellLayout and this to it's final state to compute final animation locations
                workspace.setFinalTransitionTransform((CellLayout) getParent().getParent());
                float scaleX = getScaleX();
                float scaleY = getScaleY();
                setScaleX(1.0f);
                setScaleY(1.0f);
                scaleRelativeToDragLayer = dragLayer.getDescendantRectRelativeToSelf(this, to);
                // Finished computing final animation locations, restore current state
                setScaleX(scaleX);
                setScaleY(scaleY);
                workspace.resetTransitionTransform((CellLayout) getParent().getParent());
            }

            int[] center = new int[2];
            float scale = getLocalCenterForIndex(index, center);
            center[0] = (int) Math.round(scaleRelativeToDragLayer * center[0]);
            center[1] = (int) Math.round(scaleRelativeToDragLayer * center[1]);

            to.offset(center[0] - animateView.getMeasuredWidth() / 2,
                    center[1] - animateView.getMeasuredHeight() / 2);

            float finalAlpha = index < NUM_ITEMS_IN_PREVIEW ? 0.5f : 0f;

            float finalScale = scale * scaleRelativeToDragLayer;
            dragLayer.animateView(animateView, from, to, finalAlpha,
                    1, 1, finalScale, finalScale, DROP_IN_ANIMATION_DURATION,
                    new DecelerateInterpolator(2), new AccelerateInterpolator(2),
                    postAnimationRunnable, DragLayer.ANIMATION_END_DISAPPEAR, null);
            addItem(item);
            mHiddenItems.add(item);
            mFolder.hideItem(item);
            postDelayed(new Runnable() {
                public void run() {
                    mHiddenItems.remove(item);
                    mFolder.showItem(item);
                    invalidate();
                }
            }, DROP_IN_ANIMATION_DURATION);
        } else {
            addItem(item);
        }
    }

    public void onDrop(DragObject d) {
        ShortcutInfo item;
        if (d.dragInfo instanceof ApplicationInfo) {
            // Came from all apps -- make a copy
            item = ((ApplicationInfo) d.dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo) d.dragInfo;
        }
        item.spanX = item.spanY = 1;

        mFolder.notifyDrop();
        onDrop(item, d.dragView, null, 1.0f, mInfo.contents.size(), d.postAnimationRunnable, d);
    }

    public DropTarget getDropTargetDelegate(DragObject d) {
        return null;
    }

    private void computePreviewDrawingParams(int drawableSize, int totalSize) {
        if (mIntrinsicIconSize != drawableSize) {
            mIntrinsicIconSize = drawableSize;
        }
    }

    private void computePreviewDrawingParams(Drawable d) {
        computePreviewDrawingParams(d.getIntrinsicWidth(), getMeasuredWidth());
    }

    class PreviewItemDrawingParams {
        PreviewItemDrawingParams(float transX, float transY, float scale) {
            this.transX = transX;
            this.transY = transY;
            this.scale = scale;
        }
        float transX;
        float transY;
        float scale;
        Drawable drawable;
    }

    private float getLocalCenterForIndex(int index, int[] center) {
        mParams = computePreviewItemDrawingParams(Math.min(NUM_ITEMS_IN_PREVIEW, index), mParams);

        float offsetX = mParams.transX + (PREVIEW_ICON_SIZE * mScale) / 2;
        float offsetY = mParams.transY + (PREVIEW_ICON_SIZE * mScale) / 2;

        center[0] = (int) Math.round(offsetX);
        center[1] = (int) Math.round(offsetY);
        return mParams.scale;
    }

    private float mScale;

    private PreviewItemDrawingParams computePreviewItemDrawingParams(int index,
            PreviewItemDrawingParams params) {

        float transX;
        float transY;
        if (index == 0) {
            transX = (int) Math.ceil(mTotalWidth / 2f - (PREVIEW_ICON_SIZE  + 6) * mScale);
            transY = (int) Math.ceil(mFolderIconPadTop + 24 * mScale);

        } else if (index == 1) {
            transX = (int) Math.ceil(mTotalWidth / 2f + 6 * mScale);
            transY = (int) Math.ceil(mFolderIconPadTop + 24 * mScale);

        } else if (index == 2) {
            transX = (int) Math.ceil(mTotalWidth / 2f - (PREVIEW_ICON_SIZE  + 6) * mScale);
            transY = (int) Math.ceil(mFolderIconPadTop + (24 + PREVIEW_ICON_SIZE  + 12) * mScale);

        } else if (index == 3){
            transX = (int) Math.ceil(mTotalWidth / 2f + 6 * mScale);
            transY = (int) Math.ceil(mFolderIconPadTop + (24 + PREVIEW_ICON_SIZE  + 12) * mScale);

        } else {
             transX = (int) Math.ceil(mTotalWidth / 2f - (PREVIEW_ICON_SIZE )  * mScale / 2);
             transY = (int) Math.ceil(mFolderIconPadTop + (24 + (PREVIEW_ICON_SIZE / 2)  + 6) * mScale);
        }

        if (mHoverAnmating) {
            if (index < 4) {
                float scaleCenterX = mTotalWidth / 2f;
                float scaleCenterY = mFolderIconPadTop + (24 + PREVIEW_ICON_SIZE + 6) * mScale;
                float delta = PREVIEW_ICON_SIZE * mScale * mHoverAnimationScaleFactor / 2 - PREVIEW_ICON_SIZE * mScale / 2;

                transX = mHoverAnimationScaleFactor * transX + (1 - mHoverAnimationScaleFactor) * scaleCenterX + delta;
                transY = mHoverAnimationScaleFactor * transY + (1 - mHoverAnimationScaleFactor) * scaleCenterY + delta;
            }
        }

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, 1f);
        } else {
            params.transX = transX;
            params.transY = transY;
            params.scale = 1f;
        }
        return params;
    }

    private void drawPreviewItem(Canvas canvas, PreviewItemDrawingParams params) {
        canvas.save();
        canvas.translate(params.transX, params.transY);
        Drawable d = params.drawable;

        if (d != null) {
            final int peviewSize = (int) Math.floor(PREVIEW_ICON_SIZE * mScale * params.scale);
            d.setBounds(0, 0, peviewSize, peviewSize);
//            d.setFilterBitmap(true);
            d.setColorFilter(Color.argb(20, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            d.draw(canvas);
            d.clearColorFilter();
//            d.setFilterBitmap(false);

        }
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mFolder == null) return;
        if (mFolder.getItemCount() == 0 && !mAnimating) return;

        ArrayList<View> items = mFolder.getItemsInReadingOrder();
        Drawable d;
        TextView v;

        // Update our drawing parameters if necessary
        if (mAnimating) {
            computePreviewDrawingParams(mAnimParams.drawable);
        } else {
            v = (TextView) items.get(0);
            d = v.getCompoundDrawables()[1];
            computePreviewDrawingParams(d);
        }

        int nItemsInPreview = Math.min(items.size(), NUM_ITEMS_IN_PREVIEW);
        if (!mAnimating) {
            for (int i = nItemsInPreview - 1; i >= 0; i--) {
                v = (TextView) items.get(i);
                if (!mHiddenItems.contains(v.getTag())) {
                    d = v.getCompoundDrawables()[1];
                    mParams = computePreviewItemDrawingParams(i, mParams);
                    mParams.drawable = d;
                    drawPreviewItem(canvas, mParams);
                }
            }
        } else {
            drawPreviewItem(canvas, mAnimParams);
        }
    }

    private void animateFirstItem(final Drawable d, int duration, final boolean reverse,
            final Runnable onCompleteRunnable, int icSize, int cellPadTop) {
        final PreviewItemDrawingParams finalParams = computePreviewItemDrawingParams(0, null);

        final float scale0 = icSize / (PREVIEW_ICON_SIZE * mScale);
        final float transX0 = (mTotalWidth - icSize) / 2;
        final float transY0 = cellPadTop;

        mAnimParams.drawable = d;

        ValueAnimator va = ViewAnimUtils.ofFloat(this, 0f, 1.0f);
        va.addUpdateListener(new AnimatorUpdateListener(){
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = (Float) animation.getAnimatedValue();
                if (reverse) {
                    progress = 1 - progress;
                    mPreviewBackground.setAlpha(progress);
                }

                mAnimParams.transX = transX0 + progress * (finalParams.transX - transX0);
                mAnimParams.transY = transY0 + progress * (finalParams.transY - transY0);
                mAnimParams.scale = scale0 + progress * (finalParams.scale - scale0);
                invalidate();
            }
        });
        va.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                mAnimating = true;
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimating = false;
                if (onCompleteRunnable != null) {
                    onCompleteRunnable.run();
                }
            }
        });
        va.setDuration(duration);
        va.start();
    }

    public void setTextVisible(boolean visible) {
        if (visible) {
            mFolderName.setVisibility(VISIBLE);
        } else {
            mFolderName.setVisibility(INVISIBLE);
        }
    }

    public boolean getTextVisible() {
        return mFolderName.getVisibility() == VISIBLE;
    }

    public void onItemsChanged() {
        invalidate();
        requestLayout();
    }

    public void onAdd(ShortcutInfo item) {
        invalidate();
        requestLayout();
    }

    public void onRemove(ShortcutInfo item) {
        invalidate();
        requestLayout();
    }

    public void onTitleChanged(CharSequence title) {
        mFolderName.setText(title.toString());
        setContentDescription(String.format(getContext().getString(R.string.folder_name_format),
                title));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Call the superclass onTouchEvent first, because sometimes it changes the state to
        // isPressed() on an ACTION_UP
        boolean result = super.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLongPressHelper.postCheckForLongPress();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLongPressHelper.cancelLongPress();
                break;
        }
        return result;
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mLongPressHelper.cancelLongPress();
    }

    public void setFolderLayout(int folderIcSize, int paddingTop, float scale, int totalWidth, int textPadding) {

        mScale = scale;
        mFolderIconSize = folderIcSize;
        mFolderIconPadTop = paddingTop;
        mTotalWidth = totalWidth;

        final ImageView bg = mPreviewBackground;
        final int bmpWidth = ((BitmapDrawable)bg.getDrawable()).getBitmap().getWidth();

        final float folderIconScale = folderIcSize / (float) bmpWidth;
        bg.setScaleX(folderIconScale);
        bg.setScaleY(folderIconScale);

        // スケール変更分を調節
        paddingTop -= (bmpWidth - folderIcSize) / 2;

        // 均等にパディングをいれないとスケール変更でアイコンの位置がずれる
        bg.setPadding(paddingTop, paddingTop, paddingTop, paddingTop);

        mFolderName.setPadding(textPadding, (int) Math.ceil(204 * mScale), textPadding, 0);
    }
}
