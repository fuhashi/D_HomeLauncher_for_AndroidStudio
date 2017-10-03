package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class HomeSubMenuView extends FrameLayout implements View.OnTouchListener {

    static final String TAG_SETTINGS_SYSTEM = "SETTINGS_SYSTEM";
    static final String TAG_SETTINGS_PANEL = "SETTINGS_PANEL";

    private static final int ARROW_POS_LEFT = 0;
    private static final int ARROW_POS_RIGHT = 1;
    private static final int ARROW_POS_CENTER = 2;

    private int mStaticWidth;
    private int mStaticHeight;

    private int mArrowWidth;
    private int mArrowLeftMargin;

    private int mNumItems;
    private int mItemWidth;

    private LinearLayout mBodyLayout;
    private ImageView mArrowView;

    private float mDensity;

    private OnClickListener mOnClickListener;

    int getStaticWidth() {
        return mStaticWidth;
    }

    int getStaticHeight() {
        return mStaticHeight;
    }

    int getArrowWidth() {
        return mArrowWidth;
    }

    int getArrowLeftMargin() {
        return mArrowLeftMargin;
    }

    int getItemWidth() {
        return mItemWidth;
    }

    void setOnClickListener(OnClickListener onClickListener) {
        mOnClickListener = onClickListener;
    }

    HomeSubMenuView(Context context, float density) {
        super(context);
        mDensity = density;
    }

    void initForSettings(int backgroundWidth, boolean isUpperMenu) {

        final Context context = getContext();

        BitmapDrawable d = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.submenu_settings_1);
        Bitmap menuItemBmp = d.getBitmap();

        mItemWidth = (int) Math.floor(backgroundWidth / 5f * 1.03);
        int itemHeight = (int) Math.floor(mItemWidth * menuItemBmp.getHeight() / (float) menuItemBmp.getWidth());

        mNumItems = 2;

        View[] items = new View[mNumItems];
        int index = 0;

        ImageView iv = new HomeMenuMainItemImageView(context);
        iv.setImageDrawable(d);
        iv.setOnTouchListener(this);
        iv.setTag(TAG_SETTINGS_SYSTEM);
        items[index] = iv;
        index++;

        ImageView iv2 = new HomeMenuMainItemImageView(context);
        iv2.setImageResource(R.drawable.submenu_settings_2);
        iv2.setOnTouchListener(this);
        iv2.setTag(TAG_SETTINGS_PANEL);
        items[index] = iv2;
        index++;

        init(items, menuItemBmp.getWidth(), itemHeight, isUpperMenu, ARROW_POS_RIGHT);
    }

    private void init(View[] menuItems, int itemBmpWidth, int itemHeight, boolean isUpperMenu, int arrowPos) {

        final Context context = getContext();

        mBodyLayout = new LinearLayout(context);
        mBodyLayout.setBackgroundResource(R.drawable.test_base);
        mBodyLayout.setOrientation(LinearLayout.HORIZONTAL);

        for (View item : menuItems) {
            addMenuItem(item);
        }

        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);

        if (!isUpperMenu) {
            flp.gravity = Gravity.BOTTOM;
        }

        addView(mBodyLayout, flp);

        mStaticWidth = mItemWidth * mNumItems + (int) Math.ceil(14 * mDensity);

        BitmapDrawable arrowDrawable = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.test_arrow);
        Bitmap arrowBmp = arrowDrawable.getBitmap();

        mArrowWidth =  (int) Math.floor(arrowBmp.getWidth() * mItemWidth / (float) itemBmpWidth);
        int arrowHeight = (int) Math.floor(mArrowWidth * arrowBmp.getHeight() / arrowBmp.getWidth());

        mArrowView = new ImageView(context);
        mArrowView.setImageResource(R.drawable.test_arrow);
        if (isUpperMenu) {
            mArrowView.setRotation(180);
        }

        FrameLayout.LayoutParams flp2 = new FrameLayout.LayoutParams(mArrowWidth, arrowHeight);
        if (isUpperMenu) {
            flp2.gravity = Gravity.BOTTOM;
        }

        if (arrowPos == ARROW_POS_LEFT) {
            mArrowLeftMargin = (int) Math.ceil(7 * mDensity + (mItemWidth - mArrowWidth) / 2f);
        } else if (arrowPos == ARROW_POS_RIGHT) {
            mArrowLeftMargin = (int) Math.ceil(7 * mDensity + mNumItems * mItemWidth - (mItemWidth + mArrowWidth) / 2f);
        } else {
            mArrowLeftMargin = (int) Math.ceil((mStaticWidth - mArrowWidth) / 2f);
        }
        flp2.leftMargin = mArrowLeftMargin;
        addView(mArrowView, flp2);

        mStaticHeight = itemHeight + arrowHeight + (int) Math.round(6.8 * mDensity);
    }

    void setArrowLeftMargin(int leftMargin) {
        ((FrameLayout.LayoutParams) mArrowView.getLayoutParams()).leftMargin = leftMargin;
        mArrowLeftMargin = leftMargin;
    }

    private void addMenuItem(View v) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.weight = 1;
        mBodyLayout.addView(v, lp);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            v.setPressed(true);
            break;

        case MotionEvent.ACTION_MOVE:
            if (v.isPressed()) {

                final float x = event.getX();
                final float y = event.getY();

                if (x < 0 || x >= v.getWidth() || y < 0 || y >= v.getHeight()) {
                    v.setPressed(false);
                }
            }
            break;
        case MotionEvent.ACTION_UP:

            if (v.isPressed()) {
                v.setPressed(false);

                if (mOnClickListener != null) {
                    mOnClickListener.onMenuClick(v);
                }
            }
            break;
        }

        return true;
    }

    interface OnClickListener {
        void onMenuClick(View v);
    }

    void dispose() {

//        final int count = getChildCount();
//        for (int i = 1; i < count; i++) {
//            final ImageView v = (ImageView) mBodyLayout.getChildAt(i);
//            BitmapDrawable d = (BitmapDrawable) v.getDrawable();
//            d.getBitmap().recycle();
//            v.setImageBitmap(null);
//            v.setImageDrawable(null);
//        }
//        mBodyLayout.removeAllViews();
    }
}
