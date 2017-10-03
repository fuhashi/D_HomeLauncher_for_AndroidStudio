package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.OverScroller;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class DrawerFilterView extends View {

    private static final String TAG = "DrawerFilterView";

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
    private int mDspWidth;
    private int mHeight;
    private int mLowerBtnWidth;
    private int mLowerBtnHeight;
    private int mLowerLeftBtnLeft;
    private int mLowerCenterBtnLeft;
    private int mLowerRightBtnLeft;
    private int mLowerBtnTop;
    private int mBtnGap;
    private int mBtnTextSize;
    private int mTxtGap;
    private int mBtnPadX;

    private int mMaxScrollX_alphabet;
    private int mMaxScrollX_kana;

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

    private int mTouchedLowerBtn = -1;
    private int mTouchedUpperBtn = -1;

    private int mSelectedLowerBtn = 0;
    private int mSelectedUpperBtn_alphabet = -1;
    private int mSelectedUpperBtn_kana = -1;

    private BitmapDrawable mUpperBtnBg;
    private Drawable mLowerBtnBg;

    private BitmapDrawable[] mAlphabets = new BitmapDrawable[26];
    private BitmapDrawable[] mKanas = new BitmapDrawable[10];
    private BitmapDrawable mEdge;

    private Bitmap mEdgeBitmap_left;
    private Canvas mEdgeCanvas_left;
    private Bitmap mEdgeBitmap_right;
    private Canvas mEdgeCanvas_right;

    public interface OnChangedListener {
        public void onFilterChanged(int filterCategory, int filterAlphabet, int filterKana);
    }

    private OnChangedListener mOnChangedListener;

    public void setOnChangedListener(OnChangedListener onChangedListener) {
        mOnChangedListener = onChangedListener;
    }

    public DrawerFilterView(Context context) {
        super(context);
        init(context);
    }

    public DrawerFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DrawerFilterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void reset() {
        mTouchedLowerBtn = -1;
        mTouchedUpperBtn = -1;
        mSelectedLowerBtn = 0;
        mSelectedUpperBtn_alphabet = -1;
        mSelectedUpperBtn_kana = -1;
        scrollTo(0, 0);
    }

    private void init(Context ctx) {
        mScroller = new OverScroller(ctx);
        mDensity = ctx.getResources().getDisplayMetrics().density;
        mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY * mDensity);

        mUpperBtnBg = (BitmapDrawable) ContextCompat.getDrawable(ctx, R.drawable.ic_drawer_smalltab);
        mLowerBtnBg = ContextCompat.getDrawable(ctx, R.drawable.drawer_tab);

        final int[] alphabetIds = {
            R.drawable.f_alpha_1,
            R.drawable.f_alpha_2,
            R.drawable.f_alpha_3,
            R.drawable.f_alpha_4,
            R.drawable.f_alpha_5,
            R.drawable.f_alpha_6,
            R.drawable.f_alpha_7,
            R.drawable.f_alpha_8,
            R.drawable.f_alpha_9,
            R.drawable.f_alpha_10,
            R.drawable.f_alpha_11,
            R.drawable.f_alpha_12,
            R.drawable.f_alpha_13,
            R.drawable.f_alpha_14,
            R.drawable.f_alpha_15,
            R.drawable.f_alpha_16,
            R.drawable.f_alpha_17,
            R.drawable.f_alpha_18,
            R.drawable.f_alpha_19,
            R.drawable.f_alpha_20,
            R.drawable.f_alpha_21,
            R.drawable.f_alpha_22,
            R.drawable.f_alpha_23,
            R.drawable.f_alpha_24,
            R.drawable.f_alpha_25,
            R.drawable.f_alpha_26
        };

        final int[] kanaIds = {
            R.drawable.f_kana_1,
            R.drawable.f_kana_2,
            R.drawable.f_kana_3,
            R.drawable.f_kana_4,
            R.drawable.f_kana_5,
            R.drawable.f_kana_6,
            R.drawable.f_kana_7,
            R.drawable.f_kana_8,
            R.drawable.f_kana_9,
            R.drawable.f_kana_10
        };

        for (int i = 0; i < 26; i++) {
            mAlphabets[i] = (BitmapDrawable) ContextCompat.getDrawable(ctx, alphabetIds[i]);
        }

        for (int i = 0; i < 10; i++) {
            mKanas[i] = (BitmapDrawable) ContextCompat.getDrawable(ctx, kanaIds[i]);
        }

        mEdge = (BitmapDrawable) ContextCompat.getDrawable(ctx, R.drawable.drawer_tab_gradation);

        final int edgeWidth = mEdge.getBitmap().getWidth();
        final int edgeHeight = mEdge.getBitmap().getHeight();
        mEdgeBitmap_left = Bitmap.createBitmap(edgeWidth, edgeHeight, Bitmap.Config.ARGB_8888);
        mEdgeCanvas_left = new Canvas(mEdgeBitmap_left);
        mEdgeBitmap_right = Bitmap.createBitmap(edgeWidth, edgeHeight, Bitmap.Config.ARGB_8888);
        mEdgeCanvas_right = new Canvas(mEdgeBitmap_right);
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

    @Override
    public boolean onTouchEvent(MotionEvent me) {

        acquireVelocityTrackerAndAddMovement(me);

        int btnIndex;

        float x = me.getX();
        float y = me.getY();

        switch (me.getAction()) {
        case MotionEvent.ACTION_DOWN:

            mDownMotionX = x;
            mDownMotionY = y;

            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }

            if (y < mLowerBtnTop) {

                if (mSelectedLowerBtn == 1 || mSelectedLowerBtn == 2) {

                    mLastMotionX = x;
                    mLastMotionY = y;
                    mLastMotionXRemainder = 0;
                    mTotalMotionX = 0;

                    mTouchedUpperBtn = positionToUpperBtnIndex(x + mUnboundedScrollX, y);
                    if (mTouchedUpperBtn >= 0) {
                        invalidate();
                    }
                }

            } else {

                mTouchedLowerBtn = positionToLowerBtnIndex(x, y);
                if (mTouchedLowerBtn >= 0) {
                    invalidate();
                }
            }
            break;
        case MotionEvent.ACTION_MOVE:

            mTouchedLowerBtn = -1;

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

                if (y < mLowerBtnTop) {
                    determineScrollingStart(me);
                } else {

                    mLastMotionX = x;
                    mLastMotionY = y;
                    mLastMotionXRemainder = 0;

                    mTouchedLowerBtn = positionToLowerBtnIndex(x, y);
                    if (mTouchedLowerBtn >= 0) {
                        invalidate();
                    }
                }
            }

            break;
        case MotionEvent.ACTION_UP:

            if (mTouchState == TOUCH_STATE_SCROLLING) {

                final VelocityTracker velocityTracker = mVelocityTracker;
                velocityTracker.computeCurrentVelocity(400, MAXIMUM_VELOCITY);
                int velocityX = (int) velocityTracker.getXVelocity();

                boolean isFling = mTotalMotionX > MIN_LENGTH_FOR_FLING &&
                        Math.abs(velocityX) > mFlingThresholdVelocity;

                final int maxScrollX = getMaxScrollX();

                 if (mOverScrollX >= 0&& mOverScrollX <= maxScrollX && isFling) {
                     mScroller.fling(mUnboundedScrollX, 0, - velocityX, 0, 0, maxScrollX, 0, 0, mUpperBtnBg.getBitmap().getWidth(), 0);
                 } else {
                     if (mOverScrollX < 0) {
                         mScroller.startScroll(mUnboundedScrollX, 0, -mUnboundedScrollX, 0, 550);
                     } else if (mOverScrollX > maxScrollX) {
                         mScroller.startScroll(mUnboundedScrollX, 0, maxScrollX - mUnboundedScrollX, 0, 550);
                     }
                 }

                mTotalMotionX += Math.abs(mLastMotionX + mLastMotionXRemainder - x);
            }

            if (getVisibility() == VISIBLE) {

                if (y < mLowerBtnTop) {

                    if (mSelectedLowerBtn == 1 || mSelectedLowerBtn == 2) {

                        float deltaX = (mDownMotionX - x) / mDensity;
                        if (Math.abs(deltaX) < 30f) {

                            int touchedBtn = positionToUpperBtnIndex(x + mUnboundedScrollX, y);
                            if (mTouchedUpperBtn == touchedBtn) {

                                boolean change = false;

                                if (mSelectedLowerBtn == 1) {
                                    if (mSelectedUpperBtn_alphabet!= touchedBtn) {
                                        mSelectedUpperBtn_alphabet = touchedBtn;
                                        change = true;
                                    }

                                } else {
                                    if (mSelectedUpperBtn_kana != touchedBtn) {
                                        mSelectedUpperBtn_kana = touchedBtn;
                                        change = true;
                                    }
                                }

                                if (change && mOnChangedListener != null) {
                                    mOnChangedListener.onFilterChanged(mSelectedLowerBtn, mSelectedUpperBtn_alphabet, mSelectedUpperBtn_kana);
                                }
                            }
                        }
                    }

                } else {

                    int firstTouchedBtn = positionToLowerBtnIndex(mDownMotionX, mDownMotionY);
                    if (firstTouchedBtn >= 0) {

                        int lastTouchedBtn = positionToLowerBtnIndex(x, y);
                        if (firstTouchedBtn == lastTouchedBtn) {
                            scrollTo(0, 0);
                            mSelectedUpperBtn_alphabet = -1;
                            mSelectedUpperBtn_kana = -1;
                            mSelectedLowerBtn = lastTouchedBtn;

                            if (mOnChangedListener != null) {
                                mOnChangedListener.onFilterChanged(mSelectedLowerBtn, mSelectedUpperBtn_alphabet, mSelectedUpperBtn_kana);
                            }
                        }
                    }
                }
            }

            mTouchedLowerBtn = -1;
            mTouchedUpperBtn = -1;
            mTouchState = TOUCH_STATE_REST;

            invalidate();

            releaseVelocityTracker();
            break;
        }

        return true;
    }

    private void determineScrollingStart(MotionEvent ev) {

        if (mSelectedLowerBtn != 1 && mSelectedLowerBtn != 2) return;

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

        final int maxScrollX = getMaxScrollX();

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

    private int getMaxScrollX() {
        if (mSelectedLowerBtn == 1) {
            return mMaxScrollX_alphabet;
        } else if (mSelectedLowerBtn == 2) {
            return mMaxScrollX_kana;
        }
        return 0;
    }

    private void overScroll(float amount, int maxScrollX) {

        int screenSize = mDspWidth;

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

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mDspWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = MeasureSpec.getSize(heightMeasureSpec);

        mLowerBtnHeight = (int) Math.floor(104 * mDensity / 3);
        mBtnGap = (int) Math.floor(6 * mDensity / 3);
        mBtnTextSize = (int) Math.floor(48 * mDensity / 3);

        mLowerBtnWidth = (int) Math.floor((mDspWidth / 3f) - ((2 * mBtnGap + 2 * mBtnPadX) / 3f));

        mLowerCenterBtnLeft = mDspWidth / 2 - mLowerBtnWidth / 2;
        mLowerLeftBtnLeft = mLowerCenterBtnLeft - (mLowerBtnWidth + mBtnGap);
        mLowerRightBtnLeft = mLowerCenterBtnLeft + (mLowerBtnWidth + mBtnGap);

        mTxtGap = (int) Math.ceil(mDensity * 2);

        mBtnPadX = (int) Math.floor(10 * mDensity);

        final int upperBtnSize = mUpperBtnBg.getBitmap().getWidth();

        mLowerBtnTop = upperBtnSize + (mBtnGap * 4);

        this.setMeasuredDimension((upperBtnSize * 26) + (mBtnGap * 27), mHeight);

        mMaxScrollX_alphabet = (upperBtnSize * 26) + (mBtnGap * 25) + (mBtnPadX * 2) - mDspWidth;
        if (mMaxScrollX_alphabet < 0) {
            mMaxScrollX_alphabet = 0;
        }

        mMaxScrollX_kana = (upperBtnSize * 10) + (mBtnGap * 9) + (mBtnPadX * 2) - mDspWidth;
        if (mMaxScrollX_kana < 0) {
            mMaxScrollX_kana = 0;
        }

        scrollTo(0, 0);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private int positionToUpperBtnIndex(float x, float y) {

        if (y >= mLowerBtnTop) return -1;

        final int upperBtnSize = mUpperBtnBg.getBitmap().getWidth();
        final int radius = upperBtnSize / 2;

        for (int i = 0; i < 26; i++) {

            int cx = mBtnPadX + radius + ((upperBtnSize + mBtnGap) * i);

            if (x >= cx - radius && x < cx + radius) {
                return i;
            }
        }

        return -1;
    }

    private int positionToLowerBtnIndex(float x, float y) {

        if (y < mLowerBtnTop) return -1;

        if (x < mLowerLeftBtnLeft) return -1;

        if (x < mLowerLeftBtnLeft + mLowerBtnWidth) return 0;

        if (x < mLowerCenterBtnLeft) return -1;

        if (x < mLowerCenterBtnLeft + mLowerBtnWidth) return 1;

        if (x < mLowerRightBtnLeft) return -1;

        if (x < mLowerRightBtnLeft + mLowerBtnWidth) return 2;

        return -1;
    }

    private Paint mPaint = new Paint();
    private int mSelectedItemColor = Color.argb(255, 255, 50, 171);

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int scrollX = getScrollX();

        mPaint.setAntiAlias(true);
        mPaint.setTextSize(mBtnTextSize);

        drawLowerBtns(canvas, mPaint, scrollX);
        drawUpperBtns(canvas, mPaint, scrollX);
    }

    private void drawUpperBtns(Canvas canvas, Paint paint, int scrollX) {

        if (mSelectedLowerBtn != 1 && mSelectedLowerBtn != 2) return;

        int saveCount = canvas.save();
        int ew = mEdgeCanvas_left.getWidth();
        canvas.clipRect(scrollX + ew, 0, scrollX + canvas.getWidth() - ew, canvas.getHeight());

        if (mSelectedLowerBtn == 1) {
            // アルファベット表示
            for (int i = 0; i < 26; i++) {
                drawUpperBtn(canvas, paint, scrollX, mAlphabets[i], i, mSelectedUpperBtn_alphabet);
            }

        } else {
            // カナ表示
            for (int i = 0; i < 10; i++) {
                drawUpperBtn(canvas, paint, scrollX, mKanas[i], i, mSelectedUpperBtn_kana);
            }
        }

        canvas.restoreToCount(saveCount);

        if (mSelectedLowerBtn == 1) {

            mEdgeCanvas_left.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            final int btnSize = mUpperBtnBg.getBitmap().getWidth();
            for (int i = 0; i < 26; i++) {

                final int left = mBtnPadX + ((btnSize + mBtnGap) * i);
                if (left + btnSize < scrollX) continue;
                if (left > scrollX + ew) break;

                drawUpperBtn(mEdgeCanvas_left, left - scrollX, 0, mAlphabets[i],
                        (i == mTouchedUpperBtn || i == mSelectedUpperBtn_alphabet));
            }

            Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            mEdgeCanvas_left.drawBitmap(mEdge.getBitmap(), 0, 0, paint2);

            canvas.drawBitmap(mEdgeBitmap_left, scrollX, mBtnGap, paint);

            mEdgeCanvas_right.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for (int i = 25; i >= 0; i--) {

                final int left = mBtnPadX + ((btnSize + mBtnGap) * i);
                if (left > scrollX + mDspWidth) continue;
                if (left + btnSize < scrollX + mDspWidth - ew) break;

                drawUpperBtn(mEdgeCanvas_right, left - scrollX - mDspWidth + ew, 0, mAlphabets[i],
                        (i == mTouchedUpperBtn || i == mSelectedUpperBtn_alphabet));
            }

            Matrix matrix = new Matrix();
            matrix.setRotate(180, (int) Math.ceil(mEdgeCanvas_right.getWidth() / 2f),
                    (int) Math.ceil(mEdgeCanvas_right.getHeight() / 2f));
            mEdgeCanvas_right.drawBitmap(mEdge.getBitmap(), matrix, paint2);

            canvas.drawBitmap(mEdgeBitmap_right, scrollX + mDspWidth - ew, mBtnGap, paint);

        } else {

            mEdgeCanvas_left.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            final int btnSize = mUpperBtnBg.getBitmap().getWidth();
            for (int i = 0; i < 10; i++) {

                final int left = mBtnPadX + ((btnSize + mBtnGap) * i);
                if (left + btnSize < scrollX) continue;
                if (left > scrollX + ew) break;

                drawUpperBtn(mEdgeCanvas_left, left - scrollX, 0, mKanas[i],
                        (i == mTouchedUpperBtn || i == mSelectedUpperBtn_kana));
            }

            Paint paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
            mEdgeCanvas_left.drawBitmap(mEdge.getBitmap(), 0, 0, paint2);

            canvas.drawBitmap(mEdgeBitmap_left, scrollX, mBtnGap, paint);


            mEdgeCanvas_right.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            for (int i = 9; i >= 0; i--) {

                final int left = mBtnPadX + ((btnSize + mBtnGap) * i);
                if (left > scrollX + mDspWidth) continue;
                if (left + btnSize < scrollX + mDspWidth - ew) break;

                drawUpperBtn(mEdgeCanvas_right, left - scrollX - mDspWidth + ew, 0, mKanas[i],
                        (i == mTouchedUpperBtn || i == mSelectedUpperBtn_kana));
            }

            Matrix matrix = new Matrix();
            matrix.setRotate(180, (int) Math.ceil(mEdgeCanvas_right.getWidth() / 2f),
                    (int) Math.ceil(mEdgeCanvas_right.getHeight() / 2f));
            mEdgeCanvas_right.drawBitmap(mEdge.getBitmap(), matrix, paint2);

            canvas.drawBitmap(mEdgeBitmap_right, scrollX + mDspWidth - ew, mBtnGap, paint);
        }
    }

    private void drawUpperBtn(Canvas canvas, Paint paint, int scrollX, BitmapDrawable charDrawable, int index, int selectedBtnIndex) {

        final int btnSize = mUpperBtnBg.getBitmap().getWidth();
        final int left = mBtnPadX + ((btnSize + mBtnGap) * index);
        if (left + btnSize < scrollX || left > scrollX + mDspWidth)
            return;

        drawUpperBtn(canvas, left, mBtnGap, charDrawable,
                (index == mTouchedUpperBtn || index == selectedBtnIndex));
    }

    private void drawUpperBtn(Canvas canvas, int left, int top, BitmapDrawable charDrawable, boolean isSelected) {

        final int btnSize = mUpperBtnBg.getBitmap().getWidth();

        mUpperBtnBg.setBounds(left, top, left + btnSize, top + btnSize);
        mUpperBtnBg.draw(canvas);

        final int w_char = charDrawable.getBitmap().getWidth();
        final int h_char = charDrawable.getBitmap().getHeight();

        int left_char = left + (int) Math.ceil((btnSize - w_char) / 2f);
        int top_char = top + (int) Math.ceil((btnSize - h_char) / 2f);

        charDrawable.setBounds(left_char, top_char, left_char + w_char, top_char + h_char);

        if (isSelected) {
            charDrawable.setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            charDrawable.clearColorFilter();
        }
        charDrawable.draw(canvas);
    }

    private void drawLowerBtns(Canvas canvas, Paint paint, int scrollX) {

        final int w_a = mAlphabets[0].getBitmap().getWidth();
        final int h_a = mAlphabets[0].getBitmap().getHeight();
        final int w_l = mAlphabets[11].getBitmap().getWidth();
        final int h_l = mAlphabets[11].getBitmap().getHeight();
        final int w_b = mAlphabets[1].getBitmap().getWidth();
        final int h_b = mAlphabets[1].getBitmap().getHeight();
        final int w_c = mAlphabets[2].getBitmap().getWidth();
        final int h_c = mAlphabets[2].getBitmap().getHeight();
        final int w_ka = mKanas[1].getBitmap().getWidth();
        final int h_ka = mKanas[1].getBitmap().getHeight();
        final int w_na = mKanas[4].getBitmap().getWidth();
        final int h_na = mKanas[4].getBitmap().getHeight();

        int cy = mLowerBtnTop + (int) Math.ceil(mLowerBtnHeight / 2f);

        // ALL
        mLowerBtnBg.setBounds(scrollX + mLowerLeftBtnLeft, mLowerBtnTop, scrollX + mLowerLeftBtnLeft
                + mLowerBtnWidth, mLowerBtnTop + mLowerBtnHeight);
        mLowerBtnBg.draw(canvas);

        if (mTouchedLowerBtn == 0 || mSelectedLowerBtn == 0) {
            mAlphabets[0].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
            mAlphabets[11].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            mAlphabets[0].clearColorFilter();
            mAlphabets[11].clearColorFilter();
        }

        int w_all = w_a + w_l * 2;

        // A
        int left = scrollX + mLowerLeftBtnLeft + (int) Math.ceil((mLowerBtnWidth - w_all) / 2f);
        int top = cy - (int) Math.ceil(h_a / 2f);
        mAlphabets[0].setBounds(left, top, left + w_a, top + h_a);
        mAlphabets[0].draw(canvas);
        // L
        left += w_a + mTxtGap;
        top = cy - (int) Math.ceil(h_l / 2f);
        mAlphabets[11].setBounds(left, top, left + w_l, top + h_l);
        mAlphabets[11].draw(canvas);
        // L
        left += w_l + mTxtGap;
        mAlphabets[11].setBounds(left, top, left + w_l, top + h_l);
        mAlphabets[11].draw(canvas);

        // ABC
        mLowerBtnBg.setBounds(scrollX + mLowerCenterBtnLeft, mLowerBtnTop,
                scrollX + mLowerCenterBtnLeft + mLowerBtnWidth, mLowerBtnTop
                + mLowerBtnHeight);
        mLowerBtnBg.draw(canvas);

        if (mTouchedLowerBtn == 1 || mSelectedLowerBtn == 1) {
            mAlphabets[0].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
            mAlphabets[1].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
            mAlphabets[2].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            mAlphabets[0].clearColorFilter();
            mAlphabets[1].clearColorFilter();
            mAlphabets[2].clearColorFilter();
        }

        int w_abc = w_a + w_b + w_c;

        // A
        left = scrollX + mLowerCenterBtnLeft + (int) Math.ceil((mLowerBtnWidth - w_abc) / 2f);
        top = cy - (int) Math.ceil(h_a / 2f);
        mAlphabets[0].setBounds(left, top, left + w_a, top + h_a);
        mAlphabets[0].draw(canvas);
        // B
        left += w_a + mTxtGap;
        top = cy - (int) Math.ceil(h_b / 2f);
        mAlphabets[1].setBounds(left, top, left + w_b, top + h_b);
        mAlphabets[1].draw(canvas);
        // C
        left += w_b + mTxtGap;
        top = cy - (int) Math.ceil(h_c / 2f);
        mAlphabets[2].setBounds(left, top, left + w_c, top + h_c);
        mAlphabets[2].draw(canvas);

        // カナ
        mLowerBtnBg.setBounds(scrollX + mLowerRightBtnLeft, mLowerBtnTop,
                scrollX + mLowerRightBtnLeft + mLowerBtnWidth, mLowerBtnTop
                + mLowerBtnHeight);
        mLowerBtnBg.draw(canvas);

        if (mTouchedLowerBtn == 2 || mSelectedLowerBtn == 2) {
            mKanas[1].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
            mKanas[4].setColorFilter(mSelectedItemColor, PorterDuff.Mode.SRC_ATOP);
        } else {
            mKanas[1].clearColorFilter();
            mKanas[4].clearColorFilter();
        }

        int w_kana = w_ka + w_na;

        // カ
        left = scrollX + mLowerRightBtnLeft + (int) Math.ceil((mLowerBtnWidth - w_kana) / 2f);
        top = cy - (int) Math.ceil(h_ka / 2f);
        mKanas[1].setBounds(left, top, left + w_ka, top + h_ka);
        mKanas[1].draw(canvas);
        // ナ
        left += w_ka + mTxtGap;
        top = cy - (int) Math.ceil(h_na / 2f);
        mKanas[4].setBounds(left, top, left + w_na, top + h_na);
        mKanas[4].draw(canvas);
    }
}
