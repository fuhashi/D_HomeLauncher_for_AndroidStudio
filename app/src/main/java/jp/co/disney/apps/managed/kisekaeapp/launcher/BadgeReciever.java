package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BadgeReciever extends BroadcastReceiver {
    private static final String TAG = "BadgeReciever";

    public static final String EXTRA_BADGE_COUNT_PACKAGE_NAME = "badge_count_package_name";
    public static final String EXTRA_BADGE_COUNT_CLASS_NAME = "badge_count_class_name";
    public static final String EXTRA_BADGE_COUNT = "badge_count";

    @Override
    public void onReceive(Context context, Intent intent) {

        String pkgName = intent.getStringExtra(EXTRA_BADGE_COUNT_PACKAGE_NAME);
        String className = intent.getStringExtra(EXTRA_BADGE_COUNT_CLASS_NAME);
        int badgeCount = intent.getIntExtra(EXTRA_BADGE_COUNT, -1);
        Log.d(TAG, "BadgeReciever onReceive: [package]=" + pkgName + "[class]=" + className
                + "[badgeCount]=" + String.valueOf(badgeCount));

//        Toast.makeText(context, "BadgeReciever onReceive: [package]=" + pkgName + "[class]=" + className
//                + "[badgeCount]=" + String.valueOf(badgeCount), Toast.LENGTH_LONG).show();

        boolean launcherNotLoaded = LauncherModel.getCellCountX() <= 0 ||
                LauncherModel.getCellCountY() <= 0;
        if (launcherNotLoaded) return;

        if (pkgName == null || pkgName.equals("")
                || className == null || className.equals("")
                || badgeCount < 0) {
            return;
        }

        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        ((KisekaeLauncherModel) app.getModel()).updateBadgeInfo(context, pkgName,
                className, badgeCount);
    }
}
