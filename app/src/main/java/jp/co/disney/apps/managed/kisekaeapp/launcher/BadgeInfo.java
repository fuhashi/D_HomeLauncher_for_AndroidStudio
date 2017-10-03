package jp.co.disney.apps.managed.kisekaeapp.launcher;

public class BadgeInfo {

    private String mAppName;

    private Integer mBadgeCount;

    private Integer mEnable;

    public String getAppName() {
        return mAppName;
    }

    public void setAppName(String appName) {
        this.mAppName = appName;
    }

    public Integer getBadgeCount() {
        return mBadgeCount;
    }

    public void setBadgeCount(Integer badgeCount) {
        this.mBadgeCount = badgeCount;
    }

    public Integer getEnable() {
        return mEnable;
    }

    public void setEnable(Integer enable) {
        this.mEnable = enable;
    }
}
