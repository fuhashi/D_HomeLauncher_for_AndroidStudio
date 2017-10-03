package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;

class ApplicationInfoBase extends ItemInfo {
    static final String TAG = "ApplicationInfoBase";

    /**
     * The intent used to start the application.
     */
    Intent intent;

    /**
     * The time at which the app was first installed.
     */
    long firstInstallTime;

    ComponentName componentName;

    static final int DOWNLOADED_FLAG = 1;
    static final int UPDATED_SYSTEM_APP_FLAG = 2;

    int flags = 0;

    ApplicationInfoBase() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    /**
     * Must not hold the Context.
     */
    public ApplicationInfoBase(PackageManager pm, ResolveInfo info, HashMap<Object, CharSequence> labelCache) {
        initCore(pm, info);
        this.title = getApplicationTitle(pm, this.componentName, info, labelCache);
    }

    public ApplicationInfoBase(ApplicationInfoBase info) {
        super(info);
        componentName = info.componentName;
        title = info.title.toString();
        intent = new Intent(info.intent);
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
    }

    protected void initCore(PackageManager pm, ResolveInfo info) {

        final String packageName = info.activityInfo.applicationInfo.packageName;

        this.componentName = new ComponentName(packageName, info.activityInfo.name);
        this.container = ItemInfo.NO_ID;

        this.setActivity(componentName,
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        try {
            int appFlags = pm.getApplicationInfo(packageName, 0).flags;
            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
                flags |= DOWNLOADED_FLAG;
            }
            if ((appFlags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                flags |= UPDATED_SYSTEM_APP_FLAG;
            }
            firstInstallTime = pm.getPackageInfo(packageName, 0).firstInstallTime;
        } catch (NameNotFoundException e) {
            DebugLog.instance.outputLog(TAG, "PackageManager.getApplicationInfo failed for " + packageName);
        }
    }

    /**
     * Creates the application intent based on a component name and various launch flags.
     * Sets {@link #itemType} to {@link LauncherSettings.BaseLauncherColumns#ITEM_TYPE_APPLICATION}.
     *
     * @param className the class name of the component representing the intent
     * @param launchFlags the launch flags
     */
    final void setActivity(ComponentName className, int launchFlags) {
        intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + title.toString() + ")";
    }

//    public static void dumpApplicationInfoList(String tag, String label,
//            ArrayList<ApplicationInfoBase> list) {
//        DebugLog.instance.outputLog(tag, label + " size=" + list.size());
//        for (ApplicationInfoBase info: list) {
//            DebugLog.instance.outputLog(tag, "   title=\"" + info.title + "\" firstInstallTime=" + info.firstInstallTime);
//        }
//    }

    public static String getApplicationTitle(PackageManager pm, ComponentName componentName,
            ResolveInfo info, HashMap<Object, CharSequence> labelCache) {

        String title = null;

        ComponentName key;
        if (info != null) {
            key = LauncherModel.getComponentNameFromResolveInfo(info);
        } else {
            key = componentName;
        }

        if (labelCache != null && labelCache.containsKey(key)) {
            title = labelCache.get(key).toString();
        } else {
            CharSequence labelSeq = info.loadLabel(pm);
            if (labelSeq != null) {
                title = labelSeq.toString();
                if (labelCache != null) {
                    labelCache.put(key, title);
                }
            }
        }
        if (title == null) {
            title = info.activityInfo.name;
        }

        return title;
    }
}