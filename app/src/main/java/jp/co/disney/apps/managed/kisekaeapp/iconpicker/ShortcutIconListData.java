package jp.co.disney.apps.managed.kisekaeapp.iconpicker;

import android.graphics.Bitmap;

public class ShortcutIconListData {
//    private Bitmap imageData_;
//    private String textData_;
    
    //アイコン種類（ざぶとんとか TODO
    //自身のアイコン画像
    //紐付けアプリパッケージ
    //紐付け先のアプリアイコン
    private Bitmap selfIconData_ = null;
    private String selfIconPath = "";
    
    private Bitmap appIconData_ = null;
    private String appPackage_ = "";
    private String appName_ = "";
    private String appClassName_ = "";
    
    private boolean isChecked = false;
 
    public void setSelfIconData(Bitmap image) {
        selfIconData_ = image;
    }
 
    public Bitmap getSelfIconData() {
        return selfIconData_;
    }
    
    public void setAppIconData(Bitmap image) {
        appIconData_ = image;
    }
 
    public Bitmap getAppIconData() {
        return appIconData_;
    }

    public void setAppPackage(String packageName) {
        appPackage_ = packageName;
    }
 
    public String getAppPackage() {
        return appPackage_;
    }
 
    public void setAppName(String appName) {
        appName_ = appName;
    }
 
    public String getAppName() {
        return appName_;
    }
 
    public void setAppClassName(String appClassName) {
        appClassName_ = appClassName;
    }
 
    public String getAppClassName() {
        return appClassName_;
    }
 
    public void setSelfIconPath(String path) {
        selfIconPath = path;
    }
 
    public String getSelfIconPath() {
        return selfIconPath;
    }

    public void setChecked(boolean check) {
        isChecked = check;
    }
 
    public boolean getChecked() {
        return isChecked;
    }
    
 
}