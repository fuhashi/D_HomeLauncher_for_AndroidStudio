package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class NameEditText extends EditText {

    private NameEditView mNameEditView;

    public NameEditText(Context context) {
        super(context);
    }

    public NameEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NameEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setNameEditView(NameEditView nameEditView) {
        mNameEditView = nameEditView;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == android.view.KeyEvent.KEYCODE_BACK) {
            if (mNameEditView != null) {
                mNameEditView.doneEditingName();
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }
}
