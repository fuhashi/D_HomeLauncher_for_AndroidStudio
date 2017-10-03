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

import jp.co.disney.apps.managed.kisekaeapp.R;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

/*
 * Ths bar will manage the transition between the QSB search bar and the delete drop
 * targets so that each of the individual IconDropTargets don't have to.
 */
public class SearchDropTargetBar extends FrameLayout implements DragController.DragListener {

    private static final int sTransitionInDuration = 200;
    private static final int sTransitionOutDuration = 175;

    private ObjectAnimator mDropTargetBarAnim;
    private static final AccelerateInterpolator sAccelerateInterpolator =
            new AccelerateInterpolator();

    private View mDropTargetBar;
    private DeleteDropTarget mDeleteDropTarget;
    private int mBarHeight;
    private boolean mDeferOnDragEnd = false;

    private boolean mEnableDropDownDropTargets;

    private Launcher mLauncher;

    public SearchDropTargetBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchDropTargetBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setup(Launcher launcher, DragController dragController) {
        dragController.addDragListener(this);
        dragController.addDragListener(mDeleteDropTarget);
        dragController.addDropTarget(mDeleteDropTarget);
        mLauncher = launcher;
        mDeleteDropTarget.setLauncher(launcher);
    }

    public void enableDeleteDropTarget(DragController dragController) {
        if (dragController.isDropTarget(mDeleteDropTarget)) return;
        dragController.addDropTarget(mDeleteDropTarget);
    }

    public void disableDeleteDropTarget(DragController dragController) {
        if (dragController.isDropTarget(mDeleteDropTarget)) {
            dragController.removeDropTarget(mDeleteDropTarget);
        }
    }

    private void prepareStartAnimation(View v) {
        // Enable the hw layers before the animation starts (will be disabled in the onAnimationEnd
        // callback below)
        v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    private void setupAnimation(ObjectAnimator anim, final View v) {
        anim.setInterpolator(sAccelerateInterpolator);
        anim.setDuration(sTransitionInDuration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                v.setLayerType(View.LAYER_TYPE_NONE, null);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Get the individual components
        mDropTargetBar = findViewById(R.id.drag_target_bar);
        mDeleteDropTarget = (DeleteDropTarget) mDropTargetBar.findViewById(R.id.delete_target_text);

        mDeleteDropTarget.setSearchDropTargetBar(this);

        mEnableDropDownDropTargets =
            getResources().getBoolean(R.bool.config_useDropTargetDownTransition);

        mBarHeight = mDeleteDropTarget.getStaticHeight();

        // Create the various fade animations
        if (mEnableDropDownDropTargets) {
            mDropTargetBar.setTranslationY(-mBarHeight);
            mDropTargetBarAnim = ObjectAnimator.ofFloat(mDropTargetBar, "translationY", -mBarHeight, 0f);
        } else {
            mDropTargetBar.setAlpha(0f);
            mDropTargetBarAnim = ObjectAnimator.ofFloat(mDropTargetBar, "alpha", 0f, 1f);
        }
        setupAnimation(mDropTargetBarAnim, mDropTargetBar);
    }

    public void finishAnimations() {
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.reverse();
    }

    /*
     * Gets various transition durations.
     */
    public int getTransitionInDuration() {
        return sTransitionInDuration;
    }
    public int getTransitionOutDuration() {
        return sTransitionOutDuration;
    }

    /*
     * DragController.DragListener implementation
     */
    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
        if (mLauncher.isPageEditVisible()) return;

        // Animate out the QSB search bar, and animate in the drop target bar
        prepareStartAnimation(mDropTargetBar);
        mDropTargetBarAnim.start();
    }

    public void deferOnDragEnd() {
        mDeferOnDragEnd = true;
    }

    @Override
    public void onDragEnd() {
        if (!mDeferOnDragEnd) {

            if (mLauncher.isPageEditVisible()) return;

            // Restore the QSB search bar, and animate out the drop target bar
            prepareStartAnimation(mDropTargetBar);
            mDropTargetBarAnim.reverse();
        } else {
            mDeferOnDragEnd = false;
        }
    }
}
