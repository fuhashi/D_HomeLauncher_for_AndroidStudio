package jp.co.disney.apps.managed.kisekaeapp.launcher;

public class KisekaeInfo {

    private String mAppName;

    private String mThemeId;

    private boolean mInAppTheme;

    private int mContentsType;

    private String mIconName;

    private String mAppTitle;

    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
    }

    public String getThemeId() {
        return mThemeId;
    }

    public void setThemeId(String themeId) {
        this.mThemeId = themeId;
    }

    public boolean isInAppTheme() {
        return mInAppTheme;
    }

    public void setInAppTheme(boolean inAppTheme) {
        this.mInAppTheme = inAppTheme;
    }

    public int getContentsType() {
        return mContentsType;
    }

    public void setContentsType(int contentsType) {
        mContentsType = contentsType;
    }

    public String getIconName() {
        return mIconName;
    }

    public void setIconName(String iconName) {
        this.mIconName = iconName;
    }

    public String getAppTitle() {
        return mAppTitle;
    }

    public void setAppTitle(String appTitle) {
        mAppTitle = appTitle;
    }
}
