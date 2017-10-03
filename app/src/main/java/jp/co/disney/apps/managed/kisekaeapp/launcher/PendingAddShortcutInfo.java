package jp.co.disney.apps.managed.kisekaeapp.launcher;

import android.content.pm.ActivityInfo;

class PendingAddShortcutInfo extends PendingAddItemInfo {

    ActivityInfo shortcutActivityInfo;

    public PendingAddShortcutInfo(ActivityInfo activityInfo) {
        shortcutActivityInfo = activityInfo;
    }

    @Override
    public String toString() {
        return "Shortcut: " + shortcutActivityInfo.packageName;
    }
}
