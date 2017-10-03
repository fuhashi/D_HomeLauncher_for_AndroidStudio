package jp.co.disney.apps.managed.kisekaeapp.launcher;

import jp.co.disney.apps.managed.kisekaeapp.R;
import jp.co.disney.apps.managed.kisekaeapp.system.graphics.FastBitmapDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Selection;
import android.text.Spannable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class NameEditView extends RelativeLayout implements View.OnFocusChangeListener {

    private InputMethodManager mInputMethodManager;

    private NameEditText mNameEditText;
    private ImageView mIconView;
    private ImageView mBtnComplete;
    private ImageView mBtnCancel;

    private boolean mIsEditingName;

    private String mHintText;

    private boolean mDisposeIcon = false;

    public NameEditView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mInputMethodManager = (InputMethodManager)
                getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        setFocusableInTouchMode(true);
    }

    void init(String name, String hint, Bitmap icon, boolean disposeIcon) {
        mNameEditText.setText(name);
        if (icon != null) {
            mIconView.setImageDrawable(new FastBitmapDrawable(icon));
        }
        mHintText = hint;
        mNameEditText.setHint(hint);

        mDisposeIcon = disposeIcon;
    }

    static NameEditView fromXml(Context context) {
        return (NameEditView) LayoutInflater.from(context).inflate(R.layout.name_edit, null);
    }

    public EditText getNameEditText() {
        return mNameEditText;
    }

    public String getText() {
        return mNameEditText.getText().toString();
    }

    public void setHint(String hintText) {
        mNameEditText.setHint(hintText);
    }

    public void setOnCompleteListner(View.OnClickListener onClickListener) {
        mBtnComplete.setOnClickListener(onClickListener);
    }

    public void setOnCancelListner(View.OnClickListener onClickListener) {
        mBtnCancel.setOnClickListener(onClickListener);
    }

    public boolean editTextIsFocused() {
        return mNameEditText.isFocused();
    }

    public boolean isFocusedTextStart() {
        return (mNameEditText.getSelectionStart() == 0);
    }

    public boolean isFocusedTextEnd() {
        return (mNameEditText.getSelectionEnd() == mNameEditText.length());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mNameEditText = (NameEditText) findViewById(R.id.nmae_edit);
        mNameEditText.setNameEditView(this);
        mNameEditText.setOnFocusChangeListener(this);

        mIconView = (ImageView) findViewById(R.id.name_edit_icon);

        mBtnComplete = (ImageView) findViewById(R.id.name_edit_complete);
        mBtnCancel = (ImageView) findViewById(R.id.name_edit_cancel);
    }

    public boolean isEditingName() {
        return mIsEditingName;
    }

    public void startEditingName() {
        mNameEditText.setHint("");
        mIsEditingName = true;
    }

    public void dismissEditingName() {
        mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
        doneEditingName();
    }

    public void doneEditingName() {
        mNameEditText.setHint(mHintText);

        requestFocus();

        Selection.setSelection((Spannable) mNameEditText.getText(), 0, 0);

        mIsEditingName = false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (v == mNameEditText && hasFocus) {
            startEditingName();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return true;
    }

    public void dispose() {

        FastBitmapDrawable drawable = (FastBitmapDrawable) mIconView.getDrawable();
        if (drawable != null) {

            if (mDisposeIcon && drawable.getBitmap() != null) {
                drawable.getBitmap().recycle();
            }
            drawable.setBitmap(null);
            mIconView.setImageDrawable(null);
        }

        mBtnComplete.setOnClickListener(null);
        mBtnCancel.setOnClickListener(null);
    }
}
