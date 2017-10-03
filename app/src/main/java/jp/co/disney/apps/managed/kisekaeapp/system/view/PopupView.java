package jp.co.disney.apps.managed.kisekaeapp.system.view;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

public class PopupView extends FrameLayout {

    private static final long BACKGROUND_ANI_DURATION_SHOW = 230L;
    private static final long BACKGROUND_ANI_DURATION_DISMISS = 200L;

    public static final int OUTSIDE_DONT_TOUCH = 1;
    public static final int OUTSIDE_DONT_TOUCH_WITH_CANCEL = 2;
    public static final int OUTSIDE_TOUCH = 0;

    private boolean mActive;
    private View mContentView;
    private PopupLayerView mParent;
    private boolean mDispatchingOnShow;
    private boolean mDoingHideAnimation;
    private boolean mDoingShowAnimation;
    private List<PopupListener> mListeners;
    private LayoutParams mLp;
    private Animator mOnDismissAnimator;
    private Animator mOnShowAnimator;
    private boolean mOnTop;
    private int mOutsideTouchable;
    private Rect mTmpRect;

    private ViewGroup.LayoutParams mPopupViewParams;

    public boolean isShow() {
        return mActive || mDoingShowAnimation || mDoingHideAnimation;
    }

    PopupView(Context context, View contentView, PopupLayerView parent) {
        super(context);

        mParent = parent;
        mListeners = new ArrayList<PopupListener>();
        mTmpRect = new Rect();
        mOutsideTouchable = OUTSIDE_DONT_TOUCH_WITH_CANCEL;
        mDoingShowAnimation = false;
        mDoingHideAnimation = false;
        mContentView = contentView;
        mPopupViewParams = contentView.getLayoutParams();

        if(mPopupViewParams != null) {
            if(mPopupViewParams instanceof FrameLayout.LayoutParams) {
                mLp = new LayoutParams((FrameLayout.LayoutParams) mPopupViewParams);
            } else {
                if(mPopupViewParams instanceof ViewGroup.MarginLayoutParams) {
                    mLp = new LayoutParams((ViewGroup.MarginLayoutParams) mPopupViewParams);
                } else {
                    mLp = new LayoutParams(mPopupViewParams);
                }
            }
        } else {
            mLp = new LayoutParams();
        }

        setFocusable(true);
        setFocusableInTouchMode(true);
        mOnTop = false;
    }

    public void addListener(PopupListener popuplistener) {
        mListeners.add(popuplistener);
    }

    public void dismiss(boolean animate, long dismissDuration) {

        if(getParent() == null || !mActive && !mDoingShowAnimation) {
            return;
        }

        if (mDoingShowAnimation) {
            if (mOnShowAnimator != null) {
                if(mOnShowAnimator.isRunning()) {
                    mOnShowAnimator.cancel();
                }
            }
        }

        mActive = false;
        mDispatchingOnShow = false;
        Iterator<PopupListener> iterator = mListeners.iterator();

        while (iterator.hasNext()) {
            iterator.next().onDismissRequested();
        }

        if(animate && mOnDismissAnimator != null) {
            mOnDismissAnimator.setTarget(mContentView);
            mOnDismissAnimator.start();
            mDoingHideAnimation = true;
        } else {
            finishDismiss();
        }

        if (animate) {
            startBgDrawableAnimation(1.0f, 0.0f, dismissDuration);
        }
    }

    public View getContentView() {
        return mContentView;
    }

    public void setLayerBackground(Drawable drawable) {
        setBackgroundDrawable(drawable);
    }

    public void setOnDismissAnimator(Animator animator, AnimatorListener animatorlistener) {
        mOnDismissAnimator = animator;
        if(mOnDismissAnimator != null)
            mOnDismissAnimator.addListener(new OnDismissAnimListener(animatorlistener));
    }

    public void setOnShowAnimator(Animator animator, AnimatorListener animatorlistener) {
        mOnShowAnimator = animator;
        if(mOnShowAnimator != null)
            mOnShowAnimator.addListener(new OnShowAnimListener(animatorlistener));
    }

    public void show() {
        show(false);
    }

    public void show(boolean top) {

        boolean notActive = (getParent() == null && !mActive);
        boolean dismissing = (getParent() != null && mActive && mDoingHideAnimation);
        if(notActive || dismissing) {

            if(dismissing) {
                if(mOnDismissAnimator != null) {
                    mOnDismissAnimator.cancel();
                }
            }

            Rect rect = mTmpRect;
            mParent.getGlobalVisibleRect(rect);
            LayoutParams layoutparams = new LayoutParams(mLp);
            if(layoutparams.gravity <= 0) {
                layoutparams.gravity = 51;
                layoutparams.leftMargin = layoutparams.x - rect.left;
                layoutparams.topMargin = layoutparams.y - rect.top;
            }

            final Rect contentPadding = mParent.getContentPadding();
            setPadding(contentPadding.left, contentPadding.top, contentPadding.right, contentPadding.bottom);
            setClipToPadding(false);

            addView(mContentView, layoutparams);
            mOnTop = top;
            if(top) {
                mParent.addView(this, new FrameLayout.LayoutParams(-1, -1));
                requestFocus();
            } else {

                int index_minTop = -1;
                final int count = mParent.getChildCount();
                for (int i = count - 1; i >= 0; i--) {
                    if (!((PopupView) mParent.getChildAt(count)).mOnTop) {
                        break;
                    }
                    index_minTop = i;
                }

                mParent.addView(this, index_minTop, new FrameLayout.LayoutParams(-1, -1));
                if(index_minTop == -1) {
                    requestFocus();
                }
            }

            if(mOnShowAnimator != null) {
                mOnShowAnimator.setTarget(mContentView);
                mOnShowAnimator.start();
                mDoingShowAnimation = true;

            } else {
                mActive = true;
            }

            startBgDrawableAnimation(0.0f, 1.0f, BACKGROUND_ANI_DURATION_SHOW);
            mDispatchingOnShow = true;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionevent) {
        if(!mActive) return true;
        return super.onInterceptTouchEvent(motionevent);
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyevent) {
        return i == KeyEvent.KEYCODE_BACK;
    }

    @Override
    public boolean onKeyUp(int i, KeyEvent keyevent) {
        if(i == KeyEvent.KEYCODE_BACK) {
            cancel(true, BACKGROUND_ANI_DURATION_DISMISS);
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(!mDispatchingOnShow) return;

        mDispatchingOnShow = false;

        Iterator<PopupListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            iterator.next().onShow();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent me) {

        if(me.getAction() != MotionEvent.ACTION_DOWN  || !mActive) {
            return false;
        }

        int x = (int) me.getX();
        int y = (int) me.getY();

        mContentView.getHitRect(mTmpRect);
        if(mTmpRect.contains(x, y)) {
            return true;
        }

        if(mOutsideTouchable == OUTSIDE_DONT_TOUCH_WITH_CANCEL) {
            cancel(true, BACKGROUND_ANI_DURATION_DISMISS);
            return true;
        }

        if(mOutsideTouchable == OUTSIDE_DONT_TOUCH) {
            return true;
        }

        if(isDoingShowAnimation() || isDoingDismissAnimation()) {
            return true;
        }

        return false;
    }

    public void cancel(boolean animate, long dismissDuration) {

        if (!mActive && !mDoingShowAnimation || getParent() == null) return;

        Iterator<PopupListener> iterator = mListeners.iterator();
        while(iterator.hasNext()) {
            iterator.next().onCancelled();
        }

        dismiss(animate, dismissDuration);
    }

    private void finishDismiss() {

        removeView(mContentView);
        mParent.removeView(this);

        final int count = mParent.getChildCount();
        if(count > 0) {
            mParent.getChildAt(count - 1).requestFocus();
        }

        Iterator<PopupListener> iterator = mListeners.iterator();
        while(iterator.hasNext()) {
            iterator.next().onDismiss();
        }
    }

    private boolean isDoingDismissAnimation() {

        boolean ret = false;

        if(mOnDismissAnimator != null) {
            ret = mOnDismissAnimator.isRunning();
        }

        return ret;
    }

    private boolean isDoingShowAnimation() {

        boolean ret = false;

        if(mOnShowAnimator != null) {
            ret = mOnShowAnimator.isRunning();
        }

        return ret;
    }

    private void startBgDrawableAnimation(float start, float end, long duration) {

        if(getBackground() != null) {

            ValueAnimator valueanimator = new ValueAnimator();
            valueanimator.setInterpolator(new AccelerateInterpolator());
            valueanimator.setDuration(duration);
            valueanimator.setFloatValues(new float[] { start, end });
            valueanimator.addUpdateListener(new android.animation.ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueanimator) {

                    float t = (Float)valueanimator.getAnimatedValue();

                    Drawable drawable = getBackground();
                    if(drawable != null) {
                        drawable.setAlpha((int)(255 * t));
                    }

                    invalidate();
                }

            });
            valueanimator.start();
        }
    }

    private class AnimListenerDelegator implements Animator.AnimatorListener {

        @Override
        public void onAnimationCancel(Animator animator) {
            if(originAnimatorListener != null)
                originAnimatorListener.onAnimationCancel(animator);
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            if(originAnimatorListener != null)
                originAnimatorListener.onAnimationEnd(animator);
        }

        @Override
        public void onAnimationRepeat(Animator animator) {
            if(originAnimatorListener != null)
                originAnimatorListener.onAnimationRepeat(animator);
        }

        @Override
        public void onAnimationStart(Animator animator) {
            if(originAnimatorListener != null)
                originAnimatorListener.onAnimationStart(animator);
        }

        private Animator.AnimatorListener originAnimatorListener;

        public AnimListenerDelegator(Animator.AnimatorListener animatorlistener) {
            super();
            originAnimatorListener = animatorlistener;
        }
    }

    private class LayoutParams extends FrameLayout.LayoutParams {

        int x;
        int y;

        LayoutParams() {
            super(-1, -1);
        }

        LayoutParams(ViewGroup.LayoutParams layoutparams) {
            super(layoutparams);
        }

        LayoutParams(ViewGroup.MarginLayoutParams marginlayoutparams) {
            super(marginlayoutparams);
        }

        LayoutParams(FrameLayout.LayoutParams layoutparams) {
            super((ViewGroup.MarginLayoutParams) layoutparams);
            gravity = layoutparams.gravity;
        }

        LayoutParams(LayoutParams layoutparams) {
            this(((FrameLayout.LayoutParams) (layoutparams)));
            x = layoutparams.x;
            y = layoutparams.y;
        }
    }

    private class OnDismissAnimListener extends AnimListenerDelegator {

        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            mDoingHideAnimation = false;
            finishDismiss();
        }

        public OnDismissAnimListener(Animator.AnimatorListener animatorlistener) {
            super(animatorlistener);
        }
    }

    private class OnShowAnimListener extends AnimListenerDelegator {

        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);
            mActive = true;
            mDoingShowAnimation = false;
        }

        public OnShowAnimListener(Animator.AnimatorListener animatorlistener) {
            super(animatorlistener);
        }
    }

    public static interface PopupListener {

        public abstract void onCancelled();

        public abstract void onDismiss();

        public abstract void onDismissRequested();

        public abstract void onShow();
    }
}
