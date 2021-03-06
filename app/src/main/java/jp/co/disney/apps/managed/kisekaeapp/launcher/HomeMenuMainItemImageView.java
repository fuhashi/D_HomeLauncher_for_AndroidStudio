package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.widget.ImageView;

public class HomeMenuMainItemImageView extends ImageView {

    public HomeMenuMainItemImageView(Context context) {
        this(context, null);
    }

    public HomeMenuMainItemImageView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public HomeMenuMainItemImageView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        setClickable(true);
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (isPressed()) {
            getDrawable().setColorFilter(Color.rgb(255, 39, 166), PorterDuff.Mode.SRC_ATOP);

        } else {
            getDrawable().clearColorFilter();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width * getDrawable().getIntrinsicHeight() / getDrawable().getIntrinsicWidth();
        setMeasuredDimension(width, height);
    }
}
