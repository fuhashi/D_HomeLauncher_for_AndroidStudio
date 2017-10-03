/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.co.disney.apps.managed.kisekaeapp.launcher;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import jp.co.disney.apps.managed.kisekaeapp.DebugLog;

/**
 * Represents an app in AllAppsView.
 */
class ApplicationInfo extends ApplicationInfoBase {
    static final String TAG = "ApplicationInfo";

    /**
     * A bitmap version of the application icon.
     */
    Bitmap iconBitmap;

    ApplicationInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    /**
     * Must not hold the Context.
     */
    public ApplicationInfo(PackageManager pm, ResolveInfo info, IconCache iconCache,
            HashMap<Object, CharSequence> labelCache) {
        initCore(pm, info);
        iconCache.getTitleAndIcon(this, info, labelCache);
    }

    @Override
    public String toString() {
        return "ApplicationInfo(title=" + title.toString() + ")";
    }

    public static void dumpApplicationInfoList(String tag, String label,
            ArrayList<ApplicationInfo> list) {
        DebugLog.instance.outputLog(tag, label + " size=" + list.size());
        for (ApplicationInfo info: list) {
            DebugLog.instance.outputLog(tag, "   title=\"" + info.title + "\" iconBitmap="
                    + info.iconBitmap + " firstInstallTime=" + info.firstInstallTime);
        }
    }

    public ShortcutInfo makeShortcut() {
        return new ShortcutInfo(this);
    }
}
