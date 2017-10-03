package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.List;

import jp.co.disney.apps.managed.kisekaeapp.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class DummyHomeItemView extends ImageView {


    public DummyHomeItemView(Context context) {
        super(context);
        init();
    }

    public DummyHomeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DummyHomeItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void applyFromDummyInfo(DummyInfo info) {
        setTag(info);
    }

    public void updateState() {
        DummyInfo info = (DummyInfo) getTag();
        if (info == null) return;

        if (info.inAppWidget) {
            info.dummyState = DummyInfo.DUMMY_TYPE_PICKER;
        } else {

            if (info.widgetPackageName == null) return;

            if (ThemeUtils.checkAppInstalled(getContext(), info.widgetPackageName)) {
                info.dummyState = DummyInfo.DUMMY_TYPE_PICKER;
            } else {
                info.dummyState = DummyInfo.DUMMY_TYPE_DL;
            }
        }

        if (info.dummyState == DummyInfo.DUMMY_TYPE_DL) {
            setImageResource(R.drawable.guide_dl_icon);
        } else if (info.dummyState == DummyInfo.DUMMY_TYPE_PICKER) {
            setImageResource(R.drawable.guide_tap_icon);
        }
    }

    private void init() {
        setBackgroundResource(R.drawable.guide_widget);
//        setBackgroundColor(Color.argb(100, 0, 255, 0));
        setScaleType(ScaleType.CENTER);
        setClickable(true);
    }

    // Listに設定するアイテム項目
    public static class Item {
        private String stringItem;
        private Drawable imageData_;

        public void setImagaData(Drawable image) {
            imageData_ = image;
        }

        public Drawable getImageData() {
            return imageData_;
        }

        public void setStringItem(String stringItem) {
            this.stringItem = stringItem;
        }

        public String getStringItem() {
            return this.stringItem;
        }
    }

    public static class CustomAdapter extends ArrayAdapter<Item> {
        private LayoutInflater inflater;

        public CustomAdapter(Context context, int resource, List<Item> objects) {
            super(context, resource, objects);
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View v, ViewGroup parent) {
            Item item = (Item) getItem(position);
            if (null == v)
                v = inflater.inflate(R.layout.widget_disney_picker_row, null);
            ImageView imageView;
            imageView = (ImageView) v.findViewById(R.id.row_imgview1);
            imageView.setImageDrawable(item.getImageData());

            TextView stringTextView = (TextView) v
                    .findViewById(R.id.row_textview1);
            stringTextView.setText(item.getStringItem());

            return v;
        }
    }

}
