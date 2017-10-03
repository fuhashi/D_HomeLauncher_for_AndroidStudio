package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.view.MotionEvent;
import android.widget.ImageView;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class PageEditItemView extends ImageView {
    private static final String TAG = "PageEditItemView";

    static final int ICON_TYPE_COPY = 0;
    static final int ICON_TYPE_DELETE = 1;
    static final int ICON_TYPE_ADD = 2;
    static final int ICON_TYPE_HOME = 3;

    private final int mColorOnTouch;

    private static final Paint sPaint = new Paint();

    private int mBorderWidth;
    private int mPaddingX;
    private boolean mIsThemeMode;
    private boolean mIsDummy;

    private boolean mIsTouched = false;
    private int mActivePointerId = -1;

    private boolean mIsDeleteIconTouched = false;
    private boolean mIsCopyIconTouched = false;
    private boolean mIsHomeIconTouched = false;

    private boolean mIsDefaultPage = false;

    private BitmapDrawable mPlusIcon;

    private BitmapDrawable mBtnBg;
    private BitmapDrawable mHomeIcon;
    private BitmapDrawable mDeleteIcon;
    private BitmapDrawable mCopyIcon;

    private float mAspectRatio;

    private boolean mHideDeleteIcon = false;
    private boolean mHideCopyIcon = false;

    private OnIconClickListener mOnIconClickListener;

    public interface OnIconClickListener {
        public void onIconClick(PageEditItemView v, int iconType);
    }

    public void setOnIconClickListener(OnIconClickListener onIconClickListener) {
        mOnIconClickListener = onIconClickListener;
    }

    public boolean isDummy() {
        return mIsDummy;
    }

    boolean isDefaultPage() {
        return mIsDefaultPage;
    }

    void setIsDefaultPage(boolean isDefaultPage) {
        mIsDefaultPage = isDefaultPage;
    }

    public void hideDeleteIcon(boolean hide) {
        mHideDeleteIcon = hide;
        invalidate();
    }

//    public void hideCopyIcon(boolean hide) {
//        mHideCopyIcon = hide;
//        invalidate();
//    }

    public PageEditItemView(Context context, int borderWidth, int paddingX, boolean isThemeMode) {
        this(context, borderWidth, paddingX, isThemeMode, false);
    }

    public PageEditItemView(Context context, int borderWidth, int paddingX, boolean isThemeMode, boolean isDummy) {
        super(context);
        mBorderWidth = borderWidth;
        mPaddingX = paddingX;
        mIsThemeMode = isThemeMode;
        mIsDummy = isDummy;

        if (isDummy) {
            mPlusIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_addpanel_add);
        } else {
            mBtnBg = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_addpanel_circle_base);
            mDeleteIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_addpanel_close);
            mHomeIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_addpanel_home);
//            if (isThemeMode) {
            mCopyIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_addpanel_copy);
//            }
        }
        mColorOnTouch = Color.rgb(255, 39, 166);
    }

    public void changeState(boolean isTouched) {
        if (mIsTouched == isTouched) return;
        mIsTouched = isTouched;
        invalidate();
    }

    public void setItemAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        final int cw = canvas.getWidth();
        final int ch = canvas.getHeight();

        final int borderWidth = mBorderWidth;
        final int padX = mPaddingX;
        float scale = 1.0f - ((borderWidth + padX) * 2 / (float) cw);

        if (mIsTouched) {
            sPaint.setColor(mColorOnTouch);
        } else {
            sPaint.setColor(Color.rgb(255, 255, 255));
        }

        sPaint.setStrokeWidth(borderWidth);
        sPaint.setStyle(Paint.Style.STROKE);

        float ih_scale = ch * scale;
        float iw_scale = ih_scale * mAspectRatio;
        float left = (cw - iw_scale) / 2 - borderWidth / 2f;
        float top = (ch - ih_scale) / 2 - borderWidth / 2f;
        canvas.drawRect(left, top, left + iw_scale + borderWidth, top + ih_scale + borderWidth, sPaint);

        int saveCount = canvas.save();
        canvas.scale(scale, scale, cw / 2f, ch / 2f);

        if (!mIsThemeMode) {

            sPaint.setColor(Color.rgb(249, 249, 249));
            sPaint.setStyle(Paint.Style.FILL);
            int bgh = canvas.getHeight();
            float bgw = bgh * mAspectRatio;
            float bgLeft = (canvas.getWidth() - bgw) / 2f;
            canvas.drawRect(bgLeft, 0, bgLeft + bgw, bgh, sPaint);
        }

        if (mIsDummy) {

            sPaint.setColor(Color.rgb(249, 249, 249));
            sPaint.setStyle(Paint.Style.FILL);
            int bgh = canvas.getHeight();
            float bgw = bgh * mAspectRatio;
            float bgLeft = (canvas.getWidth() - bgw) / 2f;
            canvas.drawRect(bgLeft, 0, bgLeft + bgw, bgh, sPaint);

            mPlusIconSize = (int) Math.ceil(cw * 95 / 360f);

            int iconLeft = (int) Math.ceil((canvas.getWidth() - mPlusIconSize) / 2f);
            int iconTop = (int) Math.ceil((canvas.getHeight() - mPlusIconSize) / 2f);

            mPlusIcon.setBounds(iconLeft, iconTop, iconLeft + mPlusIconSize,
                    iconTop + mPlusIconSize);

            if (mIsTouched) {
                mPlusIcon.setColorFilter(mColorOnTouch, PorterDuff.Mode.SRC_ATOP);
            } else {
                mPlusIcon.clearColorFilter();
            }

            mPlusIcon.draw(canvas);
        }

        super.onDraw(canvas);

        if (!mIsDummy) {

            int bgh = canvas.getHeight();
            float bgw = bgh * mAspectRatio;
            float bgLeft = (canvas.getWidth() - bgw) / 2f;

//            int icW = (int) Math.ceil(mDeleteIcon.getBitmap().getWidth() * scale);
//            int icH = (int) Math.ceil(mDeleteIcon.getBitmap().getHeight() * scale);
            float fIconSize = cw * 108 / 360f;
            mIconSize = (int) Math.ceil(fIconSize);
            mIconMarginY = (int) Math.ceil(fIconSize / 20);

            int mbtnBgOffsetX = (int) Math.floor((0.025f * mIconSize) / 2); // 82x80pxの2px分の補正

            // ホームボタン(下)
            mHomeIconLeft = (int) Math.ceil(bgLeft + (bgw - mIconSize) / 2f);
            mHomeIconTop = bgh - mIconSize - mIconMarginY;
            mBtnBg.setBounds(mHomeIconLeft - mbtnBgOffsetX, mHomeIconTop,
                    mHomeIconLeft + mIconSize + mbtnBgOffsetX, mHomeIconTop + mIconSize);
            mBtnBg.draw(canvas);

            int iconSizeOffsetHome = (int) Math.ceil(mIconSize * 0.1875f); // 50x50 px
            int iconOffsetYHome = (int) Math.ceil(mIconSize * 5 / 80f);
            mHomeIcon.setBounds(mHomeIconLeft + iconSizeOffsetHome, mHomeIconTop + iconSizeOffsetHome - iconOffsetYHome,
                    mHomeIconLeft + mIconSize - iconSizeOffsetHome, mHomeIconTop + mIconSize - iconSizeOffsetHome - iconOffsetYHome);
            if (mIsHomeIconTouched || mIsDefaultPage) {
                mHomeIcon.setColorFilter(mColorOnTouch, PorterDuff.Mode.SRC_ATOP);
            } else {
                mHomeIcon.clearColorFilter();
            }
            mHomeIcon.draw(canvas);

            int iconSizeOffset = (int) Math.ceil(mIconSize / 4f); // 40x40 px
            int iconOffsetY = (int) Math.ceil(mIconSize * 2 / 80f);

            // コピーボタン(左上)
            if (mCopyIcon != null && !mHideCopyIcon) {
                mCopyIconLeft = (int) Math.ceil(bgLeft + borderWidth / 4f);
                mBtnBg.setBounds(mCopyIconLeft - mbtnBgOffsetX, mIconMarginY,
                        mCopyIconLeft + mIconSize + mbtnBgOffsetX, mIconMarginY + mIconSize);
                mBtnBg.draw(canvas);

                mCopyIcon.setBounds(mCopyIconLeft + iconSizeOffset, mIconMarginY + iconSizeOffset - iconOffsetY,
                        mCopyIconLeft + mIconSize - iconSizeOffset, mIconMarginY + mIconSize - iconSizeOffset - iconOffsetY);
                if (mIsCopyIconTouched) {
                    mCopyIcon.setColorFilter(mColorOnTouch, PorterDuff.Mode.SRC_ATOP);
                } else {
                    mCopyIcon.clearColorFilter();
                }
                mCopyIcon.draw(canvas);
            }

            if (!mHideDeleteIcon) {
                // 削除ボタン(右上)
                mDeleteIconLeft = (int) Math.ceil(bgLeft + bgw - mIconSize - borderWidth / 4f);
                mBtnBg.setBounds(mDeleteIconLeft - mbtnBgOffsetX, mIconMarginY,
                        mDeleteIconLeft + mIconSize + mbtnBgOffsetX, mIconMarginY + mIconSize);
                mBtnBg.draw(canvas);

                mDeleteIcon.setBounds(mDeleteIconLeft + iconSizeOffset, mIconMarginY + iconSizeOffset - iconOffsetY,
                        mDeleteIconLeft + mIconSize - iconSizeOffset, mIconMarginY + mIconSize - iconSizeOffset - iconOffsetY);
                if (mIsDeleteIconTouched) {
                    mDeleteIcon.setColorFilter(mColorOnTouch, PorterDuff.Mode.SRC_ATOP);
                } else {
                    mDeleteIcon.clearColorFilter();
                }
                mDeleteIcon.draw(canvas);
            }
        }

        canvas.restoreToCount(saveCount);
    }

    private int mCopyIconLeft;
    private int mDeleteIconLeft;
    private int mIconMarginY;
    private int mIconSize;
    private int mPlusIconSize;
    private int mHomeIconLeft;
    private int mHomeIconTop;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (!mIsDummy) {

            final int action = ev.getAction();

            switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:

                final float x_down = ev.getX();
                final float y_down = ev.getY();
                mActivePointerId = ev.getPointerId(0);

                if (hitCopyIcon(x_down, y_down)) {
                    mIsCopyIconTouched = true;
                    invalidate();
                    return true;
                } else if (hitDeleteIcon(x_down, y_down)) {
                    mIsDeleteIconTouched = true;
                    invalidate();
                    return true;
                } else if (hitHomeIcon(x_down, y_down)) {
                    mIsHomeIconTouched = true;
                    invalidate();
                    return true;
                }

                break;
            case MotionEvent.ACTION_MOVE:

                final int pointerIndex_move = ev.findPointerIndex(mActivePointerId);
                final float x_move = ev.getX(pointerIndex_move);
                final float y_move = ev.getY(pointerIndex_move);

                if (hitCopyIcon(x_move, y_move)) {
                    if (!mIsCopyIconTouched) {
                        mIsCopyIconTouched = true;
                        invalidate();
                        return true;
                    }

                } else if (hitDeleteIcon(x_move, y_move)) {
                    if (!mIsDeleteIconTouched) {
                        mIsDeleteIconTouched = true;
                        invalidate();
                        return true;
                    }

                } else if (hitHomeIcon(x_move, y_move)) {
                    if (!mIsHomeIconTouched) {
                        mIsHomeIconTouched = true;
                        invalidate();
                        return true;
                    }

                } else {

                    if (mIsCopyIconTouched) {
                        mIsCopyIconTouched = false;
                        invalidate();
                    }

                    if (mIsDeleteIconTouched) {
                        mIsDeleteIconTouched = false;
                        invalidate();
                    }

                    if (mIsHomeIconTouched) {
                        mIsHomeIconTouched = false;
                        invalidate();
                    }
                }

                break;
            case MotionEvent.ACTION_UP:

                if (mIsCopyIconTouched) {
                    if (mOnIconClickListener != null) {
                        mOnIconClickListener.onIconClick(this, ICON_TYPE_COPY);
                    }
                }

                if (mIsDeleteIconTouched) {
                    if (mOnIconClickListener != null) {
                        mOnIconClickListener.onIconClick(this, ICON_TYPE_DELETE);
                    }
                }

                if (mIsHomeIconTouched) {
                    if (mOnIconClickListener != null) {
                        mOnIconClickListener.onIconClick(this, ICON_TYPE_HOME);
                    }
                }

                mActivePointerId = -1;

                mIsCopyIconTouched = false;
                mIsDeleteIconTouched = false;
                mIsHomeIconTouched = false;
                invalidate();

                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int pointerIndex_up = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                final int pointerId = ev.getPointerId(pointerIndex_up);
                if (pointerId == mActivePointerId) {
                    final int newPointerIndex = pointerIndex_up == 0 ? 1 : 0;
                    mActivePointerId = ev.getPointerId(newPointerIndex);

                    mIsCopyIconTouched = false;
                    mIsDeleteIconTouched = false;
                    mIsHomeIconTouched = false;
                    invalidate();
                }

                break;
            }

            return super.onTouchEvent(ev);
        }


        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN:

            final float x_down = ev.getX();
            final float y_down = ev.getY();
            mActivePointerId = ev.getPointerId(0);

            if (hitPlusIcon(x_down, y_down)) {
                mIsTouched = true;
                invalidate();
            }

            break;
        case MotionEvent.ACTION_MOVE:

            final int pointerIndex_move = ev.findPointerIndex(mActivePointerId);
            final float x_move = ev.getX(pointerIndex_move);
            final float y_move = ev.getY(pointerIndex_move);

            if (hitPlusIcon(x_move, y_move)) {
                if (!mIsTouched) {
                    mIsTouched = true;
                    invalidate();
                }

            } else {
                if (mIsTouched) {
                    mIsTouched = false;
                    invalidate();
                }
            }

            break;
        case MotionEvent.ACTION_UP:

            if (mIsTouched) {
                if (mOnIconClickListener != null) {
                    mOnIconClickListener.onIconClick(this, ICON_TYPE_ADD);
                }
            }

            mActivePointerId = -1;

            mIsTouched = false;
            invalidate();

            break;
        case MotionEvent.ACTION_POINTER_UP:
            final int pointerIndex_up = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
            MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int pointerId = ev.getPointerId(pointerIndex_up);
            if (pointerId == mActivePointerId) {
                final int newPointerIndex = pointerIndex_up == 0 ? 1 : 0;
                mActivePointerId = ev.getPointerId(newPointerIndex);

                mIsTouched = false;
                invalidate();
            }

            break;
        }

        return true;
    }

    private boolean hitHomeIcon(float x, float y) {

        if (x >= mHomeIconLeft && x <= mHomeIconLeft + mIconSize
                && y >= mHomeIconTop && y <= mHomeIconTop + mIconSize) {
            return true;
        }

        return false;
    }

    private boolean hitCopyIcon(float x, float y) {

        if (x >= mCopyIconLeft && x <= mCopyIconLeft + mIconSize
                && y >= mIconMarginY && y <= mIconMarginY + mIconSize) {
            return true;
        }

        return false;
    }

    private boolean hitDeleteIcon(float x, float y) {

        if (mHideDeleteIcon) return false;

        if (x >= mDeleteIconLeft && x <= mDeleteIconLeft + mIconSize
                && y >= mIconMarginY && y <= mIconMarginY + mIconSize) {
            return true;
        }

        return false;
    }

    private boolean hitPlusIcon(float x, float y) {

        final int centerX = getMeasuredWidth() / 2;
        final int centerY = getMeasuredHeight() / 2;

//        final Bitmap iconBmp = mPlusIcon.getBitmap();
//        final int iconWidth = iconBmp.getWidth();
//        final int iconHeight = iconBmp.getHeight();
        final int iconWidth = mPlusIconSize;
        final int iconHeight = mPlusIconSize;

        if (x >= centerX - iconWidth * 1.5f / 2 && x <= centerX + iconWidth * 1.5f / 2
                && y >= centerY - iconHeight * 1.5f / 2 && y <= centerY + iconHeight * 1.5f / 2) {
            return true;
        }

        return false;
    }
}
