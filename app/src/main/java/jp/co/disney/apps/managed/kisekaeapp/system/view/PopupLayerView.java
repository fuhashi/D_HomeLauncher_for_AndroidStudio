package jp.co.disney.apps.managed.kisekaeapp.system.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class PopupLayerView extends FrameLayout {

    private final Rect mContentsPadding;

    public PopupLayerView(Context context) {
        this(context, null);
    }

    public PopupLayerView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public PopupLayerView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        mContentsPadding = new Rect();
    }

    public PopupView newPopup(View contentView) {
        return newPopup(contentView, true);
    }

    public PopupView newPopup(View contentView, boolean flag) {
        PopupView popView = new PopupView(getContext(), contentView, this);
        popView.setClipChildren(flag);
        popView.setClipToPadding(flag);
        popView.setPadding(mContentsPadding.left, mContentsPadding.top,
                mContentsPadding.right, mContentsPadding.bottom);
        return popView;
    }

    public boolean isHasPopup() {
        return getChildCount() > 0;
    }

    public Rect getContentPadding() {
        return mContentsPadding;
    }

    public void setContentPadding(int left, int top, int right, int bottom) {
        mContentsPadding.set(left, top, right, bottom);
        updateAllPopupsPaddings();
    }

    public void closeAllPopups() {

        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            if (view instanceof PopupView) {
                ((PopupView)view).cancel(true, 200L);
            }
        }
    }

    private void updateAllPopupsPaddings() {

        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            if (view instanceof PopupView) {
                ((PopupView)view).setPadding(mContentsPadding.left, mContentsPadding.top,
                        mContentsPadding.right, mContentsPadding.bottom);
            }
        }
    }
}
