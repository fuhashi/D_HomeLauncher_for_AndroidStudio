package jp.co.disney.apps.managed.kisekaeapp;

import android.util.Log;

public class DebugLog {

    public static final DebugLog instance = new DebugLog();

    public DebugLog() {
        super();
    }

//	private static boolean isVisible = false;//TODO リリース時はfalse
    private static boolean isVisible = false;//TODO リリース時はfalse
    public final int D = 0, I =1, E = 2;//とりあえずd,i,e

//	public void setVisible(boolean b){
//		isVisible = b;
//	}

    public void outputLog(String tag, String msg){
        outputLog(D, tag, msg);
    }

    public void outputLog(int type, String tag, String msg){
        if(isVisible){
            switch (type) {
            default:
            case D:
                Log.d(tag, msg);
                break;
            case I:
                Log.i(tag, msg);
                break;
            case E:
                Log.e(tag, msg);
                break;
            }
        }
    }

}
