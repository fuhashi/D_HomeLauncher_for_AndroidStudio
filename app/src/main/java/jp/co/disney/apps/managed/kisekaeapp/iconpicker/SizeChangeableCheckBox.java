package jp.co.disney.apps.managed.kisekaeapp.iconpicker;

import jp.co.disney.apps.managed.kisekaeapp.R;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.ImageButton;

public class SizeChangeableCheckBox extends ImageButton implements Checkable {
	private boolean mChecked;
	private OnCheckedChangeListener mOnCheckedChangeListener;
    
	@SuppressLint("NewApi")
	public SizeChangeableCheckBox(Context context, AttributeSet attrs,
			int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public SizeChangeableCheckBox(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		
		 TypedArray checkedA = context.obtainStyledAttributes(attrs, new int [] { android.R.attr.focusable, android.R.attr.checked }, defStyleAttr, 0);
		 mChecked = checkedA.getBoolean(1, false);
		 setChecked(mChecked);

	}

	public SizeChangeableCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public SizeChangeableCheckBox(Context context) {
		super(context);
		// TODO 自動生成されたコンストラクター・スタブ
	}
	
	@Override
	public boolean performClick() {
		toggle();
		return super.performClick();
	}

	public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
		mOnCheckedChangeListener = listener;
	}
	
	public static interface OnCheckedChangeListener {
		void onCheckedChanged(ImageButton buttonView, boolean isChecked);
	}
	
	
	//--------------Checkable---------------
	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		//style通りの画像に変更する
		if(checked){
			setImageResource(R.drawable.icon_listtype_checkbox_on);
		}else{
			setImageResource(R.drawable.icon_listtype_checkbox_off);
		}
		
		if (mOnCheckedChangeListener != null) {
			mOnCheckedChangeListener.onCheckedChanged(this, mChecked);
		}
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}

}
