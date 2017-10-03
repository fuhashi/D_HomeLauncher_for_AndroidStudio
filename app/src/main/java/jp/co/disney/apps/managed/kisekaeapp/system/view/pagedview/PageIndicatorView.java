package jp.co.disney.apps.managed.kisekaeapp.system.view.pagedview;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.animation.AnticipateInterpolator;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class PageIndicatorView extends View {

    private BitmapDrawable mIndicatorDrawable;
    private BitmapDrawable mCurrIndicatorDrawable;

    private int mCurrentNumPages = 0;
    private int mCurrentPageIndex = -1;

    private int mOffset = 0;

    private int mGap;

    private float mScaleFactor = 1.0f;
    private int mIconAreaWidth;

    private ValueAnimator mAnim;

    private int mMin = 0;
    private int mMax = Integer.MAX_VALUE;

    private int mDisplaySizeX;

    private static final float MAX_SCALE_FACTOR = 1.2f;

    public void setOffset(int offset) {
        mOffset = offset;
    }

    public void updateNumPages(int numPages) {
        if (mCurrentNumPages != numPages) {
            mCurrentNumPages = numPages;
            requestLayout();
            invalidate();
        }
    }

    public void updateCurrentPage(int pageIndex) {
        if (mCurrentPageIndex != pageIndex) {
            mCurrentPageIndex = pageIndex;

            if (mAnim != null && mAnim.isStarted()) {
                mAnim.cancel();
            }
            mAnim.start();
        }
    }

    public PageIndicatorView(Context context) {
        this(context, null);
    }

    public PageIndicatorView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public PageIndicatorView(Context context, AttributeSet attributeset, int defStyle) {
        super(context, attributeset, defStyle);

        final Resources res = getResources();
        mIndicatorDrawable = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.indicator_off);
        mCurrIndicatorDrawable = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.indicator_on);

        mHeight = (int) Math.ceil(mCurrIndicatorDrawable.getBitmap().getHeight() * MAX_SCALE_FACTOR);

        mAnim = ValueAnimator.ofFloat(MAX_SCALE_FACTOR, 1.0f);
        mAnim.setDuration(200L);
        mAnim.setInterpolator(new AnticipateInterpolator());
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (Float) animation.getAnimatedValue();
                mScaleFactor = t;
                invalidate();
            }
        });

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        Point displaySize = new Point();
        display.getSize(displaySize);
        mDisplaySizeX = displaySize.x;
    }

    private int mHeight;

    public int getContentHeight() {
        return mHeight;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mCurrentNumPages <= 0) return;

        final int cw = canvas.getWidth();
        final int ch = canvas.getHeight();
        final int w_curr = (int) Math.ceil(mCurrIndicatorDrawable.getBitmap().getWidth() * mScaleFactor);
        final int h_curr = (int) Math.ceil(mCurrIndicatorDrawable.getBitmap().getHeight() * mScaleFactor);
        final int w = mIndicatorDrawable.getBitmap().getWidth();
        final int h = mIndicatorDrawable.getBitmap().getHeight();

        int left = 0;
        int top = 0;

        final int start = mMin;
        final int end = (mCurrentNumPages - 1 <= mMax) ? mCurrentNumPages - 1 : mMax;
        final int currentIndex = mCurrentPageIndex - mOffset;

        if (currentIndex < start) {

            left = (mIconAreaWidth + mGap) * start;
            int left1 = left + (int) Math.ceil((mIconAreaWidth - w_curr) / 2f);
            int top1 = top + (int) Math.ceil((ch - h_curr) / 2f);
            mCurrIndicatorDrawable.setBounds(left1, top1, left1 + w_curr, top1 + h_curr);
            mCurrIndicatorDrawable.draw(canvas);

            for (int i = start + 1; i <= end; i++) {
                left = (mIconAreaWidth + mGap) * i;
                int left2 = left + (int) Math.ceil((mIconAreaWidth - w) / 2f);
                int top2 = top + (int) Math.ceil((ch - h) / 2f);
                mIndicatorDrawable.setBounds(left2, top2, left2 + w, top2 + h);
                mIndicatorDrawable.draw(canvas);
            }

        } else if (currentIndex > end) {

            for (int i = start; i <= end - 1; i++) {
                left = (mIconAreaWidth + mGap) * i;
                int left2 = left + (int) Math.ceil((mIconAreaWidth - w) / 2f);
                int top2 = top + (int) Math.ceil((ch - h) / 2f);
                mIndicatorDrawable.setBounds(left2, top2, left2 + w, top2 + h);
                mIndicatorDrawable.draw(canvas);
            }

            left = (mIconAreaWidth + mGap) * end;
            int left1 = left + (int) Math.ceil((mIconAreaWidth - w_curr) / 2f);
            int top1 = top + (int) Math.ceil((ch - h_curr) / 2f);
            mCurrIndicatorDrawable.setBounds(left1, top1, left1 + w_curr, top1 + h_curr);
            mCurrIndicatorDrawable.draw(canvas);
        } else {

            for (int i = start; i <= end; i++) {

                left = (mIconAreaWidth + mGap) * i;

                if (i == currentIndex) {
                    int left1 = left + (int) Math.ceil((mIconAreaWidth - w_curr) / 2f);
                    int top1 = top + (int) Math.ceil((ch - h_curr) / 2f);
                    mCurrIndicatorDrawable.setBounds(left1, top1, left1 + w_curr, top1 + h_curr);
                    mCurrIndicatorDrawable.draw(canvas);
                } else {
                    int left2 = left + (int) Math.ceil((mIconAreaWidth - w) / 2f);
                    int top2 = top + (int) Math.ceil((ch - h) / 2f);
                    mIndicatorDrawable.setBounds(left2, top2, left2 + w, top2 + h);
                    mIndicatorDrawable.draw(canvas);
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mGap = (int) Math.ceil(mIndicatorDrawable.getBitmap().getWidth() * 0.25);

        final int width;
        if (mCurrentNumPages > 0) {
            mIconAreaWidth = (int) Math.ceil(mCurrIndicatorDrawable.getBitmap().getWidth() * MAX_SCALE_FACTOR);
            width = mIconAreaWidth * mCurrentNumPages + mGap * (mCurrentNumPages - 1);

            int left = (int) Math.ceil((mDisplaySizeX - width) / 2f);
            for (int i = 0; i < mCurrentNumPages; i++) {
                if (left + ((mIconAreaWidth + mGap) * i) >= 0) {
                    mMin = i;
                    break;
                }
            }

            mMax = mCurrentNumPages - 1 - mMin;

        } else {
            width = 0;
        }

        final int height = (int) Math.ceil(mCurrIndicatorDrawable.getBitmap().getHeight() * MAX_SCALE_FACTOR);
        setMeasuredDimension(width, height);
    }
}
