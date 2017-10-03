package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.ContentValues;

public class DummyInfo extends ItemInfo {

    static final int DUMMY_TYPE_DL = 0;
    static final int DUMMY_TYPE_PICKER = 1;

    int dummyState;

    boolean inAppWidget;
    String inAppWidgetClassName;

    String widgetPackageName;

    public DummyInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_DUMMY;
    }

    public DummyInfo(DummyInfo info) {
        super(info);
        itemType = LauncherSettings.Favorites.ITEM_TYPE_DUMMY;
        dummyState = info.dummyState;
        inAppWidget = info.inAppWidget;
        inAppWidgetClassName = info.inAppWidgetClassName;
        widgetPackageName = info.widgetPackageName;
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.DUMMY_TYPE, inAppWidget ? 1 : 0);
        if (inAppWidget) {
            values.put(LauncherSettings.Favorites.DUMMY_NAME, inAppWidgetClassName);
        } else {
            values.put(LauncherSettings.Favorites.DUMMY_NAME, widgetPackageName);
        }
    }
}
