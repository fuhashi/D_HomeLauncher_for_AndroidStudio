package jp.co.disney.apps.managed.kisekaeapp.catalog.asynctask;

import jp.co.disney.apps.managed.kisekaeapp.catalog.contents.ThumbInfo;

public interface DownloadDetailThumbSeparateTaskCallback {

	//保存時のエラー
	void onFailedDownloadDetailThumbsSeparate(ThumbInfo info);
		 
	//ネットワーク関連でのエラー
	void onFailedDownloadDetailThumbsSeparateNetwork(ThumbInfo info);

	void onFinishedDownloadDetailThumbsSeparate(ThumbInfo info);
	  
}
