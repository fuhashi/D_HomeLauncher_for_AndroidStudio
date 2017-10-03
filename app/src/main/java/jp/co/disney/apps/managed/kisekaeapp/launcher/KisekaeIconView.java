package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.ImageView;
import jp.co.disney.apps.managed.kisekaeapp.R;

public class KisekaeIconView extends ImageView {

    private BitmapDrawable mOffIcon;
    private BitmapDrawable mOnIcon;

    public KisekaeIconView(Context context) {
        this(context, null);
    }

    public KisekaeIconView(Context context, AttributeSet attributeset) {
        this(context, attributeset, 0);
    }

    public KisekaeIconView(Context context, AttributeSet attributeset, int i) {
        super(context, attributeset, i);
        setClickable(true);

        mOffIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_kisekae);
        mOnIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.btn_kisekae_on);
        setImageDrawable(mOffIcon);
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (isPressed()) {
            setImageDrawable(mOnIcon);
        } else {
            setImageDrawable(mOffIcon);
        }
    }
}
