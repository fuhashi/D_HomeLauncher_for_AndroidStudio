package jp.co.disney.apps.managed.kisekaeapp.spp;

import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class AdBannerLayout {

	public AdBannerLayout() {
		super();
	}

	public int area = 0;
	public String bannerId = "";
	public String linkURL = "";
	public String imageURL= "";
	public boolean isBlockable = true;
	public int weight = 0;
	public BitmapDrawable adImage = null;

	public LinearLayout adParent = null;
	public ImageView bannerImageView = null, checkMessageView = null;
	public View cancelAreaView = null;
	public CheckBox blockCheckBox = null;
	public RelativeLayout checkAreaLayout = null;

	public void goneLayout(){
		if(adParent != null) adParent.setVisibility(View.GONE);
		if(bannerImageView != null) bannerImageView.setImageDrawable(null);
		if(adImage != null) adImage.getBitmap().recycle();
		if(adParent != null) adParent.setOnTouchListener(null);
	}

}
