package jp.co.disney.apps.managed.kisekaeapp.launcher;

import com.badlogic.gdx.utils.Array;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.OverScroller;
import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsOperator;
import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ContentsTypeValue;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;

public class ThemeHistoryMenuView extends View {

    private static final String TAG = "ThemeHistoryMenuView";

    private static final float NANOTIME_DIV = 1000000000.0f;

    private static final float SMOOTHING_SPEED = 0.75f;
    private static final float SMOOTHING_CONSTANT = (float) (0.016 / Math.log(SMOOTHING_SPEED));

    private static final int TOUCH_STATE_REST = 0;
    private static final int TOUCH_STATE_SCROLLING = 1;

    private static final float OVERSCROLL_DAMP_FACTOR = 0.07f;
    private static final int FLING_THRESHOLD_VELOCITY = 100;

    private static final int MIN_LENGTH_FOR_FLING = 100;

    private static final int MAXIMUM_VELOCITY = 3200;

    private float mDensity;

    private int mMaxScrollX;

    private int mFlingThresholdVelocity;

    private OverScroller mScroller;

    private int mTouchState = TOUCH_STATE_REST;
    private float mDownMotionX;
    private float mDownMotionY;
    private float mLastMotionX;
    private float mLastMotionY;
    private float mLastMotionXRemainder;
    private float mSmoothingTime;
    private float mTouchX;
    private int mOverScrollX;
    private int mUnboundedScrollX;
    private float mTotalMotionX;

    private int mPaddingTop;
    private int mThumbSize;
    private int mGapY;
    private int mGapX;

    private Paint mPaint = new Paint();

    private int mItemCount = 0;

    private OnItemSelectListener mOnItemSelectListener;

    public void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
        mOnItemSelectListener = onItemSelectListener;
    }

    public interface OnItemSelectListener {
        public void onItemSelect(int index, String themeId);
    }

    public ThemeHistoryMenuView(Context context) {
        this(context, null);
    }

    public ThemeHistoryMenuView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public ThemeHistoryMenuView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);

        mScroller = new OverScroller(context);
        mDensity = context.getResources().getDisplayMetrics().density;
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);
    }

    private FastBitmapDrawable[] mThumbs = new FastBitmapDrawable[10];

    private BitmapDrawable mCheckMark;

    private String mNoHistoryText;
    private int mNoHistoryTextSize;
    private float mNoHistoryTextWidth;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final Resources res = getContext().getResources();
//        final DisplayMetrics dm = res.getDisplayMetrics();

        final int width = MeasureSpec.getSize(widthMeasureSpec);

        mThumbSize = (int) Math.ceil(324 * width / 1080f);
        mPaddingTop = (int) Math.ceil(98 * width / 1080f);
        int paddingBottom = (int) Math.ceil(72 * width / 1080f);
        mGapY = (int) Math.ceil(54 * width / 1080f);

        final int height = mThumbSize * 2 + mPaddingTop + paddingBottom + mGapY;

        this.setMeasuredDimension(width, height);

        scrollTo(0, 0);

        final Array<Long> assetIdList = ContentsOperator.op.getHistoryContentsAssetID(
                getContext(), ContentsTypeValue.CONTENTS_TYPE_THEME);

        final int itemCount = assetIdList.size;
//        final int itmeCount2 = assetIdList.size;

        mItemCount = itemCount;

        mThemeIds = new String[itemCount];
        mTestT = new float[itemCount];
        mTestT2 = new float[itemCount];

        mGapX = (int) Math.ceil(width * 27 / 1080f);
        final int numColumn = itemCount / 2 + itemCount % 2;
        mMaxScrollX = (mThumbSize * numColumn) + (mGapX * (numColumn + 1)) - width;
        if (mMaxScrollX < 0) {
            mMaxScrollX = 0;
        }

        if (itemCount == 0) {
            mNoHistoryText = res.getString(R.string.err_msg_no_theme_history);
            mNoHistoryTextSize = (int) Math.floor(48 * mDensity / 3);
            mPaint.setColor(Color.argb(255, 71, 71, 71));
            mPaint.setTextSize(mNoHistoryTextSize);
            mNoHistoryTextWidth = mPaint.measureText(mNoHistoryText);
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {

                for (int i = 0; i < itemCount; i++) {

                    Bitmap bmp = null;

                    Long id = assetIdList.get(i);

                    String thumbnailPath = ContentsOperator.op.getHistoryThumbnailImagePath(getContext(),
                            ContentsTypeValue.CONTENTS_TYPE_THEME, id);

                    bmp = ThemeUtils.loadThemeBackground(getContext(), thumbnailPath, mThumbSize);
                    mThemeIds[i] = id.toString();

                    if (bmp == null) {
                        continue;
                    }

                    mThumbs[i] = new FastBitmapDrawable(bmp);
                }

                mCheckMark = (BitmapDrawable) ContextCompat.getDrawable(getContext(), R.drawable.check_mark);

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {

                for (int i = 0; i < itemCount; i++) {

                    final int j = i;

                    ValueAnimator valAnim = ValueAnimator.ofFloat(0.0f, 1.0f);
                    valAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            mTestT[j] = (Float) animation.getAnimatedValue();
                            invalidate();
                        }
                    });
                    valAnim.setInterpolator(new OvershootInterpolator());
                    valAnim.setDuration(300L);
                    valAnim.setStartDelay(100L * i);
                    valAnim.start();
                }

            }
        }.execute();
    }

    private ValueAnimator mShowAnim;

    public void cancelAnim() {

        if (mShowAnim != null) {
            mShowAnim.cancel();
        }
    }

    public void setTestT(int i, float val) {
        if (mTestT2 == null) return;

        mTestT2[i] = val;
    }

    public float getTestT(int i) {
        if (mTestT2 == null) return 1.0f;

        return mTestT2[i];
    }

    private float[] mTestT;

    private float[] mTestT2;

    private String[] mThemeIds;

    int getItemCount() {
        return mItemCount;
    }

    private int positionToItemIndex(float x, float y) {

        final int thumbSize = mThumbSize;
        final int radius = mThumbSize / 2;
        final int gap = mGapX;

        final int itemCount = mItemCount;
        final int numColumn = itemCount / 2 + itemCount % 2;

        for (int i = 0; i < numColumn; i++) {
            int cx = gap + radius + ((thumbSize + gap) * i);
            if (x >= cx - radius && x < cx + radius) {
                if (y >= mPaddingTop && y < thumbSize) {
                    return i * 2;

                } else if (y >= mPaddingTop + thumbSize + mGapY && y < mPaddingTop + thumbSize + mGapY + thumbSize) {

                    if (i == numColumn - 1 && itemCount % 2 == 1) {
                        return -1;
                    }

                    return i * 2 + 1;
                }
                return -1;
            }
        }

        return -1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int scrollX = getScrollX();
        final int dspWidth = getMeasuredWidth();

        if (mItemCount == 0) {
            float tw = mNoHistoryTextWidth;
            canvas.drawText(mNoHistoryText, (canvas.getWidth() - tw) / 2 + scrollX,
                    (canvas.getHeight() - (mPaint.descent() + mPaint.ascent())) / 2, mPaint);
            return;
        }

        for (int i = 0; i < mItemCount; i++) {

            if (mThumbs[i] == null) continue;

            int x = i / 2;

            int left = mGapX * (x + 1) + mThumbSize * x;

            if (left + mThumbSize < scrollX) continue;
            if (left > scrollX + dspWidth) break;

            int top;
            if (i % 2 == 0) {
                top = mPaddingTop;
            } else {
                top = mPaddingTop + mThumbSize + mGapY;
            }

            int delta = (int) Math.ceil((1.0f - mTestT[i]) * mThumbSize / 2);

//            mThumbs[i].setBounds(left, top, left + mThumbSize, top + mThumbSize);
            mThumbs[i].setBounds(left + delta, top + delta, left + mThumbSize - delta, top + mThumbSize - delta);
            mThumbs[i].setAlpha((int) Math.ceil(255 * (1.0f - mTestT2[i])));

            if (i == 0) {

                if (mCheckMark != null) {
                    int markSize = mThumbSize / 3;
                    int markDelta = (int) Math.ceil(((1.0f - mTestT[i]) * markSize / 2) + ((mThumbSize - markSize) / 2f));
                    mCheckMark.setBounds(left + markDelta, top + markDelta, left + mThumbSize - markDelta, top + mThumbSize - markDelta);
                    mCheckMark.setAlpha((int) Math.ceil(255 * (1.0f - mTestT2[i])));

                    if (i == mTouchedItem) {
                        mCheckMark.setColorFilter(Color.rgb(255, 39, 166), PorterDuff.Mode.SRC_ATOP);
                    } else {
                        mCheckMark.clearColorFilter();
                    }
                }

                mThumbs[i].setColorFilter(Color.argb(130, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            } else if (i == mTouchedItem) {
                mThumbs[i].setColorFilter(Color.argb(130, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
            } else {
                mThumbs[i].clearColorFilter();
            }

            mThumbs[i].draw(canvas);
        }

        if (mCheckMark != null) {
            mCheckMark.draw(canvas);
        }
    }

    @Override
    public void computeScroll() {

        boolean scrollComputed = computeScrollHelper();

        if (!scrollComputed && mTouchState == TOUCH_STATE_SCROLLING) {
            final float now = System.nanoTime() / NANOTIME_DIV;
            final float e = (float) Math.exp((now - mSmoothingTime) / SMOOTHING_CONSTANT);

            final float dx = mTouchX - mUnboundedScrollX;
            scrollTo(Math.round(mUnboundedScrollX + dx * e), getScrollY());
            mSmoothingTime = now;

            if (dx > 1.f || dx < -1.f) {
                invalidate();
            }
        }
    }

    private boolean computeScrollHelper() {
        if (mScroller.computeScrollOffset()) {
            if (getScrollX() != mScroller.getCurrX()
                || getScrollY() != mScroller.getCurrY()
                || mOverScrollX != mScroller.getCurrX()
                ) {
                scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            }
            invalidate();
            return true;
        }
        return false;
    }

    private VelocityTracker mVelocityTracker;

    private void acquireVelocityTrackerAndAddMovement(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private int mTouchedItem = -1;

    @Override
    public boolean onTouchEvent(MotionEvent me) {

        acquireVelocityTrackerAndAddMovement(me);

        float x = me.getX();
        float y = me.getY();

        switch (me.getAction()) {
        case MotionEvent.ACTION_DOWN:

            mDownMotionX = x;
            mDownMotionY = y;

            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            mLastMotionX = x;
            mLastMotionY = y;
            mLastMotionXRemainder = 0;
            mTotalMotionX = 0;

            mTouchedItem = positionToItemIndex(x + mUnboundedScrollX, y);
            if (mTouchedItem >= 0) {
                invalidate();
            }

            break;
        case MotionEvent.ACTION_MOVE:

            if (mTouchState == TOUCH_STATE_SCROLLING) {

                final float deltaX = mLastMotionX + mLastMotionXRemainder - x;

                mTotalMotionX += Math.abs(deltaX);

                if (Math.abs(deltaX) >= 1.0f) {
                    mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
                    mTouchX += deltaX;
                    scrollTo(mUnboundedScrollX + (int) deltaX, getScrollY());
                    mLastMotionX = x;
                    mLastMotionXRemainder = deltaX - (int) deltaX;
                }

            } else {
                determineScrollingStart(me);
            }

            break;
        case MotionEvent.ACTION_UP:

            if (mTouchState == TOUCH_STATE_SCROLLING) {

                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(400, MAXIMUM_VELOCITY);
                int velocityX = (int) velocityTracker.getXVelocity();

                boolean isFling = mTotalMotionX > MIN_LENGTH_FOR_FLING &&
                        Math.abs(velocityX) > mFlingThresholdVelocity;

                 if (mOverScrollX >= 0&& mOverScrollX <= mMaxScrollX && isFling) {
                     mScroller.fling(mUnboundedScrollX, 0, - velocityX, 0, 0, mMaxScrollX, 0, 0, mThumbSize, 0);
                 } else {
                     if (mOverScrollX < 0) {
                         mScroller.startScroll(mUnboundedScrollX, 0, -mUnboundedScrollX, 0, 550);
                     } else if (mOverScrollX > mMaxScrollX) {
                         mScroller.startScroll(mUnboundedScrollX, 0, mMaxScrollX - mUnboundedScrollX, 0, 550);
                     }
                 }

                mTotalMotionX += Math.abs(mLastMotionX + mLastMotionXRemainder - x);
            }

            int selectedItem = -1;
            boolean selectItem = false;
            if (mTouchedItem >= 0) {
                float deltaX = (mDownMotionX - x) / mDensity;
                if (Math.abs(deltaX) < 30f) {
                    int touchedItem = positionToItemIndex(x + mUnboundedScrollX, y);
                    if (mTouchedItem == touchedItem) {
                        selectedItem = mTouchedItem;
                        selectItem = true;
                    }
                }
            }

            mTouchedItem = -1;

            mTouchState = TOUCH_STATE_REST;

            invalidate();

            releaseVelocityTracker();

            if (selectItem) {
                if (mOnItemSelectListener != null) {
                    mOnItemSelectListener.onItemSelect(selectedItem, mThemeIds[selectedItem]);
                }
            }

            break;
        }

        return true;
    }

    private void determineScrollingStart(MotionEvent ev) {
        determineScrollingStart(ev, 1.0f);
    }

    private void determineScrollingStart(MotionEvent ev, float touchSlopScale) {

        final float x = ev.getX();
        final int xDiff = (int) Math.abs(x - mLastMotionX);

        final int touchSlop = Math.round(touchSlopScale * 8);

        boolean xMoved = xDiff > touchSlop;
        if (xMoved) {
            mTouchState = TOUCH_STATE_SCROLLING;
            mTotalMotionX += Math.abs(mLastMotionX - x);
            mLastMotionX = x;
            mLastMotionXRemainder = 0;
            mTouchX = getScrollX();
            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
        }
    }

    @Override
    public void scrollTo(int x, int y) {

        mUnboundedScrollX = x;

        final int maxScrollX = mMaxScrollX;

        if (x < 0) {
            super.scrollTo(0, y);
            overScroll(x, maxScrollX);

        } else if (x > maxScrollX) {
            super.scrollTo(maxScrollX, y);
            overScroll(x - maxScrollX, maxScrollX);

        } else {
            mOverScrollX = x;
            super.scrollTo(x, y);
        }

        mTouchX = x;
        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
    }

    private void overScroll(float amount, int maxScrollX) {

        int screenSize = getMeasuredWidth();

        float f = (amount / screenSize);

        if (f == 0) return;
        f = f / (Math.abs(f)) * (overScrollInfluenceCurve(Math.abs(f)));

        if (Math.abs(f) >= 1) {
            f /= Math.abs(f);
        }

        int overScrollAmount = (int) Math.round(OVERSCROLL_DAMP_FACTOR * f * screenSize);
        if (amount < 0) {
            mOverScrollX = overScrollAmount;
            super.scrollTo(mOverScrollX, getScrollY());
        } else {
            mOverScrollX = maxScrollX + overScrollAmount;
            super.scrollTo(mOverScrollX, getScrollY());
        }
        invalidate();
    }

    private float overScrollInfluenceCurve(float f) {
        f -= 1.0f;
        return f * f * f + 1.0f;
    }
}
